package sk.fiit.rabbit.adaptiveproxy.plugins.services.injector;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import sk.fiit.peweproxy.headers.ResponseHeader;
import sk.fiit.peweproxy.messages.HttpResponse;
import sk.fiit.peweproxy.messages.ModifiableHttpResponse;
import sk.fiit.peweproxy.plugins.PluginProperties;
import sk.fiit.peweproxy.plugins.services.ResponseServiceModule;
import sk.fiit.peweproxy.plugins.services.ResponseServiceProvider;
import sk.fiit.peweproxy.services.ProxyService;
import sk.fiit.peweproxy.services.ServiceUnavailableException;
import sk.fiit.peweproxy.services.content.ModifiableStringService;
import sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions.HtmlInjectorService;

public class HtmlInjectorServiceModule implements ResponseServiceModule {
	
	private static final Logger logger = Logger.getLogger(HtmlInjectorServiceModule.class);
	
	private class HtmlInjectorServiceProvider implements HtmlInjectorService, ResponseServiceProvider<HtmlInjectorService> {

		private int insertIndex;
		private String requestURI;
		private String textToInsert;
		
		public HtmlInjectorServiceProvider(String requestURI) {
			this.requestURI = requestURI;
		}
		
		private class InjectingInstruction {
			String text;
			HtmlPosition position;
			public InjectingInstruction(String text, HtmlPosition position) {
				this.text = text;
				this.position = position;
			}
		}
		
		private List<InjectingInstruction> instructions = new LinkedList<InjectingInstruction>();
		
		@Override
		public void inject(String text, HtmlPosition position) {
			logger.warn("Som " + this + " a pridavam do queue s velkostou " + instructions.size());
			instructions.add(new InjectingInstruction(text, position));
		}
		
		private void injectToMessage(StringBuilder content, String text, HtmlPosition position) {
			this.textToInsert = text;
			
			String html = content.toString().toLowerCase();
			
			switch(position) {
			case END_OF_BODY:
				insertIndex = html.indexOf("</body>");
				if(insertIndex < 0) {
					logger.debug("No </body> found for " + requestURI);
					
					for(Pattern exceptionPattern : endOfBodyExceptions) {
						Matcher matcher = exceptionPattern.matcher(requestURI);
						if(matcher.matches()) {
							insertIndex = html.length();
						}
					}
				}
				break;
			case START_OF_BODY:
				insertIndex = html.indexOf("<body");
				if(insertIndex > 0) {
					insertIndex = html.indexOf(">", insertIndex);
					if(insertIndex > 0) {
						insertIndex++;
					}
				} else {
					logger.debug("No <body> found for " + requestURI);
				}
				break;
			case ON_MARK:
				insertIndex = html.indexOf("<!-- __ap_scripts__ -->");
				if(insertIndex > 0) {
					insertIndex += "<!-- __ap_scripts__ -->".length();
				}
				break;
			default:
				throw new RuntimeException("Uknown position: " + position);
			}
			
			if(insertIndex > 0) {
				content.insert(insertIndex, textToInsert);
			}
		}

		@Override
		public String getServiceIdentification() {
			return this.getClass().getName();
		}

		@Override
		public HtmlInjectorService getService() {
			return this;
		}

		@Override
		public boolean initChangedModel() {
			return false;
		}

		@Override
		public void doChanges(ModifiableHttpResponse response) {
			StringBuilder content = response.getServicesHandle().getService(ModifiableStringService.class).getModifiableContent();
			for (InjectingInstruction instruction : instructions) {
				injectToMessage(content, instruction.text, instruction.position);
			}
		}
	}
	
	private Set<Pattern> endOfBodyExceptions = new HashSet<Pattern>();
	
	@Override
	public boolean start(PluginProperties props) {
		String exceptions = props.getProperty("endOfBodyExceptions");
		for(String exception : exceptions.split(",")) {
			endOfBodyExceptions.add(Pattern.compile(exception.trim()));
		}
		return true;
	}

	@Override
	public void desiredResponseServices(
			Set<Class<? extends ProxyService>> desiredServices,
			ResponseHeader webRPHeader) {
		desiredServices.add(ModifiableStringService.class);
	}
	
	@Override
	public void getProvidedResponseServices(
			Set<Class<? extends ProxyService>> providedServices) {
		providedServices.add(HtmlInjectorService.class);
	}

	@Override
	public boolean supportsReconfigure(PluginProperties newProps) {
		return true;
	}

	@Override
	public void stop() {
	}

	@Override
	public <Service extends ProxyService> ResponseServiceProvider<Service> provideResponseService(
			HttpResponse response, Class<Service> serviceClass)
			throws ServiceUnavailableException {

		if (serviceClass == HtmlInjectorService.class) {
			String requestURI = response.getRequest().getClientRequestHeader().getRequestURI();
			
			return (ResponseServiceProvider<Service>) new HtmlInjectorServiceProvider(requestURI);
		}
		
		return null;
	}
		
	
}

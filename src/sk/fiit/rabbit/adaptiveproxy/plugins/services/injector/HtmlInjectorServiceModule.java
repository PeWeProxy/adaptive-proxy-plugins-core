package sk.fiit.rabbit.adaptiveproxy.plugins.services.injector;

import java.util.HashSet;
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
import sk.fiit.peweproxy.services.content.StringContentService;
import sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions.HtmlInjectorService;

public class HtmlInjectorServiceModule implements ResponseServiceModule {
	
	private static final Logger logger = Logger.getLogger(HtmlInjectorServiceModule.class);
	
	private class HtmlInjectorServiceProvider implements HtmlInjectorService, ResponseServiceProvider<HtmlInjectorService> {

		private String requestURI;
		private StringBuilder content;
		
		public HtmlInjectorServiceProvider(StringBuilder content, String requestURI) {
			this.content = content;
			this.requestURI = requestURI;
		}
		
		@Override
		public void inject(String text, HtmlPosition position) {
			int insertIndex;
			switch(position) {
			case END_OF_BODY:
				insertIndex = content.indexOf("</body>");
				if(insertIndex < 0) {
					logger.debug("No </body> found for " + requestURI);
					
					for(Pattern exceptionPattern : endOfBodyExceptions) {
						Matcher matcher = exceptionPattern.matcher(requestURI);
						if(matcher.matches()) {
							insertIndex = content.length();
						}
					}
				}
				break;
			case START_OF_BODY:
				insertIndex = content.indexOf("<body");
				if(insertIndex > 0) {
					insertIndex = content.indexOf(">", insertIndex);
					if(insertIndex > 0) {
						insertIndex++;
					}
				} else {
					logger.debug("No <body> found for " + requestURI);
				}
				break;
			default:
				throw new RuntimeException("Uknown position: " + position);
			}
			
			if(insertIndex > 0) {
				content.insert(insertIndex, text);
			}
		}
		
		@Override
		public void injectAfter(String afterThis, String what) {
			int insertIndex = content.indexOf(afterThis);
			if(insertIndex > 0) {
				insertIndex += afterThis.length();
			}
			
			if(insertIndex > 0) {
				content.insert(insertIndex, what);
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
			if(response.getServicesHandle().isServiceAvailable(ModifiableStringService.class)) {
				response.getServicesHandle().getService(ModifiableStringService.class).setContent(content.toString());
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
			String requestURI = response.getRequest().getOriginalRequest().getRequestHeader().getRequestURI();
			StringBuilder content = new StringBuilder(response.getServicesHandle().getService(StringContentService.class).getContent());
			return (ResponseServiceProvider<Service>) new HtmlInjectorServiceProvider(content, requestURI);
		}
		
		return null;
	}
		
	
}

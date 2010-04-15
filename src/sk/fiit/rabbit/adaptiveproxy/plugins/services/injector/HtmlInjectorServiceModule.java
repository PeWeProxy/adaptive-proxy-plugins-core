package sk.fiit.rabbit.adaptiveproxy.plugins.services.injector;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import sk.fiit.rabbit.adaptiveproxy.plugins.PluginProperties;
import sk.fiit.rabbit.adaptiveproxy.plugins.headers.ResponseHeaders;
import sk.fiit.rabbit.adaptiveproxy.plugins.helpers.ResponseServicePluginAdapter;
import sk.fiit.rabbit.adaptiveproxy.plugins.helpers.ResponseServiceProviderAdapter;
import sk.fiit.rabbit.adaptiveproxy.plugins.messages.HttpResponse;
import sk.fiit.rabbit.adaptiveproxy.plugins.messages.ModifiableHttpResponse;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.ProxyService;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.ResponseServiceProvider;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.ServiceUnavailableException;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.content.ModifiableStringService;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.content.StringContentService;

public class HtmlInjectorServiceModule extends ResponseServicePluginAdapter {
	
	private static final Logger logger = Logger.getLogger(HtmlInjectorServiceModule.class);
	
	private class HtmlInjectorServiceProvider extends ResponseServiceProviderAdapter implements HtmlInjectorService {

		private StringBuilder content;
		private int insertIndex;
		private String requestURI;
		private String textToInsert;

		public HtmlInjectorServiceProvider(String requestURI) {
			this.requestURI = requestURI;
		}
		
		@Override
		public Class<? extends ProxyService> getServiceClass() {
			return HtmlInjectorService.class;
		}
		
		@Override
		public void setResponseContext(ModifiableHttpResponse response) {
			try {
				this.content = response.getServiceHandle().getService(ModifiableStringService.class).getModifiableContent();
			} catch (ServiceUnavailableException e) {
				logger.error("ModifiableStringService unavailable", e);
			}
		}
		
		@Override
		public void inject(String text, HtmlPosition position) {
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
	}
	
	private Set<Pattern> endOfBodyExceptions = new HashSet<Pattern>();

	@Override
	public boolean wantResponseContent(ResponseHeaders webRPHeaders) {
		return true;
	}
	
	@Override
	public boolean setup(PluginProperties props) {
		String exceptions = props.getProperty("endOfBodyExceptions");
		for(String exception : exceptions.split(",")) {
			endOfBodyExceptions.add(Pattern.compile(exception.trim()));
		}
		return true;
	}
	
	@Override
	protected void addDependencies(Set<Class<? extends ProxyService>> dependencies) {
		dependencies.add(ModifiableStringService.class);
	}
	
	@Override
	protected void addProvidedServices(Set<Class<? extends ProxyService>> providedServices) {
		providedServices.add(HtmlInjectorService.class);
	}
	
	@Override
	protected void addProvidedResponseServices(
			List<ResponseServiceProvider> providedServices,
			HttpResponse response) {
		try {
			response.getServiceHandle().getService(StringContentService.class);
			providedServices.add(new HtmlInjectorServiceProvider(response.getClientRequestHeaders().getRequestURI()));
		} catch (ServiceUnavailableException e) {
			logger.trace("ModifiableStringService is unavailable");
		}
	}
	
	
}

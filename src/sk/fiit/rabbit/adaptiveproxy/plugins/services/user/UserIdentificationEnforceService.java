package sk.fiit.rabbit.adaptiveproxy.plugins.services.user;

import java.util.Set;

import org.apache.log4j.Logger;

import sk.fiit.peweproxy.headers.ResponseHeader;
import sk.fiit.peweproxy.messages.HttpMessageFactory;
import sk.fiit.peweproxy.messages.HttpResponse;
import sk.fiit.peweproxy.messages.ModifiableHttpResponse;
import sk.fiit.peweproxy.plugins.PluginProperties;
import sk.fiit.peweproxy.plugins.processing.ResponseProcessingPlugin;
import sk.fiit.peweproxy.services.ProxyService;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.injector.HtmlInjectorService;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.injector.HtmlInjectorService.HtmlPosition;

public class UserIdentificationEnforceService implements ResponseProcessingPlugin {
	
	private static final Logger logger = Logger.getLogger(UserIdentificationEnforceService.class);

	private String notificationScript;
	
	@Override
	public ResponseProcessingActions processResponse(ModifiableHttpResponse response) {
		boolean noApuidNotificaton = false;
		
		UserIdentificationService userIdentificationService = response.getServicesHandle().getService(UserIdentificationService.class);
		if(userIdentificationService.getClientIdentification() == null) {
			noApuidNotificaton = true;
		}
		
		if(noApuidNotificaton) {
			logger.debug("Unidentified user detected");

			HtmlInjectorService htmlInjector = response.getServicesHandle().getService(HtmlInjectorService.class);
			htmlInjector.inject(notificationScript, HtmlPosition.ON_MARK);
		}
		
		return ResponseProcessingActions.PROCEED;
	}
	
	@Override
	public boolean start(PluginProperties props) {
		this.notificationScript = props.getProperty("notificationScript");
		return true;
	}

	@Override
	public void desiredResponseServices(
			Set<Class<? extends ProxyService>> desiredServices,
			ResponseHeader webRPHeader) {
		desiredServices.add(HtmlInjectorService.class);
		desiredServices.add(UserIdentificationService.class);
	}

	@Override
	public boolean supportsReconfigure(PluginProperties newProps) {
		return true;
	}

	@Override
	public void stop() {
	}

	@Override
	public HttpResponse getNewResponse(ModifiableHttpResponse response,
			HttpMessageFactory messageFactory) {
		return response;
	}

	@Override
	public void processTransferedResponse(HttpResponse response) {
	}
}

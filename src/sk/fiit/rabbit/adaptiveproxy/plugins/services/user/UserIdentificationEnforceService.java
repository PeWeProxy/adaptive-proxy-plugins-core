package sk.fiit.rabbit.adaptiveproxy.plugins.services.user;

import org.apache.log4j.Logger;

import sk.fiit.rabbit.adaptiveproxy.plugins.PluginProperties;
import sk.fiit.rabbit.adaptiveproxy.plugins.headers.ResponseHeaders;
import sk.fiit.rabbit.adaptiveproxy.plugins.helpers.ResponseProcessingPluginAdapter;
import sk.fiit.rabbit.adaptiveproxy.plugins.messages.ModifiableHttpResponse;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.ServiceUnavailableException;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.injector.HtmlInjectorService;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.injector.HtmlInjectorService.HtmlPosition;

public class UserIdentificationEnforceService extends ResponseProcessingPluginAdapter {
	
	private static final Logger logger = Logger.getLogger(UserIdentificationEnforceService.class);

	private String notificationScript;
	
	@Override
	public ResponseProcessingActions processResponse(ModifiableHttpResponse response) {
		boolean noApuidNotificaton = false;
		
		try {
			UserIdentificationService userIdentificationService = response.getServiceHandle().getService(UserIdentificationService.class);
			if(userIdentificationService.getClientIdentification() == null) {
				noApuidNotificaton = true;
			}
		} catch (ServiceUnavailableException e) {
			noApuidNotificaton = true;
		}
		
		if(noApuidNotificaton) {
			logger.debug("Unidentified user detected");

			try {
				HtmlInjectorService htmlInjector = response.getServiceHandle().getService(HtmlInjectorService.class);
				htmlInjector.inject(notificationScript, HtmlPosition.ON_MARK);
			} catch (ServiceUnavailableException e) {
				logger.debug("ServiceUnavailableException: HtmlInjectorService", e);
			}
		}
		
		return ResponseProcessingActions.PROCEED;
	}

	@Override
	public boolean wantResponseContent(ResponseHeaders clientRPHeaders) {
		return false;
	}
	
	@Override
	public boolean setup(PluginProperties props) {
		this.notificationScript = props.getProperty("notificationScript");
		return true;
	}

	@Override
	public boolean supportsReconfigure() {
		return false;
	}
}

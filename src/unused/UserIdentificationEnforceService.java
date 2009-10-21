package unused;

import org.apache.log4j.Logger;

import sk.fiit.rabbit.adaptiveproxy.plugins.PluginProperties;
import sk.fiit.rabbit.adaptiveproxy.plugins.headers.RequestHeaders;
import sk.fiit.rabbit.adaptiveproxy.plugins.headers.WritableResponseHeaders;
import sk.fiit.rabbit.adaptiveproxy.plugins.messages.HttpMessageFactory;
import sk.fiit.rabbit.adaptiveproxy.plugins.messages.HttpRequest;
import sk.fiit.rabbit.adaptiveproxy.plugins.messages.HttpResponse;
import sk.fiit.rabbit.adaptiveproxy.plugins.messages.ModifiableHttpRequest;
import sk.fiit.rabbit.adaptiveproxy.plugins.messages.ModifiableHttpResponse;
import sk.fiit.rabbit.adaptiveproxy.plugins.processing.RequestProcessingPlugin;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.ServiceUnavailableException;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.user.UserIdentificationService;

public class UserIdentificationEnforceService implements RequestProcessingPlugin {
	
	private static final Logger logger = Logger.getLogger(UserIdentificationEnforceService.class);
	
	private String redirectTo;

	@Override
	public HttpRequest getNewRequest(ModifiableHttpRequest proxyRequest,
			HttpMessageFactory messageFactory) {
		return null;
	}

	@Override
	public HttpResponse getResponse(ModifiableHttpRequest proxyRequest,
			HttpMessageFactory messageFactory) {
		
		ModifiableHttpResponse r = messageFactory.constructHttpResponse(true);
		WritableResponseHeaders headers = r.getProxyResponseHeaders();
		
		headers.setStatusLine("HTTP/1.1 302 Found");
		headers.setHeader("Location", redirectTo);
		
		return r;
	}

	@Override
	public RequestProcessingActions processRequest(ModifiableHttpRequest request) {
		String requestUri = request.getClientRequestHeaders().getRequestURI();

		if(requestUri.startsWith(redirectTo)) {
			return RequestProcessingActions.PROCEED;
		}
		
		try {
			UserIdentificationService userIdentificationService = request.getServiceHandle().getService(UserIdentificationService.class);
			if(userIdentificationService.getClientIdentification() == null) {
				logger.debug("Unidentified user detected");
				return RequestProcessingActions.NEW_RESPONSE;
			} else {
				return RequestProcessingActions.PROCEED;
			}
		} catch (ServiceUnavailableException e) {
			logger.error("Service unavailable");
			return RequestProcessingActions.PROCEED;
		}
	}

	@Override
	public boolean wantRequestContent(RequestHeaders clientRQHeaders) {
		return false;
	}

	@Override
	public boolean setup(PluginProperties props) {
		redirectTo = props.getProperty("redirectTo", null);
		return true;
	}

	@Override
	public void start() {
	}

	@Override
	public void stop() {
	}

	@Override
	public boolean supportsReconfigure() {
		return false;
	}
}

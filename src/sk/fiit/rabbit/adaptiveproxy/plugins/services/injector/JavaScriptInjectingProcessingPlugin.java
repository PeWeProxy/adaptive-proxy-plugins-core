package sk.fiit.rabbit.adaptiveproxy.plugins.services.injector;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;

import sk.fiit.rabbit.adaptiveproxy.plugins.PluginProperties;
import sk.fiit.rabbit.adaptiveproxy.plugins.helpers.RequestAndResponseProcessingPluginAdapter;
import sk.fiit.rabbit.adaptiveproxy.plugins.messages.HttpMessageFactory;
import sk.fiit.rabbit.adaptiveproxy.plugins.messages.HttpRequest;
import sk.fiit.rabbit.adaptiveproxy.plugins.messages.ModifiableHttpRequest;
import sk.fiit.rabbit.adaptiveproxy.plugins.messages.ModifiableHttpResponse;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.ServiceUnavailableException;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.content.ModifiableStringService;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.user.UserIdentificationService;

public class JavaScriptInjectingProcessingPlugin extends RequestAndResponseProcessingPluginAdapter {
	
	protected Logger logger = Logger.getLogger(JavaScriptInjectingProcessingPlugin.class);
	
	private String scriptUrl;
	private String bypassPattern;
	private String bypassTo;
	private String additionalHTML;
	private Set<String> allowOnlyFor = new HashSet<String>();
	private boolean generateResponse;
	
	@Override
	public RequestProcessingActions processRequest(ModifiableHttpRequest request) {
		try {
			UserIdentificationService userIdentification = request.getServiceHandle().getService(UserIdentificationService.class);
	
			if(allowOnlyFor.isEmpty() || allowOnlyFor.contains(userIdentification.getClientIdentification())) {
				
				if(request.getClientRequestHeaders().getRequestURI().contains(bypassPattern)) {
					if(generateResponse) {
						return RequestProcessingActions.NEW_RESPONSE;
					} else {
						return RequestProcessingActions.FINAL_REQUEST;
					}
				}
			}
		} catch (ServiceUnavailableException e) {
			logger.warn("Service unavailable", e);
		}
		
		return RequestProcessingActions.PROCEED;
	}
	
	@Override
	public HttpRequest getNewRequest(ModifiableHttpRequest proxyRequest, HttpMessageFactory messageFactory) {
		String queryParams;
		
		int queryParamsIdx = proxyRequest.getClientRequestHeaders().getRequestURI().indexOf("?");
		
		if(queryParamsIdx > -1) {
			queryParams = proxyRequest.getClientRequestHeaders().getRequestURI().substring(queryParamsIdx);
		} else {
			queryParams = "";
		}
		
		proxyRequest.getProxyRequestHeaders().setRequestURI(bypassTo + queryParams);
		
		try {
			URL url = new URL(bypassTo);
			proxyRequest.getProxyRequestHeaders().removeHeader("Host");
			proxyRequest.getProxyRequestHeaders().addHeader("Host", url.getHost());
		} catch (MalformedURLException e) {
			logger.warn("Malformed URL", e);
		}
		
		return proxyRequest;
	}
	
	@Override
	public ResponseProcessingActions processResponse(ModifiableHttpResponse response) {
		try {
			ModifiableStringService ms = response.getServiceHandle().getService(ModifiableStringService.class);
			
			StringBuilder sb = ms.getModifiableContent();
			
			String html = sb.toString();

			int bodyEndIDx = html.toLowerCase().indexOf("</body>");
			if(bodyEndIDx < 0) {
				logger.debug("No </body> : " + response.getProxyRequestHeaders().getRequestURI());
				return ResponseProcessingActions.PROCEED;
			}
			
			String scripts = "<script src='" + scriptUrl + "'></script>";
			
			sb.insert(bodyEndIDx, additionalHTML + scripts);
		} catch (ServiceUnavailableException e) {
			logger.trace("ModifiableStringService is unavailable, JavaScriptInjector takes no action");
		}
		
		return ResponseProcessingActions.PROCEED;
	}
	
	@Override
	public boolean setup(PluginProperties props) {
		scriptUrl = props.getProperty("scriptUrl");
		bypassPattern = props.getProperty("bypassPattern");
		bypassTo = props.getProperty("bypassTo");
		additionalHTML = props.getProperty("additionalHTML");
		if(additionalHTML == null) {
			additionalHTML = "";
		}
		
		if(props.getProperty("allowOnlyFor") != null) {
			for (String uid : props.getProperty("allowOnlyFor").split(",")) {
				allowOnlyFor.add(uid.trim());
			}
		}
		
		generateResponse = props.getBoolProperty("generateResponse", false);
		
		return true;
	}
}

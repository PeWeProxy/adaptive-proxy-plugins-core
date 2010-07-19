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
import sk.fiit.rabbit.adaptiveproxy.plugins.services.injector.HtmlInjectorService.HtmlPosition;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.user.UserIdentificationService;

public class JavaScriptInjectingProcessingPlugin extends RequestAndResponseProcessingPluginAdapter {
	
	protected Logger logger = Logger.getLogger(JavaScriptInjectingProcessingPlugin.class);
	
	private String scriptUrl;
	private String bypassPattern;
	private String bypassTo;
	private String additionalHTML;
	private Set<String> allowOnlyFor = new HashSet<String>();
	private boolean generateResponse;
	private String allowedDomain;
	
	@Override
	public RequestProcessingActions processRequest(ModifiableHttpRequest request) {
		if(request.getClientRequestHeaders().getRequestURI().contains(bypassPattern)) {
			if(generateResponse) {
				return RequestProcessingActions.FINAL_RESPONSE;
			} else {
				return RequestProcessingActions.FINAL_REQUEST;
			}
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
			if(!isAllowedDomain(response.getClientRequestHeaders().getRequestURI())) {
				return ResponseProcessingActions.PROCEED;
			}
			
			HtmlInjectorService htmlInjectionService = response.getServiceHandle().getService(HtmlInjectorService.class);
			
			if(allowOnlyFor.isEmpty() || allowOnlyFor.contains(response.getServiceHandle().getService(UserIdentificationService.class).getClientIdentification())) {
				String scripts = "<script src='" + scriptUrl + "'></script>";
				htmlInjectionService.inject(additionalHTML + scripts, HtmlPosition.ON_MARK);
			}
		} catch (ServiceUnavailableException e) {
			logger.trace("HtmlInjectorService is unavailable, JavaScriptInjector takes no action");
		} catch (MalformedURLException e) {
			logger.warn("Cannot provide javascript injector service for invalid URL", e);
		}
		
		return ResponseProcessingActions.PROCEED;
	}
	
	private boolean isAllowedDomain(String urlString) throws MalformedURLException {
		if(allowedDomain == null) return true;
		URL url = new URL(urlString);
		String host = url.getHost();
		
		return host.contains(allowedDomain);
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
		allowedDomain = props.getProperty("allowedDomain");
		
		return true;
	}
}

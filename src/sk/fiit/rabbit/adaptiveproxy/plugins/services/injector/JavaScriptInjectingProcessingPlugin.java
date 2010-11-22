package sk.fiit.rabbit.adaptiveproxy.plugins.services.injector;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;

import sk.fiit.peweproxy.headers.RequestHeader;
import sk.fiit.peweproxy.headers.ResponseHeader;
import sk.fiit.peweproxy.messages.HttpMessageFactory;
import sk.fiit.peweproxy.messages.HttpRequest;
import sk.fiit.peweproxy.messages.HttpResponse;
import sk.fiit.peweproxy.messages.ModifiableHttpRequest;
import sk.fiit.peweproxy.messages.ModifiableHttpResponse;
import sk.fiit.peweproxy.plugins.PluginProperties;
import sk.fiit.peweproxy.plugins.processing.RequestProcessingPlugin;
import sk.fiit.peweproxy.plugins.processing.ResponseProcessingPlugin;
import sk.fiit.peweproxy.services.ProxyService;
import sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions.HtmlInjectorService;
import sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions.HtmlInjectorService.HtmlPosition;
import sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions.UserIdentificationService;

public class JavaScriptInjectingProcessingPlugin implements RequestProcessingPlugin, ResponseProcessingPlugin {
	
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
		if(request.getOriginalRequest().getRequestHeader().getRequestURI().contains(bypassPattern)) {
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
		
		int queryParamsIdx = proxyRequest.getRequestHeader().getRequestURI().indexOf("?");
		
		if(queryParamsIdx > -1) {
			queryParams = proxyRequest.getRequestHeader().getRequestURI().substring(queryParamsIdx);
		} else {
			queryParams = "";
		}
		
		proxyRequest.getRequestHeader().setRequestURI(bypassTo + queryParams);
		
		try {
			URL url = new URL(bypassTo);
			proxyRequest.getRequestHeader().removeField("Host");
			proxyRequest.getRequestHeader().addField("Host", url.getHost());
		} catch (MalformedURLException e) {
			logger.warn("Malformed URL", e);
		}
		
		return proxyRequest;
	}
	
	@Override
	public ResponseProcessingActions processResponse(ModifiableHttpResponse response) {
		if(response.getServicesHandle().isServiceAvailable(HtmlInjectorService.class)
				&& response.getServicesHandle().isServiceAvailable(UserIdentificationService.class)) {
			try {
				if(!isAllowedDomain(response.getRequest().getRequestHeader().getRequestURI())) {
					return ResponseProcessingActions.PROCEED;
				}
				
				if(allowOnlyFor.isEmpty() || allowOnlyFor.contains(response.getServicesHandle().getService(UserIdentificationService.class).getClientIdentification())) {
					HtmlInjectorService htmlInjectionService = response.getServicesHandle().getService(HtmlInjectorService.class);
					String scripts = "<script src='" + scriptUrl + "'></script>";
					htmlInjectionService.inject(additionalHTML + scripts, HtmlPosition.ON_MARK);
				}
			} catch (MalformedURLException e) {
				logger.warn("Cannot provide javascript injector service for invalid URL", e);
			}
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
	public boolean start(PluginProperties props) {
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

	@Override
	public void desiredRequestServices(
			Set<Class<? extends ProxyService>> desiredServices,
			RequestHeader clientRQHeader) {
		//no-dependencies
	}

	@Override
	public void desiredResponseServices(
			Set<Class<? extends ProxyService>> desiredServices,
			ResponseHeader webRPHeader) {
		desiredServices.add(UserIdentificationService.class);
		desiredServices.add(HtmlInjectorService.class);
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
		return null;
	}

	@Override
	public HttpResponse getResponse(ModifiableHttpRequest request,
			HttpMessageFactory messageFactory) {
		return messageFactory.constructHttpResponse(null, "text/html");
	}

	@Override
	public void processTransferedResponse(HttpResponse response) {
	}

	@Override
	public void processTransferedRequest(HttpRequest request) {
	}
}

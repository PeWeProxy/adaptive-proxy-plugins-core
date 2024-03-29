package sk.fiit.rabbit.adaptiveproxy.plugins.services.injector;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions.UserIdentificationService;
import sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions.UserPreferencesProviderService;

public class JavaScriptInjectingProcessingPlugin implements RequestProcessingPlugin, ResponseProcessingPlugin {
	
	protected Logger logger = Logger.getLogger(JavaScriptInjectingProcessingPlugin.class);
	
	private String scriptUrl;
	private String bypassPattern;
	private String bypassTo;
	private String additionalHTML;
	private boolean loadAsynchronously;
	private Set<String> allowOnlyFor = new HashSet<String>();
	private boolean generateResponse;
	private String allowedDomain;
	private String lastModifiedAppendix;
	private String scriptInjectingPosition;
	private String preferenceNamespace;
	
	@Override
	public RequestProcessingActions processRequest(ModifiableHttpRequest request) {
		if(bypassPattern != null && request.getOriginalRequest().getRequestHeader().getRequestURI().contains(bypassPattern)) {
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
	
	private String lastModifiedAppendix(){
		if ("".equals(scriptUrl) || scriptUrl == null) {
			return "";
		}
		String lastModified = "";
		if (scriptUrl.contains("/FileSender/public/")){
			Pattern pattern = Pattern.compile("^.*/FileSender/public/(.*)$");
			Matcher matcher = pattern.matcher(scriptUrl);
			if (matcher.matches()) {
				try {
					File script = new File("./htdocs/public/"+matcher.group(1));
					lastModified = "?" + script.lastModified();
				} catch (NullPointerException e){
					lastModified = "";
				}
			}
		}
		return lastModified;
	}
	
	@Override
	public ResponseProcessingActions processResponse(ModifiableHttpResponse response) {
		if(response.getServicesHandle().isServiceAvailable(HtmlInjectorService.class)
				&& response.getServicesHandle().isServiceAvailable(UserIdentificationService.class)
				&& response.getServicesHandle().isServiceAvailable(UserPreferencesProviderService.class)) {
			try {
				if(!isAllowedDomain(response.getRequest().getRequestHeader().getRequestURI())) {
					return ResponseProcessingActions.PROCEED;
				}
				
				String userId = response.getServicesHandle().getService(UserIdentificationService.class).getClientIdentification();
				String activity = response.getServicesHandle().getService(UserPreferencesProviderService.class).getProperty("activity", userId, preferenceNamespace);
				if ((activity != null) && (activity.toLowerCase().equals("false"))) {
					return ResponseProcessingActions.PROCEED;
				}
				
				if(allowOnlyFor.isEmpty() || allowOnlyFor.contains(userId)) {
					HtmlInjectorService htmlInjectionService = response.getServicesHandle().getService(HtmlInjectorService.class);
					String scripts = "";
					if (loadAsynchronously){
						scripts =	"<script type=\"text/javascript\">\n" +
									"//<![CDATA[\n" +
									"(function() {\n" +
									"	var s = document.createElement(\"script\");\n" + 
									"	s.type = \"text/javascript\";\n" +
									"	s.async = true;\n" +
									"	s.src = \""+scriptUrl + lastModifiedAppendix +"\";\n" +
									"	var x = document.getElementsByTagName(\"script\")[0];\n" +
									"	x.parentNode.insertBefore(s, x);\n" +
									"})();\n" +
									"//]]>\n" +
									"</script>\n";
					} else {
						scripts = "<script src='" + scriptUrl + lastModifiedAppendix + "'></script>";
					}
					htmlInjectionService.injectAfter(getScriptInjectingPosition(), additionalHTML + scripts);
				}
			} catch (MalformedURLException e) {
				logger.warn("Cannot provide javascript injector service for invalid URL", e);
			}
		}
		
		return ResponseProcessingActions.PROCEED;
	}
	
	protected String getScriptInjectingPosition() {
		return "<!-- __ap_scripts__ -->";
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
		additionalHTML = props.getProperty("additionalHTML", "");
		preferenceNamespace = props.getProperty("preferenceNamespace", "");
		
		if(props.getProperty("allowOnlyFor") != null) {
			for (String uid : props.getProperty("allowOnlyFor").split(",")) {
				allowOnlyFor.add(uid.trim());
			}
		}

		generateResponse = props.getBoolProperty("generateResponse", false);
		loadAsynchronously = props.getBoolProperty("loadAsynchronously", true);
		allowedDomain = props.getProperty("allowedDomain");
		
		lastModifiedAppendix = lastModifiedAppendix();
		
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

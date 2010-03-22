package sk.fiit.rabbit.adaptiveproxy.plugins.services.logging;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;

import sk.fiit.rabbit.adaptiveproxy.plugins.PluginProperties;
import sk.fiit.rabbit.adaptiveproxy.plugins.helpers.RequestProcessingPluginAdapter;
import sk.fiit.rabbit.adaptiveproxy.plugins.messages.ModifiableHttpRequest;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.ServiceUnavailableException;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.injector.JavaScript;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.injector.JavaScriptInjectorService;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.user.UserIdentificationService;

public class JavaScriptInjectingPlugin extends RequestProcessingPluginAdapter {

	Logger logger = Logger.getLogger(JavaScriptInjectingPlugin.class);
	
	private String scriptUrl;
	private String bypassPattern;
	private String bypassTo;
	private String additionalHTML;
	private Set<String> allowOnlyFor = new HashSet<String>();

	public JavaScriptInjectingPlugin() {
		super();
	}

	@Override
	public RequestProcessingActions processRequest(ModifiableHttpRequest request) {
		try {
			UserIdentificationService userIdentification = request.getServiceHandle().getService(UserIdentificationService.class);
	
			if(allowOnlyFor.isEmpty() || allowOnlyFor.contains(userIdentification.getClientIdentification())) {
				JavaScriptInjectorService injector = request.getServiceHandle().getService(JavaScriptInjectorService.class);
				logger.debug("Registering javascript " + scriptUrl + " for " + request.getClientRequestHeaders().getRequestURI());
				injector.registerJavascript(request, new JavaScript(scriptUrl, bypassPattern, bypassTo, additionalHTML));
			}
		} catch (ServiceUnavailableException e) {
			logger.warn("Service unavailable", e);
		}
	
		return RequestProcessingActions.PROCEED;
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
		
		return true;
	}

}
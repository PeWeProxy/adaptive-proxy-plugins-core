package sk.fiit.rabbit.adaptiveproxy.plugins.services.logging;

import org.apache.log4j.Logger;

import sk.fiit.rabbit.adaptiveproxy.plugins.PluginProperties;
import sk.fiit.rabbit.adaptiveproxy.plugins.helpers.ResponseProcessingPluginAdapter;
import sk.fiit.rabbit.adaptiveproxy.plugins.messages.ModifiableHttpResponse;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.ServiceUnavailableException;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.injector.JavaScript;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.injector.JavaScriptInjectorService;

public class JavaScriptInjectingPlugin extends ResponseProcessingPluginAdapter {

	Logger logger = Logger.getLogger(JavaScriptInjectingPlugin.class);
	
	private String scriptUrl;
	private String bypassPattern;
	private String bypassTo;

	public JavaScriptInjectingPlugin() {
		super();
	}

	@Override
	public ResponseProcessingActions processResponse(ModifiableHttpResponse response) {
		try {
			JavaScriptInjectorService injector = response.getServiceHandle().getService(JavaScriptInjectorService.class);
			injector.registerJavascript(new JavaScript(scriptUrl, bypassPattern, bypassTo));
		} catch (ServiceUnavailableException e) {
			logger.warn("JavaScriptInjector unavailable");
		}
	
		return ResponseProcessingActions.PROCEED;
	}
	
	@Override
	public boolean setup(PluginProperties props) {
		scriptUrl = props.getProperty("scriptUrl");
		bypassPattern = props.getProperty("bypassPattern");
		bypassTo = props.getProperty("bypassTo");
		
		return true;
	}

}
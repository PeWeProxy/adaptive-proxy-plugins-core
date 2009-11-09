package sk.fiit.rabbit.adaptiveproxy.plugins.services.logging;

import org.apache.log4j.Logger;

import sk.fiit.rabbit.adaptiveproxy.plugins.helpers.ResponseProcessingPluginAdapter;
import sk.fiit.rabbit.adaptiveproxy.plugins.messages.ModifiableHttpResponse;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.ServiceUnavailableException;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.injector.JavaScript;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.injector.JavaScriptInjectorService;

public class ActivityUpdaterPlugin extends ResponseProcessingPluginAdapter {
	
	Logger logger = Logger.getLogger(KeywordsDisplayPlugin.class);
	
	@Override
	public ResponseProcessingActions processResponse(ModifiableHttpResponse response) {
		try {
			JavaScriptInjectorService injector = response.getServiceHandle().getService(JavaScriptInjectorService.class);
			injector.registerJavascript(new JavaScript("http://127.0.0.1:3000/javascripts/timelog.js", "activity/update", "http://127.0.0.1:3000/activity/update"));
		} catch (ServiceUnavailableException e) {
			logger.warn("JavaScriptInjector unavailable");
		}

		return ResponseProcessingActions.PROCEED;
	}
	
	
}

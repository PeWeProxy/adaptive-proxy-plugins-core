package sk.fiit.rabbit.adaptiveproxy.plugins.services.injector;

import java.util.Set;

import org.apache.log4j.Logger;

import sk.fiit.peweproxy.headers.ResponseHeader;
import sk.fiit.peweproxy.messages.HttpMessageFactory;
import sk.fiit.peweproxy.messages.HttpResponse;
import sk.fiit.peweproxy.messages.ModifiableHttpResponse;
import sk.fiit.peweproxy.plugins.PluginProperties;
import sk.fiit.peweproxy.plugins.processing.ResponseProcessingPlugin;
import sk.fiit.peweproxy.services.ProxyService;
import sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions.HtmlInjectorService;
import sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions.HtmlInjectorService.HtmlPosition;

public class SetupJavaScriptEnvironmentProcessingPlugin implements ResponseProcessingPlugin {
	
	private String jQueryPath;
	private String peweproxyPath;
	protected Logger logger = Logger.getLogger(SetupJavaScriptEnvironmentProcessingPlugin.class);
	
	@Override
	public ResponseProcessingActions processResponse(ModifiableHttpResponse response) {
	    if(response.getServicesHandle().isServiceAvailable(HtmlInjectorService.class)) {
		
		HtmlInjectorService htmlInjectionService = response.getServicesHandle().getService(HtmlInjectorService.class);
		
		String scripts = "" +
                    /*"<script type='text/javascript'>" +
                      "var __ap_callback_functions = new Array();\n" +
                      "function __ap_register_callback(function_to_register){\n" +
                      "	if (typeof(__peweproxy_uid) == 'undefined' || __peweproxy_uid == null) {\n" +
                      "		__ap_callback_functions.push(function_to_register);\n"+
                      "	} else {\n"+
                      "		function_to_register.call()\n" +
					  "	}\n" +
					  "}\n"+
					  "function __ap_fire_callback(){\n" +
					  "	for (call in __ap_callback_functions) {\n" +
					  "		__ap_callback_functions[call].call();\n" +
					  "	}\n" +
					  "}\n" +
                      "</script>" +*/
                      //"<script src='" + jQueryPath + "'></script>" +
                      "<script src='" + peweproxyPath + "'></script>" +
                      "<!-- __ap_scripts__ -->";

		htmlInjectionService.inject(scripts, HtmlPosition.START_OF_BODY);
	    }
		
	    return ResponseProcessingActions.PROCEED;
	}

	@Override
	public boolean start(PluginProperties props) {
		jQueryPath = props.getProperty("jQueryPath");
		peweproxyPath = props.getProperty("peweproxyPath");
		return true;
	}

	@Override
	public void desiredResponseServices(
			Set<Class<? extends ProxyService>> desiredServices,
			ResponseHeader webRPHeader) {
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
	public void processTransferedResponse(HttpResponse response) {
	}

}

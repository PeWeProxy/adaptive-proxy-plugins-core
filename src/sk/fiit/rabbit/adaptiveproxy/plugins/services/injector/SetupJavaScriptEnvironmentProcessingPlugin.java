package sk.fiit.rabbit.adaptiveproxy.plugins.services.injector;

import org.apache.log4j.Logger;

import sk.fiit.rabbit.adaptiveproxy.plugins.PluginProperties;
import sk.fiit.rabbit.adaptiveproxy.plugins.helpers.ResponseProcessingPluginAdapter;
import sk.fiit.rabbit.adaptiveproxy.plugins.messages.ModifiableHttpResponse;
import sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions.ClearTextExtractionService;
import sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions.HtmlInjectorService;
import sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions.HtmlInjectorService.HtmlPosition;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.ServiceUnavailableException;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.common.Checksum;

public class SetupJavaScriptEnvironmentProcessingPlugin extends ResponseProcessingPluginAdapter {
	
	private static final Logger logger = Logger.getLogger(SetupJavaScriptEnvironmentProcessingPlugin.class);
	
	private String jQueryPath;
	
	@Override
	public ResponseProcessingActions processResponse(ModifiableHttpResponse response) {
		ClearTextExtractionService clearTextService;
		try {
			clearTextService = response.getServiceHandle().getService(ClearTextExtractionService.class);
		} catch (ServiceUnavailableException e) {
			logger.trace("ClearTextService unavailable, SetupJavaScriptEnvironmentProcessingPlugin takes no action");
			return ResponseProcessingActions.PROCEED;
		}
		
		try {
			HtmlInjectorService htmlInjectionService = response.getServiceHandle().getService(HtmlInjectorService.class);
			
			String scripts = "" +
                             "<script type='text/javascript'>" +
                               "_ap_checksum = '" + Checksum.md5(clearTextService.getCleartext()) + "'" +
                              "</script>" +
                              "<script src='" + jQueryPath + "'></script>" +
                              "<!-- __ap_scripts__ -->";
			htmlInjectionService.inject(scripts, HtmlPosition.START_OF_BODY);
			
		} catch (ServiceUnavailableException e) {
			logger.trace("HtmlInjectorService is unavailable, JavaScriptInjector takes no action");
		}
		
		return ResponseProcessingActions.PROCEED;
	}
	
	@Override
	public boolean setup(PluginProperties props) {
		jQueryPath = props.getProperty("jQueryPath");
		return true;
	}

}

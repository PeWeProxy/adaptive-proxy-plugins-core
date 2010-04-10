package sk.fiit.rabbit.adaptiveproxy.plugins.services.injector;

import org.apache.log4j.Logger;

import sk.fiit.rabbit.adaptiveproxy.plugins.PluginProperties;
import sk.fiit.rabbit.adaptiveproxy.plugins.helpers.ResponseProcessingPluginAdapter;
import sk.fiit.rabbit.adaptiveproxy.plugins.messages.ModifiableHttpResponse;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.ServiceUnavailableException;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.cleartext.ClearTextExtractionService;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.common.Checksum;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.content.ModifiableStringService;

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
			ModifiableStringService ms = response.getServiceHandle().getService(ModifiableStringService.class);
			
			StringBuilder sb = ms.getModifiableContent();
			
			String html = sb.toString();

			int bodyEndIDx = html.toLowerCase().indexOf("</body>");
			if(bodyEndIDx < 0) {
				logger.debug("No </body> : " + response.getProxyRequestHeaders().getRequestURI());
				return ResponseProcessingActions.PROCEED;
			}
			
			String scripts = "" +
                             "<script type='text/javascript'>" +
                               "_ap_checksum = '" + Checksum.md5(clearTextService.getCleartext()) + "'" +
                              "</script>" +
                              "<script src='" + jQueryPath + "'></script>";
			
			
			sb.insert(bodyEndIDx, scripts);
		} catch (ServiceUnavailableException e) {
			logger.trace("ModifiableStringService is unavailable, JavaScriptInjector takes no action");
		}
		
		return ResponseProcessingActions.PROCEED;
	}
	
	@Override
	public boolean setup(PluginProperties props) {
		jQueryPath = props.getProperty("jQueryPath");
		return true;
	}

}

package sk.fiit.rabbit.adaptiveproxy.plugins.services.injector;

import java.util.Set;
import java.util.UUID;

import sk.fiit.peweproxy.headers.ResponseHeader;
import sk.fiit.peweproxy.messages.HttpMessageFactory;
import sk.fiit.peweproxy.messages.HttpResponse;
import sk.fiit.peweproxy.messages.ModifiableHttpResponse;
import sk.fiit.peweproxy.plugins.PluginProperties;
import sk.fiit.peweproxy.plugins.processing.ResponseProcessingPlugin;
import sk.fiit.peweproxy.services.ProxyService;
import sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions.ClearTextExtractionService;
import sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions.HtmlInjectorService;
import sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions.HtmlInjectorService.HtmlPosition;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.common.Checksum;

public class SetupJavaScriptEnvironmentProcessingPlugin implements ResponseProcessingPlugin {
	
	private String jQueryPath;
	
	@Override
	public ResponseProcessingActions processResponse(ModifiableHttpResponse response) {
		if(response.getServicesHandle().isServiceAvailable(HtmlInjectorService.class)
				&& response.getServicesHandle().isServiceAvailable(ClearTextExtractionService.class)) {
		
			ClearTextExtractionService clearTextService = response.getServicesHandle().getService(ClearTextExtractionService.class);
			HtmlInjectorService htmlInjectionService = response.getServicesHandle().getService(HtmlInjectorService.class);

            String scripts = "" +
                            "<script type='text/javascript'>" +
                              "_ap_checksum = '" + Checksum.md5(clearTextService.getCleartext()) + "';" +
                              " page_uid = '" + UUID.randomUUID().toString() + "';" +
                              "</script>" +
                              "<script src='" + jQueryPath + "'></script>" +
                              "<!-- __ap_scripts__ -->";

            htmlInjectionService.inject(scripts, HtmlPosition.START_OF_BODY);
		}
		
		return ResponseProcessingActions.PROCEED;
	}
	
	@Override
	public boolean start(PluginProperties props) {
		jQueryPath = props.getProperty("jQueryPath");
		return true;
	}

	@Override
	public void desiredResponseServices(
			Set<Class<? extends ProxyService>> desiredServices,
			ResponseHeader webRPHeader) {
		desiredServices.add(ClearTextExtractionService.class);
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

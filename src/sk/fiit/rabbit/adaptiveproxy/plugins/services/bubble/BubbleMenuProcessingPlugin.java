package sk.fiit.rabbit.adaptiveproxy.plugins.services.bubble;

import sk.fiit.peweproxy.messages.ModifiableHttpResponse;
import sk.fiit.peweproxy.plugins.PluginProperties;
import sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions.HtmlInjectorService;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.injector.JavaScriptInjectingProcessingPlugin;

public class BubbleMenuProcessingPlugin extends JavaScriptInjectingProcessingPlugin {
	private String buttonHTML;
	private String windowHTML;

	@Override
	public ResponseProcessingActions processResponse(ModifiableHttpResponse response) {
		if(response.getServicesHandle().isServiceAvailable(HtmlInjectorService.class)) {
			HtmlInjectorService htmlInjector = response.getServicesHandle().getService(HtmlInjectorService.class);
			htmlInjector.injectAfter("<!-- bubble menu -->", "<td>" + buttonHTML + "</td>");
			htmlInjector.injectAfter("<!-- bubble windows -->", windowHTML);
		}
		return super.processResponse(response);
	}

	@Override
	public boolean start(PluginProperties props) {
		buttonHTML = props.getProperty("buttonHTML", "");
		windowHTML = props.getProperty("windowHTML", "");
		return super.start(props);
	}
}

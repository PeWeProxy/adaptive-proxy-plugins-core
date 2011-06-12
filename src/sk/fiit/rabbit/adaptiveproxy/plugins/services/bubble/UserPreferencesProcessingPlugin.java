package sk.fiit.rabbit.adaptiveproxy.plugins.services.bubble;

import java.util.Map;

import sk.fiit.peweproxy.messages.HttpMessageFactory;
import sk.fiit.peweproxy.messages.HttpResponse;
import sk.fiit.peweproxy.messages.ModifiableHttpRequest;
import sk.fiit.peweproxy.messages.ModifiableHttpResponse;
import sk.fiit.peweproxy.services.content.ModifiableStringService;
import sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions.RequestDataParserService;
import sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions.UserPreferencesProviderService;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.injector.JavaScriptInjectingProcessingPlugin;

public class UserPreferencesProcessingPlugin extends JavaScriptInjectingProcessingPlugin {
	@Override
	public HttpResponse getResponse(ModifiableHttpRequest request, HttpMessageFactory messageFactory) {
		String content = "FAIL";

		if((request.getServicesHandle().isServiceAvailable(UserPreferencesProviderService.class)) && (request.getServicesHandle().isServiceAvailable(RequestDataParserService.class))) {
			Map<String, String> postData = request.getServicesHandle().getService(RequestDataParserService.class).getDataFromPOST();
			if (request.getRequestHeader().getRequestURI().contains("action=update_preference")) {
				request.getServicesHandle().getService(UserPreferencesProviderService.class).setProperty(postData.get("preference_name"), postData.get("new_value"), postData.get("uid"), postData.get("preference_namespace"));
				content = "OK";
			}
			if (request.getRequestHeader().getRequestURI().contains("action=get_preference")) {
				request.getServicesHandle().getService(UserPreferencesProviderService.class).getProperty(postData.get("preference_name"), postData.get("uid"), postData.get("preference_namespace"));
				content = "OK";
			}
		}
		
		ModifiableHttpResponse httpResponse = messageFactory.constructHttpResponse(null, "text/html");
		ModifiableStringService stringService = httpResponse.getServicesHandle().getService(ModifiableStringService.class);
		stringService.setContent(content);
		
		return httpResponse;
	}
}

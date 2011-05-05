package sk.fiit.rabbit.adaptiveproxy.plugins.services.logging;

import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import sk.fiit.peweproxy.headers.RequestHeader;
import sk.fiit.peweproxy.messages.HttpMessageFactory;
import sk.fiit.peweproxy.messages.HttpResponse;
import sk.fiit.peweproxy.messages.ModifiableHttpRequest;
import sk.fiit.peweproxy.services.ProxyService;
import sk.fiit.peweproxy.services.ServiceUnavailableException;
import sk.fiit.peweproxy.services.content.ModifiableStringService;
import sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions.LoggingBackendFailure;
import sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions.LoggingBackendService;
import sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions.PostDataParserService;
import sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions.UserIdentificationService;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.injector.JavaScriptInjectingProcessingPlugin;

public class ActivityLoggingProcessingPlugin extends JavaScriptInjectingProcessingPlugin
{
	protected Logger logger = Logger.getLogger(ActivityLoggingProcessingPlugin.class);
	
	@Override
	public HttpResponse getResponse(ModifiableHttpRequest request, HttpMessageFactory messageFactory) {
		if(request.getServicesHandle().isServiceAvailable(PostDataParserService.class)
				&& request.getServicesHandle().isServiceAvailable(LoggingBackendService.class)) {
			
			Map<String, String> post = request.getServicesHandle().getService(PostDataParserService.class).getPostData();
			LoggingBackendService loggingBackend = request.getServicesHandle().getService(LoggingBackendService.class);
			String userId = request.getServicesHandle().getService(UserIdentificationService.class).getClientIdentification();
			
			
			try {
				do {
					try {
						loggingBackend.logActivity(userId, post.get("access_guid"), post.get("period"), post.get("scrolls"), post.get("copies"));
					} catch (LoggingBackendFailure e) {
						logger.error("Could not log activity to backend " + loggingBackend.getClass().getName(), e);
					}
					
					loggingBackend = request.getServicesHandle().getNextService(loggingBackend);
				} while(true);
			} catch(ServiceUnavailableException e) {
				// no more logging backends to write to
			}
		}
		
		return messageFactory.constructHttpResponse(null, "text/html");
	}
	
	//FIXME: toto je docasny hack kvoli late processingu, tato metoda tu inak nema byt
	@Override
	public void desiredRequestServices(
			Set<Class<? extends ProxyService>> desiredServices,
			RequestHeader webRQHeader) {
		super.desiredRequestServices(desiredServices, webRQHeader);
		desiredServices.add(ModifiableStringService.class);
		desiredServices.add(PostDataParserService.class);
		desiredServices.add(LoggingBackendService.class);
	}
}
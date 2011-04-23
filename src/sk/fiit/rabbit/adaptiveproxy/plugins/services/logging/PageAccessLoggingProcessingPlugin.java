package sk.fiit.rabbit.adaptiveproxy.plugins.services.logging;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

import org.apache.log4j.Logger;

import sk.fiit.peweproxy.headers.ResponseHeader;
import sk.fiit.peweproxy.messages.HttpMessageFactory;
import sk.fiit.peweproxy.messages.HttpResponse;
import sk.fiit.peweproxy.messages.ModifiableHttpResponse;
import sk.fiit.peweproxy.plugins.PluginProperties;
import sk.fiit.peweproxy.plugins.processing.ResponseProcessingPlugin;
import sk.fiit.peweproxy.services.ProxyService;
import sk.fiit.peweproxy.services.content.StringContentService;
import sk.fiit.rabbit.adaptiveproxy.plugins.common.JavaScript;
import sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions.HtmlInjectorService;
import sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions.UserIdentificationService;
import sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions.HtmlInjectorService.HtmlPosition;
import sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions.LoggingBackendService;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.logging.backends.LoggingBackendFailure;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.user.UIDFromCookieProcessingPlugin;

/**
 * Implementation of page and access logging.
 * In order to send the response as quickly as possible, the following process is used:
 * 
 * <ol>
 *   <li> Calculate unique identifier of the access (using a GUID scheme). This is fast. </li>
 *   <li> Insert the GUID into the HTML code. This is part of the real-time processing. </li>
 *   <li> After the message has been sent to the client, the heavy lifting proceeds. </li>
 *   <li> Logs of pages are scanned to see if a new page record needs to be created.
 *        New page record is created if the record with the URL does not exist yet, or
 *        it exists but the content at the URL has changed. The content change is detected by
 *        calculating a checksum over a cleartext extracted from the HTML code. </li>
 *   <li> New access log is created. At this time, we may not know user's identifier, so it is
 *        left blank. The access log is linked with the page record.</li>
 *   <li> The user identifier is later filled in by the activity logging plugin </li>
 * </ol>
 * 
 * <em>
 * The user identifier may not be known at this point. This is because of the cookie mechanism that is
 * employed in the identification process. When the particular domain is visited, the cookie is inserted
 * by JavaScript only after the first page has been processed by client browser. 
 * See {@link UIDFromCookieProcessingPlugin} for details.
 * </em>
 */
public class PageAccessLoggingProcessingPlugin implements ResponseProcessingPlugin {
	
	private static final Logger logger = Logger.getLogger(PageAccessLoggingProcessingPlugin.class);
	
	/**
	 * Processing plugins are instantiated only once for the whole proxy lifecycle. We need a way to introduce state
	 * to share GUID between realtime and late processing
	 */
	private Map<HttpResponse, String> accessGUIDs = new WeakHashMap<HttpResponse, String>();

	@Override
	public void desiredResponseServices(Set<Class<? extends ProxyService>> desiredServices, ResponseHeader response) {
		desiredServices.add(HtmlInjectorService.class);
	}

	@Override
	public boolean supportsReconfigure(PluginProperties newProps) {
		return true;
	}

	@Override
	public boolean start(PluginProperties props) {
		return true;
	}

	@Override
	public void stop() {
	}
	
	/**
	* Calculate a GUID for this access and insert it into the response HTML.
	* Page and access are logged after the response has been sent to the client
	* in the late processing phase
	* 
	* @see #processTransferedResponse
	*/
	@Override
	public ResponseProcessingActions processResponse(ModifiableHttpResponse response) {
		if(response.getServicesHandle().isServiceAvailable(HtmlInjectorService.class)) {
			String accessGUID = UUID.randomUUID().toString();
			accessGUIDs.put(response, accessGUID);

			HtmlInjectorService htmlInjector = response.getServicesHandle().getService(HtmlInjectorService.class);
			String script = String.format("var __access_guid='%s'", accessGUID);
			htmlInjector.inject(JavaScript.wrap(script), HtmlPosition.START_OF_BODY);
		}
		return ResponseProcessingActions.PROCEED;
	}

	@Override
	public HttpResponse getNewResponse(ModifiableHttpResponse response, HttpMessageFactory messageFactory) {
		return null;
	}

	/**
	 * Create page log if necessary (no page with this URL exists, or a new version of the page exists).
	 * Create access log. Note that the user identification may not be known at this time and may be left blank.
	 */
	@Override
	public void processTransferedResponse(HttpResponse response) {
		String accessGUID = accessGUIDs.get(response);
		
		if(accessGUID != null && response.getServicesHandle().isServiceAvailable(LoggingBackendService.class)
				&& response.getServicesHandle().isServiceAvailable(StringContentService.class)
				&& response.getServicesHandle().isServiceAvailable(UserIdentificationService.class)) {
			
			LoggingBackendService loggingBackend = response.getServicesHandle().getService(LoggingBackendService.class);
			String userId = response.getServicesHandle().getService(UserIdentificationService.class).getClientIdentification();
			String uri = response.getRequest().getRequestHeader().getRequestURI();
			String content = response.getServicesHandle().getService(StringContentService.class).getContent();
			
			try {
				String ip = response.getRequest().getClientSocketAddress().getAddress().toString();
				String referrer = response.getRequest().getRequestHeader().getField("Referer");
				
				loggingBackend.logPageAccess(accessGUID, userId, uri, content, referrer, ip);
				
			} catch (LoggingBackendFailure e) {
				logger.error("Could not log to backend " + loggingBackend.getClass().getName(), e);
			}
		}
	}

}

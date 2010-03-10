package sk.fiit.rabbit.adaptiveproxy.plugins.services.injector;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import sk.fiit.rabbit.adaptiveproxy.plugins.PluginProperties;
import sk.fiit.rabbit.adaptiveproxy.plugins.headers.ResponseHeaders;
import sk.fiit.rabbit.adaptiveproxy.plugins.helpers.RequestAndResponseProcessingPluginAdapter;
import sk.fiit.rabbit.adaptiveproxy.plugins.helpers.RequestServiceProviderAdapter;
import sk.fiit.rabbit.adaptiveproxy.plugins.messages.HttpMessageFactory;
import sk.fiit.rabbit.adaptiveproxy.plugins.messages.HttpRequest;
import sk.fiit.rabbit.adaptiveproxy.plugins.messages.ModifiableHttpRequest;
import sk.fiit.rabbit.adaptiveproxy.plugins.messages.ModifiableHttpResponse;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.ProxyService;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.RequestServicePlugin;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.RequestServiceProvider;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.ServiceUnavailableException;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.cleartext.ClearTextExtractionService;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.common.Checksum;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.content.ModifiableStringService;

public class SimpleJavaScriptInjectorService extends RequestAndResponseProcessingPluginAdapter 
 	implements RequestServicePlugin {
	
	Logger logger = Logger.getLogger(SimpleJavaScriptInjectorService.class);
	
	Set<JavaScript> javaScripts = new HashSet<JavaScript>();
	
	String currentBypass;

	private String javascriptServer;
	
	private class SimpleJavaScriptInjectorProvider extends RequestServiceProviderAdapter implements JavaScriptInjectorService {

		@Override
		public Class<? extends ProxyService> getServiceClass() {
			return JavaScriptInjectorService.class;
		}

		@Override
		public void registerJavascript(JavaScript js) {
			javaScripts.add(js);
		}
		
	}
	
	@Override
	public HttpRequest getNewRequest(ModifiableHttpRequest proxyRequest, HttpMessageFactory messageFactory) {
		String queryParams;
		
		int queryParamsIdx = proxyRequest.getClientRequestHeaders().getRequestURI().indexOf("?");
		
		if(queryParamsIdx > -1) {
			queryParams = proxyRequest.getClientRequestHeaders().getRequestURI().substring(queryParamsIdx);
		} else {
			queryParams = "";
		}
		
		logger.debug("current bypass is: " + currentBypass);
		proxyRequest.getProxyRequestHeaders().setRequestURI(currentBypass + queryParams);
		return messageFactory.constructHttpRequest(proxyRequest, proxyRequest.getProxyRequestHeaders(), true);
	}
	
	@Override
	public RequestProcessingActions processRequest(ModifiableHttpRequest request) {
		String requestURI = request.getClientRequestHeaders().getRequestURI();
		logger.debug("processing request: " + request.getClientRequestHeaders().getRequestURI());
		logger.debug("available javascripts: " + javaScripts);
		for (JavaScript js : javaScripts) {
			if(requestURI.contains(js.byassPattern)) {
				logger.debug("bypass MATCHED with " + js);
				currentBypass = js.bypassTo;
				return RequestProcessingActions.FINAL_REQUEST;
			} else {
				logger.debug("bypass unmatched with " + js);
			}
		}
		
		return RequestProcessingActions.PROCEED;
	}
	
	@Override
	public ResponseProcessingActions processResponse(ModifiableHttpResponse response) {
		ClearTextExtractionService clearTextService;
		try {
			clearTextService = response.getServiceHandle().getService(ClearTextExtractionService.class);
		} catch (ServiceUnavailableException e) {
			logger.trace("ClearTextService unavailable, JavaScriptInjector takes no action");
			return ResponseProcessingActions.PROCEED;
		}
		
		/*
		if(response.getClientRequestHeaders().getRequestURI().startsWith(kwServiceRoot)) {
			return ResponseProcessingActions.PROCEED;
		}
		*/
		
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
                              "<script src='" + javascriptServer + "javascripts/jquery-1.3.2.min.js'></script>";
			
			String additionalHTML = "";
			
			for (JavaScript js : javaScripts) {
				additionalHTML += js.additionalHTML;
				scripts += "<script src='" + js.script + "'></script>";
			}
			
			sb.insert(bodyEndIDx, additionalHTML + scripts);
		} catch (ServiceUnavailableException e) {
			logger.trace("ModifiableStringService is unavailable, JavaScriptInjector takes no action");
		}
		
		return ResponseProcessingActions.PROCEED;
	}
	
	@Override
	public boolean wantResponseContent(ResponseHeaders webRPHeaders) {
		return true;
	}

	@Override
	public List<RequestServiceProvider> provideRequestServices(HttpRequest response) {
		List<RequestServiceProvider> services = new LinkedList<RequestServiceProvider>();
		services.add(new SimpleJavaScriptInjectorProvider());
		return services;
	}

	@Override
	public Set<Class<? extends ProxyService>> getDependencies() {
		Set<Class<? extends ProxyService>> services = new HashSet<Class<? extends ProxyService>>();
		services.add(ClearTextExtractionService.class);
		services.add(ModifiableStringService.class);
		return services;
	}

	@Override
	public Set<Class<? extends ProxyService>> getProvidedServices() {
		Set<Class<? extends ProxyService>> services = new HashSet<Class<? extends ProxyService>>();
		services.add(JavaScriptInjectorService.class);
		return services;
	}
	
	@Override
	public boolean setup(PluginProperties props) {
		javascriptServer = props.getProperty("javascriptServer");
		return true;
	}

}

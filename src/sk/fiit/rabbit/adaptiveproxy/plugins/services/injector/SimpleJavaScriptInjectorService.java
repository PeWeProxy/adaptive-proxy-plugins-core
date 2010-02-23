package sk.fiit.rabbit.adaptiveproxy.plugins.services.injector;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import sk.fiit.rabbit.adaptiveproxy.plugins.PluginProperties;
import sk.fiit.rabbit.adaptiveproxy.plugins.headers.ResponseHeaders;
import sk.fiit.rabbit.adaptiveproxy.plugins.helpers.RequestAndResponseProcessingPluginAdapter;
import sk.fiit.rabbit.adaptiveproxy.plugins.helpers.ResponseServiceProviderAdapter;
import sk.fiit.rabbit.adaptiveproxy.plugins.messages.HttpMessageFactory;
import sk.fiit.rabbit.adaptiveproxy.plugins.messages.HttpRequest;
import sk.fiit.rabbit.adaptiveproxy.plugins.messages.HttpResponse;
import sk.fiit.rabbit.adaptiveproxy.plugins.messages.ModifiableHttpRequest;
import sk.fiit.rabbit.adaptiveproxy.plugins.messages.ModifiableHttpResponse;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.ProxyService;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.ResponseServicePlugin;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.ResponseServiceProvider;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.ServiceUnavailableException;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.cleartext.ClearTextExtractionService;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.common.Checksum;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.content.ModifiableStringService;

public class SimpleJavaScriptInjectorService extends RequestAndResponseProcessingPluginAdapter 
 	implements ResponseServicePlugin {
	
	Logger logger = Logger.getLogger(SimpleJavaScriptInjectorService.class);
	
	Set<JavaScript> javaScripts = new HashSet<JavaScript>();
	
	String currentBypass;

	private String javascriptServer;
	
	private class SimpleJavaScriptInjectorProvider extends ResponseServiceProviderAdapter implements JavaScriptInjectorService {

		@Override
		public Class<? extends ProxyService> getServiceClass() {
			return JavaScriptInjectorService.class;
		}

		@Override
		public void registerJavascript(JavaScript js) {
			logger.trace("Registering javascript " + js.script);
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
		
		proxyRequest.getProxyRequestHeaders().setRequestURI(currentBypass + queryParams);
		return messageFactory.constructHttpRequest(proxyRequest, proxyRequest.getProxyRequestHeaders(), true);
	}
	
	@Override
	public RequestProcessingActions processRequest(ModifiableHttpRequest request) {
		String requestURI = request.getClientRequestHeaders().getRequestURI();
		
		for (JavaScript js : javaScripts) {
			if(requestURI.contains(js.byassPattern)) {
				currentBypass = js.bypassTo;
				return RequestProcessingActions.FINAL_REQUEST;
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
			
			String scripts = "<div id='_ap_messagebox' style='position:absolute;top:0;left:0;z-index:10;background:#ffc;padding:5px;border:1px solid #ccc;text-align:center;font-weight: bold;width:99%;float:right;cursor:pointer;'>Loading</div>" +
                             "<script type='text/javascript'>" +
                               "_ap_checksum = '" + Checksum.md5(clearTextService.getCleartext()) + "'" +
                              "</script>" +
                              "<script src='" + javascriptServer + "javascripts/jquery-1.3.2.min.js'></script>" +
                              "<script>jQuery.noConflict();</script>";
			
			for (JavaScript js : javaScripts) {
				scripts += "<script src='" + js.script + "'></script>";
			}
			
			sb.insert(bodyEndIDx, scripts);
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
	public List<ResponseServiceProvider> provideResponseServices(HttpResponse response) {
		List<ResponseServiceProvider> services = new LinkedList<ResponseServiceProvider>();
		services.add(new SimpleJavaScriptInjectorProvider());
		return services;
	}

	@Override
	public Set<Class<? extends ProxyService>> getDependencies() {
		Set<Class<? extends ProxyService>> services = new HashSet<Class<? extends ProxyService>>();
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

package sk.fiit.rabbit.adaptiveproxy.plugins.services.logging;

import org.apache.log4j.Logger;

import sk.fiit.rabbit.adaptiveproxy.plugins.PluginProperties;
import sk.fiit.rabbit.adaptiveproxy.plugins.headers.ResponseHeaders;
import sk.fiit.rabbit.adaptiveproxy.plugins.helpers.RequestAndResponseProcessingPluginAdapter;
import sk.fiit.rabbit.adaptiveproxy.plugins.helpers.ResponseProcessingPluginAdapter;
import sk.fiit.rabbit.adaptiveproxy.plugins.messages.HttpMessageFactory;
import sk.fiit.rabbit.adaptiveproxy.plugins.messages.HttpRequest;
import sk.fiit.rabbit.adaptiveproxy.plugins.messages.ModifiableHttpRequest;
import sk.fiit.rabbit.adaptiveproxy.plugins.messages.ModifiableHttpResponse;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.ServiceUnavailableException;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.cleartext.ClearTextExtractionService;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.common.Checksum;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.content.ModifiableStringService;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.content.StringContentService;

public class KeywordsDisplayService extends RequestAndResponseProcessingPluginAdapter {
	
	Logger logger = Logger.getLogger(KeywordsDisplayService.class);
	private String kwServiceRoot;
	private String kwCallback;
	
	@Override
	public HttpRequest getNewRequest(ModifiableHttpRequest proxyRequest, HttpMessageFactory messageFactory) {
		String kwArgs;
		
		int queryParamsIdx = proxyRequest.getClientRequestHeaders().getRequestURI().indexOf("?");
		
		kwArgs = proxyRequest.getClientRequestHeaders().getRequestURI().substring(queryParamsIdx);
		
		proxyRequest.getProxyRequestHeaders().setRequestURI(kwServiceRoot + kwCallback + kwArgs);
		return messageFactory.constructHttpRequest(proxyRequest, proxyRequest.getProxyRequestHeaders(), true);
	}
	
	@Override
	public RequestProcessingActions processRequest(ModifiableHttpRequest request) {
		if(request.getClientRequestHeaders().getRequestURI().contains(kwCallback)) {
			return RequestProcessingActions.FINAL_REQUEST;
		} 
		
		return RequestProcessingActions.PROCEED;
	}
	
	@Override
	public ResponseProcessingActions processResponse(ModifiableHttpResponse response) {
		
		ClearTextExtractionService clearTextService;
		try {
			clearTextService = response.getServiceHandle().getService(ClearTextExtractionService.class);
		} catch (ServiceUnavailableException e1) {
			logger.trace("ClearTextService unavailable");
			return ResponseProcessingActions.PROCEED;
		}
		
		if(response.getClientRequestHeaders().getRequestURI().startsWith(kwServiceRoot)) {
			return ResponseProcessingActions.PROCEED;
		}
		
		try {
			ModifiableStringService ms = response.getServiceHandle().getService(ModifiableStringService.class);
			
			StringBuilder sb = ms.getModifiableContent();
			
			String html = sb.toString();

			int bodyEndIDx = html.indexOf("</body>");
			if(bodyEndIDx < 0) {
				logger.debug("No </body> : " + response.getProxyRequestHeaders().getRequestURI());
				return ResponseProcessingActions.PROCEED;
			}
			
			sb.insert(bodyEndIDx, "<div id='_ap_messagebox' style='position:absolute;top:0;left:0;z-index:10;background:#ffc;padding:5px;border:1px solid #ccc;text-align:center;font-weight: bold;width:99%;float:right'>Loading keywords..</div>" +
                                  "<script type='text/javascript'>" +
                                    "_ap_checksum = '" + Checksum.md5(clearTextService.getCleartext()) + "'" +
                                  "</script>" +
                                  "<script src='" + kwServiceRoot + "javascripts/jquery-1.3.2.min.js'></script>" +
                                  "<script src='" + kwServiceRoot + "javascripts/application.js'></script>");
		} catch (ServiceUnavailableException e) {
		}
		
		return ResponseProcessingActions.PROCEED;
	}
	
	@Override
	public boolean setup(PluginProperties props) {
		kwServiceRoot = props.getProperty("kwServiceRoot");
		kwCallback = props.getProperty("kwCallback");
		return true;
	}
	
	@Override
	public boolean wantResponseContent(ResponseHeaders webRPHeaders) {
		return true;
	}

}

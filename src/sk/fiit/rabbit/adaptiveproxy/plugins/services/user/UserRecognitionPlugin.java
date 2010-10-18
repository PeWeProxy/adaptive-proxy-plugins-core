package sk.fiit.rabbit.adaptiveproxy.plugins.services.user;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import sk.fiit.rabbit.adaptiveproxy.plugins.PluginProperties;
import sk.fiit.rabbit.adaptiveproxy.plugins.headers.RequestHeaders;
import sk.fiit.rabbit.adaptiveproxy.plugins.helpers.RequestAndResponseProcessingPluginAdapter;
import sk.fiit.rabbit.adaptiveproxy.plugins.messages.HttpMessageFactory;
import sk.fiit.rabbit.adaptiveproxy.plugins.messages.HttpRequest;
import sk.fiit.rabbit.adaptiveproxy.plugins.messages.HttpResponse;
import sk.fiit.rabbit.adaptiveproxy.plugins.messages.ModifiableHttpRequest;
import sk.fiit.rabbit.adaptiveproxy.plugins.messages.ModifiableHttpResponse;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.ServiceUnavailableException;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.common.SqlUtils;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.content.ModifiableStringService;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.content.StringContentService;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.database.DatabaseConnectionProviderService;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.injector.HtmlInjectorService;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.injector.JavaScriptInjectingProcessingPlugin;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.injector.HtmlInjectorService.HtmlPosition;

public class UserRecognitionPlugin extends RequestAndResponseProcessingPluginAdapter {
	
	protected Logger logger = Logger.getLogger(JavaScriptInjectingProcessingPlugin.class);
	
	private String scriptUrl;
	private String bypassPattern;
	
	@Override
	public HttpRequest getNewRequest(ModifiableHttpRequest proxyRequest, HttpMessageFactory messageFactory) {		
		return proxyRequest;
	}

	@Override
	public RequestProcessingActions processRequest(ModifiableHttpRequest request)
	{
		Map<String, String> postData;
		
		if(request.getClientRequestHeaders().getRequestURI().contains(bypassPattern))
		{
			try {
				StringContentService stringContentService = request.getServiceHandle().getService(ModifiableStringService.class);
				
				postData = getPostDataFromRequest(stringContentService.getContent());				
	
				Connection con = null;
				
				if (postData.containsKey("__peweproxy_uid") && postData.containsKey("__peweproxy_pid") && postData.containsKey("__peweproxy_timestamp"))	
				{
					try {
						con = request.getServiceHandle().getService(DatabaseConnectionProviderService.class).getDatabaseConnection();
						updateDatabaseLog(con, postData.get("__peweproxy_uid"), postData.get("__peweproxy_pid"), postData.get("__peweproxy_timestamp"));
					} finally {
						SqlUtils.close(con);
					}
				}
			} catch (ServiceUnavailableException e) {
				logger.trace("ModifiableStringService is unavailable");
			}
			
			return RequestProcessingActions.FINAL_RESPONSE;
		}
		else
		{
			return RequestProcessingActions.PROCEED;
		}
		
	}
	
	private boolean updateDatabaseLog(Connection connection, String uid, String pid, String timestamp)
	{		
		PreparedStatement stmt = null;
		
		try {
			stmt = connection.prepareStatement("UPDATE access_logs SET userid=? WHERE id=? AND timestamp=?");
			stmt.setString(1, uid);
			stmt.setString(2, pid);
			stmt.setString(3, timestamp);
			
			stmt.execute();
		} catch (SQLException e) {
			logger.error("Could not set users uid for access log", e);
		} finally {
			SqlUtils.close(stmt);
		}
		
		return true;
	}
	
	
	private Map<String, String> getPostDataFromRequest(String requestContent)
	{	
		Map<String, String> postData = new HashMap<String, String>();
		String attributeName;
		String attributeValue;
		
		for (String postPair : requestContent.split("&"))
		{
			if (postPair.split("=").length == 2)
			{
				attributeName = postPair.split("=")[0];
				attributeValue = postPair.split("=")[1];
				postData.put(attributeName, attributeValue);
			}
		}
		
		return postData;
	}
	
	@Override
	public HttpResponse getResponse(ModifiableHttpRequest proxyResponse, HttpMessageFactory messageFactory)
	{
		return messageFactory.constructHttpResponse("text/plain");
	}

	
	@Override
	public ResponseProcessingActions processResponse(ModifiableHttpResponse response) {
		try {
			HtmlInjectorService htmlInjectionService = response.getServiceHandle().getService(HtmlInjectorService.class);
			
			String scripts = "<script src='" + scriptUrl + "'></script>";
			htmlInjectionService.inject(scripts, HtmlPosition.ON_MARK);
			
		} catch (ServiceUnavailableException e) {
			logger.trace("HtmlInjectorService is unavailable, JavaScriptInjector takes no action");
		}
		
		return ResponseProcessingActions.PROCEED;
	}
	
	
	@Override
	public boolean setup(PluginProperties props) {
		scriptUrl = props.getProperty("scriptUrl");
		bypassPattern = props.getProperty("bypassPattern");
		
		return true;
	}
	
	@Override
	public boolean wantRequestContent(RequestHeaders clientRQHeaders) {
		// TODO Auto-generated method stub
		return true;
	}
	
}

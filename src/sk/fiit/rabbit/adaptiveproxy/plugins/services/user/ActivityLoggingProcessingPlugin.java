package sk.fiit.rabbit.adaptiveproxy.plugins.services.user;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import sk.fiit.peweproxy.headers.RequestHeader;
import sk.fiit.peweproxy.messages.HttpMessageFactory;
import sk.fiit.peweproxy.messages.HttpResponse;
import sk.fiit.peweproxy.messages.ModifiableHttpRequest;
import sk.fiit.peweproxy.services.ProxyService;
import sk.fiit.peweproxy.services.content.ModifiableStringService;
import sk.fiit.peweproxy.services.content.StringContentService;
import sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions.DatabaseConnectionProviderService;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.common.SqlUtils;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.injector.JavaScriptInjectingProcessingPlugin;

public class ActivityLoggingProcessingPlugin extends JavaScriptInjectingProcessingPlugin
{
	protected Logger logger = Logger.getLogger(ActivityLoggingProcessingPlugin.class);
	
	@Override
	public HttpResponse getResponse(ModifiableHttpRequest request, HttpMessageFactory messageFactory) {
		if(request.getServicesHandle().isServiceAvailable(DatabaseConnectionProviderService.class)
				&& request.getServicesHandle().isServiceAvailable(StringContentService.class)) {
			
			Map<String, String> postData;
			
			StringContentService stringContentService = request.getServicesHandle().getService(StringContentService.class);
			
			postData = getPostDataFromRequest(stringContentService.getContent());				
	
			Connection con = null;
			
			if (postData.containsKey("period") && postData.containsKey("copies") && postData.containsKey("scrolls") && postData.containsKey("_ap_uuid")) {
				try {
					con = request.getServicesHandle().getService(DatabaseConnectionProviderService.class).getDatabaseConnection();
					
					updateDatabaseLog(con, postData.get("period"), postData.get("copies"), postData.get("scrolls"), postData.get("_ap_uuid"));						
				} finally {
					SqlUtils.close(con);
				}
			}
		}
		
		return messageFactory.constructHttpResponse(null, "text/html");
	}
	
	private boolean updateDatabaseLog(Connection connection, String period, String copies, String scrolls, String uuid) {			
		try {
			PreparedStatement page_stmt = null;
			
			page_stmt = connection
						.prepareStatement("update access_logs set `time_on_page` = `time_on_page` + ?, `scroll_count` = `scroll_count` + ?, `copy_count` = `copy_count` + ? WHERE uuid = ?");
			page_stmt.setString(1, period);
			page_stmt.setString(2, scrolls);
			page_stmt.setString(3, copies);
			page_stmt.setString(4, uuid);
			page_stmt.execute();
			
		} catch (SQLException e) {
			logger.error("Could not log activity", e);
		} finally {
			SqlUtils.close(connection);
		}
		
		return true;
	}
	
	private Map<String, String> getPostDataFromRequest(String requestContent) {	
		try {
			requestContent = URLDecoder.decode(requestContent,"utf-8");
		} catch (UnsupportedEncodingException e) {
			logger.warn(e);
		}
		
		Map<String, String> postData = new HashMap<String, String>();
		String attributeName;
		String attributeValue;
		
		for (String postPair : requestContent.split("&")) {
			if (postPair.split("=").length == 2) {
				attributeName = postPair.split("=")[0];
				attributeValue = postPair.split("=")[1];
				postData.put(attributeName, attributeValue);
			}
		}
		
		return postData;
	}
	
	//FIXME: toto je docasny hack kvoli late processingu, tato metoda tu inak nema byt
	@Override
	public void desiredRequestServices(
			Set<Class<? extends ProxyService>> desiredServices,
			RequestHeader clientRQHeader) {
		super.desiredRequestServices(desiredServices, clientRQHeader);
		desiredServices.add(ModifiableStringService.class);
	}
}

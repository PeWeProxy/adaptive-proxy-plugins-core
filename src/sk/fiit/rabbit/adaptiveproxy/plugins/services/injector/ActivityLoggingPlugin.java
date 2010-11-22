package sk.fiit.rabbit.adaptiveproxy.plugins.services.injector;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.sql.Connection;
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

public class ActivityLoggingPlugin extends JavaScriptInjectingProcessingPlugin
{
	protected Logger logger = Logger.getLogger(ActivityLoggingPlugin.class);
	
	@Override
	public HttpResponse getResponse(ModifiableHttpRequest request, HttpMessageFactory messageFactory) {
		if(request.getServicesHandle().isServiceAvailable(DatabaseConnectionProviderService.class)
				&& request.getServicesHandle().isServiceAvailable(StringContentService.class)) {
			Map<String, String> postData;
			
			StringContentService stringContentService = request.getServicesHandle().getService(StringContentService.class);
			
			postData = getPostDataFromRequest(stringContentService.getContent());				
	
			Connection con = null;
			
			if (postData.containsKey("uid") && postData.containsKey("checksum")  && postData.containsKey("period") && postData.containsKey("copies") && postData.containsKey("scrolls")) {
				try {
					con = request.getServicesHandle().getService(DatabaseConnectionProviderService.class).getDatabaseConnection();
					
					updateDatabaseLog(con, postData.get("uid"), postData.get("checksum"), postData.get("period"), postData.get("copies"), postData.get("scrolls"));						
				} finally {
					SqlUtils.close(con);
				}
			}
		}
		
		return messageFactory.constructHttpResponse(null, "text/html");
	}
	
	private boolean updateDatabaseLog(Connection connection, String uid, String checksum, String period, String copies, String scrolls) {			
		try {
			Statement stmt = connection.createStatement(
                    ResultSet.TYPE_SCROLL_SENSITIVE,
                    ResultSet.CONCUR_UPDATABLE);
			String query = "SELECT * FROM access_logs INNER JOIN pages ON pages.id = access_logs.page_id WHERE pages.checksum = '" + 
				checksum + "' AND access_logs.userid = '" + uid + "' ORDER BY timestamp ASC LIMIT 1;";
				
			ResultSet rs = stmt.executeQuery(query);

			rs.absolute(1);
			
			String id = rs.getString("id");
			stmt.close();
			
			Statement stmt_log = connection.createStatement(
                    ResultSet.TYPE_SCROLL_SENSITIVE,
                    ResultSet.CONCUR_UPDATABLE);
			query = "SELECT * FROM access_logs WHERE access_logs.id = '" + 
				id + "';";
				
			ResultSet rsUpdate = stmt_log.executeQuery(query);
			
			rsUpdate.absolute(1);
			
			int time_on_page = rsUpdate.getInt("time_on_page");
			time_on_page += Integer.valueOf(period).intValue();
			rsUpdate.updateInt("time_on_page", time_on_page);
			
			int scroll_count = rsUpdate.getInt("scroll_count");
			scroll_count += Integer.valueOf(scrolls).intValue();
			rsUpdate.updateInt("scroll_count", scroll_count);
			
			int copy_count = rsUpdate.getInt("copy_count");
			copy_count += Integer.valueOf(copies).intValue();
			rsUpdate.updateInt("copy_count", copy_count);
			rsUpdate.updateRow();
			
		} catch (SQLException e) {
			logger.error("Could not get page id for access log", e);
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

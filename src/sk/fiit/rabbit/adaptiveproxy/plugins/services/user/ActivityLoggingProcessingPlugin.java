package sk.fiit.rabbit.adaptiveproxy.plugins.services.user;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.fourspaces.couchdb.Database;
import com.fourspaces.couchdb.Document;
import com.fourspaces.couchdb.View;
import com.fourspaces.couchdb.ViewResults;

import sk.fiit.peweproxy.headers.RequestHeader;
import sk.fiit.peweproxy.messages.HttpMessageFactory;
import sk.fiit.peweproxy.messages.HttpResponse;
import sk.fiit.peweproxy.messages.ModifiableHttpRequest;
import sk.fiit.peweproxy.services.ProxyService;
import sk.fiit.peweproxy.services.content.ModifiableStringService;
import sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions.DatabaseConnectionProviderService;
import sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions.DatabaseSessionProviderService;
import sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions.PostDataParserService;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.common.SqlUtils;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.injector.JavaScriptInjectingProcessingPlugin;

public class ActivityLoggingProcessingPlugin extends JavaScriptInjectingProcessingPlugin
{
	protected Logger logger = Logger.getLogger(ActivityLoggingProcessingPlugin.class);
	
	@Override
	public HttpResponse getResponse(ModifiableHttpRequest request, HttpMessageFactory messageFactory) {
	
	    if (!request.getServicesHandle().isServiceAvailable(PostDataParserService.class))
	    	return messageFactory.constructHttpResponse(null, "text/html");
	    
	    Map<String, String> postData = request.getServicesHandle().getService(PostDataParserService.class).getPostData();
	    
	    if(postData != null && request.getServicesHandle().isServiceAvailable(DatabaseConnectionProviderService.class)) {
	    	
			Connection con = null;
			Database database = null;
			
			if (postData.containsKey("period") && postData.containsKey("copies") 
				&& postData.containsKey("scrolls") && postData.containsKey("page_uid")) {
				    try {
						con = request.getServicesHandle()
							.getService(DatabaseConnectionProviderService.class)
							.getDatabaseConnection();
						updateDatabaseLog(con, postData.get("period"), postData.get("copies"), postData.get("scrolls"), postData.get("page_uid"));						
				    } finally {
				    	SqlUtils.close(con);
				    }
				    
				    try {
				    	database = request.getServicesHandle()
				    			.getService(DatabaseSessionProviderService.class).getDatabase();
				    	updateDatabaseLog(database, postData.get("period"), postData.get("copies"), postData.get("scrolls"), postData.get("page_uid"));
				    } catch (Exception e) {
				    	logger.error("Unable log to CouchDB", e);
				    }
				}
	    }
		
	    return messageFactory.constructHttpResponse(null, "text/html");
	}
	
	private boolean updateDatabaseLog(Connection connection, String period, String copies, String scrolls, String uuid) {			
		try {
			PreparedStatement page_stmt = null;
			
			page_stmt = connection
						.prepareStatement("update access_logs set `time_on_page` = `time_on_page` + ?, `scroll_count` = `scroll_count` + ?, `copy_count` = `copy_count` + ? WHERE id = ?");
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
	
	private boolean updateDatabaseLog(Database database, String period, String copies, String scrolls, String uuid) {			
		try {
			View view = new View("_all_docs");
			view.setStartKey("\""+uuid+"\"");
			view.setCount(1);
			ViewResults vr = database.view(view);
			
			if(vr == null || vr.size() == 0) {
				return(false);
			}
			
			List<Document> list = vr.getResults();
				
			if(list.size() == 0) {
				return(false);
			}
			
			Document doc = (Document)list.get(0);
			
			if(doc == null) {
				return(false);
			}
			
			doc = database.getDocumentWithRevisions((String)doc.get("_id"));
			
			if(doc == null) {
				return(false);
			}
			
			doc.put("time_on_page", period);
			doc.put("scroll_count", scrolls);
			doc.put("copy_count", copies);
			
		} catch (Exception e) {
			logger.error("Could not log activity to CouchDB", e);
		} 
		return true;
	}	
	
	//FIXME: toto je docasny hack kvoli late processingu, tato metoda tu inak nema byt
	@Override
	public void desiredRequestServices(
			Set<Class<? extends ProxyService>> desiredServices,
			RequestHeader clientRQHeader) {
		super.desiredRequestServices(desiredServices, clientRQHeader);
		desiredServices.add(ModifiableStringService.class);
		desiredServices.add(DatabaseConnectionProviderService.class);
		desiredServices.add(DatabaseSessionProviderService.class);
	}
}
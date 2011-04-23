package sk.fiit.rabbit.adaptiveproxy.plugins.services.user;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Map;
import java.util.Set;

import sk.fiit.peweproxy.headers.RequestHeader;
import sk.fiit.peweproxy.headers.ResponseHeader;
import sk.fiit.peweproxy.messages.HttpMessageFactory;
import sk.fiit.peweproxy.messages.HttpRequest;
import sk.fiit.peweproxy.messages.HttpResponse;
import sk.fiit.peweproxy.messages.ModifiableHttpRequest;
import sk.fiit.peweproxy.services.ProxyService;
import sk.fiit.peweproxy.services.content.ModifiableStringService;
import sk.fiit.rabbit.adaptiveproxy.plugins.common.Checksum;
import sk.fiit.rabbit.adaptiveproxy.plugins.common.SqlUtils;
import sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions.DatabaseConnectionProviderService;
import sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions.DatabaseSessionProviderService;
import sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions.PageInformation;
import sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions.PageInformationProviderService;
import sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions.PostDataParserService;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.injector.JavaScriptInjectingProcessingPlugin;

import com.fourspaces.couchdb.Database;
import com.fourspaces.couchdb.Document;
import com.fourspaces.couchdb.View;
import com.fourspaces.couchdb.ViewResults;

public class UserAccessLoggingProcessingPlugin extends JavaScriptInjectingProcessingPlugin {
	PageInformation pi;
	
	@Override
	public void processTransferedResponse(HttpResponse response) {
	    	String reqURI = response.getRequest().getRequestHeader().getRequestURI();
	    	if (reqURI.contains("?nologging") || reqURI.contains(".js?") || reqURI.endsWith(".js"))
	    	    return;
		if(response.getServicesHandle().isServiceAvailable(PageInformationProviderService.class)) {
			pi = response.getServicesHandle()
					.getService(PageInformationProviderService.class)
					.getPageInformation();
		}
	}
	
	@Override
	public HttpResponse getResponse(ModifiableHttpRequest request, HttpMessageFactory messageFactory) {
	    if (!request.getServicesHandle().isServiceAvailable(PostDataParserService.class))
		return messageFactory.constructHttpResponse(null, "text/html");
	    
	    Map<String, String> postData = request.getServicesHandle().getService(PostDataParserService.class).getPostData();
	    	
	    if(postData != null && request.getServicesHandle().isServiceAvailable(DatabaseConnectionProviderService.class)) {
		Connection con = null;
		    Database database = null;
		    if(request.getServicesHandle().isServiceAvailable(DatabaseSessionProviderService.class)) {
		    	database = request.getServicesHandle().getService(DatabaseSessionProviderService.class).getDatabase();
		    }
	    	if (postData.containsKey("__peweproxy_uid")
	    		&& postData.containsKey("__ap_url") && postData.containsKey("page_uid") && postData.containsKey("log_id")) {
	    	    try {
			con = request.getServicesHandle()
				.getService(DatabaseConnectionProviderService.class)
				.getDatabaseConnection();

			createDatabaseLog(database, con, postData.get("log_id"), postData.get("__peweproxy_uid"), 
				postData.get("__ap_url"), request.getClientSocketAddress().toString(), postData.get("page_uid"));
	    	    } finally {
			SqlUtils.close(con);
	    	    }
		}
	    }
	
	    return messageFactory.constructHttpResponse(null, "text/html");
	}
	
	private boolean createDatabaseLog(Database database, Connection connection, String log_id, String uid,
			String url, String ip, String uuid) {
		PreparedStatement log_stmt = null;
		ResultSet rs = null;

		java.util.Date today = new java.util.Date();
		String timestamp = new Timestamp(today.getTime()).toString();
		String formatedTimeStamp = timestamp.substring(0,
				timestamp.indexOf("."));

		try {
			url = URLDecoder.decode(url, "utf-8");
		} catch (UnsupportedEncodingException e) {
			logger.warn(e);
		}

		if (!"".equals(uid) && database != null) {
			try {
				View view = new View("_all_docs");
				view.setStartKey("\""+uuid+"\"");
				view.setEndKey("\""+uuid+"\"");
				ViewResults vr = database.view(view);
				
				if(vr == null || vr.getResults().size() == 0) {
					
					Document access_log = new Document();
					access_log.put("_id", uuid);
					access_log.put("type", "ACCESS_LOG");
					access_log.put("userid", uid);
					access_log.put("timestamp", formatedTimeStamp);
					access_log.put("time_on_page", 0);
					access_log.put("page_id", (pi != null && pi.id != null) ? pi.id : log_id);
					access_log.put("scroll_count", 0);
					access_log.put("copy_count", 0);
					access_log.put("referer", url);
					access_log.put("ip", Checksum.md5(ip));
					database.saveDocument(access_log);
				} else {
					logger.warn("Trying to save access_log but CouchDB ID "+uuid+" already exists.");
				}
			} catch (Exception e) {
				logger.error("Could not insert access_log to CouchDB ", e);
			}
			
			try {
				
				String query = "SELECT count(id) FROM access_logs WHERE id = ?";
				log_stmt = connection.prepareStatement(query);
				log_stmt.setString(1, uuid);
				rs = log_stmt.executeQuery();
				
				if(rs.next() && rs.getInt(1) == 0) {
					log_stmt = connection
						.prepareStatement("INSERT INTO `access_logs` (`id`, `userid`, `timestamp`, `time_on_page`, `page_id`, `scroll_count`, `copy_count`, `referer`, `ip`) VALUES (?, ?, ?, 0, ?, 0, 0, ?, ?);");
						
					log_stmt.setString(1, uuid);
					log_stmt.setString(2, uid);
					log_stmt.setString(3, formatedTimeStamp);
					log_stmt.setString(4, (pi != null && pi.id != null) ? pi.id : log_id);
					log_stmt.setString(5, url);
					log_stmt.setString(6, Checksum.md5(ip));
					log_stmt.execute();
				} else {
					logger.warn("Trying to save access_log but MySQL ID "+uuid+" already exists.");
				}

			} catch (SQLException e) {
				logger.error("Could not insert access_log ", e);
			} finally {
				SqlUtils.close(rs);
				SqlUtils.close(log_stmt);
			}
		} else {
			SqlUtils.close(log_stmt);
		}

		return true;
	}
	
	@Override
	public void desiredRequestServices(
			Set<Class<? extends ProxyService>> desiredServices,
			RequestHeader webRQHeader) {
		super.desiredRequestServices(desiredServices, webRQHeader);
		desiredServices.add(ModifiableStringService.class); //FIXME: toto je docasny hack kvoli late processingu, spravne tu ma byt len StringContentService
		desiredServices.add(DatabaseConnectionProviderService.class);
	}
	
	@Override
	public void desiredResponseServices(
			Set<Class<? extends ProxyService>> desiredServices,
			ResponseHeader webRPHeader) {
		super.desiredResponseServices(desiredServices, webRPHeader);
		desiredServices.add(PageInformationProviderService.class);
	}
	
}
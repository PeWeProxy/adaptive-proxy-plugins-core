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
import sk.fiit.peweproxy.messages.HttpResponse;
import sk.fiit.peweproxy.messages.ModifiableHttpRequest;
import sk.fiit.peweproxy.services.ProxyService;
import sk.fiit.peweproxy.services.content.ModifiableStringService;
import sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions.DatabaseConnectionProviderService;
import sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions.PageInformationProviderService;
import sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions.PostDataParserService;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.common.Checksum;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.common.SqlUtils;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.injector.JavaScriptInjectingProcessingPlugin;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.page.PageInformation;

public class UserAccessLoggingProcessingPlugin extends JavaScriptInjectingProcessingPlugin {
	@Override
	public void processTransferedResponse(HttpResponse response) {
		if(response.getServicesHandle().isServiceAvailable(PageInformationProviderService.class)) {
			PageInformation pi = response.getServicesHandle()
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
		
	    	if (postData.containsKey("__peweproxy_uid") && postData.containsKey("_ap_checksum")
	    		&& postData.containsKey("__ap_url") && postData.containsKey("page_uid")) {
	    	    try {
			con = request.getServicesHandle()
				.getService(DatabaseConnectionProviderService.class)
				.getDatabaseConnection();

			createDatabaseLog(con, postData.get("__peweproxy_uid"), postData.get("_ap_checksum"), 
				postData.get("__ap_url"), request.getClientSocketAddress().toString(), postData.get("page_uid"));
	    	    } finally {
			SqlUtils.close(con);
	    	    }
		}
	    }
	
	    return messageFactory.constructHttpResponse(null, "text/html");
	}
	
	private boolean createDatabaseLog(Connection connection, String uid,
			String checksum, String url, String ip, String uuid) {
		PreparedStatement page_stmt = null;
		PreparedStatement log_stmt = null;

		String pid = "";
		java.util.Date today = new java.util.Date();
		String timestamp = new Timestamp(today.getTime()).toString();
		String formatedTimeStamp = timestamp.substring(0,
				timestamp.indexOf("."));

		try {
			url = URLDecoder.decode(url, "utf-8");
		} catch (UnsupportedEncodingException e) {
			logger.warn(e);
		}

		try {
			page_stmt = connection
					.prepareStatement("SELECT * FROM pages WHERE url=? AND checksum =? ORDER BY id DESC LIMIT 1;");
			page_stmt.setString(1, url);
			page_stmt.setString(2, checksum);

			page_stmt.execute();
			ResultSet rs = page_stmt.getResultSet();

			while (rs.next()) {
				pid = rs.getString(1);
			}

			if (!"".equals(uid)) {
				try {
					log_stmt = connection
							.prepareStatement("INSERT INTO `access_logs` (`id`, `userid`, `timestamp`, `time_on_page`, `page_id`, `scroll_count`, `copy_count`, `referer`, `ip`) VALUES (?, ?, ?, 0, ?, 0, 0, ?, ?);");
					
					log_stmt.setString(1, uuid);
					log_stmt.setString(2, uid);
					log_stmt.setString(3, formatedTimeStamp);
					log_stmt.setString(4, pid);
					log_stmt.setString(5, url);
					log_stmt.setString(6, Checksum.md5(ip));

					log_stmt.execute();
				} catch (SQLException e) {
					logger.error("Could not insert access_log ", e);
				} finally {
					SqlUtils.close(log_stmt);
				}
			} else {
				SqlUtils.close(log_stmt);
			}

		} catch (SQLException e) {
			logger.error("Could not get page id for access log", e);
		} finally {
			SqlUtils.close(page_stmt);
		}

		return true;
	}

	@Override
	public void desiredRequestServices(
			Set<Class<? extends ProxyService>> desiredServices,
			RequestHeader clientRQHeader) {
		super.desiredRequestServices(desiredServices, clientRQHeader);
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
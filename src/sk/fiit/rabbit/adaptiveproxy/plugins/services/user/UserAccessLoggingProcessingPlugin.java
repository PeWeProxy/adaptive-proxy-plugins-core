package sk.fiit.rabbit.adaptiveproxy.plugins.services.user;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import sk.fiit.peweproxy.headers.RequestHeader;
import sk.fiit.peweproxy.headers.ResponseHeader;
import sk.fiit.peweproxy.messages.HttpMessageFactory;
import sk.fiit.peweproxy.messages.HttpResponse;
import sk.fiit.peweproxy.messages.ModifiableHttpRequest;
import sk.fiit.peweproxy.services.ProxyService;
import sk.fiit.peweproxy.services.content.ModifiableStringService;
import sk.fiit.peweproxy.services.content.StringContentService;
import sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions.DatabaseConnectionProviderService;
import sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions.PageInformationProviderService;
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
		if(!request.getServicesHandle().isServiceAvailable(StringContentService.class)) {
			return messageFactory.constructHttpResponse(null, "text/html");
		}
		
		StringContentService stringContentService = request.getServicesHandle()
				.getService(StringContentService.class);

		Map<String, String> postData = getPostDataFromRequest(stringContentService
				.getContent());

		Connection con = null;

		if (postData.containsKey("__peweproxy_uid")
				&& postData.containsKey("_ap_checksum")
				&& postData.containsKey("__ap_url")
				&& postData.containsKey("_ap_uuid")) {
			
			if(!request.getServicesHandle().isServiceAvailable(DatabaseConnectionProviderService.class)) {
				return messageFactory.constructHttpResponse(null, "text/html");
			}
			
			try {
				con = request.getServicesHandle()
						.getService(DatabaseConnectionProviderService.class)
						.getDatabaseConnection();

				createDatabaseLog(con, postData.get("__peweproxy_uid"),
						postData.get("_ap_checksum"), postData.get("__ap_url"),
						"ip", postData.get("_ap_uuid"));
			} finally {
				SqlUtils.close(con);
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
							.prepareStatement("INSERT INTO `access_logs` (`id`, `userid`, `timestamp`, `time_on_page`, `page_id`, `scroll_count`, `copy_count`, `referer`, `ip`, `uuid`) VALUES (NULL, ?, ?, NULL, ?, NULL, NULL, ?, ?, ?);");

					log_stmt.setString(1, uid);
					log_stmt.setString(2, formatedTimeStamp);
					log_stmt.setString(3, pid);
					log_stmt.setString(4, url);
					log_stmt.setString(5, Checksum.md5(ip));
					log_stmt.setString(6, uuid);

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
	
	private Map<String, String> getPostDataFromRequest(String requestContent) {
		try {
			requestContent = URLDecoder.decode(requestContent, "utf-8");
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

	@Override
	public void desiredRequestServices(
			Set<Class<? extends ProxyService>> desiredServices,
			RequestHeader clientRQHeader) {
		super.desiredRequestServices(desiredServices, clientRQHeader);
		desiredServices.add(ModifiableStringService.class); //FIXME: hack kvoli late processingu, ma tu byt len StringContentService
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

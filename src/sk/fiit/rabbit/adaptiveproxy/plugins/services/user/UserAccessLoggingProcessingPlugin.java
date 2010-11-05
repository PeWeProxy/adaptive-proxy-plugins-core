package sk.fiit.rabbit.adaptiveproxy.plugins.services.user;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
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
import sk.fiit.rabbit.adaptiveproxy.plugins.services.common.Checksum;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.common.SqlUtils;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.content.ModifiableStringService;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.content.StringContentService;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.database.DatabaseConnectionProviderService;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.injector.HtmlInjectorService;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.injector.HtmlInjectorService.HtmlPosition;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.injector.JavaScriptInjectingProcessingPlugin;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.page.PageInformation;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.page.PageInformationProviderService;

public class UserAccessLoggingProcessingPlugin extends RequestAndResponseProcessingPluginAdapter {
	
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
				StringContentService stringContentService = request.getServiceHandle().getService(StringContentService.class);
				
				postData = getPostDataFromRequest(stringContentService.getContent());				
	
				Connection con = null;
				
				if (postData.containsKey("__peweproxy_uid") && postData.containsKey("_ap_checksum") && postData.containsKey("__ap_url"))	
				{
					try {
						con = request.getServiceHandle().getService(DatabaseConnectionProviderService.class).getDatabaseConnection();
						
						createDatabaseLog(con, postData.get("__peweproxy_uid"), postData.get("_ap_checksum"), postData.get("__ap_url"), "ip");						
					} finally {
						SqlUtils.close(con);
					}
				}
			} catch (ServiceUnavailableException e) {
				logger.trace("StringContentService is unavailable");
			}
			
			return RequestProcessingActions.FINAL_RESPONSE;
		}
		else
		{
			return RequestProcessingActions.PROCEED;
		}
		
	}
	
	private boolean createDatabaseLog(Connection connection, String uid, String checksum, String url, String ip)
	{		
		PreparedStatement page_stmt = null;
		PreparedStatement log_stmt = null;
		
		String pid = "";
		java.util.Date today = new java.util.Date();
		String timestamp = new Timestamp(today.getTime()).toString();
		String formatedTimeStamp = timestamp.substring(0, timestamp.indexOf("."));
		
		try {
			url = URLDecoder.decode(url, "utf-8");
		} catch (UnsupportedEncodingException e) {
			logger.warn(e);
		}
		
//		url = url.replace("%2F", "/");
//		url = url.replace("%3A", ":");
//		url = url.replace("%3F", "?");
//		url = url.replace("%26", "&");
//		url = url.replace("%3D", "=");
		
		try {
			page_stmt = connection.prepareStatement("SELECT * FROM pages WHERE url=? AND checksum =? ORDER BY id DESC LIMIT 1;");
			page_stmt.setString(1, url);
			page_stmt.setString(2, checksum);
			
			page_stmt.execute();
			ResultSet rs = page_stmt.getResultSet();
			
			while (rs.next()) {
				pid = rs.getString(1);
			}
			
			if (!"".equals(uid))
			{
				try {
					log_stmt = connection.prepareStatement("INSERT INTO `access_logs` (`id`, `userid`, `timestamp`, `time_on_page`, `page_id`, `scroll_count`, `copy_count`, `referer`, `ip`) VALUES (NULL, ?, ?, NULL, ?, NULL, NULL, ?, ?);");
	
					log_stmt.setString(1, uid);
					log_stmt.setString(2, formatedTimeStamp);
					log_stmt.setString(3, pid);
					log_stmt.setString(4, url);
					log_stmt.setString(5, Checksum.md5(ip));
					
					log_stmt.execute();
				} catch (SQLException e) {
					logger.error("Could not insert access_log ", e);
				} finally {
					SqlUtils.close(log_stmt);
				}
			}
			else
			{
				SqlUtils.close(log_stmt);
			}
			
		} catch (SQLException e) {
			logger.error("Could not get page id for access log", e);
		} finally {
			SqlUtils.close(page_stmt);
		}
		
		return true;
	}
	
	
	
	private Map<String, String> getPostDataFromRequest(String requestContent)
	{	
		try {
			requestContent = URLDecoder.decode(requestContent,"utf-8");
		} catch (UnsupportedEncodingException e) {
			logger.warn(e);
		}
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
			PageInformation pi = response.getServiceHandle().getService(PageInformationProviderService.class).getPageInformation();
		} catch (ServiceUnavailableException e) {
			logger.error("PageInformationProviderService is unavailable", e);
		}

		
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
		return true;
	}
	
}

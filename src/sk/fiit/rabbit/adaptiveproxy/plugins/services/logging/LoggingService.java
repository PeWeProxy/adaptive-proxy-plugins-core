package sk.fiit.rabbit.adaptiveproxy.plugins.services.logging;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

import org.apache.log4j.Logger;

import sk.fiit.rabbit.adaptiveproxy.plugins.PluginProperties;
import sk.fiit.rabbit.adaptiveproxy.plugins.helpers.AsynchronousResponseProcessingPluginAdapter;
import sk.fiit.rabbit.adaptiveproxy.plugins.messages.ModifiableHttpResponse;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.ProxyService;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.ServiceUnavailableException;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.ServicesHandle;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.content.StringContentService;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.database.DatabaseConnectionProviderService;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.page.PageInformation;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.page.PageInformationProviderService;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.user.UserIdentificationService;

public class LoggingService extends AsynchronousResponseProcessingPluginAdapter {
	
	private static final Logger logger = Logger.getLogger(LoggingService.class);
	
	private String nologgingParamName;

	@Override
	public boolean setup(PluginProperties props) {
		nologgingParamName = props.getProperty("nologgingParamName", "nologging");
		return true;
	}
	
	@Override
	protected boolean prepareServices(ServicesHandle handle) {
		try {
			ProxyService con = handle.getService(DatabaseConnectionProviderService.class);
			ProxyService uid = handle.getService(UserIdentificationService.class);
			ProxyService pi = handle.getService(PageInformationProviderService.class);
			
			addToCache("connection", con);
			addToCache("uid", uid);
			addToCache("pageInformation", pi);
		} catch(ServiceUnavailableException e) {
			logger.error("prepare service " + e.getServiceClass().getName() + " failed, due to: " + e.getCause().getMessage());
			return false;
		}

		return true;
	}

	@Override
	public void processResponseAsynchronously(ModifiableHttpResponse response) {
		String requestURI = response.getClientRequestHeaders().getRequestURI();

		if (!shouldLog(requestURI)) {
			return;
		}
		
		Connection con = ((DatabaseConnectionProviderService) getFromCache("connection")).getDatabaseConnection();

		try {
			String uid = ((UserIdentificationService) getFromCache("uid")).getClientIdentification();
			PageInformation pi = ((PageInformationProviderService) getFromCache("pageInformation")).getPageInformation();
		
			if(pi.getId() != null) {
				log(con, uid, pi.getId());
			}
		} finally {
			try {
				con.close();
			} catch (SQLException e) {}
			
		}
	}
	

	private void log(Connection connection, String userId, Long pageId) {
		PreparedStatement stmt = null;

		try {
			stmt = connection.prepareStatement("INSERT INTO access_logs(userid, timestamp, page_id) VALUES(?, ?, ?)");
			stmt.setString(1, userId);
			stmt.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
			stmt.setLong(3, pageId);
			
			stmt.execute();
		} catch (SQLException e) {
			logger.error("Could not save access log", e);
		} finally {
			try {
				if(stmt != null) {
					stmt.close();
				}
			} catch (SQLException e) {}
		}
	}
	
	private boolean shouldLog(String requestURL) {
		return !requestURL.contains(nologgingParamName);
	}

}

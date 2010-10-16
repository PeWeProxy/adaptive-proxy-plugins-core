package sk.fiit.rabbit.adaptiveproxy.plugins.services.logging;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import org.apache.log4j.Logger;

import sk.fiit.peweproxy.headers.ResponseHeader;
import sk.fiit.peweproxy.messages.HttpMessageFactory;
import sk.fiit.peweproxy.messages.HttpResponse;
import sk.fiit.peweproxy.messages.ModifiableHttpResponse;
import sk.fiit.peweproxy.plugins.PluginProperties;
import sk.fiit.peweproxy.plugins.processing.ResponseProcessingPlugin;
import sk.fiit.peweproxy.services.ProxyService;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.common.Checksum;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.common.SqlUtils;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.database.DatabaseConnectionProviderService;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.page.PageInformation;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.page.PageInformationProviderService;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.user.UserIdentificationService;

public class LoggingService implements ResponseProcessingPlugin {
	
	private static final Logger logger = Logger.getLogger(LoggingService.class);
	
	private String nologgingParamName;
	private ExecutorService threadPool;

	@Override
	public boolean start(PluginProperties props) {
		nologgingParamName = props.getProperty("nologgingParamName", "nologging");
		threadPool = props.getThreadPool();
		return true;
	}
	
	public void processResponseAsynchronously(ModifiableHttpResponse response) {
		String requestURI = response.getRequest().getClientRequestHeader().getRequestURI();

		if (!shouldLog(requestURI)) {
			return;
		}
		
		Connection con = null;

		try {
			con = response.getServicesHandle().getService(DatabaseConnectionProviderService.class).getDatabaseConnection();
			String uid = response.getServicesHandle().getService(UserIdentificationService.class).getClientIdentification();
			PageInformation pi = response.getServicesHandle().getService(PageInformationProviderService.class).getPageInformation();
		
			if(pi.getId() != null) {
				log(con, uid, pi.getId(), response.getRequest().getClientRequestHeader().getField("Referer"), Checksum.md5(response.getRequest().getClientSocketAddress().getAddress().getHostAddress()));
				
			}
		} finally {
			SqlUtils.close(con);			
		}
	}
	

	private void log(Connection connection, String userId, Long pageId, String referer, String ipAddress) {
		PreparedStatement stmt = null;

		try {
			stmt = connection.prepareStatement("INSERT INTO access_logs(userid, timestamp, page_id, referer, ip) VALUES(?, ?, ?, ?, ?)");
			stmt.setString(1, userId);
			stmt.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
			stmt.setLong(3, pageId);
			stmt.setString(4, referer);
			stmt.setString(5, ipAddress);
			
			stmt.execute();
		} catch (SQLException e) {
			logger.error("Could not save access log", e);
		} finally {
			SqlUtils.close(stmt);
		}
	}
	
	private boolean shouldLog(String requestURL) {
		return !requestURL.contains(nologgingParamName);
	}

	@Override
	public void desiredResponseServices(
			Set<Class<? extends ProxyService>> desiredServices,
			ResponseHeader webRPHeader) {
		desiredServices.add(UserIdentificationService.class);
		desiredServices.add(PageInformationProviderService.class);
		desiredServices.add(DatabaseConnectionProviderService.class);
	}

	@Override
	public boolean supportsReconfigure(PluginProperties newProps) {
		return true;
	}

	@Override
	public void stop() {
	}

	@Override
	public ResponseProcessingActions processResponse(ModifiableHttpResponse response) {
		
		final ModifiableHttpResponse responseClone = response.clone();
		
		threadPool.execute(new Runnable() {
			@Override
			public void run() {
				processResponseAsynchronously(responseClone);
			}
		});
		
		return ResponseProcessingActions.PROCEED;
	}

	@Override
	public HttpResponse getNewResponse(ModifiableHttpResponse response,
			HttpMessageFactory messageFactory) {
		return null;
	}

}

package sk.fiit.rabbit.adaptiveproxy.plugins.services.logging;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;

import sk.fiit.keyextractor.JKeyExtractor;
import sk.fiit.keyextractor.extractors.TagTheNetKeyExtractor;
import sk.fiit.rabbit.adaptiveproxy.plugins.PluginProperties;
import sk.fiit.rabbit.adaptiveproxy.plugins.helpers.AsynchronousResponseProcessingPluginAdapter;
import sk.fiit.rabbit.adaptiveproxy.plugins.messages.ModifiableHttpResponse;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.ServiceUnavailableException;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.ServicesHandle;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.cleartext.ClearTextExtractionService;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.common.Checksum;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.database.DatabaseConnectionProviderService;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.user.UserIdentificationService;

public class LoggingService extends AsynchronousResponseProcessingPluginAdapter {
	
	private static final Logger logger = Logger.getLogger(LoggingService.class);
	
	private String[] blacklist;

	@Override
	public boolean setup(PluginProperties props) {
		
		String blacklistLine = props.getProperty("blacklist", "");
		
		String[] extensions = blacklistLine.split(" ");
		blacklist = new String[extensions.length];
		
		int i = 0;
		for(String extension : extensions) {
			blacklist[i] = "." + extension.trim();
			i++;
		}
		
		return true;
	}
	
	@Override
	protected boolean prepareServices(ServicesHandle handle) {
		try {
			Connection con = handle.getService(DatabaseConnectionProviderService.class).getDatabaseConnection();
			String uid = handle.getService(UserIdentificationService.class).getClientIdentification();
			
			addToCache("connection", con);
			addToCache("uid", uid);
		} catch(ServiceUnavailableException e) {
			logger.error("prepare service " + e.getServiceClass().getName() + " failed, due to: " + e.getCause().getMessage());
			return false;
		}

		try {
			String clearText = handle.getService(ClearTextExtractionService.class).getCleartext();
			addToCache("clearText", clearText);
		} catch (ServiceUnavailableException e) {
			logger.trace("ClearTextExtractionService is unavailable", e);
		}
		
		return true;
	}

	@Override
	public void processResponseAsynchronously(ModifiableHttpResponse response) {
		String requestURI = response.getClientRequestHeaders().getRequestURI();

		if (!shouldLog(requestURI)) {
			return;
		}
		
		Connection con = (Connection) getFromCache("connection");
		String uid = (String) getFromCache("uid");
		String clearText = (String) getFromCache("clearText");
		
		String checksum = (clearText != null ? Checksum.md5(clearText) : null);
		
		Long pageId = getPageIdFromCache(requestURI, checksum, con);
		
		if(pageId == null) {
			pageId = savePageInformation(con, clearText, checksum, requestURI);
		}
		
		if(pageId != null) {
			log(con, uid, pageId);
		}
	}
	
	private Long savePageInformation(Connection con, String clearText, String checksum, String requestURI) {
		Integer contentLength = 0;
		
		if(clearText != null) {
			contentLength = clearText.length();
		}
		
		String query = "INSERT INTO pages(url, checksum, content_length, keywords) VALUES(?, ?, ?, ?)";
		
		try {
			PreparedStatement stmt = con.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
			stmt.setString(1, requestURI);
			stmt.setString(2, checksum);
			stmt.setInt(3, contentLength);
			stmt.setString(4, extractKeywords(clearText));
			
			stmt.execute();
			
			ResultSet keys = stmt.getGeneratedKeys();
			
			if(keys.next()) {
				return keys.getLong(1);
			} else {
				return null;
			}
		} catch (SQLException e) {
			logger.error("Could not save page information", e);
			return null;
		}
	}
	
	private Long getPageIdFromCache(String requestURI, String checksum, Connection con) {
		
		try {
			String query = "SELECT id FROM pages WHERE url = ?";
			
			if(checksum != null) {
				query += " AND checksum = ?";
			}
			
			PreparedStatement stmt = con.prepareStatement(query);
			stmt.setString(1, requestURI);
			
			if(checksum != null) {
				stmt.setString(2, checksum);
			}
	
			ResultSet rs = stmt.executeQuery();
			
			if(rs.next()) {
				return rs.getLong(1);
			} else {
				return null;
			}
		} catch(SQLException e) {
			logger.error("Could not load pageId from cache", e);
			return null;
		}
	}

	
	private String extractKeywords(String clearText) {
		
		if(clearText == null || clearText.trim() == "") {
			return "";
		}
		
		JKeyExtractor ke = new JKeyExtractor();
		ke.addAlgorithm(new TagTheNetKeyExtractor());
		
		List<String> l = ke.getAllKeysForText(clearText);
		String kws = Arrays.toString(l.toArray());
		return kws;
	}

	private void log(Connection connection, String userId, Long pageId) {

		try {
			PreparedStatement stmt = connection
					.prepareStatement("INSERT INTO access_logs(userid, timestamp, page_id) VALUES(?, ?, ?)");
			stmt.setString(1, userId);
			stmt.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
			stmt.setLong(3, pageId);
			
			stmt.execute();
		} catch (SQLException e) {
			logger.error("Could not save access log", e);
		}
	}
	
	private boolean shouldLog(String requestURL) {
		
		// nestaci spravit endsWith na requestURI lebo je celkom bezne za
		// resourcom sa kvoli cachovaniu nachadza este query string
		// napr. image.jpg?23454
		
		String fileName;
		
		int lastIdx = requestURL.lastIndexOf("?");
		if(lastIdx > 0) {
			fileName = requestURL.substring(0, lastIdx);
		} else {
			fileName = requestURL;
		}
		
		for (String extension : blacklist) {
			if (fileName.endsWith(extension)) {
				return false;
			}
		}

		return true;
	}

}

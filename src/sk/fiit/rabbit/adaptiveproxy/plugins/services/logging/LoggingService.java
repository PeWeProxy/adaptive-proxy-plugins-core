package sk.fiit.rabbit.adaptiveproxy.plugins.services.logging;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Arrays;

import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

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
	
	private static final Logger log = Logger.getLogger(LoggingService.class);
	
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
			return false;
		}

		try {
			String clearText = handle.getService(ClearTextExtractionService.class).getCleartext();
			addToCache("clearText", clearText);
		} catch (ServiceUnavailableException e) {
			// toto nie je dolezita sluzba, keywordy sa nezaloguju
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
		
		String keywords = "";
		try {
			keywords = getKeywords(requestURI, clearText, con);
		} catch (Exception e) {
			log.warn("Keyword extracting error: " + e);
		} 

		log.info("keywords - " + keywords + " : " + response.getProxyRequestHeaders().getRequestURI());

		log(con, uid, requestURI, keywords);	

			
	}
	
	private String getKeywords(String requestURI,
			                   String clearText, 
			                   Connection connection) {
		
		if(clearText == null) {
			return "";
		}
		
		String checksum = Checksum.md5(clearText);
		
		String keywords = getKeywordsFromCache(checksum, connection);
		
		if(keywords == null) {
			log.debug("Keywords Cache MISS");
			
			keywords = extractKeywords(requestURI);
			
			if(keywords != null) {
				saveKeywords(keywords, checksum, connection);
			}
		} else {
			log.debug("Keywords Cache HIT");
		}

		return keywords;
	}
	
	private String extractKeywords(String url) {
		String reqURL;
		try {
			reqURL = "http://www.tagthe.net/api?url=" + URLEncoder.encode(url, "UTF-8") + "&view=json";
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
			return null;
		}
		
		JSONObject obj = null;
		try {
			obj = (JSONObject) new JSONParser().parse(new InputStreamReader(new URL(reqURL).openStream()));
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		JSONArray arr = (JSONArray) obj.get("memes");
		if(arr == null) return null;
		obj = (JSONObject) arr.get(0);
		if(obj == null) return null;
		obj = (JSONObject) obj.get("dimensions");
		if(obj == null) return null;
		arr = (JSONArray) obj.get("topic");
		if(arr == null) return null;
		
		return Arrays.toString(arr.toArray());
	}

	private String getKeywordsFromCache(String checksum, Connection con) {
		PreparedStatement stmt;
		try {
			stmt = con.prepareStatement("SELECT keywords FROM keyword_cache WHERE checksum = ?");
			stmt.setString(1, checksum);
			
			ResultSet rs = stmt.executeQuery();
			
			if(rs.next()) {
				return rs.getString(1);
			} else {
				return null;
			}
				
		} catch (SQLException e) {
			return null;
		}
	}
	
	private void saveKeywords(String keywords, String checksum,  Connection con) {
		PreparedStatement stmt;
		try {
			stmt = con.prepareStatement("INSERT INTO keyword_cache(checksum, keywords) VALUES(?, ?)");
			stmt.setString(1, checksum);
			stmt.setString(2, keywords);
			
			stmt.execute();
		} catch (SQLException e) {
			log.error("Error inserting keywords into cache: " + e.getMessage());
		}
	}

	private void log(Connection connection, String userId, String requestURL, String keywords) {

		try {
			PreparedStatement stmt = connection
					.prepareStatement("INSERT INTO access_log(userid, timestamp, url, keywords) VALUES(?, ?, ?, ?)");
			stmt.setString(1, userId);
			stmt.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
			stmt.setString(3, requestURL);
			stmt.setString(4, keywords);
			
			stmt.execute();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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

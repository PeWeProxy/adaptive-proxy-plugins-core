package sk.fiit.rabbit.adaptiveproxy.plugins.services.bifrost;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import sk.fiit.bifrost.Disambiguator;
import sk.fiit.bifrost.dunco.Document;
import sk.fiit.bifrost.dunco.FetchException;
import sk.fiit.bifrost.dunco.WebSearch;
import sk.fiit.bifrost.dunco.google.GoogleWebSearch;
import sk.fiit.bifrost.recommendations.Context;
import sk.fiit.bifrost.recommendations.KeywordsCompletionStrategy;
import sk.fiit.bifrost.recommendations.QueryRedefinitionsRecommender;
import sk.fiit.bifrost.recommendations.RecommendationStrategy;
import sk.fiit.rabbit.adaptiveproxy.plugins.PluginProperties;
import sk.fiit.rabbit.adaptiveproxy.plugins.messages.HttpMessageFactory;
import sk.fiit.rabbit.adaptiveproxy.plugins.messages.HttpResponse;
import sk.fiit.rabbit.adaptiveproxy.plugins.messages.ModifiableHttpRequest;
import sk.fiit.rabbit.adaptiveproxy.plugins.messages.ModifiableHttpResponse;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.ServiceUnavailableException;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.common.SqlUtils;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.content.ModifiableStringService;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.database.DatabaseConnectionProviderService;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.injector.JavaScriptInjectingProcessingPlugin;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.user.UserIdentificationService;

public class BifrostProcessingPlugin extends JavaScriptInjectingProcessingPlugin {
	
	private String recommendationUrlBase;
	private Integer maxRecommendedDocuments;
	private Integer maxDocumentsFromQuery;
	
	@Override
	public HttpResponse getResponse(ModifiableHttpRequest proxyResponse, HttpMessageFactory messageFactory) {
		String query = extractQueryFromURI(proxyResponse.getClientRequestHeaders().getRequestURI());
		
		Context context = new Context();
		context.setLastQuery(query);
		context.setIpAddress(proxyResponse.getClientSocketAddress().getAddress().getHostAddress());
		
		Collection<Document> resultDocuments = new LinkedList<Document>();
		
		ModifiableHttpResponse response = messageFactory.constructHttpResponse("text/html");
		
		Connection connection = null;

		try {
			connection = response.getServiceHandle().getService(DatabaseConnectionProviderService.class).getDatabaseConnection();
			String userId = response.getServiceHandle().getService(UserIdentificationService.class).getClientIdentification();
			
			Disambiguator disambiguator = new Disambiguator();
			resultDocuments = disambiguator.search(context, maxRecommendedDocuments, maxDocumentsFromQuery);

			for (Document document : resultDocuments) {
				Long recommendationId = logRecommendation(connection, userId, query, document.getRewrittenQuery(), document.getDisplayUrl(), document.getRecommenderStrategy());
				document.setRecommendationUrl(recommendationUrlBase + recommendationId);
			}
			String resultHtml = GoogleResultsFormatter.format(resultDocuments);
			
			ModifiableStringService mss = response.getServiceHandle().getService(ModifiableStringService.class);
			mss.setCharset(Charset.forName("UTF-8"));
			mss.setContent(resultHtml);
		} catch (ServiceUnavailableException e) {
			logger.error("ModifiableStringService is unavailable, cannot generate new response");
		} catch (SQLException e) {
			logger.error("Could not save recommended query", e);
		} finally {
			SqlUtils.close(connection);
		}
		
		return response;
	}
	
	private Long logRecommendation(Connection con, String userid, String originalQuery, String recommendedQuery, String recommendedUrl, String method) throws SQLException {
		String insert = "INSERT INTO bf_recommendations(userid, timestamp, original_query, recommended_query, recommended_url, method) " +
						"VALUES(?, ?, ?, ?, ?, ?)";
		PreparedStatement stmt = con.prepareStatement(insert, PreparedStatement.RETURN_GENERATED_KEYS);
		stmt.setString(1, userid);
		stmt.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
		stmt.setString(3, originalQuery);
		stmt.setString(4, recommendedQuery);
		stmt.setString(5, recommendedUrl);
		stmt.setString(6, method);
		
		stmt.execute();
		
		ResultSet keys = stmt.getGeneratedKeys();
		if(keys.next()) {
			return keys.getLong(1);
		} else {
			throw new RuntimeException("Could not load auto generated keys");
		}
	}
	
	private String extractQueryFromURI(String uri) {
		Pattern pattern = Pattern.compile(".*[?&]q=(.*?)(:?$|&.*)");
		Matcher matcher = pattern.matcher(uri);
		if(matcher.matches()) {
			return matcher.group(1).replace("%2B", " ").replace("+", " "); 
		} else {
			return null;
		}
	}
	
	@Override
	public boolean setup(PluginProperties props) {
		if(!super.setup(props)) {
			return false;
		}
		
		recommendationUrlBase = props.getProperty("recommendationUrlBase");
		maxRecommendedDocuments = props.getIntProperty("maxRecommendedDocuments", 4);
		maxDocumentsFromQuery = props.getIntProperty("maxDocumentsFromQuery", 2);
		
		return true;
	}
}

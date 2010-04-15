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
	
	private WebSearch searcher = new GoogleWebSearch();
	private RecommendationStrategy coKeywordsStrategy = new KeywordsCompletionStrategy();
	private QueryRedefinitionsRecommender queryRedefinitionStrategy = new QueryRedefinitionsRecommender();
	
	private String recommendationUrlBase;
	private Integer maxRecommendedDocuments;
	private Integer maxDocumentsFromQuery;
	private String recommenderId;
	
	@Override
	public HttpResponse getResponse(ModifiableHttpRequest proxyResponse, HttpMessageFactory messageFactory) {
		String query = extractQueryFromURI(proxyResponse.getClientRequestHeaders().getRequestURI());
		
		Context context = new Context();
		context.setLastQuery(query);
		
		RecommendationStrategy recommender = selectRecommender();
		Collection<String> queries = recommender.recommendQueries("TODO", context); //TODO
		Collection<Document> resultDocuments = new LinkedList<Document>();
		Set<String> recommendedDomains = new HashSet<String>();
		
		ModifiableHttpResponse response = messageFactory.constructHttpResponse(true);
		
		Connection connection = null;
		
		try {
			connection = response.getServiceHandle().getService(DatabaseConnectionProviderService.class).getDatabaseConnection();
			String userId = response.getServiceHandle().getService(UserIdentificationService.class).getClientIdentification();
			
			int recommendedDocumentCount = 0;
			for(String q : queries) {
				if(recommendedDocumentCount >= maxRecommendedDocuments) break;
				try {
					int documentCount = 0;
					for(Document doc : searcher.search(q, response.getClientSocketAddress().getAddress().getHostAddress())) {
						if(documentCount >= maxDocumentsFromQuery || recommendedDocumentCount >= maxRecommendedDocuments) break;
						try {
							String host = new URL(doc.getDisplayUrl()).getHost();
							if(host.startsWith("www.")) {
								host = host.substring(4);
							}
							if(!recommendedDomains.contains(host)) {
								recommendedDomains.add(host);
								Long recommendationId = logRecommendation(connection, userId, query, q, doc.getDisplayUrl(), "querystream");
								doc.setRecommendationUrl(recommendationUrlBase + recommendationId);
								doc.setRewrittenQuery(q);
								resultDocuments.add(doc);
								documentCount++;
								recommendedDocumentCount++;
							}
						} catch (MalformedURLException e) {
							logger.warn("Cannot recommend document, the URL is invalid", e);
							continue;
						}
					}
				} catch (FetchException e) {
					logger.warn("Could not fetch search results for query: " + q, e);
				}
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
	
	private RecommendationStrategy selectRecommender() {
		if ("cokeywords".equals(recommenderId)) {
			return coKeywordsStrategy;
		} else if("querystreams".equals(recommenderId)) {
			return queryRedefinitionStrategy;
		} else {
			throw new RuntimeException("Unknown recommender: " + recommenderId);
		}
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
		
		recommenderId = props.getProperty("recommender");
		
		return true;
	}
}

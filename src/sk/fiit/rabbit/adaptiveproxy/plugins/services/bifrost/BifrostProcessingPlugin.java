package sk.fiit.rabbit.adaptiveproxy.plugins.services.bifrost;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
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
import sk.fiit.rabbit.adaptiveproxy.plugins.messages.HttpMessageFactory;
import sk.fiit.rabbit.adaptiveproxy.plugins.messages.HttpResponse;
import sk.fiit.rabbit.adaptiveproxy.plugins.messages.ModifiableHttpRequest;
import sk.fiit.rabbit.adaptiveproxy.plugins.messages.ModifiableHttpResponse;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.ServiceUnavailableException;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.content.ModifiableStringService;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.injector.JavaScriptInjectingProcessingPlugin;

public class BifrostProcessingPlugin extends JavaScriptInjectingProcessingPlugin {
	
	private WebSearch searcher = new GoogleWebSearch();
	private RecommendationStrategy coKeywordsStrategy = new KeywordsCompletionStrategy();
	private QueryRedefinitionsRecommender queryRedefinitionStrategy = new QueryRedefinitionsRecommender();
	
	private static final int MAX_RECOMMENDED_DOCUMENTS = 4;
	private static final int MAX_DOCUMENTS_FROM_QUERY = 2;
	
	@Override
	public HttpResponse getResponse(ModifiableHttpRequest proxyResponse, HttpMessageFactory messageFactory) {
		String query = extractQueryFromURI(proxyResponse.getClientRequestHeaders().getRequestURI());
		
		Context context = new Context();
		context.setLastQuery(query);
		
		Collection<String> queries = queryRedefinitionStrategy.recommendQueries("TODO", context); //TODO
		Collection<Document> resultDocuments = new LinkedList<Document>();
		Set<String> recommendedDomains = new HashSet<String>();
		
		ModifiableHttpResponse response = messageFactory.constructHttpResponse(true);
		
		try {
			int recommendedDocumentCount = 0;
			for(String q : queries) {
				if(recommendedDocumentCount >= MAX_RECOMMENDED_DOCUMENTS) break;
				try {
					int documentCount = 0;
					for(Document doc : searcher.search(q)) {
						if(documentCount >= MAX_DOCUMENTS_FROM_QUERY || recommendedDocumentCount >= MAX_RECOMMENDED_DOCUMENTS) break;
						try {
							String host = new URL(doc.getUrl()).getHost();
							if(host.startsWith("www.")) {
								host = host.substring(4);
							}
							if(!recommendedDomains.contains(host)) {
								recommendedDomains.add(host);
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
		}
		
		return response;
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
}

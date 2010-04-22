package sk.fiit.rabbit.adaptiveproxy.plugins.services.bifrost;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import sk.fiit.bifrost.dunco.Document;

public class GoogleResultsFormatter {
    
	public static String format(Collection<Document> documents) {
		String html = "";

		Set<String> queries = new HashSet<String>();
		
		for(Document doc : documents) {
			queries.add(doc.getRewrittenQuery());
			
			html+= 
				"<li class='g w0'>" +
					"<h3 class='r'>" +
						"<a class='l' href='" + doc.getRecommendationUrl() +  "'>" + doc.getTitle() + "</a>" +
					"</h3>" +
					"<div class='s'>" + 
						doc.getContent() + 
						"<br/>" +
						"<cite>" + doc.getVisibleUrl() + " - </cite>" +
						"<span class='gl'><a href='" + doc.getCacheUrl() + "'>Cached</a></span>" +
					"</div>" +
				"</li>";
		}
		
		if(!"".equals(html)) {
			
			String usedQueries = "";
			for(String query : queries) {
				try {
					usedQueries += "<b><a href='search?q=" + URLEncoder.encode(query, "UTF-8") + "#dontrecommend'>" + query + "</a></b>,&nbsp;";
				} catch (UnsupportedEncodingException e) {
					// no-op
				}
			}
			
			usedQueries = usedQueries.substring(0, usedQueries.length() - 8);
			
			String headerHtml = 
				"<li>" +
					"<p style='margin-top: 0pt;' class='g'>" +
						"<span class='med'>Results provided by peweproxy:&nbsp;" + usedQueries + "</span>" +
					"</p>" +
				"</li>";
			
			html = headerHtml + html + "<li><hr color='#c9d7f1' align='left' width='65%' size='1'></li>";
		}
		
		return html;
	}
}

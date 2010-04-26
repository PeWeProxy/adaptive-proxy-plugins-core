package sk.fiit.rabbit.adaptiveproxy.plugins.services.bifrost;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import sk.fiit.bifrost.dunco.Document;

public class GoogleResultsFormatter {
    
	public static String format(Collection<Document> documents, String originalQuery,
			String queryGroupUrl, String negativeFeedbackUrl, String assetsUrl) {
		String html = "";

		Set<String> queries = new HashSet<String>();
		
		for(Document doc : documents) {
			queries.add(doc.getRewrittenQuery());
			
			try {
				html+= 
					"<li class='g w0'>" +
						"<h3 class='r'>" +
							"<a class='l' href='" + doc.getRecommendationUrl() +  "'>" + doc.getTitle() + "</a>" +
							"&nbsp;<a href='" + negativeFeedbackUrl + "?q=" + URLEncoder.encode(doc.getRewrittenQuery(), "UTF-8") + "'><img style='border: none;' src='" + assetsUrl + "delete.png' title=\"This is not what I've been looking for\"/></a>" +
						"</h3>" +
						"<div class='s'>" + 
							doc.getContent() + 
							"<br/>" +
							"<cite>" + URLDecoder.decode(doc.getDisplayUrl(), "UTF-8") + " - </cite>" +
							"<span class='gl'><a href='" + doc.getCacheUrl() + "'>Cached</a></span>" +
						"</div>" +
					"</li>";
			} catch (UnsupportedEncodingException e) {
				// no-op
			}
		}
		
		if(!"".equals(html)) {
			
			String usedQueries = "";
			for(String query : queries) {
				try {
					usedQueries += 
						"<b>" +
							"<a href='" + queryGroupUrl + "?q=" + URLEncoder.encode(query, "UTF-8") + "'>" + query + "</a>" +
						"</b>" +
						"<a href='" + negativeFeedbackUrl + "?q=" + URLEncoder.encode(query, "UTF-8") + "'><img style='border: none;' src='" + assetsUrl + "delete.png' title=\"This is not what I've been looking for\"/></a>,&nbsp;";
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
			html += "<li><p style='margin-top: 0pt;' class='g'><span class='med'>Results for:&nbsp;<b>" + originalQuery + "</b></span></p></li>";
		}
		
		return html;
	}
}

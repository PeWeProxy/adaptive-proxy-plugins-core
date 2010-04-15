package sk.fiit.rabbit.adaptiveproxy.plugins.services.bifrost;

import java.util.Collection;

import sk.fiit.bifrost.dunco.Document;

public class GoogleResultsFormatter {
    
	public static String format(Collection<Document> documents) {
		String html = "";
		
		for(Document doc : documents) {
			html+= 
				"<li class='g w0'>" +
					"<h3 class='r'>" +
						"<a class='l' href='" + doc.getRecommendationUrl() +  "'>" + doc.getTitle() + "</a>" +
					"</h3>" +
					"<div class='s'>" + 
						doc.getContent() + 
						"<br/>" +
						"<cite>" + doc.getDisplayUrl() + "</cite>" +
					"</div>" +
				"</li>";
		}
		
		html = "<div style='border: 1px solid green;'>" + html + "</div>";
		
		return html;
	}
}

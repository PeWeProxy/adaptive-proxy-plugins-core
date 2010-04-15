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
					"<span style='display: inline-block;'><button title='' class='ws'></button></span>" +
					"<div class='s'>" + 
						doc.getContent() + 
						"<br/>" +
						"<cite>" + doc.getVisibleUrl() + " - </cite>" +
						"<span class='gl'><a href='" + doc.getCacheUrl() + "'>Cached</a></span>" +
					"</div>" +
				"</li>";
		}
		
		if(!"".equals(html)) {
			html = "<div style='border-bottom: 1px solid green; margin-bottom: 10px;'>" + html + "</div>";
		}
		
		return html;
	}
}

/*
 * Website Navigation Adaptation Based on Behavior of Users
 * Master Thesis
 * Bc. Michal Holub
 * 
 * Faculty of Informatics and Information Technologies
 * Slovak University of Technology
 * Bratislava, 2008 - 2010  
 */
package sk.fiit.rabbit.adaptiveproxy.plugins.services.webimp.newssection;


/**
 * Class that creates news section personalized to specific user. 
 */
public class NewsSection {
	
	/**
	 * Creates HTML code of news section for a given web page source.
	 * 
	 * @param page web page to which we want to add news section
	 * @return HTML code of a news section
	 */
	public String getNewsSectionCode() {		
		String htmlCode = "<div class=\"box_nadpis\">\n" 
			+ "osobné novinky <span>›</span>\n"
			+ "</div>\n";
		
//		htmlCode = htmlCode  
//			+ "<div class=\"box\">\n"
//			+ "<ul>\n"
//			+ "<li>\n"
//			+ "<a title=\"" + heading + "\" href=\"" + page.getUrl() + "\">"
//			+ heading + "</a>\n"
//			+ "</li>\n"
//			+ "</ul>\n"
//			+ "</div>\n";
		
		return htmlCode;
	}
}

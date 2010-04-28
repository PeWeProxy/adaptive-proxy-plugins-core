/*
 * Website Navigation Adaptation Based on Behavior of Users

 * Master Thesis
 * Bc. Michal Holub
 * 
 * Faculty of Informatics and Information Technologies
 * Slovak University of Technology
 * Bratislava, 2008 - 2010  
 */
package sk.fiit.rabbit.adaptiveproxy.plugins.services.webimp.feedback;

public class Feedback {
	public static String getCode(final String directory) {
		String code = "<div id=\"wiFeedback\"><img id=\"wiLike\" style=\"cursor:pointer\" src=\"" + directory + "/like.png\" title=\"Zaujala\" alt=\"Stránka ma zaujala\" width=\"32\" height=\"32\"/>";
		code += "<img id=\"wiDislike\" style=\"cursor:pointer\" src=\"" + directory + "/dislike.png\" title=\"Nezaujala\" alt=\"Stránka ma nezaujala\" width=\"32\" height=\"32\"/></div>";
		
		return code;
	}
}

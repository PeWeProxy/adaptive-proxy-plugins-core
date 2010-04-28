package sk.fiit.rabbit.adaptiveproxy.plugins.services.webimp.feedback;

public class Feedback {
	public static String getCode(final String directory) {
		String code = "<div id=\"wiFeedback\"><img id=\"wiLike\" style=\"cursor:pointer\" src=\"" + directory + "/like.png\" title=\"Zaujala\" alt=\"Stránka ma zaujala\" width=\"32\" height=\"32\"/>";
		code += "<img id=\"wiDislike\" style=\"cursor:pointer\" src=\"" + directory + "/dislike.png\" title=\"Nezaujala\" alt=\"Stránka ma nezaujala\" width=\"32\" height=\"32\"/></div>";
		
		return code;
	}
}

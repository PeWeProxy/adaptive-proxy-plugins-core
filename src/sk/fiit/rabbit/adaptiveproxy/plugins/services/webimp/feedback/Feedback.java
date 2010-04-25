package sk.fiit.rabbit.adaptiveproxy.plugins.services.webimp.feedback;

public class Feedback {
	public static String getCode(final String directory) {
		String code = "<img style=\"cursor:pointer\" src=\"" + directory + "/like.png\" onClick=\"wiFeedback.wiSendFeedback(1);\" alt=\"Stránka ma zaujala\" width=\"30\" height=\"30\"/>";
		code += "<img style=\"cursor:pointer\" src=\"" + directory + "/dislike.png\" onClick=\"wiFeedback.wiSendFeedback(-1);\" alt=\"Stránka ma nezaujala\" width=\"30\" height=\"30\"/>";
		/*
		String code = "<a href=\"www.fiit.sk\">";
		code += "<img src='" + directory + "/star-bad.png' alt='Úplne nezaujala' width='15' height='15'/>";
		code += "</a>";
		
		code += "<a href=\"www.fiit.sk\">";
		code += "<img src='" + directory + "/star-bad.png' alt='Skôr nezaujala' width='15' height='15'/>";
		code += "</a>";
		
		code += "<a href=\"www.fiit.sk\">";
		code += "<img src='" + directory + "/star-good.png' alt='Skôr zaujala' width='15' height='15'/>";
		code += "</a>";
		
		code += "<a href=\"www.fiit.sk\">";
		code += "<img src='" + directory + "/star-good.png' alt='Veľmi zaujala' width='15' height='15'/>";
		code += "</a>";		
		*/
		return code;
	}
}

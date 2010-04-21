package sk.fiit.rabbit.adaptiveproxy.plugins.services.webimp.feedback;

public class Feedback {
	public static String getCode(final String directory) {
		/*
		String code = "<a href=\"www.fiit.sk\">";
		code += "<img src=\"http://miho.mine.nu/WebImp/like.png\" alt=\"Odporúčam stránku\" width=\"30\" height=\"30\"/>";
		code += "</a>";
		
		code += "<a href=\"www.fiit.sk\">";
		code += "<img src=\"http://miho.mine.nu/WebImp/dislike.png\" alt=\"Neodporúčam stránku\" width=\"30\" height=\"30\"/>";
		code += "</a>";
		*/		
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
		
		return code;
	}
}

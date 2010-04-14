/*
 * Website Navigation Adaptation Based on Behavior of Users
 * Master Thesis
 * Bc. Michal Holub
 * 
 * Faculty of Informatics and Information Technologies
 * Slovak University of Technology
 * Bratislava, 2008 - 2010  
 */
package sk.fiit.rabbit.adaptiveproxy.plugins.services.webimp;

import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import sk.fiit.rabbit.adaptiveproxy.plugins.PluginProperties;
import sk.fiit.rabbit.adaptiveproxy.plugins.headers.ResponseHeaders;
import sk.fiit.rabbit.adaptiveproxy.plugins.messages.HttpMessageFactory;
import sk.fiit.rabbit.adaptiveproxy.plugins.messages.HttpResponse;
import sk.fiit.rabbit.adaptiveproxy.plugins.messages.ModifiableHttpResponse;
import sk.fiit.rabbit.adaptiveproxy.plugins.processing.ResponseProcessingPlugin;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.ServiceUnavailableException;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.content.ModifiableStringService;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.webimp.calendar.PersonalizedCalendar;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.webimp.feedback.Feedback;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.webimp.newssection.NewsSection;

/**
 * Plugin for the adaptive proxy server that processes the response packets.
 * It implements the method of improving website structure. This method
 * is the result of a master thesis.
 */
public class WebImpProcessingPlugin implements ResponseProcessingPlugin {
	
	/**
	 * URI of current HTTP response item.
	 */
	private String uri;
	
	/**
	 * URL of the domain in which the processing plugin works. We adapt only content
	 * of responses that come from this domain (e.g. only faculty sites are 
	 * personalized, other sites remain unaffected).
	 */
	private String domainUrl;
	
	/**
	 * URL of the script that sends explicit feedback from user to the server. 
	 * This script is added to the header of the web page.
	 */
	private String feedbackScriptUrl;
	
	private String imagesUrl;
	private String stylesheetsUrl;
	
	static Logger log = Logger.getLogger(WebImpProcessingPlugin.class);
	
	public WebImpProcessingPlugin() {
	}
	
	@Override
	public HttpResponse getNewResponse(ModifiableHttpResponse response,
			HttpMessageFactory messageFactory) {
		// TODO Auto-generated method stub
		return null;
	}
	
	/**
	 * Modifies the response packets by adding personalized content to the
	 * source code of the web page. At first it adds JavaScripts that
	 * monitor user behavior. Then it inserts personalized calendar to the
	 * web page.
	 */
	@Override
	public ResponseProcessingActions processResponse(ModifiableHttpResponse response) {
		
		uri = response.getClientRequestHeaders().getRequestURI();
		
		// modify page only if it is from domain of our interest
		if (!uri.contains(domainUrl)) {
			return ResponseProcessingActions.PROCEED;
		}
		
		ModifiableStringService mss = null;
		try {
			mss = response.getServiceHandle().getService(ModifiableStringService.class);
			StringBuilder sb = mss.getModifiableContent();
			String content = sb.toString();
			
			int headerEnd = content.toLowerCase().indexOf("</head>");
			if(headerEnd < 0) {
				log.debug("No </head> on page : " + response.getProxyRequestHeaders().getRequestURI());
				return ResponseProcessingActions.PROCEED;
			}
			sb.insert(headerEnd, getFeedbackScriptTag());	
			sb.insert(headerEnd, getCssTag());
			
			content = sb.toString();
			String rightMenu = "<div id=\"content_right\">";
			int rightMenuIdx = content.toLowerCase().indexOf(rightMenu);
			if (rightMenuIdx < 0) {
				log.debug("No right menu on page : " + response.getProxyRequestHeaders().getRequestURI());
				return ResponseProcessingActions.PROCEED;
			}
			sb.insert(rightMenuIdx + rightMenu.length(), getPersonalizedCalendar());
			
			Document doc = Jsoup.parse(sb.toString(), uri);
			Element div = doc.select("div[class=print_button]").first();
			if (div != null) {
				div.html(Feedback.getCode(imagesUrl));
				String printBtnStartCode = "<div class=\"print_button\">";
				String printBtnEndCode = "</div>";
				content = sb.toString();
				int iStart = content.indexOf(printBtnStartCode);
				int iEnd = content.indexOf(printBtnEndCode, iStart);
				sb.replace(iStart, iEnd + printBtnEndCode.length(), div.outerHtml());
			}
			mss.setContent(sb.toString());
		} catch (ServiceUnavailableException e) {
			log.warn("ModifiableStringService is unavailable. Plugin cannot modify the page content.");
		}
		
		return ResponseProcessingActions.PROCEED;
	}
	
	@Override
	public boolean wantResponseContent(ResponseHeaders webRPHeaders) {
		// TODO Auto-generated method stub
		return true;	// we want to access the content of response to modify it
	}
	
	@Override
	public boolean setup(PluginProperties props) {
		feedbackScriptUrl = props.getProperty("feedbackScriptUrl");
		domainUrl = props.getProperty("domainUrl");
		imagesUrl = props.getProperty("imagesUrl");
		stylesheetsUrl = props.getProperty("stylesheetsUrl");
		
		return true;
	}
	
	@Override
	public void start() {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void stop() {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public boolean supportsReconfigure() {
		// TODO Auto-generated method stub
		return true;
	}
	
	private String getCssTag() {
		return "<link rel='stylesheet' type='text/css' href='" + stylesheetsUrl + "/wi_calendar_style.css'" + "/>";
	}
	
	private String getFeedbackScriptTag() {
		return "<script type='text/javascript' src='" + feedbackScriptUrl + "'></script>";
	}
	
	/**
	 * @return HTML source code of the personalized calendar
	 */
	private String getPersonalizedCalendar() {
		PersonalizedCalendar cal = new PersonalizedCalendar();
		return cal.getCalendarCode();
	}
	
	/**
	 * @return HTML source code of the news section
	 */
	private String getPersonalizedNews() {
		NewsSection news = new NewsSection();
		return news.getNewsSectionCode(); 
	}
}
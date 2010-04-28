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
import sk.fiit.rabbit.adaptiveproxy.plugins.services.database.DatabaseConnectionProviderService;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.injector.HtmlInjectorService;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.injector.HtmlInjectorService.HtmlPosition;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.user.UserIdentificationService;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.webimp.calendar.PersonalizedCalendar;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.webimp.feedback.Feedback;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.webimp.newssection.NewsSection;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.webimp.utils.StructureReader;

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
	private String scriptsUrl;
	
	private String imagesUrl;
	private String stylesheetsUrl;
	
	private String portalStructureFile;
	
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
	 * source code of the web page. 
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
			// insert JavaScripts
			try {
				HtmlInjectorService htmlInjectionService = response.getServiceHandle().getService(HtmlInjectorService.class);				
				htmlInjectionService.inject(
						getYahooScriptTag()
						+ getBalloonScriptTag()
						+ getBalloonConfScriptTag()
						+ getFeedbackScriptTag(), HtmlPosition.ON_MARK);
			} catch (ServiceUnavailableException e) {
				log.warn("HtmlInjectorService is unavailable, JavaScriptInjector takes no action");
			}
						
			mss = response.getServiceHandle().getService(ModifiableStringService.class);
			StringBuilder sb = mss.getModifiableContent();
			String content = sb.toString();
			
			int headerEnd = content.toLowerCase().indexOf("</head>");
			if(headerEnd < 0) {
				log.debug("No </head> on page : " + response.getProxyRequestHeaders().getRequestURI());
				return ResponseProcessingActions.PROCEED;
			}
			sb.insert(headerEnd, getCssTag());	// CSS for calendar
			
			content = sb.toString();
			StructureReader sr = new StructureReader(portalStructureFile);
			try {
				sr.readWebStructure("menuRight");
			} catch (Exception e) {
				log.error("Unable to modify the web page content.");
				return ResponseProcessingActions.PROCEED;
			}
			String rightMenu = "<" + sr.getTag() + " " + sr.getType() 
				+ "=\"" + sr.getValue() + "\">";
			
			int rightMenuIdx = content.toLowerCase().indexOf(rightMenu);
			if (rightMenuIdx < 0) {
				log.debug("No right menu on page : " + response.getProxyRequestHeaders().getRequestURI());
				return ResponseProcessingActions.PROCEED;
			}
			
			UserIdentificationService userIdentification = null;
			try {
				userIdentification = response.getServiceHandle().getService(UserIdentificationService.class);				
			} catch (ServiceUnavailableException sue) {
				log.error("User identification service unavailable, cannot get user agent ID.");
			}
			String uid = userIdentification.getClientIdentification();

			DatabaseConnectionProviderService dbService = null;
			try {
				dbService = response.getServiceHandle().getService(DatabaseConnectionProviderService.class);
			} catch (ServiceUnavailableException seu) {
				log.error("Database service unavailable, cannot connect to database.");
			}
			sb.insert(rightMenuIdx + rightMenu.length(), 
					getPersonalizedCalendar(uid, dbService)
					+ getPersonalizedNews(uid, dbService));
			
			Document doc = Jsoup.parse(sb.toString(), uri);
			Element div = doc.select("div[class=print_button]").first();			
			if (div != null) {
				div.html(Feedback.getCode(imagesUrl));
				try {
					sr.readWebStructure("print");
				} catch (Exception e) {
					log.fatal("Unable to add buttons for feedback.");
					return ResponseProcessingActions.PROCEED;
				}
				String printBtnStartCode = "<" + sr.getTag() + " " + sr.getType() 
					+ "=\"" + sr.getValue() + "\">";
				String printBtnEndCode = "</" + sr.getTag() + ">";
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
		scriptsUrl = props.getProperty("scriptsUrl");
		domainUrl = props.getProperty("domainUrl");
		imagesUrl = props.getProperty("imagesUrl");
		stylesheetsUrl = props.getProperty("stylesheetsUrl");
		portalStructureFile = props.getProperty("portalStructureFile");
		
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
	
	private String getYahooScriptTag() {
		return "<script type='text/javascript' src='" + scriptsUrl + "/wi_yahoo-dom-event.js'></script>";
	}
	
	private String getBalloonScriptTag() {
		return "<script type='text/javascript' src='" + scriptsUrl + "/wi_balloon.js'></script>";
	}
	
	private String getBalloonConfScriptTag() {
		return "<script type='text/javascript' src='" + scriptsUrl + "/wi_balloon-config.js'></script>";		
	}
	
	private String getFeedbackScriptTag() {
		return "<script type='text/javascript' src='http://miho.mine.nu/WebImp/wi_feedback.js'></script>";
		//return "<script type='text/javascript' src='" + scriptsUrl + "/wi_feedback.js'></script>";
	}
	
	private String getCssTag() {
		return "<link rel='stylesheet' type='text/css' href='" + stylesheetsUrl + "/wi_calendar_style.css'" + "/>";
	}	
	
	/**
	 * @param userId value of user agent ID for current user
	 * @param dbService service for working with database 
	 * @return HTML source code of the personalized calendar for given user
	 */
	private String getPersonalizedCalendar(final String userId, 
			final DatabaseConnectionProviderService dbService) {
		PersonalizedCalendar cal = new PersonalizedCalendar();
		return cal.getCalendarCode(userId, dbService);
	}
	
	/**
	 * @param userId value of user agent ID for current user
	 * @param dbService service for working with database
	 * @return HTML source code of the news section for given user
	 */
	private String getPersonalizedNews(final String userId,
			final DatabaseConnectionProviderService dbService) {
		NewsSection news = new NewsSection();
		return news.getNewsSectionCode(userId, dbService); 
	}
}
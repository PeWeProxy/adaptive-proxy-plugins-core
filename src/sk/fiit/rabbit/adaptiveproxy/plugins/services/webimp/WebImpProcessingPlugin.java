/*
 * Website Navigation Adaptation Based on Behavior of Users
 * 
 * Master Thesis
 * Bc. Michal Holub
 * 
 * Faculty of Informatics and Information Technologies
 * Slovak University of Technology
 * Bratislava, 2008 - 2010  
 */
package sk.fiit.rabbit.adaptiveproxy.plugins.services.webimp;

import java.io.FileNotFoundException;

import net.htmlparser.jericho.MasonTagTypes;
import net.htmlparser.jericho.MicrosoftTagTypes;
import net.htmlparser.jericho.PHPTagTypes;
import net.htmlparser.jericho.Source;

import org.apache.log4j.Logger;
import org.dom4j.DocumentException;
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
import sk.fiit.rabbit.adaptiveproxy.plugins.services.webimp.exceptions.PageContentException;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.webimp.feedback.Feedback;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.webimp.links.PersonalizedLinks;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.webimp.newssection.NewsSection;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.webimp.structure.StructureReader;

/**
 * Plugin for the adaptive proxy server that processes the response packets.
 * It implements the method of improving website structure. This method
 * is the result of a master thesis.
 */
public class WebImpProcessingPlugin implements ResponseProcessingPlugin {
	
	/**
	 * HTML tag that is used to determine the end of head section of the page.
	 */
	private final String HEADER_END_TAG = "</head>";
	
	/**
	 * HTML tag that is used to determine the end of div section.
	 */
	private final String DIV_END_TAG = "</div>";
	
	/**
	 * URI of current HTTP response item.
	 */
	private String actualUri;
	
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
	
	/**
	 * Reader for the XML file describing the structure of the web portal.
	 */
	private StructureReader structRead;
	
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
		// save URI of current web page
		actualUri = response.getClientRequestHeaders().getRequestURI();
				
		// modify page only if it is from domain of our interest
		if (!actualUri.contains(domainUrl)) {
			return ResponseProcessingActions.PROCEED;
		}
		
		ModifiableStringService mss = null;
		try {
			// insert JavaScripts (balloon tooltips, explicit feedback)
			try {
				HtmlInjectorService htmlInjectionService = response.getServiceHandle().getService(HtmlInjectorService.class);				
				htmlInjectionService.inject(
						getYahooScriptTag()
						+ getBalloonScriptTag()
						+ getBalloonConfScriptTag()
						+ getCalendarScriptTag(), HtmlPosition.ON_MARK);
			} catch (ServiceUnavailableException e) {
				log.warn("HtmlInjectorService is unavailable, JavaScriptInjector takes no action");
			}
						
			mss = response.getServiceHandle().getService(ModifiableStringService.class);
			StringBuilder sb = mss.getModifiableContent();
			
			// insert CSS for calendar
			try {
				sb.insert(getHtmlElementIndex(sb.toString(), HEADER_END_TAG), getCssTag());
			} catch (PageContentException e) {
				log.warn(e.getMessage());
				return ResponseProcessingActions.PROCEED;
			}
			
			String rightMenu = "<" + structRead.getRightMenu().getTag() + " " 
				+ structRead.getRightMenu().getType() + "=\"" 
				+ structRead.getRightMenu().getValue() + "\">";
			
			// initialize services - user ID, database access
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
			
			// add personalized calendar and news to the right menu
			try {
				sb.insert(getHtmlElementIndex(sb.toString(), rightMenu) 
						+ rightMenu.length(), 
						getPersonalizedCalendar(uid, dbService)
						+ getPersonalizedLinks(uid, dbService)
						+ getPersonalizedNews(uid, dbService));			
			} catch (PageContentException e) {
				log.warn(e.getMessage());
				return ResponseProcessingActions.PROCEED;
			}
			
			// replace print section with buttons for sending explicit feedback
			Document doc = Jsoup.parse(sb.toString(), actualUri);
			Element div = doc.select(structRead.getPrint().getTag()
					+ "[" + structRead.getPrint().getType() + "="
					+ structRead.getPrint().getValue() + "]").first();			
			if (div != null) {
				div.html(Feedback.getCode(imagesUrl));
				String printBtnStartCode = "<" + structRead.getPrint().getTag() + " " 
					+ structRead.getPrint().getType() + "=\"" + structRead.getPrint().getValue() + "\">";
				String printBtnEndCode = "</" + structRead.getPrint().getTag() + ">";				
				int iStart = sb.toString().indexOf(printBtnStartCode);
				int iEnd = sb.toString().indexOf(printBtnEndCode, iStart);
				sb.replace(iStart, iEnd + printBtnEndCode.length(), div.outerHtml());
			}	
			
			// domain specific
			// remove some sections from right menu of FIIT website
			Source source = new Source(sb.toString());
			source.fullSequentialParse();
			String najnovsie = null;
			String anketa = null;			
			for (net.htmlparser.jericho.Element e : source.getAllElements("div")) {
				String val = e.getAttributeValue("class");
				if (val != null) {
					if (val.equals("box_nadpis")) {
						String s = e.getContent().toString().toLowerCase();
						if (s.contains("najnovšie")) {
							najnovsie = e.toString();
						}
						else if (s.contains("anketa")) {
							anketa = e.toString();
						}						
					}
				}
			}
			if (najnovsie != null) {
				int iNajStart = sb.indexOf(najnovsie);
				int iEnd = sb.indexOf(DIV_END_TAG, iNajStart + najnovsie.length()) 
					+ DIV_END_TAG.length();
				sb.delete(sb.indexOf(najnovsie), iEnd);
			}
			if (anketa != null) {
				int iAnketaStart = sb.indexOf(anketa);
				int iEnd = sb.indexOf(DIV_END_TAG, iAnketaStart + anketa.length()) 
					+ DIV_END_TAG.length();
				sb.delete(sb.indexOf(anketa), iEnd);
			}			
			
			// replace content in response with modified content 
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
		
		// create reader of the XML file describing portal's structure
		try {
			structRead = new StructureReader(portalStructureFile);
		} catch (FileNotFoundException fnfExc) {
			log.error("Structure file not found: " + portalStructureFile);
			log.error(fnfExc.getMessage());
			return false;
		} catch (DocumentException docExc) {
			log.error("Unable to parse document: " + portalStructureFile);
			log.error(docExc.getMessage());
			return false;
		}
		
		setupJerichoParser();
		
		return true;
	}
	
	/**
	 * Sets up the Jericho HTML Parser by registering special
	 * tag types.
	 */
	private void setupJerichoParser() {
		MicrosoftTagTypes.register();
		PHPTagTypes.register();
		PHPTagTypes.PHP_SHORT.deregister(); // remove PHP short tags for this example otherwise they override processing instructions
		MasonTagTypes.register();
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
	
	private String getCalendarScriptTag() {
		return "<script type='text/javascript' src='" + scriptsUrl + "/wi_calendar.js'></script>";
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
	
	private String getPersonalizedLinks(final String userId,
			final DatabaseConnectionProviderService dbService) {
		PersonalizedLinks links = new PersonalizedLinks();
		return links.getLinksCode(userId, dbService);
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
	
	/**
	 * @param content source code of HTML page
	 * @return index of HTML element within the source code of the page
	 * @throws PageContentException in case the element was not found 
	 * 		   in the source code of the page
	 */
	private int getHtmlElementIndex(final String content, final String element) 
		throws PageContentException {
		
		int iHtmlElement = content.toLowerCase().indexOf(element);
		if(iHtmlElement < 0) {
			throw new PageContentException(
					"No " + element + " on page: " + actualUri);
		}
		return iHtmlElement;
	}
}
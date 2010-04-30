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
package sk.fiit.rabbit.adaptiveproxy.plugins.services.webimp.exceptions;

/**
 * This class represents an exception which is thrown anytime the WebImp adaptive
 * plugin cannot process the content of the page and therefore cannot 
 * personalize the web page.  
 */
public class PageContentException extends Exception {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1241424900561552653L;

	public PageContentException(final String message) {
		super(message);
	}
	
	public PageContentException(final String message, final Throwable cause) {
		super(message, cause);
	}
}

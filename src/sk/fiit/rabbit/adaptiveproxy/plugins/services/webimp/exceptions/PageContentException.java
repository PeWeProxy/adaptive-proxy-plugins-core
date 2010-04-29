package sk.fiit.rabbit.adaptiveproxy.plugins.services.webimp.exceptions;

/**
 * This class represents an exception which is thrown anytime the WebImp adaptive
 * plugin cannot process the content of the page and therefore cannot 
 * personalize the web page.  
 */
public class PageContentException extends Exception {
	
	public PageContentException(final String message) {
		super(message);
	}
	
	public PageContentException(final String message, final Throwable cause) {
		super(message, cause);
	}
}

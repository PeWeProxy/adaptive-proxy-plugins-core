package sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions;

import sk.fiit.peweproxy.services.ProxyService;

public interface HtmlInjectorService extends ProxyService {
	
	public enum HtmlPosition {
		END_OF_BODY,
		START_OF_BODY,
		ON_MARK
	}
	
	public void inject(String text, HtmlPosition position);
}

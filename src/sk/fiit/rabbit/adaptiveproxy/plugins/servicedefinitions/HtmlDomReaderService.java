package sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions;

import org.jdom.Document;

import sk.fiit.peweproxy.services.ProxyService;

public interface HtmlDomReaderService extends ProxyService {
	
	@readonly
	public Document getHTMLDom();

}

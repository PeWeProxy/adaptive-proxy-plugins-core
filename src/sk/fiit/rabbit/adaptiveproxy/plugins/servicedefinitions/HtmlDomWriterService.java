package sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions;

import org.jdom.Document;

import sk.fiit.peweproxy.services.ProxyService;

public interface HtmlDomWriterService extends HtmlDomReaderService {
	
	public void setHTMLDom(Document document);

}

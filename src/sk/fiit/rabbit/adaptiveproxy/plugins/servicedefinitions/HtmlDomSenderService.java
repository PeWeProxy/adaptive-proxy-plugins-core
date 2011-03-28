package sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions;

import org.jdom.Document;

import sk.fiit.peweproxy.services.ProxyService;

public interface HtmlDomSenderService extends ProxyService {
	
	public void setHTMLDom(Document document);

}

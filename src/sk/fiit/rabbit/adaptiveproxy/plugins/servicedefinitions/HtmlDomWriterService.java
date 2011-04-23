package sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions;

import org.jdom.Document;

public interface HtmlDomWriterService extends HtmlDomReaderService {
	
	public void setHTMLDom(Document document);

}

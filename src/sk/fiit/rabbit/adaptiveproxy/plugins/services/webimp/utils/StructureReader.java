package sk.fiit.rabbit.adaptiveproxy.plugins.services.webimp.utils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;


public class StructureReader {
	static Logger log = Logger.getLogger(StructureReader.class);
	private String tag, type, value;
	
	public StructureReader() {
	}
	
	public String getTag() {
		return tag;
	}
	
	public String getType() {
		return type;
	}
	
	public String getValue() {
		return value;
	}
	
	private void readDocument(final Document document, final String elementName) {
		Element root = document.getRootElement();
		Element rightMenu = root.element(elementName);
		tag = rightMenu.element("tag").getText();
		type = rightMenu.element("type").getText();
		value = rightMenu.element("value").getText();
	}
	
	public void readWebStructure(final String elementName) {
		try {
			SAXReader reader = new SAXReader();
			InputStream in = new FileInputStream(Configuration.getInstance().getStructureFile());
			Document document = reader.read(in);
			readDocument(document, elementName);
		} catch (FileNotFoundException fnfExc) {
			
		} catch (DocumentException docExc) {
			
		}
	}
}

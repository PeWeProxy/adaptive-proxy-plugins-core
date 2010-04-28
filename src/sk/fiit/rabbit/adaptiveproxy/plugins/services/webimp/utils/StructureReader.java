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
package sk.fiit.rabbit.adaptiveproxy.plugins.services.webimp.utils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

/**
 * This class reads the XML file describing the structure of web portal being
 * personalized. 
 */
public class StructureReader {
	static Logger log = Logger.getLogger(StructureReader.class);
	private String tag, type, value;
	private String portalStructureFile;
	
	public StructureReader(final String portalStructureFile) {
		this.portalStructureFile = portalStructureFile;
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
	
	public void readWebStructure(final String elementName) throws FileNotFoundException,
		DocumentException {
		try {
			SAXReader reader = new SAXReader();
			InputStream in = new FileInputStream(portalStructureFile);
			Document document = reader.read(in);
			readDocument(document, elementName);
		} catch (FileNotFoundException fnfExc) {
			log.error("Structure file not found: " + fnfExc.getMessage());
			throw fnfExc;
		} catch (DocumentException docExc) {
			throw docExc;
		}
	}
}

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
package sk.fiit.rabbit.adaptiveproxy.plugins.services.webimp.structure;

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
	private String portalStructureFile;
	private Structure rightMenu;
	private Structure print;
	
	public StructureReader(final String portalStructureFile) 
		throws FileNotFoundException, DocumentException {
		
		this.portalStructureFile = portalStructureFile;
		this.readWebStructure();
	}
	
	public Structure getRightMenu() {
		return rightMenu;
	}
	
	public Structure getPrint() {
		return print;
	}

	private void readWebStructure() throws FileNotFoundException,
		DocumentException {
		try {
			SAXReader reader = new SAXReader();
			InputStream in = new FileInputStream(portalStructureFile);
			Document document = reader.read(in);
			rightMenu = readDocument(document, "menuRight");
			print = readDocument(document, "print");
		} catch (FileNotFoundException fnfExc) {
			log.error("Structure file not found: " + fnfExc.getMessage());
			throw fnfExc;
		} catch (DocumentException docExc) {
			throw docExc;
		}
	}
	
	private Structure readDocument(final Document document, final String elementName) {
		Element root = document.getRootElement();
		Element rightMenu = root.element(elementName);
		String tag = rightMenu.element("tag").getText();
		String type = rightMenu.element("type").getText();
		String value = rightMenu.element("value").getText();
		return new Structure(tag, type, value);
	}
}

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

public class Structure {
	private String tag;
	private String type;
	private String value;
	
	public Structure(final String tag, final String type, final String value) {
		this.tag = tag;
		this.type = type;
		this.value = value;
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
}

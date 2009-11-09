package sk.fiit.rabbit.adaptiveproxy.plugins.services.injector;

public class JavaScript {
	String script;
	String byassPattern;
	String bypassTo;
	
	public JavaScript(String script, String byassPattern, String bypassTo) {
		super();
		this.script = script;
		this.byassPattern = byassPattern;
		this.bypassTo = bypassTo;
	}
	
	
}

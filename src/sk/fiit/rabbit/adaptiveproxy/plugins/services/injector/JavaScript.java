package sk.fiit.rabbit.adaptiveproxy.plugins.services.injector;

public class JavaScript {
	String script;
	String byassPattern;
	String bypassTo;
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((script == null) ? 0 : script.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		JavaScript other = (JavaScript) obj;
		if (script == null) {
			if (other.script != null)
				return false;
		} else if (!script.equals(other.script))
			return false;
		return true;
	}

	public JavaScript(String script, String byassPattern, String bypassTo) {
		super();
		this.script = script;
		this.byassPattern = byassPattern;
		this.bypassTo = bypassTo;
	}
}

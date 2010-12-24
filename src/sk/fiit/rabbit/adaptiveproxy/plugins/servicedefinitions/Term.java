package sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions;

public class Term {
	public Long id;
	public String label;
	public String termType;
	
	public Term() {
		super();
	}
	
	public Term(Long id, String label, String termType) {
		super();
		this.id = id;
		this.label = label;
		this.termType = termType;
	}
	
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public String getLabel() {
		return label;
	}
	public void setLabel(String label) {
		this.label = label;
	}
	public String getTermType() {
		return termType;
	}
	public void setTermType(String termType) {
		this.termType = termType;
	}
		
}

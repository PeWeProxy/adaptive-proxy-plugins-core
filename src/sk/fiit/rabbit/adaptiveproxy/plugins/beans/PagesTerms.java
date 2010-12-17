package sk.fiit.rabbit.adaptiveproxy.plugins.beans;

import java.sql.Timestamp;

public class PagesTerms {
	public Long id;
	public Term term;
	public PageInformation pi;
	public Float weight;
	public Timestamp createdAt;
	public Timestamp updatedAt;
	public String source;
	
	public PagesTerms() {
		super();
	}
	
	public PagesTerms(Long id, Term term,
			Float weight, Timestamp createdAt, Timestamp updatedAt,
			String source) {
		super();
		this.id = id;
		this.term = term;
		this.weight = weight;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
		this.source = source;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Term getTerm() {
		return term;
	}

	public void setTerm(Term term) {
		this.term = term;
	}

	public PageInformation getPi() {
		return pi;
	}

	public void setPi(PageInformation pi) {
		this.pi = pi;
	}

	public Float getWeight() {
		return weight;
	}

	public void setWeight(Float weight) {
		this.weight = weight;
	}

	public Timestamp getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Timestamp createdAt) {
		this.createdAt = createdAt;
	}

	public Timestamp getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(Timestamp updatedAt) {
		this.updatedAt = updatedAt;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}
	
}

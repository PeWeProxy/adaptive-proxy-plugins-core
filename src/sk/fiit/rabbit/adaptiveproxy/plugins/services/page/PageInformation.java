package sk.fiit.rabbit.adaptiveproxy.plugins.services.page;

public class PageInformation {
	Long id;
	String url;
	String checksum;
	Integer contentLength;
	String keywords;
	
	public PageInformation() {
	}

	public PageInformation(Long id, String url, String checksum,
			Integer contentLength, String keywords) {
		super();
		this.id = id;
		this.url = url;
		this.checksum = checksum;
		this.contentLength = contentLength;
		this.keywords = keywords;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getChecksum() {
		return checksum;
	}

	public void setChecksum(String checksum) {
		this.checksum = checksum;
	}

	public Integer getContentLength() {
		return contentLength;
	}

	public void setContentLength(Integer contentLength) {
		this.contentLength = contentLength;
	}

	public String getKeywords() {
		return keywords;
	}

	public void setKeywords(String keywords) {
		this.keywords = keywords;
	}
}

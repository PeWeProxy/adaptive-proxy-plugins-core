package sk.fiit.rabbit.adaptiveproxy.plugins.services.cleartext;

import sk.fiit.rabbit.adaptiveproxy.plugins.services.ProxyService;

public interface ClearTextExtractionService extends ProxyService {
	public String getCleartext();

}

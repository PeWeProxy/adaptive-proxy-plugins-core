package sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions;

import sk.fiit.peweproxy.services.ProxyService;

public interface ClearTextExtractionService extends ProxyService {
	@readonly
	public String getCleartext();

}

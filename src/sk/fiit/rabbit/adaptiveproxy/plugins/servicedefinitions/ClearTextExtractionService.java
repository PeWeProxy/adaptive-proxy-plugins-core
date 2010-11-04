package sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions;

import sk.fiit.rabbit.adaptiveproxy.plugins.services.ProxyService;

public interface ClearTextExtractionService extends ProxyService {
	public String getCleartext();

}

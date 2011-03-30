package sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions;

import sk.fiit.peweproxy.services.ProxyService;

public interface PageInformationProviderService extends ProxyService {
	@readonly
	public PageInformation getPageInformation();
}

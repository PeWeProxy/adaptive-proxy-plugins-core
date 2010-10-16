package sk.fiit.rabbit.adaptiveproxy.plugins.services.page;

import sk.fiit.peweproxy.services.ProxyService;

public interface PageInformationProviderService extends ProxyService {
	public PageInformation getPageInformation();
}

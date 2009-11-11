package sk.fiit.rabbit.adaptiveproxy.plugins.services.page;

import sk.fiit.rabbit.adaptiveproxy.plugins.services.ProxyService;

public interface PageInformationProviderService extends ProxyService {
	public PageInformation getPageInformation();
}

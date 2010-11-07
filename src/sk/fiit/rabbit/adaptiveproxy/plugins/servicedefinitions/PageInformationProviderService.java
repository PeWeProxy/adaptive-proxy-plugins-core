package sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions;

import sk.fiit.peweproxy.services.ProxyService;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.page.PageInformation;

public interface PageInformationProviderService extends ProxyService {
	public PageInformation getPageInformation();
}

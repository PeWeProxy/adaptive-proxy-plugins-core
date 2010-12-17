package sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions;

import sk.fiit.peweproxy.services.ProxyService;
import sk.fiit.rabbit.adaptiveproxy.plugins.beans.PageInformation;

public interface PageInformationProviderService extends ProxyService {
	@readonly
	public PageInformation getPageInformation();
}

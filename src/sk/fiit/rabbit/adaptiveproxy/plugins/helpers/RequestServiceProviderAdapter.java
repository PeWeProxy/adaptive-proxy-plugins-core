package sk.fiit.rabbit.adaptiveproxy.plugins.helpers;

import sk.fiit.rabbit.adaptiveproxy.plugins.messages.ModifiableHttpRequest;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.ProxyService;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.RequestServiceProvider;

public abstract class RequestServiceProviderAdapter implements RequestServiceProvider, ProxyService {

	@Override
	public void setRequestContext(ModifiableHttpRequest request) {
	}

	@Override
	public void doChanges() {
	}

	@Override
	public ProxyService getService() {
		return this;
	}

	@Override
	public String getServiceIdentification() {
		return this.getClass().getSimpleName();
	}

}

package sk.fiit.rabbit.adaptiveproxy.plugins.helpers;

import sk.fiit.rabbit.adaptiveproxy.plugins.messages.ModifiableHttpResponse;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.ProxyService;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.ResponseServiceProvider;

public abstract class ResponseServiceProviderAdapter implements ResponseServiceProvider, ProxyService {

	@Override
	public void setResponseContext(ModifiableHttpResponse response) {
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

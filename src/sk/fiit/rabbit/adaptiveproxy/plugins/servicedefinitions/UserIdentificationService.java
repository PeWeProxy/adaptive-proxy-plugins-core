package sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions;

import sk.fiit.peweproxy.services.ProxyService;

public interface UserIdentificationService extends ProxyService {
	String getClientIdentification();
}

package sk.fiit.rabbit.adaptiveproxy.plugins.services.user;

import sk.fiit.peweproxy.services.ProxyService;

public interface UserIdentificationService extends ProxyService {
	String getClientIdentification();
}

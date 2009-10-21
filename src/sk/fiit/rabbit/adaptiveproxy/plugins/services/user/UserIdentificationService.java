package sk.fiit.rabbit.adaptiveproxy.plugins.services.user;

import sk.fiit.rabbit.adaptiveproxy.plugins.services.ProxyService;

public interface UserIdentificationService extends ProxyService {
	String getClientIdentification();
}

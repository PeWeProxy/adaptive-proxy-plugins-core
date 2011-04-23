package sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions;

import sk.fiit.peweproxy.services.ProxyService;
import sk.fiit.peweproxy.services.ProxyService.readonly;

public interface UserIdentificationService extends ProxyService {
	@readonly
	String getClientIdentification();
}

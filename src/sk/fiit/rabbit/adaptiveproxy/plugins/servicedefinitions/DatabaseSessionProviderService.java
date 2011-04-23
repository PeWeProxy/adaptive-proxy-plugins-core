package sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions;

import sk.fiit.peweproxy.services.ProxyService;
import sk.fiit.peweproxy.services.ProxyService.readonly;

import com.fourspaces.couchdb.Database;

public interface DatabaseSessionProviderService extends ProxyService {
	
	@readonly
	public Database getDatabase();
	
}

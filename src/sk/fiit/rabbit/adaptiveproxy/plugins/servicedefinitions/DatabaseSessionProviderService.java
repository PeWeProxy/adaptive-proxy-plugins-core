package sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions;

import com.fourspaces.couchdb.Database;
import com.fourspaces.couchdb.Session;

import sk.fiit.peweproxy.services.ProxyService;

public interface DatabaseSessionProviderService extends ProxyService {
	
	@readonly
	public Database getDatabase();
	
}

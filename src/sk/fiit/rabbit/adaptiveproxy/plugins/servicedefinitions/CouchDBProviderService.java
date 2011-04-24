package sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions;

import org.jcouchdb.db.Database;

import sk.fiit.peweproxy.services.ProxyService;

public interface CouchDBProviderService extends ProxyService {
	
	@readonly
	public Database getDatabase();
	
}

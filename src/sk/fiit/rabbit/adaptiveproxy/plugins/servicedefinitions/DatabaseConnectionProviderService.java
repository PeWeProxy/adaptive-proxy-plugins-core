package sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions;

import java.sql.Connection;

import sk.fiit.peweproxy.services.ProxyService;

public interface DatabaseConnectionProviderService extends ProxyService {
	@readonly
	public Connection getDatabaseConnection();
}

package sk.fiit.rabbit.adaptiveproxy.plugins.services.database;

import java.sql.Connection;

import sk.fiit.rabbit.adaptiveproxy.plugins.services.ProxyService;

public interface DatabaseConnectionProviderService extends ProxyService {
	public Connection getDatabaseConnection();
}

package sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions;

import java.sql.Connection;

import com.fourspaces.couchdb.Database;

import sk.fiit.peweproxy.services.ProxyService;

public interface PageInformationProviderService extends ProxyService {
	@readonly
	public PageInformation getPageInformation(Connection connection, Database database);
}

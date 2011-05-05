package sk.fiit.rabbit.adaptiveproxy.plugins.services.database;

import java.util.Set;

import org.apache.log4j.Logger;
import org.jcouchdb.db.Database;

import sk.fiit.peweproxy.headers.RequestHeader;
import sk.fiit.peweproxy.headers.ResponseHeader;
import sk.fiit.peweproxy.messages.HttpRequest;
import sk.fiit.peweproxy.messages.HttpResponse;
import sk.fiit.peweproxy.messages.ModifiableHttpRequest;
import sk.fiit.peweproxy.messages.ModifiableHttpResponse;
import sk.fiit.peweproxy.plugins.PluginProperties;
import sk.fiit.peweproxy.plugins.services.RequestServiceModule;
import sk.fiit.peweproxy.plugins.services.RequestServiceProvider;
import sk.fiit.peweproxy.plugins.services.ResponseServiceModule;
import sk.fiit.peweproxy.plugins.services.ResponseServiceProvider;
import sk.fiit.peweproxy.services.ProxyService;
import sk.fiit.peweproxy.services.ServiceUnavailableException;
import sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions.CouchDBProviderService;

public class CouchDBProviderServiceModule implements RequestServiceModule, ResponseServiceModule {
	
	private static final Logger logger = Logger.getLogger(CouchDBProviderServiceModule.class);

	private Database database;

	private class CouchDBDatabaseSessionProvider implements CouchDBProviderService,
			RequestServiceProvider<CouchDBProviderService>,
			ResponseServiceProvider<CouchDBProviderService> {

		@Override
		public Database getDatabase() {
			return database;
		}

		@Override
		public String getServiceIdentification() {
			return this.getClass().getName();
		}

		@Override
		public CouchDBProviderService getService() {
			return this;
		}

		@Override
		public boolean initChangedModel() {
			return true;
		}

		@Override
		public void doChanges(ModifiableHttpResponse response) {
			// non-modifying service
		}

		@Override
		public void doChanges(ModifiableHttpRequest request) {
			// non-modifying service
		}
	}

	@Override
	public boolean supportsReconfigure(PluginProperties newProps) {
		return false;
	}

	@Override
	public boolean start(PluginProperties props) {
		String host = props.getProperty("host", "localhost");
		int port = props.getIntProperty("port", 5984);
		String dbName = props.getProperty("dbName", "proxy");

		try {
			database = new Database(host, port, dbName);
			database.getStatus();
		} catch (Exception e) {
			logger.error("Unable to create CouchDB Session ", e);
			return false;
		}

		return true;
	}

	@Override
	public void stop() {
	}

	@Override
	public void desiredRequestServices(Set<Class<? extends ProxyService>> desiredServices, RequestHeader clientRQHeader) {
		// no dependencies

	}

	@Override
	public void desiredResponseServices(Set<Class<? extends ProxyService>> desiredServices, ResponseHeader webRPHeader) {
		// no dependencies
	}

	@Override
	public void getProvidedResponseServices(Set<Class<? extends ProxyService>> providedServices) {
		providedServices.add(CouchDBProviderService.class);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <Service extends ProxyService> ResponseServiceProvider<Service> provideResponseService(
			HttpResponse response, Class<Service> serviceClass) throws ServiceUnavailableException {
		if (serviceClass.equals(CouchDBProviderService.class)) {
			return (ResponseServiceProvider<Service>) new CouchDBDatabaseSessionProvider();
		}
		return null;
	}

	@Override
	public void getProvidedRequestServices(Set<Class<? extends ProxyService>> providedServices) {
		providedServices.add(CouchDBProviderService.class);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <Service extends ProxyService> RequestServiceProvider<Service> provideRequestService(HttpRequest request,
			Class<Service> serviceClass) throws ServiceUnavailableException {
		if (serviceClass.equals(CouchDBProviderService.class)) {
			return (RequestServiceProvider<Service>) new CouchDBDatabaseSessionProvider();
		}

		return null;
	}

}

package sk.fiit.rabbit.adaptiveproxy.plugins.services.database;

import java.util.Set;

import org.apache.log4j.Logger;

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
import sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions.DatabaseSessionProviderService;

import com.fourspaces.couchdb.Database;
import com.fourspaces.couchdb.Session;

public class CouchDBDatabaseSessionServiceModul implements RequestServiceModule, ResponseServiceModule {
	
	
	private static final Logger logger = Logger.getLogger(CouchDBDatabaseSessionServiceModul.class);

	private static String host = null;
	private static Integer port = null;
	private static String dbName = null;
	
	private static String userName = null;
	private static String password = null;
	private Session session = null;

	private class CouchDBDatabaseSessionProvider implements
	DatabaseSessionProviderService,
	RequestServiceProvider<DatabaseSessionProviderService>,
	ResponseServiceProvider<DatabaseSessionProviderService> {
	
		Database database = null;
				
		@Override
		public Database getDatabase() {
			try {
				session = new Session(host, port);
				
				if(session == null) {
					throw new Exception("Unable to create session to CouchDB");
				}

				database = session.getDatabase(dbName);
				
				if(database == null) {
					throw new Exception("Unable to connect to CouchDB database.");
				}
				
				return(database);
				
			} catch (Exception e) {
				logger.error("Could not get CouchDB session", e);
				return null;
			}
		}
		
		@Override
		public String getServiceIdentification() {
			return this.getClass().getName();
		}
		
		@Override
		public DatabaseSessionProviderService getService() {
			return this;
		}
		
		@Override
		public boolean initChangedModel() {
			return true;
		}
		
		@Override
		public void doChanges(ModifiableHttpResponse response) {
			// TODO Auto-generated method stub
			
		}
		
		@Override
		public void doChanges(ModifiableHttpRequest request) {
			// TODO Auto-generated method stub
			
		}

	}
	
	@Override
	public boolean supportsReconfigure(PluginProperties newProps) {
		// TODO Auto-generated method stub
		return true;
	}
	
	@Override
	public boolean start(PluginProperties props) {
		this.host = props.getProperty("host", "localhost");
		this.port = props.getIntProperty("port", 5984);
		this.dbName = props.getProperty("dbName", "proxy");
		
		this.userName = props.getProperty("userName");
		this.password = props.getProperty("password");
		
		this.session = new Session(host, port);
		
		return true;
	}
	@Override
	public void stop() {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void desiredRequestServices(
			Set<Class<? extends ProxyService>> desiredServices,
			RequestHeader clientRQHeader) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void desiredResponseServices(
			Set<Class<? extends ProxyService>> desiredServices,
			ResponseHeader webRPHeader) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void getProvidedResponseServices(
			Set<Class<? extends ProxyService>> providedServices) {
		providedServices.add(DatabaseSessionProviderService.class);	
	}
	
	@Override
	public <Service extends ProxyService> ResponseServiceProvider<Service> provideResponseService(
			HttpResponse response, Class<Service> serviceClass)
			throws ServiceUnavailableException {
		if(serviceClass.equals(DatabaseSessionProviderService.class)) {
			return (ResponseServiceProvider<Service>) new CouchDBDatabaseSessionProvider();
		}
		return null;
	}
	
	@Override
	public void getProvidedRequestServices(
			Set<Class<? extends ProxyService>> providedServices) {
		providedServices.add(DatabaseSessionProviderService.class);
		
	}

	@Override
	public <Service extends ProxyService> RequestServiceProvider<Service> provideRequestService(
			HttpRequest request, Class<Service> serviceClass)
			throws ServiceUnavailableException {
		if(serviceClass.equals(DatabaseSessionProviderService.class)) {
			return (RequestServiceProvider<Service>) new CouchDBDatabaseSessionProvider();
		}
		
		return null;
	}

}

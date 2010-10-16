package sk.fiit.rabbit.adaptiveproxy.plugins.services.database;

import java.sql.Connection;
import java.sql.SQLException;
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

import com.jolbox.bonecp.BoneCP;
import com.jolbox.bonecp.BoneCPConfig;

public class MySQLDatabaseConnectionProviderServiceModule implements RequestServiceModule, ResponseServiceModule {
	
	private BoneCP connectionPool;
	
	private static final Logger logger = Logger.getLogger(MySQLDatabaseConnectionProviderServiceModule.class);
	
	private class MySQLDatabaseConnectionProvider implements
			DatabaseConnectionProviderService,
			RequestServiceProvider<DatabaseConnectionProviderService>,
			ResponseServiceProvider<DatabaseConnectionProviderService> {
		
		@Override
		public Connection getDatabaseConnection() {
			try {
				return connectionPool.getConnection();
			} catch (SQLException e) {
				logger.error("Could not get connection from a pool", e);
				return null;
			}
		}

		@Override
		public String getServiceIdentification() {
			return this.getClass().getName();
		}

		@Override
		public DatabaseConnectionProviderService getService() {
			return this;
		}

		@Override
		public boolean initChangedModel() {
			return false;
		}

		@Override
		public void doChanges(ModifiableHttpResponse response) {
			// this service makes no modifications
		}

		@Override
		public void doChanges(ModifiableHttpRequest request) {
			// this service makes no modifications
		}
	}
	
	@Override
	public boolean start(PluginProperties props) {
		String url = props.getProperty("jdbcURL");
		String username = props.getProperty("userName");
		String password = props.getProperty("password");
		String validationQuery = props.getProperty("validationQuery");
		Integer idleTestPeriod = props.getIntProperty("idleTestPeriod", 100);
		Integer partitionCount = props.getIntProperty("partitionCount", 4);
		Integer maxConnectionsPerPartition = props.getIntProperty("maxConnectionsPerPartition", 10);
		Integer minConnectionsPerPartition = props.getIntProperty("minConnectionsPerPartition", 5);
		
		try {
			String jdbcDriver = props.getProperty("driver");
			Class.forName(jdbcDriver);
		} catch (ClassNotFoundException e) {
			logger.error("Could not load JDBC Driver: " + e.getMessage());
			return false;
		}

		BoneCPConfig config = new BoneCPConfig();
		config.setJdbcUrl(url);
		config.setUsername(username);
		config.setPassword(password);
		
		config.setConnectionTestStatement(validationQuery);
		config.setIdleConnectionTestPeriod(idleTestPeriod);
		
		config.setPartitionCount(partitionCount);
		config.setMinConnectionsPerPartition(minConnectionsPerPartition);
		config.setMaxConnectionsPerPartition(maxConnectionsPerPartition);
		
		try {
			connectionPool = new BoneCP(config);
		} catch (SQLException e) {
			logger.error("Could not initialize connection pool", e);
			return false;
		}
		
		return true;
	}

	@Override
	public boolean supportsReconfigure(PluginProperties newProps) {
		return false;
	}

	@Override
	public void stop() {
	}

	@Override
	public void desiredRequestServices(
			Set<Class<? extends ProxyService>> desiredServices,
			RequestHeader clientRQHeader) {
		// no dependencies
	}

	@Override
	public void desiredResponseServices(
			Set<Class<? extends ProxyService>> desiredServices,
			ResponseHeader webRPHeader) {
		// no dependencies
	}

	@Override
	public void getProvidedResponseServices(
			Set<Class<? extends ProxyService>> providedServices) {
		providedServices.add(DatabaseConnectionProviderService.class);
	}

	@Override
	public <Service extends ProxyService> ResponseServiceProvider<Service> provideResponseService(
			HttpResponse response, Class<Service> serviceClass)
			throws ServiceUnavailableException {
		if(serviceClass.equals(DatabaseConnectionProviderService.class)) {
			return (ResponseServiceProvider<Service>) new MySQLDatabaseConnectionProvider();
		}
		
		return null;
	}

	@Override
	public void getProvidedRequestServices(
			Set<Class<? extends ProxyService>> providedServices) {
		providedServices.add(DatabaseConnectionProviderService.class);
	}

	@Override
	public <Service extends ProxyService> RequestServiceProvider<Service> provideRequestService(
			HttpRequest request, Class<Service> serviceClass)
			throws ServiceUnavailableException {
		if(serviceClass.equals(DatabaseConnectionProviderService.class)) {
			return (RequestServiceProvider<Service>) new MySQLDatabaseConnectionProvider();
		}
		
		return null;
	}
}

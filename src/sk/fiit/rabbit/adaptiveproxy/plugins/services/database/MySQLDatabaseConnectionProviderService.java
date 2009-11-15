package sk.fiit.rabbit.adaptiveproxy.plugins.services.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

import org.apache.commons.dbcp.ConnectionFactory;
import org.apache.commons.dbcp.DriverManagerConnectionFactory;
import org.apache.commons.dbcp.PoolableConnectionFactory;
import org.apache.commons.dbcp.PoolingDriver;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.log4j.Logger;

import sk.fiit.rabbit.adaptiveproxy.plugins.PluginProperties;
import sk.fiit.rabbit.adaptiveproxy.plugins.helpers.RequestAndResponseServicePluginAdapter;
import sk.fiit.rabbit.adaptiveproxy.plugins.helpers.RequestAndResponseServiceProviderAdapter;
import sk.fiit.rabbit.adaptiveproxy.plugins.messages.HttpRequest;
import sk.fiit.rabbit.adaptiveproxy.plugins.messages.HttpResponse;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.ProxyService;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.RequestServiceProvider;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.ResponseServiceProvider;

public class MySQLDatabaseConnectionProviderService extends RequestAndResponseServicePluginAdapter {
	
	private static final Logger logger = Logger.getLogger(MySQLDatabaseConnectionProviderService.class);
	
	private class MySQLDatabaseConnectionProvider extends RequestAndResponseServiceProviderAdapter 
	    implements DatabaseConnectionProviderService {
		
		@Override
		public Connection getDatabaseConnection() {
			try {
				return DriverManager.getConnection("jdbc:apache:commons:dbcp:proxyJdbcPool");
			} catch (SQLException e) {
				logger.error("Could not get connection from a pool", e);
				return null;
			}
		}

		@Override
		public Class<? extends ProxyService> getServiceClass() {
			return DatabaseConnectionProviderService.class;
		}
	}

	@Override
	protected void addProvidedRequestServices(List<RequestServiceProvider> providedServices, HttpRequest request) {
		providedServices.add(new MySQLDatabaseConnectionProvider());
	}
	
	@Override
	protected void addProvidedResponseServices(List<ResponseServiceProvider> providedServices, HttpResponse response) {
		providedServices.add(new MySQLDatabaseConnectionProvider());
	}
	
	@Override
	protected void addProvidedServices(Set<Class<? extends ProxyService>> providedServices) {
		providedServices.add(DatabaseConnectionProviderService.class);
	}
	
	@Override
	public boolean setup(PluginProperties props) {
		String url = props.getProperty("jdbcURL");
		String username = props.getProperty("userName");
		String password = props.getProperty("password");
		
		try {
			String jdbcDriver = props.getProperty("driver");
			Class.forName(jdbcDriver);
		} catch (ClassNotFoundException e) {
			logger.error("Could not load JDBC Driver: " + e.getMessage());
			return false;
		}

		GenericObjectPool connectionPool = new GenericObjectPool(null);
		ConnectionFactory connectionFactory = new DriverManagerConnectionFactory(url, username, password);
		PoolableConnectionFactory poolableConnectionFactory = new PoolableConnectionFactory(connectionFactory, connectionPool, null, null, false, true);
		PoolingDriver driver = new PoolingDriver();
		driver.registerPool("proxyJdbcPool", connectionPool);
		

		return true;
	}
}

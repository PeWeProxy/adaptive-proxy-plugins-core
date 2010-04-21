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
import org.apache.commons.pool.ObjectPool;
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
	private static PoolingDriver driver;
	
	private class PoolStatus implements Runnable {

		@Override
		public void run() {
			while(true) {
				ObjectPool op;
				try {
					op = driver.getConnectionPool("proxyJdbcPool");
					System.err.println("NumActive: " + op.getNumActive());
					System.err.println("NumIdle: " + op.getNumIdle());
				} catch (SQLException e) {
					e.printStackTrace();
				}
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {}
			}
		}
		
	}
	
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
		String validationQuery = props.getProperty("validationQuery");
		
		try {
			String jdbcDriver = props.getProperty("driver");
			Class.forName(jdbcDriver);
		} catch (ClassNotFoundException e) {
			logger.error("Could not load JDBC Driver: " + e.getMessage());
			return false;
		}

		GenericObjectPool connectionPool = new GenericObjectPool(null);
		
		connectionPool.setMaxActive(Integer.parseInt(props.getProperty("maxActive", "20")));
		connectionPool.setMaxIdle(Integer.parseInt(props.getProperty("maxIdle", "-1")));
		connectionPool.setMaxWait(Integer.parseInt(props.getProperty("maxWait", "2000")));
		
		DriverManagerConnectionFactory connectionFactory = new DriverManagerConnectionFactory(url, username, password);
		PoolableConnectionFactory poolableConnectionFactory = new PoolableConnectionFactory(connectionFactory, connectionPool, null, null, false, true);
		poolableConnectionFactory.setValidationQuery(validationQuery);
		driver = new PoolingDriver();
		driver.registerPool("proxyJdbcPool", connectionPool);
		
		return true;
	}
}

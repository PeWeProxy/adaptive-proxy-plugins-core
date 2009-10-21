package sk.fiit.rabbit.adaptiveproxy.plugins.services.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import sk.fiit.rabbit.adaptiveproxy.plugins.PluginProperties;
import sk.fiit.rabbit.adaptiveproxy.plugins.helpers.RequestAndResponseServicePluginAdapter;
import sk.fiit.rabbit.adaptiveproxy.plugins.helpers.RequestAndResponseServiceProviderAdapter;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.ProxyService;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.RequestServiceProvider;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.ResponseServiceProvider;

public class MySQLDatabaseConnectionProviderService extends RequestAndResponseServicePluginAdapter {
	
	private static final Logger logger = Logger.getLogger(MySQLDatabaseConnectionProviderService.class);
	
	private static Connection connection;
	
	
	private class DerbyDatabaseConnectionProvider extends RequestAndResponseServiceProviderAdapter 
	    implements DatabaseConnectionProviderService {
		
		@Override
		public Connection getDatabaseConnection() {
			return connection;		
		}

		@Override
		public Class<? extends ProxyService> getServiceClass() {
			return DatabaseConnectionProviderService.class;
		}
	}

	@Override
	protected void addProvidedRequestServices(List<RequestServiceProvider> providedServices) {
		providedServices.add(new DerbyDatabaseConnectionProvider());
	}
	
	@Override
	protected void addProvidedResponseServices(List<ResponseServiceProvider> providedServices) {
		providedServices.add(new DerbyDatabaseConnectionProvider());
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
			String driver = props.getProperty("driver");
			Class.forName(driver);
			connection = DriverManager.getConnection(url, username, password);			
		} catch (SQLException e) {
			logger.error("Could not connect to database: " + e.getMessage());
			return false;
		} catch (ClassNotFoundException e) {
			logger.error("Could not load JDBC Driver: " + e.getMessage());
			return false;
		}

		return true;
	}
}

package sk.fiit.rabbit.adaptiveproxy.plugins.services.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import com.jolbox.bonecp.BoneCP;
import com.jolbox.bonecp.BoneCPConfig;

import sk.fiit.rabbit.adaptiveproxy.plugins.PluginProperties;
import sk.fiit.rabbit.adaptiveproxy.plugins.helpers.RequestAndResponseServicePluginAdapter;
import sk.fiit.rabbit.adaptiveproxy.plugins.helpers.RequestAndResponseServiceProviderAdapter;
import sk.fiit.rabbit.adaptiveproxy.plugins.messages.HttpRequest;
import sk.fiit.rabbit.adaptiveproxy.plugins.messages.HttpResponse;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.ProxyService;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.RequestServiceProvider;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.ResponseServiceProvider;

public class MySQLDatabaseConnectionProviderService extends RequestAndResponseServicePluginAdapter {
	
	private class ConnectionPoolMonitor implements Runnable {
		@Override
		public void run() {
			while(true) {
				System.out.println(new Date());
				System.out.println("Total created connections: " + connectionPool.getTotalCreatedConnections());
				System.out.println("Total FREE connections: " + connectionPool.getTotalFree());
				System.out.println("Total LEASED connections: " + connectionPool.getTotalLeased());
				
				try {
					Thread.sleep(1000 * 60);
				} catch (InterruptedException e) {
					System.out.println("ConnectionPoolMonitor interrupted");
				}
			}
		}
	}
	
	private BoneCP connectionPool;
	
	private static final Logger logger = Logger.getLogger(MySQLDatabaseConnectionProviderService.class);
	
	private class MySQLDatabaseConnectionProvider extends RequestAndResponseServiceProviderAdapter 
	    implements DatabaseConnectionProviderService {
		
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
		
		//new Thread(new ConnectionPoolMonitor()).start();
		
		return true;
	}
}

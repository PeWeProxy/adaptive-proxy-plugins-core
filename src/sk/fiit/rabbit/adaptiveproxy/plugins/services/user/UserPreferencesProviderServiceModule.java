package sk.fiit.rabbit.adaptiveproxy.plugins.services.user;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;

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
import sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions.DatabaseConnectionProviderService;
import sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions.UserPreferencesProviderService;
import sk.fiit.rabbit.adaptiveproxy.plugins.utils.JdbcTemplate;
import sk.fiit.rabbit.adaptiveproxy.plugins.utils.JdbcTemplate.ResultProcessor;
import sk.fiit.rabbit.adaptiveproxy.plugins.utils.SqlUtils;

public class UserPreferencesProviderServiceModule implements RequestServiceModule, ResponseServiceModule {

	private class UserPreferencesProviderServiceProvider implements UserPreferencesProviderService, 
			RequestServiceProvider<UserPreferencesProviderService>, 
			ResponseServiceProvider<UserPreferencesProviderService> {

		private DatabaseConnectionProviderService connectionProvider;

		public UserPreferencesProviderServiceProvider(DatabaseConnectionProviderService connectionProvider) {
			this.connectionProvider = connectionProvider;
		}

		@Override
		public String getProperty(String preferenceName, String userUid, String propertyNamespace) {
			Connection connection = connectionProvider.getDatabaseConnection();
			JdbcTemplate jdbc = new JdbcTemplate(connection);
			String userPreference;
			try {
				userPreference = 
					jdbc.find("SELECT preference_value FROM user_preferences WHERE user = ? AND preference_name = ? LIMIT 1", 
						new Object[] { userUid, propertyNamespace + "_" + preferenceName },
						new ResultProcessor<String>() {
					@Override
					public String processRow(ResultSet rs) throws SQLException {
						return rs.getString("preference_value");
					}
				});
			} finally {
			
				SqlUtils.close(connection);
			}
			
			System.err.println("a vraciam   " + userPreference);
			
			return userPreference;
		}
		
		@Override
		public void setProperty(String preferenceName, String propertyValue, String userUid, String preferenceNamespace) {
			Connection connection = connectionProvider.getDatabaseConnection();
			JdbcTemplate jdbc = new JdbcTemplate(connection);
			
			try {
				String userPreferenceExists = 
					jdbc.find("SELECT ID FROM user_preferences WHERE user = ? AND preference_name = ? LIMIT 1", 
						new Object[] { userUid, preferenceNamespace + "_" + preferenceName },
						new ResultProcessor<String>() {
					
					@Override
					public String processRow(ResultSet rs) throws SQLException {
						return rs.getString("ID");
					}
				});
				
				if (userPreferenceExists == null) {	
					jdbc.insert("INSERT INTO user_preferences (user, preference_name, preference_value) VALUES (?, ?, ?)", new Object[] { userUid, preferenceNamespace + "_" + preferenceName, propertyValue });
				}
				else {
					jdbc.update("UPDATE user_preferences SET preference_value = ? WHERE user = ? AND preference_name = ?", new Object[] { propertyValue, userUid, preferenceNamespace + "_" + preferenceName });
				}
			} finally {
				SqlUtils.close(connection);
			}
		}
		
		@Override
		public void setProperty(String propertyName, String propertyValue, String userUid) {
			setProperty(propertyName, propertyValue, userUid, "global");
		}

		@Override
		public String getServiceIdentification() {
			return this.getClass().getName();
		}

		@Override
		public UserPreferencesProviderService getService() {
			return this;
		}

		@Override
		public boolean initChangedModel() {
			return false;
		}

		@Override
		public void doChanges(ModifiableHttpResponse response) {
		}

		@Override
		public void doChanges(ModifiableHttpRequest request) {
		}
		

	}

	@SuppressWarnings("unchecked")
	@Override
	public <Service extends ProxyService> RequestServiceProvider<Service> provideRequestService(HttpRequest request,
			Class<Service> serviceClass) throws ServiceUnavailableException {

		if (serviceClass.equals(UserPreferencesProviderService.class) && (request.getServicesHandle().isServiceAvailable(DatabaseConnectionProviderService.class))) {
			DatabaseConnectionProviderService connectionProvider = request.getServicesHandle().getService(DatabaseConnectionProviderService.class);
			return (RequestServiceProvider<Service>) new UserPreferencesProviderServiceProvider(connectionProvider);
		}
		return null;
	}

	@Override
	public void getProvidedRequestServices(Set<Class<? extends ProxyService>> providedServices) {
		providedServices.add(UserPreferencesProviderService.class);
	}

	@Override
	public void desiredRequestServices(Set<Class<? extends ProxyService>> desiredServices, RequestHeader clientRQHeader) {
		desiredServices.add(DatabaseConnectionProviderService.class);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <Service extends ProxyService> ResponseServiceProvider<Service> provideResponseService(
			HttpResponse response, Class<Service> serviceClass) throws ServiceUnavailableException {
		
		if (serviceClass.equals(UserPreferencesProviderService.class) && (response.getServicesHandle().isServiceAvailable(DatabaseConnectionProviderService.class))) {
			DatabaseConnectionProviderService connectionProvider = response.getServicesHandle().getService(DatabaseConnectionProviderService.class);
			return (ResponseServiceProvider<Service>) new UserPreferencesProviderServiceProvider(connectionProvider);
		}
		return null;
	}

	@Override
	public void getProvidedResponseServices(Set<Class<? extends ProxyService>> providedServices) {
		providedServices.add(UserPreferencesProviderService.class);
	}
	
	@Override
	public void desiredResponseServices(Set<Class<? extends ProxyService>> desiredServices, ResponseHeader webRPHeader) {
		desiredServices.add(DatabaseConnectionProviderService.class);
	}

	@Override
	public boolean start(PluginProperties props) {
		return true;
	}

	@Override
	public void stop() {
	}

	@Override
	public boolean supportsReconfigure(PluginProperties newProps) {
		return true;
	}
}

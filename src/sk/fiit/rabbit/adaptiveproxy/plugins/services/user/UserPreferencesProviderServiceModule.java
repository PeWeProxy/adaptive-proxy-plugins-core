package sk.fiit.rabbit.adaptiveproxy.plugins.services.user;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import sk.fiit.peweproxy.headers.RequestHeader;
import sk.fiit.peweproxy.messages.HttpRequest;
import sk.fiit.peweproxy.messages.ModifiableHttpRequest;
import sk.fiit.peweproxy.plugins.PluginProperties;
import sk.fiit.peweproxy.plugins.services.RequestServiceModule;
import sk.fiit.peweproxy.plugins.services.RequestServiceProvider;
import sk.fiit.peweproxy.services.ProxyService;
import sk.fiit.peweproxy.services.ServiceUnavailableException;
import sk.fiit.peweproxy.services.content.StringContentService;
import sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions.UserPreferencesProviderService;
import sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions.DatabaseConnectionProviderService;
import sk.fiit.rabbit.adaptiveproxy.plugins.utils.JdbcTemplate;
import sk.fiit.rabbit.adaptiveproxy.plugins.utils.JdbcTemplate.ResultProcessor;

public class UserPreferencesProviderServiceModule implements RequestServiceModule {

	private static final Logger logger = Logger.getLogger(UserPreferencesProviderServiceModule.class);

	private class UserPreferencesProviderServiceProvider implements UserPreferencesProviderService, RequestServiceProvider<UserPreferencesProviderService> {
		JdbcTemplate jdbc;

		public UserPreferencesProviderServiceProvider(Connection connection) {
			jdbc = new JdbcTemplate(connection);
		}

		@Override
		public String getProperty(String propertyName, String userUid, String propertyNamespace) {			
			String userPreference = 
				jdbc.find("SELECT preference_value FROM user_preferences WHERE user = ? AND preference_name = ? LIMIT 1", 
					new Object[] { userUid, propertyNamespace + "_" + propertyName },
					new ResultProcessor<String>() {
				@SuppressWarnings("unchecked")
				@Override
				public String processRow(ResultSet rs) throws SQLException {
					return rs.getString("preference_value");
				}
			});
			
			return userPreference;
		}
		
		@Override
		public void setProperty(String propertyName, String propertyValue, String userUid, String propertyNamespace) {
			
			String userPreferenceExists = 
				jdbc.find("SELECT ID FROM user_preferences WHERE user = ? AND preference_name = ? LIMIT 1", 
					new Object[] { userUid, propertyNamespace + "_" + propertyName },
					new ResultProcessor<String>() {
				@SuppressWarnings("unchecked")
				@Override
				public String processRow(ResultSet rs) throws SQLException {
					return rs.getString("ID");
				}
			});
			
			if (userPreferenceExists == null) {	
				jdbc.insert("INSERT INTO user_preferences (user, preference_name, preference_value) VALUES (?, ?, ?)", new Object[] { userUid, propertyNamespace + "_" + propertyName, propertyValue });
			}
			else {
				jdbc.update("UPDATE user_preferences SET preference_value = ? WHERE user = ? AND preference_name = ?", new Object[] { propertyValue, userUid, propertyNamespace + "_" + propertyName });
			}
		}
		
		@Override
		public void setProperty(String propertyName, String propertyValue, String userUid) {
			
			String userPreferenceExists = 
				jdbc.find("SELECT ID FROM user_preferences WHERE user = ? AND preference_name = ? LIMIT 1", 
					new Object[] { userUid, "global_" + propertyName },
					new ResultProcessor<String>() {
				@SuppressWarnings("unchecked")
				@Override
				public String processRow(ResultSet rs) throws SQLException {
					return rs.getString("ID");
				}
			});
			
			if (userPreferenceExists == null) {	
				jdbc.insert("INSERT INTO user_preferences (user, preference_name, preference_value) VALUES (?, ?, ?)", new Object[] { userUid, "global_" + propertyName, propertyValue });
			}
			else {
				jdbc.update("UPDATE user_preferences SET preference_value = ? WHERE user = ? AND preference_name = ?", new Object[] { propertyValue, userUid, "global_" + propertyName });
			}
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
		public void doChanges(ModifiableHttpRequest request) {
		}

	}

	@Override
	public <Service extends ProxyService> RequestServiceProvider<Service> provideRequestService(HttpRequest request,
			Class<Service> serviceClass) throws ServiceUnavailableException {

		if (serviceClass.equals(UserPreferencesProviderService.class) && (request.getServicesHandle().isServiceAvailable(DatabaseConnectionProviderService.class))) {
			Connection connection = request.getServicesHandle().getService(DatabaseConnectionProviderService.class).getDatabaseConnection();
			return (RequestServiceProvider<Service>) new UserPreferencesProviderServiceProvider(connection);
		}
		return null;
	}

	@Override
	public void getProvidedRequestServices(Set<Class<? extends ProxyService>> providedServices) {
		providedServices.add(UserPreferencesProviderService.class);
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

	@Override
	public void desiredRequestServices(Set<Class<? extends ProxyService>> desiredServices, RequestHeader clientRQHeader) {
		desiredServices.add(StringContentService.class);
	}
	
	
}

package sk.fiit.rabbit.adaptiveproxy.plugins.services.logging.backends;

import java.sql.Connection;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;
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
import sk.fiit.peweproxy.services.content.ModifiableStringService;
import sk.fiit.rabbit.adaptiveproxy.plugins.common.JdbcTemplate;
import sk.fiit.rabbit.adaptiveproxy.plugins.common.MetallClient;
import sk.fiit.rabbit.adaptiveproxy.plugins.common.SqlUtils;
import sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions.DatabaseConnectionProviderService;
import sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions.LoggingBackendService;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class RelationalDBLoggingBackend implements RequestServiceModule, ResponseServiceModule {
	
	private static final Logger logger = Logger.getLogger(RelationalDBLoggingBackendProvider.class);

	private class RelationalDBLoggingBackendProvider implements LoggingBackendService,
			RequestServiceProvider<LoggingBackendService>, ResponseServiceProvider<LoggingBackendService> {
		
		private final DatabaseConnectionProviderService connectionProvider;

		public RelationalDBLoggingBackendProvider(DatabaseConnectionProviderService connectionProvider) {
			this.connectionProvider = connectionProvider;
		}

		@Override
		public String getServiceIdentification() {
			return this.getClass().getName();
		}

		@Override
		public LoggingBackendService getService() {
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
		
		@Override
		public void logPageAccess(String accessGuid, String userId, String uri, String content, String referrer, String ip,
								  String checksum, List<Map> terms) throws LoggingBackendFailure {
			Connection connection = connectionProvider.getDatabaseConnection();
			JdbcTemplate jdbc = new JdbcTemplate(connection);
			try {
				Integer pageId = (Integer) jdbc.queryFor("SELECT id FROM pages WHERE url = ? AND checksum = ?", new Object[] { uri, checksum }, Integer.class);
				
				if(pageId == null) {
					pageId = createNewPage(jdbc, uri, content, checksum, terms);
				}
				
				jdbc.insert("INSERT INTO access_logs(guid, userid, timestamp, page_id, referer, ip) VALUES(?,?,NOW(),?,?,?)", 
						new Object[] { accessGuid, userId, pageId, referrer, ip });
			} finally {
				SqlUtils.close(connection);
			}
		}

		private Integer createNewPage(JdbcTemplate jdbc, String uri, String content, String checksum, List<Map> terms) {
			Integer id = jdbc.insert("INSERT INTO pages(url, checksum, content_length) VALUES(?,?,?)", new Object[] { uri, checksum, content.length() });

			for(Map term : terms) {
				Integer termId = (Integer) jdbc.queryFor("SELECT id FROM terms WHERE label = ? AND term_type = ?", 
						new Object[] { term.get("name"), term.get("type") }, Integer.class);
				if (termId == null) {
					termId = jdbc.insert("INSERT INTO terms(label, term_type) VALUES(?, ?)", 
							new Object[] { term.get("name"), term.get("type") });
				}
				
				jdbc.insert("INSERT INTO pages_terms(page_id, term_id, weight, created_at, updated_at, source) VALUES (?,?,?,NOW(),NOW(),?)", 
						new Object[] { id, termId, term.get("relevance"), term.get("source") });
			}
			
			return id;
		}

		@Override
		public void logActivity(String userId, String accessGuid, String timeOnPage, String scrollCount, String copyCount) {
			Connection connection = connectionProvider.getDatabaseConnection();
			JdbcTemplate jdbc = new JdbcTemplate(connection);
			try {
				jdbc.update(
						"UPDATE access_logs " +
						   "SET userid = ?, time_on_page = time_on_page + ?, scroll_count = scroll_count + ?, copy_count = copy_count + ? " +
					     "WHERE guid = ?",
						new Object[] { userId, timeOnPage, scrollCount, copyCount, accessGuid });
			} finally {
				SqlUtils.close(connection);
			}
		}
	}

	@Override
	public boolean supportsReconfigure(PluginProperties newProps) {
		return true;
	}

	@Override
	public boolean start(PluginProperties props) {
		return true;
	}

	@Override
	public void stop() {
	}

	@Override
	public void desiredRequestServices(Set<Class<? extends ProxyService>> desiredServices, RequestHeader clientRQHeader) {
		desiredServices.add(DatabaseConnectionProviderService.class);
		desiredServices.add(ModifiableStringService.class); // TODO: hack
	}

	@Override
	public void desiredResponseServices(Set<Class<? extends ProxyService>> desiredServices, ResponseHeader webRPHeader) {
		desiredServices.add(DatabaseConnectionProviderService.class);
		desiredServices.add(ModifiableStringService.class); // TODO: hack
	}

	@Override
	public void getProvidedResponseServices(Set<Class<? extends ProxyService>> providedServices) {
		providedServices.add(LoggingBackendService.class);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <Service extends ProxyService> ResponseServiceProvider<Service> provideResponseService(
			HttpResponse response, Class<Service> serviceClass) throws ServiceUnavailableException {
		if (serviceClass.equals(LoggingBackendService.class)
				&& response.getServicesHandle().isServiceAvailable(DatabaseConnectionProviderService.class)) {
			DatabaseConnectionProviderService connectionProvider = response.getServicesHandle().getService(DatabaseConnectionProviderService.class);
			return (ResponseServiceProvider<Service>) new RelationalDBLoggingBackendProvider(connectionProvider);
		}

		return null;
	}

	@Override
	public void getProvidedRequestServices(Set<Class<? extends ProxyService>> providedServices) {
		providedServices.add(LoggingBackendService.class);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <Service extends ProxyService> RequestServiceProvider<Service> provideRequestService(HttpRequest request,
			Class<Service> serviceClass) throws ServiceUnavailableException {
		if (serviceClass.equals(LoggingBackendService.class)
				&& request.getServicesHandle().isServiceAvailable(DatabaseConnectionProviderService.class)) {
			DatabaseConnectionProviderService connectionProvider = request.getServicesHandle().getService(DatabaseConnectionProviderService.class);
			return (RequestServiceProvider<Service>) new RelationalDBLoggingBackendProvider(connectionProvider);
		}

		return null;
	}

}

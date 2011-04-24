package sk.fiit.rabbit.adaptiveproxy.plugins.services.logging.backends;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.jcouchdb.db.Database;
import org.jcouchdb.exception.NotFoundException;

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
import sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions.CouchDBProviderService;
import sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions.LoggingBackendService;

public class CouchDBLoggingBackend implements RequestServiceModule, ResponseServiceModule {
	
	private static final Logger logger = Logger.getLogger(CouchDBLoggingBackend.class);

	private class CouchDBLoggingBackendProvider implements LoggingBackendService,
			RequestServiceProvider<LoggingBackendService>, ResponseServiceProvider<LoggingBackendService> {
		
		private final Database couch;

		public CouchDBLoggingBackendProvider(CouchDBProviderService couchProvider) {
			this.couch = couchProvider.getDatabase();
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
		
		@SuppressWarnings({ "rawtypes", "unchecked" })
		@Override
		public void logPageAccess(String accessGuid, String userId, String uri, String content, String referrer, String ip, 
								  String checksum, List<Map> terms) throws LoggingBackendFailure {

			HashMap page;
			
			try {
				page = couch.getDocument(HashMap.class, uri);

				if(!page.get("checksum").equals(checksum)) {
					page.put("checksum", checksum);
					page.put("content_length", content.length());
					page.put("terms", terms);
					couch.updateDocument(page);
				}
			} catch (NotFoundException e) {
				page = new HashMap();
				page.put("_id", uri);
				page.put("checksum", checksum);
				page.put("type", "page");
				page.put("content_length", content.length());
				page.put("terms", terms);
				couch.createDocument(page);
			}
			
			Map access = new HashMap();
			access.put("_id", accessGuid);
			access.put("type", "access");
			access.put("page", page.get("_id"));
			access.put("page_rev", page.get("_rev"));
			access.put("user_id", userId);
			access.put("referrer", referrer);
			access.put("ip", ip);
			couch.createDocument(access);
		}

		@SuppressWarnings({ "rawtypes", "unchecked" })
		@Override
		public void logActivity(String userId, String accessGuid, String timeOnPage, String scrollCount, String copyCount) {
			Map access = couch.getDocument(HashMap.class, accessGuid);
			access.put("user_id", userId);
			increment(access, "time_on_page", timeOnPage);
			increment(access, "scroll_count", scrollCount);
			increment(access, "copy_count", copyCount);
			couch.updateDocument(access);
		}
		
		@SuppressWarnings({ "unchecked", "rawtypes" })
		private void increment(Map document, String property, String increment) {
			Long old = (Long) document.get(property);
			if(old == null) old = 0l;
			Long n = old + Long.parseLong(increment);
			document.put(property, n);
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
	}

	@Override
	public void desiredResponseServices(Set<Class<? extends ProxyService>> desiredServices, ResponseHeader webRPHeader) {
		desiredServices.add(CouchDBProviderService.class);
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
			CouchDBProviderService couchProvider = response.getServicesHandle().getService(CouchDBProviderService.class);
			return (ResponseServiceProvider<Service>) new CouchDBLoggingBackendProvider(couchProvider);
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
			CouchDBProviderService couchProvider = request.getServicesHandle().getService(CouchDBProviderService.class);
			return (RequestServiceProvider<Service>) new CouchDBLoggingBackendProvider(couchProvider);
		}

		return null;
	}

}

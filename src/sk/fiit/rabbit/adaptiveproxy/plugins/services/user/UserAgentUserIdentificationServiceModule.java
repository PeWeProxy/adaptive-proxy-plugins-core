package sk.fiit.rabbit.adaptiveproxy.plugins.services.user;

import java.util.Set;

import org.apache.log4j.Logger;

import sk.fiit.peweproxy.headers.ReadableHeader;
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
import sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions.UserIdentificationService;

public class UserAgentUserIdentificationServiceModule implements RequestServiceModule, ResponseServiceModule {
	
	private static final int APUID_LENGTH = 32;

	private static final Logger logger = Logger.getLogger(UserAgentUserIdentificationServiceModule.class);

	String idPart = null;

	private class UserServiceProvider implements UserIdentificationService,
			RequestServiceProvider<UserIdentificationService>,
			ResponseServiceProvider<UserIdentificationService> {
		
		private final String userId;
		
		public UserServiceProvider(String userIDString) {
			this.userId = userIDString;
		}
		
		@Override
		public String getClientIdentification() {
			return userId;
		}

		@Override
		public String getServiceIdentification() {
			return this.getClass().getName();
		}

		@Override
		public UserIdentificationService getService() {
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
	
	private String getUIDForMessage(ReadableHeader headers) {
		String uaHeader =  headers.getField("Cookie");
		
		if (uaHeader != null) {
			int indexOfIdPart = uaHeader.indexOf(idPart);
			
			if (indexOfIdPart != -1) {
				indexOfIdPart += idPart.length();
				
				String uid = uaHeader.substring(indexOfIdPart, indexOfIdPart + APUID_LENGTH);
				
				if (!uid.isEmpty()) {
					return uid;
				}
			}
		}
		return null;
	}
	

	@Override
	public boolean start(PluginProperties props) {
		idPart = props.getProperty("idPart");
		return true;
	}


	@Override
	public boolean supportsReconfigure(PluginProperties newProps) {
		return true;
	}


	@Override
	public void stop() {
	}


	@Override
	public void desiredRequestServices(
			Set<Class<? extends ProxyService>> desiredServices,
			RequestHeader clientRQHeader) {
		// no-dependencies
	}


	@Override
	public void desiredResponseServices(
			Set<Class<? extends ProxyService>> desiredServices,
			ResponseHeader webRPHeader) {
		// no-dependencies
	}


	@Override
	public void getProvidedResponseServices(
			Set<Class<? extends ProxyService>> providedServices) {
		providedServices.add(UserIdentificationService.class);
	}


	@Override
	public <Service extends ProxyService> ResponseServiceProvider<Service> provideResponseService(
			HttpResponse response, Class<Service> serviceClass)
			throws ServiceUnavailableException {
		if(serviceClass.equals(UserIdentificationService.class)) {
			String userIDString = getUIDForMessage(response.getRequest().getRequestHeader());
			return (ResponseServiceProvider<Service>) new UserServiceProvider(userIDString);
		}
		
		return null;
	}


	@Override
	public void getProvidedRequestServices(
			Set<Class<? extends ProxyService>> providedServices) {
		providedServices.add(UserIdentificationService.class);
	}


	@Override
	public <Service extends ProxyService> RequestServiceProvider<Service> provideRequestService(
			HttpRequest request, Class<Service> serviceClass)
			throws ServiceUnavailableException {
		
		if(serviceClass.equals(UserIdentificationService.class)) {
			String userIDString = getUIDForMessage(request.getRequestHeader());
			return (RequestServiceProvider<Service>) new UserServiceProvider(userIDString);
		}
		
		return null;
	}

}

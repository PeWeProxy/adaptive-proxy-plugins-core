package sk.fiit.rabbit.adaptiveproxy.plugins.services.user;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import sk.fiit.rabbit.adaptiveproxy.plugins.PluginProperties;
import sk.fiit.rabbit.adaptiveproxy.plugins.headers.ReadableHeaders;
import sk.fiit.rabbit.adaptiveproxy.plugins.helpers.RequestAndResponseServicePluginAdapter;
import sk.fiit.rabbit.adaptiveproxy.plugins.helpers.RequestAndResponseServiceProviderAdapter;
import sk.fiit.rabbit.adaptiveproxy.plugins.messages.HttpRequest;
import sk.fiit.rabbit.adaptiveproxy.plugins.messages.HttpResponse;
import sk.fiit.rabbit.adaptiveproxy.plugins.messages.ModifiableHttpRequest;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.ProxyService;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.RequestServiceProvider;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.ResponseServiceProvider;

public class UserAgentUserIdentification extends RequestAndResponseServicePluginAdapter {
	
	private static final int APUID_LENGTH = 32;

	private static final Logger logger = Logger.getLogger(UserAgentUserIdentification.class);
	
	private static final String USER_AGENT = "User-Agent";

	String idPart = null;
	
	private class UserServiceProvider extends RequestAndResponseServiceProviderAdapter implements UserIdentificationService{
		private final String userId;
		
		public UserServiceProvider(String userIDString) {
			if(userIDString == null) {
				this.userId = null;
			} else {
				this.userId = userIDString;
			}
		}
		
		@Override
		public Class<? extends ProxyService> getServiceClass() {
			return UserIdentificationService.class;
		}

		@Override
		public String getClientIdentification() {
			return userId;
		}
	}
	
	private String getUIDForMessage(ReadableHeaders headers) {
		String uaHeader =  headers.getHeader(USER_AGENT);
		
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
	public List<RequestServiceProvider> provideRequestServices(
			HttpRequest request) {
		List<RequestServiceProvider> retVal = null;
		String uidString = getUIDForMessage(request.getClientRequestHeaders());
		if (uidString != null) {
		retVal = new ArrayList<RequestServiceProvider>(1);
		retVal.add(new UserServiceProvider(uidString));
		}
		return retVal;
	}
	
	@Override
	protected void addProvidedServices(Set<Class<? extends ProxyService>> providedServices) {
		providedServices.add(UserIdentificationService.class);
	}

	@Override
	public boolean setup(PluginProperties props) {
		idPart = props.getProperty("idPart");
		return true;
	}

	@Override
	public List<ResponseServiceProvider> provideResponseServices(
			HttpResponse response) {
		List<ResponseServiceProvider> retVal = null;
		String uidString = getUIDForMessage(response.getClientRequestHeaders());
		if (uidString != null) {
			retVal = new ArrayList<ResponseServiceProvider>(1);
			retVal.add(new UserServiceProvider(uidString));
		}
		return retVal;
	}

}

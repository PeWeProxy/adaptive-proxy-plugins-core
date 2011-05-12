package sk.fiit.rabbit.adaptiveproxy.plugins.services.common;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import sk.fiit.peweproxy.headers.RequestHeader;
import sk.fiit.peweproxy.messages.HttpRequest;
import sk.fiit.peweproxy.messages.ModifiableHttpRequest;
import sk.fiit.peweproxy.plugins.PluginProperties;
import sk.fiit.peweproxy.plugins.services.RequestServiceModule;
import sk.fiit.peweproxy.plugins.services.RequestServiceProvider;
import sk.fiit.peweproxy.services.ProxyService;
import sk.fiit.peweproxy.services.ServiceUnavailableException;
import sk.fiit.peweproxy.services.content.StringContentService;
import sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions.RequestDataParserService;

public class RequestDataParserServiceModule implements RequestServiceModule {

	private static final Logger logger = Logger.getLogger(RequestDataParserServiceModule.class);

	private class GettingPostDataProviderService implements RequestDataParserService, RequestServiceProvider<RequestDataParserService> {

		protected String content;
		protected String url;
		
		public GettingPostDataProviderService(String content, String url) {
			this.content = content;
			this.url = url;
		}

		private Map<String, String> postData;
		private Map<String, String> getData;
		
		@Override
		public Map<String, String> getDataFromPOST() {
			if (postData != null) {
				return postData;
			}

			postData = getDataFromPostRequest();

			return postData;
		}

		private Map<String, String> getDataFromPostRequest() {
			try {
				content = URLDecoder.decode(content, "utf-8");
			} catch (UnsupportedEncodingException e) {
				logger.warn(e);
			}

			return getDataMap(content);
		}

		@Override
		public Map<String, String> getDataFromGET() {
			if (getData != null) {
				return getData;
			}

			getData = getDataFromGetRequest();

			return getData;
		}
		
		private Map<String, String> getDataFromGetRequest() {
			String data = url.split("?")[1];

			return getDataMap(data);
		}
		
		private Map<String, String> getDataMap(String data) {
		    Map<String, String> map = new HashMap<String, String>();
		    String attributeName;
		    String attributeValue;
		    for (String postPair : data.split("&")) {
			if (postPair.split("=").length == 2) {
			    attributeName = postPair.split("=")[0];
			    attributeValue = postPair.split("=")[1];
			    map.put(attributeName, attributeValue);
        		}
		    }

		    return map;
		}
		@Override
		public String getServiceIdentification() {
			return this.getClass().getName();
		}

		@Override
		public RequestDataParserService getService() {
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

		if (serviceClass.equals(RequestDataParserService.class)
				&& request.getServicesHandle().isServiceAvailable(StringContentService.class)) {

			String content = request.getServicesHandle().getService(StringContentService.class).getContent();
			String url = request.getRequestHeader().getRequestURI();
			
			return (RequestServiceProvider<Service>) new GettingPostDataProviderService(content, url);
		}

		return null;
	}

	@Override
	public void getProvidedRequestServices(Set<Class<? extends ProxyService>> providedServices) {
		providedServices.add(RequestDataParserService.class);
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

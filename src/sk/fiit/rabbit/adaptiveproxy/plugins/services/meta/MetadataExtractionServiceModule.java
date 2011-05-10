package sk.fiit.rabbit.adaptiveproxy.plugins.services.meta;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.LinkedList;
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
import sk.fiit.peweproxy.services.content.StringContentService;
import sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions.MetadataExtractionService;
import sk.fiit.rabbit.adaptiveproxy.plugins.utils.MetallClient;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class MetadataExtractionServiceModule implements RequestServiceModule, ResponseServiceModule {
	
	private static final Logger logger = Logger.getLogger(MetadataExtractionServiceModule.class);
	
	private class MetadataExtractionServiceProvider implements
			RequestServiceProvider<MetadataExtractionService>,
			ResponseServiceProvider<MetadataExtractionService>,
			MetadataExtractionService {

		private String content;

		public MetadataExtractionServiceProvider(String content) {
			this.content = content;
		}

		@Override
		public MetadataExtractionService getService() {
			return this;
		}

		@Override
		public boolean initChangedModel() {
			return false;
		}

		@Override
		public void doChanges(ModifiableHttpResponse response) {
			// no-op
		}

		@Override
		public String getServiceIdentification() {
			return this.getClass().getSimpleName();
		}

		@Override
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public List<Map> metadata() {
			List terms = new LinkedList();
			
			try {
				JsonElement keywords = new JsonParser().parse(new MetallClient().keywords(content));
				for(JsonElement keyword : keywords.getAsJsonArray()) {
					JsonObject keywordObject = keyword.getAsJsonObject();
					String name = keywordObject.get("name").getAsString();
					String type = keywordObject.get("type").getAsString();
					String relevance = keywordObject.get("relevance").getAsString();
					try {
						double floatRelevance = Double.parseDouble(relevance);
						relevance = new DecimalFormat("#.##").format(floatRelevance);
					} catch (NumberFormatException e) {
						relevance = null;
					}
					String source = keywordObject.get("source").getAsString();
					
					Map term = new HashMap();
					term.put("name", name);
					term.put("type", type);
					term.put("relevance", relevance);
					term.put("source", source);
					
					terms.add(term);
				}
			} catch(Exception e) {
				logger.warn("Could not retrieve terms.", e);
			}
			
			return terms;
		}

		@Override
		public void doChanges(ModifiableHttpRequest request) {
			// no-op
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
	public void desiredResponseServices(
			Set<Class<? extends ProxyService>> desiredServices,
			ResponseHeader webRPHeader) {
		desiredServices.add(StringContentService.class);
	}

	@Override
	public void getProvidedResponseServices(
			Set<Class<? extends ProxyService>> providedServices) {
		providedServices.add(MetadataExtractionService.class);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <Service extends ProxyService> ResponseServiceProvider<Service> provideResponseService(
			HttpResponse response, Class<Service> serviceClass)
			throws ServiceUnavailableException {
		
		if(serviceClass.equals(MetadataExtractionService.class)) {
			String content = response.getServicesHandle().getService(StringContentService.class).getContent();
			return (ResponseServiceProvider<Service>) new MetadataExtractionServiceProvider(content);
		}
		
		return null;
	}

	@Override
	public void desiredRequestServices(
			Set<Class<? extends ProxyService>> desiredServices,
			RequestHeader clientRQHeader) {
		desiredServices.add(StringContentService.class);
	}

	@Override
	public void getProvidedRequestServices(
			Set<Class<? extends ProxyService>> providedServices) {
		providedServices.add(MetadataExtractionService.class);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <Service extends ProxyService> RequestServiceProvider<Service> provideRequestService(
			HttpRequest request, Class<Service> serviceClass)
			throws ServiceUnavailableException {
		
		if(serviceClass.equals(MetadataExtractionService.class)) {
			String content = request.getServicesHandle().getService(StringContentService.class).getContent();
			return (RequestServiceProvider<Service>) new MetadataExtractionServiceProvider(content);
		}
		
		return null;
	}

}

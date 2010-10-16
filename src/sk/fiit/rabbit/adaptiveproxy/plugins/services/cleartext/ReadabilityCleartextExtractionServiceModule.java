package sk.fiit.rabbit.adaptiveproxy.plugins.services.cleartext;

import java.util.Set;

import org.apache.log4j.Logger;

import sk.fiit.keyextractor.exceptions.TextFilteringException;
import sk.fiit.keyextractor.filters.io.StringSource;
import sk.fiit.keyextractor.filters.parser.ReadabilityParser;
import sk.fiit.peweproxy.headers.ResponseHeader;
import sk.fiit.peweproxy.messages.HttpResponse;
import sk.fiit.peweproxy.messages.ModifiableHttpResponse;
import sk.fiit.peweproxy.plugins.PluginProperties;
import sk.fiit.peweproxy.plugins.services.ResponseServiceModule;
import sk.fiit.peweproxy.plugins.services.ResponseServiceProvider;
import sk.fiit.peweproxy.services.ProxyService;
import sk.fiit.peweproxy.services.ServiceUnavailableException;
import sk.fiit.peweproxy.services.content.StringContentService;

public class ReadabilityCleartextExtractionServiceModule implements ResponseServiceModule {
	
	private static final Logger logger = Logger.getLogger(ReadabilityCleartextExtractionServiceModule.class);
	
	private class ReadabilityCleartextExtractionServiceProvider
			implements
			ClearTextExtractionService,
			ResponseServiceProvider<ReadabilityCleartextExtractionServiceProvider> {

		private String content;
		private String clearText;
		
		public ReadabilityCleartextExtractionServiceProvider(String content) {
			this.content = content;
		}
		
		@Override
		public String getCleartext() {
			if(clearText == null) {
				try{
					clearText = new ReadabilityParser(new StringSource(content)).process();
					if(clearText == null) {
						clearText = content;
					}
				} catch(TextFilteringException e) {
					logger.debug("Readability parser FAILED", e);
					clearText = content;
				}
			}
			
			return clearText;
		}

		@Override
		public String getServiceIdentification() {
			return this.getClass().getName();
		}

		@Override
		public ReadabilityCleartextExtractionServiceProvider getService() {
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
		providedServices.add(ClearTextExtractionService.class);
	}

	@Override
	public <Service extends ProxyService> ResponseServiceProvider<Service> provideResponseService(
			HttpResponse response, Class<Service> serviceClass)
			throws ServiceUnavailableException {
		
		if(serviceClass.equals(ClearTextExtractionService.class)) {
			String content = response.getServicesHandle().getService(StringContentService.class).getContent();
			return (ResponseServiceProvider<Service>) new ReadabilityCleartextExtractionServiceProvider(content);
		}
		
		return null;
	}


}

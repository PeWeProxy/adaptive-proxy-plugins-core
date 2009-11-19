package sk.fiit.rabbit.adaptiveproxy.plugins.services.cleartext;

import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import sk.fiit.keyextractor.exceptions.TextFilteringException;
import sk.fiit.keyextractor.filters.io.StringSource;
import sk.fiit.keyextractor.filters.parser.ReadabilityParser;
import sk.fiit.rabbit.adaptiveproxy.plugins.headers.ResponseHeaders;
import sk.fiit.rabbit.adaptiveproxy.plugins.helpers.ResponseServicePluginAdapter;
import sk.fiit.rabbit.adaptiveproxy.plugins.helpers.ResponseServiceProviderAdapter;
import sk.fiit.rabbit.adaptiveproxy.plugins.messages.HttpResponse;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.ProxyService;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.ResponseServiceProvider;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.ServiceUnavailableException;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.content.StringContentService;

public class ReadabilityCleartextExtractionService extends ResponseServicePluginAdapter {
	
	private static final Logger logger = Logger.getLogger(ReadabilityCleartextExtractionService.class);
	
	private class ReadabilityCleartextExtractionServiceProvider extends ResponseServiceProviderAdapter implements ClearTextExtractionService {

		private String content;
		private String clearText;
		
		public ReadabilityCleartextExtractionServiceProvider(String content) {
			this.content = content;
		}
		
		@Override
		public Class<? extends ProxyService> getServiceClass() {
			return ClearTextExtractionService.class;
		}

		@Override
		public String getCleartext() {
			
			
			
			if(clearText == null) {
				
				try{
					clearText = new ReadabilityParser(new StringSource(content)).process();
				} catch(TextFilteringException e) {
					logger.debug("Readability parser FAILED", e);
					clearText = content;
				}
			}
			
			return clearText;
		}
	}

	@Override
	protected void addDependencies(Set<Class<? extends ProxyService>> dependencies) {
		dependencies.add(StringContentService.class);
	}

	@Override
	protected void addProvidedServices(Set<Class<? extends ProxyService>> providedServices) {
		providedServices.add(ClearTextExtractionService.class);
	}

	@Override
	protected void addProvidedResponseServices(List<ResponseServiceProvider> providedServices, HttpResponse response) {
		try {
			String content = response.getServiceHandle().getService(StringContentService.class).getContent();
			providedServices.add(new ReadabilityCleartextExtractionServiceProvider(content));
		} catch (ServiceUnavailableException e) {
			logger.debug("StringContentService unavailable, that makes ReadabilityCleartextExtractionService unavailable too");
		}
	}

	@Override
	public boolean wantResponseContent(ResponseHeaders webRPHeaders) {
		return true;
	}

}

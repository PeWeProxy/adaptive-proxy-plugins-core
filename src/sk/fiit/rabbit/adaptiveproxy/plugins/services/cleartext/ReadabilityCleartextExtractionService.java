package sk.fiit.rabbit.adaptiveproxy.plugins.services.cleartext;

import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import sk.fiit.keyextractor.downloader.WebPageAquirer;
import sk.fiit.keyextractor.exceptions.JKeyExtractorException;
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
		
		public ReadabilityCleartextExtractionServiceProvider(String content) {
			this.content = content;
		}
		
		@Override
		public Class<? extends ProxyService> getServiceClass() {
			return ClearTextExtractionService.class;
		}

		@Override
		public String getCleartext() {
			
			String clearText = null;
			try {
				clearText = WebPageAquirer.getPageContent(content);
			} catch (JKeyExtractorException e) {
				logger.error("clearTextExtraction failed", e);
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

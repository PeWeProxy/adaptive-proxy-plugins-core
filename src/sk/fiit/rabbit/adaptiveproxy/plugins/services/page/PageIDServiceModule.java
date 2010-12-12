package sk.fiit.rabbit.adaptiveproxy.plugins.services.page;

import java.util.Set;

import org.apache.log4j.Logger;

import sk.fiit.peweproxy.headers.ResponseHeader;
import sk.fiit.peweproxy.messages.HttpResponse;
import sk.fiit.peweproxy.messages.ModifiableHttpResponse;
import sk.fiit.peweproxy.plugins.PluginProperties;
import sk.fiit.peweproxy.plugins.services.ResponseServiceModule;
import sk.fiit.peweproxy.plugins.services.ResponseServiceProvider;
import sk.fiit.peweproxy.services.ProxyService;
import sk.fiit.peweproxy.services.ServiceUnavailableException;
import sk.fiit.peweproxy.services.content.StringContentService;
import sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions.ClearTextExtractionService;
import sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions.PageIDService;
import sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions.PostDataParserService;

public class PageIDServiceModule implements ResponseServiceModule {
	
	private static final Logger logger = Logger.getLogger(PageIDServiceModule.class);
	
	private class PageIDServiceProvider
			implements PageIDService, ResponseServiceProvider<PageIDService> {

		private String id;
		
		public PageIDServiceProvider() {
		}
		

		@Override
		public String getServiceIdentification() {
			return this.getClass().getName();
		}

		@Override
		public PageIDServiceProvider getService() {
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
		public String getID() {
		    if (this.id == null)
		    {
			this.id = Double.toString(Math.random());
		    }
		    return this.id;
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
	}

	@Override
	public void getProvidedResponseServices(
			Set<Class<? extends ProxyService>> providedServices) {
		providedServices.add(PageIDService.class);
	}

	@Override
	public <Service extends ProxyService> ResponseServiceProvider<Service> provideResponseService(
			HttpResponse response, Class<Service> serviceClass)
			throws ServiceUnavailableException {
	    if(serviceClass.equals(PageIDService.class)) {
		return (ResponseServiceProvider<Service>) new PageIDServiceProvider();
	    }
	    
	    return null;
	}


}

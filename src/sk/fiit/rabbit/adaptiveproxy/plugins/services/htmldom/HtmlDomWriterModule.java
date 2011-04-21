package sk.fiit.rabbit.adaptiveproxy.plugins.services.htmldom;

import java.util.Set;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import sk.fiit.peweproxy.headers.ResponseHeader;
import sk.fiit.peweproxy.messages.HttpResponse;
import sk.fiit.peweproxy.messages.ModifiableHttpResponse;
import sk.fiit.peweproxy.plugins.PluginProperties;
import sk.fiit.peweproxy.plugins.services.ResponseServiceModule;
import sk.fiit.peweproxy.plugins.services.ResponseServiceProvider;
import sk.fiit.peweproxy.services.ProxyService;
import sk.fiit.peweproxy.services.ServiceUnavailableException;
import sk.fiit.peweproxy.services.content.ModifiableStringService;
import sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions.HtmlDomReaderService;
import sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions.HtmlDomWriterService;

public class HtmlDomWriterModule implements ResponseServiceModule {
	
	private static final Logger logger = Logger.getLogger(HtmlDomWriterModule.class);
	
	private class HtmlDomWriterProvider
			implements HtmlDomWriterService, ResponseServiceProvider<HtmlDomWriterProvider> {

		private Document document;
		
		public HtmlDomWriterProvider(Document document) {
			this.document = document;
		}
		
		@Override
		public String getServiceIdentification() {
			return this.getClass().getName();
		}

		@Override
		public HtmlDomWriterProvider getService() {
			return this;
		}

		@Override
		public boolean initChangedModel() {
			return true;
		}

		public void setHTMLDom(Document document) {
			this.document = document;
		}

		@Override
		public void doChanges(ModifiableHttpResponse response) {
			Document modifiedDocument = (Document) document.clone();
			Format format = Format.getRawFormat();
			format.setExpandEmptyElements(true);
			XMLOutputter outputter = new XMLOutputter(format);
            String modifiedContent = outputter.outputString(modifiedDocument);
            
            //FIXME: this is just a hot fix, better parser needed
            modifiedContent = StringEscapeUtils.unescapeHtml(modifiedContent);
            modifiedContent = StringEscapeUtils.unescapeHtml(modifiedContent);
            modifiedContent = modifiedContent.replaceAll("<br></br>", "<br/>");
            
            response.getServicesHandle().getService(ModifiableStringService.class).setContent(modifiedContent);
		}

		@Override
		public Document getHTMLDom() {
			return this.document;
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
		desiredServices.add(ModifiableStringService.class);
		desiredServices.add(HtmlDomReaderService.class);
	}

	@Override
	public void getProvidedResponseServices(
			Set<Class<? extends ProxyService>> providedServices) {
		providedServices.add(HtmlDomWriterService.class);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <Service extends ProxyService> ResponseServiceProvider<Service> provideResponseService(
			HttpResponse response, Class<Service> serviceClass)
			throws ServiceUnavailableException {
			
		if (serviceClass.equals(HtmlDomWriterService.class)
				&& response.getServicesHandle().isServiceAvailable(ModifiableStringService.class)
				&& response.getServicesHandle().isServiceAvailable(HtmlDomReaderService.class)) {
			
			Document document = response.getServicesHandle().getService(HtmlDomReaderService.class).getHTMLDom();
			return (ResponseServiceProvider<Service>) new HtmlDomWriterProvider(document);
		}
		
		return null;
	}

}

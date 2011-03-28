package sk.fiit.rabbit.adaptiveproxy.plugins.services.htmldom;

import java.io.IOException;
import java.util.Set;

import org.apache.log4j.Logger;
import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.JDomSerializer;
import org.htmlcleaner.TagNode;
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
import sk.fiit.peweproxy.services.content.StringContentService;
import sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions.HtmlDomBuilderService;


public class HtmlDomBuilderModule implements ResponseServiceModule {
	
	private static final Logger logger = Logger.getLogger(HtmlDomBuilderModule.class);
	
	private class HtmlDomBuilderProvider
			implements HtmlDomBuilderService, ResponseServiceProvider<HtmlDomBuilderProvider> {

		private String content;
		private Document document;
		
		public HtmlDomBuilderProvider(String content) {
			this.content = content;
		}
		
		public Document getHTMLDom() {
			Document document = null;

	        try {
	        	// initialize HtmlCleaner parser  
	    		HtmlCleaner cleaner = new HtmlCleaner();
	    		CleanerProperties props = cleaner.getProperties();
	    		props.setUseCdataForScriptAndStyle(false);
	    		
	    		// parse
	    		TagNode node = cleaner.clean(content);
	    		
	        	// store to jdom
	    		document = new JDomSerializer(props, true).createJDom(node);

	        } catch (IOException e) {
	            logger.error("Html parser IO exception.", e);
	        } 
	        this.document = document; 
			return (Document)document.clone();
		}
		
		@Override
		public String getServiceIdentification() {
			return this.getClass().getName();
		}

		@Override
		public HtmlDomBuilderProvider getService() {
			return this;
		}

		@Override
		public boolean initChangedModel() {
			return true;
		}

		@Override
		public void doChanges(ModifiableHttpResponse response) {
			// do nothing
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
		providedServices.add(HtmlDomBuilderService.class);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <Service extends ProxyService> ResponseServiceProvider<Service> provideResponseService(
			HttpResponse response, Class<Service> serviceClass)
			throws ServiceUnavailableException {
		
		if(serviceClass.equals(HtmlDomBuilderService.class)) {
			String content = response.getServicesHandle().getService(StringContentService.class).getContent();
			return (ResponseServiceProvider<Service>) new HtmlDomBuilderProvider(content);
		}
		
		return null;
	}

	public static void main(String[] args) {
		String content = "<html><body>";
			for (int i = 0; i < 100; i++)
				content += "<li><table><tr><td>222222222222222</table>\n";
			content += "</body></html>";
		
		Document document = null;

		// Create an instance of the tester and test
        try {

    		HtmlCleaner cleaner = new HtmlCleaner();
    		CleanerProperties props = cleaner.getProperties();
    		TagNode node = cleaner.clean(content);
        	
    		document = new JDomSerializer(props, true).createJDom(node);

    		XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
            outputter.output(document, System.out);
        } catch (IOException e) {
            e.printStackTrace();
        } 
	}
}

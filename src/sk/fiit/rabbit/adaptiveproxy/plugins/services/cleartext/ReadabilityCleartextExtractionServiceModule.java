package sk.fiit.rabbit.adaptiveproxy.plugins.services.cleartext;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Set;

import org.apache.log4j.Logger;

import sk.fiit.keyextractor.exceptions.TextFilteringException;
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
import sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions.ClearTextExtractionService;

public class ReadabilityCleartextExtractionServiceModule implements ResponseServiceModule {
	private static final String serviceMethod = "GET";
	private static final String readabilityServiceLocation = "http://peweproxy.fiit.stuba.sk/metall/readability";
	
	private static final Logger logger = Logger.getLogger(ReadabilityCleartextExtractionServiceModule.class);
	
	private class ReadabilityCleartextExtractionServiceProvider
			implements ClearTextExtractionService, ResponseServiceProvider<ReadabilityCleartextExtractionServiceProvider> {

		private String requestURI;
		private String content;
		private String clearText;
		
		public ReadabilityCleartextExtractionServiceProvider(String requestURI) {
			this.requestURI = requestURI;
		}
		
		@Override
		public String getCleartext() {

			if(clearText == null) {
				try {
					clearText = MetallReadabilityCleartextClient(requestURI);
				} catch(IOException e) {
					logger.debug("Metall readability cleartext client parser FAILED", e);
					clearText = content;
				} 
			}
			
			return clearText;
			
		}
		

		private String MetallReadabilityCleartextClient(String sourceURL) throws IOException {
		    String clearText = null;
		    
			URL serviceCallURL = new URL(readabilityServiceLocation+"?url="+sourceURL);

		    HttpURLConnection connection = (HttpURLConnection)serviceCallURL.openConnection();
		    connection.setRequestMethod(serviceMethod);
		    connection.setDoInput(true);
		    connection.setAllowUserInteraction(false);
		    connection.setRequestProperty("Accept", "text/html, application/xml;q=0.9, application/xhtml+xml, image/png, image/jpeg, image/gif, image/x-xbitmap, */*;q=0.1");
		    connection.setRequestProperty("Accept-Language", "sk-SK,sk;q=0.9,en;q=0.8");
		    connection.setRequestProperty("Accept-Charset", "iso-8859-1, utf-8, utf-16, *;q=0.1");
		    connection.setRequestProperty("Accept-Encoding", "deflate, gzip, x-gzip, identity, *;q=0");

		    OutputStream os = new ByteArrayOutputStream();
		    InputStream is = connection.getInputStream();

		    connection.connect();
		    
		    byte[] response = new byte[4096];
		    while (is.read(response) != -1) {
		    	os.write(response);
		    }

		    connection.disconnect();
	    	is.close();
		    os.flush();
		    
		    if(os != null) {
		    	clearText = os.toString();
		    	os.close();
		    }
		    
		    System.out.println("\n\n\n"+clearText);
			    
			return(clearText);
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
		
		if (serviceClass.equals(ClearTextExtractionService.class) && response.getResponseHeader().getField("content-type").contains("text/html")) {
			String requestURI = response.getRequest().getOriginalRequest().getRequestHeader().getRequestURI();
			return (ResponseServiceProvider<Service>) new ReadabilityCleartextExtractionServiceProvider(requestURI);
		}

		return null;
	}


}

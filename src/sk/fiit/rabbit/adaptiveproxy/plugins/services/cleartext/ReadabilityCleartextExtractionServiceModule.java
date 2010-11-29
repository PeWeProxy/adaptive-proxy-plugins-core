package sk.fiit.rabbit.adaptiveproxy.plugins.services.cleartext;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.Buffer;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Set;

import org.apache.log4j.Logger;

import rabbit.util.CharsetUtils;
import sk.fiit.peweproxy.headers.ReadableHeader;
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
	private static final String readabilityServiceLocation = "http://peweproxy-staging.fiit.stuba.sk/metall/readability";
	
	private static final Logger logger = Logger.getLogger(ReadabilityCleartextExtractionServiceModule.class);
	
	private class ReadabilityCleartextExtractionServiceProvider
			implements ClearTextExtractionService, ResponseServiceProvider<ReadabilityCleartextExtractionServiceProvider> {

		private String requestURI;
		private String content;
		private String clearText;
		private String charset;
		
		public ReadabilityCleartextExtractionServiceProvider(String requestURI, String charset, String content) {
			this.requestURI = requestURI;
			this.charset = charset;
			this.content = content;
		}
		
		@Override
		public String getCleartext() {

			if(clearText == null) {
				try {
					clearText = MetallReadabilityCleartextClient(requestURI, charset);
				} catch(IOException e) {
					// TODO: some error with response 500, when sending img url
					logger.debug("Metall readability cleartext client parser FAILED:"+e.getMessage());
					clearText = content;
				} 
			}
			
			return clearText;
		}
		

		private String MetallReadabilityCleartextClient(String requestURI, String charset) throws IOException {
		    String clearText = null;
		    
			URL serviceCallURL = new URL(readabilityServiceLocation+"/?url="+requestURI);

		    HttpURLConnection connection = (HttpURLConnection)serviceCallURL.openConnection();
		    connection.setRequestMethod(serviceMethod);
		    connection.setDoInput(true);
		    connection.setDoOutput(true);
		    connection.setAllowUserInteraction(false);
		    connection.setRequestProperty("Accept", "text/html, application/xml;q=0.9, */*;q=0.1");
		    connection.setRequestProperty("Accept-Language", "sk-SK,sk;q=0.9,en;q=0.8");
		    
		    if(charset != null) {
		    	connection.setRequestProperty("Accept-Charset", charset+";q=1");
		    } else {
		    	connection.setRequestProperty("Accept-Charset", "windows-1250, utf-8, iso-8859-2, iso-8859-1;q=0.2, utf-16;q=0.1, *;q=0.1");
		    }
		    
		    ByteArrayOutputStream os = new ByteArrayOutputStream();
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
			    if(charset == null) {
			    	charset="iso-8859-2";
			    }
		    	Buffer charBuffer = CharsetUtils.decodeBytes(os.toByteArray(), Charset.forName(charset), false);
		    	clearText = charBuffer.toString();
//		    	clearText = os.toString();
		    	os.close();
		    }
		    
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
		
		if (serviceClass.equals(ClearTextExtractionService.class)
//				&& response.getResponseHeader().getField("Content-Type").contains("text/html")
				&& response.getServicesHandle().isServiceAvailable(StringContentService.class)) {
			String requestURI = response.getRequest().getOriginalRequest().getRequestHeader().getRequestURI();
			String content = response.getServicesHandle().getService(StringContentService.class).getContent();
			
			String charset = null;
			try {
				charset = CharsetUtils.detectCharset((ReadableHeader)response.getResponseHeader(), content.getBytes(), false).toString();
			} catch (UnsupportedCharsetException e) {
				logger.debug("Unable to detect character set:"+e.getMessage());
			} catch (IOException e) {
				logger.error("Wrong input. This should not happens:"+e.getMessage());
			}
			
			return (ResponseServiceProvider<Service>) new ReadabilityCleartextExtractionServiceProvider(requestURI, charset, content);
		}
		

		return null;
	}


}

package sk.fiit.rabbit.adaptiveproxy.plugins.services.webserver;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import org.apache.log4j.Logger;

import rabbit.util.MimeTypeMapper;
import sk.fiit.rabbit.adaptiveproxy.plugins.PluginProperties;
import sk.fiit.rabbit.adaptiveproxy.plugins.headers.RequestHeaders;
import sk.fiit.rabbit.adaptiveproxy.plugins.headers.WritableResponseHeaders;
import sk.fiit.rabbit.adaptiveproxy.plugins.messages.HttpMessageFactory;
import sk.fiit.rabbit.adaptiveproxy.plugins.messages.HttpRequest;
import sk.fiit.rabbit.adaptiveproxy.plugins.messages.HttpResponse;
import sk.fiit.rabbit.adaptiveproxy.plugins.messages.ModifiableHttpRequest;
import sk.fiit.rabbit.adaptiveproxy.plugins.messages.ModifiableHttpResponse;
import sk.fiit.rabbit.adaptiveproxy.plugins.processing.RequestProcessingPlugin;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.ServiceUnavailableException;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.content.ModifiableBytesService;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.content.ModifiableStringService;

public class SimpleWebServer implements RequestProcessingPlugin {
	
	private static final Logger logger = Logger.getLogger(SimpleWebServer.class);
	
	private String rootDirectoryName;
	private File rootDirectory;
	
	@Override
	public HttpRequest getNewRequest(ModifiableHttpRequest proxyRequest,
			HttpMessageFactory messageFactory) {
		return null;
	}

	@Override
	public HttpResponse getResponse(ModifiableHttpRequest proxyRequest,
			HttpMessageFactory messageFactory) {
		
		String requestURI = proxyRequest.getClientRequestHeaders().getRequestURI();
		
		ModifiableHttpResponse r = messageFactory.constructHttpResponse(true);
		
		if("GET".equals(proxyRequest.getClientRequestHeaders().getMethod())) {
			File req = new File("." + requestURI);
			
			if(isSafe(req)) {
				loadResource(r, req);
			} else {
				logger.warn("Detected attempt to acces resource outside the root directory (" + requestURI + ")");
				buildResponse(r, "500 Internal Error");
			}
		} else {
			buildResponse(r, "405 Method Not Allowed");
		}
		
		return r;
	}
	
	private boolean isSafe(File req) {
		return req.getAbsolutePath().startsWith(rootDirectory.getAbsolutePath());
	}

	private void loadResource(ModifiableHttpResponse r, File target) {
		WritableResponseHeaders headers = r.getProxyResponseHeaders();
		
		try {
			ModifiableBytesService svc = r.getServiceHandle().getService(ModifiableBytesService.class);
			
			InputStream fr = new FileInputStream(target);

			if(target.length() > Integer.MAX_VALUE) {
				logger.warn("Maximum file size exceeded");
				buildResponse(r, "500 Internal Error");
				return;
			}
			
			byte[] buffer = new byte[(int) target.length()];
			
			int offset = 0;
			int bytesRead = 0;
			
			while(offset < target.length() && 
					(bytesRead = fr.read(buffer, offset, buffer.length - offset)) >= 0) {
				offset += bytesRead;
			}
			
			svc.setData(buffer);
			
			String contentType = MimeTypeMapper.getMimeType(target.getName()); 
			
			headers.setHeader("Expires", "-1");
			headers.setHeader("Content-Type", contentType);
			headers.setHeader("Content-Length", String.valueOf(target.length()));
		} catch(ServiceUnavailableException e) {
			logger.error("ServiceUnavailable: " + e);
			buildResponse(r, "500 Internal Error");
		} catch(FileNotFoundException e) {
			buildResponse(r, "404 Not Found");
		} catch (IOException e) {
			logger.error("Error reading resource " + target.getPath() + ": " + e);
			buildResponse(r, "500 Internal Error");
		}
	}

	private void buildResponse(ModifiableHttpResponse r, String status) {
		r.getProxyResponseHeaders().setStatusLine("HTTP/1.1 " + status);
		try {
			ModifiableStringService svc = r.getServiceHandle().getService(ModifiableStringService.class);
			svc.getModifiableContent().append(status);
			svc.setCharset(Charset.forName("UTF-8"));
		} catch (ServiceUnavailableException e) {
		}
	}

	@Override
	public RequestProcessingActions processRequest(ModifiableHttpRequest request) {
		String requestURI = request.getClientRequestHeaders().getRequestURI();
		
		if(requestURI.startsWith(rootDirectoryName)) {
			return RequestProcessingActions.FINAL_RESPONSE;
		} else {
			return RequestProcessingActions.PROCEED;
		}
	}

	@Override
	public boolean wantRequestContent(RequestHeaders clientRQHeaders) {
		return false;
	}

	@Override
	public boolean setup(PluginProperties props) {
		rootDirectoryName = props.getProperty("rootDirectory");
		rootDirectory = new File("." + rootDirectoryName);
		if(!rootDirectory.canRead()) {
			logger.warn("The root directory " + rootDirectory.getAbsolutePath() + " cannot be read");
			return false;
		}
		
		return true;
	}

	@Override
	public void start() {
	}

	@Override
	public void stop() {
	}

	@Override
	public boolean supportsReconfigure() {
		return false;
	}

}

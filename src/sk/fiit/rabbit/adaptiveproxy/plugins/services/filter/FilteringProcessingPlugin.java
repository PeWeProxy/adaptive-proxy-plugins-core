package sk.fiit.rabbit.adaptiveproxy.plugins.services.filter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;

import sk.fiit.peweproxy.headers.ReadableHeader;
import sk.fiit.peweproxy.headers.RequestHeader;
import sk.fiit.peweproxy.headers.ResponseHeader;
import sk.fiit.peweproxy.messages.HttpMessageFactory;
import sk.fiit.peweproxy.messages.HttpRequest;
import sk.fiit.peweproxy.messages.HttpResponse;
import sk.fiit.peweproxy.messages.ModifiableHttpRequest;
import sk.fiit.peweproxy.messages.ModifiableHttpResponse;
import sk.fiit.peweproxy.plugins.PluginProperties;
import sk.fiit.peweproxy.plugins.processing.RequestProcessingPlugin;
import sk.fiit.peweproxy.plugins.processing.ResponseProcessingPlugin;
import sk.fiit.peweproxy.services.ProxyService;

public class FilteringProcessingPlugin implements RequestProcessingPlugin, ResponseProcessingPlugin {
	
	private Set<String> startFilters = new HashSet<String>();
	private Set<String> endFilters = new HashSet<String>();
	private Set<String> matchFilters = new HashSet<String>();
	private Set<String> simpleFilters = new HashSet<String>();
	
	Logger logger = Logger.getLogger(FilteringProcessingPlugin.class);
	
	@Override
	public boolean start(PluginProperties props) {
		try {
			String line;
			
			File filter = new File(props.getRootDir().getAbsolutePath() + File.separator + "filter.txt");
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filter)));

			// toto nebude fungovat pre filtre tvaru |swf| ale take
			// aj tak nemaju zmysel
			
			while((line = reader.readLine()) != null) {
				if(line.charAt(0) == '|') {
					line = line.substring(1, line.length());
					startFilters.add(line);
					continue;
				}
				
				if(line.charAt(line.length() - 1) == '|') {
					line = line.substring(0, line.length() - 1);
					endFilters.add(line);
					continue;
				}
				
				if(line.contains("*"))  {
					matchFilters.add(line);
				} else {
					simpleFilters.add(line);
				}
			}
			
			logger.info(startFilters.size() + " START FILTERS");
			logger.info(endFilters.size() + " END FILTERS");
			logger.info(simpleFilters.size() + "  SIMPLE FILTERS");
			logger.info(matchFilters.size() + " MATCH FILTERS");
			
			return true;
		} catch (FileNotFoundException e) {
			logger.warn(e);
			return false;
		} catch (IOException e) {
			logger.warn(e);
			return false;
		}
	}
	
	@Override
	public boolean supportsReconfigure(PluginProperties newProps) {
		return true;
	}
	
	@Override
	public void stop() {
	}
	
	@Override
	public void desiredRequestServices(
			Set<Class<? extends ProxyService>> desiredServices,
			RequestHeader clientRQHeader) {
	}
	
	@Override
	public RequestProcessingActions processRequest(ModifiableHttpRequest request) {
		String url = request.getOriginalRequest().getRequestHeader().getRequestURI();
		if(canProceed(url, request.getOriginalRequest().getRequestHeader(), "REQUEST")) {
			return RequestProcessingActions.PROCEED;
		} else {
			return RequestProcessingActions.FINAL_REQUEST;
		}
	}
	
	@Override
	public HttpRequest getNewRequest(ModifiableHttpRequest request,
			HttpMessageFactory messageFactory) {
		return request;
	}
	
	@Override
	public HttpResponse getResponse(ModifiableHttpRequest request,
			HttpMessageFactory messageFactory) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public HttpResponse getNewResponse(ModifiableHttpResponse response,
			HttpMessageFactory messageFactory) {
		// TODO Auto-generated method stub
		return response;
	}
	
	@Override
	public void desiredResponseServices(
			Set<Class<? extends ProxyService>> desiredServices,
			ResponseHeader webRPHeader) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public ResponseProcessingActions processResponse(ModifiableHttpResponse response) {
		String url = response.getRequest().getOriginalRequest().getRequestHeader().getRequestURI();
		if(canProceed(url, response.getOriginalResponse().getRequest().getRequestHeader(), "RESPONSE")) {
			return ResponseProcessingActions.PROCEED;
		} else {
			return ResponseProcessingActions.FINAL_RESPONSE;
		}
	}
	
	private boolean canProceed(String url, ReadableHeader headers, String type) {
		String location = headers.getField("Location");
		
		if(location != null) {
			logger.debug("Blocked redirect from: " + url + " to: " + location);
			return false;
		}
			
		for (String filter : simpleFilters) {
			if(url.contains(filter)) {
				logger.debug("Blocked [" + type + "]: " + url);
				return false;
			}
		}
		
		for (String filter : endFilters) {
			if(url.endsWith(filter)) {
				logger.debug("Blocked [" + type + "]: " + url);
				return false;
			}
		}
		
		for (String filter : startFilters) {
			if(url.startsWith(filter)) {
				logger.debug("Blocked [" + type + "]: " + url);
				return false;
			}
		}
		
		for (String filter : matchFilters) {
			if(wildCardMatch(url, filter)) {
				logger.debug("Blocked [" + type + "]: " + url);
				return false;
			}
		}
		
		logger.debug("Passed [" + type + "]: " + url);
		return true;
	}
	
	private boolean wildCardMatch(String text, String pattern) {
		String[] cards = pattern.split("\\*");

		for (String card : cards) {
			int idx = text.indexOf(card);

			if (idx == -1) {
				return false;
			}

			text = text.substring(idx + card.length());
		}

		return true;
	}

	@Override
	public void processTransferedResponse(HttpResponse response) {
	}

	@Override
	public void processTransferedRequest(HttpRequest request) {
	}
}

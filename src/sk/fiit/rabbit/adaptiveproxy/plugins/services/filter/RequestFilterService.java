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

import sk.fiit.rabbit.adaptiveproxy.plugins.PluginProperties;
import sk.fiit.rabbit.adaptiveproxy.plugins.headers.RequestHeaders;
import sk.fiit.rabbit.adaptiveproxy.plugins.headers.ResponseHeaders;
import sk.fiit.rabbit.adaptiveproxy.plugins.helpers.RequestAndResponseProcessingPluginAdapter;
import sk.fiit.rabbit.adaptiveproxy.plugins.helpers.RequestProcessingPluginAdapter;
import sk.fiit.rabbit.adaptiveproxy.plugins.helpers.ResponseProcessingPluginAdapter;
import sk.fiit.rabbit.adaptiveproxy.plugins.messages.HttpMessageFactory;
import sk.fiit.rabbit.adaptiveproxy.plugins.messages.HttpRequest;
import sk.fiit.rabbit.adaptiveproxy.plugins.messages.HttpResponse;
import sk.fiit.rabbit.adaptiveproxy.plugins.messages.ModifiableHttpRequest;
import sk.fiit.rabbit.adaptiveproxy.plugins.messages.ModifiableHttpResponse;
import sk.fiit.rabbit.adaptiveproxy.plugins.processing.ResponseProcessingPlugin.ResponseProcessingActions;

public class RequestFilterService extends RequestAndResponseProcessingPluginAdapter {
	
	private Set<String> startFilters = new HashSet<String>();
	private Set<String> endFilters = new HashSet<String>();
	private Set<String> matchFilters = new HashSet<String>();
	private Set<String> simpleFilters = new HashSet<String>();
	
	Logger logger = Logger.getLogger(RequestFilterService.class);
	
	@Override
	public boolean setup(PluginProperties props) {
		try {
			String line;
			
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(props.getProperty("patternFile")))));

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
	public RequestProcessingActions processRequest(ModifiableHttpRequest request) {
		String url = request.getProxyRequestHeaders().getRequestURI();
		if(canProceed(url, "REQUEST")) {
			return RequestProcessingActions.PROCEED;
		} else {
			return RequestProcessingActions.FINAL_REQUEST;
		}
	}
	
	@Override
	public ResponseProcessingActions processResponse(ModifiableHttpResponse response) {
		String url = response.getProxyRequestHeaders().getRequestURI();
		if(canProceed(url, "RESPONSE")) {
			return ResponseProcessingActions.PROCEED;
		} else {
			return ResponseProcessingActions.FINAL_RESPONSE;
		}
	}
	
	private boolean canProceed(String url, String type) {
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
}

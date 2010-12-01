package sk.fiit.rabbit.adaptiveproxy.plugins.services.common;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import sk.fiit.peweproxy.headers.RequestHeader;
import sk.fiit.peweproxy.messages.HttpRequest;
import sk.fiit.peweproxy.messages.ModifiableHttpRequest;
import sk.fiit.peweproxy.plugins.PluginProperties;
import sk.fiit.peweproxy.plugins.services.RequestServiceModule;
import sk.fiit.peweproxy.plugins.services.RequestServiceProvider;
import sk.fiit.peweproxy.services.ProxyService;
import sk.fiit.peweproxy.services.ServiceUnavailableException;
import sk.fiit.peweproxy.services.content.StringContentService;
import sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions.PostDataParserService;

public class PostDataParserServiceModule implements RequestServiceModule {

    private static final Logger logger = Logger.getLogger(PostDataParserServiceModule.class);
    
    private class GettingPostDataProviderService implements PostDataParserService, 
    								RequestServiceProvider<PostDataParserService> {
	
	protected String content;
	
	public GettingPostDataProviderService(String content) {
	    this.content = content;
	}
	
	private Map<String, String> postData;
	@Override
	public Map<String, String> getPostData() {
	    if(postData != null) {
		return postData;
	    }
        	
	    postData = getPostDataFromRequest();
        	
	    return postData;
	
	}
	
    	 private Map<String, String> getPostDataFromRequest() {
    		try {
    			content = URLDecoder.decode(content, "utf-8");
    		} catch (UnsupportedEncodingException e) {
    			logger.warn(e);
    		}
    		Map<String, String> postData = new HashMap<String, String>();
    		String attributeName;
    		String attributeValue;
    
    		for (String postPair : content.split("&")) {
    			if (postPair.split("=").length == 2) {
    				attributeName = postPair.split("=")[0];
    				attributeValue = postPair.split("=")[1];
    				postData.put(attributeName, attributeValue);
    			}
    		}
    
    		return postData;
    	    }
	 
	@Override
	public String getServiceIdentification() {
		return this.getClass().getName();
	}

	@Override
	public PostDataParserService getService() {
		return this;
	}

	@Override
	public boolean initChangedModel() {
		return false;
	}

	
	@Override
	public void doChanges(ModifiableHttpRequest request) { 
	}

    }
    
    @Override
    public <Service extends ProxyService> RequestServiceProvider<Service> provideRequestService(
	    HttpRequest request, Class<Service> serviceClass)
	    throws ServiceUnavailableException {
	
	if(serviceClass.equals(PostDataParserService.class)
		&& request.getServicesHandle().isServiceAvailable(StringContentService.class)) {
	
	    String content = request.getServicesHandle().getService(StringContentService.class).getContent();
	    
	    return (RequestServiceProvider<Service>) new GettingPostDataProviderService(content);
	}
	
	return null;
    }
    
    @Override
    public void getProvidedRequestServices(
	    Set<Class<? extends ProxyService>> providedServices) {
	providedServices.add(PostDataParserService.class);
    }
    @Override
    public boolean start(PluginProperties props) {
	return true;
    }

    @Override
    public void stop() {
    }

    @Override
    public boolean supportsReconfigure(PluginProperties newProps) {
	return true;
    }

    @Override
    public void desiredRequestServices(
	    Set<Class<? extends ProxyService>> desiredServices,
	    RequestHeader clientRQHeader) {
	desiredServices.add(StringContentService.class);
    }
}

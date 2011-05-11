package sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions;

import java.util.Map;

import sk.fiit.peweproxy.services.ProxyService;

public interface UserPreferencesProviderService extends ProxyService {
    
	public String getProperty(String propertyName, String userUid, String pluginName);
	public void setProperty(String propertyName, String propertyValue, String userUid);
	public void setProperty(String propertyName, String propertyValue, String userUid, String propertyNamespace);
	
}

package sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions;

import sk.fiit.peweproxy.services.ProxyService;

public interface UserPreferencesProviderService extends ProxyService {
    
	public String getProperty(String preferenceName, String userUid, String propertyNamespace);
	public void setProperty(String propertyName, String propertyValue, String userUid);
	public void setProperty(String preferenceName, String propertyValue, String userUid, String preferenceNamespace);
	
}

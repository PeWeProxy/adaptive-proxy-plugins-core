package sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions;

import sk.fiit.peweproxy.services.ProxyService;

public interface InjectClientBubbleMenuItemService extends ProxyService  {
	
	public void injectScript(String buttonHtml);
	public void injectWindow(String buttonHtml);
	public void injectButton(String buttonHtml);
	
}

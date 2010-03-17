package sk.fiit.rabbit.adaptiveproxy.plugins.services.injector;

import sk.fiit.rabbit.adaptiveproxy.plugins.messages.HttpRequest;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.ProxyService;

public interface JavaScriptInjectorService extends ProxyService {
	public void registerJavascript(HttpRequest request, JavaScript js);
}

package sk.fiit.rabbit.adaptiveproxy.plugins.services.injector;

import sk.fiit.rabbit.adaptiveproxy.plugins.services.ProxyService;

public interface JavaScriptInjectorService extends ProxyService {
	public void registerJavascript(JavaScript js);
}

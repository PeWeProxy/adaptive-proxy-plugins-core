package sk.fiit.rabbit.adaptiveproxy.plugins.services.user;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import sk.fiit.rabbit.adaptiveproxy.plugins.PluginProperties;
import sk.fiit.rabbit.adaptiveproxy.plugins.messages.HttpMessageFactory;
import sk.fiit.rabbit.adaptiveproxy.plugins.messages.HttpResponse;
import sk.fiit.rabbit.adaptiveproxy.plugins.messages.ModifiableHttpRequest;
import sk.fiit.rabbit.adaptiveproxy.plugins.messages.ModifiableHttpResponse;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.ServiceUnavailableException;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.content.ModifiableStringService;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.injector.JavaScriptInjectingProcessingPlugin;

public class UIDFromCookiePlugin extends JavaScriptInjectingProcessingPlugin {
	
	private String cookieDomain; 
	
	@Override	
	public HttpResponse getResponse(ModifiableHttpRequest proxyRequest,
			HttpMessageFactory messageFactory) {
		ModifiableHttpResponse httpResponse = messageFactory.constructHttpResponse("text/html");
		URL url = null;
		try {
			url = new URL(proxyRequest.getClientRequestHeaders().getRequestURI());
		} catch (MalformedURLException e) {
			logger.warn("MalformedURLException: " + url, e);
		}
		//TODO: http? https?, port!
		System.err.println("CookieDomain: " + cookieDomain + ", url: " + url.getHost());
		if (cookieDomain.equals("http://"+url.getHost())) {
			return httpResponse;
		}
		try {
			ModifiableStringService stringService = httpResponse.getServiceHandle().getService(ModifiableStringService.class);
			String cookies = proxyRequest.getClientRequestHeaders().getHeader("Cookie");
			Pattern pattern = Pattern.compile("^.*uid=(.*?)(:?;|$)");
			Matcher matcher = pattern.matcher(cookies);
			String uid = "";
			String content;
			if (matcher.matches()) {
				uid = matcher.group(1);
				content = "var __peweproxy_uid = '" + uid + "'";
			} else {
				content = "location.href='" + cookieDomain + "?back=" + proxyRequest.getClientRequestHeaders().getRequestURI() + "';";
			}
			stringService.setContent(content);
		} catch (ServiceUnavailableException e) {
			//toto by sa nemalo stat, sluzbu poskytuje priamo proxy
			logger.warn("ModifiableStringService is unavailable, UIDFromCookie takes no action");
		}
		return httpResponse;
	}
	
	@Override
	public boolean setup(PluginProperties props) {
		super.setup(props);
		cookieDomain = props.getProperty("cookieDomain", "http://peweproxy.fiit.stuba.sk");
		return true;
	}
}

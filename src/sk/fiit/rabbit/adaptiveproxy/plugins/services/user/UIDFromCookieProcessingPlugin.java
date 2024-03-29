package sk.fiit.rabbit.adaptiveproxy.plugins.services.user;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import sk.fiit.peweproxy.messages.HttpMessageFactory;
import sk.fiit.peweproxy.messages.HttpResponse;
import sk.fiit.peweproxy.messages.ModifiableHttpRequest;
import sk.fiit.peweproxy.messages.ModifiableHttpResponse;
import sk.fiit.peweproxy.plugins.PluginProperties;
import sk.fiit.peweproxy.services.content.ModifiableStringService;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.injector.JavaScriptInjectingProcessingPlugin;

public class UIDFromCookieProcessingPlugin extends JavaScriptInjectingProcessingPlugin {
	
	private String cookieDomain; 
	
	private String redirectContent(){
		// funkcia getHostName je z 'http://beardscratchers.com/journal/using-javascript-to-get-the-hostname-of-a-url'
		return 	"function getHostName(str) {\n" +
				"	var re = new RegExp('^(?:f|ht)tp(?:s)?\\://([^/]+)', 'im');\n"+
				"	return str.match(re)[1].toString();\n"+
				"}\n"+
				"__peweproxy_uid = null;\n"+
				"if (window.location.host != getHostName('" + cookieDomain + "/')) {\n" +
				"	window.location='" + cookieDomain + "/en/set_cookie/cookie?back='+window.location;\n}\n";
	}
	
	@Override	
	public HttpResponse getResponse(ModifiableHttpRequest proxyRequest,
			HttpMessageFactory messageFactory) {
		ModifiableHttpResponse httpResponse = messageFactory.constructHttpResponse(null, "text/html");
		
		if(!httpResponse.getServicesHandle().isServiceAvailable(ModifiableStringService.class)) return httpResponse;
		
		ModifiableStringService stringService = httpResponse.getServicesHandle().getService(ModifiableStringService.class);
		String cookies = proxyRequest.getRequestHeader().getField("Cookie");
		String content = ""; 
		if (cookies != null) {
			Pattern pattern = Pattern.compile("^.*__peweproxy_uid=(.*?)(:?;|$)");
			Matcher matcher = pattern.matcher(cookies);
			String uid = "";
			if (matcher.matches()) {
				uid = matcher.group(1).split(";")[0];
				content = 	"var exdate=new Date();\n"+
							"exdate.setDate(exdate.getDate()+1000);\n"+ //1000 dni bude cookie validna
							"document.cookie='__peweproxy_uid=" + uid + ";expires='+exdate.toUTCString();\n" +
							"peweproxy.set_current_uid('" + uid + "');";
				if ("".equals(uid)) {
					content = redirectContent();
				}
			} else {
				content = redirectContent();
			}
		} else {
			content = redirectContent();
		}
		stringService.setContent(content);
		return httpResponse;
	}

	@Override
	public boolean start(PluginProperties props) {
		super.start(props);
		cookieDomain = props.getProperty("cookieDomain", "http://peweproxy.fiit.stuba.sk");
		return true;
	}
}

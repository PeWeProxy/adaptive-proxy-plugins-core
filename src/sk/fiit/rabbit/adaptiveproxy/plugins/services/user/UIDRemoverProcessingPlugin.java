package sk.fiit.rabbit.adaptiveproxy.plugins.services.user;

import java.util.Set;

import sk.fiit.peweproxy.headers.RequestHeader;
import sk.fiit.peweproxy.messages.HttpMessageFactory;
import sk.fiit.peweproxy.messages.HttpRequest;
import sk.fiit.peweproxy.messages.HttpResponse;
import sk.fiit.peweproxy.messages.ModifiableHttpRequest;
import sk.fiit.peweproxy.plugins.PluginProperties;
import sk.fiit.peweproxy.plugins.processing.RequestProcessingPlugin;
import sk.fiit.peweproxy.services.ProxyService;

public class UIDRemoverProcessingPlugin implements RequestProcessingPlugin {

	private String pattern = null;
	private String header = null;
	private String exception = null;
	
	public RequestProcessingActions processRequest(ModifiableHttpRequest request)
	{
		if ((exception != null) && (!exception.equals("")) && (!(request.getRequestHeader().getRequestURI().contains(exception))))
		{
			{
				String cookie = request.getRequestHeader().getField(header);
				if (cookie != null) {
				    cookie = removeUID(cookie);
				    request.getRequestHeader().setField(header, cookie);
				}
			}
		}
		
		return RequestProcessingActions.PROCEED;
	}
	
	public HttpRequest getNewRequest(ModifiableHttpRequest request, HttpMessageFactory messageFactory)
	{
		return request;
	}
	
	private String removeUID (String text)
	{
		String[] data = text.split(";");
		String result = "";
		
		for (int i = 0; i < data.length; i++)
		{
			String element = data[i];
			result += element.indexOf(pattern) == -1 ? (element + ";") : "";
		}
		
		result = result.substring(0, result.length() - 1);
		return result;
	}
	
	public HttpResponse getResponse(ModifiableHttpRequest request, HttpMessageFactory messageFactory)
	{
		return null;
	}
	
	public void processTransferedRequest(HttpRequest request)
	{
		
	}
	
	public void desiredRequestServices(Set<Class<? extends ProxyService>> desiredServices,
			RequestHeader webRQHeader)
	{
	}
	
	public boolean start(PluginProperties props)
	{
		pattern = props.getProperty("pattern");
		header = props.getProperty("header");
		exception = props.getProperty("exception");
		return true;
	}
	
	public void stop()
	{
		
	}
	
	public boolean supportsReconfigure(PluginProperties newProps)
	{
		return false;
	}
}

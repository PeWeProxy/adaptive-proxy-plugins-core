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
	private String exception = null;

	@Override
	public RequestProcessingActions processRequest(ModifiableHttpRequest request) {
		if ((exception != null) && (!exception.equals(""))
				&& (!(request.getRequestHeader().getRequestURI().contains(exception)))) {
			{
				String cookie = request.getRequestHeader().getField("Cookie");
				if (cookie != null) {
					cookie = removeUID(cookie);
					request.getRequestHeader().setField("Cookie", cookie);
				}
			}
		}

		return RequestProcessingActions.PROCEED;
	}

	@Override
	public HttpRequest getNewRequest(ModifiableHttpRequest request, HttpMessageFactory messageFactory) {
		return request;
	}

	private String removeUID(String text) {
		String[] data = text.split(";");
		String result = "";

		for (int i = 0; i < data.length; i++) {
			String element = data[i];
			result += element.indexOf(pattern) == -1 ? (element + ";") : "";
		}

		result = result.substring(0, result.length() - 1);
		return result;
	}

	@Override
	public HttpResponse getResponse(ModifiableHttpRequest request, HttpMessageFactory messageFactory) {
		return null;
	}

	@Override
	public void processTransferedRequest(HttpRequest request) {

	}

	@Override
	public void desiredRequestServices(Set<Class<? extends ProxyService>> desiredServices, RequestHeader webRQHeader) {
	}

	@Override
	public boolean start(PluginProperties props) {
		pattern = props.getProperty("pattern");
		exception = props.getProperty("exception");
		return true;
	}

	@Override
	public void stop() {
	}

	@Override
	public boolean supportsReconfigure(PluginProperties newProps) {
		return true;
	}
}

package sk.fiit.rabbit.adaptiveproxy.plugins.common;

import java.io.IOException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.PostMethod;

public class MetallClient {
	private static final String METALL_BASE_URL = "http://localhost:9292";
	
	public class MetallClientException extends RuntimeException {
		private static final long serialVersionUID = 1L;
		
		public MetallClientException(String message) {
			super(message);
		}
		public MetallClientException(Exception e) {
			super(e);
		}
	}

	public String cleartext(String content) throws MetallClientException {
		HttpClient client = new HttpClient();
		PostMethod post = new PostMethod(METALL_BASE_URL + "/readability");
		post.setRequestHeader("Content-Type", PostMethod.FORM_URL_ENCODED_CONTENT_TYPE + "; charset=utf-8" );
		post.setRequestHeader("Accept-Charset", "utf-8");
		NameValuePair[] data = { new NameValuePair("content", content) };
		post.setRequestBody(data);
		
		try {
			int resp = client.executeMethod(post);
			if(resp == 200) {
				return post.getResponseBodyAsString();
			} else {
				throw new MetallClientException(post.getResponseBodyAsString());
			}
		} catch (IOException e) {
			throw new MetallClientException(e);
		}
		
	}
	
	public String keywords(String content) throws MetallClientException {
		HttpClient client = new HttpClient();
		PostMethod post = new PostMethod(METALL_BASE_URL + "/meta");
		post.setRequestHeader("Content-Type", PostMethod.FORM_URL_ENCODED_CONTENT_TYPE + "; charset=utf-8" );
		post.setRequestHeader("Accept-Charset", "utf-8");
		NameValuePair[] data = { new NameValuePair("content", content) };
		post.setRequestBody(data);
		
		try {
			int resp = client.executeMethod(post);
			if(resp == 200) {
				return post.getResponseBodyAsString();
			} else {
				throw new MetallClientException(post.getResponseBodyAsString());
			}
		} catch (IOException e) {
			throw new MetallClientException(e);
		}
		
	}
	
}

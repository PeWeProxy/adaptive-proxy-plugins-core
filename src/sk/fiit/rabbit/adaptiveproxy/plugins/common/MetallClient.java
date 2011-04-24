package sk.fiit.rabbit.adaptiveproxy.plugins.common;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

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
	
	public String post(String where, String content) throws MetallClientException {
		try {
			HttpClient client = new DefaultHttpClient();
			HttpPost post = new HttpPost(METALL_BASE_URL + "/" + where);
			post.setHeader("Accept-Charset", "utf-8");
			post.setHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
			List<NameValuePair> data = new ArrayList<NameValuePair>();
			data.add(new BasicNameValuePair("content", content));
			UrlEncodedFormEntity entity = new UrlEncodedFormEntity(data, HTTP.UTF_8);
			post.setEntity(entity);

			HttpResponse response = client.execute(post);
			String contentResponse = EntityUtils.toString(response.getEntity());
			
			if(response.getStatusLine().getStatusCode() == 200) {
				return contentResponse;
			} else {
				throw new MetallClientException(contentResponse);
			}
		} catch (UnsupportedEncodingException e) {
			throw new MetallClientException(e);
		} catch (ClientProtocolException e) {
			throw new MetallClientException(e);
		} catch (IOException e) {
			throw new MetallClientException(e);
		}
	}

	public String cleartext(String content) throws MetallClientException {
		return post("readability", content);
	}
	
	public String keywords(String content) throws MetallClientException {
		return post("meta", content);
	}
	
}

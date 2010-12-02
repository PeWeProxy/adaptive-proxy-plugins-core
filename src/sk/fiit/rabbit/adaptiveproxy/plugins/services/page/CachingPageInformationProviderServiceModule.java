package sk.fiit.rabbit.adaptiveproxy.plugins.services.page;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.Buffer;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Set;

import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import rabbit.util.CharsetUtils;
import sk.fiit.peweproxy.headers.ReadableHeader;
import sk.fiit.peweproxy.headers.ResponseHeader;
import sk.fiit.peweproxy.messages.HttpResponse;
import sk.fiit.peweproxy.messages.ModifiableHttpResponse;
import sk.fiit.peweproxy.plugins.PluginProperties;
import sk.fiit.peweproxy.plugins.services.ResponseServiceModule;
import sk.fiit.peweproxy.plugins.services.ResponseServiceProvider;
import sk.fiit.peweproxy.services.ProxyService;
import sk.fiit.peweproxy.services.ServiceUnavailableException;
import sk.fiit.peweproxy.services.content.StringContentService;
import sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions.ClearTextExtractionService;
import sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions.DatabaseConnectionProviderService;
import sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions.PageInformationProviderService;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.common.Checksum;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.common.SqlUtils;

public class CachingPageInformationProviderServiceModule implements ResponseServiceModule {
	
	private static final Logger logger = Logger.getLogger(CachingPageInformationProviderServiceModule.class);
	
	private class CachingPageInformationProviderServiceProvider 
		implements PageInformationProviderService,
		ResponseServiceProvider<PageInformationProviderService> {
		
		private static final String serviceMethod = "POST";
		private static final String metaServiceLocation = "http://peweproxy-staging.fiit.stuba.sk/metall/meta/";
		
		DatabaseConnectionProviderService connectionService;
		String clearText;
		String requestURI;
		String charset;
		String content;
		
		Connection connection;
		
		public CachingPageInformationProviderServiceProvider(String requestURI,
				DatabaseConnectionProviderService connectionService, String clearText,
				String charset, String content) {
			this.connectionService = connectionService;
			this.clearText = clearText;
			this.requestURI = requestURI;
			this.charset = charset;
			this.content = content;
		}

		PageInformation pi;
		
		@Override
		public PageInformation getPageInformation() {
			
			if(pi != null) {
				return pi;
			}
			
			pi = new PageInformation();
			extractPageInformation(pi);
			
			return pi;
		}
		
		private void extractPageInformation(PageInformation pi) {
			try {
				connection = connectionService.getDatabaseConnection();
				
				pi.url = requestURI;
				pi.checksum = clearText != null ? Checksum.md5(clearText) : null;
				loadPageInformationFromCache(pi);
				System.out.println("\n\n\n\n\n\n00");
				
				if(pi.id == null && clearText != null) {
					pi.contentLength = clearText.length();
					try {
						pi.keywords = extractKeywords(content, charset);
					} catch (IOException e) {
						// TODO: some error with response 500, when sending img url
						logger.debug("Metall meta keywords extraction client FAILED:"+e.getMessage());
					}
					
					savePageInformation(pi);
				}
			} finally {
				SqlUtils.close(connection);
				connection = null;
			}
		};
		
		private void loadPageInformationFromCache(PageInformation pi) {
			
			if(connection == null) {
				return;
			}
			
			PreparedStatement stmt = null;
			ResultSet rs = null;
			
			try {
				String query = "SELECT id, content_length, keywords FROM pages WHERE url = ?";
				
				if(pi.checksum != null) {
					query += " AND checksum = ?";
				}
				
				stmt = connection.prepareStatement(query);
				stmt.setString(1, requestURI);
				
				if(pi.checksum != null) {
					stmt.setString(2, pi.checksum);
				}
		
				rs = stmt.executeQuery();
				
				if(rs.next()) {
					pi.id = rs.getLong(1);
					pi.contentLength = rs.getInt(2);
					pi.keywords = rs.getString(3);
				}
			} catch(SQLException e) {
				logger.error("Could not load pageId from cache", e);
			} finally {
				SqlUtils.close(rs);
				SqlUtils.close(stmt);
			}
		}

		private String extractKeywords(String content, String chrset) throws MalformedURLException, IOException {
			String jsonString = null;
			String keywords = "";

			// initialize connection and set headers 
			URL serviceCallURL = new URL(metaServiceLocation);
		    HttpURLConnection connection = (HttpURLConnection)serviceCallURL.openConnection();
		    connection.setRequestMethod(serviceMethod);
		    connection.setDoInput(true);
		    connection.setDoOutput(true);
		    connection.setAllowUserInteraction(false);
		    connection.setRequestProperty("Accept", "text/html, application/xml;q=0.9, */*;q=0.1");
		    connection.setRequestProperty("Accept-Language", "sk-SK,sk;q=0.9,en;q=0.8");
		    
		    if(charset != null) {
		    	connection.setRequestProperty("Accept-Charset", charset+";q=1");
		    } else {
		    	connection.setRequestProperty("Accept-Charset", "windows-1250, utf-8, iso-8859-2, iso-8859-1;q=0.2, utf-16;q=0.1, *;q=0.1");
		    	charset="utf-8";
		    }
		    
	        // establish connection
		    connection.connect();
		    
		    // prepare post data
		    content = "content="+content;
		    String data = URLEncoder.encode(content);
		    InputStream byteInputStream = new ByteArrayInputStream(data.getBytes());
		    
	        // open output stream & write request data to body
		    OutputStream os = connection.getOutputStream();
	        byte buffer[] = new byte[2048];
	        int read = 0;
	        if (byteInputStream != null) {
	            while ((read = byteInputStream.read(buffer)) != -1) {
	                os.write(buffer, 0, read);
	            }
	            os.flush();
	            os.close();
	        }

		    
		    // read response
		    InputStream is = connection.getInputStream();
		    ByteArrayOutputStream responseOut = new ByteArrayOutputStream();
		    byte[] response = new byte[2048];
		    while (is.read(response) != -1) {
		    	responseOut.write(response);
		    }
		    is.close();
		    

		    // close connection
		    connection.disconnect();
		    
		    // read response data to string
		    if(responseOut != null) {
		    	Buffer charBuffer = CharsetUtils.decodeBytes(responseOut.toByteArray(), Charset.forName(charset), false);
		    	jsonString = charBuffer.toString();
		    	responseOut.close();
		    }


		    // trim some extra curious characters
		    if(jsonString.lastIndexOf("]") != -1) {
		    	jsonString = jsonString.substring(0, jsonString.lastIndexOf("]")+1).trim();
		    }
		    jsonString = jsonString.trim();

			try {
				if(jsonString != null && jsonString.equals("")) {
					JSONParser parser = new JSONParser();
					JSONArray jsonArray = (JSONArray)parser.parse(jsonString);
					for (Object jsonObject : jsonArray) {
						if(((JSONObject)jsonObject).containsKey("name")) {
							keywords += ((JSONObject)jsonObject).get("name")+",";
						}
					}
				}
			} catch (ParseException e) {
				logger.error("JSON parser exception:"+e.getMessage());
			}
		    
		    return keywords;
		}
		
		private void savePageInformation(PageInformation pi) {
			
			if(connection == null) {
				return;
			}
			
			String query = "INSERT INTO pages(url, checksum, content_length, keywords) VALUES(?, ?, ?, ?)";
			
			PreparedStatement stmt = null;
			ResultSet keys = null;
			
			try {
				stmt = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
				stmt.setString(1, pi.getUrl());
				stmt.setString(2, pi.getChecksum());
				stmt.setInt(3, pi.getContentLength());
				stmt.setString(4, pi.getKeywords());
				
				stmt.execute();
				
				keys = stmt.getGeneratedKeys();
				
				if(keys.next()) {
					pi.id = keys.getLong(1);
				}
			} catch (SQLException e) {
				logger.error("Could not save page information", e);
			} finally {
				SqlUtils.close(keys);
				SqlUtils.close(stmt);
			}
		}

		@Override
		public String getServiceIdentification() {
			return this.getClass().getName();
		}

		@Override
		public PageInformationProviderService getService() {
			return this;
		}

		@Override
		public boolean initChangedModel() {
			return false;
		}

		@Override
		public void doChanges(ModifiableHttpResponse response) {
			// this service makes no modifications
		}
		
	}

	@Override
	public boolean supportsReconfigure(PluginProperties newProps) {
		return true;
	}

	@Override
	public boolean start(PluginProperties props) {
		return true;
	}

	@Override
	public void stop() {
	}

	@Override
	public void desiredResponseServices(
			Set<Class<? extends ProxyService>> desiredServices,
			ResponseHeader webRPHeader) {
		desiredServices.add(ClearTextExtractionService.class);
		desiredServices.add(DatabaseConnectionProviderService.class);
	}

	@Override
	public void getProvidedResponseServices(
			Set<Class<? extends ProxyService>> providedServices) {
		providedServices.add(PageInformationProviderService.class);
	}

	@Override
	public <Service extends ProxyService> ResponseServiceProvider<Service> provideResponseService(
			HttpResponse response, Class<Service> serviceClass)
			throws ServiceUnavailableException {
		
		if (serviceClass.equals(PageInformationProviderService.class)
				&& response.getServicesHandle().isServiceAvailable(StringContentService.class)
				&& response.getServicesHandle().isServiceAvailable(ClearTextExtractionService.class)) {
			DatabaseConnectionProviderService connectionService = response.getServicesHandle().getService(DatabaseConnectionProviderService.class);
			String requestURI = response.getRequest().getOriginalRequest().getRequestHeader().getRequestURI();
			String content = response.getServicesHandle().getService(StringContentService.class).getContent();
			String clearText = response.getServicesHandle().getService(ClearTextExtractionService.class).getCleartext();
			
			String charset = null;
			try {
				charset = CharsetUtils.detectCharset((ReadableHeader)response.getResponseHeader(), content.getBytes(), false).toString();
			} catch (UnsupportedCharsetException e) {
				logger.debug("Unable to detect character set:"+e.getMessage());
			} catch (IOException e) {
				logger.error("Wrong input. This should not happens:"+e.getMessage());
			}
			
			return (ResponseServiceProvider<Service>) new CachingPageInformationProviderServiceProvider(requestURI, connectionService, clearText, charset, content);
		}
		
		return null;
	}
}

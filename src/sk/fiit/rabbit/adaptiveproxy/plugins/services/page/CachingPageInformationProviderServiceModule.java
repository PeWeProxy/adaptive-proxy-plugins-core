package sk.fiit.rabbit.adaptiveproxy.plugins.services.page;

import java.net.MalformedURLException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Set;

import org.apache.log4j.Logger;

import sk.fiit.keyextractor.JKeyExtractor;
import sk.fiit.keyextractor.extractors.OpenCalaisKeyExtractor;
import sk.fiit.keyextractor.extractors.TagTheNetKeyExtractor;
import sk.fiit.peweproxy.headers.ResponseHeader;
import sk.fiit.peweproxy.messages.HttpResponse;
import sk.fiit.peweproxy.messages.ModifiableHttpResponse;
import sk.fiit.peweproxy.plugins.PluginProperties;
import sk.fiit.peweproxy.plugins.services.ResponseServiceModule;
import sk.fiit.peweproxy.plugins.services.ResponseServiceProvider;
import sk.fiit.peweproxy.services.ProxyService;
import sk.fiit.peweproxy.services.ServiceUnavailableException;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.cleartext.ClearTextExtractionService;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.common.Checksum;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.common.SqlUtils;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.database.DatabaseConnectionProviderService;

public class CachingPageInformationProviderServiceModule implements ResponseServiceModule {
	
	private static final Logger logger = Logger.getLogger(CachingPageInformationProviderServiceModule.class);
	
	private class CachingPageInformationProviderServiceProvider 
		implements PageInformationProviderService,
		ResponseServiceProvider<PageInformationProviderService> {
		
		DatabaseConnectionProviderService connectionService;
		String clearText;
		String requestURI;
		
		Connection connection;
		
		public CachingPageInformationProviderServiceProvider(String requestURI,
				DatabaseConnectionProviderService connectionService, String clearText) {
			this.connectionService = connectionService;
			this.clearText = clearText;
			this.requestURI = requestURI;
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
				
				if(pi.id == null && clearText != null) {
					pi.contentLength = clearText.length();
					pi.keywords = extractKeywords(requestURI, clearText);
					
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

		
		private String extractKeywords(String url, String clearText) {
			
			if(clearText == null || clearText.trim() == "") {
				return "";
			}
			
			JKeyExtractor jKeyExtractor = new JKeyExtractor();
			
			jKeyExtractor.addAlgorithm(new TagTheNetKeyExtractor());
			jKeyExtractor.addAlgorithm(new OpenCalaisKeyExtractor());
			
			Set<String> l = null;
			try {
				l = jKeyExtractor.getAllKeys(url, clearText);
			} catch (MalformedURLException e) {
				return "";
			}
			
			String kws = "";
			
			for (String kw : l) {
				kws += kw + ',';
			}
			
			if(kws.length() > 0) {
				return kws.substring(0, kws.length() - 1);
			} else {
				return "";
			}
			
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
		
		if(serviceClass.equals(PageInformationProviderService.class)) {
			String requestURI = response.getRequest().getClientRequestHeader().getRequestURI();
			DatabaseConnectionProviderService connectionService = response.getServicesHandle().getService(DatabaseConnectionProviderService.class);
			String clearText = response.getServicesHandle().getService(ClearTextExtractionService.class).getCleartext();
			
			return (ResponseServiceProvider<Service>) new CachingPageInformationProviderServiceProvider(requestURI, connectionService, clearText);
		}
		
		return null;
	}
}

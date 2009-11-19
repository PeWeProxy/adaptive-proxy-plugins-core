package sk.fiit.rabbit.adaptiveproxy.plugins.services.page;

import java.lang.Thread.UncaughtExceptionHandler;
import java.net.MalformedURLException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import sk.fiit.keyextractor.JKeyExtractor;
import sk.fiit.keyextractor.extractors.TagTheNetKeyExtractor;
import sk.fiit.rabbit.adaptiveproxy.plugins.helpers.ResponseServicePluginAdapter;
import sk.fiit.rabbit.adaptiveproxy.plugins.helpers.ResponseServiceProviderAdapter;
import sk.fiit.rabbit.adaptiveproxy.plugins.messages.HttpResponse;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.ProxyService;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.ResponseServiceProvider;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.ServiceUnavailableException;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.ServicesHandle;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.cleartext.ClearTextExtractionService;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.common.Checksum;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.common.SqlUtils;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.database.DatabaseConnectionProviderService;

public class CachingPageInformationProviderService extends ResponseServicePluginAdapter {
	
	private static final Logger logger = Logger.getLogger(CachingPageInformationProviderService.class);
	
	private class CachingPageInformationProviderServiceProvider extends ResponseServiceProviderAdapter 
		implements PageInformationProviderService {
		
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

		@Override
		public Class<? extends ProxyService> getServiceClass() {
			return PageInformationProviderService.class;
		}

		volatile PageInformation pi;
		volatile boolean extracted = false;
		
		@Override
		public PageInformation getPageInformation() {
			
			if(extracted) {
				return pi;
			}
			
			pi = new PageInformation();
			
			final Object lock = new Object();
			
			try {
				Thread t = new Thread() {
					@Override
					public void run() {
						try {
							extractPageInformation(pi);
							
							synchronized (lock) {
								extracted = true;
							}
						} finally {
							synchronized (lock) {
								lock.notifyAll();
							}
						}
					}
				};
				
				t.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
					@Override
					public void uncaughtException(Thread t, Throwable e) {
						logger.warn("Uncaught exception, releasing the lock, closing connection");
						
						synchronized (lock) {
							lock.notifyAll();
						}
					}
				});
				
				t.start();
				
				synchronized (lock) {
					if(!extracted) {
						lock.wait();
					} else {
					}
				}
			} catch (InterruptedException e) {
			}
			
			return pi;
		}
		
		private void extractPageInformation(PageInformation pi) {
			connection = connectionService.getDatabaseConnection();
			
			try {
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
			
			JKeyExtractor ke = new JKeyExtractor();
			ke.addAlgorithm(new TagTheNetKeyExtractor());
			
			Set<String> l = null;
			try {
				l = ke.getAllKeys(url, clearText);
			} catch (MalformedURLException e) {
				return "";
			}
			
			String kws = "";
			
			for (String kw : l) {
				kws += kw + ',';
			}
			
			return kws.substring(0, kws.length() - 1);
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
		
	}
	
	@Override
	protected void addDependencies(Set<Class<? extends ProxyService>> dependencies) {
		dependencies.add(ClearTextExtractionService.class);
		dependencies.add(DatabaseConnectionProviderService.class);
	}
	
	@Override
	protected void addProvidedServices(Set<Class<? extends ProxyService>> providedServices) {
		providedServices.add(PageInformationProviderService.class);
	}
	
	@Override
	protected void addProvidedResponseServices(List<ResponseServiceProvider> providedServices, HttpResponse response) {
		ServicesHandle handle = response.getServiceHandle();
		DatabaseConnectionProviderService connectionService = null;
		String clearText = null;

		try {
			clearText = handle.getService(ClearTextExtractionService.class).getCleartext();
		} catch (ServiceUnavailableException e) {
			logger.debug("ClearText service is unavailable, no keywords will be extracted");
		}
		
		try {
			connectionService = handle.getService(DatabaseConnectionProviderService.class);
		} catch (ServiceUnavailableException e) {
			logger.debug("Database service is unavailable, page information cannot be loaded from cache");
		}
		
		String requestURI = response.getClientRequestHeaders().getRequestURI();
		providedServices.add(new CachingPageInformationProviderServiceProvider(requestURI, connectionService, clearText));
	}
}

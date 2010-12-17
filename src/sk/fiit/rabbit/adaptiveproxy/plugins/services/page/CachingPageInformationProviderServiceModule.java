package sk.fiit.rabbit.adaptiveproxy.plugins.services.page;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import sk.fiit.peweproxy.headers.ResponseHeader;
import sk.fiit.peweproxy.messages.HttpResponse;
import sk.fiit.peweproxy.messages.ModifiableHttpResponse;
import sk.fiit.peweproxy.plugins.PluginProperties;
import sk.fiit.peweproxy.plugins.services.ResponseServiceModule;
import sk.fiit.peweproxy.plugins.services.ResponseServiceProvider;
import sk.fiit.peweproxy.services.ProxyService;
import sk.fiit.peweproxy.services.ServiceUnavailableException;
import sk.fiit.peweproxy.services.content.StringContentService;
import sk.fiit.rabbit.adaptiveproxy.plugins.beans.PageInformation;
import sk.fiit.rabbit.adaptiveproxy.plugins.beans.PagesTerms;
import sk.fiit.rabbit.adaptiveproxy.plugins.beans.Term;
import sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions.ClearTextExtractionService;
import sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions.DatabaseConnectionProviderService;
import sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions.PageInformationProviderService;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.common.Checksum;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.common.MetallClient;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.common.MetallClient.MetallClientException;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.common.SqlUtils;

public class CachingPageInformationProviderServiceModule implements ResponseServiceModule {
	
	private static final Logger logger = Logger.getLogger(CachingPageInformationProviderServiceModule.class);
	
	private class CachingPageInformationProviderServiceProvider 
		implements PageInformationProviderService,
		ResponseServiceProvider<PageInformationProviderService> {
		
		DatabaseConnectionProviderService connectionService;
		String clearText;
		String requestURI;
		String content;
		
		Connection connection;
		
		public CachingPageInformationProviderServiceProvider(
				DatabaseConnectionProviderService connectionService,
				String content, String requestURI, String cleartext) {
			this.connectionService = connectionService;
			this.clearText = cleartext;
			this.requestURI = requestURI;
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
			JSONArray jsonArray = null;
			try {
				connection = connectionService.getDatabaseConnection();
				pi.url = requestURI;
				pi.checksum = clearText != null ? Checksum.md5(clearText) : null;
				loadPageInformationFromCache(pi);
				if(pi.id == null && clearText != null) {
					pi.contentLength = clearText.length();
					try {
						jsonArray = (JSONArray) new JSONParser().parse(new MetallClient().keywords(content));
					} catch (MetallClientException e) {
						logger.warn(e);
					} catch (ParseException e) {
						logger.warn(e);
					}
					
					if(jsonArray == null) {
						return;
					}
					
					pi.setPageTermsList(Json2PagesTerms(jsonArray));
					save(pi);
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

				if(pi.getId() != null) {
					loadPagesTermsFromCache(pi);
				}
				
			} catch(SQLException e) {
				logger.error("Could not load pageId from cache", e);
			} finally {
				SqlUtils.close(rs);
				SqlUtils.close(stmt);
			}
		}
		
		private void loadPagesTermsFromCache(PageInformation pi) {

			if(connection == null) {
				return;
			}
			
			PreparedStatement stmt = null;
			ResultSet rs = null;
			
			List<PagesTerms> list = new ArrayList<PagesTerms>();
			PagesTerms pt = null;
			Term term = null;
			
			try {
				String query = "SELECT" +
						" terms.id as termId," +
						" terms.label as label," +
						" terms.term_type as term_type," +
						" pages_terms.id as id," +
						" pages_terms.weight as weight," +
						" pages_terms.created_at as created_at," +
						" pages_terms.updated_at as updated_at," +
						" pages_terms.source as source" +
						" FROM pages_terms LEFT JOIN terms ON pages_terms.term_id=terms.id" +
						" WHERE pages_terms.page_id = ? ";
				
				stmt = connection.prepareStatement(query);
				stmt.setLong(1, pi.getId());
				
				rs = stmt.executeQuery();
				
				while(rs.next()) {
					term = new Term();
					term.setId(rs.getLong("termId"));
					term.setLabel(rs.getString("label"));
					term.setTermType(rs.getString("term_type"));
					
					pt = new PagesTerms();
					pt.setPi(pi);
					pt.setTerm(term);
					pt.setId(rs.getLong("id"));
					pt.setWeight(rs.getFloat("weight"));
					pt.setCreatedAt(new Timestamp(rs.getDate("created_at").getTime()));
					pt.setUpdatedAt(new Timestamp(rs.getDate("updated_at").getTime()));
					pt.setSource(rs.getString("source"));
					
					list.add(pt);
				}
				
				pi.setPageTermsList(list);
			} catch(SQLException e) {
				logger.error("Could not load pageId from cache", e);
			} finally {
				SqlUtils.close(rs);
				SqlUtils.close(stmt);
			}
		}
		
		private List<PagesTerms> Json2PagesTerms(JSONArray jsonArray) {

			if(jsonArray == null) {
				return(null);
			}
			
			Iterator<JSONObject> i = jsonArray.iterator();
			List<PagesTerms> ptList = new ArrayList<PagesTerms>();
			PagesTerms pt = null;
			Term term = null;
			
			while(i.hasNext()) {
				JSONObject jsonObject = (JSONObject)i.next();
				
				pt = new PagesTerms();
				term = new Term();
				
				pt.setCreatedAt(new Timestamp(System.currentTimeMillis()));
				pt.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
				
				if(jsonObject.containsKey("name") && !jsonObject.get("name").equals(null)) {
					term.setLabel((String)jsonObject.get("name"));
				}
				if(jsonObject.containsKey("type") && !jsonObject.get("type").equals(null)) {
					term.setTermType((String)jsonObject.get("type"));
				}
				if(jsonObject.containsKey("relevance") && !jsonObject.get("relevance").equals("")) {
					try {
						pt.setWeight(new Float(jsonObject.get("relevance").toString()));
					} catch (NumberFormatException e){
						logger.warn("Metall meta keywords extraction relevance weight is not a float:"+e.getMessage());
					}
				}
				if(jsonObject.containsKey("source") && !jsonObject.get("source").equals(null)) {
					pt.setSource((String)jsonObject.get("source"));
				}
				pt.setTerm(term);
				ptList.add(pt);
			}
			return(ptList);
		}
			
		private Long savePageInformation(PageInformation pi) {
			
			if(connection == null) {
				return null;
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
			return (pi.id);
		}
		
		private void saveTerm(Term term) {

			if(connection == null) {
				return;
			}
			
			if(term == null) {
				return;
			}
			
			String query = "INSERT INTO terms(label, term_type) VALUES(?, ?)";
			
			PreparedStatement stmt = null;
			ResultSet keys = null;
			
			try {
				stmt = connection.prepareStatement("SELECT *, count(id) as count FROM terms WHERE label=? AND term_type=?");
				stmt.setString(1, term.getTermType());
				stmt.setString(1, term.getLabel());
				if(term.getTermType() == null) {
					stmt.setNull(2, Types.VARCHAR);
				} else {
					stmt.setString(2, term.getTermType());
				}
				stmt.execute();
				ResultSet rs = stmt.getResultSet();
				rs.next();
				if(rs.getLong("count") != 0) {
					term.setId(rs.getLong("id"));
					return;
				}
				
				stmt = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
				stmt.setString(1, term.getLabel());
				if(term.getTermType() == null) {
					stmt.setNull(2, Types.VARCHAR);
				} else {
					stmt.setString(2, term.getTermType());
				}
				
				stmt.execute();
				
				keys = stmt.getGeneratedKeys();
				
				if(keys.next()) {
					term.setId(keys.getLong(1));
				}
			} catch (SQLException e) {
				logger.error("Could not save terms", e);
			} finally {
				SqlUtils.close(keys);
				SqlUtils.close(stmt);
			}
			return;			
		}

		private void savePagesTerms(PagesTerms pt) {

			if(connection == null) {
				return;
			}
			
			if(pt == null) {
				return;
			}
			
			String query = "INSERT INTO pages_terms(page_id, term_id, weight, created_at, updated_at, source) VALUES(?, ?, ?, ?, ?, ?)";
			
			PreparedStatement stmt = null;
			ResultSet keys = null;
			
			try {
				stmt = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
				stmt.setLong(1, pt.getPi().getId());
				stmt.setLong(2, pt.getTerm().getId());
				if(pt.getWeight() == null) {
					stmt.setNull(3, Types.FLOAT);
				} else {
					stmt.setFloat(3, pt.getWeight());
				}
				stmt.setDate(4, new Date(pt.getCreatedAt().getTime()));
				stmt.setDate(5, new Date(System.currentTimeMillis()));
				if(pt.getSource() == null) {
					stmt.setNull(6, Types.VARCHAR);
				} else {
					stmt.setString(6, pt.getSource());
				}
				
				
				stmt.execute();
				
				keys = stmt.getGeneratedKeys();
				
				if(keys.next()) {
					pt.id = keys.getLong(1);
				}
			} catch (SQLException e) {
				logger.error("Could not save terms", e);
			} finally {
				SqlUtils.close(keys);
				SqlUtils.close(stmt);
			}
			
			return;			
		}
		
		private void save(PageInformation pi) {
			
			savePageInformation(pi);
			
			List<PagesTerms> ptList = pi.getPageTermsList();
			for (PagesTerms pagesTerms : ptList) {
				pagesTerms.setPi(pi);

				Term term = pagesTerms.getTerm();
				saveTerm(term);
				
				savePagesTerms(pagesTerms);
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
		desiredServices.add(StringContentService.class);
		desiredServices.add(DatabaseConnectionProviderService.class);
		desiredServices.add(ClearTextExtractionService.class);
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

			return (ResponseServiceProvider<Service>) new CachingPageInformationProviderServiceProvider(connectionService, content, requestURI, clearText);
		}
		
		return null;
	}
}

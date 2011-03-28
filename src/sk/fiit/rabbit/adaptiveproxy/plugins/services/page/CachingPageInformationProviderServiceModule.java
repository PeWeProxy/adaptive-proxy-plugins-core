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

import com.fourspaces.couchdb.Database;
import com.fourspaces.couchdb.Document;
import com.fourspaces.couchdb.View;
import com.fourspaces.couchdb.ViewResults;

import sk.fiit.peweproxy.headers.ResponseHeader;
import sk.fiit.peweproxy.messages.HttpResponse;
import sk.fiit.peweproxy.messages.ModifiableHttpResponse;
import sk.fiit.peweproxy.plugins.PluginProperties;
import sk.fiit.peweproxy.plugins.services.ResponseServiceModule;
import sk.fiit.peweproxy.plugins.services.ResponseServiceProvider;
import sk.fiit.peweproxy.services.ProxyService;
import sk.fiit.peweproxy.services.ServiceUnavailableException;
import sk.fiit.peweproxy.services.content.StringContentService;
import sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions.DatabaseConnectionProviderService;
import sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions.DatabaseSessionProviderService;
import sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions.PageIDService;
import sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions.PageInformation;
import sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions.PageInformationProviderService;
import sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions.PagesTerms;
import sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions.Term;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.common.MetallClient;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.common.MetallClient.MetallClientException;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.common.SqlUtils;

public class CachingPageInformationProviderServiceModule implements ResponseServiceModule {
	
	private static final Logger logger = Logger.getLogger(CachingPageInformationProviderServiceModule.class);
	
	private class CachingPageInformationProviderServiceProvider 
		implements PageInformationProviderService,
		ResponseServiceProvider<PageInformationProviderService> {
		
		DatabaseConnectionProviderService connectionService;
		String requestURI;
		String content;
		String log_id;
		HttpResponse response;
		
		Connection connection;
		Database database;
		
		public CachingPageInformationProviderServiceProvider(
				HttpResponse response,
				DatabaseConnectionProviderService connectionService,
				String content, String requestURI,
				String log_id) {
			this.response = response;
			this.connectionService = connectionService;
			this.requestURI = requestURI;
			this.content = content;
			this.log_id = log_id;
		}

		PageInformation pi;
		PageInformation piCouchDB;
		
		@Override
		public PageInformation getPageInformation(Connection connection, Database database) {
			
			if(pi != null) {
				return pi;
			}
			
			pi = new PageInformation();
			pi.pageTermsList = new ArrayList<PagesTerms>();
			
			piCouchDB = new PageInformation();
			piCouchDB.pageTermsList = new ArrayList<PagesTerms>();
			
			extractPageInformation(pi);
			
			return pi;
		}

		private void extractPageInformation(PageInformation pi) {
			JSONArray jsonArray = null;
			try {
				connection = connectionService.getDatabaseConnection();
				pi.url = requestURI;
				
				piCouchDB.url = requestURI;

				loadPageInformationFromCache(pi);
				loadPageInformationFromCacheCouchDB(piCouchDB);
				
				if(pi.id == null) {
					try {
						jsonArray = (JSONArray) new JSONParser().parse(new MetallClient().keywords(content));
					} catch (MetallClientException e) {
						logger.warn(e);
					} catch (ParseException e) {
						logger.warn(e);
					}
					
					pi.setId(log_id);
					pi.setPageTermsList(Json2PagesTerms(jsonArray));
					save(pi);
					
					piCouchDB.setId(log_id);
					piCouchDB.setPageTermsList(Json2PagesTerms(jsonArray));
					saveCouchDB(piCouchDB);
					
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
				
				stmt = connection.prepareStatement(query);
				stmt.setString(1, requestURI);
				
				rs = stmt.executeQuery();
				
				if(rs.next()) {
					pi.id = rs.getString(1);
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
				stmt.setString(1, pi.getId());
				
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
		
		private void loadPageInformationFromCacheCouchDB(PageInformation piCouchDB) {
			
			if(database == null) {
				if(response.getServicesHandle().isServiceAvailable(DatabaseSessionProviderService.class)) {
					database = response.getServicesHandle().getService(DatabaseSessionProviderService.class).getDatabase();
				} else {
					return;
				}
			}
			View view = new View("_design/page/_view/url");
			view.setStartKey("\""+(piCouchDB.url)+"\"");
			view.setEndKey("\""+(piCouchDB.url)+"\"");
			ViewResults vr = database.view(view);
			
			if(vr == null || vr.size() == 0) {
				return;
			}
			
			List<Document> list = vr.getResults();
				
			if(list == null || list.size() == 0) {
				return;
			}
			
			Document doc = (Document)list.get(0);
			
			if(doc == null) {
				return;
			}
			
			doc = database.getDocumentWithRevisions((String)doc.get("id"));
			if(doc == null) {
				return;
			}
			
			
			piCouchDB.id = doc.getId();
			piCouchDB.url = doc.containsKey("url") ? doc.getString("url") : null;
			piCouchDB.keywords = doc.containsKey("keywords") ? doc.getString("keywords") : null;
			
			
			net.sf.json.JSONArray pages_terms = doc.getJSONArray("pages_terms");
			
			PagesTerms page_term = null;
			Term term = null;
			for (Object ptObj : pages_terms) {
				net.sf.json.JSONObject pageTermJson = (net.sf.json.JSONObject)ptObj;

				term = new Term();
				term.label = pageTermJson.containsKey("label") ? pageTermJson.getString("label") : null;
				term.termType = pageTermJson.containsKey("term_type") ? pageTermJson.getString("term_type") : null;

				page_term = new PagesTerms();
				page_term.weight = pageTermJson.containsKey("weight") ? new Float(pageTermJson.getDouble("weight")) : null;
				page_term.createdAt = pageTermJson.containsKey("created_at") ? java.sql.Timestamp.valueOf(pageTermJson.getString("created_at")+" 00:00:00") : null;
				page_term.updatedAt = pageTermJson.containsKey("updated_at") ? java.sql.Timestamp.valueOf(pageTermJson.getString("updated_at")+" 00:00:00") : null;
				page_term.source = pageTermJson.containsKey("source") ? pageTermJson.getString("source") : null;
				page_term.term = term;
				page_term.pi = piCouchDB;
				
				piCouchDB.pageTermsList.add(page_term);
			}					
		}		
		
		private List<PagesTerms> Json2PagesTerms(JSONArray jsonArray) {

			if(jsonArray == null) {
				return new JSONArray();
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
			
		private String savePageInformation(PageInformation pi) {
			
			if(connection == null) {
				return null;
			}
			
			String query = "INSERT INTO pages(id, url, keywords) VALUES(?, ?, ?)";
			PreparedStatement stmt = null;
			ResultSet keys = null;
			
			try {
				stmt = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
				stmt.setString(1, pi.getId());
				stmt.setString(2, pi.getUrl());
				stmt.setString(3, pi.getKeywords());
				
				stmt.execute();
				
				keys = stmt.getGeneratedKeys();
				
				if(keys.next()) {
					pi.id = keys.getString(1);
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
				stmt.setString(1, pt.getPi().getId());
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
		
		private void saveCouchDB(PageInformation piCouchDB) {
			if(database == null) {
				if(response.getServicesHandle().isServiceAvailable(DatabaseSessionProviderService.class)) {
					database = response.getServicesHandle().getService(DatabaseSessionProviderService.class).getDatabase();
				} else {
					return;
				}
			}
			
			Document page = new Document();
			page.put("_id", piCouchDB.getId());
			page.put("type", "PAGE");
			page.put("url", piCouchDB.getUrl());
			page.put("keywords", piCouchDB.getKeywords());
			
			JSONArray pages_terms = new JSONArray();
			Document page_term = null;
			List<PagesTerms> ptList = piCouchDB.getPageTermsList();
			for (PagesTerms pt : ptList) {
				page_term = new Document();
				
				page_term.put("label", pt.getTerm().getLabel());
				page_term.put("term_type", pt.getTerm().getTermType());
				page_term.put("weight", pt.getWeight());
				page_term.put("created_at", new Date(pt.getCreatedAt().getTime()).toString());
				page_term.put("updated_at", new Date(System.currentTimeMillis()).toString());
				page_term.put("source", pt.getSource());
				pages_terms.add(page_term);
			}
			
			page.put("pages_terms", pages_terms);
			try {
				database.saveDocument(page);
			} catch (Exception e) {
				System.err.println("Document save error");
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
		desiredServices.add(DatabaseSessionProviderService.class);
		desiredServices.add(PageIDService.class);
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
			) {
			DatabaseConnectionProviderService connectionService = response.getServicesHandle().getService(DatabaseConnectionProviderService.class);
			String requestURI = response.getRequest().getOriginalRequest().getRequestHeader().getRequestURI();
			String content = response.getServicesHandle().getService(StringContentService.class).getContent();
			String log_id = response.getServicesHandle().getService(PageIDService.class).getID();
			return (ResponseServiceProvider<Service>) new CachingPageInformationProviderServiceProvider(response, connectionService, content, requestURI, log_id);
		}
		
		return null;
	}
}

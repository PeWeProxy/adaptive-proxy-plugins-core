/*
 * Website Navigation Adaptation Based on Behavior of Users
 * 
 * Master Thesis
 * Bc. Michal Holub
 * 
 * Faculty of Informatics and Information Technologies
 * Slovak University of Technology
 * Bratislava, 2008 - 2010  
 */
package sk.fiit.rabbit.adaptiveproxy.plugins.services.webimp.newssection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Logger;

import sk.fiit.rabbit.adaptiveproxy.plugins.services.common.SqlUtils;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.database.DatabaseConnectionProviderService;


/**
 * Class that creates news section personalized to specific user. 
 */
public class NewsSection {
	
	static Logger log = Logger.getLogger(NewsSection.class);
	
	/**
	 * Loads personalized news section from database.
	 * 
	 * @param userId value of user agent ID for current user
	 * @param dbService service for working with database
	 * @return HTML code of the news section for user with given userId
	 */
	public String getNewsSectionCode(final String userId, 
			final DatabaseConnectionProviderService dbService) {		
	
		String newsCode = null;
		
		try {
			Connection con = dbService.getDatabaseConnection();
			String query = "SELECT r.code FROM wi_recommendations_news r WHERE r.userid = ?";
			PreparedStatement ps = con.prepareStatement(query);
			ps.setString(1, userId);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				newsCode = rs.getString(1);				
			}
			else {
				log.debug("No recommended news found for user " + userId);
				Statement stmt = con.createStatement();
				ResultSet defNews = stmt.executeQuery("SELECT r.code FROM wi_recommendations_news r WHERE r.userid = 'defaultnews'");
				if (defNews.next()) {
					newsCode = defNews.getString(1);
				}
				else {
					newsCode = "";
					log.warn("No default news found, something is wrong in AdaptiveImp or database.");
				}
				SqlUtils.close(defNews);
				SqlUtils.close(stmt);
			}
			SqlUtils.close(rs);
			SqlUtils.close(ps);
			SqlUtils.close(con);
		} catch (SQLException sqlExc) {
			log.fatal("SQLException: " + sqlExc.getMessage());
			log.fatal("SQLState: " + sqlExc.getSQLState());
			log.fatal("Error code: " + sqlExc.getErrorCode());
		}
		
		return newsCode;	
	}
}

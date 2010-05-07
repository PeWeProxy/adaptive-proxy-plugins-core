package sk.fiit.rabbit.adaptiveproxy.plugins.services.webimp.links;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Logger;

import sk.fiit.rabbit.adaptiveproxy.plugins.services.common.SqlUtils;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.database.DatabaseConnectionProviderService;

public class PersonalizedLinks {
	static Logger log = Logger.getLogger(PersonalizedLinks.class);
	
	public PersonalizedLinks() {
		
	}
	
	/**
	 * Loads personalized links code from database.
	 * 
	 * @param userId value of user agent ID for current user
	 * @param dbService service for working with database
	 * @return HTML code of the links section for user with given userId
	 */
	public String getLinksCode(final String userId, 
			final DatabaseConnectionProviderService dbService) {
		
		String linksCode = null;
		
		try {
			Connection con = dbService.getDatabaseConnection();			
			String query = "SELECT r.code FROM wi_recommendations r WHERE r.userid = ?";
			PreparedStatement ps = con.prepareStatement(query);
			ps.setString(1, userId);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				linksCode = rs.getString(1);				
			}
			else {
				log.debug("No recommended links found for user " + userId);
				Statement stmt = con.createStatement();
				ResultSet defLinks = stmt.executeQuery("SELECT r.code FROM wi_recommendations r WHERE r.userid = 'defaultlinks'");
				if (defLinks.next()) {
					linksCode = defLinks.getString(1);
				}
				else {
					linksCode = "";
					log.warn("No default links found, something is wrong in AdaptiveImp or database.");
				}
				SqlUtils.close(defLinks);
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
		
		return linksCode;
	}
}

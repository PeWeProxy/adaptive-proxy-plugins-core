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
package sk.fiit.rabbit.adaptiveproxy.plugins.services.webimp.calendar;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Logger;

import sk.fiit.rabbit.adaptiveproxy.plugins.services.common.SqlUtils;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.database.DatabaseConnectionProviderService;

/**
 * Class that creates calendar personalized to specific user. 
 */
public class PersonalizedCalendar {
	
	static Logger log = Logger.getLogger(PersonalizedCalendar.class);
	
	public PersonalizedCalendar() {
	}
	
	/**
	 * Loads personalized calendar code from database.
	 * 
	 * @param userId value of user agent ID for current user
	 * @param dbService service for working with database
	 * @return HTML code of a calendar for user with given userId
	 */
	public String getCalendarCode(final String userId, 
			final DatabaseConnectionProviderService dbService) {		
		String calendarCode = null;
		
		try {
			Connection con = dbService.getDatabaseConnection();
			String query = "SELECT code FROM wi_calendars c WHERE userid = ?";
			PreparedStatement ps = con.prepareStatement(query);
			ps.setString(1, userId);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				calendarCode = rs.getString(1);
				log.debug("No calendar found for user " + userId);
			}
			else {
				Statement stmt = con.createStatement();
				ResultSet defCal = stmt.executeQuery("SELECT code FROM wi_calendars c WHERE userid = 'defaultcalendar'");
				if (defCal.next()) {
					calendarCode = defCal.getString(1);
				}
				else {
					calendarCode = "";
					log.warn("No default calendar found, something is wrong in AdaptiveImp or database.");
				}
				SqlUtils.close(defCal);
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
		
		return calendarCode;		
	}
}

/*
 * Website Navigation Adaptation Based on Behavior of Users
 * Master Thesis
 * Bc. Michal Holub
 * 
 * Faculty of Informatics and Information Technologies
 * Slovak University of Technology
 * Bratislava, 2008 - 2010  
 */
package sk.fiit.rabbit.adaptiveproxy.plugins.services.webimp.calendar;

import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * Class that creates calendar personalized to specific user. 
 */
public class PersonalizedCalendar {
	
	private String[] months = {"Január", "Február", "Marec", "Apríl", "Máj", "Jún",
			"Júl", "August", "September", "Október", "November", "December"}; 
	
	public PersonalizedCalendar() {
	}
	
	/**
	 * Creates HTML code of personalized calendar for a given web page source.
	 * The code is modified Google Calendar code.
	 * Source: calendar.google.com
	 * 
	 * @param page web page to which we want to add calendar
	 * @return HTML code of a calendar
	 */
	public String getCalendarCode() {		
		Set<Date> dates = new HashSet<Date>();	// load dates from DB
		String htmlCode = "<div class=\"box_nadpis\">\n" 
			+ "osobný kalendár <span>›</span>\n"
			+ "</div>\n";
		
		if (true) {
			//String heading = "Event heading.";
			
			htmlCode = htmlCode
			+ "<table id=\"dp_0_tbl\" class=\"dp-monthtable\" cellspacing=\"0\" cellpadding=\"0\">"
			+ "<tbody>"
			+ "<tr id=\"dp_0_header\" class=\"dp-cell dp-heading\" style=\"cursor: pointer;\">"
			+ "<td id=\"dp_0_prev\" class=\"dp-cell dp-prev\"><</td>"
			+ "<td id=\"dp_0_cur\" class=\"dp-cell dp-cur\" colspan=\"5\">"
			+ months[Calendar.getInstance().get(Calendar.MONTH)]
			+ " "
			+ String.valueOf(Calendar.getInstance().get(Calendar.YEAR))
			+ "</td>"
			+ "<td id=\"dp_0_next\" class=\"dp-cell dp-next\">></td>"
			+ "</tr>"
			+ "<tr class=\"dp-days\">"
			+ "<td class=\"dp-cell dp-dayh\">Po</td>"
			+ "<td class=\"dp-cell dp-dayh\">Ut</td>"
			+ "<td class=\"dp-cell dp-dayh\">St</td>"
			+ "<td class=\"dp-cell dp-dayh\">Št</td>"
			+ "<td class=\"dp-cell dp-dayh\">Pi</td>"
			+ "<td class=\"dp-cell dp-dayh\">So</td>"
			+ "<td class=\"dp-cell dp-dayh\">Ne</td>"
			+ "</tr>"
			+ "<tr id=\"dp_0_row_0\" style=\"cursor: pointer;\">";
			
			Calendar tempCal = Calendar.getInstance();
			tempCal.setFirstDayOfWeek(Calendar.MONDAY);
			tempCal.set(Calendar.DAY_OF_MONTH, 1);			
			int dayNo = tempCal.get(Calendar.DAY_OF_WEEK);
			if (dayNo == 1) {
				dayNo = 7;
			}
			else {
				dayNo--;
			}
			
			for (int i = 1; i < dayNo; i++) {
				htmlCode += "<td id=\"dp_0_day_20380\" class=\"dp-cell dp-weekday dp-offmonth\">&nbsp;</td>";
			}
			
			for (int i = 1; i < tempCal.getActualMaximum(Calendar.DAY_OF_MONTH)+1; i++) {
				htmlCode += "<td id=\"dp_0_day_20383\" class=\"dp-cell";
				if (dayNo % 6 == 0 || dayNo % 7 == 0) {
					htmlCode += " dp-weekend";
				}
				else {
					htmlCode += " dp-weekday";
				}
				if (i == Calendar.getInstance().get(Calendar.DATE)) {
					htmlCode += " dp-today";
				}
				for (Date date : dates) {
					tempCal.setTime(date);
					if (tempCal.get(Calendar.YEAR) == Calendar.getInstance().get(Calendar.YEAR)
							&& tempCal.get(Calendar.MONTH) == Calendar.getInstance().get(Calendar.MONTH)
							&& tempCal.get(Calendar.DATE) == i) {
						htmlCode += " dp-event-my";
					}
				}
				htmlCode += " dp-onmonth\">" + i + "</td>"; 
				
				dayNo++;
				if (dayNo == 8) {
					htmlCode += "</tr>";
					if (i < tempCal.getActualMaximum(Calendar.DAY_OF_MONTH)) {
						htmlCode += "<tr id=\"dp_0_row_1\" style=\"cursor: pointer;\">";
					}
					dayNo = 1;
				}
			}
			
			if (dayNo > 1) {
				for (int i = dayNo; i < 8; i++) {
					htmlCode += "<td id=\"dp_0_day_20380\" class=\"dp-cell dp-weekday dp-offmonth\">&nbsp;</td>";
				}
			}
			
			// dp-event-my, dp-event-recommended, dp-today
			
			htmlCode += "</tbody>";
			htmlCode += "</table>";
		} // end if
		
		return htmlCode;
	}
}
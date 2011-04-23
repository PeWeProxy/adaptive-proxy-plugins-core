package sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions;

import sk.fiit.peweproxy.services.ProxyService;
import sk.fiit.peweproxy.services.ProxyService.readonly;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.logging.backends.LoggingBackendFailure;

public interface LoggingBackendService extends ProxyService {

	/**
	 * Log the page access.
	 * 
	 * @param accessGuid GUID of the access.
	 * @param userId ID of the user. May not be known at this time.
	 * @param uri URI of the accessed page
	 * @param content textual content of the accessed page
	 * @param referrer HTTP referrer
	 * @param ip client's IP address
	 * @param ip2 
	 * 
	 * @throws LoggingBackendFailure
	 */
	@readonly
	public void logPageAccess(String accessGuid, String userId, String uri, String content, String referrer, String ip) throws LoggingBackendFailure;

	/**
	 * Update activity on the specified access (identified by its GUID)
	 * 
	 * @param userId id of the user who has requested the page
	 * @param accessGuid GUID of the access
	 * @param timeOnPage increment to the total time spent on the page
	 * @param scrollCount increment to the total count of scrolling activity
	 * @param copyCount increment to the total count of copying to clipboard
	 */
	@readonly
	public void logActivity(String userId, String accessGuid, String timeOnPage, String scrollCount, String copyCount);
}

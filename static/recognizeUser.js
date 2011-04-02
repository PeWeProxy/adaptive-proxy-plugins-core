
var __ap_url = document.location.href;

userRecognition = function($) {
	upload_userId = function() {
			$.post('./userRecognitionRequest.html?nologging', { '__peweproxy_uid': __peweproxy_uid, '__ap_url': __ap_url, 'page_uid' : page_uid, 'log_id' : log_id });
	}
	if (typeof(__peweproxy_uid) == 'undefined' || __peweproxy_uid == null) {
		__ap_register_callback('upload_userId()')
	} else {
		upload_userId();
	}
}(adaptiveProxyJQuery);

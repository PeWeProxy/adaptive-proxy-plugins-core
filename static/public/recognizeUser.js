
var __ap_url = document.location.href;

userRecognition = function($) {
	upload_userId = function() {


		if ((__peweproxy_uid != null))
		{
			$.post('./userRecognitionRequest.html', { '__peweproxy_uid': __peweproxy_uid, '__ap_url': __ap_url, 'page_uid' : page_uid, 'log_id' : log_id });

		}

	}
	upload_userId();
}(adaptiveProxyJQuery);

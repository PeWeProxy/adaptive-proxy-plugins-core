
var __ap_url = document.location.href;

userRecognition = function($) {
	upload_userId = function() {


		if ((__peweproxy_uid != null) && (_ap_checksum != null))
		{
			$.post('./userRecognitionRequest.html', { '__peweproxy_uid': __peweproxy_uid, '__ap_url': __ap_url, '_ap_checksum' : _ap_checksum, 'page_uid' : page_uid });

		}

	}
	upload_userId();
}(adaptiveProxyJQuery);

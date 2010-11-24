
var __ap_url = document.location.href;

userRecognition = function($) {
	upload_userId = function() {


		if ((__peweproxy_uid != null) && (_ap_checksum != null))
		{
			$.post('./userRecognitionRequest.html', { '__peweproxy_uid': __peweproxy_uid, '_ap_checksum': _ap_checksum, '__ap_url': __ap_url,
					'_ap_uuid' : _ap_uuid });

		}

	}
	upload_userId();
}(adaptiveProxyJQuery);

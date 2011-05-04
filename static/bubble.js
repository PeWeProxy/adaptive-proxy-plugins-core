peweproxy.register_module('bubble', function($) {
	var smallButtonSelector = 'div#peweproxy_addons_container a.__peweproxy_addons_button';
	var peweproxy_addonIconBannerSelector = 'div#peweproxy_icon_banner';
	
	$(document).ready(function() {
		$(peweproxy_addonIconBannerSelector).addClass('hidden');
		$(smallButtonSelector).mouseenter(function() {
			$(peweproxy_addonIconBannerSelector).removeClass('hidden').fadeIn('fast', function() {});
		});
		$(peweproxy_addonIconBannerSelector).mouseleave(function() {
			$(this).fadeOut('fast', function() {
				$(this).addClass('hidden');
			});
		});
	});
});

peweproxy.register_module('bubble', function($) {
	this.smallButtonSelector = 'div#peweproxy_addons_container a.__peweproxy_addons_button';
	this.addonIconBannerSelector = 'div#peweproxy_icon_banner';
	
	var smallButtonSelector = this.smallButtonSelector;
	var addonIconBannerSelector = this.addonIconBannerSelector
	
	$(document).ready(function() {
		$(addonIconBannerSelector).addClass('hidden');
		$(smallButtonSelector).mouseenter(function() {
			$(addonIconBannerSelector).removeClass('hidden').fadeIn('fast', function() {});
		});
		$(addonIconBannerSelector).mouseleave(function() {
			$(this).fadeOut('fast', function() {
				$(this).addClass('hidden');
			});
		});
	});
});

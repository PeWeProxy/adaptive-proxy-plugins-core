var smallButtonSelector = 'div#peweproxy_addons_container a.__peweproxy_addons_button';

var peweproxy_addonIconBannerSelector = 'div#peweproxy_icon_banner';

var temp = function($) {
    $(document).ready(function(){
		
            $(peweproxy_addonIconBannerSelector).addClass('hidden');

		
            $(smallButtonSelector).mouseenter(function(){
                $(peweproxy_addonIconBannerSelector).removeClass('hidden').fadeIn('fast', function(){
                });
            });

            $(peweproxy_addonIconBannerSelector).mouseleave(function(){
                $(this).fadeOut('fast',function(){
                    $(this).addClass('hidden');
                });
            });
        
        
	});
	
	$(document).scroll(function(){
        $('#peweproxy_addons_container').animate({
            'top':$(document).scrollTop()
        }, 'fast');
    });
	
} (adaptiveProxyJQuery);

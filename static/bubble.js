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

        $('div#peweproxy_icon_banner a.__peweproxy_preference_button').click(function(){
                $(this).blur();
                renewSmallButton = false;
                $('#__peweproxy_preferences').hide().removeClass('hidden').fadeIn('fast');
                $(peweproxy.modules.bubble.peweproxy_addonIconBannerSelector).addClass('hidden');
                $(peweproxy.modules.bubble.smallButtonSelector).addClass('hidden');
                return false;
        });
	
	
});

peweproxy.register_module('preferences', function($) {

        $(document).ready(function(){
            $('div#__peweproxy_preferences a.__peweproxy_preference_closebutton').click(function() {
                $(peweproxy.modules.bubble.smallButtonSelector).removeClass('hidden');
                $('#__peweproxy_preferences').fadeOut('fast');
                return false;
            });
        });
	
	this.updateField = function(fieldId) {
	    $('#' + fieldId + ' .__peweproxy_preference_row_display').css("display", "none");
	    $('#' + fieldId + ' .__peweproxy_preference_row_updating').css("display", "inline");
	
	    $('#' + fieldId + ' .__peweproxy_preference_row_display').css("display", "none");
            $('#' + fieldId + ' .__peweproxy_preference_row_updating').css("display", "inline");
	}

        this.cancelUpdate = function(fieldId) {
            var oldFieldValue = $('#' + fieldId + ' .__peweproxy_preference_table_value .__peweproxy_preference_row_display').text();

            $('#' + fieldId + ' .__peweproxy_preference_row_updating input').val(oldFieldValue);

            $('#' + fieldId + ' .__peweproxy_preference_row_updating').css("display", "none");
            $('#' + fieldId + ' .__peweproxy_preference_row_display').css("display", "inline");

            $('#' + fieldId + ' .__peweproxy_preference_row_updating').css("display", "none");
            $('#' + fieldId + ' .__peweproxy_preference_row_display').css("display", "inline");
        }


        this.confirmUpdate = function(fieldId) {
            $(document).ready(function() {
                var newFieldValue = $('#' + fieldId + ' .__peweproxy_preference_row_updating input').val();

                var preference_namespace = fieldId.substring(0, fieldId.indexOf("_"));
                var preference_name = fieldId.substring(fieldId.indexOf("_") + 1);

                $.post('adaptive-proxy/user_preferences_call.html?action=update_preference', { preference_name: preference_name, preference_namespace: preference_namespace, new_value: newFieldValue, uid: peweproxy.uid }, function(response) {

                    if (response == "OK") {
                        $('#' + fieldId + ' .__peweproxy_preference_table_value .__peweproxy_preference_row_display').text(newFieldValue);

                        $('#' + fieldId + ' .__peweproxy_preference_row_updating').css("display", "none");
                        $('#' + fieldId + ' .__peweproxy_preference_row_display').css("display", "inline");

                        $('#' + fieldId + ' .__peweproxy_preference_row_updating').css("display", "none");
                        $('#' + fieldId + ' .__peweproxy_preference_row_display').css("display", "inline");

                        eval($('#' + fieldId + ' .__peweproxy_preference_function_call').text());
                    }
                    else {
                        $('#' + fieldId + ' .__peweproxy_preference_table_value .__peweproxy_preference_row_display').text('');
                        $('#' + fieldId + ' .__peweproxy_preference_table_value .__peweproxy_preference_row_display').append("<span style='color: darkred;'>nepodarilo sa upraviù</span>");

                        $('#' + fieldId + ' .__peweproxy_preference_row_updating').css("display", "none");
                        $('#' + fieldId + ' .__peweproxy_preference_row_updating').css("display", "none");
                        $('#' + fieldId + ' .__peweproxy_preference_table_value .__peweproxy_preference_row_display').css("display", "inline");
                    }
                });
            });
        }

        this.deactivatePlugin = function(preference_namespace) {
            $(document).ready(function() {
                $.post('adaptive-proxy/user_preferences_call.html?action=update_preference', { preference_name: "activity", preference_namespace: preference_namespace, new_value: "false", uid: peweproxy.uid }, function(response) {
                    if (response == "OK") {
                        $('#' + preference_namespace).removeClass('activated');
                        $('#' + preference_namespace). addClass('deactivated');
                    }
                });
            });
        }

        this.activatePlugin = function(preference_namespace) {
            $(document).ready(function() {
                $.post('adaptive-proxy/user_preferences_call.html?action=update_preference', { preference_name: "activity", preference_namespace: preference_namespace, new_value: "true", uid: peweproxy.uid }, function(response) {
                    if (response == "OK") {
                        $('#' + preference_namespace).removeClass('deactivated');
                        $('#' + preference_namespace). addClass('activated');
                    }
                });
            });
        }
});

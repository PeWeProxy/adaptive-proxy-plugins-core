package sk.fiit.rabbit.adaptiveproxy.plugins.services.bubble;

import java.util.HashSet;
import java.util.Set;

import sk.fiit.peweproxy.messages.ModifiableHttpResponse;
import sk.fiit.peweproxy.plugins.PluginProperties;
import sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions.HtmlInjectorService;
import sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions.UserIdentificationService;
import sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions.UserPreferencesProviderService;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.injector.JavaScriptInjectingProcessingPlugin;

public class BubbleMenuProcessingPlugin extends JavaScriptInjectingProcessingPlugin {
	private String buttonHTML;
	private String windowHTML;
	private String preferenceLabel;
	private String preferenceNamespace;
	private String functionCall;
	private Set<String> preferences = new HashSet<String>();
	private String tableRowTemplate;
	private String tableTemplate;
	private String activity;
	private String userId;

	@Override
	public ResponseProcessingActions processResponse(ModifiableHttpResponse response) {
		if ((response.getServicesHandle().isServiceAvailable(HtmlInjectorService.class)) &&
				response.getServicesHandle().isServiceAvailable(UserPreferencesProviderService.class) &&
				response.getServicesHandle().isServiceAvailable(UserIdentificationService.class)) {
			
			HtmlInjectorService htmlInjector = response.getServicesHandle().getService(HtmlInjectorService.class);
			userId = response.getServicesHandle().getService(UserIdentificationService.class).getClientIdentification();
			activity = response.getServicesHandle().getService(UserPreferencesProviderService.class).getProperty("activity", userId, preferenceNamespace);
			if ((activity == null) || (activity.equals("true"))) {
				activity = "activated";
				htmlInjector.injectAfter("<!-- bubble menu -->", "<td>" + buttonHTML + "</td>");
				htmlInjector.injectAfter("<!-- bubble windows -->", windowHTML);
			} else {
				activity = "deactivated";
			}
			
			htmlInjector.injectAfter("<!-- preference tables -->", getPreferenceHTML(response));
			
		}
		return super.processResponse(response);
	}
	
	private String getPreferenceHTML(ModifiableHttpResponse response) {
		String preferenceTableRow;
		String preferenceTable;
			
		preferenceTable = tableTemplate.replace("[:activity:]", activity);
		preferenceTable = preferenceTable.replace("[:preference:]", preferenceNamespace);
		preferenceTable = preferenceTable.replace("[:preferenceLabel:]", preferenceLabel);
		
		for (String preference : preferences) {
			String preferenceName;
			String preferenceLabel;
			String preferenceValue;
			
			if (preference.contains("|")) {
				preferenceName = preference.substring(0, preference.indexOf("|"));
				preferenceLabel = preference.substring(preference.indexOf("|") + 1);
			} else {
				preferenceName = preference;
				preferenceLabel = preference;
			}
			String prefId = preferenceNamespace + "_" + preferenceName;
			
			preferenceTableRow = tableRowTemplate.replace("[:preferenceId:]", prefId);
			preferenceTableRow = preferenceTableRow.replace("[:functionCall:]", functionCall);
			preferenceTableRow = preferenceTableRow.replace("[:preferenceName:]", preferenceLabel);
			
			preferenceValue = response.getServicesHandle().getService(UserPreferencesProviderService.class).getProperty(preferenceName, userId, preferenceNamespace);
			if (preferenceValue == null) {
				preferenceValue = "";
			}
			preferenceTableRow = preferenceTableRow.replace("[:preferenceValue:]", preferenceValue);
			
			preferenceTable += preferenceTableRow; 
		}
		preferenceTable += "</tbody></table>";
		
		return preferenceTable;
	}

	@Override
	public boolean start(PluginProperties props) {
		this.setTemplates();
		buttonHTML = props.getProperty("buttonHTML", "");
		windowHTML = props.getProperty("windowHTML", "");
		functionCall = props.getProperty("functionCall", "");
		preferenceNamespace = props.getProperty("preferenceNamespace", "global");
		preferenceLabel = props.getProperty("preferenceLabel", "Globalne nastavenia");
		
		if(props.getProperty("preferences") != null) {
			preferences.clear();
			for (String preference : props.getProperty("preferences").split(",")) {
				preferences.add(preference.trim());
			}
		}
		return super.start(props);
	}

	private void setTemplates() {
		// unable to set through xml config 
		// TODO: remove static address
		tableRowTemplate = "<tr id='[:preferenceId:]'>" +
				"		<td>[:preferenceName:]</td>" +
				"		<td class='__peweproxy_preference_table_value'>" +
				"			<span class='__peweproxy_preference_row_display'>[:preferenceValue:]</span>" +
				"			<span class='__peweproxy_preference_row_updating'>" +
				"				<input value='[:preferenceValue:]'>" +
				"			</span>" +
				"		</td>" +
				"		<td class='__peweproxy_preference_update_btn'>" +
				"			<span class='__peweproxy_preference_row_display'>" +
				"				<a onclick='peweproxy.modules.preferences.updateField(\"[:preferenceId:]\");' href='#'><img alt='edit' src='http://127.0.0.1:9666/FileSender/public/preferenceImages/edit_icon.png'></a>" +
				"			</span>" +
				"			<span class='__peweproxy_preference_row_updating'>" +
				"				<a onclick='peweproxy.modules.preferences.confirmUpdate(\"[:preferenceId:]\");' href='#'><img alt='ok' src='http://127.0.0.1:9666/FileSender/public/preferenceImages/ok_icon.png'></a>" +
				"				<a alt='cancel' onclick='peweproxy.modules.preferences.cancelUpdate(\"[:preferenceId:]\");' href='#'><img alt='cancel' src='http://127.0.0.1:9666/FileSender/public/preferenceImages/delete_icon.png'></a>" +
				"			</span>" +
				"			<span class='__peweproxy_preference_function_call' style='display: none;'>[:functionCall:]</span>" +
				"		</td>" +
				"	</tr>";
		
		tableTemplate = "<table id='[:preference:]' class='__peweproxy_preferences_table [:activity:]'>" +
				"	<thead>" +
				"		<tr>" +
				"			<th colspan='3' class='__peweproxy_preference_name'>[:preferenceLabel:]" + 
				"				<a class='__peweproxy_preference_activate_btn' onclick='peweproxy.modules.preferences.activatePlugin(\"[:preference:]\")'>Aktivovať'</a>" +
				"				<a class='__peweproxy_preference_deactivate_btn' onclick='peweproxy.modules.preferences.deactivatePlugin(\"[:preference:]\")'>Deaktivovať'</a>" +
				"			</th>" +
				"		</tr>" +
				"	</thead>" +
				"	<tfoot>" +
				"		<tr>" +
				"			<td colspan='3'></td>" +
				"		</tr>" +
				"	</tfoot>" +
				"<tbody>";
	}
}

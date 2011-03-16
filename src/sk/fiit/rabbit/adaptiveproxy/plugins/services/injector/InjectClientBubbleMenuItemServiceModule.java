package sk.fiit.rabbit.adaptiveproxy.plugins.services.injector;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import sk.fiit.peweproxy.headers.ResponseHeader;
import sk.fiit.peweproxy.messages.HttpResponse;
import sk.fiit.peweproxy.messages.ModifiableHttpResponse;
import sk.fiit.peweproxy.plugins.PluginProperties;
import sk.fiit.peweproxy.plugins.services.ResponseServiceModule;
import sk.fiit.peweproxy.plugins.services.ResponseServiceProvider;
import sk.fiit.peweproxy.services.ProxyService;
import sk.fiit.peweproxy.services.ServiceUnavailableException;
import sk.fiit.peweproxy.services.content.ModifiableStringService;
import sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions.InjectClientBubbleMenuItemService;


public class InjectClientBubbleMenuItemServiceModule implements ResponseServiceModule {
	private static final Logger logger = Logger.getLogger(InjectClientBubbleMenuItemServiceModule.class);
	private String bubbleMenuStyles = "";
	private String bubbleMenuScript = "";
    
	
	private class InjectClientBubbleMenuItemServiceProvider implements InjectClientBubbleMenuItemService, ResponseServiceProvider<InjectClientBubbleMenuItemService> {
		private List<String> menuButtons = new LinkedList<String>();
		private List<String> menuWindows = new LinkedList<String>();
		private List<String> menuScripts = new LinkedList<String>();
		
		private int insertIndex;
		private String requestURI;
		
		public InjectClientBubbleMenuItemServiceProvider(String requestURI) {
			this.requestURI = requestURI;
		}
		
		@Override
		public void injectScript(String scriptHtml) {
			logger.warn("Som " + this + " a pridavam do queue s velkostou " + menuButtons.size());
			menuScripts.add(scriptHtml);
		}
		
		@Override
		public void injectWindow(String windowHtml) {
			logger.warn("Som " + this + " a pridavam do queue s velkostou " + menuButtons.size());
			menuWindows.add(windowHtml);
		}
		
		@Override
		public void injectButton(String buttonHtml) {
			logger.warn("Som " + this + " a pridavam do queue s velkostou " + menuButtons.size());
			menuButtons.add(buttonHtml); 
		}
		
		private void injectClientBubbleMenu(StringBuilder content) {
			String html = content.toString().toLowerCase();
			String htmlInjection = "";
			insertIndex = 0;
			
			if (html.indexOf("<!-- client bubble menu -->") < 0) {
				insertIndex = html.indexOf("<body");
				if(insertIndex > 0) {
					insertIndex = html.indexOf(">", insertIndex);
					if(insertIndex > 0) {
						insertIndex++;
					}
					htmlInjection = "<!-- client bubble menu --><div id=\"peweproxy_addons_container\"><!-- bubble script --><a href=\"#\" class=\"__peweproxy_addons_button\"></a><div style=\"\" id=\"peweproxy_icon_banner\" class=\"hidden\"><table><tr><!-- bubble menu --></tr></table></div><!-- bubble window --></div>";
				} else {
					logger.debug("No <body> found for " + requestURI);
				}
			}
			
			if(insertIndex > 0) {
				content.insert(insertIndex, htmlInjection);
			}
		}
		
		private void injectClientBubbleMenuStyles(StringBuilder content) {
			String html = content.toString().toLowerCase();
			String htmlInjection = "";
			insertIndex = 0;
			if (html.indexOf("<!-- client bubble menu styles -->") < 0) {
				insertIndex = html.indexOf("<body");
				if(insertIndex > 0) {
					insertIndex = html.indexOf(">", insertIndex);
					if(insertIndex > 0) {
						insertIndex++;
					}
					htmlInjection = "<!-- client bubble menu styles -->" + bubbleMenuStyles;
				} else {
					logger.debug("No <body> found for " + requestURI);
				}
			}
			
			if(insertIndex > 0) {
				content.insert(insertIndex, htmlInjection);
			}
		}
		
		private void injectClientBubbleMenuScript(StringBuilder content) {
			String html = content.toString().toLowerCase();
			String htmlInjection = "";
			insertIndex = 0;
			if (html.indexOf("<!-- client bubble menu script -->") < 0) {
				insertIndex = html.indexOf("<body");
				if(insertIndex > 0) {
					insertIndex = html.indexOf(">", insertIndex);
					if(insertIndex > 0) {
						insertIndex++;
					}
					htmlInjection = "<!-- client bubble menu script -->" + "<script type='text/javascript' src='" + bubbleMenuScript  + "'></script>";
				} else {
					logger.debug("No <body> found for " + requestURI);
				}
			}
			
			if(insertIndex > 0) {
				content.insert(insertIndex, htmlInjection);
			}
		}

		
		private void injectMenuButtonsToMessage(StringBuilder content, String htmlInjection) {
			String html = content.toString().toLowerCase();
			
			if (html.indexOf("<!-- bubble menu -->") >= 0) {
				insertIndex = html.indexOf("<!-- bubble menu -->");
				if(insertIndex > 0) {
					insertIndex = html.indexOf(">", insertIndex);
					if(insertIndex > 0) {
						insertIndex++;
					}
				} else {
					logger.debug("No <!-- bubble menu --> found for " + requestURI);
				}
			}
			
			if(insertIndex > 0) {
				content.insert(insertIndex, "<td>" + htmlInjection + "</td>");
			}
		}
		
		private void injectMenuWindowsToMessage(StringBuilder content, String htmlInjection) {
			String html = content.toString().toLowerCase();
			
			if (html.indexOf("<!-- bubble window -->") >= 0) {
				insertIndex = html.indexOf("<!-- bubble window -->");
				if(insertIndex > 0) {
					insertIndex = html.indexOf(">", insertIndex);
					if(insertIndex > 0) {
						insertIndex++;
					}
				} else {
					logger.debug("No <!-- bubble window --> found for " + requestURI);
				}
			}
			
			if(insertIndex > 0) {
				content.insert(insertIndex, htmlInjection);
			}
		}
		
		private void injectMenuScriptsToMessage(StringBuilder content, String htmlInjection) {
			String html = content.toString().toLowerCase();
			
			if (html.indexOf("<!-- bubble script -->") >= 0) {
				insertIndex = html.indexOf("<!-- bubble script -->");
				if(insertIndex > 0) {
					insertIndex = html.indexOf(">", insertIndex);
					if(insertIndex > 0) {
						insertIndex++;
					}
				} else {
					logger.debug("No <!-- bubble script --> found for " + requestURI);
				}
			}
			
			if(insertIndex > 0) {
				content.insert(insertIndex, htmlInjection);
			}
		}

		@Override
		public void doChanges(ModifiableHttpResponse response) {
			String clientMenuHtml = "";
			
			if(response.getServicesHandle().isServiceAvailable(ModifiableStringService.class)) {
				StringBuilder content = response.getServicesHandle().getService(ModifiableStringService.class).getModifiableContent();
				
				if ((menuButtons.size() > 0) || (menuWindows.size() > 0) || (menuScripts.size() > 0)) {
					injectClientBubbleMenu(content);
					injectClientBubbleMenuStyles(content);
					injectClientBubbleMenuScript(content);
					
					clientMenuHtml = "";
					for (String menuButton : menuButtons) {
						clientMenuHtml += menuButton;
					}
					injectMenuButtonsToMessage(content, clientMenuHtml);
				
					clientMenuHtml = "";
					for (String menuWindow : menuWindows) {
						clientMenuHtml += menuWindow;
					}
					injectMenuWindowsToMessage(content, clientMenuHtml);
					
					clientMenuHtml = "";
					for (String menuScript : menuScripts) {
						clientMenuHtml += menuScript;
					}
					injectMenuScriptsToMessage(content, clientMenuHtml);
				}
			}
			
		}
			
		@Override
		public String getServiceIdentification() {
			return this.getClass().getName();
		}
	
		@Override
		public InjectClientBubbleMenuItemService getService() {
			return this;
		}
	
		@Override
		public boolean initChangedModel() {
			return false;
		}
	}


	@Override
	public boolean start(PluginProperties props) {
		this.bubbleMenuStyles = props.getProperty("bubbleMenuStyles");
		this.bubbleMenuScript = props.getProperty("bubbleMenuScript");
		return true;                              
	}

	@Override
	public void desiredResponseServices(
			Set<Class<? extends ProxyService>> desiredServices,
			ResponseHeader webRPHeader) {
		desiredServices.add(ModifiableStringService.class);
	}

	@Override
	public void getProvidedResponseServices(
			Set<Class<? extends ProxyService>> providedServices) {
		providedServices.add(InjectClientBubbleMenuItemService.class);
		
	}
	
	@Override
	public boolean supportsReconfigure(PluginProperties newProps) {
		return true;
	}

	@Override
	public void stop() {
	}

	@Override
	public <Service extends ProxyService> ResponseServiceProvider<Service> provideResponseService(
			HttpResponse response, Class<Service> serviceClass)
			throws ServiceUnavailableException {

		if (serviceClass == InjectClientBubbleMenuItemService.class) {
			String requestURI = response.getRequest().getOriginalRequest().getRequestHeader().getRequestURI();
			return (ResponseServiceProvider<Service>) new InjectClientBubbleMenuItemServiceProvider(requestURI);
		}
		
		return null;
	}
		
}

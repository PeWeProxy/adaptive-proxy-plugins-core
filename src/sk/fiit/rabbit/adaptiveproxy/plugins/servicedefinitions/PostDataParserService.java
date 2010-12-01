package sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions;

import java.util.Map;

import sk.fiit.peweproxy.services.ProxyService;

public interface PostDataParserService extends ProxyService {
    
    public Map<String, String> getPostData();
}

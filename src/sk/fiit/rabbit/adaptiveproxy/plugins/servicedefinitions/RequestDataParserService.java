package sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions;

import java.util.Map;

import sk.fiit.peweproxy.services.ProxyService;

public interface RequestDataParserService extends ProxyService {
    public Map<String, String> getDataFromPOST();
    public Map<String, String> getDataFromGET();
}

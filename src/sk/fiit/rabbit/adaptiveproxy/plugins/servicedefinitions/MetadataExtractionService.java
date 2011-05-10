package sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions;

import java.util.List;
import java.util.Map;

import sk.fiit.peweproxy.services.ProxyService;

public interface MetadataExtractionService extends ProxyService {
	public List<Map> metadata();
}

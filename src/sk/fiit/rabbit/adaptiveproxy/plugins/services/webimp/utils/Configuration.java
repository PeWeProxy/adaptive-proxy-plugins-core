package sk.fiit.rabbit.adaptiveproxy.plugins.services.webimp.utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.log4j.Logger;

public class Configuration {
	private static Configuration instance = null;
	static Logger log = Logger.getLogger(Configuration.class);
	
	private String startPage;
	private String domain;
	private String webMap;
	private String dbName;
	private String dbUser;
	private String dbPassword;
	private String structureFile;
	
	private Configuration() {
		try {
			Properties config = new Properties();
			config.load(new FileInputStream("conf/webimp_conf.properties"));			
			startPage = config.getProperty("start_url");
			domain = config.getProperty("domain");
			webMap = config.getProperty("web_map");
			dbName = config.getProperty("db_name");
			dbUser = config.getProperty("db_user");
			dbPassword = config.getProperty("db_password");
			structureFile = config.getProperty("web_structure_file");
		} catch (IOException ioExc) {
			log.fatal(ioExc.getMessage());
			System.exit(1);
		}
	}
	
	public String getStartPage() {
		return startPage;
	}
	
	public String getDomain() {
		return domain;
	}
	
	public String getWebMap() {
		return webMap;
	}
	
	public String getDbName() {
		return dbName;
	}
	
	public String getDbUser() {
		return dbUser;
	}
	
	public String getDbPassword() {
		return dbPassword;
	}
	
	public String getStructureFile() {
		return structureFile;
	}
	
	public static Configuration getInstance() {
		if (instance == null) {
			instance = new Configuration();
		}
		return instance;
	}
}

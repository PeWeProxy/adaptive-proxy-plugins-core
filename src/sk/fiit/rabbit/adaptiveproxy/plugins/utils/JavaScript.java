package sk.fiit.rabbit.adaptiveproxy.plugins.utils;

import org.apache.commons.lang.StringEscapeUtils;

public class JavaScript {
	public static String wrap(String script) {
		return "<script type='text/javascript'>" + script + "</script>";
	}
	
	public static String escape(String script) {
		return StringEscapeUtils.escapeJavaScript(script);
	}
}

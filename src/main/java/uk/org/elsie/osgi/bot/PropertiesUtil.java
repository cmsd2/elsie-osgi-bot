package uk.org.elsie.osgi.bot;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Map;
import java.util.TreeMap;

import org.osgi.service.event.Event;

public class PropertiesUtil {
	public static Map<String, Object> publicPropertiesAsMap(Dictionary<String,Object> dict) {
		TreeMap<String, Object> map = new TreeMap<String, Object>();
		if(dict != null) {
			Enumeration<String> e = dict.keys();
			while(e.hasMoreElements()) {
				String k = e.nextElement();
				if(!k.startsWith(".")) {
					map.put(k, dict.get(k));
				}
			}
		}
		return map;
	}
	
	public static Map<String, Object> publicPropertiesAsMap(Map<String,Object> dict) {
		TreeMap<String, Object> map = new TreeMap<String, Object>();
		if(dict != null) {
			for(Map.Entry<String, Object> me : dict.entrySet()) {
				if(!me.getKey().startsWith(".")) {
					map.put(me.getKey(), me.getValue());
				}
			}
		}
		return map;
	}
	
	public static Map<String, Object> propertiesAsMap(Dictionary<String,Object> dict) {
		TreeMap<String, Object> map = new TreeMap<String, Object>();
		if(dict != null) {
			Enumeration<String> e = dict.keys();
			while(e.hasMoreElements()) {
				String k = e.nextElement();
				map.put(k, dict.get(k));
			}
		}
		return map;
	}

	public static Map<String, Object> propertiesAsMap(Map<String,Object> dict) {
		TreeMap<String, Object> map = new TreeMap<String, Object>();
		if(dict != null) {
			for(Map.Entry<String, Object> me : dict.entrySet()) {
				map.put(me.getKey(), me.getValue());
			}
		}
		return map;
	}
	
	public static Map<String, Object> eventProperties(Event event) {
		TreeMap<String, Object> properties = new TreeMap<String, Object>();
		String[] names = event.getPropertyNames();
		for(int i = 0; i < names.length; i++) {
			properties.put(names[i], event.getProperty(names[i]));
		}
		return properties;
	}
}

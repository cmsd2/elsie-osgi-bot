package uk.org.elsie.osgi.bot;

import java.lang.reflect.Array;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

public class EventLogger implements EventHandler {
	private Log log = LogFactory.getLog(EventLogger.class);
	
	private long eventCounter = 0;

	@Override
	public synchronized void handleEvent(Event event) {
		long currentEventNumber = eventCounter++;
		String eventNumStr = "[" + currentEventNumber + "] ";
		
		log.info(eventNumStr + event.getTopic());
		
		for(int i = 0; i < event.getPropertyNames().length; i++) {
			String name = event.getPropertyNames()[i];
			log.info("  " + eventNumStr + name + " = " + valueToString(event.getProperty(name)));
		}
	}
	
	protected String valueToString(Object value) {
		if(value == null) {
			return "null";
		} else {
			int len;
			try {
				len = Array.getLength(value);
			} catch (IllegalArgumentException e) {
				return value.toString();
			}
				
			StringBuilder sb = new StringBuilder();
			sb.append("[");
			for(int i = 0; i < len; i++) {
				if(i != 0)
					sb.append(", ");
				sb.append(Array.get(value, i).toString());
			}
			sb.append("]");
			return sb.toString();
		}
	}

}

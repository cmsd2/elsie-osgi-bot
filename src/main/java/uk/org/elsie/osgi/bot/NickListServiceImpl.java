package uk.org.elsie.osgi.bot;

import java.util.Collections;
import java.util.Dictionary;
import java.util.Map;

import org.osgi.framework.BundleContext;

public class NickListServiceImpl implements NickListService {

	private class NickEntryImpl implements NickEntry {
		public int index;
		private String name;
		
		public NickEntryImpl(int index, String name) {
			this.index = index;
			this.name = name;
		}
		
		@Override
		public String getName() {
			return name;
		}
		
	}
	
	private Map<String, Object> properties = Collections.emptyMap();
	
	public void activate(BundleContext context, Map<String, Object> properties) {
		this.properties = PropertiesUtil.propertiesAsMap(properties);
	}
	
	@SuppressWarnings("unchecked")
	public void updated(@SuppressWarnings("rawtypes") Dictionary properties) {
		this.properties = PropertiesUtil.propertiesAsMap(properties);
	}

	@Override
	public NickEntry nextNick(NickEntry object) {
		String[] nicks = getNicks();
		if(nicks.length == 0)
			throw new IllegalStateException("Must have at least one nick to try");
		int index = nicks.length;
		if(object != null)
			index = ((NickEntryImpl) object).index;
		index++;
		if(index >= nicks.length)
			index = 0;
		return new NickEntryImpl(index, nicks[index]);
	}

	public String[] getNicks() {
		Object nicksObj = properties.get(IrcEventConstants.IRC_NICKS);
		if(nicksObj == null)
			return new String[0];
		if(nicksObj instanceof String)
			return new String[] { (String) nicksObj };
		return (String[])nicksObj;
	}
}

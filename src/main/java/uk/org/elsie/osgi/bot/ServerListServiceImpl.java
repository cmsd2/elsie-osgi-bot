package uk.org.elsie.osgi.bot;

import java.util.Collections;
import java.util.Dictionary;
import java.util.Map;

import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;

public class ServerListServiceImpl implements ManagedService, ServerListService {

	private Map<String, Object> properties = Collections.emptyMap();
	public static final String NETWORK = "irc.network";
	public static final String SERVER_LIST = "irc.servers";
	
	public void activate(Map<String, Object> properties) {
		this.properties = PropertiesUtil.propertiesAsMap(properties);
	}
	
	public void deactivate() {
	}

	@SuppressWarnings("unchecked")
	@Override
	public void updated(@SuppressWarnings("rawtypes") Dictionary properties) throws ConfigurationException {
		this.properties = PropertiesUtil.propertiesAsMap(properties);
	}
	
	public String[] getServers() {
		Object servers = properties.get(SERVER_LIST);
		if(servers == null)
			return new String[0];
		if(servers instanceof String)
			return new String[] { (String) servers };
		if(servers instanceof String[])
			return (String[])servers;
		throw new IllegalStateException("Can't convert " + servers + " to server list");
	}

	@Override
	public String getNetwork() {
		return (String) properties.get(NETWORK);
	}

	@Override
	public ServerEntry nextServer(ServerEntry entry) {
		if(entry == null)
			return new ServerEntryImpl(0);
		else {
			return ((ServerEntryImpl)entry).next();
		}
	}

	class ServerEntryImpl implements ServerEntry {
		int index;
		String hostname;
		int port;
		public ServerEntryImpl(int index) {
			this.index = index;
			if(index >= getServers().length) {
				throw new IllegalArgumentException();
			}
			String server = getServers()[index];
			String[] parts = server.split(":");
			hostname = parts[0];
			if(parts.length > 1)
				port = Integer.parseInt(parts[1]);
			else
				port = 6667;
		}
		public ServerEntryImpl next() {
			int nextIndex = index + 1;
			if(nextIndex >= getServers().length)
				nextIndex = 0;
			return new ServerEntryImpl(nextIndex);
		}
		@Override
		public String getHostname() {
			return hostname;
		}
		@Override
		public int getPort() {
			return port;
		}
	}
}

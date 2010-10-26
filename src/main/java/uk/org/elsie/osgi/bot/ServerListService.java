package uk.org.elsie.osgi.bot;

public interface ServerListService {
	
	public String getNetwork();

	public interface ServerEntry {
		public String getHostname();
		public int getPort();
	}
	
	public ServerEntry nextServer(ServerEntry entry);
}

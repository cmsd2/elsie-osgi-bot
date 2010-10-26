package uk.org.elsie.osgi.bot;

public interface ConnectionService {
	public static final String NETWORK = "irc.network";
	public static final String SERVER = "irc.server";

	public void postCommand(String command);
	public boolean isConnected();
	public void nextServer();
	public void reconnect(String reason);
}

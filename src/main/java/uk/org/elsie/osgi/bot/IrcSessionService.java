package uk.org.elsie.osgi.bot;

public interface IrcSessionService {
	public String getNick();

	void privMsg(String target, String message);

	public boolean isRegistered();
}

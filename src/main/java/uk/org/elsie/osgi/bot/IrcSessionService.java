package uk.org.elsie.osgi.bot;

import org.osgi.service.event.Event;

public interface IrcSessionService {
	public String getNick();

	void privMsg(String target, String message);
	
	void reply(Event event, String message);

	public boolean isRegistered();
}

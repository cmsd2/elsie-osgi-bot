package uk.org.elsie.osgi.bot;

import java.util.Map;

import org.osgi.service.event.Event;

public class ErrorIrcEventHandlerImpl extends AbstractIrcEventHandler {

	protected ConnectionService irc;
	
	protected void setConnectionService(ConnectionService irc) {
		this.irc = irc;
	}
	
	protected void unsetConnectionService(ConnectionService irc) {
		this.irc = null;
	}
	
	private int errorCount = 0;

	@Override
	public void handleEvent(Event event) {
		if(eventAdmin == null || ircProtocol == null)
			return;

		Map<String, Object> inputProperties = PropertiesUtil.eventProperties(event);
		IRCMessage command = new IRCMessage(inputProperties);
		
		if(command.getCommand() == null)
			return;
		
		if(command.getCommand().equalsIgnoreCase("PING")) {
			resetErrorCount();
		} else if(command.getCommand().equalsIgnoreCase("ERROR")) {
			incrementErrorCount();
		}
	}
	
	protected void resetErrorCount() {
		errorCount = 0;
	}
	
	protected void incrementErrorCount() {
		errorCount++;
		if(errorCount > 1) {
			irc.nextServer();
		}
		irc.reconnect("too many irc errors");
	}

}

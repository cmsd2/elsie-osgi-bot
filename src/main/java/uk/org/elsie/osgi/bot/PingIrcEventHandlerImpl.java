package uk.org.elsie.osgi.bot;

import java.util.Map;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

public class PingIrcEventHandlerImpl extends AbstractIrcEventHandler implements EventHandler {

	protected ConnectionService irc;
	
	protected void setConnection(ConnectionService irc) {
		this.irc = irc;
	}
	
	protected void unsetConnection(ConnectionService irc) {
		this.irc = null;
	}

	@Override
	public void handleEvent(Event pingEvent) {
		if(eventAdmin == null || ircProtocol == null || !pingEvent.getTopic().equals(IrcEventConstants.IRC_MESSAGE_TOPIC))
			return;

		Map<String, Object> inputProperties = PropertiesUtil.eventProperties(pingEvent);
		IRCMessage command = new IRCMessage(inputProperties);
		
		if(command.getCommand() == null || !command.getCommand().equalsIgnoreCase("PING"))
			return;

		String input;
		if (command.getEscapedParams() != null && command.getEscapedParams().matches("[0-9A-Z]*")) {
			input = command.getEscapedParams();
		} else {
			input = ircProtocol.getHostname();
		}
		String reply = ircProtocol.pong(input);
		
		if(irc != null) {
			irc.postCommand(reply);
		}
	}

}

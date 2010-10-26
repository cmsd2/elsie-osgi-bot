package uk.org.elsie.osgi.bot;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

public class SimpleCtcpEventHandlerImpl extends AbstractIrcEventHandler implements EventHandler {

	protected ConnectionService irc;
	private DateFormat df = DateFormat.getDateTimeInstance(DateFormat.FULL,DateFormat.LONG,Locale.US);

	protected void setConnection(ConnectionService irc) {
		this.irc = irc;
	}
	
	protected void unsetConnection(ConnectionService irc) {
		this.irc = null;
	}

	@Override
	public void handleEvent(Event pingEvent) {
		if(eventAdmin == null || ircProtocol == null)
			return;

		Map<String, Object> inputProperties = PropertiesUtil.eventProperties(pingEvent);
		IRCMessage command = new IRCMessage(inputProperties);
		
		if (command.getCommand().equalsIgnoreCase("CTCP_VERSION")) {
			irc.postCommand(ircProtocol.ctcpVersion(command.getPrefixNick(), ircProtocol.getDescription(), ircProtocol.getVersion(), ircProtocol.getEnvironment()));
		}
		else if (command.getCommand().equalsIgnoreCase("CTCP_PING")) {
			irc.postCommand(ircProtocol.ctcpPing(command.getPrefixNick(), command.getEscapedParams()));
		}
		else if (command.getCommand().equalsIgnoreCase("CTCP_TIME")) {
			String dateTime = df.format(new Date(System.currentTimeMillis()));
			irc.postCommand(ircProtocol.ctcpTime(command.getPrefixNick(), dateTime));
		}
	}

}

package uk.org.elsie.osgi.bot;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

public class IrcProtocolEventHandler extends AbstractIrcEventHandler implements EventHandler {

	ServiceReference self;
	ConnectionService connection;
	String myNick = null;
	
	public void setConnection(ConnectionService connection) {
		this.connection = connection;
	}
	
	public void unsetConnection(ConnectionService connection) {
		this.connection = null;
	}

	@Override
	public void activate(BundleContext bundleContext,
			ComponentContext componentContext, Map<String, Object> properties) throws Exception {
		super.activate(bundleContext, componentContext, properties);

		if(componentContext != null)
			self = componentContext.getServiceReference();
	}

	@Override
	public void deactivate(BundleContext bundleContext,
			ComponentContext componentContext, Map<String, Object> properties) throws Exception {
		super.deactivate(bundleContext, componentContext, properties);
		this.self = null;
	}

	@Override
	public void handleEvent(Event event) {
		if(event.getTopic().equals(IrcEventConstants.IRC_RAW_MESSAGE_TOPIC)) {
			String input = (String) event.getProperty(IrcEventConstants.RAW_MESSAGE);
			List<IRCMessage> msgs = this.ircProtocol.parse(input, myNick);
			
			for(IRCMessage msg : msgs) {
				sendMessageEvents(event, msg);
			}
		} else if(event.getTopic().equals(IrcEventConstants.IRC_NICK_CHANGING_TOPIC)) {
			myNick = (String) event.getProperty(IrcEventConstants.IRC_NICK);
		}
	}
	
	public void sendMessageEvents(Event origEvent, IRCMessage msg) {
		sendIrcMessageEvent(origEvent, msg);

		if(msg.getCommand().equalsIgnoreCase("PRIVMSG")) {
			sendIrcPrivmsgEvent(origEvent, msg);
		}
	}
	
	public void sendIrcPrivmsgEvent(Event origEvent, IRCMessage msg) {
		if(eventAdmin == null)
			return;

		Map<String, Object> properties = new TreeMap<String, Object>();
		properties.putAll(PropertiesUtil.eventProperties(origEvent));
		properties.put(EventConstants.SERVICE, self);
		properties.put(IrcEventConstants.IRC_MESSAGE, msg);
		if(msg.getParams() != null && msg.getParams().length > 0 && !msg.isPrivate()) {
			properties.put(IrcEventConstants.RETURN_TARGET, msg.getParams()[0]);
		} else {
			properties.put(IrcEventConstants.RETURN_TARGET, msg.getPrefixNick());
		}
		properties.putAll(msg.getProperties());
		
		Event event = new Event(IrcEventConstants.IRC_PRIVMSG_TOPIC, properties);
		eventAdmin.postEvent(event);
	}
	
	public void sendIrcMessageEvent(Event origEvent, IRCMessage msg) {
		if(eventAdmin == null)
			return;

		Map<String, Object> properties = new TreeMap<String, Object>();
		properties.putAll(PropertiesUtil.eventProperties(origEvent));
		properties.put(EventConstants.SERVICE, self);
		properties.put(IrcEventConstants.IRC_MESSAGE, msg);
		properties.putAll(msg.getProperties());
		
		Event event = new Event(IrcEventConstants.IRC_MESSAGE_TOPIC, properties);
		eventAdmin.postEvent(event);
	}

}

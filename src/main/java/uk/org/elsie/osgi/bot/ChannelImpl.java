package uk.org.elsie.osgi.bot;

/**
 * @author sffubs
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */

/*
 * The job of the Channel object is to
 *  - join a channel
 *  - maintain the public userStatus hashtable containing
 *    the ident and status of each nick (User objects for each nick)
 *  - send ChanEvent events (events relevant to the channel)
 *  - send ChanBotEvent events (bot commands issued on this channel)
 *  - send ChanBotEvent to ChanBotUnknownCmdListener objects if nothing
 *    responds to a ChanBotEvent.
 * 
 * A Channel must be registered with the bot as a Channel to ensure auto-joining
 * on connect and reconnect.
 * 
 * Note that a PRIVATE command is considered relevant to the channel if
 * the user that issued it is on that channel. This can lead to multiple
 * channels responding to the same PRIVATE bot command.
 */

import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.TreeMap;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentFactory;
import org.osgi.service.component.ComponentInstance;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventHandler;

public class ChannelImpl implements EventHandler, Channel {
	private ConnectionService bot;
	private Map<String, Object> properties = Collections.emptyMap();

	private IRCProtocol irc;
	private EventAdmin eventAdmin;
	private String nick;
	private Dictionary<String, Object> nameServiceProps;
	private ComponentFactory nameServiceFactory;
	private ComponentInstance nameService;
	private boolean joined = false;
	
	public ChannelImpl() {
		irc = new IRCProtocol();
	}
	
	public void activate(BundleContext context, Map<String, Object> properties) {
		this.properties = PropertiesUtil.propertiesAsMap(properties);
		
		nameServiceProps = new Hashtable<String, Object>();
		nameServiceProps.put(IrcEventConstants.IRC_CHANNEL, this.getChannel());

		if(nameServiceFactory != null)
			createNameService();
	}
	
	public void deactivate(BundleContext context) {
		destroyNameService();
	}
	
	protected void createNameService() {
		nameService = nameServiceFactory.newInstance(nameServiceProps);
	}
	
	protected void destroyNameService() {
		if(nameService != null) {
			nameService.dispose();
			nameService = null;
		}
	}

	protected boolean stringEquals(String a, String b) {
		return a == b || (a != null && a.equals(b));
	}
	
	protected boolean stringEqualsIgnoreCase(String a, String b) {
		return a == b || (a != null && a.equalsIgnoreCase(b));
	}
	
	protected boolean stringMatches(String a, String r) {
		return a != null && a.matches(r);
	}
	
	public void setNameServiceFactory(ComponentFactory factory) {
		this.nameServiceFactory = factory;
		if(this.nameServiceProps != null) {
			createNameService();
		}
	}
	
	public void unsetNameServiceFactory(ComponentFactory factory) {
		this.nameServiceFactory = null;
		destroyNameService();
	}
	
	public void setConnection(ConnectionService connection) {
		this.bot = connection;
	}
	
	public void unsetConnection(ConnectionService connection) {
		this.bot = null;
	}
	
	public void setEventAdmin(EventAdmin eventAdmin) {
		this.eventAdmin = eventAdmin;
	}
	
	public void unsetEventAdmin(EventAdmin eventAdmin) {
		this.eventAdmin = null;
	}
	
	protected void sendChanEvent(Event origEvent, IRCMessage msg) {
		Map<String,Object> properties = new TreeMap<String, Object>();
		properties.putAll(PropertiesUtil.eventProperties(origEvent));
		properties.putAll(msg.getProperties());
		properties.put(IrcEventConstants.IRC_CHANNEL, getChannel());
		Event event = new Event(IrcEventConstants.IRC_CHANNEL_MESSAGE_TOPIC, properties);
		eventAdmin.postEvent(event);
	}
	
	public void handleEvent(Event event) {
		if(event.getTopic().equals(IrcEventConstants.IRC_NICK_CHANGING_TOPIC)) {
			this.nick = (String) event.getProperty(IrcEventConstants.IRC_NICK);
			return;
		} else if(!event.getTopic().equals(IrcEventConstants.IRC_MESSAGE_TOPIC)) {
			return;
		}
		
		IRCMessage msg = (IRCMessage) event.getProperty(IrcEventConstants.IRC_MESSAGE);
		if(msg == null)
			return;

		if ((msg.getCommand().equalsIgnoreCase("NICK")
			|| msg.getCommand().equalsIgnoreCase("QUIT")) ) {
			//&& userStatus.containsKey(msg.getPrefixNick())
				sendChanEvent(event, msg);
		}
		
		if(msg.getParams() != null) {
			if (msg.getParams().length >= 3) {
				if (msg.getCommand().equalsIgnoreCase("353") & stringEqualsIgnoreCase(msg.getParams()[2], getChannel())) {
					sendChanEvent(event, msg);
				}
			} else if (msg.getParams().length >= 2) {
				if (msg.getCommand().equals("311")) {
					// && userStatus.containsKey(msg.getParams()[1])
					sendChanEvent(event, msg);
				}
			} else if (msg.getParams().length >= 1) {
				if ((msg.getCommand().equalsIgnoreCase("MODE")
						|| msg.getCommand().equalsIgnoreCase("PRIVMSG")
						|| msg.getCommand().equalsIgnoreCase("TOPIC")
						|| msg.getCommand().equalsIgnoreCase("KICK")
						|| msg.getCommand().equalsIgnoreCase("CTCP_ACTION"))
						&&
						stringEqualsIgnoreCase(msg.getParams()[0], getChannel())
					) {
			
					sendChanEvent(event, msg);
				}
				else if ((msg.getCommand().equalsIgnoreCase("JOIN")
						|| msg.getCommand().equalsIgnoreCase("PART"))	
						&&
						(stringEqualsIgnoreCase(msg.getParams()[0], getChannel())
								|| stringEqualsIgnoreCase(msg.getEscapedParams(), getChannel()))
						) {
				
					sendChanEvent(event, msg);
				}
			}
		} else if(msg.getEscapedParams() != null) {
			if ((msg.getCommand().equalsIgnoreCase("JOIN")
					|| msg.getCommand().equalsIgnoreCase("PART"))	
					&&
					msg.getEscapedParams().equalsIgnoreCase(getChannel())) {
			
				sendChanEvent(event, msg);
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see uk.org.elsie.osgi.bot.Channel#getNick()
	 */
	@Override
	public String getNick() {
		return nick;
	}

	/* (non-Javadoc)
	 * @see uk.org.elsie.osgi.bot.Channel#getChannel()
	 */
	@Override
	public String getChannel() {
		return (String) properties.get(IrcEventConstants.IRC_CHANNEL);
	}
	
	/* (non-Javadoc)
	 * @see uk.org.elsie.osgi.bot.Channel#join()
	 */
	@Override
	public void join() {
		this.joined = true;
		bot.postCommand(irc.join(getChannel()));
	}
	
	/* (non-Javadoc)
	 * @see uk.org.elsie.osgi.bot.Channel#part()
	 */
	@Override
	public void part() {
		if(joined) {
			bot.postCommand(irc.part(getChannel()));
			joined = false;
		}
	}
	
	public boolean isJoined() {
		return joined;
	}
}

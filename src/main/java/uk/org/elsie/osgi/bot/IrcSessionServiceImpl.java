package uk.org.elsie.osgi.bot;

import java.util.Collections;
import java.util.Dictionary;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;

import uk.org.elsie.osgi.bot.NickListService.NickEntry;

public class IrcSessionServiceImpl extends AbstractIrcEventHandler implements IrcSessionService {

	private static Log log = LogFactory.getLog(IrcSessionServiceImpl.class);

	private ConnectionService connection;
	private NickListService nickList;
	private NickListService.NickEntry currentNick;
	private Filter nickErrorFilter;
	private Filter endMotdFilter;
	private Filter notRegisteredErrorFilter;
	private boolean registeringFirstTime = false;
	private boolean awaitingSuccess = false;
	private boolean registered = false;
	private Map<String, Object> properties = Collections.emptyMap();
	
	@Override
	public void activate(BundleContext bundleContext,
			ComponentContext componentContext, Map<String, Object> properties) throws Exception {
		super.activate(bundleContext, componentContext, properties);
		this.properties = PropertiesUtil.propertiesAsMap(properties);

		try {
			nickErrorFilter = bundleContext.createFilter("(|(irc.msg.command=432)(irc.msg.command=433)(irc.msg.command=436))");
			endMotdFilter = bundleContext.createFilter("(irc.msg.command=376)");
			notRegisteredErrorFilter = bundleContext.createFilter("(irc.msg.command=451)");
		} catch (InvalidSyntaxException e) {
			log.error("failed to create filter", e);
		}
		
		if(connection != null && connection.isConnected()) {
			log.info("activated session service. now registering.");
			registeringFirstTime = true;
			if(currentNick == null)
				currentNick = nickList.nextNick(null);
			registerNick(null, currentNick);
		}
	}
	
	@SuppressWarnings("unchecked")
	public void updated(@SuppressWarnings("rawtypes") Dictionary properties) {
		this.properties = PropertiesUtil.propertiesAsMap(properties);
	}
	
	public String getNick() {
		if(currentNick != null)
			return currentNick.getName();
		else
			return null;
	}

	public void setConnection(ConnectionService connection) {
		this.connection = connection;
	}
	
	public void unsetConnection(ConnectionService connection) {
		this.connection = null;
	}
	
	public void setNickList(NickListService nickList) {
		this.nickList = nickList;
	}
	
	public void unsetNickList(NickListService nickList) {
		this.nickList = null;
	}

	@Override
	public synchronized void handleEvent(Event event) {
		if(event.getTopic().equals(IrcEventConstants.IRC_CONNECTED_TOPIC)) {
			log.info("connected. now registering.");
			registeringFirstTime = true;
			registered = false;
			if(currentNick == null)
				currentNick = nickList.nextNick(null);
			sendNickEvent(event, IrcEventConstants.IRC_UNREGISTERED_TOPIC, currentNick.getName());
			registerNick(event, currentNick);
		} else if(event.getTopic().equals(IrcEventConstants.IRC_MESSAGE_TOPIC)) {
			if(event.matches(nickErrorFilter)) {
				log.info("message matches nick error filter");
				sendNickEvent(event, IrcEventConstants.IRC_UNREGISTERED_TOPIC, currentNick.getName());
				if(nickList != null) {
					registerNick(event, nickList.nextNick(currentNick));
				}
			} else if(event.matches(notRegisteredErrorFilter)) {
				log.info("event matches not-registered error filter");
				sendNickEvent(event, IrcEventConstants.IRC_UNREGISTERED_TOPIC, currentNick.getName());
				registered = false;
				if(nickList != null) {
					if(currentNick == null)
						currentNick = nickList.nextNick(null);
					registerNick(event, currentNick);
				}
			} else if(event.matches(endMotdFilter)) {
				if(registeringFirstTime) {
					log.info("now successfully registered");
					registeringFirstTime = false;
					registered = true;
					sendNickEvent(event, IrcEventConstants.IRC_REGISTERED_TOPIC, currentNick.getName());
				}
			} else {
				if(awaitingSuccess && !registeringFirstTime) {
					log.info("last command wasn't a nick error. nick must be registered successfully");
					awaitingSuccess = false;
					sendNickEvent(event, IrcEventConstants.IRC_NICK_CHANGED_TOPIC, currentNick.getName());
				}
			}
		}
	}
	
	public synchronized boolean isRegistered() {
		return registered;
	}
	
	protected synchronized void registerNick(Event event, NickEntry newNick) {
		log.info("registering nick " + newNick.getName());
		awaitingSuccess = true;
		currentNick = newNick;
		
		if(currentNick == null && nickList != null) {
			currentNick = nickList.nextNick(null);
		}

		if(currentNick != null) {
			String newNickName = currentNick.getName();
			sendNickEvent(event, IrcEventConstants.IRC_NICK_CHANGING_TOPIC, newNickName);
			connection.postCommand(this.ircProtocol.nick(newNickName));
			if(registeringFirstTime)
				connection.postCommand(this.ircProtocol.user(newNickName, getMode(), getRealName()));
		}
	}
	
	public int getMode() {
		return (Integer) properties.get(IrcEventConstants.IRC_USER_MODE);
	}
	
	public String getRealName() {
		return (String) properties.get(IrcEventConstants.IRC_USER_REAL_NAME);
	}
	
	protected void sendNickEvent(Event origEvent, String topic, String newNick) {
		Map<String, Object> properties = new TreeMap<String, Object>();
		if(origEvent != null)
			properties.putAll(PropertiesUtil.eventProperties(origEvent));
		properties.put(IrcEventConstants.IRC_NICK, newNick);
		Event event = new Event(topic, properties);
		eventAdmin.sendEvent(event);
	}

	@Override
	public void privMsg(String target, String message) {
		if (this.connection == null)
			throw new IllegalStateException();

		String string = ircProtocol.privmsg(target, message);
		String nextString;
		int i;
		final int limit = 510 - (target.length()
				+ this.getNick().length()
				+ ircProtocol.getHostname().length() + 15);

		while (string.length() > limit) {
			i = string.lastIndexOf(" ", limit - 1);
			if (i <= limit - 50) {
				nextString = string.substring(limit);
				string = string.substring(0, limit);
			} else {
				nextString = string.substring(i + 1);
				string = string.substring(0, i);
			}
			this.connection.postCommand(string + "\n");
			string = ircProtocol.privmsg(target, nextString);
		}

		this.connection.postCommand(string);
	}
	
	public void reply(Event event, String message) {
		String target = (String)event.getProperty(IrcEventConstants.RETURN_TARGET);
		privMsg(target, message);
	}
}

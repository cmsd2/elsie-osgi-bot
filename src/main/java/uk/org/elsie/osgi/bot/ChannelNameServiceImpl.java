package uk.org.elsie.osgi.bot;

import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

public class ChannelNameServiceImpl implements ChannelNameService {

	public class UserInfo {
		public String ident;
		public String status;
		public UserInfo() { }
		public UserInfo(String ident, String status) {
			this.ident = ident;
			this.status = status;
		}
		
		public String toString() {
			return "ident: " + ident + " status: " + status;
		}
	}
	
	private static Log log = LogFactory.getLog(ChannelNameServiceImpl.class);

	private ConnectionService bot;
	private IRCProtocol irc = new IRCProtocol();
	private Map<String, UserInfo> userStatus = new TreeMap<String, UserInfo>();
	private EventAdmin eventAdmin;
	private ServiceRegistration eventHandlerRegistration;
	private Map<String, Object> properties;

	public void activate(BundleContext bundleContext, Map<String, Object> properties) {
		this.properties = PropertiesUtil.propertiesAsMap(properties);
		Dictionary<String, Object> eventHandlerProps = new Hashtable<String, Object>();
		eventHandlerProps.put(EventConstants.EVENT_TOPIC, IrcEventConstants.IRC_CHANNEL_MESSAGE_TOPIC);
		eventHandlerProps.put(EventConstants.EVENT_FILTER, "(" + IrcEventConstants.IRC_CHANNEL + "=" + getChannelName() + ")");
		eventHandlerRegistration = bundleContext.registerService(EventHandler.class.getName(), new EventHandler() {
			@Override
			public void handleEvent(Event event) {
				ChannelNameServiceImpl.this.handleEvent(event);
			}
		}, eventHandlerProps);
	}
	
	public void deactivate(BundleContext bundleContext) {
		eventHandlerRegistration.unregister();
	}

	public void setEventAdmin(EventAdmin eventAdmin) {
		this.eventAdmin = eventAdmin;
	}
	
	public void unsetEventAdmin(EventAdmin eventAdmin) {
		this.eventAdmin = null;
	}
	
	/* (non-Javadoc)
	 * @see uk.org.elsie.osgi.bot.ChannelNameService#getChannelName()
	 */
	@Override
	public String getChannelName() {
		return (String) properties.get(IrcEventConstants.IRC_CHANNEL);
	}

	public void updateIdent(String user, String ident) {
		if (userStatus.containsKey(user) == true) {
			UserInfo temp = (UserInfo)userStatus.get(user);
			if (ident.compareTo(temp.ident) != 0) {
				UserInfo newIdent = new UserInfo(ident,temp.status);
				log.info("updating ident for user: " + user + " old: " + temp + " new: " + newIdent);
				userStatus.remove(user);
				userStatus.put(user, newIdent);
			}
		}
	}
	
	public void setConnection(ConnectionService bot) {
		this.bot = bot;
	}
	
	public void unsetConnection(ConnectionService bot) {
		this.bot = null;
	}
	
	/* (non-Javadoc)
	 * @see uk.org.elsie.osgi.bot.ChannelNameService#rehash()
	 */
	@Override
	public void rehash() {
		log.info("rehashing " + getChannelName());
		userStatus.clear();
		bot.postCommand(irc.names(getChannelName()));
	}

	/* (non-Javadoc)
	 * @see uk.org.elsie.osgi.bot.ChannelNameService#getUserStatus(java.lang.String)
	 */
	@Override
	public UserInfo getUserStatus(String user) {
		if (userStatus.containsKey(user)) {
			return (UserInfo)userStatus.get(user);
		}
		else {
			return null;
		}
	}
	
	/* (non-Javadoc)
	 * @see uk.org.elsie.osgi.bot.ChannelNameService#getNumUsers()
	 */
	@Override
	public int getNumUsers() {
		return userStatus.size();
	}
	
	/* (non-Javadoc)
	 * @see uk.org.elsie.osgi.bot.ChannelNameService#getUsers()
	 */
	@Override
	public Collection<UserInfo> getUsers() {
		return userStatus.values();
	}

	public void handleEvent(Event event) {
		IRCMessage command = (IRCMessage) event.getProperty(IrcEventConstants.IRC_MESSAGE);
		
		if (command.getCommand().equals("353")) {		//Names command; update user hashtable with current status.
			UserInfo temp = new UserInfo();
			String[] names = command.getEscapedParams().split("[ ]");
			String name;
			String status;

			for (int i = 0; i < names.length; i++) {
				status = null;
				status = names[i].substring(0,1);
				name = names[i].replaceFirst("[+|@]","");
				if (userStatus.containsKey(name) == false) {
					UserInfo newStatus = new UserInfo("",status);
					log.info("353: adding new name: " + name + " new: " + newStatus);
					userStatus.put(name, newStatus);
				}
				else {
					temp = (UserInfo)userStatus.get(name);
					if (temp.status.compareTo(status) != 0) {
						UserInfo newStatus = new UserInfo(temp.ident,status);
						log.info("353: updating name: " + name + " old: " + temp + " new: " + newStatus);
						userStatus.remove(name);
						userStatus.put(name, newStatus);
					}
				}
			}
		}
		else if (command.getCommand().equalsIgnoreCase("MODE")) {
			bot.postCommand(irc.names(getChannelName()));
			if (userStatus.containsKey(command.getPrefixNick())) {
				updateIdent(command.getPrefixNick(), command.getIdent());
			}
		}
		else if (command.getCommand().equalsIgnoreCase("PRIVMSG")) {
			if (userStatus.containsKey(command.getPrefixNick()) == true) {
				updateIdent(command.getPrefixNick(), command.getIdent());
			}
		}
		else if (command.getCommand().equals("311") && command.getParams() != null) {
			String user = command.getParams()[1];
			String ident = command.getParams()[2] + "@" + command.getParams()[3];

			updateIdent(user,ident);
		}
		else if (command.getCommand().equalsIgnoreCase("JOIN")) {
			if (userStatus.containsKey(command.getPrefixNick()) == false) {
				UserInfo newUser = new UserInfo(command.getIdent(),command.getPrefixNick().substring(1,2));
				log.info("user " + command.getPrefixNick() + " joined. info: " + newUser);
				userStatus.put(command.getPrefixNick(), newUser);
			}
			else {
				sendErrorEvent(event, "Channel.ircRespond(JOIN)", "Hashtable corrupted - resetting", null);
				UserInfo newUser = new UserInfo(command.getIdent(),command.getPrefixNick().substring(1,2));
				log.info("user " + command.getPrefixNick() + " joined but user already in users hash. info: " + newUser);
				rehash();
				userStatus.put(command.getPrefixNick(), newUser);
			}
		}
		else if (command.getCommand().equalsIgnoreCase("PART")) {
			String nick = command.getPrefixNick();
			if (userStatus.containsKey(nick) == true) {
				UserInfo oldUser = userStatus.get(nick);
				log.info("user " + nick + " parted. info: " + oldUser);
				userStatus.remove(nick);
			}
		}
		else if (command.getCommand().equalsIgnoreCase("QUIT")) {
			String nick = command.getPrefixNick();
			if (userStatus.containsKey(nick) == true) {
				UserInfo oldUser = userStatus.get(nick);
				log.info("user " + nick + " quit. info: " + oldUser);
				userStatus.remove(nick);
			}
		}
		else if (command.getCommand().equalsIgnoreCase("KICK")) {
			String nick = command.getParams()[1];
			if (userStatus.containsKey(nick) == true) {
				UserInfo oldUser = userStatus.get(nick);
				log.info("user " + nick + " was kicked. info: " + oldUser);
				userStatus.remove(nick);
			}
		}
		else if (command.getCommand().equalsIgnoreCase("NICK")) {
			String oldNick = command.getPrefixNick();
			UserInfo temp = (UserInfo)userStatus.get(oldNick);
			String newNick = command.getEscapedParams();
			if (temp != null) {
				UserInfo newUserInfo = new UserInfo(command.getIdent(), temp.status);
				log.info("user " + oldNick + " has changed nick to " + newNick + ". info: " + newUserInfo);
				userStatus.remove(oldNick);
				userStatus.put(newNick, newUserInfo);
			}
			else {
				log.info("user " + oldNick + " has changed nick to " + newNick + " but isn't in the users hash.");
				rehash();
				sendErrorEvent(event, "Channel.ircRespond(NICK)", "Hashtable corrupted - resetting", null);
			}
		}
	}
	
	protected void sendErrorEvent(Event origEvent, String src, String msg, Throwable err) {
		if(eventAdmin == null)
			return;

		Map<String, Object> properties = new TreeMap<String, Object>();
		properties.put(EventConstants.MESSAGE, src + ": " + msg);
		if(err != null) {
			properties.put(EventConstants.EXCEPTION, err);
			properties.put(EventConstants.EXCEPTION_CLASS, err.getClass().getName());
			properties.put(EventConstants.EXCEPTION_MESSAGE, err.getMessage());
		}
		
		Event event = new Event(IrcEventConstants.ERROR_TOPIC, properties);
		eventAdmin.postEvent(event);
	}

}

package uk.org.elsie.osgi.bot;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventHandler;

public class ChannelAdminImpl implements EventHandler {
	private static Log log = LogFactory.getLog(ChannelAdminImpl.class);

	@SuppressWarnings("unused")
	private Map<String,Object> properties = new TreeMap<String, Object>();

	private Map<String, Channel> channels = new HashMap<String, Channel>();
	private EventAdmin eventAdmin;
	private boolean registered = false;
	private IrcSessionService session;
	
	public void addChannel(Channel channel) {
		log.info("adding channel " + channel);
		channels.put(channel.getChannel(), channel);
		join(channel);
	}
	
	public void removeChannel(Channel channel) {
		log.info("removing channel " + channel);
		part(channel);
		channels.remove(channel.getChannel());
	}
	
	protected void setRegistered(boolean reg) {
		if(this.registered != reg) {
			log.info("registered = " + reg);
			this.registered = reg;
			onFlagChanged();
		}
	}

	private void onFlagChanged() {
		if(this.registered)
			joinAll();
		else
			partAll();
	}

	protected void joinAll() {
		log.info("joinAll");
		for(Channel c : channels.values()) {
			join(c);
		}
	}
	
	protected void partAll() {
		log.info("partAll");
		for(Channel c : channels.values()) {
			part(c);
		}
	}
	
	public void setSession(IrcSessionService session) {
		this.session = session;
	}
	
	public void unsetSession(IrcSessionService session) {
		this.session = null;
	}
	
	public void setEventAdmin(EventAdmin eventAdmin) {
		this.eventAdmin = eventAdmin;
	}
	
	public void unsetEventAdmin(EventAdmin eventAdmin) {
		this.eventAdmin = null;
	}
	
	@SuppressWarnings("unchecked")
	public synchronized void activate(BundleContext bundleContext, @SuppressWarnings("rawtypes") Map properties) {
		log.info("Activating");
		this.properties = PropertiesUtil.publicPropertiesAsMap(properties);
		setRegistered(session.isRegistered());
	}
	
	public void deactivate(ComponentContext context) {
		log.info("Deactivating");
	}

	@SuppressWarnings("unchecked")
	public void updated(@SuppressWarnings("rawtypes") Dictionary properties) throws ConfigurationException {
		this.properties = PropertiesUtil.publicPropertiesAsMap(properties);
	}
	
	protected void join(Channel channel) {
		if(registered) {
			log.info("joining " + channel);
			channel.join();
			sendChannelEvent(IrcEventConstants.IRC_CHANNEL_JOINED_TOPIC, channel);
		}
	}
	
	protected void part(Channel channel) {
		if(registered) {
			log.info("parting " + channel);
			channel.part();
			sendChannelEvent(IrcEventConstants.IRC_CHANNEL_PARTED_TOPIC, channel);
		}
	}

	@Override
	public synchronized void handleEvent(Event event) {
		if(event.getTopic().equals(IrcEventConstants.IRC_REGISTERED_TOPIC)) {
			setRegistered(true);
		} else if(event.getTopic().equals(IrcEventConstants.IRC_UNREGISTERED_TOPIC)) {
			setRegistered(false);
		}
	}
	
	protected void sendChannelEvent(String topic, Channel channel) {
		Map<String, Object> properties = new TreeMap<String, Object>();
		properties.put(IrcEventConstants.IRC_CHANNEL, channel.getChannel());
		Event event = new Event(topic, properties);
		eventAdmin.sendEvent(event);
	}
}

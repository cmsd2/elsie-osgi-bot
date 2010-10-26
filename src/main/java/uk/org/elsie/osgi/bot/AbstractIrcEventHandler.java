package uk.org.elsie.osgi.bot;

import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventHandler;

public abstract class AbstractIrcEventHandler implements EventHandler {
	protected BundleContext bundleContext;
	protected IRCProtocol ircProtocol = new IRCProtocol();
	protected EventAdmin eventAdmin;
	
	public void setIrcProtocol(IRCProtocol ircProtocol) {
		this.ircProtocol = ircProtocol;
	}
	
	public void unsetIrcProtocol(IRCProtocol ircProtocol) {
		this.ircProtocol = null;
	}
	
	public void setEventAdmin(EventAdmin eventAdmin) {
		this.eventAdmin = eventAdmin;
	}
	
	public void unsetEventAdmin(EventAdmin eventAdmin) {
		this.eventAdmin = null;
	}

	public void activate(BundleContext bundleContext, ComponentContext componentContext, Map<String, Object> properties) throws Exception {
		this.bundleContext = bundleContext;
	}
	
	public void deactivate(BundleContext bundleContext, ComponentContext componentContext, Map<String, Object> properties) throws Exception {
		this.bundleContext = null;
	}
}

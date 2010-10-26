package uk.org.elsie.osgi.bot;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author sffubs
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class IRCMessage implements Cloneable {

	private Map<String, Object> properties = new TreeMap<String, Object>();
	
	public IRCMessage(String command, String prefix, String[] params, String
		escapedParams, String prefixNick, String ident, boolean isPrivate) {
		
		properties.put(IrcEventConstants.COMMAND, command);
		properties.put(IrcEventConstants.PREFIX, prefix);
		properties.put(IrcEventConstants.PARAMS, params);
		properties.put(IrcEventConstants.ESCAPED_PARAMS, escapedParams);
		properties.put(IrcEventConstants.PREFIX_NICK, prefixNick);
		properties.put(IrcEventConstants.IDENT, ident);
		properties.put(IrcEventConstants.PRIVATE, isPrivate);
	}
	
	public IRCMessage(Map<String, Object> properties) {
		this.properties = PropertiesUtil.publicPropertiesAsMap(properties);
	}

	public IRCMessage() {
	}
	
	public Map<String, Object> getProperties() {
		return Collections.unmodifiableMap(properties);
	}
	
	public String getNetwork() {
		return (String) properties.get(IrcEventConstants.NETWORK);
	}
	
	public String getCommand() {
		return (String) properties.get(IrcEventConstants.COMMAND);
	}
	
	public String getPrefix() {
		return (String) properties.get(IrcEventConstants.PREFIX);
	}
	
	public String[] getParams() {
		return (String[]) properties.get(IrcEventConstants.PARAMS);
	}
	
	public String getEscapedParams() {
		return (String) properties.get(IrcEventConstants.ESCAPED_PARAMS);
	}
	
	public String getPrefixNick() {
		return (String) properties.get(IrcEventConstants.PREFIX_NICK);
	}
	
	public String getIdent() {
		return (String) properties.get(IrcEventConstants.IDENT);
	}
	
	public boolean isPrivate() {
		return (Boolean) properties.get(IrcEventConstants.PRIVATE);
	}
	
	public Object clone() {
		try {
			return super.clone();
		}
		catch (Exception e) {
			return null;
		}
	}
}

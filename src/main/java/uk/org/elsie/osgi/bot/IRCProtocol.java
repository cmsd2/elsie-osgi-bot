/**
 * @author sffubs
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */

package uk.org.elsie.osgi.bot;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IRCProtocol {
	Pattern cmdParser1;
	Pattern cmdParser2;
	Pattern cmdParser3;
	Pattern ctcpParser;

	String hostname;

	{
		try {
			InetAddress localhost = InetAddress.getLocalHost();
			hostname = localhost.getHostName();
		} catch (UnknownHostException e) {
			hostname = "localhost";
		}
	}

	public IRCProtocol() {
		cmdParser1 = Pattern.compile("^\\:([^ ]+) ([^ ]+) ([^\n]+)");			//Prefix, command, parameters
		cmdParser2 = Pattern.compile("([\\w]+) ([^\n]+)");					//Command, parameters
		cmdParser3 = Pattern.compile("([^\\:]*) ?\\:(.*)");
		ctcpParser = Pattern.compile("(.*)\001([^\001]*?)\001(.*)");
	}
	
	public String getHostname() {
		return hostname;
	}
	
	public String getDescription() {
		return "The Elsie Bot Framework";
	}
	
	public String getVersion() {
		return "0.644 2003-06-23";
	}
	
	public String getEnvironment() {
		return System.getProperty("os.name") + " "
		+ System.getProperty("os.version") + " "
		+ System.getProperty("os.arch") + " "
		+ "[Java " + System.getProperty("java.version") + "]";
	}

	public List<IRCMessage> parse(String input, String myNick) {
		List<IRCMessage> outputs = new ArrayList<IRCMessage>();
		IRCMessage output;
		String temp = null;
		String[] temp2 = null;
		
		String prefix = null;
		String prefixNick = null;
		String ident = null;
		String command;
		String[] params;
		String escapedParams;
		boolean isPrivate;

		Matcher matcher;
		boolean match;
		
		matcher = cmdParser1.matcher(input);
		match = matcher.lookingAt();

		if (match == true) {
			prefix = matcher.group(1);
			temp2 = prefix.split("!",2);
			if (temp2.length > 1) {
				prefixNick = temp2[0];
				ident = temp2[1];
			}
			else {
				prefixNick = "";
			}
			command = matcher.group(2);
			temp = matcher.group(3);
		}
		else {

			matcher = cmdParser2.matcher(input);
			match = matcher.lookingAt();

			if (match == true) {
				command = matcher.group(1);
				temp = matcher.group(2);
			} else {
				return Collections.emptyList();
			}
		}
		
		matcher = cmdParser3.matcher(temp);
		if (matcher.matches()) {
			matcher.lookingAt();
			
			params = matcher.group(1).split(" ");
			escapedParams = matcher.group(2);
		}
		else {
			params = temp.split(" ");
			escapedParams = "";
		}
		
		if (prefixNick == null) {
			prefixNick = "";
		}
		
		isPrivate = false;

		if (myNick != null && command.equalsIgnoreCase("PRIVMSG")) {
			if(params[0].equalsIgnoreCase(myNick))
				isPrivate = true;

			escapedParams = escapedParams.replaceAll("\020\020","\020");
			escapedParams = escapedParams.replaceAll("\020\060","\000");
			escapedParams = escapedParams.replaceAll("\020\156","\012");
			escapedParams = escapedParams.replaceAll("\020\162","\015");
			escapedParams = escapedParams.replaceAll("\020","");
			
			output = new IRCMessage(command, prefix, params, escapedParams, prefixNick, ident, isPrivate);
			
			matcher = ctcpParser.matcher(escapedParams);
			
			while(matcher.matches()) {
				match = matcher.lookingAt();
				
				temp = matcher.group(1) + matcher.group(3);
				escapedParams = temp;
				
				temp = matcher.group(2);
				if (temp.length() > 0) {
					temp = temp.replaceAll("\134\134\141","\001");
					temp = temp.replaceAll("\134\134\134\134","\134\134");
					
					temp2 = temp.split(" ");
					command = "CTCP_" + temp2[0];
					if (temp2.length > 1) {
						escapedParams = temp2[1];
					}
					else {
						escapedParams = "";
					}
					for(int j = 2; j < temp2.length; j++) {
						escapedParams = escapedParams + " " + temp2[j];
					}

					outputs.add(new IRCMessage(command, prefix, params, escapedParams, prefixNick, ident, isPrivate));
				}
				
				matcher = ctcpParser.matcher(escapedParams);
			}
			
			if (escapedParams.length() > 0) {
				outputs.add(0, output);
			}
		}
		else {
			output = new IRCMessage(command, prefix, params, escapedParams, prefixNick, ident, isPrivate);
			outputs.add(0,output);
		}
		
		return outputs;
	}
	
	public String nick(String nick) {
		String output = "NICK " + nick + "\n";
		return output;
	}
	public String user(String nick, int mode, String username) {
		String output = "USER " + nick + " " + mode + " * " + username + "\n";
		return output;
	}
	public String join(String channel) {
		String output = "JOIN " + channel + "\n";
		return output;
	}
	public String part(String channel) {
		String output = "PART " + channel + "\n";
		return output;
	}
	public String quit(String reason) {
		String output = "QUIT :" + reason + "\n";
		return output;
	}
	public String pong(String hostname) {
		String output = "PONG " + hostname + "\n";
		return output;
	}
	public String privmsg(String target, String message) {
		String output = "PRIVMSG " + target + " :" + message + "\n";
		return output;
	}
	public String names(String channel) {
		String output = "NAMES " + channel + "\n";
		return output;
	}
	public String op(String nick, String channel) {
		String output = "MODE " + channel + " +o " + nick + "\n";
		return output;
	}
	public String deop(String nick, String channel) {
		String output = "MODE " + channel + " -o " + nick + "\n";
		return output;
	}
	public String voice(String nick, String channel) {
		String output = "MODE " + channel + " +v " + nick + "\n";
		return output;
	}
	public String devoice(String nick, String channel) {
		String output = "MODE " + channel + " -v " + nick + "\n";
		return output;
	}
	public String whois(String nick) {
		String output = "WHOIS " + nick + "\n";
		return output;
	}
	public String kick(String nick, String channel, String reason) {
		String output = "KICK " + channel + " " + nick + " :" + reason + "\n";
		return output;
	}
	public String ban(String hostmask, String channel) {
		String output = "MODE " + channel + " +b " + hostmask + "\n";
		return output;
	}
	public String except(String hostmask, String channel) {
		String output = "MODE " + channel + " +e " + hostmask + "\n";
		return output;
	}
	public String ctcpPing(String nick, String timestamp) {
		String output = "NOTICE " + nick + " :\001PING " + timestamp + "\001\n";
		return output;
	}
	public String ctcpVersion(String nick, String name, String version, String environment) {
		String output = "NOTICE " + nick + " :\001VERSION " + name + " " + version + " " + environment + "\001\n";
		return output;
	}
	public String ctcpTime(String nick, String time) {
		String output = "NOTICE " + nick + " :\001TIME " + time + "\001\n";
		return output;
	}
	public String topic(String topic, String channel) {
		String output = "TOPIC " + channel + " :" + topic + "\n";
		return output;
	}
}

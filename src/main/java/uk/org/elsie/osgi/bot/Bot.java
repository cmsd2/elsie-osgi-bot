/**
 * @author sffubs
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */

/*
 * This is the bot.
 * Its job is to
 *  - maintain a connection to the server
 *  - respond to pings
 *  - spot nick collisions & take action
 *  - send IRCEvents
 *  - send BotEvents
 *  - manage public sender object for sending to server
 */
 
package uk.org.elsie.osgi.bot;

import java.net.Socket;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.util.Vector;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.TimerTask;
import java.util.Timer;
import java.util.Date;
import java.text.DateFormat;
import java.util.Locale;

public class Bot extends Thread {
	private final String version = "0.644 2003-06-23";
	private final String description = "The Elsie Bot Framework";
	private final String environment;
	
	private final long pauseTime = 50;		//Time to sleep before checking for new messages
	
	private Vector nicks;
	private int currentNick;
	private String myNick;
	private Vector servers;
	private int currentServer;
	private int port;
	private int mode;
	private String hostname;
	private String realname;
	private int consecutiveErrors;
	
	private Socket connection;					//Server connection object
	private BufferedReader receiver;			//Input object
	public BufferedWriter sender;				//Output object

	private IRCProtocol irc;					//IRC protocol object

	//Listeners
	private Vector ircListeners;
	private Vector botListeners;
	private Vector errorListeners;
	private Vector channels;
	
	private Vector queuedCommands;
	private Vector queuedMessages;
	
	private Timer messageTimer;
	
	private DateFormat df;
	
	private String encoding;
	
	private class Message extends TimerTask {
		String target;
		String message;
		long delay;
		
		public Message(String target,String message,long delay) {
			this.target = target;
			this.message = message;
			this.delay = delay;
		}
		
		public void run() {
			String string = irc.privmsg(target,message);
			String nextString;
			int i;
			String[] params = new String[1];
			final int limit = 510 - (target.length() + myNick.length() + hostname.length() + 15);
			
			while (string.length() > limit) {
				i = string.lastIndexOf(" ",limit - 1);
				if (i <= limit-50) {
					nextString = string.substring(limit);
					string = string.substring(0,limit);
				}
				else {
					nextString = string.substring(i+1);
					string = string.substring(0,i);
				}
				enqueueCommand(string + "\n");
				string = irc.privmsg(target,nextString);
			}
			
			enqueueCommand(string);
			params[0] = target;
			boolean isPrivate;
			
			if (target.startsWith("#") | target.startsWith("&")
				| target.startsWith("!") | target.startsWith("+")) {
				isPrivate = false;
			}
			else {
				isPrivate = true;
			}
			
			if (message.matches("\001ACTION .*\001")) {
				sendIRCEvent(new IRCMessage("CTCP_ACTION",myNick,params,message.replaceAll("\001ACTION (.*)\001","$1"),myNick,"",isPrivate));
			}
			else {
				sendIRCEvent(new IRCMessage("PRIVMSG",myNick,params,message,myNick,"",isPrivate));
			}
		}
	}

	//Mode is the join mode on the server - i.e. invisible etc - see RFC
	public Bot(Vector nicks, Vector servers, int port, int mode, String realname, String encoding) {
		this.nicks = nicks;
		currentNick = 0;
		myNick = (String)nicks.elementAt(currentNick);
		this.servers = servers;
		currentServer = 0;
		this.port = port;
		this.realname = realname;
		this.mode = mode;
		this.encoding = encoding;

		ircListeners = new Vector();
		botListeners = new Vector();
		errorListeners = new Vector();

		
		environment = System.getProperty("os.name") + " "
			+ System.getProperty("os.version") + " "
			+ System.getProperty("os.arch") + " "
			+ "[Java " + System.getProperty("java.version") + "]";
			
		df = DateFormat.getDateTimeInstance(DateFormat.FULL,DateFormat.LONG,Locale.US);
		
		messageTimer = new Timer();
		
		consecutiveErrors = 0;
		
		try {
			InetAddress localhost = InetAddress.getLocalHost();
			hostname = localhost.getHostName();
		}
		catch (UnknownHostException e) {
			sendErrorEvent("Bot", "UnknownHostException", e);
			hostname = "localhost";
		}
		
		irc = new IRCProtocol();
				
		channels = new Vector();
		
		queuedCommands = new Vector();
		queuedMessages = new Vector();
	}
	
	public void addChannel(Channel c) {
		if (channels.contains(c) == false) {
			channels.addElement(c);
		}
	}
	
	protected void sendIRCEvent(IRCMessage msg) {
		
	}
	
	protected void sendErrorEvent(String source, String message, Throwable err) {
	}

	public void run() {
		List<IRCMessage> commands;
		
		long lastIRCEvent = System.currentTimeMillis();
		
		boolean successful = false;
		currentServer--;
		do {
			currentServer++;
			if (currentServer == servers.size()) {
				currentServer = 0;
			}
			successful = connect();
		} while (successful == false);
		
		while (true) {
			try {
				sendCommands();

				sleep(pauseTime);
				
				while (receiver.ready()) {
					commands = receive();
				
					lastIRCEvent = System.currentTimeMillis();
					
					for (IRCMessage msg : commands) {
						sendIRCEvent(msg);
					}

					sendCommands();
				}
				if (System.currentTimeMillis() - lastIRCEvent > 1000000) {
					lastIRCEvent = System.currentTimeMillis();
					sendErrorEvent("run", "Timeout (no data for 1000 seconds).", null);
					sendIRCEvent(new IRCMessage("QUIT", myNick + "!" + myNick + "@" + hostname
							, null, "Timeout (no data for 500 seconds).", myNick, myNick + "@" + hostname,false));
					reconnect("Timeout (no data for 500 seconds)");
				}
			}
			catch (SocketTimeoutException e) {
				sendErrorEvent("run", "SocketTimeoutException", e);
				reconnect("Read timeout.");
			}
			catch (IOException e) {
				sendErrorEvent("run", "IOException", e);
				reconnect("Read error.");
			}
			catch (InterruptedException e) {
				sendErrorEvent("run", "InterruptedException", e);
			}
		}
	}
	
	public synchronized boolean connect() {
		try {
			connection = new Socket((String)servers.elementAt(currentServer),port);
			connection.setSoTimeout(10000);
			receiver = new BufferedReader(new InputStreamReader(connection.getInputStream(),encoding));
			sender = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream(),encoding));
		
			sender.write(irc.nick(myNick));
			sender.write(irc.user(myNick,mode,realname));
			sender.flush();
		}
		catch (SocketTimeoutException e) {
			sendErrorEvent("connect", "SocketTimeoutException", e);
			return false;
		}
		catch (IOException e) {
			sendErrorEvent("connect", "IOException", e);
			return false;
		}
		return true;
	}

	public synchronized void disconnect(String reason) throws IOException {
		for (int i = 0; i < channels.size(); i++) {
			Channel c = (Channel)channels.elementAt(i);
			c.part();
		}
		
		sender.write(irc.quit(reason));
		sender.flush();
		
		sender.close();
		receiver.close();

		if (!connection.isClosed()) {
			connection.close();
		}
	}
	
	public synchronized void reconnect(String reason) {
		try {
			disconnect(reason);
		}
		catch (IOException e) {
		}
		
		boolean successful = false;
		currentServer--;
		do {
			nextServer();
			successful = connect();
		} while (successful == false);
	}
	
	private synchronized void nextServer() {
		currentServer++;
		if (currentServer == servers.size()) {
			currentServer= 0;
		}
	}

	public List<IRCMessage> receive() throws SocketTimeoutException,IOException {
		List<IRCMessage> output;
		
		if (receiver.ready() == true) {
			String input = receiver.readLine();
			
			output = irc.parse(input,myNick);
			
			return output;
		}
		else {
			output = null;
			return output;
		}
		
	}
	
	public synchronized boolean send(String string) {
		try {
			sender.write(string);
		}
		catch(SocketTimeoutException e) {
			sendErrorEvent("send", "SocketTimeoutException", e);
			return false;
		}
		catch (IOException e) {
			sendErrorEvent("send", "IOException", e);
			return false;
		}
		return true;
	}
	
	public synchronized void enqueueCommand(String command) {
		queuedCommands.add(command);
	}
	
	public void sendCommands() {
		String command;
		boolean success;
		while (!queuedCommands.isEmpty()) {
			command = (String)queuedCommands.remove(0);
			success = send(command);
			if (success == false) {
				queuedCommands.add(0,command);
				reconnect("Write error!");
				return;
			}
		}
		try {
			sender.flush();
		}
		catch (SocketTimeoutException e) {
			sendErrorEvent("sendCommands", "SocketTimeoutException", e);
		}
		catch (IOException e) {
			sendErrorEvent("sendCommands", "IOException", e);
			reconnect("Write error!");
		}
	}
	
	public synchronized void enqueueMessage(String target, String message, long delay) {
		queuedMessages.add(new Message(target,message,delay));
	}
	
	public void sendMessages() {
		Message msg;
		boolean success;
		if (!queuedMessages.isEmpty()) {
			msg = (Message)queuedMessages.remove(0);
			messageTimer.schedule(msg,msg.delay);
		}
	}
	
	public String getNick() {
		return myNick;
	}
	
	public String getEncoding() {
		return encoding;
	}
}

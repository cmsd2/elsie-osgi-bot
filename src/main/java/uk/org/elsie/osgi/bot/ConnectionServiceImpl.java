package uk.org.elsie.osgi.bot;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;

public class ConnectionServiceImpl implements ConnectionService, Runnable {
	private static Log log = LogFactory.getLog(ConnectionServiceImpl.class);

	private Socket connection;					//Server connection object
	private BufferedReader receiver;			//Input object
	private BufferedWriter sender;				//Output object
	private EventAdmin eventAdmin;
	private ServerListService serverList;
	private ServerListService.ServerEntry currentServer;
	private Thread thread;
	private volatile boolean finished = false;
	private ServiceReference self;
	private String encoding = "iso-8859-1";
	private long pauseTime = 1000;
	private Object monitor = new Object();
	private Object watchers = new Object();
	private List<String> queuedCommands = new LinkedList<String>();
	private IRCProtocol ircProtocol = new IRCProtocol();

	public void setEventAdmin(EventAdmin eventAdmin) {
		this.eventAdmin = eventAdmin;
	}
	
	public void unsetEventAdmin(EventAdmin eventAdmin) {
		this.eventAdmin = null;
	}
	
	public void setServerList(ServerListService serverList) {
		this.serverList = serverList;
	}
	
	public void unsetServerList(ServerListService serverList) {
		this.serverList = null;
	}
	
	public void activate(ComponentContext componentContext) {
		this.self = componentContext.getServiceReference();
		thread = new Thread(this);
		thread.start();
	}
	
	public void deactivate() {
		if(thread != null) {
			finished = true;
			synchronized (monitor) {
				monitor.notify();
			}
		}
	}
	
	@Override
	public synchronized void nextServer() {
		currentServer = serverList.nextServer(currentServer);
	}
	
	protected synchronized boolean hasQueuedCommands() {
		return !queuedCommands.isEmpty();
	}
	
	public void run() {
		nextServer();
		while (!finished) {
			try {
				if(!isConnected())
					reconnect("Connecting");

				synchronized(monitor) {
					if(!hasQueuedCommands()) {
						monitor.wait(pauseTime);
					}
				}
				
				synchronized(watchers) {
					sendQueued();
					watchers.notifyAll();
				}
				
				while (receiver.ready()) {
					receive();
				}
			}
			catch (SocketTimeoutException e) {
				sendErrorEvent("run", "SocketTimeoutException", e);
				reconnect("socket timeout");
			}
			catch (IOException e) {
				sendErrorEvent("run", "IOException", e);
				reconnect("io error");
			}
			catch (InterruptedException e) {
			}
		}
	}
	
	public synchronized boolean connect(ServerListService.ServerEntry serverEntry) {
		log.info("Connecting to " + serverEntry.getHostname() + ":" + serverEntry.getPort());
		this.currentServer = serverEntry;
		sendConnectionEvent(IrcEventConstants.IRC_CONNECTING_TOPIC, false);
		try {
			connection = new Socket(currentServer.getHostname(), currentServer.getPort());
			connection.setSoTimeout(10000);
			receiver = new BufferedReader(new InputStreamReader(connection.getInputStream(), encoding));
			sender = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream(), encoding));

			log.info("Connected");
			sendConnectionEvent(IrcEventConstants.IRC_CONNECTED_TOPIC, true);
			return true;
		}
		catch (SocketTimeoutException e) {
			log.error("socket timeout", e);
			sendErrorEvent("connect", "SocketTimeoutException", e);
		}
		catch (IOException e) {
			log.error("io error", e);
			sendErrorEvent("connect", "IOException", e);
		}
		log.info("Connect failed; disonnected");
		sendConnectionEvent(IrcEventConstants.IRC_DISCONNECTED_TOPIC, false);
		return false;
	}

	public synchronized void disconnect(String reason) throws IOException {
		log.info("Disconnecting: " + reason);
		try {
			send(ircProtocol.quit(reason));
			sender.close();
			receiver.close();

			if (!connection.isClosed()) {
				connection.close();
			}
		} catch (Exception e) {
			log.error("error", e);
			sendErrorEvent("disconnect", "Error disconnecting", e);
		} finally {
			log.info("Disconnected");
			sendConnectionEvent(IrcEventConstants.IRC_DISCONNECTED_TOPIC, false);
			connection = null;
		}
	}
	
	public synchronized void reconnect(String reason) {
		log.info("Reconnecting: " + reason);
		if(isConnected()) {
			try {
				disconnect(reason);
			}
			catch (IOException e) {
			}
		}
		
		if(serverList == null)
			throw new RuntimeException("no servers configured");

		boolean successful = false;
		successful = connect(currentServer);
		int count = 0;
		while(!successful && !finished) {
			try {
				synchronized(monitor) {
					if(!finished) {
						monitor.wait(pauseTime + (1000 * count++));
					}
				}
			} catch (InterruptedException e) { }
			if(!finished) {
				currentServer = serverList.nextServer(currentServer);
				successful = connect(currentServer);
			}
		}
		log.info("Reconnected");
	}
	
	@Override
	public synchronized boolean isConnected() {
		return connection != null && !connection.isClosed();
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
	
	public void receive() throws SocketTimeoutException, IOException {
		if (receiver.ready() == true) {
			String input = receiver.readLine();
			
			sendRawMessageEvent(input);
		}
	}
	
	@Override
	public synchronized void postCommand(String command) {
		queuedCommands.add(command);
		synchronized (monitor) {
			monitor.notify();
		}
	}
	
	public void sendCommand(String command) throws InterruptedException {
		synchronized (watchers) {
			postCommand(command);
			watchers.wait();
		}
	}
	
	public void sendCommand(String command, long timeout) throws InterruptedException {
		synchronized (watchers) {
			postCommand(command);
			watchers.wait(timeout);
		}
	}
	
	private synchronized void sendQueued() {
		String command;
		boolean success;
		while (!queuedCommands.isEmpty()) {
			command = (String)queuedCommands.remove(0);
			success = send(command);
			if (success == false) {
				queuedCommands.add(0,command);
				reconnect("send error");
				return;
			}
		}
		try {
			sender.flush();
		}
		catch (SocketTimeoutException e) {
			sendErrorEvent("sendCommands", "SocketTimeoutException", e);
			reconnect("socket timeout");
		}
		catch (IOException e) {
			sendErrorEvent("sendCommands", "IOException", e);
			reconnect("send io error");
		}
	}
	
	protected void sendRawMessageEvent(String msg) {
		if(eventAdmin == null)
			return;

		Map<String, Object> properties = new TreeMap<String, Object>();
		if(self != null)
			properties.put(EventConstants.SERVICE, self);
		properties.put(IrcEventConstants.RAW_MESSAGE, msg);
		if(currentServer != null) {
			properties.put(IrcEventConstants.SERVER_HOSTNAME, currentServer.getHostname());
			properties.put(IrcEventConstants.SERVER_PORT, currentServer.getPort());
		}
		
		Event event = new Event(IrcEventConstants.IRC_RAW_MESSAGE_TOPIC, properties);
		eventAdmin.postEvent(event);
	}
	
	protected void sendErrorEvent(String source, String msg, Throwable err) {
		if(eventAdmin == null)
			return;

		Map<String, Object> properties = new TreeMap<String, Object>();
		if(self != null)
			properties.put(EventConstants.SERVICE, self);
		properties.put(IrcEventConstants.RAW_MESSAGE, source + ": " + msg);
		if(err != null) {
			properties.put(EventConstants.EXCEPTION, err);
			properties.put(EventConstants.EXCEPTION_CLASS, err.getClass().getName());
			properties.put(EventConstants.EXCEPTION_MESSAGE, err.getMessage());
		}
		if(currentServer != null) {
			properties.put(IrcEventConstants.SERVER_HOSTNAME, currentServer.getHostname());
			properties.put(IrcEventConstants.SERVER_PORT, currentServer.getPort());
		}
		
		Event event = new Event(IrcEventConstants.ERROR_TOPIC, properties);
		eventAdmin.postEvent(event);
	}
	
	protected void sendConnectionEvent(String topic, boolean connected) {
		if(eventAdmin == null)
			return;
		
		Map<String, Object> properties = new TreeMap<String, Object>();
		if(self != null)
			properties.put(EventConstants.SERVICE, self);
		properties.put(IrcEventConstants.SERVER_HOSTNAME, currentServer.getHostname());
		properties.put(IrcEventConstants.SERVER_PORT, currentServer.getPort());
		properties.put(IrcEventConstants.CONNECTED, connected);
		
		Event event = new Event(topic, properties);
		eventAdmin.postEvent(event);
	}
}

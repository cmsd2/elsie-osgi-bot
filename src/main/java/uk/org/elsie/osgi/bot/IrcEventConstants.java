package uk.org.elsie.osgi.bot;

public interface IrcEventConstants {
	public static final String COMMAND = "irc.msg.command";
	public static final String PREFIX = "irc.msg.prefix";
	public static final String PARAMS = "irc.msg.params";
	public static final String ESCAPED_PARAMS = "irc.msg.escapedParams";
	public static final String PREFIX_NICK = "irc.msg.prefixNick";
	public static final String IDENT = "irc.msg.ident";
	public static final String PRIVATE = "irc.msg.private";
	public static final String NETWORK = "irc.network";
	
	public static final String IRC_SERVICE = "irc.service";
	public static final String SERVER_HOSTNAME = "irc.server.hostname";
	public static final String SERVER_PORT = "irc.server.port";
	public static final String CONNECTED = "irc.connected";
	public static final String RAW_MESSAGE = "irc.rawMessage";
	public static final String IRC_MESSAGE = "irc.msg";
	public static final String IRC_NICK = "irc.nick";
	public static final String IRC_CHANNEL = "irc.channel";
	public static final String SENDER_NICK = "irc.sender.nick";
	public static final String BOT_COMMAND = "irc.bot.command";
	public static final String IRC_USER_MODE = "irc.user.mode";
	public static final String IRC_USER_REAL_NAME = "irc.user.realName";
	public static final String IRC_NICKS = "irc.nicks";

	public static final String IRC_PRIVMSG_TOPIC = "elsie/irc/privmsg";
	public static final String IRC_CTCP_ACTION_TOPIC = "elsie/irc/ctcpAction";
	public static final String IRC_CONNECTED_TOPIC = "elsie/irc/connected";
	public static final String IRC_DISCONNECTED_TOPIC = "elsie/irc/disconnected";
	public static final String IRC_CONNECTING_TOPIC = "elsie/irc/connecting";
	public static final String ERROR_TOPIC = "elsie/error";
	public static final String IRC_RAW_MESSAGE_TOPIC = "elsie/irc/rawMessage";
	public static final String IRC_MESSAGE_TOPIC = "elsie/irc/msg";
	public static final String IRC_NICK_CHANGING_TOPIC = "elsie/irc/nick/changing";
	public static final String IRC_NICK_CHANGED_TOPIC = "elsie/irc/nick/changed";
	public static final String IRC_REGISTERED_TOPIC = "elsie/irc/registered";
	public static final String IRC_CHANNEL_JOINED_TOPIC = "elsie/irc/channel/joined";
	public static final String IRC_CHANNEL_PARTED_TOPIC = "elsie/irc/channel/parted";
	public static final String IRC_CHANNEL_MESSAGE_TOPIC = "elsie/irc/channel/message";
	public static final String IRC_BOT_MESSAGE_TOPIC = "elsie/irc/bot/message";
	public static final String IRC_UNREGISTERED_TOPIC = "elsie/irc/unregistered";
}

package uk.org.elsie.osgi.bot;

import java.util.Collection;

import uk.org.elsie.osgi.bot.ChannelNameServiceImpl.UserInfo;

public interface ChannelNameService {

	public abstract String getChannelName();

	public abstract void rehash();

	public abstract UserInfo getUserStatus(String user);

	public abstract int getNumUsers();

	public abstract Collection<UserInfo> getUsers();

}
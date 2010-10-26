package uk.org.elsie.osgi.bot;

public interface Channel {

	public abstract String getNick();

	public abstract String getChannel();

	public abstract void join();

	public abstract void part();

}
package uk.org.elsie.osgi.bot;


public interface NickListService {
	public interface NickEntry {
		public String getName();
	}

	public NickEntry nextNick(NickEntry object);
}

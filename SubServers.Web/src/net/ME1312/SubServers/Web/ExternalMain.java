package net.ME1312.SubServers.Web;

public class ExternalMain {
	public static void main(String[] args) throws Exception {
		JettyServer server = new JettyServer();
		server.start(null);
	}
}

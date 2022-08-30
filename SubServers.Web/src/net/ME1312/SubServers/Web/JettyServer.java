package net.ME1312.SubServers.Web;

import net.ME1312.SubServers.Web.endpoints.StatusServlet;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletHandler;

public class JettyServer {
	public Server server;

	public void start() throws Exception {
		server = new Server();
		ServerConnector connector = new ServerConnector(server);
		connector.setPort(8090);
		server.setConnectors(new Connector[] {connector});

		ServletHandler handler = new ServletHandler();
		server.setHandler(handler);
		handler.addServletWithMapping(StatusServlet.class, "/status");
		server.start();
	}
}
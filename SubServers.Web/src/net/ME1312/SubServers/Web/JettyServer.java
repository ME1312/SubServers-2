package net.ME1312.SubServers.Web;

import net.ME1312.Galaxi.Engine.GalaxiEngine;
import net.ME1312.Galaxi.Event.Engine.GalaxiReloadEvent;
import net.ME1312.Galaxi.Event.Engine.GalaxiStopEvent;
import net.ME1312.Galaxi.Library.Config.YAMLConfig;
import net.ME1312.Galaxi.Library.Container.Pair;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.Galaxi.Log.Logger;
import net.ME1312.Galaxi.Plugin.App;
import net.ME1312.Galaxi.Plugin.PluginInfo;
import net.ME1312.SubData.Client.DataClient;
import net.ME1312.SubData.Client.Encryption.AES;
import net.ME1312.SubData.Client.Encryption.DHE;
import net.ME1312.SubData.Client.Encryption.RSA;
import net.ME1312.SubData.Client.Library.DisconnectReason;
import net.ME1312.SubData.Client.SubDataClient;
import net.ME1312.SubServers.Bungee.SubProxy;
import net.ME1312.SubServers.Host.Executable.SubCreatorImpl;
import net.ME1312.SubServers.Host.Executable.SubLoggerImpl;
import net.ME1312.SubServers.Host.Executable.SubServerImpl;
import net.ME1312.SubServers.Web.Library.ConfigUpdater;
import net.ME1312.SubServers.Web.Network.SubProtocol;
import net.ME1312.SubServers.Web.Endpoints.StatusServlet;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletHandler;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.TimeUnit;

@App(name = "SubServers.Web", version = "2.19a", authors = "ME1312", website = "https://github.com/ME1312/SubServers-2", description = "Operate subservers from web browsers")
public class JettyServer {
	public Server server;
	public boolean isPlugin = true;

	HashMap<Integer, SubDataClient> subdata = new HashMap<Integer, SubDataClient>();
	Pair<Long, Map<String, Map<String, String>>> lang = null;
	public HashMap<String, SubCreatorImpl.ServerTemplate> templatesR = new HashMap<String, SubCreatorImpl.ServerTemplate>();
	public HashMap<String, SubCreatorImpl.ServerTemplate> templates = new HashMap<String, SubCreatorImpl.ServerTemplate>();
	public HashMap<String, SubServerImpl> servers = new HashMap<String, SubServerImpl>();

	public Logger log;
	public PluginInfo info;
	public GalaxiEngine engine;
	public YAMLConfig config;
	public ObjectMap<String> host = null;
	public SubProtocol subprotocol;
	public SubProxy proxy = null;

	public final SubAPI api = new SubAPI(this);

	private long resetDate = 0;
	private boolean reconnect = true;
	private boolean running = false;

	public void start(SubProxy proxy) throws Exception {
		if (proxy == null){
			isPlugin = false;
		}
		this.proxy = proxy;

		log = new Logger("SubServers");
		info = PluginInfo.load(this);
		info.setLogger(log);
		engine = GalaxiEngine.init(info);

		ConfigUpdater.updateConfig(new File(engine.getRuntimeDirectory(), "config.yml"), isPlugin);
		config = new YAMLConfig(new File(engine.getRuntimeDirectory(), "config.yml"));

		Util.reflect(SubLoggerImpl.class.getDeclaredField("logn"), null, config.get().getMap("Settings").getBoolean("Network-Log", true));
		Util.reflect(SubLoggerImpl.class.getDeclaredField("logc"), null, config.get().getMap("Settings").getBoolean("Console-Log", true));

		engine.getPluginManager().loadPlugins(new File(engine.getRuntimeDirectory(), "Plugins"));

		running = true;
		reload(false);

		subdata.put(0, null);
		subprotocol = SubProtocol.get();
		subprotocol.registerCipher("DHE", DHE.get(128));
		subprotocol.registerCipher("DHE-128", DHE.get(128));
		subprotocol.registerCipher("DHE-192", DHE.get(192));
		subprotocol.registerCipher("DHE-256", DHE.get(256));
		api.name = config.get().getMap("Settings").getMap("SubData").getString("Name");
		Logger log = new Logger("SubData");

		if (config.get().getMap("Settings").getMap("SubData").getString("Password", "").length() > 0) {
			subprotocol.registerCipher("AES", new AES(128, config.get().getMap("Settings").getMap("SubData").getString("Password")));
			subprotocol.registerCipher("AES-128", new AES(128, config.get().getMap("Settings").getMap("SubData").getString("Password")));
			subprotocol.registerCipher("AES-192", new AES(192, config.get().getMap("Settings").getMap("SubData").getString("Password")));
			subprotocol.registerCipher("AES-256", new AES(256, config.get().getMap("Settings").getMap("SubData").getString("Password")));

			log.info.println("AES Encryption Available");
		}
		if (new File(engine.getRuntimeDirectory(), "subdata.rsa.key").exists()) {
			try {
				subprotocol.registerCipher("RSA", new RSA(new File(engine.getRuntimeDirectory(), "subdata.rsa.key")));
				log.info.println("RSA Encryption Available");
			} catch (Exception e) {
				log.error.println(e);
			}
		}

		reconnect = true;
		log.info.println();
		log.info.println("Connecting to /" + config.get().getMap("Settings").getMap("SubData").getString("Address", "127.0.0.1:4391"));
		connect(log.toPrimitive(), null);

		server = new Server();
		ServerConnector connector = new ServerConnector(server);
		connector.setPort(8090);
		server.setConnectors(new Connector[] {connector});

		ServletHandler handler = new ServletHandler();
		server.setHandler(handler);
		handler.addServletWithMapping(StatusServlet.class, "/status");
		server.start();
	}

	public void reload(boolean notifyPlugins) throws IOException {
		resetDate = Calendar.getInstance().getTime().getTime();

		ConfigUpdater.updateConfig(new File(engine.getRuntimeDirectory(), "config.yml"), isPlugin);
		config.reload();

		if (notifyPlugins) {
			engine.getPluginManager().executeEvent(new GalaxiReloadEvent(engine));
		}
	}

	private void connect(final java.util.logging.Logger log, Pair<DisconnectReason, DataClient> disconnect) throws IOException {
		int port = Integer.parseInt(config.get().getMap("Settings").getMap("SubData").getString("Address", "127.0.0.1:4391").split(":")[1]);
		String address = config.get().getMap("Settings").getMap("SubData").getString("Address", "127.0.0.1:4391").split(":")[0];
		if (isPlugin){
			address = proxy.subdata.getSocket().getInetAddress().getHostName();
			port = proxy.subdata.getSocket().getLocalPort();
		}

		final int reconnect = config.get().getMap("Settings").getMap("SubData").getInt("Reconnect", 60);
		if (disconnect == null || (this.reconnect && reconnect > 0 && disconnect.key() != DisconnectReason.PROTOCOL_MISMATCH && disconnect.key() != DisconnectReason.ENCRYPTION_MISMATCH)) {
			final long reset = resetDate;
			final Timer timer = new Timer(SubAPI.getInstance().getAppInfo().getName() + "::SubData_Reconnect_Handler");
			if (disconnect != null) log.info("Attempting reconnect in " + reconnect + " seconds");
			String finalAddress = address;
			int finalPort = port;
			timer.scheduleAtFixedRate(new TimerTask() {
				@Override
				public void run() {
					try {
						if (reset == resetDate && (subdata.getOrDefault(0, null) == null || subdata.get(0).isClosed())) {
							SubDataClient open = subprotocol.open(InetAddress.getByName(finalAddress), finalPort);

							if (subdata.getOrDefault(0, null) != null) subdata.get(0).reconnect(open);
							subdata.put(0, open);
						}
						timer.cancel();
					} catch (IOException e) {
						log.info("Connection was unsuccessful, retrying in " + reconnect + " seconds");
					}
				}
			}, (disconnect == null) ? 0 : TimeUnit.SECONDS.toMillis(reconnect), TimeUnit.SECONDS.toMillis(reconnect));
		}
	}

	public void stop() throws Exception {
		running = false;
		engine.getPluginManager().executeEvent(new GalaxiStopEvent(engine, 0));
		server.stop();
	}
}
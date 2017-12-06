package net.ME1312.SubServers.Sync;

import net.ME1312.SubServers.Sync.Event.*;
import net.ME1312.SubServers.Sync.Library.Config.YAMLConfig;
import net.ME1312.SubServers.Sync.Library.Config.YAMLSection;
import net.ME1312.SubServers.Sync.Library.Metrics;
import net.ME1312.SubServers.Sync.Library.UniversalFile;
import net.ME1312.SubServers.Sync.Library.Util;
import net.ME1312.SubServers.Sync.Library.Version.Version;
import net.ME1312.SubServers.Sync.Network.Packet.PacketDownloadServerInfo;
import net.ME1312.SubServers.Sync.Network.SubDataClient;
import net.ME1312.SubServers.Sync.Server.Server;
import net.ME1312.SubServers.Sync.Server.SubServer;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Main Plugin Class
 */
public final class SubPlugin extends BungeeCord implements Listener {
    public final Map<String, Server> servers = new TreeMap<String, Server>();

    public final PrintStream out;
    public final UniversalFile dir = new UniversalFile(new File(System.getProperty("user.dir")));
    public YAMLConfig config;
    public YAMLSection lang = null;
    public SubDataClient subdata = null;
    public final Version version = new Version(SubPlugin.class.getPackage().getImplementationVersion());
    public final Version bversion = (SubPlugin.class.getPackage().getSpecificationVersion().equals("0"))?null:new Version(SubPlugin.class.getPackage().getSpecificationVersion());

    public final SubAPI api = new SubAPI(this);
    private boolean posted = false;

    protected SubPlugin(PrintStream out) throws IOException {
        System.out.println("SubServers > Loading SubServers.Sync v" + version.toString() + " Libraries... ");

        this.out = out;
        if (!(new UniversalFile(dir, "config.yml").exists())) {
            Util.copyFromJar(SubPlugin.class.getClassLoader(), "net/ME1312/SubServers/Sync/Library/Files/bungee.yml", new UniversalFile(dir, "config.yml").getPath());
            YAMLConfig tmp = new YAMLConfig(new UniversalFile("config.yml"));
            tmp.get().set("stats", UUID.randomUUID().toString());
            tmp.save();
            System.out.println("SubServers > Created ~/config.yml");
        }
        if (!(new UniversalFile(dir, "modules.yml").exists())) {
            Util.copyFromJar(SubPlugin.class.getClassLoader(), "net/ME1312/SubServers/Sync/Library/Files/modules.yml", new UniversalFile(dir, "modules.yml").getPath());
            System.out.println("SubServers > Created ~/modules.yml");
        }
        UniversalFile dir = new UniversalFile(this.dir, "SubServers");
        dir.mkdir();
        if (!(new UniversalFile(dir, "sync.yml").exists())) {
            Util.copyFromJar(SubPlugin.class.getClassLoader(), "net/ME1312/SubServers/Sync/Library/Files/config.yml", new UniversalFile(dir, "sync.yml").getPath());
            System.out.println("SubServers > Created ~/SubServers/sync.yml");
        } else if ((new Version((new YAMLConfig(new UniversalFile(dir, "sync.yml"))).get().getSection("Settings").getString("Version", "0")).compareTo(new Version("2.11.2a+"))) != 0) {
            Files.move(new UniversalFile(dir, "sync.yml").toPath(), new UniversalFile(dir, "config.old" + Math.round(Math.random() * 100000) + ".yml").toPath());

            Util.copyFromJar(SubPlugin.class.getClassLoader(), "net/ME1312/SubServers/Sync/Library/Files/config.yml", new UniversalFile(dir, "sync.yml").getPath());
            System.out.println("SubServers > Updated ~/SubServers/sync.yml");
        }
        config = new YAMLConfig(new UniversalFile(dir, "sync.yml"));

        getPluginManager().registerCommand(null, new SubCommand.BungeeServer(this, "server"));
        getPluginManager().registerCommand(null, new SubCommand.BungeeList(this, "glist"));
        getPluginManager().registerCommand(null, new SubCommand(this, "subservers"));
        getPluginManager().registerCommand(null, new SubCommand(this, "subserver"));
        getPluginManager().registerCommand(null, new SubCommand(this, "sub"));
        getPluginManager().registerListener(null, this);

        System.out.println("SubServers > Loading BungeeCord Libraries...");
    }

    /**
     * Load Hosts, Servers, SubServers, and SubData Direct
     */
    @Override
    public void startListeners() {
        try {
            config.reload();
            SubDataClient.Encryption encryption = SubDataClient.Encryption.NONE;
            if (config.get().getSection("Settings").getSection("SubData").getString("Password", "").length() == 0) {
                System.out.println("SubData > Cannot encrypt connection without a password");
            } else if (Util.isException(() -> SubDataClient.Encryption.valueOf(config.get().getSection("Settings").getSection("SubData").getRawString("Encryption", "NONE").replace('-', '_').replace(' ', '_').toUpperCase()))) {
                System.out.println("SubData > Unknown encryption type: " + SubDataClient.Encryption.valueOf(config.get().getSection("Settings").getSection("SubData").getRawString("Encryption", "None")));
            } else {
                encryption = SubDataClient.Encryption.valueOf(config.get().getSection("Settings").getSection("SubData").getRawString("Encryption", "NONE").replace('-', '_').replace(' ', '_').toUpperCase());
            }
            subdata = new SubDataClient(this, InetAddress.getByName(config.get().getSection("Settings").getSection("SubData").getRawString("Address", "127.0.0.1:4391").split(":")[0]), Integer.parseInt(config.get().getSection("Settings").getSection("SubData").getRawString("Address", "127.0.0.1:4391").split(":")[1]), encryption);

            super.startListeners();
            if (!posted) {
                posted = true;
                post();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void post() {
        new Metrics(this);

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    Document updxml = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new StringReader(Util.readAll(new BufferedReader(new InputStreamReader(new URL("https://src.me1312.net/maven/net/ME1312/SubServers/SubServers.Sync/maven-metadata.xml").openStream(), Charset.forName("UTF-8")))))));

                    NodeList updnodeList = updxml.getElementsByTagName("version");
                    Version updversion = version;
                    int updcount = -1;
                    for (int i = 0; i < updnodeList.getLength(); i++) {
                        Node node = updnodeList.item(i);
                        if (node.getNodeType() == Node.ELEMENT_NODE) {
                            if (!node.getTextContent().startsWith("-") && new Version(node.getTextContent()).compareTo(updversion) >= 0) {
                                updversion = new Version(node.getTextContent());
                                updcount++;
                            }
                        }
                    }
                    if (!updversion.equals(version)) System.out.println("SubServers > SubServers.Sync v" + updversion + " is available. You are " + updcount + " version" + ((updcount == 1)?"":"s") + " behind.");
                } catch (Exception e) {}
            }
        }, 0, TimeUnit.DAYS.toMillis(2));
    }

    /**
     * Emulate BungeeCord's getServers()
     *
     * @return Server Map
     */
    @Override
    public Map<String, ServerInfo> getServers() {
        if (servers.size() > 0) {
            return new TreeMap<String, ServerInfo>(servers);
        } else {
            return super.getServers();
        }
    }

    /**
     * Reset all changes made by startListeners
     *
     * @see SubPlugin#startListeners()
     */
    @Override
    public void stopListeners() {
        try {
            System.out.println("SubServers > Resetting Server Data");
            servers.clear();

            subdata.destroy(0);
        } catch (Exception e) {
            e.printStackTrace();
        }

        super.stopListeners();
    }

    @EventHandler(priority = Byte.MAX_VALUE)
    public void reroute(ServerConnectEvent e) {
        Map<String, ServerInfo> servers = new TreeMap<String, ServerInfo>(api.getServers());
        if (servers.keySet().contains(e.getTarget().getName().toLowerCase()) && e.getTarget() != servers.get(e.getTarget().getName().toLowerCase())) {
            e.setTarget(servers.get(e.getTarget().getName().toLowerCase()));
        } else {
            servers = getServers();
            if (servers.keySet().contains(e.getTarget().getName()) && e.getTarget() != servers.get(e.getTarget().getName())) {
                e.setTarget(servers.get(e.getTarget().getName()));
            }
        }
    }

    @EventHandler(priority = Byte.MIN_VALUE)
    public void add(SubAddServerEvent e) {
        subdata.sendPacket(new PacketDownloadServerInfo(e.getServer(), json -> {
            switch (json.getString("type").toLowerCase()) {
                case "invalid":
                    System.out.println("PacketDownloadServerInfo(" + e.getServer() + ") returned with an invalid response");
                    break;
                case "subserver":
                    servers.put(json.getJSONObject("server").getString("name").toLowerCase(), new SubServer(json.getJSONObject("server").getString("name"), json.getJSONObject("server").getString("display"), new InetSocketAddress(json.getJSONObject("server").getString("address").split(":")[0], Integer.parseInt(json.getJSONObject("server").getString("address").split(":")[1])), json.getJSONObject("server").getString("motd"), json.getJSONObject("server").getBoolean("hidden"), json.getJSONObject("server").getBoolean("restricted"), json.getJSONObject("server").getBoolean("running")));
                    System.out.println("SubServers > Added SubServer: " + e.getServer());
                    break;
                default:
                    servers.put(json.getJSONObject("server").getString("name").toLowerCase(), new Server(json.getJSONObject("server").getString("name"), json.getJSONObject("server").getString("display"), new InetSocketAddress(json.getJSONObject("server").getString("address").split(":")[0], Integer.parseInt(json.getJSONObject("server").getString("address").split(":")[1])), json.getJSONObject("server").getString("motd"), json.getJSONObject("server").getBoolean("hidden"), json.getJSONObject("server").getBoolean("restricted")));
                    System.out.println("SubServers > Added Server: " + e.getServer());
                    break;
            }
        }));
    }

    @EventHandler(priority = Byte.MIN_VALUE)
    public void start(SubStartEvent e) {
        if (servers.keySet().contains(e.getServer().toLowerCase()) && servers.get(e.getServer().toLowerCase()) instanceof SubServer)
            ((SubServer) servers.get(e.getServer().toLowerCase())).setRunning(true);
    }

    @EventHandler(priority = Byte.MIN_VALUE)
    public void edit(SubEditServerEvent e) {
        if (servers.keySet().contains(e.getServer().toLowerCase())) {
            Server server = servers.get(e.getServer().toLowerCase());
            switch (e.getEdit().name().toLowerCase()) {
                case "display":
                    server.setDisplayName(e.getEdit().get().asString());
                    break;
                case "motd":
                    server.setMotd(e.getEdit().get().asColoredString('&'));
                    break;
                case "restricted":
                    server.setRestricted(e.getEdit().get().asBoolean());
                    break;
                case "hidden":
                    server.setHidden(e.getEdit().get().asBoolean());
                    break;
            }
        }
    }

    @EventHandler(priority = Byte.MIN_VALUE)
    public void stop(SubStoppedEvent e) {
        if (servers.keySet().contains(e.getServer().toLowerCase()) && servers.get(e.getServer().toLowerCase()) instanceof SubServer)
            ((SubServer) servers.get(e.getServer().toLowerCase())).setRunning(false);
    }

    @EventHandler(priority = Byte.MIN_VALUE)
    public void remove(SubRemoveServerEvent e) {
        if (servers.keySet().contains(e.getServer().toLowerCase()))
            servers.remove(e.getServer().toLowerCase());
            System.out.println("SubServers > Removed Server: " + e.getServer());
    }
}

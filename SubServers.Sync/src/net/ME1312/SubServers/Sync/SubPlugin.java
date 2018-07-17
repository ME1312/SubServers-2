package net.ME1312.SubServers.Sync;

import net.ME1312.SubServers.Sync.Event.*;
import net.ME1312.SubServers.Sync.Library.Config.YAMLConfig;
import net.ME1312.SubServers.Sync.Library.Config.YAMLSection;
import net.ME1312.SubServers.Sync.Library.Metrics;
import net.ME1312.SubServers.Sync.Library.NamedContainer;
import net.ME1312.SubServers.Sync.Library.UniversalFile;
import net.ME1312.SubServers.Sync.Library.Util;
import net.ME1312.SubServers.Sync.Library.Version.Version;
import net.ME1312.SubServers.Sync.Library.Version.VersionType;
import net.ME1312.SubServers.Sync.Network.Cipher;
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
    protected NamedContainer<Long, Map<String, Map<String, String>>> lang = null;
    public final Map<String, Server> servers = new TreeMap<String, Server>();

    public final PrintStream out;
    public final UniversalFile dir = new UniversalFile(new File(System.getProperty("user.dir")));
    public YAMLConfig config;
    public boolean redis = false;
    public final SubAPI api = new SubAPI(this);
    public SubDataClient subdata = null;
    //public static final Version version = Version.fromString("2.13a/pr5");
    public static final Version version = new Version(Version.fromString("2.13a/pr5"), VersionType.SNAPSHOT, (SubPlugin.class.getPackage().getSpecificationTitle() == null)?"undefined":SubPlugin.class.getPackage().getSpecificationTitle()); // TODO Snapshot Version

    public final boolean isPatched;
    public long lastReload = -1;
    private boolean posted = false;

    protected SubPlugin(PrintStream out, boolean isPatched) throws IOException {
        this.isPatched = isPatched;
        System.out.println("SubServers > Loading SubServers.Sync v" + version.toString() + " Libraries (for Minecraft " + api.getGameVersion()[api.getGameVersion().length - 1] + ")");

        this.out = out;
        if (!(new UniversalFile(dir, "config.yml").exists())) {
            Util.copyFromJar(SubPlugin.class.getClassLoader(), "net/ME1312/SubServers/Sync/Library/Files/bungee.yml", new UniversalFile(dir, "config.yml").getPath());
            YAMLConfig tmp = new YAMLConfig(new UniversalFile("config.yml"));
            tmp.get().set("stats", UUID.randomUUID().toString());
            tmp.save();
            System.out.println("SubServers > Created ~/config.yml");
        }
        UniversalFile dir = new UniversalFile(this.dir, "SubServers");
        dir.mkdir();
        if (!(new UniversalFile(dir, "sync.yml").exists())) {
            Util.copyFromJar(SubPlugin.class.getClassLoader(), "net/ME1312/SubServers/Sync/Library/Files/config.yml", new UniversalFile(dir, "sync.yml").getPath());
            System.out.println("SubServers > Created ~/SubServers/sync.yml");
        } else if ((new Version((new YAMLConfig(new UniversalFile(dir, "sync.yml"))).get().getSection("Settings").getRawString("Version", "0")).compareTo(new Version("2.11.2a+"))) != 0) {
            Files.move(new UniversalFile(dir, "sync.yml").toPath(), new UniversalFile(dir, "config.old" + Math.round(Math.random() * 100000) + ".yml").toPath());

            Util.copyFromJar(SubPlugin.class.getClassLoader(), "net/ME1312/SubServers/Sync/Library/Files/config.yml", new UniversalFile(dir, "sync.yml").getPath());
            System.out.println("SubServers > Updated ~/SubServers/sync.yml");
        }
        config = new YAMLConfig(new UniversalFile(dir, "sync.yml"));

        getPluginManager().registerListener(null, this);

        System.out.println("SubServers > Loading BungeeCord Libraries...");
    }

    /**
     * Load Hosts, Servers, SubServers, and SubData Direct
     */
    @Override
    public void startListeners() {
        try {
            redis = getPluginManager().getPlugin("RedisBungee") != null;
            config.reload();

            Cipher cipher = null;
            if (!config.get().getSection("Settings").getSection("SubData").getRawString("Encryption", "NONE").equalsIgnoreCase("NONE")) {
                if (config.get().getSection("Settings").getSection("SubData").getRawString("Password", "").length() == 0) {
                    System.out.println("SubData > Cannot encrypt connection without a password");
                } else if (!SubDataClient.getCiphers().keySet().contains(config.get().getSection("Settings").getSection("SubData").getRawString("Encryption").toUpperCase().replace('-', '_').replace(' ', '_'))) {
                    System.out.println("SubData > Unknown encryption type: " + config.get().getSection("Settings").getSection("SubData").getRawString("Encryption"));
                } else {
                    cipher = SubDataClient.getCipher(config.get().getSection("Settings").getSection("SubData").getRawString("Encryption"));
                }
            }
            subdata = new SubDataClient(this, config.get().getSection("Settings").getSection("SubData").getRawString("Name", null),
                    InetAddress.getByName(config.get().getSection("Settings").getSection("SubData").getRawString("Address", "127.0.0.1:4391").split(":")[0]),
                    Integer.parseInt(config.get().getSection("Settings").getSection("SubData").getRawString("Address", "127.0.0.1:4391").split(":")[1]), cipher);

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
        if (config.get().getSection("Settings").getBoolean("Override-Bungee-Commands", true)) {
            getPluginManager().registerCommand(null, SubCommand.BungeeServer.newInstance(this, "server").get());
            getPluginManager().registerCommand(null, new SubCommand.BungeeList(this, "glist"));
        }
        getPluginManager().registerCommand(null, SubCommand.newInstance(this, "subservers").get());
        getPluginManager().registerCommand(null, SubCommand.newInstance(this, "subserver").get());
        getPluginManager().registerCommand(null, SubCommand.newInstance(this, "sub").get());

        new Metrics(this);
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    Document updxml = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new StringReader(Util.readAll(new BufferedReader(new InputStreamReader(new URL("https://src.me1312.net/maven/net/ME1312/SubServers/SubServers.Sync/maven-metadata.xml").openStream(), Charset.forName("UTF-8")))))));

                    NodeList updnodeList = updxml.getElementsByTagName("version");
                    Version updversion = version;
                    int updcount = 0;
                    for (int i = 0; i < updnodeList.getLength(); i++) {
                        Node node = updnodeList.item(i);
                        if (node.getNodeType() == Node.ELEMENT_NODE) {
                            if (!node.getTextContent().startsWith("-") && !node.getTextContent().equals(version.toString()) && Version.fromString(node.getTextContent()).compareTo(updversion) > 0) {
                                updversion = Version.fromString(node.getTextContent());
                                updcount++;
                            }
                        }
                    }
                    if (updcount > 0) System.out.println("SubServers > SubServers.Sync v" + updversion + " is available. You are " + updcount + " version" + ((updcount == 1)?"":"s") + " behind.");
                } catch (Exception e) {}
            }
        }, 0, TimeUnit.DAYS.toMillis(2));
    }

    /**
     * Reference a RedisBungee method via reflection
     *
     * @param method Method to reference
     * @param args Method arguments
     * @return Method Response
     */
    @SuppressWarnings("unchecked")
    public Object redis(String method, NamedContainer<Class<?>, ?>... args) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        if (redis) {
            Object api = getPluginManager().getPlugin("RedisBungee").getClass().getMethod("getApi").invoke(null);
            Class<?>[] classargs = new Class<?>[args.length];
            Object[] objargs = new Object[args.length];
            for (int i = 0; i < args.length; i++) {
                classargs[i] = args[i].name();
                objargs[i] = args[i].get();
                if (!classargs[i].isInstance(objargs[i])) throw new ClassCastException(classargs[i].getCanonicalName() + " != " + objargs[i].getClass().getCanonicalName());
            }
            return api.getClass().getMethod(method, classargs).invoke(api, objargs);
        } else {
            throw new IllegalStateException("RedisBungee is not installed");
        }
    }

    /**
     * Further override BungeeCord's signature when patched into the same jar
     *
     * @return Software Name
     */
    @Override
    public String getName() {
        return (isPatched)?"SubServers.Sync":super.getName();
    }

    /**
     * Get the name from BungeeCord's original signature (for determining which fork is being used)
     *
     * @return BungeeCord Software Name
     */
    public String getBungeeName() {
        return super.getName();
    }

    /**
     * Emulate BungeeCord's getServers()
     *
     * @return Server Map
     */
    @Override
    public Map<String, ServerInfo> getServers() {
        if (servers.size() > 0) {
            HashMap<String, ServerInfo> servers = new HashMap<String, ServerInfo>();
            for (ServerInfo server : this.servers.values()) servers.put(server.getName(), server);
            return servers;
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
        Map<String, ServerInfo> servers = new TreeMap<String, ServerInfo>(this.servers);
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
        subdata.sendPacket(new PacketDownloadServerInfo(e.getServer(), data -> {
            switch (data.getRawString("type").toLowerCase()) {
                case "invalid":
                    System.out.println("PacketDownloadServerInfo(" + e.getServer() + ") returned with an invalid response");
                    break;
                case "subserver":
                    servers.put(data.getSection("server").getRawString("name").toLowerCase(), new SubServer(data.getSection("server").getRawString("signature"), data.getSection("server").getRawString("name"), data.getSection("server").getRawString("display"), new InetSocketAddress(data.getSection("server").getRawString("address").split(":")[0], Integer.parseInt(data.getSection("server").getRawString("address").split(":")[1])), data.getSection("server").getRawString("motd"), data.getSection("server").getBoolean("hidden"), data.getSection("server").getBoolean("restricted"), data.getSection("server").getBoolean("running")));
                    System.out.println("SubServers > Added SubServer: " + e.getServer());
                    break;
                default:
                    servers.put(data.getSection("server").getRawString("name").toLowerCase(), new Server(data.getSection("server").getRawString("signature"), data.getSection("server").getRawString("name"), data.getSection("server").getRawString("display"), new InetSocketAddress(data.getSection("server").getRawString("address").split(":")[0], Integer.parseInt(data.getSection("server").getRawString("address").split(":")[1])), data.getSection("server").getRawString("motd"), data.getSection("server").getBoolean("hidden"), data.getSection("server").getBoolean("restricted")));
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

    public Boolean merge(String name, YAMLSection data, boolean isSubServer) {
        Server server = servers.get(name.toLowerCase());
        if (server == null || isSubServer || !(server instanceof SubServer)) {
            if (server == null || !server.getSignature().equals(data.getRawString("signature"))) {
                if (isSubServer) {
                    servers.put(name.toLowerCase(), new SubServer(data.getRawString("signature"), name, data.getRawString("display"), new InetSocketAddress(data.getRawString("address").split(":")[0],
                            Integer.parseInt(data.getRawString("address").split(":")[1])), data.getRawString("motd"), data.getBoolean("hidden"), data.getBoolean("restricted"), data.getBoolean("running")));
                } else {
                    servers.put(name.toLowerCase(), new Server(data.getRawString("signature"), name, data.getRawString("display"), new InetSocketAddress(data.getRawString("address").split(":")[0],
                            Integer.parseInt(data.getRawString("address").split(":")[1])), data.getRawString("motd"), data.getBoolean("hidden"), data.getBoolean("restricted")));
                }

                System.out.println("SubServers > Added "+((isSubServer)?"Sub":"")+"Server: " + name);
                return true;
            } else {
                if (isSubServer) {
                    if (data.getBoolean("running") != ((SubServer) server).isRunning())
                        ((SubServer) server).setRunning(data.getBoolean("running"));
                }
                if (!data.getRawString("motd").equals(server.getMotd()))
                    server.setMotd(data.getRawString("motd"));
                if (data.getBoolean("hidden") != server.isHidden())
                    server.setHidden(data.getBoolean("hidden"));
                if (data.getBoolean("restricted") != server.isRestricted())
                    server.setRestricted(data.getBoolean("restricted"));
                if (!data.getRawString("display").equals(server.getDisplayName()))
                    server.setDisplayName(data.getRawString("display"));

                System.out.println("SubServers > Re-added "+((isSubServer)?"Sub":"")+"Server: " + name);
                return false;
            }
        }
        return null;
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

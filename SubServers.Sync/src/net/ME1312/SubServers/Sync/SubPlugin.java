package net.ME1312.SubServers.Sync;

import com.dosse.upnp.UPnP;
import com.google.gson.Gson;
import net.ME1312.SubServers.Sync.Event.*;
import net.ME1312.SubServers.Sync.Library.Config.YAMLConfig;
import net.ME1312.SubServers.Sync.Library.Config.YAMLSection;
import net.ME1312.SubServers.Sync.Library.Metrics;
import net.ME1312.SubServers.Sync.Library.NamedContainer;
import net.ME1312.SubServers.Sync.Library.UniversalFile;
import net.ME1312.SubServers.Sync.Library.Util;
import net.ME1312.SubServers.Sync.Library.Version.Version;
import net.ME1312.SubServers.Sync.Network.Cipher;
import net.ME1312.SubServers.Sync.Network.SubDataClient;
import net.ME1312.SubServers.Sync.Server.ServerContainer;
import net.ME1312.SubServers.Sync.Server.SubServerContainer;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.config.ListenerInfo;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.event.ServerKickEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
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
    public final Map<String, ServerContainer> servers = new TreeMap<String, ServerContainer>();

    public final PrintStream out;
    public final UniversalFile dir = new UniversalFile(new File(System.getProperty("user.dir")));
    public YAMLConfig config;
    public boolean redis = false;
    public final SubAPI api = new SubAPI(this);
    public SubDataClient subdata = null;
    public static final Version version = Version.fromString("2.13.2a");

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

            if (UPnP.isUPnPAvailable()) {
                if (config.get().getSection("Settings").getSection("UPnP", new YAMLSection()).getBoolean("Forward-Proxy", true)) for (ListenerInfo listener : getConfig().getListeners()) {
                    UPnP.openPortTCP(listener.getHost().getPort());
                }
            } else {
                getLogger().warning("UPnP is currently unavailable; Ports may not be automatically forwarded on this device");
            }

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
            @SuppressWarnings("unchecked")
            @Override
            public void run() {
                try {
                    YAMLSection tags = new YAMLSection(new Gson().fromJson("{\"tags\":" + Util.readAll(new BufferedReader(new InputStreamReader(new URL("https://api.github.com/repos/ME1312/SubServers-2/git/refs/tags").openStream(), Charset.forName("UTF-8")))) + '}', Map.class));
                    List<Version> versions = new LinkedList<Version>();

                    Version updversion = version;
                    int updcount = 0;
                    for (YAMLSection tag : tags.getSectionList("tags")) versions.add(Version.fromString(tag.getString("ref").substring(10)));
                    Collections.sort(versions);
                    for (Version version : versions) {
                        if (version.compareTo(updversion) > 0) {
                            updversion = version;
                            updcount++;
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
        return (isPatched)?"SubServers Platform":super.getName();
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

            for (ListenerInfo listener : getConfig().getListeners()) {
                if (UPnP.isUPnPAvailable() && UPnP.isMappedTCP(listener.getHost().getPort())) UPnP.closePortTCP(listener.getHost().getPort());
            }
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

    @SuppressWarnings("deprecation")
    @EventHandler(priority = Byte.MIN_VALUE)
    public void fallback(ServerKickEvent e) {
        if (e.getPlayer().getPendingConnection().getListener().isForceDefault()) {
            NamedContainer<Integer, ServerInfo> next = null;
            for (String name : e.getPlayer().getPendingConnection().getListener().getServerPriority()) {
                if (!e.getKickedFrom().getName().equalsIgnoreCase(name)) {
                    ServerInfo server = getServerInfo(name);
                    if (server != null) {
                        int confidence = 0;
                        if (server instanceof ServerContainer) {
                            if (!((ServerContainer) server).isHidden()) confidence++;
                            if (!((ServerContainer) server).isRestricted()) confidence++;
                            if (((ServerContainer) server).getSubData() != null) confidence++;

                            if (server instanceof SubServerContainer) {
                                if (((SubServerContainer) server).isRunning()) confidence++;
                            } else confidence++;
                        }

                        if (next == null || confidence > next.name())
                            next = new NamedContainer<Integer, ServerInfo>(confidence, server);
                    }
                }
            }

            if (next != null) {
                e.setCancelServer(next.get());
                e.setCancelled(true);
                e.getPlayer().sendMessage(api.getLang("SubServers", "Bungee.Feature.Return").replace("$str$", (next.get() instanceof ServerContainer)?((ServerContainer) next.get()).getDisplayName():next.get().getName()).replace("$msg$", e.getKickReason()));
            }
        }
    }

    @EventHandler(priority = Byte.MIN_VALUE)
    public void add(SubAddServerEvent e) {
        api.getServer(e.getServer(), server -> {
            if (server != null) {
                if (server instanceof net.ME1312.SubServers.Sync.Network.API.SubServer) {
                    servers.put(server.getName().toLowerCase(), new SubServerContainer(server.getSignature(), server.getName(), server.getDisplayName(), server.getAddress(),
                            server.getSubData(), server.getMotd(), server.isHidden(), server.isRestricted(), ((net.ME1312.SubServers.Sync.Network.API.SubServer) server).isRunning()));
                    System.out.println("SubServers > Added SubServer: " + e.getServer());
                } else {
                    servers.put(server.getName().toLowerCase(), new ServerContainer(server.getSignature(), server.getName(), server.getDisplayName(), server.getAddress(),
                            server.getSubData(), server.getMotd(), server.isHidden(), server.isRestricted()));
                    System.out.println("SubServers > Added Server: " + e.getServer());
                }
            } else System.out.println("PacketDownloadServerInfo(" + e.getServer() + ") returned with an invalid response");
        });
    }

    public Boolean merge(net.ME1312.SubServers.Sync.Network.API.Server server) {
        ServerContainer current = servers.get(server.getName().toLowerCase());
        if (current == null || server instanceof net.ME1312.SubServers.Sync.Network.API.SubServer || !(current instanceof SubServerContainer)) {
            if (current == null || !current.getSignature().equals(server.getSignature())) {
                if (server instanceof net.ME1312.SubServers.Sync.Network.API.SubServer) {
                    servers.put(server.getName().toLowerCase(), new SubServerContainer(server.getSignature(), server.getName(), server.getDisplayName(), server.getAddress(),
                            server.getSubData(), server.getMotd(), server.isHidden(), server.isRestricted(), ((net.ME1312.SubServers.Sync.Network.API.SubServer) server).isRunning()));
                } else {
                    servers.put(server.getName().toLowerCase(), new ServerContainer(server.getSignature(), server.getName(), server.getDisplayName(), server.getAddress(),
                            server.getSubData(), server.getMotd(), server.isHidden(), server.isRestricted()));
                }

                System.out.println("SubServers > Added "+((server instanceof net.ME1312.SubServers.Sync.Network.API.SubServer)?"Sub":"")+"Server: " + server.getName());
                return true;
            } else {
                if (server instanceof net.ME1312.SubServers.Sync.Network.API.SubServer) {
                    if (((net.ME1312.SubServers.Sync.Network.API.SubServer) server).isRunning() != ((SubServerContainer) current).isRunning())
                        ((SubServerContainer) current).setRunning(((net.ME1312.SubServers.Sync.Network.API.SubServer) server).isRunning());
                }
                if (!server.getMotd().equals(current.getMotd()))
                    current.setMotd(server.getMotd());
                if (server.isHidden() != current.isHidden())
                    current.setHidden(server.isHidden());
                if (server.isRestricted() != current.isRestricted())
                    current.setRestricted(server.isRestricted());
                if (!server.getDisplayName().equals(current.getDisplayName()))
                    current.setDisplayName(server.getDisplayName());

                System.out.println("SubServers > Re-added "+((server instanceof net.ME1312.SubServers.Sync.Network.API.SubServer)?"Sub":"")+"Server: " + server.getName());
                return false;
            }
        }
        return null;
    }

    @EventHandler(priority = Byte.MIN_VALUE)
    public void edit(SubEditServerEvent e) {
        if (servers.keySet().contains(e.getServer().toLowerCase())) {
            ServerContainer server = servers.get(e.getServer().toLowerCase());
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
    public void start(SubStartEvent e) {
        if (servers.keySet().contains(e.getServer().toLowerCase()) && servers.get(e.getServer().toLowerCase()) instanceof SubServerContainer)
            ((SubServerContainer) servers.get(e.getServer().toLowerCase())).setRunning(true);
    }

    public void connect(ServerContainer server, String address) {
        server.setSubData(address);
    }

    public void disconnect(ServerContainer server) {
        server.setSubData(null);
    }

    @EventHandler(priority = Byte.MIN_VALUE)
    public void stop(SubStoppedEvent e) {
        if (servers.keySet().contains(e.getServer().toLowerCase()) && servers.get(e.getServer().toLowerCase()) instanceof SubServerContainer)
            ((SubServerContainer) servers.get(e.getServer().toLowerCase())).setRunning(false);
    }

    @EventHandler(priority = Byte.MIN_VALUE)
    public void remove(SubRemoveServerEvent e) {
        if (servers.keySet().contains(e.getServer().toLowerCase()))
            servers.remove(e.getServer().toLowerCase());
            System.out.println("SubServers > Removed Server: " + e.getServer());
    }
}

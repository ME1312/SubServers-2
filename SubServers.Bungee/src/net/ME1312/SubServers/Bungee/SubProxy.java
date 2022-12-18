package net.ME1312.SubServers.Bungee;

import net.ME1312.Galaxi.Library.Config.YAMLConfig;
import net.ME1312.Galaxi.Library.Config.YAMLSection;
import net.ME1312.Galaxi.Library.Container.Container;
import net.ME1312.Galaxi.Library.Directories;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Try;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.SubData.Server.ClientHandler;
import net.ME1312.SubData.Server.Encryption.AES;
import net.ME1312.SubData.Server.Encryption.DHE;
import net.ME1312.SubData.Server.Encryption.RSA;
import net.ME1312.SubData.Server.SubDataClient;
import net.ME1312.SubData.Server.SubDataServer;
import net.ME1312.SubServers.Bungee.Event.SubRemoveProxyEvent;
import net.ME1312.SubServers.Bungee.Event.SubStoppedEvent;
import net.ME1312.SubServers.Bungee.Host.*;
import net.ME1312.SubServers.Bungee.Library.Compatibility.LegacyServerMap;
import net.ME1312.SubServers.Bungee.Library.Compatibility.Logger;
import net.ME1312.SubServers.Bungee.Library.ConfigUpdater;
import net.ME1312.SubServers.Bungee.Library.Exception.InvalidHostException;
import net.ME1312.SubServers.Bungee.Library.Exception.InvalidServerException;
import net.ME1312.SubServers.Bungee.Library.Fallback.FallbackState;
import net.ME1312.SubServers.Bungee.Library.Fallback.SmartFallback;
import net.ME1312.SubServers.Bungee.Library.Metrics;
import net.ME1312.SubServers.Bungee.Network.Packet.PacketExDisconnectPlayer;
import net.ME1312.SubServers.Bungee.Network.Packet.PacketExSyncPlayer;
import net.ME1312.SubServers.Bungee.Network.Packet.PacketLinkServer;
import net.ME1312.SubServers.Bungee.Network.Packet.PacketOutExReload;
import net.ME1312.SubServers.Bungee.Network.SubProtocol;

import com.dosse.upnp.UPnP;
import com.google.common.collect.Range;
import com.google.gson.Gson;
import io.netty.channel.Channel;
import net.md_5.bungee.BungeeServerInfo;
import net.md_5.bungee.UserConnection;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ListenerInfo;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.*;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginDescription;
import net.md_5.bungee.api.plugin.PluginManager;
import net.md_5.bungee.connection.InitialHandler;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.util.CaseInsensitiveMap;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Main Plugin Class
 */
public final class SubProxy extends BungeeCommon implements Listener {
    final LinkedHashMap<String, LinkedHashMap<String, String>> exLang = new LinkedHashMap<String, LinkedHashMap<String, String>>();
    final HashMap<String, Class<? extends Host>> hostDrivers = new HashMap<String, Class<? extends Host>>();
    public final HashMap<String, Proxy> proxies = new HashMap<String, Proxy>();
    public final HashMap<String, Host> hosts = new HashMap<String, Host>();
    public final HashMap<String, Server> exServers = new HashMap<String, Server>();
    private final HashMap<String, ServerInfo> legServers = new HashMap<String, ServerInfo>();
    public final HashMap<UUID, Server> rPlayerLinkS = new HashMap<UUID, Server>();
    public final HashMap<UUID, Proxy> rPlayerLinkP = new HashMap<UUID, Proxy>();
    public final HashMap<UUID, RemotePlayer> rPlayers = new HashMap<UUID, RemotePlayer>();
    private final HashMap<UUID, FallbackState> fallback = new HashMap<UUID, FallbackState>();

    public final PrintStream out;
    public final File dir = new File(System.getProperty("user.dir"));
    public YAMLConfig config;
    public YAMLConfig servers;
    private YAMLConfig bungee;
    public YAMLConfig lang;
    public final Plugin plugin;
    public final SubAPI api = new SubAPI(this);
    public SubProtocol subprotocol;
    public SubDataServer subdata = null;
    public SubServer sudo = null;
    public final Collection<Channel> listeners = super.listeners;
    public static final Version version = Version.fromString("2.19a");

    public final Proxy mProxy;
    public boolean canSudo = false;
    public final boolean isPatched;
    public long resetDate = 0;
    private boolean pluginDeployed = false;
    private boolean running = false;
    private boolean ready = false;
    private boolean reloading = false;
    private boolean posted = false;
    private LinkedList<String> autorun = null;
    private static BigInteger lastSignature = BigInteger.valueOf(-1);

    @SuppressWarnings("unchecked")
    SubProxy(PrintStream out, boolean isPatched) throws Exception {
        super(SubAPI::getInstance);
        this.isPatched = isPatched;

        Logger.get("SubServers").info("Loading SubServers.Bungee v" + version.toString() + " Libraries (for Minecraft " + api.getGameVersion()[api.getGameVersion().length - 1] + ")");
        Try.all.run(() -> new RemotePlayer(null)); // runs <clinit>

        this.out = out;
        if (!(new File(dir, "config.yml").exists())) {
            Util.copyFromJar(SubProxy.class.getClassLoader(), "net/ME1312/SubServers/Bungee/Library/Files/bungee.yml", new File(dir, "config.yml").getPath());
            YAMLConfig tmp = new YAMLConfig(new File("config.yml"));
            tmp.get().set("stats", UUID.randomUUID().toString());
            tmp.save();
            Logger.get("SubServers").info("Created ./config.yml");
        }
        bungee = new YAMLConfig(new File(dir, "config.yml"));

        File dir = new File(this.dir, "SubServers");
        dir.mkdir();

        ConfigUpdater.updateConfig(new File(dir, "config.yml"));
        config = new YAMLConfig(new File(dir, "config.yml"));

        ConfigUpdater.updateServers(new File(dir, "servers.yml"));
        servers = new YAMLConfig(new File(dir, "servers.yml"));

        ConfigUpdater.updateLang(new File(dir, "lang.yml"));
        lang = new YAMLConfig(new File(dir, "lang.yml"));

        if (!(new File(dir, "Templates").exists())) {
            new File(dir, "Templates").mkdirs();

            Directories.unzip(SubProxy.class.getResourceAsStream("/net/ME1312/SubServers/Bungee/Library/Files/Templates/vanilla.zip"), new File(dir, "Templates"));
            Logger.get("SubServers").info("Created ./SubServers/Templates/Vanilla");

            Directories.unzip(SubProxy.class.getResourceAsStream("/net/ME1312/SubServers/Bungee/Library/Files/Templates/spigot.zip"), new File(dir, "Templates"));
            Logger.get("SubServers").info("Created ./SubServers/Templates/Spigot");

            Directories.unzip(SubProxy.class.getResourceAsStream("/net/ME1312/SubServers/Bungee/Library/Files/Templates/purpur.zip"), new File(dir, "Templates"));
            Logger.get("SubServers").info("Created ./SubServers/Templates/Purpur");

            Directories.unzip(SubProxy.class.getResourceAsStream("/net/ME1312/SubServers/Bungee/Library/Files/Templates/forge.zip"), new File(dir, "Templates"));
            Logger.get("SubServers").info("Created ./SubServers/Templates/Forge");

            Directories.unzip(SubProxy.class.getResourceAsStream("/net/ME1312/SubServers/Bungee/Library/Files/Templates/sponge.zip"), new File(dir, "Templates"));
            Logger.get("SubServers").info("Created ./SubServers/Templates/Sponge");
        } else {
            long stamp = Math.round(Math.random() * 100000);
            Version tv = new Version("2.18a+");

            if (new File(dir, "Templates/Vanilla/template.yml").exists() && ((new YAMLConfig(new File(dir, "Templates/Vanilla/template.yml"))).get().getVersion("Version", tv)).compareTo(tv) != 0) {
                Files.move(new File(dir, "Templates/Vanilla").toPath(), new File(dir, "Templates/Vanilla." + stamp + ".x").toPath());
                Directories.unzip(SubProxy.class.getResourceAsStream("/net/ME1312/SubServers/Bungee/Library/Files/Templates/vanilla.zip"), new File(dir, "Templates"));
                Logger.get("SubServers").info("Updated ./SubServers/Templates/Vanilla");
            }
            if (new File(dir, "Templates/Spigot/template.yml").exists() && ((new YAMLConfig(new File(dir, "Templates/Spigot/template.yml"))).get().getVersion("Version", tv)).compareTo(tv) != 0) {
                Files.move(new File(dir, "Templates/Spigot").toPath(), new File(dir, "Templates/Spigot." + stamp + ".x").toPath());
                Directories.unzip(SubProxy.class.getResourceAsStream("/net/ME1312/SubServers/Bungee/Library/Files/Templates/spigot.zip"), new File(dir, "Templates"));
                Logger.get("SubServers").info("Updated ./SubServers/Templates/Spigot");
            }
            if (new File(dir, "Templates/Purpur/template.yml").exists() && ((new YAMLConfig(new File(dir, "Templates/Purpur/template.yml"))).get().getVersion("Version", tv)).compareTo(tv) != 0) {
                Files.move(new File(dir, "Templates/Purpur").toPath(), new File(dir, "Templates/Purpur." + stamp + ".x").toPath());
                Directories.unzip(SubProxy.class.getResourceAsStream("/net/ME1312/SubServers/Bungee/Library/Files/Templates/purpur.zip"), new File(dir, "Templates"));
                Logger.get("SubServers").info("Updated ./SubServers/Templates/Purpur");
            }
            if (new File(dir, "Templates/Forge/template.yml").exists() && ((new YAMLConfig(new File(dir, "Templates/Forge/template.yml"))).get().getVersion("Version", tv)).compareTo(tv) != 0) {
                Files.move(new File(dir, "Templates/Forge").toPath(), new File(dir, "Templates/Forge." + stamp + ".x").toPath());
                Directories.unzip(SubProxy.class.getResourceAsStream("/net/ME1312/SubServers/Bungee/Library/Files/Templates/forge.zip"), new File(dir, "Templates"));
                Logger.get("SubServers").info("Updated ./SubServers/Templates/Forge");
            }
            if (new File(dir, "Templates/Sponge/template.yml").exists() && ((new YAMLConfig(new File(dir, "Templates/Sponge/template.yml"))).get().getVersion("Version", tv)).compareTo(tv) != 0) {
                Files.move(new File(dir, "Templates/Sponge").toPath(), new File(dir, "Templates/Sponge." + stamp + ".x").toPath());
                Directories.unzip(SubProxy.class.getResourceAsStream("/net/ME1312/SubServers/Bungee/Library/Files/Templates/sponge.zip"), new File(dir, "Templates"));
                Logger.get("SubServers").info("Updated ./SubServers/Templates/Sponge");
            }
        }

        Runnable clean = () -> {
            try {
                if (new File(dir, "Recently Deleted").exists()) {
                    int f = new File(dir, "Recently Deleted").listFiles().length;
                    for (File file : new File(dir, "Recently Deleted").listFiles()) {
                        try {
                            if (file.isDirectory()) {
                                if (new File(dir, "Recently Deleted/" + file.getName() + "/info.json").exists()) {
                                    FileReader reader = new FileReader(new File(dir, "Recently Deleted/" + file.getName() + "/info.json"));
                                    YAMLSection info = new YAMLSection(new Gson().fromJson(Util.readAll(reader), Map.class));
                                    reader.close();
                                    if (info.contains("Timestamp")) {
                                        if (TimeUnit.MILLISECONDS.toDays(Calendar.getInstance().getTime().getTime() - info.getLong("Timestamp")) >= 7) {
                                            Directories.delete(file);
                                            f--;
                                            Logger.get("SubServers").info("Removed ./SubServers/Recently Deleted/" + file.getName());
                                        }
                                    } else {
                                        Directories.delete(file);
                                        f--;
                                        Logger.get("SubServers").info("Removed ./SubServers/Recently Deleted/" + file.getName());
                                    }
                                } else {
                                    Directories.delete(file);
                                    f--;
                                    Logger.get("SubServers").info("Removed ./SubServers/Recently Deleted/" + file.getName());
                                }
                            } else {
                                Files.delete(file.toPath());
                                f--;
                                Logger.get("SubServers").info("Removed ./SubServers/Recently Deleted/" + file.getName());
                            }
                        } catch (Exception e) {
                            Logger.get("SubServers").info("Problem scanning ./SubServers/Recently Deleted/" + file.getName());
                            e.printStackTrace();
                            Files.delete(file.toPath());
                        }
                    }
                    if (f <= 0) {
                        Files.delete(new File(dir, "Recently Deleted").toPath());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        };
        clean.run();
        new Timer("SubServers.Bungee::Recycle_Cleaner").scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                clean.run();
            }
        }, TimeUnit.DAYS.toMillis(7), TimeUnit.DAYS.toMillis(7));

        mProxy = new Proxy("(master)");
        api.addHostDriver(net.ME1312.SubServers.Bungee.Host.Internal.InternalHost.class, "virtual");
        api.addHostDriver(net.ME1312.SubServers.Bungee.Host.External.ExternalHost.class, "network");

        {
            PluginDescription description = new PluginDescription();
            description.setName("SubServers-Bungee");
            description.setMain(net.ME1312.SubServers.Bungee.Library.Compatibility.Plugin.class.getCanonicalName());
            description.setFile(Try.all.get(() -> new File(SubProxy.class.getProtectionDomain().getCodeSource().getLocation().toURI())));
            description.setVersion(version.toString());
            description.setAuthor("ME1312");

            Plugin plugin = null;
            String stage = "access";
            try {
                plugin = new Plugin(this, description) {
                    @Override
                    public void onEnable() {
                        try {
                            pluginDeployed = true;
                            reload();
                        } catch (Throwable e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onDisable() {
                        try {
                            shutdown();
                        } catch (Throwable e) {
                            e.printStackTrace();
                        }
                    }
                };

                if (plugin.getDescription() == null) {
                    stage = "initialize";
                    Util.reflect(Plugin.class.getDeclaredMethod("init", ProxyServer.class, PluginDescription.class), plugin, this, description);
                }

                stage = "deploy";
                Util.<Map<String, Plugin>>reflect(PluginManager.class.getDeclaredField("plugins"), getPluginManager()).put(null, plugin);
            } catch (Throwable e) {
                Logger.get("SubServers").warning("Could not " + stage + " plugin emulation");
            } finally {
                this.plugin = plugin;
            }
        }
        getPluginManager().registerListener(plugin, this);

        Logger.get("SubServers").info("Pre-Parsing Config...");
        for (String name : servers.get().getMap("Servers").getKeys()) {
            try {
                if (Util.getCaseInsensitively(config.get().getMap("Hosts").get(), servers.get().getMap("Servers").getMap(name).getString("Host")) == null) throw new InvalidServerException("There is no host with this name: " + servers.get().getMap("Servers").getMap(name).getString("Host"));
                legServers.put(name, constructServerInfo(name, new InetSocketAddress(InetAddress.getByName((String) ((Map<String, ?>) Util.getCaseInsensitively(config.get().getMap("Hosts").get(), servers.get().getMap("Servers").getMap(name).getString("Host"))).get("Address")), servers.get().getMap("Servers").getMap(name).getInt("Port")),
                        ChatColor.translateAlternateColorCodes('&', Util.unescapeJavaString(servers.get().getMap("Servers").getMap(name).getString("Motd"))), servers.get().getMap("Servers").getMap(name).getBoolean("Restricted")));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        SmartFallback.addInspector((player, server) -> {
            double confidence = 0;
            if (server instanceof Server) {
                if (!((Server) server).isHidden()) confidence++;
                if (!((Server) server).isRestricted()) confidence++;
                if (((Server) server).getSubData()[0] != null) confidence++;

                if (player != null) {
                    if (((Server) server).canAccess(player)) confidence++;
                }
            } if (server instanceof SubServer) {
                if (!((SubServer) server).isRunning()) return null;
            }
            return confidence;
        });

        if (config.get().getMap("Settings").getMap("Smart-Fallback", new ObjectMap<>()).getBoolean("Enabled", true))
            setReconnectHandler(new SmartFallback(config.get().getMap("Settings").getMap("Smart-Fallback", new ObjectMap<>())));

        subprotocol = SubProtocol.get();
        subprotocol.registerCipher("DHE", DHE.get(128));
        subprotocol.registerCipher("DHE-128", DHE.get(128));
        subprotocol.registerCipher("DHE-192", DHE.get(192));
        subprotocol.registerCipher("DHE-256", DHE.get(256));
        Logger.get("SubServers").info("Loading BungeeCord Libraries...");
    }

    /**
     * Load data from the config (will attempt to merge with current configuration)
     *
     * @throws IOException
     */
    public void reload() throws IOException {
        List<String> ukeys = new ArrayList<String>();
        long begin = Calendar.getInstance().getTime().getTime();
        boolean status = ready;
        if (!status) resetDate = begin;
        reloading = true;

        ConfigUpdater.updateConfig(new File(dir, "SubServers/config.yml"));
        ConfigUpdater.updateServers(new File(dir, "SubServers/servers.yml"));
        ConfigUpdater.updateLang(new File(dir, "SubServers/lang.yml"));

        YAMLSection prevconfig = config.get();
        config.reload();
        servers.reload();
        lang.reload();
        for (String key : lang.get().getMap("Lang").getKeys())
            api.setLang("SubServers", key, ChatColor.translateAlternateColorCodes('&', Util.unescapeJavaString(lang.get().getMap("Lang").getString(key))));

        if (subdata != null && ( // SubData Server must be reset
                !config.get().getMap("Settings").getMap("SubData").getString("Address", "127.0.0.1:4391").equals(prevconfig.getMap("Settings").getMap("SubData").getString("Address", "127.0.0.1:4391")) ||
                !config.get().getMap("Settings").getMap("SubData").getString("Encryption", "NONE").equals(prevconfig.getMap("Settings").getMap("SubData").getString("Encryption", "NONE"))
                )) {
            SubDataServer subdata = this.subdata;
            subdata.close();
            Try.all.run(subdata::waitFor);
        }

        PacketLinkServer.strict = config.get().getMap("Settings").getBoolean("Strict-Server-Linking", true);
        SmartFallback.dns_forward = config.get().getMap("Settings").getMap("Smart-Fallback", new ObjectMap<>()).getBoolean("DNS-Forward", false);

        int hosts = 0;
        Logger.get("SubServers").info(((status)?"Rel":"L")+"oading Hosts...");
        for (String name : config.get().getMap("Hosts").getKeys()) {
            if (!ukeys.contains(name.toLowerCase())) try {
                boolean add = false;
                Host host = this.hosts.get(name.toLowerCase());
                Class<? extends Host> driver = hostDrivers.get(config.get().getMap("Hosts").getMap(name).getString("Driver").toUpperCase().replace('-', '_').replace(' ', '_'));
                if (driver == null) throw new InvalidHostException("Invalid Driver for host: " + name);
                if (host == null || // Host must be reset
                        !hostDrivers.get(config.get().getMap("Hosts").getMap(name).getString("Driver").toUpperCase().replace('-', '_').replace(' ', '_')).equals(host.getClass()) ||
                        !config.get().getMap("Hosts").getMap(name).getString("Address").equals(host.getAddress().getHostAddress()) ||
                        !config.get().getMap("Hosts").getMap(name).getString("Directory").equals(host.getPath()) ||
                        !config.get().getMap("Hosts").getMap(name).getString("Git-Bash").equals(host.getCreator().getBashDirectory())
                        ) {
                    if (host != null) api.forceRemoveHost(name);
                    add = true;
                    host = constructHost(driver, name, config.get().getMap("Hosts").getMap(name).getBoolean("Enabled"),
                            Range.closed(Integer.parseInt(config.get().getMap("Hosts").getMap(name).getString("Port-Range", "25500-25559").split("-")[0]), Integer.parseInt(config.get().getMap("Hosts").getMap(name).getString("Port-Range", "25500-25559").split("-")[1])),
                            config.get().getMap("Hosts").getMap(name).getBoolean("Log-Creator", true), InetAddress.getByName(config.get().getMap("Hosts").getMap(name).getString("Address")),
                            config.get().getMap("Hosts").getMap(name).getString("Directory"), config.get().getMap("Hosts").getMap(name).getString("Git-Bash"));
                } else { // Host wasn't reset, so check for these changes
                    if (config.get().getMap("Hosts").getMap(name).getBoolean("Enabled") != host.isEnabled())
                        host.setEnabled(config.get().getMap("Hosts").getMap(name).getBoolean("Enabled"));
                    if (!config.get().getMap("Hosts").getMap(name).getString("Port-Range", "25500-25559").equals(prevconfig.getMap("Hosts", new ObjectMap<String>()).getMap(name, new ObjectMap<String>()).getString("Port-Range", "25500-25559")))
                        host.getCreator().setPortRange(Range.closed(Integer.parseInt(config.get().getMap("Hosts").getMap(name).getString("Port-Range", "25500-25559").split("-")[0]), Integer.parseInt(config.get().getMap("Hosts").getMap(name).getString("Port-Range", "25500-25559").split("-")[1])));
                    if (config.get().getMap("Hosts").getMap(name).getBoolean("Log-Creator", true) != host.getCreator().isLogging())
                        host.getCreator().setLogging(config.get().getMap("Hosts").getMap(name).getBoolean("Log-Creator", true));
                    host.getCreator().reload();
                } // Check for other changes
                if (config.get().getMap("Hosts").getMap(name).getKeys().contains("Display") && ((config.get().getMap("Hosts").getMap(name).getString("Display").length() == 0 && !host.getName().equals(host.getDisplayName())) || (config.get().getMap("Hosts").getMap(name).getString("Display").length() > 0 && !Util.unescapeJavaString(config.get().getMap("Hosts").getMap(name).getString("Display")).equals(host.getDisplayName()))))
                    host.setDisplayName(Util.unescapeJavaString(config.get().getMap("Hosts").getMap(name).getString("Display")));
                if (config.get().getMap("Hosts").getMap(name).getKeys().contains("Extra"))
                    for (String extra : config.get().getMap("Hosts").getMap(name).getMap("Extra").getKeys()) host.addExtra(extra, config.get().getMap("Hosts").getMap(name).getMap("Extra").getObject(extra));
                if (add)
                    api.addHost(host);
                ukeys.add(name.toLowerCase());
                hosts++;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        ukeys.clear();

        int servers = 0;
        Logger.get("SubServers").info(((status)?"Rel":"L")+"oading Servers...");
        bungee.reload();
        for (String name : bungee.get().getMap("servers").getKeys()) {
            if (!ukeys.contains(name.toLowerCase())) try {
                boolean add = false;
                Server server = api.getServer(name);
                if (server == null || !(server instanceof SubServer)) {
                    if (server == null || // Server must be reset
                            bungee.get().getMap("servers").getMap(name).getString("address").equals(server.getAddress().getAddress().getHostAddress() + ':' + server.getAddress().getPort())
                    ) {
                        if (server != null) api.forceRemoveServer(name);
                        add = true;
                        server = ServerImpl.construct(name, new InetSocketAddress(InetAddress.getByName(bungee.get().getMap("servers").getMap(name).getString("address").split(":")[0]),
                                Integer.parseInt(bungee.get().getMap("servers").getMap(name).getString("address").split(":")[1])), ChatColor.translateAlternateColorCodes('&', Util.unescapeJavaString(bungee.get().getMap("servers").getMap(name).getString("motd"))),
                                bungee.get().getMap("servers").getMap(name).getBoolean("hidden", false), bungee.get().getMap("servers").getMap(name).getBoolean("restricted"));
                    } else { // Server wasn't reset, so check for these changes
                        if (!ChatColor.translateAlternateColorCodes('&', Util.unescapeJavaString(bungee.get().getMap("servers").getMap(name).getString("motd"))).equals(server.getMotd()))
                            server.setMotd(ChatColor.translateAlternateColorCodes('&', Util.unescapeJavaString(bungee.get().getMap("servers").getMap(name).getString("motd"))));
                        if (bungee.get().getMap("servers").getMap(name).getBoolean("hidden", false) != server.isHidden())
                            server.setHidden(bungee.get().getMap("servers").getMap(name).getBoolean("hidden", false));
                        if (bungee.get().getMap("servers").getMap(name).getBoolean("restricted") != server.isRestricted())
                            server.setRestricted(bungee.get().getMap("servers").getMap(name).getBoolean("restricted"));
                    } // Check for other changes
                    if (bungee.get().getMap("servers").getMap(name).getKeys().contains("display") && ((bungee.get().getMap("servers").getMap(name).getString("display").length() == 0 && !server.getName().equals(server.getDisplayName())) || (bungee.get().getMap("servers").getMap(name).getString("display").length() > 0 && !bungee.get().getMap("servers").getMap(name).getString("display").equals(server.getDisplayName()))))
                        server.setDisplayName(Util.unescapeJavaString(bungee.get().getMap("servers").getMap(name).getString("display")));
                    if (bungee.get().getMap("servers").getMap(name).getKeys().contains("group")) {
                        for (String group : server.getGroups()) server.removeGroup(group);
                        for (String group : bungee.get().getMap("servers").getMap(name).getStringList("group")) server.addGroup(group);
                    }
                    if (bungee.get().getMap("servers").getMap(name).getKeys().contains("extra"))
                        for (String extra : config.get().getMap("servers").getMap(name).getMap("extra").getKeys()) server.addExtra(extra, config.get().getMap("servers").getMap(name).getMap("extra").getObject(extra));
                    if (server.getSubData()[0] != null)
                        ((SubDataClient) server.getSubData()[0]).sendPacket(new PacketOutExReload(null));
                    if (add)
                        api.addServer(server);
                    ukeys.add(name.toLowerCase());
                    servers++;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        ukeys.clear();

        int subservers = 0;
        Logger.get("SubServers").info(((status)?"Rel":"L")+"oading SubServers...");
        autorun = new LinkedList<String>();
        for (String name : this.servers.get().getMap("Servers").getKeys()) {
            if (!ukeys.contains(name.toLowerCase())) try {
                if (!this.hosts.containsKey(this.servers.get().getMap("Servers").getMap(name).getString("Host").toLowerCase())) throw new InvalidServerException("There is no host with this name: " + this.servers.get().getMap("Servers").getMap(name).getString("Host"));
                if (exServers.containsKey(name.toLowerCase())) {
                    exServers.remove(name.toLowerCase());
                    servers--;
                }
                boolean add = false;
                SubServer server = api.getSubServer(name);
                if (server != null && server.isEditable()) { // Server can edit() (May be reset depending on change severity)
                    ObjectMap<String> edits = new ObjectMap<String>();
                    if (this.servers.get().getMap("Servers").getMap(name).getBoolean("Enabled") != server.isEnabled())
                        edits.set("enabled", this.servers.get().getMap("Servers").getMap(name).getBoolean("Enabled"));
                    if (this.servers.get().getMap("Servers").getMap(name).getKeys().contains("Display") && ((this.servers.get().getMap("Servers").getMap(name).getString("Display").length() == 0 && !server.getName().equals(server.getDisplayName())) || (this.servers.get().getMap("Servers").getMap(name).getString("Display").length() > 0 && !Util.unescapeJavaString(this.servers.get().getMap("Servers").getMap(name).getString("Display")).equals(server.getDisplayName()))))
                        edits.set("display", Util.unescapeJavaString(this.servers.get().getMap("Servers").getMap(name).getString("Display")));
                    if (!this.servers.get().getMap("Servers").getMap(name).getString("Host").equalsIgnoreCase(server.getHost().getName()))
                        edits.set("host", this.servers.get().getMap("Servers").getMap(name).getString("Host"));
                    if (this.servers.get().getMap("Servers").getMap(name).getKeys().contains("Template") && ((this.servers.get().getMap("Servers").getMap(name).getString("Template").length() == 0 && server.getTemplate() != null) || (this.servers.get().getMap("Servers").getMap(name).getString("Template").length() > 0 && server.getTemplate() == null) || (server.getTemplate() != null && !this.servers.get().getMap("Servers").getMap(name).getString("Template").equalsIgnoreCase(server.getTemplate().getName()))))
                        edits.set("template", this.servers.get().getMap("Servers").getMap(name).getString("Template"));
                    if (!this.servers.get().getMap("Servers").getMap(name).getStringList("Group").equals(server.getGroups()))
                        edits.set("group", this.servers.get().getMap("Servers").getMap(name).getStringList("Group"));
                    if (this.servers.get().getMap("Servers").getMap(name).getInt("Port") != server.getAddress().getPort())
                        edits.set("port", this.servers.get().getMap("Servers").getMap(name).getInt("Port"));
                    if (!(ChatColor.translateAlternateColorCodes('&', Util.unescapeJavaString(this.servers.get().getMap("Servers").getMap(name).getString("Motd"))).equals(server.getMotd())))
                        edits.set("motd", this.servers.get().getMap("Servers").getMap(name).getString("Motd"));
                    if (this.servers.get().getMap("Servers").getMap(name).getBoolean("Log") != server.isLogging())
                        edits.set("log", this.servers.get().getMap("Servers").getMap(name).getBoolean("Log"));
                    if (!this.servers.get().getMap("Servers").getMap(name).getString("Directory").equals(server.getPath()))
                        edits.set("dir", this.servers.get().getMap("Servers").getMap(name).getString("Directory"));
                    if (!this.servers.get().getMap("Servers").getMap(name).getString("Executable").equals(server.getExecutable()))
                        edits.set("exec", this.servers.get().getMap("Servers").getMap(name).getString("Executable"));
                    if (!this.servers.get().getMap("Servers").getMap(name).getString("Stop-Command").equals(server.getStopCommand()))
                        edits.set("stop-cmd", this.servers.get().getMap("Servers").getMap(name).getString("Stop-Command"));
                    SubServer.StopAction action = Try.all.get(() -> SubServer.StopAction.valueOf(this.servers.get().getMap("Servers").getMap(name).getString("Stop-Action", "NONE").toUpperCase().replace('-', '_').replace(' ', '_')));
                    if (action != null && action != server.getStopAction())
                        edits.set("stop-action", action.toString());
                    if (this.servers.get().getMap("Servers").getMap(name).getBoolean("Restricted") != server.isRestricted())
                        edits.set("restricted", this.servers.get().getMap("Servers").getMap(name).getBoolean("Restricted"));
                    if (this.servers.get().getMap("Servers").getMap(name).getBoolean("Hidden") != server.isHidden())
                        edits.set("hidden", this.servers.get().getMap("Servers").getMap(name).getBoolean("Hidden"));


                    if (edits.getKeys().size() > 0) {
                        server.edit(edits);
                        server = api.getSubServer(name);
                    }
                } else { // Server cannot edit()
                    if (server == null ||  // Server must be reset
                            !this.servers.get().getMap("Servers").getMap(name).getString("Host").equalsIgnoreCase(server.getHost().getName()) ||
                            this.servers.get().getMap("Servers").getMap(name).getInt("Port") != server.getAddress().getPort() ||
                            !this.servers.get().getMap("Servers").getMap(name).getString("Directory").equals(server.getPath()) ||
                            !this.servers.get().getMap("Servers").getMap(name).getString("Executable").equals(server.getExecutable())
                            ) {
                            if (server != null) server.getHost().forceRemoveSubServer(name);
                            add = true;
                            server = this.hosts.get(this.servers.get().getMap("Servers").getMap(name).getString("Host").toLowerCase()).constructSubServer(name, this.servers.get().getMap("Servers").getMap(name).getBoolean("Enabled"),
                                    this.servers.get().getMap("Servers").getMap(name).getInt("Port"), ChatColor.translateAlternateColorCodes('&', Util.unescapeJavaString(this.servers.get().getMap("Servers").getMap(name).getString("Motd"))), this.servers.get().getMap("Servers").getMap(name).getBoolean("Log"),
                                    this.servers.get().getMap("Servers").getMap(name).getString("Directory"), this.servers.get().getMap("Servers").getMap(name).getString("Executable"), this.servers.get().getMap("Servers").getMap(name).getString("Stop-Command"),
                                    this.servers.get().getMap("Servers").getMap(name).getBoolean("Hidden"), this.servers.get().getMap("Servers").getMap(name).getBoolean("Restricted"));
                    } else { // Server doesn't need to reset
                        if (this.servers.get().getMap("Servers").getMap(name).getBoolean("Enabled") != server.isEnabled())
                            server.setEnabled(this.servers.get().getMap("Servers").getMap(name).getBoolean("Enabled"));
                        if (!ChatColor.translateAlternateColorCodes('&', Util.unescapeJavaString(this.servers.get().getMap("Servers").getMap(name).getString("Motd"))).equals(server.getMotd()))
                            server.setMotd(ChatColor.translateAlternateColorCodes('&', Util.unescapeJavaString(this.servers.get().getMap("Servers").getMap(name).getString("Motd"))));
                        if (this.servers.get().getMap("Servers").getMap(name).getBoolean("Log") != server.isLogging())
                            server.setLogging(this.servers.get().getMap("Servers").getMap(name).getBoolean("Log"));
                        if (!this.servers.get().getMap("Servers").getMap(name).getString("Stop-Command").equals(server.getStopCommand()))
                            server.setStopCommand(this.servers.get().getMap("Servers").getMap(name).getString("Stop-Command"));
                        if (this.servers.get().getMap("Servers").getMap(name).getBoolean("Restricted") != server.isRestricted())
                            server.setRestricted(this.servers.get().getMap("Servers").getMap(name).getBoolean("Restricted"));
                        if (this.servers.get().getMap("Servers").getMap(name).getBoolean("Hidden") != server.isHidden())
                            server.setHidden(this.servers.get().getMap("Servers").getMap(name).getBoolean("Hidden"));
                    } // Apply these changes regardless of reset
                    SubServer.StopAction action = Try.all.get(() -> SubServer.StopAction.valueOf(this.servers.get().getMap("Servers").getMap(name).getString("Stop-Action", "NONE").toUpperCase().replace('-', '_').replace(' ', '_')));
                    if (action != null && action != server.getStopAction())
                        server.setStopAction(action);
                    if (!status && this.servers.get().getMap("Servers").getMap(name).getBoolean("Run-On-Launch"))
                        autorun.add(name.toLowerCase());
                    if (this.servers.get().getMap("Servers").getMap(name).getKeys().contains("Display") && ((this.servers.get().getMap("Servers").getMap(name).getString("Display").length() == 0 && !server.getName().equals(server.getDisplayName())) || (this.servers.get().getMap("Servers").getMap(name).getString("Display").length() > 0 && !Util.unescapeJavaString(this.servers.get().getMap("Servers").getMap(name).getString("Display")).equals(server.getDisplayName()))))
                        server.setDisplayName(Util.unescapeJavaString(this.servers.get().getMap("Servers").getMap(name).getString("Display")));
                    if (this.servers.get().getMap("Servers").getMap(name).getKeys().contains("Template") && ((this.servers.get().getMap("Servers").getMap(name).getString("Template").length() == 0 && server.getTemplate() != null) || (this.servers.get().getMap("Servers").getMap(name).getString("Template").length() > 0 && server.getTemplate() == null) || (server.getTemplate() != null && !this.servers.get().getMap("Servers").getMap(name).getString("Template").equalsIgnoreCase(server.getTemplate().getName()))))
                        server.setTemplate(this.servers.get().getMap("Servers").getMap(name).getString("Template"));
                    if (this.servers.get().getMap("Servers").getMap(name).getKeys().contains("Group")) {
                        for (String group : server.getGroups()) server.removeGroup(group);
                        for (String group : this.servers.get().getMap("Servers").getMap(name).getStringList("Group")) server.addGroup(group);
                    }
                } // Apply these changes regardless of edit/reset
                if (this.servers.get().getMap("Servers").getMap(name).getKeys().contains("Extra")) for (String extra : this.servers.get().getMap("Servers").getMap(name).getMap("Extra").getKeys()) server.addExtra(extra, this.servers.get().getMap("Servers").getMap(name).getMap("Extra").getObject(extra));
                if (add) server.getHost().addSubServer(server);
                ukeys.add(name.toLowerCase());
                subservers++;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        for (String name : ukeys) {
            SubServer server = api.getSubServer(name);
            for (String oname : this.servers.get().getMap("Servers").getMap(server.getName()).getStringList("Incompatible", new ArrayList<>())) {
                SubServer oserver = api.getSubServer(oname);
                if (oserver != null && server.isCompatible(oserver)) server.toggleCompatibility(oserver);
            }
        }
        ukeys.clear();

        if (!posted) Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            shutdown = true;
            shutdown();
        }, "SubServers.Bungee::System_Shutdown"));
        running = ready = true;
        legServers.clear();

        int plugins = 0;
        if (status) {
            List<Runnable> listeners = api.reloadListeners;
            if (listeners.size() > 0) {
                Logger.get("SubServers").info("Reloading SubAPI Plugins...");
                for (Runnable obj : listeners) {
                    try {
                        obj.run();
                        plugins++;
                    } catch (Throwable e) {
                        new InvocationTargetException(e, "Problem " + ((status)?"reloading":"enabling") + " plugin").printStackTrace();
                    }
                }
            }

            for (Host host : api.getHosts().values()) if (host instanceof ClientHandler && ((ClientHandler) host).getSubData()[0] != null) ((SubDataClient) ((ClientHandler) host).getSubData()[0]).sendPacket(new PacketOutExReload(null));
            for (Server server : api.getServers().values()) if (server.getSubData()[0] != null) ((SubDataClient) server.getSubData()[0]).sendPacket(new PacketOutExReload(null));
        }

        reloading = false;
        Logger.get("SubServers").info(((plugins > 0)?plugins+" Plugin"+((plugins == 1)?"":"s")+", ":"") + hosts + " Host"+((hosts == 1)?"":"s")+", " + servers + " Server"+((servers == 1)?"":"s")+", and " + subservers + " SubServer"+((subservers == 1)?"":"s")+" "+((status)?"re":"")+"loaded in " + new DecimalFormat("0.000").format((Calendar.getInstance().getTime().getTime() - begin) / 1000D) + "s");
        if (status) startDataListeners();
    }
	
	private void startDataListeners() throws IOException {
        if (subdata == null) {
            subprotocol.unregisterCipher("AES");
            subprotocol.unregisterCipher("AES-128");
            subprotocol.unregisterCipher("AES-192");
            subprotocol.unregisterCipher("AES-256");
            subprotocol.unregisterCipher("RSA");

            subprotocol.setTimeout(TimeUnit.SECONDS.toMillis(config.get().getMap("Settings").getMap("SubData").getInt("Timeout", 30)));

            String cipher = config.get().getMap("Settings").getMap("SubData").getString("Encryption", "NULL");
            String[] ciphers = (cipher.contains("/"))?cipher.split("/"):new String[]{cipher};

            if (ciphers[0].equals("AES") || ciphers[0].equals("AES-128") || ciphers[0].equals("AES-192") || ciphers[0].equals("AES-256")) {
                if (config.get().getMap("Settings").getMap("SubData").getString("Password", "").length() == 0) {
                    byte[] bytes = new byte[32];
                    new SecureRandom().nextBytes(bytes);
                    String random = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
                    if (random.length() > bytes.length) random = random.substring(0, bytes.length);
                    config.get().getMap("Settings").getMap("SubData").set("Password", random);
                    config.save();
                }

                subprotocol.registerCipher("AES", new AES(128, config.get().getMap("Settings").getMap("SubData").getString("Password")));
                subprotocol.registerCipher("AES-128", new AES(128, config.get().getMap("Settings").getMap("SubData").getString("Password")));
                subprotocol.registerCipher("AES-192", new AES(192, config.get().getMap("Settings").getMap("SubData").getString("Password")));
                subprotocol.registerCipher("AES-256", new AES(256, config.get().getMap("Settings").getMap("SubData").getString("Password")));

                Logger.get("SubData").info("Encrypting SubData with AES:");
                Logger.get("SubData").info("Use the password field in config.yml to allow clients to connect");
            } else if (ciphers[0].equals("DHE") || ciphers[0].equals("DHE-128") || ciphers[0].equals("DHE-192") || ciphers[0].equals("DHE-256")) {

                Logger.get("SubData").info("Encrypting SubData with DHE/AES:");
                Logger.get("SubData").info("SubData will negotiate what password to use automatically using the Diffie-Hellman Exchange");
            } else if (ciphers[0].equals("RSA") || ciphers[0].equals("RSA-2048") || ciphers[0].equals("RSA-3072") || ciphers[0].equals("RSA-4096")) {
                try {
                    int length = (ciphers[0].contains("-"))?Integer.parseInt(ciphers[0].split("-")[1]):2048;
                    if (!(new File("SubServers/Cache").exists())) new File("SubServers/Cache").mkdirs();
                    subprotocol.registerCipher("RSA", new RSA(length, new File("SubServers/Cache/private.rsa.key"), new File("SubServers/subdata.rsa.key")));
                    cipher = "RSA" + cipher.substring(ciphers[0].length());

                    Logger.get("SubData").info("Encrypting SubData with RSA:");
                    Logger.get("SubData").info("Copy your subdata.rsa.key to clients to allow them to connect");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            Logger.get("SubData").info("");
            subdata = subprotocol.open((config.get().getMap("Settings").getMap("SubData").getString("Address", "127.0.0.1:4391").split(":")[0].equals("0.0.0.0"))?
                            null:InetAddress.getByName(config.get().getMap("Settings").getMap("SubData").getString("Address", "127.0.0.1:4391").split(":")[0]),
                    Integer.parseInt(config.get().getMap("Settings").getMap("SubData").getString("Address", "127.0.0.1:4391").split(":")[1]), cipher);
        }

        // Add new entries to Allowed-Connections
        for (String s : config.get().getMap("Settings").getMap("SubData").getStringList("Whitelist", new ArrayList<String>())) {
            try {
                subdata.whitelist(s);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void startListeners() {
        try {
            if (posted || !ready) reload();

            synchronized (rPlayers) {
                for (ProxiedPlayer local : getPlayers()) {
                    RemotePlayer player = new RemotePlayer(local);
                    rPlayerLinkP.put(player.getUniqueId(), player.getProxy());
                    rPlayers.put(player.getUniqueId(), player);
                    if (player.getServer() != null) rPlayerLinkS.put(player.getUniqueId(), player.getServer());
                }
            }

            if (UPnP.isUPnPAvailable()) {
                if (config.get().getMap("Settings").getMap("UPnP", new ObjectMap<String>()).getBoolean("Forward-Proxy", true)) for (ListenerInfo listener : getConfig().getListeners()) {
                    UPnP.openPortTCP(listener.getHost().getPort());
                }
            } else {
                getLogger().warning("UPnP service is unavailable. SubServers can't port-forward for you from this device.");
            }

            startDataListeners();
            super.startListeners();

            if (autorun != null && autorun.size() > 0) {
                final long begin = resetDate;
                final long scd = TimeUnit.SECONDS.toMillis(this.servers.get().getMap("Settings").getLong("Run-On-Launch-Timeout", 0L));
                for (Host host : api.getHosts().values()) {
                    List<String> ar = new LinkedList<String>();
                    for (String name : autorun) if (host.getSubServer(name) != null) ar.add(name);
                    if (ar.size() > 0) new Thread(() -> {
                        try {
                            while (ready && begin == resetDate && !host.isAvailable()) {
                                Thread.sleep(250);
                            }
                            long init = Calendar.getInstance().getTime().getTime();
                            while (ready && begin == resetDate && ar.size() > 0) {
                                SubServer server = host.getSubServer(ar.get(0));
                                ar.remove(0);
                                if (server != null && !server.isRunning()) {
                                    server.start();
                                    if (ar.size() > 0 && scd > 0) {
                                        long sleep = Calendar.getInstance().getTime().getTime();
                                        while (ready && begin == resetDate && server.getSubData()[0] == null && Calendar.getInstance().getTime().getTime() - sleep < scd) {
                                            Thread.sleep(250);
                                        }
                                    }
                                }
                            }
                            if (ready && begin == resetDate && Calendar.getInstance().getTime().getTime() - init >= 5000)
                                Logger.get("SubServers").info("The auto-start queue for " + host.getName() + " has been finished");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }, "SubServers.Bungee::Automatic_Server_Starter(" + host.getName() + ")").start();
                }
            }
            autorun = null;

            if (!posted) {
                posted = true;
                post();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void post() {
        if (!config.get().getMap("Settings").getStringList("Disabled-Overrides", Collections.emptyList()).contains("/server"))
            getPluginManager().registerCommand(plugin, new SubCommand.BungeeServer(this, "server"));
        if (!config.get().getMap("Settings").getStringList("Disabled-Overrides", Collections.emptyList()).contains("/glist"))
            getPluginManager().registerCommand(plugin, new SubCommand.BungeeList(this, "glist"));

        registerChannel("subservers:input");
        getPluginManager().registerCommand(plugin, new SubCommand(this, "subservers"));
        getPluginManager().registerCommand(plugin, new SubCommand(this, "subserver"));
        getPluginManager().registerCommand(plugin, new SubCommand(this, "sub"));

        if (getReconnectHandler() != null && getReconnectHandler().getClass().equals(SmartFallback.class))
            setReconnectHandler(new SmartFallback(config.get().getMap("Settings").getMap("Smart-Fallback", new ObjectMap<>()))); // Re-initialize Smart Fallback

        if (plugin != null) Try.none.run(() -> new Metrics(plugin, 1406)
                .addCustomChart(new Metrics.SingleLineChart("managed_hosts", () -> {
                    return hosts.size();
                })).addCustomChart(new Metrics.SingleLineChart("subdata_connected", () -> {
                    final SubDataServer subdata = this.subdata;
                    return (subdata != null)? subdata.getClients().size() : 0;
                })).addCustomChart(Util.reflect(Metrics.class.getDeclaredField("PLAYER_VERSIONS"), null))
        );
        new Timer("SubServers.Bungee::Routine_Update_Check").schedule(new TimerTask() {
            @SuppressWarnings("unchecked")
            @Override
            public void run() {
                try {
                    ObjectMap<String> tags = new ObjectMap<String>(new Gson().fromJson("{\"tags\":" + Util.readAll(new BufferedReader(new InputStreamReader(new URL("https://api.github.com/repos/ME1312/SubServers-2/git/refs/tags").openStream(), Charset.forName("UTF-8")))) + '}', Map.class));
                    List<Version> versions = new LinkedList<Version>();

                    Version updversion = version;
                    int updcount = 0;
                    for (ObjectMap<String> tag : tags.getMapList("tags")) versions.add(Version.fromString(tag.getString("ref").substring(10)));
                    Collections.sort(versions);
                    for (Version version : versions) {
                        if (version.compareTo(updversion) > 0) {
                            updversion = version;
                            updcount++;
                        }
                    }
                    if (updcount > 0) Logger.get("SubServers").info("SubServers.Bungee v" + updversion + " is available. You are " + updcount + " version" + ((updcount == 1)?"":"s") + " behind.");
                } catch (Exception e) {}
            }
        }, 0, TimeUnit.DAYS.toMillis(2));

        int rpec_i = config.get().getMap("Settings").getInt("RPEC-Check-Interval", 300);
        int rpec_s = rpec_i - new Random().nextInt((rpec_i / 3) + 1);
        new Timer("SubServers.Bungee::RemotePlayer_Error_Checking").schedule(new TimerTask() {
            @Override
            public void run() {
                synchronized (rPlayers) {
                    ArrayList<RemotePlayer> add = new ArrayList<RemotePlayer>();
                    for (ProxiedPlayer player : getPlayers()) {
                        if (!rPlayers.containsKey(player.getUniqueId())) { // Add players that don't exist
                            RemotePlayer p = new RemotePlayer(player);
                            rPlayerLinkP.put(player.getUniqueId(), p.getProxy());
                            rPlayers.put(player.getUniqueId(), p);
                            if (p.getServer() != null) rPlayerLinkS.put(player.getUniqueId(), p.getServer());
                            add.add(p);
                        }
                    }
                    ArrayList<RemotePlayer> remove = new ArrayList<RemotePlayer>();
                    for (UUID player : Util.getBackwards(rPlayerLinkP, mProxy)) { // Remove players that shouldn't exist
                        if (getPlayer(player) == null) {
                            remove.add(rPlayers.get(player));
                            rPlayerLinkS.remove(player);
                            rPlayerLinkP.remove(player);
                            rPlayers.remove(player);
                        }
                    }
                    LinkedList<PacketExSyncPlayer> packets = new LinkedList<PacketExSyncPlayer>(); // Compile change data for external proxies
                    if (add.size() > 0) packets.add(new PacketExSyncPlayer(mProxy.getName(), true, add.toArray(new RemotePlayer[0])));
                    if (remove.size() > 0) packets.add(new PacketExSyncPlayer(mProxy.getName(), false, remove.toArray(new RemotePlayer[0])));
                    if (packets.size() > 0) {
                        PacketExSyncPlayer[] packet = packets.toArray(new PacketExSyncPlayer[0]);
                        for (Proxy proxy : SubAPI.getInstance().getProxies().values()) if (proxy.getSubData()[0] != null) {
                            ((SubDataClient) proxy.getSubData()[0]).sendPacket(packet);
                        }
                    }
                }
            }
        }, TimeUnit.SECONDS.toMillis(rpec_s), TimeUnit.SECONDS.toMillis(rpec_i));
    }

    @Override
    public void stopListeners() {
        if (ready) {
            if (pluginDeployed) {
                shutdown = !super.isRunning;
                super.isRunning = true;
            }

            ListenerInfo[] listeners = getConfig().getListeners().toArray(new ListenerInfo[0]);
            super.stopListeners();

            if (UPnP.isUPnPAvailable()) {
                for (ListenerInfo listener : listeners) {
                    if (UPnP.isMappedTCP(listener.getHost().getPort())) UPnP.closePortTCP(listener.getHost().getPort());
                }
            }
        }
    }

    private boolean shutdown = false;
    void shutdown() {
        if (ready) {
            legServers.clear();
            legServers.putAll(getServersCopy());
            ready = false;

            Logger.get("SubServers").info("Stopping hosted servers");
            String[] hosts = this.hosts.keySet().toArray(new String[0]);
            if (shutdown) running = false;
            for (String host : hosts) {
                api.forceRemoveHost(host);
            }

            Logger.get("SubServers").info("Removing dynamic data");
            exServers.clear();
            this.hosts.clear();

            String[] proxies = this.proxies.keySet().toArray(new String[0]);
            for (String proxy : proxies) {
                getPluginManager().callEvent(new SubRemoveProxyEvent(this.proxies.get(proxy)));
            }
            this.proxies.clear();
            rPlayerLinkS.clear();
            rPlayerLinkP.clear();
            rPlayers.clear();

            if (subdata != null) try {
                SubDataServer subdata = this.subdata;
                subdata.close();
                subdata.waitFor();
            } catch (InterruptedException | IOException e) {}

            if (shutdown) super.isRunning = false;
        }
    }

    String getNewSignature() {
        BigInteger number = (lastSignature = lastSignature.add(BigInteger.ONE));
        final String DIGITS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz-_";
        final BigInteger BASE = BigInteger.valueOf(DIGITS.length());

        StringBuilder result = new StringBuilder();
        while (number.compareTo(BigInteger.ZERO) > 0) { // number > 0
            BigInteger[] divmod = number.divideAndRemainder(BASE);
            number = divmod[0];
            int digit = divmod[1].intValue();
            result.insert(0, DIGITS.charAt(digit));
        }
        return (result.length() == 0) ? DIGITS.substring(0, 1) : result.toString();
    }

    Host constructHost(Class<? extends Host> driver, String name, boolean enabled, Range<Integer> ports, boolean log, InetAddress address, String directory, String gitBash) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Util.nullpo(driver, name, enabled, ports, log, address, directory, gitBash);
        return driver.getConstructor(SubProxy.class, String.class, boolean.class, Range.class, boolean.class, InetAddress.class, String.class, String.class).newInstance(this, name, enabled, ports, log, address, directory, gitBash);
    }

    /**
     * Further override BungeeCord's signature when patched into the same jar
     *
     * @return Software Name
     */
    @Override
    public String getName() {
        return (isPatched)?"SubServers.Bungee":super.getName();
    }

    /**
     * Get the name from BungeeCord's original signature (for determining which fork is being used)
     *
     * @return BungeeCord Software Name
     */
    @Override
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
        return new LegacyServerMap(getServersCopy());
    }

    /**
     * Emulate Waterfall's getServersCopy()
     *
     * @return Server Map Copy
     */
    @Override
    public Map<String, ServerInfo> getServersCopy() {
        Map<String, ServerInfo> servers = new CaseInsensitiveMap<ServerInfo>();
        if (!ready) {
            servers.putAll(super.getServers());
            servers.putAll(legServers);
        } else {
            for (ServerInfo server : exServers.values()) servers.put(server.getName(), server);
            for (Host host : this.hosts.values()) {
                for (ServerInfo server : host.getSubServers().values()) servers.put(server.getName(), server);
            }
        }
        return servers;
    }

    /**
     * Force BungeeCord's implementation of getServerInfo()
     *
     * @return ServerInfo
     */
    @Override
    public ServerInfo getServerInfo(String name) {
        if (!ready) {
            return getServersCopy().get(name);
        } else {
            return api.getServer(name);
        }
    }

    @EventHandler(priority = Byte.MIN_VALUE)
    public void ping_passthrough(ProxyPingEvent e) {
        boolean dynamic;
        ServerInfo override;
        if ((dynamic = SmartFallback.getForcedHost(e.getConnection()) == null) && getReconnectHandler() instanceof SmartFallback && (override = SmartFallback.getDNS(e.getConnection())) != null) {
            if (!(override instanceof SubServer) || ((SubServer) override).isRunning()) {
                if (!e.getConnection().getListener().isPingPassthrough()) {
                    e.setResponse(new ServerPing(e.getResponse().getVersion(), e.getResponse().getPlayers(), new TextComponent(override.getMotd()), e.getResponse().getFaviconObject()));
                } else {
                    Container<Boolean> lock = new Container<>(true);
                    boolean mode = plugin != null;
                    if (mode) e.registerIntent(plugin);
                    ((BungeeServerInfo) override).ping((ping, error) -> {
                        if (error != null) {
                            e.setResponse(new ServerPing(e.getResponse().getVersion(), e.getResponse().getPlayers(), new TextComponent(getTranslation("ping_cannot_connect")), e.getResponse().getFaviconObject()));
                        } else e.setResponse(ping);
                        lock.value = false;
                        if (mode) e.completeIntent(plugin);
                    }, ((InitialHandler) e.getConnection()).getHandshake().getProtocolVersion());
                    if (!mode) while (lock.value) Try.all.run(() -> Thread.sleep(4));
                }
            }
        } else if (dynamic) {
            e.getResponse().getPlayers().setOnline(rPlayers.size());
        }
    }

    @EventHandler(priority = Byte.MAX_VALUE)
    public void ping(ProxyPingEvent e) {
        ServerInfo override;
        if ((override = SmartFallback.getForcedHost(e.getConnection())) != null || (override = SmartFallback.getDNS(e.getConnection())) != null) {
            if (override instanceof SubServer && !((SubServer) override).isRunning()) {
                e.setResponse(new ServerPing(e.getResponse().getVersion(), e.getResponse().getPlayers(), new TextComponent(api.getLang("SubServers", "Bungee.Ping.Offline")), e.getResponse().getFaviconObject()));
            }
        } else {
            int offline = 0;
            for (String name : e.getConnection().getListener().getServerPriority()) {
                ServerInfo server = api.getServer(name.toLowerCase());
                if (server == null) server = getServerInfo(name);
                if (server == null || (server instanceof SubServer && !((SubServer) server).isRunning())) offline++;
            }

            if (offline >= e.getConnection().getListener().getServerPriority().size()) {
                e.setResponse(new ServerPing(e.getResponse().getVersion(), e.getResponse().getPlayers(), new TextComponent(api.getLang("SubServers", "Bungee.Ping.Offline")), e.getResponse().getFaviconObject()));
            }
        }
    }

    @EventHandler(priority = Byte.MIN_VALUE)
    public void login(LoginEvent e) {
        super.getLogger().info("UUID of player " + e.getConnection().getName() + " is " + e.getConnection().getUniqueId());
        if (rPlayers.containsKey(e.getConnection().getUniqueId())) {
            Logger.get("SubServers").warning(e.getConnection().getName() + " connected, but already had a database entry");
            RemotePlayer player = rPlayers.get(e.getConnection().getUniqueId());
            if (player.getProxy() == null || player.getProxy().isMaster()) {
                ProxiedPlayer p = getPlayer(player.getUniqueId());
                if (p != null) p.disconnect(new TextComponent(getTranslation("already_connected_proxy")));
            } else if (player.getProxy().getSubData()[0] != null) {
                ((SubDataClient) player.getProxy().getSubData()[0]).sendPacket(new PacketExDisconnectPlayer(Collections.singletonList(player.getUniqueId()), getTranslation("already_connected_proxy")));
            }
        }
    }

    @EventHandler(priority = Byte.MAX_VALUE)
    public void validate(ServerConnectEvent e) {
        if (e.getPlayer().isConnected()) {
            Map<String, ServerInfo> servers = new TreeMap<String, ServerInfo>(api.getServers());
            if (servers.containsKey(e.getTarget().getName().toLowerCase()) && e.getTarget() != servers.get(e.getTarget().getName().toLowerCase())) {
                e.setTarget(servers.get(e.getTarget().getName().toLowerCase()));
            } else {
                servers = getServersCopy();
                if (servers.containsKey(e.getTarget().getName()) && e.getTarget() != servers.get(e.getTarget().getName())) {
                    e.setTarget(servers.get(e.getTarget().getName()));
                }
            }

            if (!e.getTarget().canAccess(e.getPlayer())) {
                if (e.getPlayer().getServer() == null || fallback.containsKey(e.getPlayer().getUniqueId())) {
                    if (!fallback.containsKey(e.getPlayer().getUniqueId()) || fallback.get(e.getPlayer().getUniqueId()).names.contains(e.getTarget().getName())) {
                        ServerKickEvent kick = new ServerKickEvent(e.getPlayer(), e.getTarget(), new BaseComponent[]{
                                new TextComponent(api.getLang("SubServers", "Bungee.Restricted"))
                        }, null, ServerKickEvent.State.CONNECTING);
                        fallback(kick);
                        if (!kick.isCancelled()) e.getPlayer().disconnect(kick.getKickReasonComponent());
                        if (e.getPlayer().getServer() != null) e.setCancelled(true);
                    }
                } else {
                    e.getPlayer().sendMessage(api.getLang("SubServers", "Bungee.Restricted"));
                    e.setCancelled(true);
                }
            } else if (e.getPlayer().getServer() != null && !fallback.containsKey(e.getPlayer().getUniqueId()) && e.getTarget() instanceof SubServer && !((SubServer) e.getTarget()).isRunning()) {
                e.getPlayer().sendMessage(api.getLang("SubServers", "Bungee.Server.Offline"));
                e.setCancelled(true);
            }

            if (fallback.containsKey(e.getPlayer().getUniqueId())) {
                FallbackState state = fallback.get(e.getPlayer().getUniqueId());
                if (state.names.contains(e.getTarget().getName())) {
                    state.remove(e.getTarget().getName());
                } else if (e.getPlayer().getServer() != null) {
                    fallback.remove(e.getPlayer().getUniqueId());
                }
            }
        } else {
            e.setCancelled(true);
        }
    }

    @SuppressWarnings("deprecation")
    @EventHandler(priority = Byte.MAX_VALUE)
    public void connected(ServerConnectedEvent e) {
        if (e.getPlayer().isConnected()) {
            synchronized (rPlayers) {
                RemotePlayer player = new RemotePlayer(e.getPlayer(), e.getServer().getInfo());
                rPlayerLinkP.put(player.getUniqueId(), player.getProxy());
                rPlayers.put(player.getUniqueId(), player);
                if (player.getServer() != null) rPlayerLinkS.put(player.getUniqueId(), player.getServer());
                for (Proxy proxy : SubAPI.getInstance().getProxies().values()) if (proxy.getSubData()[0] != null) {
                    ((SubDataClient) proxy.getSubData()[0]).sendPacket(new PacketExSyncPlayer(mProxy.getName(), true, player));
                }
            }


            if (fallback.containsKey(e.getPlayer().getUniqueId())) {
                fallback.get(e.getPlayer().getUniqueId()).done(() -> {
                    if (e.getPlayer().getServer() != null && !((UserConnection) e.getPlayer()).isDimensionChange() && e.getPlayer().getServer().getInfo().getName().equals(e.getServer().getInfo().getName())) {
                        fallback.remove(e.getPlayer().getUniqueId());
                        e.getPlayer().sendMessage(api.getLang("SubServers", "Bungee.Feature.Smart-Fallback.Result").replace("$str$", (e.getServer().getInfo() instanceof Server)?((Server) e.getServer().getInfo()).getDisplayName():e.getServer().getInfo().getName()));
                    }
                }, getConfig().getServerConnectTimeout() + 500);
            }
        }
    }

    @SuppressWarnings("deprecation")
    @EventHandler(priority = Byte.MAX_VALUE)
    public void fallback(ServerKickEvent e) {
        if (e.getPlayer().isConnected() && e.getPlayer() instanceof UserConnection && config.get().getMap("Settings").getMap("Smart-Fallback", new ObjectMap<>()).getBoolean("Fallback", true)) {
            FallbackState state;
            boolean init = !fallback.containsKey(e.getPlayer().getUniqueId());
            if (init) {
                Map<String, ServerInfo> map = SmartFallback.getFallbackServers(e.getPlayer().getPendingConnection().getListener(), e.getPlayer());
                map.remove(e.getKickedFrom().getName());
                state = new FallbackState(e.getPlayer().getUniqueId(), map, e.getKickReasonComponent());
            } else {
                state = fallback.get(e.getPlayer().getUniqueId());
                e.setKickReasonComponent(state.reason);
                LinkedList<ServerInfo> tmp = new LinkedList<>(state.servers);
                for (ServerInfo server : tmp) if (server.getName().equals(e.getKickedFrom().getName()))
                    state.remove(server);
            }

            if (!state.servers.isEmpty()) {
                e.setCancelled(true);
                e.getPlayer().sendMessage(api.getLang("SubServers", "Bungee.Feature.Smart-Fallback").replace("$str$", (e.getKickedFrom() instanceof Server)?((Server) e.getKickedFrom()).getDisplayName():e.getKickedFrom().getName()).replace("$msg$", e.getKickReason()));
                if (init) fallback.put(e.getPlayer().getUniqueId(), state);

                e.setCancelServer(state.servers.getFirst());
            }
        }
    }

    @EventHandler(priority = Byte.MAX_VALUE)
    public void disconnected(PlayerDisconnectEvent e) {
        UUID id = e.getPlayer().getUniqueId();
        fallback.remove(id);
        SubCommand.players.remove(id);

        synchronized (rPlayers) {
            if (rPlayers.containsKey(id) && (!rPlayerLinkP.containsKey(id) || rPlayerLinkP.get(id).isMaster())) {
                RemotePlayer player = rPlayers.get(id);
                rPlayerLinkS.remove(id);
                rPlayerLinkP.remove(id);
                rPlayers.remove(id);

                for (Proxy proxy : SubAPI.getInstance().getProxies().values()) if (proxy.getSubData()[0] != null) {
                    ((SubDataClient) proxy.getSubData()[0]).sendPacket(new PacketExSyncPlayer(mProxy.getName(), false, player));
                }
            }
        }
    }

    @EventHandler(priority = Byte.MIN_VALUE)
    public void unsudo(SubStoppedEvent e) {
        if (sudo == e.getServer()) {
            sudo = null;
            Logger.get("SubServers").info("Reverting to the BungeeCord Console");
        }
    }
}

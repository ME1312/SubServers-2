package net.ME1312.SubServers.Bungee;

import com.dosse.upnp.UPnP;
import com.google.common.collect.Range;
import com.google.gson.Gson;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Container.NamedContainer;
import net.ME1312.Galaxi.Library.UniversalFile;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Server.*;
import net.ME1312.SubData.Server.Encryption.AES;
import net.ME1312.SubData.Server.Encryption.DHE;
import net.ME1312.SubData.Server.Encryption.RSA;
import net.ME1312.SubData.Server.Library.DataSize;
import net.ME1312.SubServers.Bungee.Event.*;
import net.ME1312.SubServers.Bungee.Host.*;
import net.ME1312.SubServers.Bungee.Library.*;
import net.ME1312.Galaxi.Library.Config.YAMLConfig;
import net.ME1312.Galaxi.Library.Config.YAMLSection;
import net.ME1312.SubServers.Bungee.Library.Compatibility.Galaxi.GalaxiCommand;
import net.ME1312.SubServers.Bungee.Library.Compatibility.LegacyServerMap;
import net.ME1312.SubServers.Bungee.Library.Compatibility.Logger;
import net.ME1312.SubServers.Bungee.Library.Fallback.SmartReconnectHandler;
import net.ME1312.SubServers.Bungee.Library.ConfigUpdater;
import net.ME1312.SubServers.Bungee.Library.Exception.InvalidHostException;
import net.ME1312.SubServers.Bungee.Library.Exception.InvalidServerException;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.SubServers.Bungee.Network.Packet.PacketOutExReload;
import net.ME1312.SubServers.Bungee.Network.SubProtocol;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.BungeeServerInfo;
import net.md_5.bungee.UserConnection;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ListenerInfo;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.event.*;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

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
public final class SubProxy extends BungeeCord implements Listener {
    final LinkedHashMap<String, LinkedHashMap<String, String>> exLang = new LinkedHashMap<String, LinkedHashMap<String, String>>();
    final HashMap<String, Class<? extends Host>> hostDrivers = new HashMap<String, Class<? extends Host>>();
    public final HashMap<String, Proxy> proxies = new HashMap<String, Proxy>();
    public final HashMap<String, Host> hosts = new HashMap<String, Host>();
    public final HashMap<String, Server> exServers = new HashMap<String, Server>();
    private final HashMap<String, ServerInfo> legServers = new HashMap<String, ServerInfo>();
    private final HashMap<UUID, List<ServerInfo>> fallbackLimbo = new HashMap<UUID, List<ServerInfo>>();

    public final PrintStream out;
    public final UniversalFile dir = new UniversalFile(new File(System.getProperty("user.dir")));
    public YAMLConfig config;
    public YAMLConfig servers;
    private YAMLConfig bungee;
    public YAMLConfig lang;
    public final SubAPI api = new SubAPI(this);
    public SubProtocol subprotocol;
    public SubDataServer subdata = null;
    public SubServer sudo = null;
    public static final Version version = Version.fromString("2.16a");

    public Proxy redis = null;
    public boolean canSudo = false;
    public final boolean isPatched;
    public final boolean isGalaxi;
    public long resetDate = 0;
    private boolean running = false;
    private boolean reloading = false;
    private boolean posted = false;
    private static BigInteger lastSignature = BigInteger.valueOf(-1);

    @SuppressWarnings("unchecked")
    SubProxy(PrintStream out, boolean isPatched) throws Exception {
        this.isPatched = isPatched;
        this.isGalaxi = !Util.isException(() ->
                Util.reflect(Class.forName("net.ME1312.Galaxi.Engine.PluginManager").getMethod("findClasses", Class.class),
                        Util.reflect(Class.forName("net.ME1312.Galaxi.Engine.GalaxiEngine").getMethod("getPluginManager"),
                                Util.reflect(Class.forName("net.ME1312.Galaxi.Engine.GalaxiEngine").getMethod("getInstance"), null)), Launch.class));

        Util.reflect(Logger.class.getDeclaredField("plugin"), null, this);
        Logger.get("SubServers").info("Loading SubServers.Bungee v" + version.toString() + " Libraries (for Minecraft " + api.getGameVersion()[api.getGameVersion().length - 1] + ")");

        this.out = out;
        if (!(new UniversalFile(dir, "config.yml").exists())) {
            Util.copyFromJar(SubProxy.class.getClassLoader(), "net/ME1312/SubServers/Bungee/Library/Files/bungee.yml", new UniversalFile(dir, "config.yml").getPath());
            YAMLConfig tmp = new YAMLConfig(new UniversalFile("config.yml"));
            tmp.get().set("stats", UUID.randomUUID().toString());
            tmp.save();
            Logger.get("SubServers").info("Created ./config.yml");
        }
        bungee = new YAMLConfig(new UniversalFile(dir, "config.yml"));

        UniversalFile dir = new UniversalFile(this.dir, "SubServers");
        dir.mkdir();

        ConfigUpdater.updateConfig(new UniversalFile(dir, "config.yml"));
        config = new YAMLConfig(new UniversalFile(dir, "config.yml"));

        ConfigUpdater.updateServers(new UniversalFile(dir, "servers.yml"));
        servers = new YAMLConfig(new UniversalFile(dir, "servers.yml"));

        ConfigUpdater.updateLang(new UniversalFile(dir, "lang.yml"));
        lang = new YAMLConfig(new UniversalFile(dir, "lang.yml"));

        if (!(new UniversalFile(dir, "Templates").exists())) {
            new UniversalFile(dir, "Templates").mkdirs();

            Util.unzip(SubProxy.class.getResourceAsStream("/net/ME1312/SubServers/Bungee/Library/Files/Templates/vanilla.zip"), new UniversalFile(dir, "Templates"));
            Logger.get("SubServers").info("Created ./SubServers/Templates/Vanilla");

            Util.unzip(SubProxy.class.getResourceAsStream("/net/ME1312/SubServers/Bungee/Library/Files/Templates/spigot.zip"), new UniversalFile(dir, "Templates"));
            Logger.get("SubServers").info("Created ./SubServers/Templates/Spigot");

            Util.unzip(SubProxy.class.getResourceAsStream("/net/ME1312/SubServers/Bungee/Library/Files/Templates/paper.zip"), new UniversalFile(dir, "Templates"));
            Logger.get("SubServers").info("Created ./SubServers/Templates/Paper");

            Util.unzip(SubProxy.class.getResourceAsStream("/net/ME1312/SubServers/Bungee/Library/Files/Templates/forge.zip"), new UniversalFile(dir, "Templates"));
            Logger.get("SubServers").info("Created ./SubServers/Templates/Forge");

            Util.unzip(SubProxy.class.getResourceAsStream("/net/ME1312/SubServers/Bungee/Library/Files/Templates/sponge.zip"), new UniversalFile(dir, "Templates"));
            Logger.get("SubServers").info("Created ./SubServers/Templates/Sponge");
        } else {
            long stamp = Math.round(Math.random() * 100000);
            Version version = new Version("2.16a+");

            if (new UniversalFile(dir, "Templates:Vanilla:template.yml").exists() && ((new YAMLConfig(new UniversalFile(dir, "Templates:Vanilla:template.yml"))).get().getVersion("Version", version)).compareTo(version) != 0) {
                Files.move(new UniversalFile(dir, "Templates:Vanilla").toPath(), new UniversalFile(dir, "Templates:Vanilla." + stamp + ".x").toPath());
                Util.unzip(SubProxy.class.getResourceAsStream("/net/ME1312/SubServers/Bungee/Library/Files/Templates/vanilla.zip"), new UniversalFile(dir, "Templates"));
                Logger.get("SubServers").info("Updated ./SubServers/Templates/Vanilla");
            }
            if (new UniversalFile(dir, "Templates:Spigot:template.yml").exists() && ((new YAMLConfig(new UniversalFile(dir, "Templates:Spigot:template.yml"))).get().getVersion("Version", version)).compareTo(version) != 0) {
                Files.move(new UniversalFile(dir, "Templates:Spigot").toPath(), new UniversalFile(dir, "Templates:Spigot." + stamp + ".x").toPath());
                Util.unzip(SubProxy.class.getResourceAsStream("/net/ME1312/SubServers/Bungee/Library/Files/Templates/spigot.zip"), new UniversalFile(dir, "Templates"));
                Logger.get("SubServers").info("Updated ./SubServers/Templates/Spigot");
            }
            if (new UniversalFile(dir, "Templates:Paper:template.yml").exists() && ((new YAMLConfig(new UniversalFile(dir, "Templates:Paper:template.yml"))).get().getVersion("Version", version)).compareTo(version) != 0) {
                Files.move(new UniversalFile(dir, "Templates:Paper").toPath(), new UniversalFile(dir, "Templates:Paper." + stamp + ".x").toPath());
                Util.unzip(SubProxy.class.getResourceAsStream("/net/ME1312/SubServers/Bungee/Library/Files/Templates/paper.zip"), new UniversalFile(dir, "Templates"));
                Logger.get("SubServers").info("Updated ./SubServers/Templates/Paper");
            }
            if (new UniversalFile(dir, "Templates:Forge:template.yml").exists() && ((new YAMLConfig(new UniversalFile(dir, "Templates:Forge:template.yml"))).get().getVersion("Version", version)).compareTo(version) != 0) {
                Files.move(new UniversalFile(dir, "Templates:Forge").toPath(), new UniversalFile(dir, "Templates:Forge." + stamp + ".x").toPath());
                Util.unzip(SubProxy.class.getResourceAsStream("/net/ME1312/SubServers/Bungee/Library/Files/Templates/forge.zip"), new UniversalFile(dir, "Templates"));
                Logger.get("SubServers").info("Updated ./SubServers/Templates/Forge");
            }
            if (new UniversalFile(dir, "Templates:Sponge:template.yml").exists() && ((new YAMLConfig(new UniversalFile(dir, "Templates:Sponge:template.yml"))).get().getVersion("Version", version)).compareTo(version) != 0) {
                Files.move(new UniversalFile(dir, "Templates:Sponge").toPath(), new UniversalFile(dir, "Templates:Sponge." + stamp + ".x").toPath());
                Util.unzip(SubProxy.class.getResourceAsStream("/net/ME1312/SubServers/Bungee/Library/Files/Templates/sponge.zip"), new UniversalFile(dir, "Templates"));
                Logger.get("SubServers").info("Updated ./SubServers/Templates/Sponge");
            }
        }

        Runnable clean = () -> {
            try {
                if (new UniversalFile(dir, "Recently Deleted").exists()) {
                    int f = new UniversalFile(dir, "Recently Deleted").listFiles().length;
                    for (File file : new UniversalFile(dir, "Recently Deleted").listFiles()) {
                        try {
                            if (file.isDirectory()) {
                                if (new UniversalFile(dir, "Recently Deleted:" + file.getName() + ":info.json").exists()) {
                                    FileReader reader = new FileReader(new UniversalFile(dir, "Recently Deleted:" + file.getName() + ":info.json"));
                                    YAMLSection info = new YAMLSection(new Gson().fromJson(Util.readAll(reader), Map.class));
                                    reader.close();
                                    if (info.contains("Timestamp")) {
                                        if (TimeUnit.MILLISECONDS.toDays(Calendar.getInstance().getTime().getTime() - info.getLong("Timestamp")) >= 7) {
                                            Util.deleteDirectory(file);
                                            f--;
                                            Logger.get("SubServers").info("Removed ./SubServers/Recently Deleted/" + file.getName());
                                        }
                                    } else {
                                        Util.deleteDirectory(file);
                                        f--;
                                        Logger.get("SubServers").info("Removed ./SubServers/Recently Deleted/" + file.getName());
                                    }
                                } else {
                                    Util.deleteDirectory(file);
                                    f--;
                                    Logger.get("SubServers").info("Removed ./SubServers/Recently Deleted/" + file.getName());
                                }
                            } else {
                                Files.delete(file.toPath());
                                f--;
                                Logger.get("SubServers").info("Removed ./SubServers/Recently Deleted/" + file.getName());
                            }
                        } catch (Exception e) {
                            Logger.get("SubServers").info("Problem scanning .SubServers/Recently Deleted/" + file.getName());
                            e.printStackTrace();
                            Files.delete(file.toPath());
                        }
                    }
                    if (f <= 0) {
                        Files.delete(new UniversalFile(dir, "Recently Deleted").toPath());
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

        api.addHostDriver(net.ME1312.SubServers.Bungee.Host.Internal.InternalHost.class, "virtual");
        api.addHostDriver(net.ME1312.SubServers.Bungee.Host.External.ExternalHost.class, "network");

        getPluginManager().registerListener(null, this);

        Logger.get("SubServers").info("Pre-Parsing Config...");
        for (String name : servers.get().getMap("Servers").getKeys()) {
            try {
                if (Util.getCaseInsensitively(config.get().getMap("Hosts").get(), servers.get().getMap("Servers").getMap(name).getString("Host")) == null) throw new InvalidServerException("There is no host with this name: " + servers.get().getMap("Servers").getMap(name).getString("Host"));
                legServers.put(name, constructServerInfo(name, new InetSocketAddress(InetAddress.getByName((String) ((Map<String, ?>) Util.getCaseInsensitively(config.get().getMap("Hosts").get(), servers.get().getMap("Servers").getMap(name).getString("Host"))).get("Address")), servers.get().getMap("Servers").getMap(name).getInt("Port")),
                        ChatColor.translateAlternateColorCodes('&', servers.get().getMap("Servers").getMap(name).getString("Motd")), servers.get().getMap("Servers").getMap(name).getBoolean("Restricted")));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        subprotocol = SubProtocol.get();
        subprotocol.registerCipher("DHE", DHE.get(128));
        subprotocol.registerCipher("DHE-128", DHE.get(128));
        subprotocol.registerCipher("DHE-192", DHE.get(192));
        subprotocol.registerCipher("DHE-256", DHE.get(256));
        Logger.get("SubServers").info("Loading BungeeCord Libraries...");
        if (isGalaxi) Util.reflect(net.ME1312.SubServers.Bungee.Library.Compatibility.Galaxi.GalaxiEventListener.class.getConstructor(SubProxy.class), this);
    }

    /**
     * Load SubServers before BungeeCord finishes
     */
    @Override
    @SuppressWarnings("unchecked")
    public void startListeners() {
        try {
            if (getPluginManager().getPlugin("RedisBungee") != null) redis = Util.getDespiteException(() -> new Proxy((String) redis("getServerId")), null);
            reload();

            if (UPnP.isUPnPAvailable()) {
                if (config.get().getMap("Settings").getMap("UPnP", new ObjectMap<String>()).getBoolean("Forward-Proxy", true)) for (ListenerInfo listener : getConfig().getListeners()) {
                    UPnP.openPortTCP(listener.getHost().getPort());
                }
            } else {
                getLogger().warning("UPnP is currently unavailable; Ports may not be automatically forwarded on this device");
            }

            super.startListeners();

            if (!posted) {
                post();
                posted = true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Load data from the config (will attempt to merge with current configuration)
     *
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    public void reload() throws IOException {
        List<String> ukeys = new ArrayList<String>();
        long begin = Calendar.getInstance().getTime().getTime();
        boolean status;
        if (!(status = running)) resetDate = begin;
        reloading = true;

        ConfigUpdater.updateConfig(new UniversalFile(dir, "SubServers:config.yml"));
        ConfigUpdater.updateServers(new UniversalFile(dir, "SubServers:servers.yml"));
        ConfigUpdater.updateLang(new UniversalFile(dir, "SubServers:lang.yml"));

        YAMLSection prevconfig = config.get();
        config.reload();
        servers.reload();
        lang.reload();
        for (String key : lang.get().getMap("Lang").getKeys())
            api.setLang("SubServers", key, ChatColor.translateAlternateColorCodes('&', lang.get().getMap("Lang").getString(key)));

        if (subdata != null && ( // SubData Server must be reset
                !config.get().getMap("Settings").getMap("SubData").getRawString("Address", "127.0.0.1:4391").equals(prevconfig.getMap("Settings").getMap("SubData").getRawString("Address", "127.0.0.1:4391")) ||
                !config.get().getMap("Settings").getMap("SubData").getRawString("Encryption", "NONE").equals(prevconfig.getMap("Settings").getMap("SubData").getRawString("Encryption", "NONE"))
                )) {
            subdata.close();
            Util.isException(subdata::waitFor);
            subdata = null;
        }
        int proxies = 1;
        if (redis != null) {
            try {
                boolean first = true;
                String master = (String) redis("getServerId");
                if (!master.equals(redis.getName())) redis = new Proxy(master);
                if (!redis.getDisplayName().equals("(master)")) redis.setDisplayName("(master)");
                for (String name : (List<String>) redis("getAllServers")) {
                    if (!ukeys.contains(name.toLowerCase()) && !master.equals(name)) try {
                        if (first) Logger.get("SubServers").info(((status)?"Rel":"L")+"oading Proxies...");
                        first = false;
                        Proxy proxy = this.proxies.get(name.toLowerCase());
                        if (proxy == null) {
                            proxy = new Proxy(name);
                            getPluginManager().callEvent(new SubAddProxyEvent(proxy));
                            this.proxies.put(name.toLowerCase(), proxy);
                        }
                        ukeys.add(name.toLowerCase());
                        proxies++;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        ukeys.clear();

        int hosts = 0;
        Logger.get("SubServers").info(((status)?"Rel":"L")+"oading Hosts...");
        for (String name : config.get().getMap("Hosts").getKeys()) {
            if (!ukeys.contains(name.toLowerCase())) try {
                if (!hostDrivers.keySet().contains(config.get().getMap("Hosts").getMap(name).getRawString("Driver").toUpperCase().replace('-', '_').replace(' ', '_'))) throw new InvalidHostException("Invalid Driver for host: " + name);
                Host host = this.hosts.get(name.toLowerCase());
                if (host == null || // Host must be reset
                        !hostDrivers.get(config.get().getMap("Hosts").getMap(name).getRawString("Driver").toUpperCase().replace('-', '_').replace(' ', '_')).equals(host.getClass()) ||
                        !config.get().getMap("Hosts").getMap(name).getRawString("Address").equals(host.getAddress().getHostAddress()) ||
                        !config.get().getMap("Hosts").getMap(name).getRawString("Directory").equals(host.getPath()) ||
                        !config.get().getMap("Hosts").getMap(name).getRawString("Git-Bash").equals(host.getCreator().getBashDirectory())
                        ) {
                    if (host != null) api.forceRemoveHost(name);
                    host = api.addHost(config.get().getMap("Hosts").getMap(name).getRawString("Driver").toLowerCase(), name, config.get().getMap("Hosts").getMap(name).getBoolean("Enabled"),
                            Range.closed(Integer.parseInt(config.get().getMap("Hosts").getMap(name).getRawString("Port-Range", "25500-25559").split("-")[0]), Integer.parseInt(config.get().getMap("Hosts").getMap(name).getRawString("Port-Range", "25500-25559").split("-")[1])),
                            config.get().getMap("Hosts").getMap(name).getBoolean("Log-Creator", true), InetAddress.getByName(config.get().getMap("Hosts").getMap(name).getRawString("Address")),
                            config.get().getMap("Hosts").getMap(name).getRawString("Directory"), config.get().getMap("Hosts").getMap(name).getRawString("Git-Bash"));
                } else { // Host wasn't reset, so check for these changes
                    if (config.get().getMap("Hosts").getMap(name).getBoolean("Enabled") != host.isEnabled())
                        host.setEnabled(config.get().getMap("Hosts").getMap(name).getBoolean("Enabled"));
                    if (!config.get().getMap("Hosts").getMap(name).getRawString("Port-Range", "25500-25559").equals(prevconfig.getMap("Hosts", new ObjectMap<String>()).getMap(name, new ObjectMap<String>()).getRawString("Port-Range", "25500-25559")))
                        host.getCreator().setPortRange(Range.closed(Integer.parseInt(config.get().getMap("Hosts").getMap(name).getRawString("Port-Range", "25500-25559").split("-")[0]), Integer.parseInt(config.get().getMap("Hosts").getMap(name).getRawString("Port-Range", "25500-25559").split("-")[1])));
                    if (config.get().getMap("Hosts").getMap(name).getBoolean("Log-Creator", true) != host.getCreator().isLogging())
                        host.getCreator().setLogging(config.get().getMap("Hosts").getMap(name).getBoolean("Log-Creator", true));
                    host.getCreator().reload();
                } // Check for other changes
                if (config.get().getMap("Hosts").getMap(name).getKeys().contains("Display") && ((config.get().getMap("Hosts").getMap(name).getString("Display").length() == 0 && !host.getName().equals(host.getDisplayName())) || (config.get().getMap("Hosts").getMap(name).getString("Display").length() > 0 && !config.get().getMap("Hosts").getMap(name).getString("Display").equals(host.getDisplayName()))))
                    host.setDisplayName(config.get().getMap("Hosts").getMap(name).getString("Display"));
                if (config.get().getMap("Hosts").getMap(name).getKeys().contains("Extra"))
                    for (String extra : config.get().getMap("Hosts").getMap(name).getMap("Extra").getKeys()) host.addExtra(extra, config.get().getMap("Hosts").getMap(name).getMap("Extra").getObject(extra));
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
                Server server = api.getServer(name);
                if (server == null || !(server instanceof SubServer)) {
                    if (server == null || // Server must be reset
                            bungee.get().getMap("servers").getMap(name).getRawString("address").equals(server.getAddress().getAddress().getHostAddress() + ':' + server.getAddress().getPort())
                    ) {
                        if (server != null) api.forceRemoveServer(name);
                        server = api.addServer(name, InetAddress.getByName(bungee.get().getMap("servers").getMap(name).getRawString("address").split(":")[0]),
                                Integer.parseInt(bungee.get().getMap("servers").getMap(name).getRawString("address").split(":")[1]), ChatColor.translateAlternateColorCodes('&', bungee.get().getMap("servers").getMap(name).getString("motd")),
                                bungee.get().getMap("servers").getMap(name).getBoolean("hidden", false), bungee.get().getMap("servers").getMap(name).getBoolean("restricted"));
                    } else { // Server wasn't reset, so check for these changes
                        if (!ChatColor.translateAlternateColorCodes('&', bungee.get().getMap("servers").getMap(name).getString("motd")).equals(server.getMotd()))
                            server.setMotd(ChatColor.translateAlternateColorCodes('&', bungee.get().getMap("servers").getMap(name).getString("motd")));
                        if (bungee.get().getMap("servers").getMap(name).getBoolean("hidden", false) != server.isHidden())
                            server.setHidden(bungee.get().getMap("servers").getMap(name).getBoolean("hidden", false));
                        if (bungee.get().getMap("servers").getMap(name).getBoolean("restricted") != server.isRestricted())
                            server.setRestricted(bungee.get().getMap("servers").getMap(name).getBoolean("restricted"));
                    } // Check for other changes
                    if (bungee.get().getMap("servers").getMap(name).getKeys().contains("display") && ((bungee.get().getMap("servers").getMap(name).getRawString("display").length() == 0 && !server.getName().equals(server.getDisplayName())) || (bungee.get().getMap("servers").getMap(name).getRawString("display").length() > 0 && !bungee.get().getMap("servers").getMap(name).getRawString("display").equals(server.getDisplayName()))))
                        server.setDisplayName(bungee.get().getMap("servers").getMap(name).getString("display"));
                    if (bungee.get().getMap("servers").getMap(name).getKeys().contains("group")) {
                        for (String group : server.getGroups()) server.removeGroup(group);
                        for (String group : bungee.get().getMap("servers").getMap(name).getStringList("group")) server.addGroup(group);
                    }
                    if (bungee.get().getMap("servers").getMap(name).getKeys().contains("extra"))
                        for (String extra : config.get().getMap("servers").getMap(name).getMap("extra").getKeys()) server.addExtra(extra, config.get().getMap("servers").getMap(name).getMap("extra").getObject(extra));
                    if (server.getSubData()[0] != null)
                        ((SubDataClient) server.getSubData()[0]).sendPacket(new PacketOutExReload(null));
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
        if (!posted) Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (running) {
                Logger.get("SubServers").info("Received request from system to shutdown");
                try {
                    shutdown();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, "SubServers.Bungee::System_Shutdown"));
        running = true;
        List<String> autorun = new LinkedList<String>();
        for (String name : this.servers.get().getMap("Servers").getKeys()) {
            if (!ukeys.contains(name.toLowerCase())) try {
                if (!this.hosts.keySet().contains(this.servers.get().getMap("Servers").getMap(name).getString("Host").toLowerCase())) throw new InvalidServerException("There is no host with this name: " + this.servers.get().getMap("Servers").getMap(name).getString("Host"));
                if (exServers.keySet().contains(name.toLowerCase())) {
                    exServers.remove(name.toLowerCase());
                    servers--;
                }
                SubServer server = api.getSubServer(name);
                if (server != null && server.isEditable()) { // Server can edit() (May be reset depending on change severity)
                    ObjectMap<String> edits = new ObjectMap<String>();
                    if (this.servers.get().getMap("Servers").getMap(name).getBoolean("Enabled") != server.isEnabled())
                        edits.set("enabled", this.servers.get().getMap("Servers").getMap(name).getBoolean("Enabled"));
                    if (this.servers.get().getMap("Servers").getMap(name).getKeys().contains("Display") && ((this.servers.get().getMap("Servers").getMap(name).getRawString("Display").length() == 0 && !server.getName().equals(server.getDisplayName())) || (this.servers.get().getMap("Servers").getMap(name).getRawString("Display").length() > 0 && !this.servers.get().getMap("Servers").getMap(name).getRawString("Display").equals(server.getDisplayName()))))
                        edits.set("display", this.servers.get().getMap("Servers").getMap(name).getRawString("Display"));
                    if (!this.servers.get().getMap("Servers").getMap(name).getString("Host").equalsIgnoreCase(server.getHost().getName()))
                        edits.set("host", this.servers.get().getMap("Servers").getMap(name).getRawString("Host"));
                    if (this.servers.get().getMap("Servers").getMap(name).getKeys().contains("Template") && ((this.servers.get().getMap("Servers").getMap(name).getRawString("Template").length() == 0 && server.getTemplate() != null) || (this.servers.get().getMap("Servers").getMap(name).getRawString("Template").length() > 0 && server.getTemplate() == null) || (server.getTemplate() != null && !this.servers.get().getMap("Servers").getMap(name).getString("Template").equalsIgnoreCase(server.getTemplate().getName()))))
                        edits.set("template", this.servers.get().getMap("Servers").getMap(name).getString("Template"));
                    if (!this.servers.get().getMap("Servers").getMap(name).getStringList("Group").equals(server.getGroups()))
                        edits.set("group", this.servers.get().getMap("Servers").getMap(name).getRawStringList("Group"));
                    if (this.servers.get().getMap("Servers").getMap(name).getInt("Port") != server.getAddress().getPort())
                        edits.set("port", this.servers.get().getMap("Servers").getMap(name).getInt("Port"));
                    if (!(ChatColor.translateAlternateColorCodes('&', this.servers.get().getMap("Servers").getMap(name).getString("Motd")).equals(server.getMotd())))
                        edits.set("motd", this.servers.get().getMap("Servers").getMap(name).getRawString("Motd"));
                    if (this.servers.get().getMap("Servers").getMap(name).getBoolean("Log") != server.isLogging())
                        edits.set("log", this.servers.get().getMap("Servers").getMap(name).getBoolean("Log"));
                    if (!this.servers.get().getMap("Servers").getMap(name).getRawString("Directory").equals(server.getPath()))
                        edits.set("dir", this.servers.get().getMap("Servers").getMap(name).getRawString("Directory"));
                    if (!this.servers.get().getMap("Servers").getMap(name).getRawString("Executable").equals(server.getExecutable()))
                        edits.set("exec", this.servers.get().getMap("Servers").getMap(name).getRawString("Executable"));
                    if (!this.servers.get().getMap("Servers").getMap(name).getRawString("Stop-Command").equals(server.getStopCommand()))
                        edits.set("stop-cmd", this.servers.get().getMap("Servers").getMap(name).getRawString("Stop-Command"));
                    SubServer.StopAction action = Util.getDespiteException(() -> SubServer.StopAction.valueOf(this.servers.get().getMap("Servers").getMap(name).getRawString("Stop-Action", "NONE").toUpperCase().replace('-', '_').replace(' ', '_')), null);
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
                            !this.servers.get().getMap("Servers").getMap(name).getRawString("Directory").equals(server.getPath()) ||
                            !this.servers.get().getMap("Servers").getMap(name).getRawString("Executable").equals(server.getExecutable())
                            ) {
                            if (server != null) server.getHost().forceRemoveSubServer(name);
                            server = this.hosts.get(this.servers.get().getMap("Servers").getMap(name).getString("Host").toLowerCase()).addSubServer(name, this.servers.get().getMap("Servers").getMap(name).getBoolean("Enabled"),
                                    this.servers.get().getMap("Servers").getMap(name).getInt("Port"), ChatColor.translateAlternateColorCodes('&', this.servers.get().getMap("Servers").getMap(name).getString("Motd")), this.servers.get().getMap("Servers").getMap(name).getBoolean("Log"),
                                    this.servers.get().getMap("Servers").getMap(name).getRawString("Directory"), this.servers.get().getMap("Servers").getMap(name).getRawString("Executable"), this.servers.get().getMap("Servers").getMap(name).getRawString("Stop-Command"),
                                    this.servers.get().getMap("Servers").getMap(name).getBoolean("Hidden"), this.servers.get().getMap("Servers").getMap(name).getBoolean("Restricted"));
                    } else { // Server doesn't need to reset
                        if (this.servers.get().getMap("Servers").getMap(name).getBoolean("Enabled") != server.isEnabled())
                            server.setEnabled(this.servers.get().getMap("Servers").getMap(name).getBoolean("Enabled"));
                        if (!ChatColor.translateAlternateColorCodes('&', this.servers.get().getMap("Servers").getMap(name).getString("Motd")).equals(server.getMotd()))
                            server.setMotd(ChatColor.translateAlternateColorCodes('&', this.servers.get().getMap("Servers").getMap(name).getString("Motd")));
                        if (this.servers.get().getMap("Servers").getMap(name).getBoolean("Log") != server.isLogging())
                            server.setLogging(this.servers.get().getMap("Servers").getMap(name).getBoolean("Log"));
                        if (!this.servers.get().getMap("Servers").getMap(name).getRawString("Stop-Command").equals(server.getStopCommand()))
                            server.setStopCommand(this.servers.get().getMap("Servers").getMap(name).getRawString("Stop-Command"));
                        if (this.servers.get().getMap("Servers").getMap(name).getBoolean("Restricted") != server.isRestricted())
                            server.setRestricted(this.servers.get().getMap("Servers").getMap(name).getBoolean("Restricted"));
                        if (this.servers.get().getMap("Servers").getMap(name).getBoolean("Hidden") != server.isHidden())
                            server.setHidden(this.servers.get().getMap("Servers").getMap(name).getBoolean("Hidden"));
                    } // Apply these changes regardless of reset
                    SubServer.StopAction action = Util.getDespiteException(() -> SubServer.StopAction.valueOf(this.servers.get().getMap("Servers").getMap(name).getRawString("Stop-Action", "NONE").toUpperCase().replace('-', '_').replace(' ', '_')), null);
                    if (action != null && action != server.getStopAction())
                        server.setStopAction(action);
                    if (!status && this.servers.get().getMap("Servers").getMap(name).getBoolean("Run-On-Launch"))
                        autorun.add(name.toLowerCase());
                    if (this.servers.get().getMap("Servers").getMap(name).getKeys().contains("Display") && ((this.servers.get().getMap("Servers").getMap(name).getRawString("Display").length() == 0 && !server.getName().equals(server.getDisplayName())) || (this.servers.get().getMap("Servers").getMap(name).getRawString("Display").length() > 0 && !this.servers.get().getMap("Servers").getMap(name).getRawString("Display").equals(server.getDisplayName()))))
                        server.setDisplayName(this.servers.get().getMap("Servers").getMap(name).getRawString("Display"));
                    if (this.servers.get().getMap("Servers").getMap(name).getKeys().contains("Template") && ((this.servers.get().getMap("Servers").getMap(name).getRawString("Template").length() == 0 && server.getTemplate() != null) || (this.servers.get().getMap("Servers").getMap(name).getRawString("Template").length() > 0 && server.getTemplate() == null) || (server.getTemplate() != null && !this.servers.get().getMap("Servers").getMap(name).getString("Template").equalsIgnoreCase(server.getTemplate().getName()))))
                        server.setTemplate(this.servers.get().getMap("Servers").getMap(name).getRawString("Template"));
                    if (this.servers.get().getMap("Servers").getMap(name).getKeys().contains("Group")) {
                        for (String group : server.getGroups()) server.removeGroup(group);
                        for (String group : this.servers.get().getMap("Servers").getMap(name).getStringList("Group")) server.addGroup(group);
                    }
                } // Apply these changes regardless of edit/reset
                if (this.servers.get().getMap("Servers").getMap(name).getKeys().contains("Extra")) for (String extra : this.servers.get().getMap("Servers").getMap(name).getMap("Extra").getKeys()) server.addExtra(extra, this.servers.get().getMap("Servers").getMap(name).getMap("Extra").getObject(extra));
                ukeys.add(name.toLowerCase());
                subservers++;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        for (String name : ukeys) {
            SubServer server = api.getSubServer(name);
            for (String oname : this.servers.get().getMap("Servers").getMap(server.getName()).getRawStringList("Incompatible", new ArrayList<>())) {
                SubServer oserver = api.getSubServer(oname);
                if (oserver != null && server.isCompatible(oserver)) server.toggleCompatibility(oserver);
            }
        }
        ukeys.clear();
        api.ready = true;
        legServers.clear();

        // Initialize SubData
        if (subdata == null) {
            subprotocol.unregisterCipher("AES");
            subprotocol.unregisterCipher("AES-128");
            subprotocol.unregisterCipher("AES-192");
            subprotocol.unregisterCipher("AES-256");
            subprotocol.unregisterCipher("RSA");

            subprotocol.setBlockSize(config.get().getMap("Settings").getMap("SubData").getLong("Block-Size", (long) DataSize.MB));
            subprotocol.setTimeout(TimeUnit.SECONDS.toMillis(config.get().getMap("Settings").getMap("SubData").getInt("Timeout", 30)));

            String cipher = config.get().getMap("Settings").getMap("SubData").getRawString("Encryption", "NULL");
            String[] ciphers = (cipher.contains("/"))?cipher.split("/"):new String[]{cipher};

            if (ciphers[0].equals("AES") || ciphers[0].equals("AES-128") || ciphers[0].equals("AES-192") || ciphers[0].equals("AES-256")) {
                if (config.get().getMap("Settings").getMap("SubData").getRawString("Password", "").length() == 0) {
                    byte[] bytes = new byte[32];
                    new SecureRandom().nextBytes(bytes);
                    String random = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
                    if (random.length() > bytes.length) random = random.substring(0, bytes.length);
                    config.get().getMap("Settings").getMap("SubData").set("Password", random);
                    config.save();
                }

                subprotocol.registerCipher("AES", new AES(128, config.get().getMap("Settings").getMap("SubData").getRawString("Password")));
                subprotocol.registerCipher("AES-128", new AES(128, config.get().getMap("Settings").getMap("SubData").getRawString("Password")));
                subprotocol.registerCipher("AES-192", new AES(192, config.get().getMap("Settings").getMap("SubData").getRawString("Password")));
                subprotocol.registerCipher("AES-256", new AES(256, config.get().getMap("Settings").getMap("SubData").getRawString("Password")));

                Logger.get("SubData").info("Encrypting SubData with AES:");
                Logger.get("SubData").info("Use the password field in config.yml to allow clients to connect");
            } else if (ciphers[0].equals("DHE") || ciphers[0].equals("DHE-128") || ciphers[0].equals("DHE-192") || ciphers[0].equals("DHE-256")) {

                Logger.get("SubData").info("Encrypting SubData with DHE/AES:");
                Logger.get("SubData").info("SubData will negotiate what password to use automatically using the Diffie-Hellman Exchange");
            } else if (ciphers[0].equals("RSA") || ciphers[0].equals("RSA-2048") || ciphers[0].equals("RSA-3072") || ciphers[0].equals("RSA-4096")) {
                try {
                    int length = (ciphers[0].contains("-"))?Integer.parseInt(ciphers[0].split("-")[1]):2048;
                    if (!(new UniversalFile("SubServers:Cache").exists())) new UniversalFile("SubServers:Cache").mkdirs();
                    subprotocol.registerCipher("RSA", new RSA(length, new UniversalFile("SubServers:Cache:private.rsa.key"), new UniversalFile("SubServers:subdata.rsa.key")));
                    cipher = "RSA" + cipher.substring(ciphers[0].length());

                    Logger.get("SubData").info("Encrypting SubData with RSA:");
                    Logger.get("SubData").info("Copy your subdata.rsa.key to clients to allow them to connect");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            Logger.get("SubData").info("");
            subdata = subprotocol.open((config.get().getMap("Settings").getMap("SubData").getRawString("Address", "127.0.0.1:4391").split(":")[0].equals("0.0.0.0"))?null:InetAddress.getByName(config.get().getMap("Settings").getMap("SubData").getRawString("Address", "127.0.0.1:4391").split(":")[0]),
                    Integer.parseInt(config.get().getMap("Settings").getMap("SubData").getRawString("Address", "127.0.0.1:4391").split(":")[1]), cipher);
        } // Add new entries to Allowed-Connections
        for (String s : config.get().getMap("Settings").getMap("SubData").getStringList("Whitelist", new ArrayList<String>())) {
            try {
                subdata.whitelist(s);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        int plugins = 0;
        List<Runnable> listeners = (status)?api.reloadListeners:api.enableListeners;
        if (listeners.size() > 0) {
            Logger.get("SubServers").info(((status)?"Rel":"L")+"oading SubAPI Plugins...");
            for (Runnable obj : listeners) {
                try {
                    obj.run();
                    plugins++;
                } catch (Throwable e) {
                    new InvocationTargetException(e, "Problem " + ((status)?"reloading":"enabling") + " plugin").printStackTrace();
                }
            }
        }

        if (status) {
            for (Host host : api.getHosts().values()) if (host instanceof ClientHandler && ((ClientHandler) host).getSubData()[0] != null) ((SubDataClient) ((ClientHandler) host).getSubData()[0]).sendPacket(new PacketOutExReload(null));
            for (Server server : api.getServers().values()) if (server.getSubData()[0] != null) ((SubDataClient) server.getSubData()[0]).sendPacket(new PacketOutExReload(null));
        }

        reloading = false;
        Logger.get("SubServers").info(((plugins > 0)?plugins+" Plugin"+((plugins == 1)?"":"s")+", ":"") + ((proxies > 1)?proxies+" Proxies, ":"") + hosts + " Host"+((hosts == 1)?"":"s")+", " + servers + " Server"+((servers == 1)?"":"s")+", and " + subservers + " SubServer"+((subservers == 1)?"":"s")+" "+((status)?"re":"")+"loaded in " + new DecimalFormat("0.000").format((Calendar.getInstance().getTime().getTime() - begin) / 1000D) + "s");

        long scd = TimeUnit.SECONDS.toMillis(this.servers.get().getMap("Settings").getLong("Run-On-Launch-Timeout", 0L));
        if (autorun.size() > 0) for (Host host : api.getHosts().values()) {
            List<String> ar = new LinkedList<String>();
            for (String name : autorun) if (host.getSubServer(name) != null) ar.add(name);
            if (ar.size() > 0) new Thread(() -> {
                try {
                    while (running && begin == resetDate && !host.isAvailable()) {
                        Thread.sleep(250);
                    }
                    long init = Calendar.getInstance().getTime().getTime();
                    while (running && begin == resetDate && ar.size() > 0) {
                        SubServer server = host.getSubServer(ar.get(0));
                        ar.remove(0);
                        if (server != null && !server.isRunning()) {
                            server.start();
                            if (ar.size() > 0 && scd > 0) {
                                long sleep = Calendar.getInstance().getTime().getTime();
                                while (running && begin == resetDate && server.getSubData()[0] == null && Calendar.getInstance().getTime().getTime() - sleep < scd) {
                                    Thread.sleep(250);
                                }
                            }
                        }
                    }
                    if (running && begin == resetDate && Calendar.getInstance().getTime().getTime() - init >= 5000)
                        Logger.get("SubServers").info("The auto-start queue for " + host.getName() + " has been finished");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, "SubServers.Bungee::Automatic_Server_Starter(" + host.getName() + ")").start();
        }
    }

    private void post() {
        if (config.get().getMap("Settings").getBoolean("Override-Bungee-Commands", true)) {
            getPluginManager().registerCommand(null, SubCommand.BungeeServer.newInstance(this, "server").get());
            getPluginManager().registerCommand(null, new SubCommand.BungeeList(this, "glist"));
        }
        if (config.get().getMap("Settings").getBoolean("Smart-Fallback", true)) {
            setReconnectHandler(new SmartReconnectHandler());
        }
        getPluginManager().registerCommand(null, SubCommand.newInstance(this, "subservers").get());
        getPluginManager().registerCommand(null, SubCommand.newInstance(this, "subserver").get());
        getPluginManager().registerCommand(null, SubCommand.newInstance(this, "sub").get());
        GalaxiCommand.group(SubCommand.class);

        new Metrics(this);
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
    }

    /**
     * Reset all changes made by startListeners
     *
     * @see SubProxy#startListeners()
     */
    @Override
    public void stopListeners() {
        try {
            legServers.clear();
            legServers.putAll(getServers());
            if (api.disableListeners.size() > 0) {
                Logger.get("SubServers").info("Resetting SubAPI Plugins...");
                for (Runnable listener : api.disableListeners) {
                    try {
                        listener.run();
                    } catch (Throwable e) {
                        new InvocationTargetException(e, "Problem disabling plugin").printStackTrace();
                    }
                }
            }

            shutdown();

            subdata.close();

            for (ListenerInfo listener : getConfig().getListeners()) {
                if (UPnP.isUPnPAvailable() && UPnP.isMappedTCP(listener.getHost().getPort())) UPnP.closePortTCP(listener.getHost().getPort());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        super.stopListeners();
    } private void shutdown() throws Exception {
        api.ready = false;
        Logger.get("SubServers").info("Resetting Hosts and Server Data");
        List<String> hosts = new ArrayList<String>();
        hosts.addAll(this.hosts.keySet());

        for (String host : hosts) {
            api.forceRemoveHost(host);
        }
        running = false;
        this.hosts.clear();
        exServers.clear();

        for (String proxy : proxies.keySet()) {
            getPluginManager().callEvent(new SubRemoveProxyEvent(proxies.get(proxy)));
        }
        proxies.clear();

        for (ListenerInfo listener : getConfig().getListeners()) {
            if (UPnP.isUPnPAvailable() && UPnP.isMappedTCP(listener.getHost().getPort())) UPnP.closePortTCP(listener.getHost().getPort());
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

    /**
     * Reference a RedisBungee method via reflection
     *
     * @param method Method to reference
     * @param args Method arguments
     * @return Method Response
     */
    @SuppressWarnings("unchecked")
    public Object redis(String method, NamedContainer<Class<?>, ?>... args) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        if (getPluginManager().getPlugin("RedisBungee") != null) {
            Object api = getPluginManager().getPlugin("RedisBungee").getClass().getMethod("getApi").invoke(null);
            Class<?>[] classargs = new Class<?>[args.length];
            Object[] objargs = new Object[args.length];
            for (int i = 0; i < args.length; i++) {
                classargs[i] = args[i].name();
                objargs[i] = args[i].get();
                if (!classargs[i].isPrimitive() && !classargs[i].isInstance(objargs[i])) throw new ClassCastException(classargs[i].getCanonicalName() + " != " + objargs[i].getClass().getCanonicalName());
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
        return new LegacyServerMap(getServersCopy());
    }

    /**
     * Emulate Waterfall's getServersCopy()
     *
     * @return Server Map Copy
     */
    public Map<String, ServerInfo> getServersCopy() {
        HashMap<String, ServerInfo> servers = new HashMap<String, ServerInfo>();
        if (!api.ready) {
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
        return getServersCopy().get(name);
    }

    @EventHandler(priority = Byte.MAX_VALUE)
    public void ping(ProxyPingEvent e) {
        int offline = 0;
        for (String name : e.getConnection().getListener().getServerPriority()) {
            ServerInfo server = api.getServer(name.toLowerCase());
            if (server == null) server = getServerInfo(name);
            if (server == null || (server instanceof SubServer && !((SubServer) server).isRunning())) offline++;
        }

        if (offline >= e.getConnection().getListener().getServerPriority().size()) {
            e.setResponse(new ServerPing(e.getResponse().getVersion(), e.getResponse().getPlayers(), new TextComponent(api.getLang("SubServers", "Bungee.Ping.Offline")), null));
        }
    }

    @SuppressWarnings("deprecation")
    @EventHandler(priority = Byte.MAX_VALUE)
    public void validate(ServerConnectEvent e) {
        Map<String, ServerInfo> servers = new TreeMap<String, ServerInfo>(api.getServers());
        if (servers.keySet().contains(e.getTarget().getName().toLowerCase()) && e.getTarget() != servers.get(e.getTarget().getName().toLowerCase())) {
            e.setTarget(servers.get(e.getTarget().getName().toLowerCase()));
        } else {
            servers = getServers();
            if (servers.keySet().contains(e.getTarget().getName()) && e.getTarget() != servers.get(e.getTarget().getName())) {
                e.setTarget(servers.get(e.getTarget().getName()));
            }
        }

        if (!e.getTarget().canAccess(e.getPlayer())) {
            if (e.getPlayer().getServer() == null || fallbackLimbo.keySet().contains(e.getPlayer().getUniqueId())) {
                if (!fallbackLimbo.keySet().contains(e.getPlayer().getUniqueId()) || fallbackLimbo.get(e.getPlayer().getUniqueId()).contains(e.getTarget())) {
                    ServerKickEvent kick = new ServerKickEvent(e.getPlayer(), e.getTarget(), new BaseComponent[]{
                            new TextComponent(getTranslation("no_server_permission"))
                    }, null, ServerKickEvent.State.CONNECTING);
                    fallback(kick);
                    if (!kick.isCancelled()) e.getPlayer().disconnect(kick.getKickReasonComponent());
                    if (e.getPlayer().getServer() != null) e.setCancelled(true);
                }
            } else {
                e.getPlayer().sendMessage(getTranslation("no_server_permission"));
                e.setCancelled(true);
            }
        } else if (e.getPlayer().getServer() != null && !fallbackLimbo.keySet().contains(e.getPlayer().getUniqueId()) && e.getTarget() instanceof SubServer && !((SubServer) e.getTarget()).isRunning()) {
            e.getPlayer().sendMessage(api.getLang("SubServers", "Bungee.Server.Offline"));
            e.setCancelled(true);
        }

        if (fallbackLimbo.keySet().contains(e.getPlayer().getUniqueId())) {
            if (fallbackLimbo.get(e.getPlayer().getUniqueId()).contains(e.getTarget())) {
                fallbackLimbo.get(e.getPlayer().getUniqueId()).remove(e.getTarget());
            } else if (e.getPlayer().getServer() != null) {
                e.setCancelled(true);
            }
        }
    }

    @SuppressWarnings("deprecation")
    @EventHandler(priority = Byte.MAX_VALUE)
    public void fallback(ServerKickEvent e) {
        if (e.getPlayer() instanceof UserConnection && config.get().getMap("Settings").getBoolean("Smart-Fallback", true)) {
            Map<String, ServerInfo> fallbacks;
            if (!fallbackLimbo.keySet().contains(e.getPlayer().getUniqueId())) {
                fallbacks = SmartReconnectHandler.getFallbackServers(e.getPlayer().getPendingConnection().getListener());
            } else {
                fallbacks = new LinkedHashMap<String, ServerInfo>();
                for (ServerInfo server : fallbackLimbo.get(e.getPlayer().getUniqueId())) fallbacks.put(server.getName(), server);
            }

            fallbacks.remove(e.getKickedFrom().getName());
            if (!fallbacks.isEmpty()) {
                e.setCancelled(true);
                e.getPlayer().sendMessage(api.getLang("SubServers", "Bungee.Feature.Smart-Fallback").replace("$str$", (e.getKickedFrom() instanceof Server)?((Server) e.getKickedFrom()).getDisplayName():e.getKickedFrom().getName()).replace("$msg$", e.getKickReason()));
                if (!fallbackLimbo.keySet().contains(e.getPlayer().getUniqueId())) fallbackLimbo.put(e.getPlayer().getUniqueId(), new LinkedList<>(fallbacks.values()));

                ServerInfo next = new LinkedList<Map.Entry<String, ServerInfo>>(fallbacks.entrySet()).getFirst().getValue();
                e.setCancelServer(next);
                if (Util.isException(() -> Util.reflect(ServerKickEvent.class.getDeclaredMethod("setCancelServers", ServerInfo[].class), e, (Object) fallbacks.values().toArray(new ServerInfo[0])))) {
                    ((UserConnection) e.getPlayer()).setServerJoinQueue(new LinkedList<>(fallbacks.keySet()));
                    ((UserConnection) e.getPlayer()).connect(next, null, true);
                }
            }
        }
    }
    @SuppressWarnings("deprecation")
    @EventHandler(priority = Byte.MAX_VALUE)
    public void fallbackFound(ServerConnectedEvent e) {
        if (fallbackLimbo.keySet().contains(e.getPlayer().getUniqueId())) new Timer("SubServers.Bungee::Fallback_Limbo_Timer(" + e.getPlayer().getUniqueId() + ')').schedule(new TimerTask() {
            @Override
            public void run() {
               if (e.getPlayer().getServer() != null && !((UserConnection) e.getPlayer()).isDimensionChange() && e.getPlayer().getServer().getInfo().getAddress().equals(e.getServer().getInfo().getAddress())) {
                   fallbackLimbo.remove(e.getPlayer().getUniqueId());
                   e.getPlayer().sendMessage(api.getLang("SubServers", "Bungee.Feature.Smart-Fallback.Result").replace("$str$", (e.getServer().getInfo() instanceof Server)?((Server) e.getServer().getInfo()).getDisplayName():e.getServer().getInfo().getName()));
               }
            }
        }, 1000);
    }
    @EventHandler(priority = Byte.MIN_VALUE)
    public void resetLimbo(PlayerDisconnectEvent e) {
        fallbackLimbo.remove(e.getPlayer().getUniqueId());
        SubCommand.players.remove(e.getPlayer().getUniqueId());
    }

    @EventHandler(priority = Byte.MIN_VALUE)
    public void unsudo(SubStoppedEvent e) {
        if (sudo == e.getServer()) {
            sudo = null;
            Logger.get("SubServers").info("Reverting to the BungeeCord Console");
        }
    }
}

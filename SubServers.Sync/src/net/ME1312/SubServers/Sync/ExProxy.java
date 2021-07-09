package net.ME1312.SubServers.Sync;

import net.ME1312.Galaxi.Library.Config.YAMLConfig;
import net.ME1312.Galaxi.Library.Container.Container;
import net.ME1312.Galaxi.Library.Container.Pair;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.UniversalFile;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.SubData.Client.DataClient;
import net.ME1312.SubData.Client.Encryption.AES;
import net.ME1312.SubData.Client.Encryption.DHE;
import net.ME1312.SubData.Client.Encryption.RSA;
import net.ME1312.SubData.Client.Library.DisconnectReason;
import net.ME1312.SubData.Client.SubDataClient;
import net.ME1312.SubServers.Bungee.BungeeCommon;
import net.ME1312.SubServers.Bungee.Library.Compatibility.Logger;
import net.ME1312.SubServers.Bungee.Library.Fallback.FallbackState;
import net.ME1312.SubServers.Bungee.Library.Fallback.SmartFallback;
import net.ME1312.SubServers.Client.Common.Network.Packet.PacketDisconnectPlayer;
import net.ME1312.SubServers.Sync.Event.*;
import net.ME1312.SubServers.Sync.Library.ConfigUpdater;
import net.ME1312.SubServers.Sync.Library.Metrics;
import net.ME1312.SubServers.Sync.Network.Packet.PacketExSyncPlayer;
import net.ME1312.SubServers.Sync.Network.SubProtocol;
import net.ME1312.SubServers.Sync.Server.CachedPlayer;
import net.ME1312.SubServers.Sync.Server.ServerImpl;
import net.ME1312.SubServers.Sync.Server.SubServerImpl;

import com.dosse.upnp.UPnP;
import com.google.gson.Gson;
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
import net.md_5.bungee.connection.InitialHandler;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.util.CaseInsensitiveMap;

import java.io.*;
import java.net.InetAddress;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Main Plugin Class
 */
public final class ExProxy extends BungeeCommon implements Listener {
    HashMap<Integer, SubDataClient> subdata = new HashMap<Integer, SubDataClient>();
    Pair<Long, Map<String, Map<String, String>>> lang = null;
    public final Map<String, ServerImpl> servers = new TreeMap<String, ServerImpl>();
    public final HashMap<UUID, ServerImpl> rPlayerLinkS = new HashMap<UUID, ServerImpl>();
    public final HashMap<UUID, String> rPlayerLinkP = new HashMap<UUID, String>();
    public final HashMap<UUID, CachedPlayer> rPlayers = new HashMap<UUID, CachedPlayer>();
    private final HashMap<UUID, FallbackState> fallback = new HashMap<UUID, FallbackState>();

    public final PrintStream out;
    public final UniversalFile dir = new UniversalFile(new File(System.getProperty("user.dir")));
    public YAMLConfig config;
    public final Plugin plugin;
    public final SubAPI api = new SubAPI(this);
    public SubProtocol subprotocol;
    public static final Version version = Version.fromString("2.17.1a");

    public final boolean isPatched;
    public long lastReload = -1;
    private long resetDate = 0;
    private boolean reconnect = false;
    private boolean posted = false;

    ExProxy(PrintStream out, boolean isPatched) throws Exception {
        super(SubAPI::getInstance);
        this.isPatched = isPatched;

        Logger.get("SubServers").info("Loading SubServers.Sync v" + version.toString() + " Libraries (for Minecraft " + api.getGameVersion()[api.getGameVersion().length - 1] + ")");
        Util.isException(() -> new CachedPlayer((ProxiedPlayer) null)); // runs <clinit>

        this.out = out;
        if (!(new UniversalFile(dir, "config.yml").exists())) {
            Util.copyFromJar(ExProxy.class.getClassLoader(), "net/ME1312/SubServers/Bungee/Library/Files/bungee.yml", new UniversalFile(dir, "config.yml").getPath());
            YAMLConfig tmp = new YAMLConfig(new UniversalFile("config.yml"));
            tmp.get().set("stats", UUID.randomUUID().toString());
            tmp.save();
            Logger.get("SubServers").info("Created ./config.yml");
        }
        UniversalFile dir = new UniversalFile(this.dir, "SubServers");
        dir.mkdir();

        ConfigUpdater.updateConfig(new UniversalFile(dir, "sync.yml"));
        config = new YAMLConfig(new UniversalFile(dir, "sync.yml"));

        SmartFallback.addInspector((player, server) -> {
            double confidence = 0;
            if (server instanceof ServerImpl) {
                if (!((ServerImpl) server).isHidden()) confidence++;
                if (!((ServerImpl) server).isRestricted()) confidence++;
                if (((ServerImpl) server).getSubData()[0] != null) confidence++;

                if (player != null) {
                    if (((ServerImpl) server).canAccess(player)) confidence++;
                }
            } if (server instanceof SubServerImpl) {
                if (!((SubServerImpl) server).isRunning()) return null;
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

        {
            PluginDescription description = new PluginDescription();
            description.setName("SubServers-Sync");
            description.setMain(net.ME1312.SubServers.Sync.Library.Compatibility.Plugin.class.getCanonicalName());
            description.setFile(Util.getDespiteException(() -> new File(ExProxy.class.getProtectionDomain().getCodeSource().getLocation().toURI()), null));
            description.setVersion(version.toString());
            description.setAuthor("ME1312");

            String stage = "access";
            Plugin plugin = null;
            try {
                plugin = new Plugin(this, description) {
                    // SubServers.Sync doesn't deploy code here at this time.
                };

                if (plugin.getDescription() == null) {
                    stage = "initialize";
                    Util.reflect(Plugin.class.getDeclaredMethod("init", ProxyServer.class, PluginDescription.class), plugin, this, description);
                }
            } catch (Throwable e) {
                Logger.get("SubServers").warning("Could not " + stage + " plugin emulation");
            } finally {
                this.plugin = plugin;
            }
        }
        getPluginManager().registerListener(plugin, this);

        Logger.get("SubServers").info("Loading BungeeCord Libraries...");
    }

    /**
     * Load Hosts, Servers, SubServers, and SubData Direct
     */
    @Override
    public void startListeners() {
        try {
            SmartFallback.dns_forward = config.get().getMap("Settings").getMap("Smart-Fallback", new ObjectMap<>()).getBoolean("DNS-Forward", false);

            resetDate = Calendar.getInstance().getTime().getTime();
            ConfigUpdater.updateConfig(new UniversalFile(dir, "SubServers:sync.yml"));
            config.reload();

            subprotocol.unregisterCipher("AES");
            subprotocol.unregisterCipher("AES-128");
            subprotocol.unregisterCipher("AES-192");
            subprotocol.unregisterCipher("AES-256");
            subprotocol.unregisterCipher("RSA");

            api.name = config.get().getMap("Settings").getMap("SubData").getString("Name", null);

            if (config.get().getMap("Settings").getMap("SubData").getRawString("Password", "").length() > 0) {
                subprotocol.registerCipher("AES", new AES(128, config.get().getMap("Settings").getMap("SubData").getRawString("Password")));
                subprotocol.registerCipher("AES-128", new AES(128, config.get().getMap("Settings").getMap("SubData").getRawString("Password")));
                subprotocol.registerCipher("AES-192", new AES(192, config.get().getMap("Settings").getMap("SubData").getRawString("Password")));
                subprotocol.registerCipher("AES-256", new AES(256, config.get().getMap("Settings").getMap("SubData").getRawString("Password")));

                Logger.get("SubData").info("AES Encryption Available");
            }
            if (new UniversalFile(dir, "SubServers:subdata.rsa.key").exists()) {
                try {
                    subprotocol.registerCipher("RSA", new RSA(new UniversalFile(dir, "SubServers:subdata.rsa.key")));
                    Logger.get("SubData").info("RSA Encryption Available");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            reconnect = true;
            Logger.get("SubData").info("");
            Logger.get("SubData").info("Connecting to /" + config.get().getMap("Settings").getMap("SubData").getRawString("Address", "127.0.0.1:4391"));
            connect(Logger.get("SubData"), null);

            super.startListeners();

            if (UPnP.isUPnPAvailable()) {
                if (config.get().getMap("Settings").getMap("UPnP", new ObjectMap<String>()).getBoolean("Forward-Proxy", true)) for (ListenerInfo listener : getConfig().getListeners()) {
                    UPnP.openPortTCP(listener.getHost().getPort());
                }
            } else {
                getLogger().warning("UPnP is currently unavailable. Ports may not be automatically forwarded on this device.");
            }

            if (!posted) {
                posted = true;
                post();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void connect(java.util.logging.Logger log, Pair<DisconnectReason, DataClient> disconnect) throws IOException {
        int reconnect = config.get().getMap("Settings").getMap("SubData").getInt("Reconnect", 60);
        if (disconnect == null || (this.reconnect && reconnect > 0 && disconnect.key() != DisconnectReason.PROTOCOL_MISMATCH && disconnect.key() != DisconnectReason.ENCRYPTION_MISMATCH)) {
            long reset = resetDate;
            Timer timer = new Timer("SubServers.Sync::SubData_Reconnect_Handler");
            if (disconnect != null) log.info("Attempting reconnect in " + reconnect + " seconds");
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    try {
                        if (reset == resetDate && (subdata.getOrDefault(0, null) == null || subdata.get(0).isClosed())) {
                            SubDataClient open = subprotocol.open(InetAddress.getByName(config.get().getMap("Settings").getMap("SubData").getRawString("Address", "127.0.0.1:4391").split(":")[0]),
                                    Integer.parseInt(config.get().getMap("Settings").getMap("SubData").getRawString("Address", "127.0.0.1:4391").split(":")[1]));

                            if (subdata.getOrDefault(0, null) != null) subdata.get(0).reconnect(open);
                            subdata.put(0, open);
                        }
                        timer.cancel();
                    } catch (IOException e) {
                        Logger.get("SubData").info("Connection was unsuccessful, retrying in " + reconnect + " seconds");
                    }
                }
            }, (disconnect == null)?0:TimeUnit.SECONDS.toMillis(reconnect), TimeUnit.SECONDS.toMillis(reconnect));
        }
    }

    private void post() {
        if (!config.get().getMap("Settings").getRawStringList("Disabled-Overrides", Collections.emptyList()).contains("/server"))
            getPluginManager().registerCommand(plugin, new SubCommand.BungeeServer(this, "server"));
        if (!config.get().getMap("Settings").getRawStringList("Disabled-Overrides", Collections.emptyList()).contains("/glist"))
            getPluginManager().registerCommand(plugin, new SubCommand.BungeeList(this, "glist"));

        getPluginManager().registerCommand(plugin, new SubCommand(this, "subservers"));
        getPluginManager().registerCommand(plugin, new SubCommand(this, "subserver"));
        getPluginManager().registerCommand(plugin, new SubCommand(this, "sub"));

        if (getReconnectHandler() != null && getReconnectHandler().getClass().equals(SmartFallback.class))
            setReconnectHandler(new SmartFallback(config.get().getMap("Settings").getMap("Smart-Fallback", new ObjectMap<>()))); // Re-initialize Smart Fallback

        new Metrics(this);
        new Timer("SubServers.Sync::Routine_Update_Check").schedule(new TimerTask() {
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
                    if (updcount > 0) Logger.get("SubServers").info("SubServers.Sync v" + updversion + " is available. You are " + updcount + " version" + ((updcount == 1)?"":"s") + " behind.");
                } catch (Exception e) {}
            }
        }, 0, TimeUnit.DAYS.toMillis(2));

        int rpec_i = config.get().getMap("Settings").getInt("RPEC-Check-Interval", 300);
        int rpec_s = rpec_i - new Random().nextInt((rpec_i / 3) + 1);
        new Timer("SubServers.Sync::RemotePlayer_Error_Checking").schedule(new TimerTask() {
            @Override
            public void run() {
                if (api.getName() != null && api.getSubDataNetwork()[0] != null && !api.getSubDataNetwork()[0].isClosed()) {
                    api.getProxy(api.getName(), proxy -> {
                        synchronized (rPlayers) {
                            ArrayList<CachedPlayer> add = new ArrayList<CachedPlayer>();
                            for (ProxiedPlayer player : getPlayers()) {
                                if (!rPlayers.containsKey(player.getUniqueId())) { // Add players that don't exist
                                    CachedPlayer p = new CachedPlayer(player);
                                    rPlayerLinkP.put(player.getUniqueId(), p.getProxyName().toLowerCase());
                                    rPlayers.put(player.getUniqueId(), p);
                                    if (player.getServer().getInfo() instanceof ServerImpl) rPlayerLinkS.put(player.getUniqueId(), (ServerImpl) player.getServer().getInfo());
                                    add.add(p);
                                }
                            }
                            ArrayList<CachedPlayer> remove = new ArrayList<CachedPlayer>();
                            for (Pair<String, UUID> player : proxy.getPlayers()) { // Remove players that shouldn't exist
                                if (getPlayer(player.value()) == null) {
                                    remove.add(rPlayers.get(player.value()));
                                    rPlayerLinkS.remove(player.value());
                                    rPlayerLinkP.remove(player.value());
                                    rPlayers.remove(player.value());
                                }
                            }
                            for (UUID player : Util.getBackwards(rPlayerLinkP, api.getName().toLowerCase())) { // Remove players that shouldn't exist (internally)
                                if (getPlayer(player) == null) {
                                    rPlayerLinkS.remove(player);
                                    rPlayerLinkP.remove(player);
                                    rPlayers.remove(player);
                                }
                            }
                            LinkedList<PacketExSyncPlayer> packets = new LinkedList<PacketExSyncPlayer>(); // Compile change data for external proxies
                            if (add.size() > 0) packets.add(new PacketExSyncPlayer(true, add.toArray(new CachedPlayer[0])));
                            if (remove.size() > 0) packets.add(new PacketExSyncPlayer(false, remove.toArray(new CachedPlayer[0])));
                            if (packets.size() > 0) {
                                PacketExSyncPlayer[] packet = packets.toArray(new PacketExSyncPlayer[0]);
                                if (api.getSubDataNetwork()[0] != null) {
                                    ((SubDataClient) api.getSubDataNetwork()[0]).sendPacket(packet);
                                }
                            }
                        }
                    });
                }
            }
        }, TimeUnit.SECONDS.toMillis(rpec_s), TimeUnit.SECONDS.toMillis(rpec_i));
    }

    /**
     * Further override BungeeCord's signature when patched into the same jar
     *
     * @return Software Name
     */
    @Override // SubServers.Bungee is used here to hide the fact that this isn't the controller instance
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
        if (servers.size() > 0) {
            Map<String, ServerInfo> servers = new CaseInsensitiveMap<ServerInfo>();
            for (ServerInfo server : this.servers.values()) servers.put(server.getName(), server);
            return servers;
        } else {
            return super.getServers();
        }
    }

    /**
     * Emulate Waterfall's getServersCopy()
     *
     * @return Server Map Copy (which is the default, by the way)
     */
    public Map<String, ServerInfo> getServersCopy() {
        return getServers();
    }

    /**
     * Force BungeeCord's implementation of getServerInfo()
     *
     * @return ServerInfo
     */
    @Override
    public ServerInfo getServerInfo(String name) {
        return getServers().get(name);
    }

    /**
     * Reset all changes made by startListeners
     *
     * @see ExProxy#startListeners()
     */
    @Override
    public void stopListeners() {
        try {
            Logger.get("SubServers").info("Removing synced data");
            servers.clear();

            reconnect = false;
            ArrayList<SubDataClient> tmp = new ArrayList<SubDataClient>();
            tmp.addAll(subdata.values());
            for (SubDataClient client : tmp) if (client != null) {
                client.close();
                Util.isException(client::waitFor);
            }
            subdata.clear();
            subdata.put(0, null);

            if (UPnP.isUPnPAvailable()) {
                for (ListenerInfo listener : getConfig().getListeners()) {
                    if (UPnP.isMappedTCP(listener.getHost().getPort())) UPnP.closePortTCP(listener.getHost().getPort());
                }
            }

            rPlayerLinkS.clear();
            rPlayerLinkP.clear();
            rPlayers.clear();
        } catch (Exception e) {
            e.printStackTrace();
        }

        super.stopListeners();
    }

    @EventHandler(priority = Byte.MIN_VALUE)
    public void ping_passthrough(ProxyPingEvent e) {
        boolean dynamic;
        ServerInfo override;
        if ((dynamic = SmartFallback.getForcedHost(e.getConnection()) == null) && getReconnectHandler() instanceof SmartFallback && (override = SmartFallback.getDNS(e.getConnection())) != null) {
            if (!(override instanceof SubServerImpl) || ((SubServerImpl) override).isRunning()) {
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
                    if (!mode) while (lock.value) Util.isException(() -> Thread.sleep(4));
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
            if (override instanceof SubServerImpl && !((SubServerImpl) override).isRunning()) {
                e.setResponse(new ServerPing(e.getResponse().getVersion(), e.getResponse().getPlayers(), new TextComponent(api.getLang("SubServers", "Bungee.Ping.Offline")), e.getResponse().getFaviconObject()));
            }
        } else {
            int offline = 0;
            for (String name : e.getConnection().getListener().getServerPriority()) {
                ServerInfo server = getServerInfo(name);
                if (server instanceof SubServerImpl && !((SubServerImpl) server).isRunning()) offline++;
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
            CachedPlayer player = rPlayers.get(e.getConnection().getUniqueId());
            if (player.getProxyName() != null && player.getProxyName().equalsIgnoreCase(api.getName())) {
                ProxiedPlayer p = getPlayer(player.getUniqueId());
                if (p != null) p.disconnect(new TextComponent(getTranslation("already_connected_proxy")));
            } else {
                ((SubDataClient) api.getSubDataNetwork()[0]).sendPacket(new PacketDisconnectPlayer(new UUID[]{ player.getUniqueId() }, getTranslation("already_connected_proxy")));
            }
        }
    }

    @EventHandler(priority = Byte.MAX_VALUE)
    public void validate(ServerConnectEvent e) {
        if (e.getPlayer().isConnected()) {
            Map<String, ServerInfo> servers = new TreeMap<String, ServerInfo>(this.servers);
            if (servers.keySet().contains(e.getTarget().getName().toLowerCase()) && e.getTarget() != servers.get(e.getTarget().getName().toLowerCase())) {
                e.setTarget(servers.get(e.getTarget().getName().toLowerCase()));
            } else {
                servers = getServers();
                if (servers.keySet().contains(e.getTarget().getName()) && e.getTarget() != servers.get(e.getTarget().getName())) {
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
            } else if (e.getPlayer().getServer() != null && !fallback.containsKey(e.getPlayer().getUniqueId()) && e.getTarget() instanceof SubServerImpl && !((SubServerImpl) e.getTarget()).isRunning()) {
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
                ObjectMap<String> raw = CachedPlayer.translate(e.getPlayer());
                raw.set("server", e.getServer().getInfo().getName());
                CachedPlayer player = new CachedPlayer(raw);
                rPlayerLinkP.put(player.getUniqueId(), player.getProxyName().toLowerCase());
                rPlayers.put(player.getUniqueId(), player);
                if (e.getServer().getInfo() instanceof ServerImpl) rPlayerLinkS.put(player.getUniqueId(), (ServerImpl) e.getServer().getInfo());
                if (api.getSubDataNetwork()[0] != null) {
                    ((SubDataClient) api.getSubDataNetwork()[0]).sendPacket(new PacketExSyncPlayer(true, player));
                }
            }


            if (fallback.containsKey(e.getPlayer().getUniqueId())) {
                fallback.get(e.getPlayer().getUniqueId()).done(() -> {
                    if (e.getPlayer().getServer() != null && !((UserConnection) e.getPlayer()).isDimensionChange() && e.getPlayer().getServer().getInfo().getName().equals(e.getServer().getInfo().getName())) {
                        fallback.remove(e.getPlayer().getUniqueId());
                        e.getPlayer().sendMessage(api.getLang("SubServers", "Bungee.Feature.Smart-Fallback.Result").replace("$str$", (e.getServer().getInfo() instanceof ServerImpl)?((ServerImpl) e.getServer().getInfo()).getDisplayName():e.getServer().getInfo().getName()));
                    }
                }, getConfig().getServerConnectTimeout() + 500);
            }
        }
    }

    @SuppressWarnings("deprecation")
    @EventHandler(priority = Byte.MAX_VALUE)
    public void fallback(ServerKickEvent e) {
        if (e.getPlayer().isConnected() && config.get().getMap("Settings").getMap("Smart-Fallback", new ObjectMap<>()).getBoolean("Fallback", true)) {
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
                e.getPlayer().sendMessage(api.getLang("SubServers", "Bungee.Feature.Smart-Fallback").replace("$str$", (e.getKickedFrom() instanceof ServerImpl)?((ServerImpl) e.getKickedFrom()).getDisplayName():e.getKickedFrom().getName()).replace("$msg$", e.getKickReason()));
                if (init) fallback.put(e.getPlayer().getUniqueId(), state);

                e.setCancelServer(state.servers.getFirst());
            }
        }
    }

    @EventHandler(priority = Byte.MAX_VALUE)
    public void disconnected(PlayerDisconnectEvent e) {
        UUID id = e.getPlayer().getUniqueId();
        fallback.remove(id);
        SubCommand.permitted.remove(id);

        synchronized (rPlayers) {
            if (rPlayers.containsKey(id) && (!rPlayerLinkP.containsKey(id) || rPlayerLinkP.get(id).equalsIgnoreCase(api.getName()))) {
                CachedPlayer player = rPlayers.get(id);
                rPlayerLinkS.remove(id);
                rPlayerLinkP.remove(id);
                rPlayers.remove(id);

                if (api.getSubDataNetwork()[0] != null) {
                    ((SubDataClient) api.getSubDataNetwork()[0]).sendPacket(new PacketExSyncPlayer(false, player));
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Map<Integer, UUID> getSubDataAsMap(net.ME1312.SubServers.Client.Common.Network.API.Server server) {
        HashMap<Integer, UUID> map = new HashMap<Integer, UUID>();
        ObjectMap<Integer> subdata = new ObjectMap<Integer>((Map<Integer, ?>) server.getRaw().getObject("subdata"));
        for (Integer channel : subdata.getKeys()) map.put(channel, subdata.getUUID(channel));
        return map;
    }

    @EventHandler(priority = Byte.MIN_VALUE)
    public void add(SubAddServerEvent e) {
        api.getServer(e.getServer(), server -> {
            if (server != null) {
                if (server instanceof net.ME1312.SubServers.Client.Common.Network.API.SubServer) {
                    servers.put(server.getName().toLowerCase(), SubServerImpl.construct(server.getSignature(), server.getName(), server.getDisplayName(), server.getAddress(),
                            getSubDataAsMap(server), server.getMotd(), server.isHidden(), server.isRestricted(), server.getWhitelist(), ((net.ME1312.SubServers.Client.Common.Network.API.SubServer) server).isRunning()));
                    Logger.get("SubServers").info("Added SubServer: " + e.getServer());
                } else {
                    servers.put(server.getName().toLowerCase(), ServerImpl.construct(server.getSignature(), server.getName(), server.getDisplayName(), server.getAddress(),
                            getSubDataAsMap(server), server.getMotd(), server.isHidden(), server.isRestricted(), server.getWhitelist()));
                    Logger.get("SubServers").info("Added Server: " + e.getServer());
                }
            } else System.out.println("PacketDownloadServerInfo(" + e.getServer() + ") returned with an invalid response");
        });
    }

    public Boolean merge(net.ME1312.SubServers.Client.Common.Network.API.Server server) {
        ServerImpl current = servers.get(server.getName().toLowerCase());
        if (server instanceof net.ME1312.SubServers.Client.Common.Network.API.SubServer || !(current instanceof SubServerImpl)) {
            if (current == null || !current.getSignature().equals(server.getSignature())) {
                if (server instanceof net.ME1312.SubServers.Client.Common.Network.API.SubServer) {
                    servers.put(server.getName().toLowerCase(), SubServerImpl.construct(server.getSignature(), server.getName(), server.getDisplayName(), server.getAddress(),
                            getSubDataAsMap(server), server.getMotd(), server.isHidden(), server.isRestricted(), server.getWhitelist(), ((net.ME1312.SubServers.Client.Common.Network.API.SubServer) server).isRunning()));
                } else {
                    servers.put(server.getName().toLowerCase(), ServerImpl.construct(server.getSignature(), server.getName(), server.getDisplayName(), server.getAddress(),
                            getSubDataAsMap(server), server.getMotd(), server.isHidden(), server.isRestricted(), server.getWhitelist()));
                }

                Logger.get("SubServers").info("Added "+((server instanceof net.ME1312.SubServers.Client.Common.Network.API.SubServer)?"Sub":"")+"Server: " + server.getName());
                return true;
            } else {
                if (server instanceof net.ME1312.SubServers.Client.Common.Network.API.SubServer) {
                    if (((net.ME1312.SubServers.Client.Common.Network.API.SubServer) server).isRunning() != ((SubServerImpl) current).isRunning())
                        ((SubServerImpl) current).setRunning(((net.ME1312.SubServers.Client.Common.Network.API.SubServer) server).isRunning());
                }
                if (!server.getMotd().equals(current.getMotd()))
                    current.setMotd(server.getMotd());
                if (server.isHidden() != current.isHidden())
                    current.setHidden(server.isHidden());
                if (server.isRestricted() != current.isRestricted())
                    current.setRestricted(server.isRestricted());
                if (!server.getDisplayName().equals(current.getDisplayName()))
                    current.setDisplayName(server.getDisplayName());

                Logger.get("SubServers").info("Re-added "+((server instanceof net.ME1312.SubServers.Client.Common.Network.API.SubServer)?"Sub":"")+"Server: " + server.getName());
                return false;
            }
        }
        return null;
    }

    @EventHandler(priority = Byte.MIN_VALUE)
    public void start(SubStartEvent e) {
        if (servers.keySet().contains(e.getServer().toLowerCase()) && servers.get(e.getServer().toLowerCase()) instanceof SubServerImpl)
            ((SubServerImpl) servers.get(e.getServer().toLowerCase())).setRunning(true);
    }

    @EventHandler(priority = Byte.MIN_VALUE)
    public void stop(SubStoppedEvent e) {
        if (servers.keySet().contains(e.getServer().toLowerCase()) && servers.get(e.getServer().toLowerCase()) instanceof SubServerImpl)
            ((SubServerImpl) servers.get(e.getServer().toLowerCase())).setRunning(false);
    }

    @EventHandler(priority = Byte.MIN_VALUE)
    public void remove(SubRemoveServerEvent e) {
        if (servers.keySet().contains(e.getServer().toLowerCase()))
            servers.remove(e.getServer().toLowerCase());
            Logger.get("SubServers").info("Removed Server: " + e.getServer());
    }
}

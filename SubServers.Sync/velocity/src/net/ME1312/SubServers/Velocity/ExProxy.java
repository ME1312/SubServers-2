package net.ME1312.SubServers.Velocity;

import net.ME1312.Galaxi.Library.Config.YAMLConfig;
import net.ME1312.Galaxi.Library.Container.Pair;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Try;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.SubData.Client.DataClient;
import net.ME1312.SubData.Client.Encryption.AES;
import net.ME1312.SubData.Client.Encryption.DHE;
import net.ME1312.SubData.Client.Encryption.RSA;
import net.ME1312.SubData.Client.Library.DisconnectReason;
import net.ME1312.SubData.Client.SubDataClient;
import net.ME1312.SubServers.Client.Common.Network.Packet.PacketDisconnectPlayer;
import net.ME1312.SubServers.Velocity.Event.SubAddServerEvent;
import net.ME1312.SubServers.Velocity.Event.SubRemoveServerEvent;
import net.ME1312.SubServers.Velocity.Event.SubStartEvent;
import net.ME1312.SubServers.Velocity.Event.SubStoppedEvent;
import net.ME1312.SubServers.Velocity.Library.Compatibility.ChatColor;
import net.ME1312.SubServers.Velocity.Library.Compatibility.Logger;
import net.ME1312.SubServers.Velocity.Library.ConfigUpdater;
import net.ME1312.SubServers.Velocity.Library.Fallback.FallbackState;
import net.ME1312.SubServers.Velocity.Library.Fallback.SmartFallback;
import net.ME1312.SubServers.Velocity.Library.Metrics;
import net.ME1312.SubServers.Velocity.Network.Packet.PacketExSyncPlayer;
import net.ME1312.SubServers.Velocity.Network.SubProtocol;
import net.ME1312.SubServers.Velocity.Server.CachedPlayer;
import net.ME1312.SubServers.Velocity.Server.ServerData;
import net.ME1312.SubServers.Velocity.Server.SubServerData;

import com.dosse.upnp.UPnP;
import com.google.gson.Gson;
import com.google.inject.Inject;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.event.proxy.*;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.PluginDescription;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import com.velocitypowered.api.proxy.server.ServerPing;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Plugin(id = "subservers-sync", name = "SubServers-Sync", authors = "ME1312", version = "2.18.2a", url = "https://github.com/ME1312/SubServers-2", description = "Dynamically sync player and server connection info over multiple proxy instances")
public class ExProxy {

    HashMap<Integer, SubDataClient> subdata = new HashMap<Integer, SubDataClient>();
    Pair<Long, Map<String, Map<String, String>>> lang = null;
    public final Map<ServerInfo, ServerData> servers = new TreeMap<ServerInfo, ServerData>();
    public final HashMap<UUID, ServerData> rPlayerLinkS = new HashMap<UUID, ServerData>();
    public final HashMap<UUID, String> rPlayerLinkP = new HashMap<UUID, String>();
    public final HashMap<UUID, CachedPlayer> rPlayers = new HashMap<UUID, CachedPlayer>();
    private final HashMap<UUID, FallbackState> fallback = new HashMap<UUID, FallbackState>();

    public final org.apache.logging.log4j.Logger out;
    public final File dir = new File(System.getProperty("user.dir"));
    public YAMLConfig config;
    public final PluginDescription plugin;
    private final ProxyServer proxy;
    private final Metrics.Factory metrics;
    public final SubAPI api = new SubAPI(this);
    public SubProtocol subprotocol;
    public final Version version;

    public boolean running = false;
    public long lastReload = -1;
    private long resetDate = 0;
    private boolean reconnect = false;
    private boolean posted = false;

    @Inject
    private ExProxy(ProxyServer proxy, PluginContainer plugin, Metrics.Factory metrics) throws Exception {
        this.proxy = proxy;
        this.metrics = metrics;
        this.version = Version.fromString((this.plugin = plugin.getDescription()).getVersion().get());
        Util.reflect(Logger.class.getDeclaredField("parent"), null, (this.out = Util.reflect(proxy.getClass().getDeclaredField("logger"), proxy)));

        Logger.get("SubServers").info("Loading SubServers.Sync v" + version.toString() + " Libraries (for Minecraft " + api.getGameVersion()[api.getGameVersion().length - 1] + ")");
        Try.all.run(() -> new CachedPlayer((Player) null)); // runs <clinit>

        File dir = new File(this.dir, "SubServers");
        dir.mkdir();

        ConfigUpdater.updateConfig(new File(dir, "sync.yml"));
        config = new YAMLConfig(new File(dir, "sync.yml"));

        SmartFallback.addInspector((player, info) -> {
            ServerData server = getData(info.getServerInfo());
            double confidence = 0;
            if (server != null) {
                if (!server.isHidden()) confidence++;
                if (!server.isRestricted()) confidence++;
                if (server.getSubData()[0] != null) confidence++;

                if (player != null) {
                    if (server.canAccess(player)) confidence++;
                }
            } if (server instanceof SubServerData) {
                if (!((SubServerData) server).isRunning()) return null;
            }
            return confidence;
        });

        subprotocol = SubProtocol.get();
        subprotocol.registerCipher("DHE", DHE.get(128));
        subprotocol.registerCipher("DHE-128", DHE.get(128));
        subprotocol.registerCipher("DHE-192", DHE.get(192));
        subprotocol.registerCipher("DHE-256", DHE.get(256));
    }

    /**
     * Load Hosts, Servers, SubServers, and SubData Direct
     */
    @Subscribe
    public void initialize(ProxyInitializeEvent e) {
        try {
            running = true;
            SmartFallback.dns_forward = config.get().getMap("Settings").getMap("Smart-Fallback", new ObjectMap<>()).getBoolean("DNS-Forward", false);

            resetDate = Calendar.getInstance().getTime().getTime();
            ConfigUpdater.updateConfig(new File(dir, "SubServers/sync.yml"));
            config.reload();

            synchronized (rPlayers) {
                for (Player local : proxy.getAllPlayers()) {
                    CachedPlayer player = new CachedPlayer(local);
                    rPlayerLinkP.put(player.getUniqueId(), player.getProxyName().toLowerCase());
                    rPlayers.put(player.getUniqueId(), player);
                    ServerInfo server = local.getCurrentServer().map(ServerConnection::getServerInfo).orElse(null);
                    if (servers.containsKey(server)) rPlayerLinkS.put(player.getUniqueId(), servers.get(server));
                }
            }

            subprotocol.unregisterCipher("AES");
            subprotocol.unregisterCipher("AES-128");
            subprotocol.unregisterCipher("AES-192");
            subprotocol.unregisterCipher("AES-256");
            subprotocol.unregisterCipher("RSA");

            api.name = config.get().getMap("Settings").getMap("SubData").getString("Name");

            if (config.get().getMap("Settings").getMap("SubData").getString("Password", "").length() > 0) {
                subprotocol.registerCipher("AES", new AES(128, config.get().getMap("Settings").getMap("SubData").getString("Password")));
                subprotocol.registerCipher("AES-128", new AES(128, config.get().getMap("Settings").getMap("SubData").getString("Password")));
                subprotocol.registerCipher("AES-192", new AES(192, config.get().getMap("Settings").getMap("SubData").getString("Password")));
                subprotocol.registerCipher("AES-256", new AES(256, config.get().getMap("Settings").getMap("SubData").getString("Password")));

                Logger.get("SubData").info("AES Encryption Available");
            }
            if (new File(dir, "SubServers/subdata.rsa.key").exists()) {
                try {
                    subprotocol.registerCipher("RSA", new RSA(new File(dir, "SubServers/subdata.rsa.key")));
                    Logger.get("SubData").info("RSA Encryption Available");
                } catch (Exception x) {
                    x.printStackTrace();
                }
            }

            reconnect = true;
            Logger.get("SubData").info("");
            Logger.get("SubData").info("Connecting to /" + config.get().getMap("Settings").getMap("SubData").getString("Address", "127.0.0.1:4391"));
            connect(Logger.get("SubData"), null);

            if (!posted) {
                posted = true;
                post();
            }
        } catch (IOException x) {
            x.printStackTrace();
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
                            SubDataClient open = subprotocol.open(InetAddress.getByName(config.get().getMap("Settings").getMap("SubData").getString("Address", "127.0.0.1:4391").split(":")[0]),
                                    Integer.parseInt(config.get().getMap("Settings").getMap("SubData").getString("Address", "127.0.0.1:4391").split(":")[1]));

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
        if (!config.get().getMap("Settings").getStringList("Disabled-Overrides", Collections.emptyList()).contains("/server"))
            proxy.getCommandManager().register("server", new SubCommand.BungeeServer(this));
        if (!config.get().getMap("Settings").getStringList("Disabled-Overrides", Collections.emptyList()).contains("/glist"))
            proxy.getCommandManager().register("glist", new SubCommand.BungeeList(this));

        proxy.getCommandManager().register("subservers", new SubCommand(this), "subserver", "sub");

        if (config.get().getMap("Settings").getMap("Smart-Fallback", new ObjectMap<>()).getBoolean("Enabled", true))
            proxy.getEventManager().register(this, new SmartFallback(config.get().getMap("Settings").getMap("Smart-Fallback", new ObjectMap<>())));

        Try.none.run(() -> metrics.make(this, 11953).addCustomChart(Util.reflect(Metrics.class.getDeclaredField("PLAYER_VERSIONS"), null)));
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
                            for (Player player : ExProxy.this.proxy.getAllPlayers()) {
                                if (!rPlayers.containsKey(player.getUniqueId())) { // Add players that don't exist
                                    CachedPlayer p = new CachedPlayer(player);
                                    rPlayerLinkP.put(player.getUniqueId(), p.getProxyName().toLowerCase());
                                    rPlayers.put(player.getUniqueId(), p);
                                    ServerInfo server = player.getCurrentServer().map(ServerConnection::getServerInfo).orElse(null);
                                    if (servers.containsKey(server)) rPlayerLinkS.put(player.getUniqueId(), servers.get(server));
                                    add.add(p);
                                }
                            }
                            ArrayList<CachedPlayer> remove = new ArrayList<CachedPlayer>();
                            for (Pair<String, UUID> player : proxy.getPlayers()) { // Remove players that shouldn't exist
                                if (!ExProxy.this.proxy.getPlayer(player.value()).isPresent()) {
                                    remove.add(rPlayers.get(player.value()));
                                    rPlayerLinkS.remove(player.value());
                                    rPlayerLinkP.remove(player.value());
                                    rPlayers.remove(player.value());
                                }
                            }
                            for (UUID player : Util.getBackwards(rPlayerLinkP, api.getName().toLowerCase())) { // Remove players that shouldn't exist (internally)
                                if (!ExProxy.this.proxy.getPlayer(player).isPresent()) {
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
     * Port-forward Listeners
     */
    @Subscribe
    public void registerListener(ListenerBoundEvent e) {
        if (UPnP.isUPnPAvailable()) {
            if (config.get().getMap("Settings").getMap("UPnP", new ObjectMap<String>()).getBoolean("Forward-Proxy", true)) {
                UPnP.openPortTCP(e.getAddress().getPort());
            }
        } else {
            out.warn("UPnP is currently unavailable. Ports may not be automatically forwarded on this device.");
        }
    }

    /**
     * Get the Proxy Instance
     *
     * @return Proxy Instance
     */
    @SuppressWarnings("deprecation")
    public static ProxyServer getInstance() {
        return SubAPI.getInstance().getInternals().proxy;
    }

    /**
     * Get Internal Server Data
     *
     * @param server ServerInfo
     * @return ServerData
     */
    public ServerData getData(ServerInfo server) {
        return (server == null)? null : servers.getOrDefault(server, null);
    }

    /**
     * Simulate Proxy Reload
     */
    @Subscribe
    public void reload(ProxyReloadEvent e) {
        shutdown(null);
        initialize(null);
    }

    /**
     * Un-port-forward Listeners
     */
    @Subscribe
    public void unregisterListener(ListenerCloseEvent e) {
        if (UPnP.isUPnPAvailable()) {
            if (UPnP.isMappedTCP(e.getAddress().getPort())) UPnP.closePortTCP(e.getAddress().getPort());
        }
    }

    /**
     * Reset all changes made by initialize()
     *
     * @see ExProxy#initialize(ProxyInitializeEvent)
     */
    @Subscribe
    public void shutdown(ProxyShutdownEvent e) {
        try {
            running = false;
            Logger.get("SubServers").info("Removing synced data");
            servers.clear();

            reconnect = false;
            ArrayList<SubDataClient> tmp = new ArrayList<SubDataClient>();
            tmp.addAll(subdata.values());
            for (SubDataClient client : tmp) if (client != null) {
                client.close();
                Try.all.run(client::waitFor);
            }
            subdata.clear();
            subdata.put(0, null);

            rPlayerLinkS.clear();
            rPlayerLinkP.clear();
            rPlayers.clear();
        } catch (Exception x) {
            x.printStackTrace();
        }
    }

    @Subscribe(order = PostOrder.LAST)
    public void ping(ProxyPingEvent e) {
        int offline = 0;
        RegisteredServer[] overrides;
        if ((overrides = SmartFallback.getForcedHosts(e.getConnection())) != null || (overrides = SmartFallback.getDNS(e.getConnection())) != null) {
            for (RegisteredServer server : overrides)
                if (server instanceof SubServerData && !((SubServerData) server).isRunning()) offline++;

            if (offline >= overrides.length) {
                e.setPing(new ServerPing(e.getPing().getVersion(), e.getPing().getPlayers().orElse(null), ChatColor.convertColor(api.getLang("SubServers", "Bungee.Ping.Offline")),
                        e.getPing().getFavicon().orElse(null), e.getPing().getModinfo().orElse(null)));
            }
        } else {
            for (String name : proxy.getConfiguration().getAttemptConnectionOrder()) {
                ServerData server = proxy.getServer(name).map(RegisteredServer::getServerInfo).map(this::getData).orElse(null);
                if (server instanceof SubServerData && !((SubServerData) server).isRunning()) offline++;
            }

            if (offline >= proxy.getConfiguration().getAttemptConnectionOrder().size()) {
                e.setPing(new ServerPing(e.getPing().getVersion(), e.getPing().getPlayers().orElse(null), ChatColor.convertColor(api.getLang("SubServers", "Bungee.Ping.Offline")),
                        e.getPing().getFavicon().orElse(null), e.getPing().getModinfo().orElse(null)));
            }
        }
    }

    @Subscribe(order = PostOrder.FIRST)
    public void login(LoginEvent e) {
        out.info("UUID of player " + e.getPlayer().getGameProfile().getName() + " is " + e.getPlayer().getUniqueId());
        if (rPlayers.containsKey(e.getPlayer().getUniqueId())) {
            Logger.get("SubServers").warning(e.getPlayer().getGameProfile().getName() + " connected, but already had a database entry");
            CachedPlayer player = rPlayers.get(e.getPlayer().getUniqueId());
            if (player.getProxyName() != null && player.getProxyName().equalsIgnoreCase(api.getName())) {
                proxy.getPlayer(player.getUniqueId()).ifPresent(p -> p.disconnect(Component.translatable("velocity.error.already-connected-proxy", NamedTextColor.RED)));
            } else {
                ((SubDataClient) api.getSubDataNetwork()[0]).sendPacket(new PacketDisconnectPlayer(new UUID[]{ player.getUniqueId() }, LegacyComponentSerializer.legacySection().serialize(Component.translatable("velocity.error.already-connected-proxy", NamedTextColor.RED))));
            }
        }
    }

    @Subscribe(order = PostOrder.LAST)
    public void validate(ServerPreConnectEvent e) {
        if (e.getPlayer().isActive()) {
            ServerData server = getData(e.getOriginalServer().getServerInfo());
            if (server == null || !server.canAccess(e.getPlayer())) {
                if (!e.getPlayer().getCurrentServer().isPresent() || fallback.containsKey(e.getPlayer().getUniqueId())) {
                    if (!fallback.containsKey(e.getPlayer().getUniqueId()) || fallback.get(e.getPlayer().getUniqueId()).names.contains(e.getOriginalServer().getServerInfo().getName())) {
                        Component text = ChatColor.convertColor(api.getLang("SubServers", "Bungee.Restricted"));
                        KickedFromServerEvent kick = new KickedFromServerEvent(e.getPlayer(), e.getOriginalServer(), text, true, KickedFromServerEvent.DisconnectPlayer.create(text));
                        fallback(kick);
                        if (e.getPlayer().getCurrentServer().isPresent()) e.setResult(ServerPreConnectEvent.ServerResult.denied());
                        if (kick.getResult() instanceof KickedFromServerEvent.DisconnectPlayer) e.getPlayer().disconnect(text);
                        else if (kick.getResult() instanceof KickedFromServerEvent.RedirectPlayer) e.getPlayer().createConnectionRequest(((KickedFromServerEvent.RedirectPlayer) kick.getResult()).getServer()).fireAndForget();
                    }
                } else {
                    e.getPlayer().sendMessage(ChatColor.convertColor(api.getLang("SubServers", "Bungee.Restricted")));
                    e.setResult(ServerPreConnectEvent.ServerResult.denied());
                }
            } else if (e.getPlayer().getCurrentServer().isPresent() && !fallback.containsKey(e.getPlayer().getUniqueId()) && server instanceof SubServerData && !((SubServerData) server).isRunning()) {
                e.getPlayer().sendMessage(ChatColor.convertColor(api.getLang("SubServers", "Bungee.Server.Offline")));
                e.setResult(ServerPreConnectEvent.ServerResult.denied());
            }

            if (fallback.containsKey(e.getPlayer().getUniqueId())) {
                FallbackState state = fallback.get(e.getPlayer().getUniqueId());
                if (state.names.contains(e.getOriginalServer().getServerInfo().getName())) {
                    state.remove(e.getOriginalServer().getServerInfo().getName());
                } else if (e.getPlayer().getCurrentServer().isPresent()) {
                    fallback.remove(e.getPlayer().getUniqueId());
                }
            }
        } else {
            e.setResult(ServerPreConnectEvent.ServerResult.denied());
        }
    }

    @SuppressWarnings("UnstableApiUsage")
    @Subscribe(order = PostOrder.LAST)
    public void connected(ServerPostConnectEvent e) {
        ServerData server = getData(e.getPlayer().getCurrentServer().get().getServerInfo());
        if (server != null) {
            if (e.getPlayer().isActive()) {
                synchronized (rPlayers) {
                    ObjectMap<String> raw = CachedPlayer.translate(e.getPlayer());
                    raw.set("server", server.getName());
                    CachedPlayer player = new CachedPlayer(raw);
                    rPlayerLinkP.put(player.getUniqueId(), player.getProxyName().toLowerCase());
                    rPlayers.put(player.getUniqueId(), player);
                    rPlayerLinkS.put(player.getUniqueId(), server);
                    if (api.getSubDataNetwork()[0] != null) {
                        ((SubDataClient) api.getSubDataNetwork()[0]).sendPacket(new PacketExSyncPlayer(true, player));
                    }
                }
            }


            if (fallback.containsKey(e.getPlayer().getUniqueId())) {
                fallback.get(e.getPlayer().getUniqueId()).done(() -> {
                    if (fallback.containsKey(e.getPlayer().getUniqueId()) && e.getPlayer().getCurrentServer().get().getServerInfo().getName().equals(server.get().getName())) {
                        fallback.remove(e.getPlayer().getUniqueId());
                    }
                }, proxy.getConfiguration().getConnectTimeout() + 500);
            }
        }
    }

    @Subscribe(order = PostOrder.LAST)
    public void fallback(KickedFromServerEvent e) {
        if (e.getPlayer().isActive() && config.get().getMap("Settings").getMap("Smart-Fallback", new ObjectMap<>()).getBoolean("Fallback", true)) {
            FallbackState state;
            boolean init = !fallback.containsKey(e.getPlayer().getUniqueId());
            if (init) {
                Map<String, RegisteredServer> map = SmartFallback.getFallbackServers(e.getPlayer());
                map.remove(e.getServer().getServerInfo().getName());
                state = new FallbackState(e.getPlayer().getUniqueId(), map, e.getServerKickReason().orElse(Component.text("")));
                if (e.getPlayer().getCurrentServer().isPresent())
                    state.remove(e.getPlayer().getCurrentServer().get().getServerInfo().getName());
            } else {
                state = fallback.get(e.getPlayer().getUniqueId());
                LinkedList<RegisteredServer> tmp = new LinkedList<>(state.servers);
                for (RegisteredServer server : tmp) if (server.getServerInfo().equals(e.getServer().getServerInfo()))
                    state.remove(server);
            }

            RegisteredServer rs = e.getServer();
            ServerData server = getData(rs.getServerInfo());
            e.getPlayer().sendMessage(ChatColor.convertColor(api.getLang("SubServers", "Bungee.Feature.Smart-Fallback")
                    .replace("$str$", (server != null)? server.getDisplayName() : rs.getServerInfo().getName()))
                    .replaceText(TextReplacementConfig.builder().match("\\$msg\\$").replacement(e.getServerKickReason().orElse(Component.text(""))).build())
            );

            if (state.servers.isEmpty()) {
                if (e.getPlayer().getCurrentServer().isPresent()) {
                    fallback.remove(e.getPlayer().getUniqueId());
                    rs = e.getPlayer().getCurrentServer().get().getServer();
                    server = getData(rs.getServerInfo());
                    e.setResult(KickedFromServerEvent.Notify.create(ChatColor.convertColor(api.getLang("SubServers", "Bungee.Feature.Smart-Fallback.Result").replace("$str$", (server != null)? server.getDisplayName() : rs.getServerInfo().getName()))));
                } else {
                    e.setResult(KickedFromServerEvent.DisconnectPlayer.create(state.reason));
                }
            } else {
                if (init) fallback.put(e.getPlayer().getUniqueId(), state);
                rs = state.servers.getFirst();
                server = getData(rs.getServerInfo());

                e.setResult(KickedFromServerEvent.RedirectPlayer.create(rs, ChatColor.convertColor(api.getLang("SubServers", "Bungee.Feature.Smart-Fallback.Result").replace("$str$",  (server != null)? server.getDisplayName() : rs.getServerInfo().getName()))));
            }
        }
    }

    @Subscribe(order = PostOrder.LAST)
    public void disconnected(DisconnectEvent e) {
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

    @Subscribe(order = PostOrder.FIRST)
    public void add(SubAddServerEvent e) {
        api.getServer(e.getServer(), server -> {
            if (server != null) {
                ServerData data;
                if (server instanceof net.ME1312.SubServers.Client.Common.Network.API.SubServer) {
                    data = new SubServerData(server.getSignature(), server.getName(), server.getDisplayName(), server.getAddress(),
                            getSubDataAsMap(server), server.getMotd(), server.isHidden(), server.isRestricted(), server.getWhitelist(), ((net.ME1312.SubServers.Client.Common.Network.API.SubServer) server).isRunning());
                    Logger.get("SubServers").info("Added SubServer: " + e.getServer());
                } else {
                    data = new ServerData(server.getSignature(), server.getName(), server.getDisplayName(), server.getAddress(),
                            getSubDataAsMap(server), server.getMotd(), server.isHidden(), server.isRestricted(), server.getWhitelist());
                    Logger.get("SubServers").info("Added Server: " + e.getServer());
                }
                ServerData old = servers.put(data.get(), data);
                if (old != null && proxy.getServer(data.getName()).isPresent())
                    proxy.unregisterServer(old.get());
                proxy.registerServer(data.get());

            } else System.out.println("PacketDownloadServerInfo(" + e.getServer() + ") returned with an invalid response");
        });
    }

    public Boolean merge(net.ME1312.SubServers.Client.Common.Network.API.Server server) {
        ServerData current = proxy.getServer(server.getName()).map(RegisteredServer::getServerInfo).map(this::getData).orElse(null);
        if (server instanceof net.ME1312.SubServers.Client.Common.Network.API.SubServer || !(current instanceof SubServerData)) {
            if (current == null || !current.getSignature().equals(server.getSignature())) {
                ServerData data;
                if (server instanceof net.ME1312.SubServers.Client.Common.Network.API.SubServer) {
                    data = new SubServerData(server.getSignature(), server.getName(), server.getDisplayName(), server.getAddress(),
                            getSubDataAsMap(server), server.getMotd(), server.isHidden(), server.isRestricted(), server.getWhitelist(), ((net.ME1312.SubServers.Client.Common.Network.API.SubServer) server).isRunning());
                } else {
                    data = new ServerData(server.getSignature(), server.getName(), server.getDisplayName(), server.getAddress(),
                            getSubDataAsMap(server), server.getMotd(), server.isHidden(), server.isRestricted(), server.getWhitelist());
                }
                ServerData old = servers.put(data.get(), data);
                if (old != null && proxy.getServer(data.getName()).isPresent())
                    proxy.unregisterServer(old.get());
                proxy.registerServer(data.get());

                Logger.get("SubServers").info("Added "+((server instanceof net.ME1312.SubServers.Client.Common.Network.API.SubServer)?"Sub":"")+"Server: " + server.getName());
                return true;
            } else {
                if (server instanceof net.ME1312.SubServers.Client.Common.Network.API.SubServer) {
                    if (((net.ME1312.SubServers.Client.Common.Network.API.SubServer) server).isRunning() != ((SubServerData) current).isRunning())
                        ((SubServerData) current).setRunning(((net.ME1312.SubServers.Client.Common.Network.API.SubServer) server).isRunning());
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

    @Subscribe(order = PostOrder.FIRST)
    public void start(SubStartEvent e) {
        ServerData server = proxy.getServer(e.getServer()).map(RegisteredServer::getServerInfo).map(this::getData).orElse(null);
        if (server instanceof SubServerData) ((SubServerData) server).setRunning(true);
    }

    @Subscribe(order = PostOrder.FIRST)
    public void stop(SubStoppedEvent e) {
        ServerData server = proxy.getServer(e.getServer()).map(RegisteredServer::getServerInfo).map(this::getData).orElse(null);
        if (server instanceof SubServerData) ((SubServerData) server).setRunning(false);
    }

    @Subscribe(order = PostOrder.FIRST)
    public void remove(SubRemoveServerEvent e) {
        ServerData server = proxy.getServer(e.getServer()).map(RegisteredServer::getServerInfo).map(this::getData).orElse(null);
        if (server != null) {
            servers.remove(server.get());
            proxy.unregisterServer(server.get());
            Logger.get("SubServers").info("Removed Server: " + e.getServer());
        }
    }
}

package net.ME1312.SubServers.Sync;

import net.ME1312.Galaxi.Library.Callback.Callback;
import net.ME1312.Galaxi.Library.Container.NamedContainer;
import net.ME1312.Galaxi.Library.UniversalFile;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Client.DataClient;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.SubData.Client.DataProtocol;
import net.ME1312.SubServers.Bungee.BungeeAPI;
import net.ME1312.SubServers.Sync.Network.API.*;
import net.ME1312.SubServers.Sync.Network.Packet.*;
import net.ME1312.SubData.Client.SubDataClient;
import net.md_5.bungee.protocol.ProtocolConstants;

import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.util.*;

import static net.ME1312.SubServers.Sync.Network.API.SimplifiedData.*;

/**
 * SubAPI Class
 */
public final class SubAPI implements BungeeAPI {
    LinkedList<Runnable> enableListeners = new LinkedList<Runnable>();
    LinkedList<Runnable> disableListeners = new LinkedList<Runnable>();
    private final ExProxy plugin;
    private static SubAPI api;
    String name;

    SubAPI(ExProxy plugin) {
        this.plugin = plugin;
        GAME_VERSION = getGameVersion();
        api = this;
    }

    /**
     * Gets the SubAPI Methods
     *
     * @return SubAPI
     */
    public static SubAPI getInstance() {
        return api;
    }

    /**
     * Gets the SubServers Internals
     *
     * @deprecated Use SubAPI Methods when available
     * @return SubPlugin Internals
     */
    @Deprecated
    public ExProxy getInternals() {
        return plugin;
    }

    /**
     * Adds a SubAPI Listener
     *
     * @param enable An Event that will be called when SubAPI is ready
     * @param disable An Event that will be called before SubAPI is disabled (your plugin should reset it's values in case this is a hard-reset instead of a shutdown)
     */
    public void addListener(Runnable enable, Runnable disable) {
        if (enable != null) enableListeners.add(enable);
        if (disable != null) disableListeners.add(disable);
    }

    /**
     * Get the Proxy Name
     *
     * @return Proxy Name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the Hosts
     *
     * @param callback Host Map
     */
    public void getHosts(Callback<Map<String, Host>> callback) {
        requestHosts(null, callback);
    }

    /**
     * Gets a Host
     *
     * @param name Host name
     * @param callback a Host
     */
    public void getHost(String name, Callback<Host> callback) {
        requestHost(null, name, callback);
    }

    /**
     * Gets the Server Groups (Group names are case sensitive here)
     *
     * @param callback Group Map
     */
    public void getGroups(Callback<Map<String, List<Server>>> callback) {
        requestGroups(null, callback);
    }

    /**
     * Gets the Server Groups (Group names are all lowercase here)
     *
     * @param callback Group Map
     */
    public void getLowercaseGroups(Callback<Map<String, List<Server>>> callback) {
        requestLowercaseGroups(null, callback);
    }

    /**
     * Gets a Server Group (Group names are case insensitive here)
     *
     * @param name Group name
     * @param callback a Server Group
     */
    public void getGroup(String name, Callback<NamedContainer<String, List<Server>>> callback) {
        requestGroup(null, name, callback);
    }

    /**
     * Gets the Servers (including SubServers)
     *
     * @param callback Server Map
     */
    public void getServers(Callback<Map<String, Server>> callback) {
        requestServers(null, callback);
    }

    /**
     * Gets a Server
     *
     * @param name Server name
     * @param callback a Server
     */
    public void getServer(String name, Callback<Server> callback) {
        requestServer(null, name, callback);
    }

    /**
     * Adds a Server to the Network
     *
     * @param name Name of the Server
     * @param ip IP of the Server
     * @param port Port of the Server
     * @param motd MOTD of the Server
     * @param hidden if the server should be hidden from players
     * @param restricted Players will need a permission to join if true
     * @param response Response Code
     */
    public void addServer(String name, InetAddress ip, int port, String motd, boolean hidden, boolean restricted, Callback<Integer> response) {
        addServer(null, name, ip, port, motd, hidden, restricted, response);
    }

    /**
     * Adds a Server to the Network
     *
     * @param player Player who added
     * @param name Name of the Server
     * @param ip IP of the Server
     * @param port Port of the Server
     * @param motd MOTD of the Server
     * @param hidden If the server should be hidden from players
     * @param restricted Players will need a permission to join if true
     * @param response Response Code
     */
    public void addServer(UUID player, String name, InetAddress ip, int port, String motd, boolean hidden, boolean restricted, Callback<Integer> response) {
        if (Util.isNull(response)) throw new NullPointerException();
        StackTraceElement[] origin = new Exception().getStackTrace();
        ((SubDataClient) SubAPI.getInstance().getSubDataNetwork()[0]).sendPacket(new PacketAddServer(player, name, ip, port, motd, hidden, restricted, data -> {
            try {
                response.run(data.getInt(0x0001));
            } catch (Throwable e) {
                Throwable ew = new InvocationTargetException(e);
                ew.setStackTrace(origin);
                ew.printStackTrace();
            }
        }));
    }

    /**
     * Adds a Server to the Network
     *
     * @param name Name of the Server
     * @param ip IP of the Server
     * @param port Port of the Server
     * @param motd MOTD of the Server
     * @param hidden if the server should be hidden from players
     * @param restricted Players will need a permission to join if true
     */
    public void addServer(String name, InetAddress ip, int port, String motd, boolean hidden, boolean restricted) {
        addServer(null, name, ip, port, motd, hidden, restricted);
    }

    /**
     * Adds a Server to the Network
     *
     * @param player Player who added
     * @param name Name of the Server
     * @param ip IP of the Server
     * @param port Port of the Server
     * @param motd MOTD of the Server
     * @param hidden If the server should be hidden from players
     * @param restricted Players will need a permission to join if true
     */
    public void addServer(UUID player, String name, InetAddress ip, int port, String motd, boolean hidden, boolean restricted) {
        addServer(player, name, ip, port, motd, hidden, restricted, i -> {});
    }

    /**
     * Remove a Server from the Network
     *
     * @param name Name of the Server
     * @param response Response Code
     */
    public void removeServer(String name, Callback<Integer> response) {
        removeServer(null, name, response);
    }

    /**
     * Remove a Server from the Network
     *
     * @param player Player Removing
     * @param name Name of the Server
     * @param response Response Code
     */
    public void removeServer(UUID player, String name, Callback<Integer> response) {
        if (Util.isNull(name)) throw new NullPointerException();
        removeServer(player, name, false, response);
    }

    /**
     * Remove a Server from the Network
     *
     * @param name Name of the Server
     */
    public void removeServer(String name) {
        removeServer(null, name);
    }

    /**
     * Remove a Server from the Network
     *
     * @param player Player Removing
     * @param name Name of the Server
     */
    public void removeServer(UUID player, String name) {
        if (Util.isNull(name)) throw new NullPointerException();
        removeServer(player, name, i -> {});
    }

    /**
     * Force Remove a Server from the Network
     *
     * @param name Name of the Server
     * @param response Response Code
     */
    public void forceRemoveServer(String name, Callback<Integer> response) {
        forceRemoveServer(null, name, response);
    }

    /**
     * Force Remove a Server from the Network
     *
     * @param player Player Removing
     * @param name Name of the Server
     * @param response Response Code
     */
    public void forceRemoveServer(UUID player, String name, Callback<Integer> response) {
        if (Util.isNull(name)) throw new NullPointerException();
        removeServer(player, name, true, response);
    }

    /**
     * Force Remove a Server from the Network
     *
     * @param name Name of the Server
     */
    public void forceRemoveServer(String name) {
        forceRemoveServer(null, name);
    }

    /**
     * Force Remove a Server from the Network
     *
     * @param player Player Removing
     * @param name Name of the Server
     */
    public void forceRemoveServer(UUID player, String name) {
        if (Util.isNull(name)) throw new NullPointerException();
        forceRemoveServer(player, name, i -> {});
    }

    private void removeServer(UUID player, String name, boolean force, Callback<Integer> response) {
        if (Util.isNull(response)) throw new NullPointerException();
        StackTraceElement[] origin = new Exception().getStackTrace();
        ((SubDataClient) SubAPI.getInstance().getSubDataNetwork()[0]).sendPacket(new PacketRemoveServer(player, name, force, data -> {
            try {
                response.run(data.getInt(0x0001));
            } catch (Throwable e) {
                Throwable ew = new InvocationTargetException(e);
                ew.setStackTrace(origin);
                ew.printStackTrace();
            }
        }));
    }

    /**
     * Gets the SubServers
     *
     * @param callback SubServer Map
     */
    public void getSubServers(Callback<Map<String, SubServer>> callback) {
        requestSubServers(null, callback);
    }

    /**
     * Gets a SubServer
     *
     * @param name SubServer name
     * @param callback a SubServer
     */
    public void getSubServer(String name, Callback<SubServer> callback) {
        requestSubServer(null, name, callback);
    }

    /**
     * Gets the known Proxies
     *
     * @param callback Proxy Map
     */
    public void getProxies(Callback<Map<String, Proxy>> callback) {
        requestProxies(null, callback);
    }

    /**
     * Gets a Proxy
     *
     * @param name Proxy name
     * @param callback a Proxy
     */
    public void getProxy(String name, Callback<Proxy> callback) {
        requestProxy(null, name, callback);
    }

    /**
     * Get the Master Proxy Container
     *
     * @param callback Master Proxy
     */
    public void getMasterProxy(Callback<Proxy> callback) {
        requestMasterProxy(null, callback);
    }

    /**
     * Gets players on this network across all known proxies
     *
     * @param callback Remote Player Collection
     */
    public void getGlobalPlayers(Callback<Map<UUID, RemotePlayer>> callback) {
        requestGlobalPlayers(null, callback);
    }

    /**
     * Gets a player on this network by searching across all known proxies
     *
     * @param name Player name
     * @param callback Remote Player
     */
    public void getGlobalPlayer(String name, Callback<RemotePlayer> callback) {
        requestGlobalPlayer(null, name, callback);
    }

    /**
     * Gets a player on this network by searching across all known proxies
     *
     * @param id Player UUID
     * @param callback Remote Player
     */
    public void getGlobalPlayer(UUID id, Callback<RemotePlayer> callback) {
        requestGlobalPlayer(null, id, callback);
    }

    /**
     * Gets players on this network across all known proxies (Cached)
     *
     * @return Remote Player Collection
     */
    public Map<UUID, RemotePlayer> getGlobalPlayers() {
        return new HashMap<UUID, RemotePlayer>(plugin.rPlayers);
    }

    /**
     * Gets a player on this network by searching across all known proxies (Cached)
     *
     * @param name Player name
     * @return Remote Player
     */
    public RemotePlayer getGlobalPlayer(String name) {
        if (Util.isNull(name)) throw new NullPointerException();
        for (RemotePlayer player : getGlobalPlayers().values()) {
            if (player.getName().equalsIgnoreCase(name)) return player;
        }
        return null;
    }

    /**
     * Gets a player on this network by searching across all known proxies (Cached)
     *
     * @param id Player UUID
     * @return Remote Player
     */
    public RemotePlayer getGlobalPlayer(UUID id) {
        if (Util.isNull(id)) throw new NullPointerException();
        return getGlobalPlayers().getOrDefault(id, null);
    }

    /**
     * Gets the SubData Network Connections
     *
     * @return SubData Network Connections
     */
    public DataClient[] getSubDataNetwork() {
        LinkedList<Integer> keys = new LinkedList<Integer>(plugin.subdata.keySet());
        LinkedList<SubDataClient> channels = new LinkedList<SubDataClient>();
        Collections.sort(keys);
        for (Integer channel : keys) channels.add(plugin.subdata.get(channel));
        return channels.toArray(new DataClient[0]);
    }

    /**
     * Gets the SubData Network Protocol
     *
     * @return SubData Network Protocol
     */
    public DataProtocol getSubDataProtocol() {
        return plugin.subprotocol;
    }

    /**
     * Gets the current SubServers Lang Channels
     *
     * @return SubServers Lang Channel list
     */
    public Collection<String> getLangChannels() {
        return plugin.lang.get().keySet();
    }

    /**
     * Gets values from the SubServers Lang
     *
     * @param channel Lang Channel
     * @return Lang Value
     */
    public Map<String, String> getLang(String channel) {
        if (Util.isNull(channel)) throw new NullPointerException();
        return new LinkedHashMap<>(plugin.lang.get().get(channel.toLowerCase()));
    }

    /**
     * Gets the Runtime Directory
     *
     * @return Directory
     */
    public UniversalFile getRuntimeDirectory() {
        return plugin.dir;
    }

    /**
     * Gets the SubServers Version
     *
     * @return SubServers Version
     */
    public Version getWrapperVersion() {
        return plugin.version;
    }

    /**
     * Gets the SubServers Build Signature
     *
     * @return SubServers Build Signature (or null if unsigned)
     */
    public Version getWrapperBuild() {
        return (ExProxy.class.getPackage().getSpecificationTitle() != null)?new Version(ExProxy.class.getPackage().getSpecificationTitle()):null;
    }

    /**
     * Gets the BungeeCord Version
     *
     * @return BungeeCord Version
     */
    public Version getProxyVersion() {
        return new Version(plugin.getVersion());
    }

    /**
     * Get an array of compatible Minecraft Versions
     *
     * @return Minecraft Versions
     */
    public Version[] getGameVersion() {
        if (GAME_VERSION == null) {
            if (System.getProperty("subservers.minecraft.version", "").length() > 0) {
                return new Version[]{new Version(System.getProperty("subservers.minecraft.version"))};
            } else if (Util.getDespiteException(() -> ProtocolConstants.SUPPORTED_VERSIONS != null, false)) {
                List<Version> versions = new LinkedList<Version>();
                for (String version : ProtocolConstants.SUPPORTED_VERSIONS) versions.add(new Version(version));
                Collections.sort(versions);
                return versions.toArray(new Version[versions.size()]);
            } else if (Util.getDespiteException(() -> plugin.getGameVersion() != null, false)) {
                String raw = plugin.getGameVersion();
                if (raw.contains("-") || raw.contains(",")) {
                    List<Version> versions = new LinkedList<Version>();
                    for (String version : raw.split("(?:\\s*-|,)\\s*")) versions.add(new Version(version));
                    Collections.sort(versions);
                    return versions.toArray(new Version[versions.size()]);
                } else {
                    return new Version[]{new Version(plugin.getGameVersion())};
                }
            } else {
                plugin.getLogger().warning("Could not determine compatible Minecraft version(s); Now using 1.x.x as a placeholder.");
                plugin.getLogger().warning("Use this launch argument to specify a compatible Minecraft version: -Dsubservers.minecraft.version=1.x.x");
                return new Version[]{new Version("1.x.x")};
            }
        } else return GAME_VERSION;
    }
    private final Version[] GAME_VERSION;
}

package net.ME1312.SubServers.Client.Common;

import net.ME1312.Galaxi.Library.Container.Pair;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Client.DataClient;
import net.ME1312.SubData.Client.DataProtocol;
import net.ME1312.SubData.Client.SubDataClient;
import net.ME1312.SubServers.Client.Common.Network.API.*;
import net.ME1312.SubServers.Client.Common.Network.Packet.PacketAddServer;
import net.ME1312.SubServers.Client.Common.Network.Packet.PacketRemoveServer;

import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

import static net.ME1312.SubServers.Client.Common.Network.API.SimplifiedData.*;

public abstract class ClientAPI {
    private static ClientAPI api;

    protected ClientAPI() {
        api = this;
    }

    /**
     * Gets the SubAPI Client Common Methods
     *
     * @return SubAPI Client Common
     */
    public static ClientAPI getInstance() {
        return api;
    }

    /**
     * Get the Server Name
     *
     * @return Server Name
     */
    public abstract String getName();

    /**
     * Gets the Hosts
     *
     * @param callback Host Map
     */
    public void getHosts(Consumer<Map<String, Host>> callback) {
        requestHosts(null, callback);
    }

    /**
     * Gets a Host
     *
     * @param name Host name
     * @param callback a Host
     */
    public void getHost(String name, Consumer<Host> callback) {
        requestHost(null, name, callback);
    }

    /**
     * Gets the Server Groups (Group names are case sensitive here)
     *
     * @param callback Group Map
     */
    public void getGroups(Consumer<Map<String, List<Server>>> callback) {
        requestGroups(null, callback);
    }

    /**
     * Gets the Server Groups (Group names are all lowercase here)
     *
     * @param callback Group Map
     */
    public void getLowercaseGroups(Consumer<Map<String, List<Server>>> callback) {
        requestLowercaseGroups(null, callback);
    }

    /**
     * Gets a Server Group (Group names are case insensitive here)
     *
     * @param name Group name
     * @param callback a Server Group
     */
    public void getGroup(String name, Consumer<Pair<String, List<Server>>> callback) {
        requestGroup(null, name, callback);
    }

    /**
     * Gets the Servers (including SubServers)
     *
     * @param callback Server Map
     */
    public void getServers(Consumer<Map<String, Server>> callback) {
        requestServers(null, callback);
    }

    /**
     * Gets a Server
     *
     * @param name Server name
     * @param callback a Server
     */
    public void getServer(String name, Consumer<Server> callback) {
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
    public void addServer(String name, InetAddress ip, int port, String motd, boolean hidden, boolean restricted, IntConsumer response) {
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
    public void addServer(UUID player, String name, InetAddress ip, int port, String motd, boolean hidden, boolean restricted, IntConsumer response) {
        Util.nullpo(response);
        StackTraceElement[] origin = new Exception().getStackTrace();
        ((SubDataClient) getSubDataNetwork()[0]).sendPacket(new PacketAddServer(player, name, ip, port, motd, hidden, restricted, data -> {
            try {
                response.accept(data.getInt(0x0001));
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
    public void removeServer(String name, IntConsumer response) {
        removeServer(null, name, response);
    }

    /**
     * Remove a Server from the Network
     *
     * @param player Player Removing
     * @param name Name of the Server
     * @param response Response Code
     */
    public void removeServer(UUID player, String name, IntConsumer response) {
        Util.nullpo(name);
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
        Util.nullpo(name);
        removeServer(player, name, i -> {});
    }

    /**
     * Force Remove a Server from the Network
     *
     * @param name Name of the Server
     * @param response Response Code
     */
    public void forceRemoveServer(String name, IntConsumer response) {
        forceRemoveServer(null, name, response);
    }

    /**
     * Force Remove a Server from the Network
     *
     * @param player Player Removing
     * @param name Name of the Server
     * @param response Response Code
     */
    public void forceRemoveServer(UUID player, String name, IntConsumer response) {
        Util.nullpo(name);
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
        Util.nullpo(name);
        forceRemoveServer(player, name, i -> {});
    }

    private void removeServer(UUID player, String name, boolean force, IntConsumer response) {
        Util.nullpo(response);
        StackTraceElement[] origin = new Exception().getStackTrace();
        ((SubDataClient) getSubDataNetwork()[0]).sendPacket(new PacketRemoveServer(player, name, force, data -> {
            try {
                response.accept(data.getInt(0x0001));
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
    public void getSubServers(Consumer<Map<String, SubServer>> callback) {
        requestSubServers(null, callback);
    }

    /**
     * Gets a SubServer
     *
     * @param name SubServer name
     * @param callback a SubServer
     */
    public void getSubServer(String name, Consumer<SubServer> callback) {
        requestSubServer(null, name, callback);
    }

    /**
     * Gets the known Proxies
     *
     * @param callback Proxy Map
     */
    public void getProxies(Consumer<Map<String, Proxy>> callback) {
        requestProxies(null, callback);
    }

    /**
     * Gets a Proxy
     *
     * @param name Proxy name
     * @param callback a Proxy
     */
    public void getProxy(String name, Consumer<Proxy> callback) {
        requestProxy(null, name, callback);
    }

    /**
     * Get the Master Proxy Value
     *
     * @param callback Master Proxy
     */
    public void getMasterProxy(Consumer<Proxy> callback) {
        requestMasterProxy(null, callback);
    }

    /**
     * Gets players on this network across all known proxies
     *
     * @param callback Remote Player Collection
     */
    public void getRemotePlayers(Consumer<Map<UUID, RemotePlayer>> callback) {
        requestRemotePlayers(null, callback);
    }

    /**
     * Gets a player on this network by searching across all known proxies
     *
     * @param name Player name
     * @param callback Remote Player
     */
    public void getRemotePlayer(String name, Consumer<RemotePlayer> callback) {
        requestRemotePlayer(null, name, callback);
    }

    /**
     * Gets a player on this network by searching across all known proxies
     *
     * @param id Player UUID
     * @param callback Remote Player
     */
    public void getRemotePlayer(UUID id, Consumer<RemotePlayer> callback) {
        requestRemotePlayer(null, id, callback);
    }

    /**
     * Gets the SubData Network Connections
     *
     * @return SubData Network Connections
     */
    public abstract DataClient[] getSubDataNetwork();

    /**
     * Gets the SubData Network Protocol
     *
     * @return SubData Network Protocol
     */
    public abstract DataProtocol getSubDataProtocol();

    /**
     * Gets the current SubServers Lang Channels
     *
     * @return SubServers Lang Channel list
     */
    public abstract Collection<String> getLangChannels();

    /**
     * Gets values from the SubServers Lang
     *
     * @param channel Lang Channel
     * @return Lang Value
     */
    public abstract Map<String, String> getLang(String channel);

    /**
     * Gets a value from the SubServers Lang
     *
     * @param channel Lang Channel
     * @param key Key
     * @return Lang Values
     */
    public String getLang(String channel, String key) {
        Util.nullpo(channel, key);
        return getLang(channel).get(key);
    }
}

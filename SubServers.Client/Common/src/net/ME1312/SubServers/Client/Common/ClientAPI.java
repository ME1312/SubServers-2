package net.ME1312.SubServers.Client.Common;

import net.ME1312.Galaxi.Library.Callback.Callback;
import net.ME1312.Galaxi.Library.Container.NamedContainer;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Client.DataClient;
import net.ME1312.SubData.Client.DataProtocol;
import net.ME1312.SubData.Client.SubDataClient;
import net.ME1312.SubServers.Client.Common.Network.API.*;
import net.ME1312.SubServers.Client.Common.Network.Packet.*;

import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.util.*;

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
        ((SubDataClient) getSubDataNetwork()[0]).sendPacket(new PacketAddServer(player, name, ip, port, motd, hidden, restricted, data -> {
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
        ((SubDataClient) getSubDataNetwork()[0]).sendPacket(new PacketRemoveServer(player, name, force, data -> {
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
        if (Util.isNull(channel, key)) throw new NullPointerException();
        return getLang(channel).get(key);
    }
}

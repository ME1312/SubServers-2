package net.ME1312.SubServers.Host;

import net.ME1312.Galaxi.Library.Callback.Callback;
import net.ME1312.Galaxi.Library.NamedContainer;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.Galaxi.Plugin.PluginInfo;
import net.ME1312.SubData.Client.DataClient;
import net.ME1312.SubData.Client.SubDataClient;
import net.ME1312.SubServers.Host.Network.API.Host;
import net.ME1312.SubServers.Host.Network.API.Proxy;
import net.ME1312.SubServers.Host.Network.API.Server;
import net.ME1312.SubServers.Host.Network.API.SubServer;
import net.ME1312.SubServers.Host.Network.Packet.*;

import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.util.*;

/**
 * SubAPI Class
 */
public final class SubAPI {
    private final ExHost host;
    private static SubAPI api;
    String name;

    protected SubAPI(ExHost host) {
        this.host = host;
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
    public ExHost getInternals() {
        return host;
    }

    /**
     * Get the Server Name
     *
     * @return Server Name
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
        if (Util.isNull(callback)) throw new NullPointerException();
        StackTraceElement[] origin = new Exception().getStackTrace();
        ((SubDataClient) host.api.getSubDataNetwork()[0]).sendPacket(new PacketDownloadHostInfo(null, data -> {
            TreeMap<String, Host> hosts = new TreeMap<String, Host>();
            for (String host : data.getKeys()) {
                hosts.put(host.toLowerCase(), new Host(data.getMap(host)));
            }

            try {
                callback.run(hosts);
            } catch (Throwable e) {
                Throwable ew = new InvocationTargetException(e);
                ew.setStackTrace(origin);
                ew.printStackTrace();
            }
        }));
    }

    /**
     * Gets a Host
     *
     * @param name Host name
     * @param callback a Host
     */
    public void getHost(String name, Callback<Host> callback) {
        if (Util.isNull(name, callback)) throw new NullPointerException();
        StackTraceElement[] origin = new Exception().getStackTrace();
        ((SubDataClient) host.api.getSubDataNetwork()[0]).sendPacket(new PacketDownloadHostInfo(name, data -> {
            Host host = null;
            if (data.getKeys().size() > 0) {
                host = new Host(data.getMap(new LinkedList<String>(data.getKeys()).getFirst()));
            }

            try {
                callback.run(host);
            } catch (Throwable e) {
                Throwable ew = new InvocationTargetException(e);
                ew.setStackTrace(origin);
                ew.printStackTrace();
            }
        }));
    }

    /**
     * Gets the Server Groups (Group names are case sensitive here)
     *
     * @param callback Group Map
     */
    public void getGroups(Callback<Map<String, List<Server>>> callback) {
        if (Util.isNull(callback)) throw new NullPointerException();
        StackTraceElement[] origin = new Exception().getStackTrace();
        ((SubDataClient) host.api.getSubDataNetwork()[0]).sendPacket(new PacketDownloadGroupInfo(null, data -> {
            TreeMap<String, List<Server>> groups = new TreeMap<String, List<Server>>();
            for (String group : data.getKeys()) {
                ArrayList<Server> servers = new ArrayList<Server>();
                for (String server : data.getMap(group).getKeys()) {
                    if (data.getMap(group).getMap(server).getRawString("type", "Server").equals("SubServer")) {
                        servers.add(new SubServer(data.getMap(group).getMap(server)));
                    } else {
                        servers.add(new Server(data.getMap(group).getMap(server)));
                    }
                }
                if (servers.size() > 0) groups.put(group, servers);
            }

            try {
                callback.run(groups);
            } catch (Throwable e) {
                Throwable ew = new InvocationTargetException(e);
                ew.setStackTrace(origin);
                ew.printStackTrace();
            }
        }));
    }

    /**
     * Gets the Server Groups (Group names are all lowercase here)
     *
     * @param callback Group Map
     */
    public void getLowercaseGroups(Callback<Map<String, List<Server>>> callback) {
        if (Util.isNull(callback)) throw new NullPointerException();
        getGroups(groups -> {
            TreeMap<String, List<Server>> lowercaseGroups = new TreeMap<String, List<Server>>();
            for (String key : groups.keySet()) {
                lowercaseGroups.put(key.toLowerCase(), groups.get(key));
            }
            callback.run(lowercaseGroups);
        });
    }

    /**
     * Gets a Server Group (Group names are case insensitive here)
     *
     * @param name Group name
     * @param callback a Server Group
     */
    public void getGroup(String name, Callback<List<Server>> callback) {
        if (Util.isNull(name, callback)) throw new NullPointerException();
        StackTraceElement[] origin = new Exception().getStackTrace();
        ((SubDataClient) host.api.getSubDataNetwork()[0]).sendPacket(new PacketDownloadGroupInfo(name, data -> {
            List<Server> servers = null;
            if (data.getKeys().size() > 0) {
                String key = new LinkedList<String>(data.getKeys()).getFirst();
                servers = new ArrayList<Server>();
                for (String server : data.getMap(key).getKeys()) {
                    if (data.getMap(key).getMap(server).getRawString("type", "Server").equals("SubServer")) {
                        servers.add(new SubServer(data.getMap(key).getMap(server)));
                    } else {
                        servers.add(new Server(data.getMap(key).getMap(server)));
                    }
                }
            }

            try {
                callback.run(servers);
            } catch (Throwable e) {
                Throwable ew = new InvocationTargetException(e);
                ew.setStackTrace(origin);
                ew.printStackTrace();
            }
        }));
    }

    /**
     * Gets the Servers (including SubServers)
     *
     * @param callback Server Map
     */
    public void getServers(Callback<Map<String, Server>> callback) {
        if (Util.isNull(callback)) throw new NullPointerException();
        StackTraceElement[] origin = new Exception().getStackTrace();
        ((SubDataClient) host.api.getSubDataNetwork()[0]).sendPacket(new PacketDownloadServerInfo(null, data -> {
            TreeMap<String, Server> servers = new TreeMap<String, Server>();
            for (String server : data.getKeys()) {
                if (data.getMap(server).getRawString("type", "Server").equals("SubServer")) {
                    servers.put(server.toLowerCase(), new SubServer(data.getMap(server)));
                } else {
                    servers.put(server.toLowerCase(), new Server(data.getMap(server)));
                }
            }

            try {
                callback.run(servers);
            } catch (Throwable e) {
                Throwable ew = new InvocationTargetException(e);
                ew.setStackTrace(origin);
                ew.printStackTrace();
            }
        }));
    }

    /**
     * Gets a Server
     *
     * @param name Server name
     * @param callback a Server
     */
    public void getServer(String name, Callback<Server> callback) {
        if (Util.isNull(name, callback)) throw new NullPointerException();
        StackTraceElement[] origin = new Exception().getStackTrace();
        ((SubDataClient) host.api.getSubDataNetwork()[0]).sendPacket(new PacketDownloadServerInfo(name, data -> {
            Server server = null;
            if (data.getKeys().size() > 0) {
                String key = new LinkedList<String>(data.getKeys()).getFirst();
                if (data.getMap(key).getRawString("type", "Server").equals("SubServer")) {
                    server = new SubServer(data.getMap(key));
                } else {
                    server = new Server(data.getMap(key));
                }
            }

            try {
                callback.run(server);
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
        if (Util.isNull(callback)) throw new NullPointerException();
        getServers(servers -> {
            TreeMap<String, SubServer> subservers = new TreeMap<String, SubServer>();
            for (String server : servers.keySet()) {
                if (servers.get(server) instanceof SubServer) subservers.put(server, (SubServer) servers.get(server));
            }
            callback.run(subservers);
        });
    }

    /**
     * Gets a SubServer
     *
     * @param name SubServer name
     * @param callback a SubServer
     */
    public void getSubServer(String name, Callback<SubServer> callback) {
        if (Util.isNull(name, callback)) throw new NullPointerException();
        getServer(name, server -> callback.run((server instanceof SubServer)?(SubServer) server:null));
    }

    /**
     * Gets the known Proxies
     *
     * @param callback Proxy Map
     */
    public void getProxies(Callback<Map<String, Proxy>> callback) {
        if (Util.isNull(callback)) throw new NullPointerException();
        StackTraceElement[] origin = new Exception().getStackTrace();
        ((SubDataClient) host.api.getSubDataNetwork()[0]).sendPacket(new PacketDownloadProxyInfo(null, data -> {
            TreeMap<String, Proxy> proxies = new TreeMap<String, Proxy>();
            for (String proxy : data.getKeys()) {
                proxies.put(proxy.toLowerCase(), new Proxy(data.getMap(proxy)));
            }

            try {
                callback.run(proxies);
            } catch (Throwable e) {
                Throwable ew = new InvocationTargetException(e);
                ew.setStackTrace(origin);
                ew.printStackTrace();
            }
        }));
    }

    /**
     * Gets a Proxy
     *
     * @param name Proxy name
     * @param callback a Proxy
     */
    public void getProxy(String name, Callback<Proxy> callback) {
        if (Util.isNull(name, callback)) throw new NullPointerException();
        StackTraceElement[] origin = new Exception().getStackTrace();
        ((SubDataClient) host.api.getSubDataNetwork()[0]).sendPacket(new PacketDownloadProxyInfo(name, data -> {
            Proxy proxy = null;
            if (data.getKeys().size() > 0) {
                proxy = new Proxy(data.getMap(new LinkedList<String>(data.getKeys()).getFirst()));
            }

            try {
                callback.run(proxy);
            } catch (Throwable e) {
                Throwable ew = new InvocationTargetException(e);
                ew.setStackTrace(origin);
                ew.printStackTrace();
            }
        }));
    }

    /**
     * Get the Master Proxy redis container (null if unavailable)
     *
     * @param callback Master Proxy
     */
    public void getMasterProxy(Callback<Proxy> callback) {
        if (Util.isNull(callback)) throw new NullPointerException();
        StackTraceElement[] origin = new Exception().getStackTrace();
        ((SubDataClient) host.api.getSubDataNetwork()[0]).sendPacket(new PacketDownloadProxyInfo("", data -> {
            Proxy proxy = null;
            if (data != null) {
                proxy = new Proxy(data);
            }

            try {
                callback.run(proxy);
            } catch (Throwable e) {
                Throwable ew = new InvocationTargetException(e);
                ew.setStackTrace(origin);
                ew.printStackTrace();
            }
        }));
    }

    /**
     * Get players on this network across all known proxies
     *
     * @param callback Player Collection
     */
    @SuppressWarnings("unchecked")
    public void getGlobalPlayers(Callback<Collection<NamedContainer<String, UUID>>> callback) {
        if (Util.isNull(callback)) throw new NullPointerException();
        StackTraceElement[] origin = new Exception().getStackTrace();
        ((SubDataClient) host.api.getSubDataNetwork()[0]).sendPacket(new PacketDownloadPlayerList(data -> {
            List<NamedContainer<String, UUID>> players = new ArrayList<NamedContainer<String, UUID>>();
            for (String id : data.getKeys()) {
                players.add(new NamedContainer<String, UUID>(data.getMap(id).getRawString("name"), UUID.fromString(id)));
            }

            try {
                callback.run(players);
            } catch (Throwable e) {
                Throwable ew = new InvocationTargetException(e);
                ew.setStackTrace(origin);
                ew.printStackTrace();
            }
        }));
    }

    /**
     * Gets the SubData Network Manager
     *
     * @return SubData Network Manager
     */
    public DataClient[] getSubDataNetwork() {
        LinkedList<Integer> keys = new LinkedList<Integer>(host.subdata.keySet());
        LinkedList<SubDataClient> channels = new LinkedList<SubDataClient>();
        Collections.sort(keys);
        for (Integer channel : keys) channels.add(host.subdata.get(channel));
        return channels.toArray(new DataClient[0]);
    }

    /**
     * Gets the current SubServers Lang Channels
     *
     * @return SubServers Lang Channel list
     */
    public Collection<String> getLangChannels() {
        return host.lang.get().keySet();
    }

    /**
     * Gets values from the SubServers Lang
     *
     * @param channel Lang Channel
     * @return Lang Value
     */
    public Map<String, String> getLang(String channel) {
        if (Util.isNull(channel)) throw new NullPointerException();
        return new LinkedHashMap<>(host.lang.get().get(channel.toLowerCase()));
    }

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

    /**
     * Gets the SubServers App Info
     *
     * @return SubServers App Info
     */
    public PluginInfo getAppInfo() {
        return host.info;
    }
}

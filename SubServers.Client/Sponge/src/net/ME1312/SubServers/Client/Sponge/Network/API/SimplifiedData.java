package net.ME1312.SubServers.Client.Sponge.Network.API;

import net.ME1312.Galaxi.Library.Callback.Callback;
import net.ME1312.Galaxi.Library.Container.NamedContainer;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Client.DataClient;
import net.ME1312.SubData.Client.SubDataClient;
import net.ME1312.SubServers.Client.Sponge.Network.Packet.*;
import net.ME1312.SubServers.Client.Sponge.SubAPI;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * Simplified Data Requesting Class
 */
public final class SimplifiedData {
    private SimplifiedData() {}
    static SubDataClient client(DataClient client) {
        return (SubDataClient) ((client != null)? client : SubAPI.getInstance().getSubDataNetwork()[0]);
    }

    /**
     * Requests the Hosts
     *
     * @param client SubData connection
     * @param callback Host Map
     */
    public static void requestHosts(DataClient client, Callback<Map<String, Host>> callback) {
        if (Util.isNull(callback)) throw new NullPointerException();
        StackTraceElement[] origin = new Exception().getStackTrace();
        client(client).sendPacket(new PacketDownloadHostInfo(null, data -> {
            TreeMap<String, Host> hosts = new TreeMap<String, Host>();
            for (String host : data.getKeys()) {
                hosts.put(host.toLowerCase(), new Host(client, data.getMap(host)));
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
     * Requests a Host
     *
     * @param client SubData connection
     * @param name Host name
     * @param callback a Host
     */
    public static void requestHost(DataClient client, String name, Callback<Host> callback) {
        if (Util.isNull(name, callback)) throw new NullPointerException();
        StackTraceElement[] origin = new Exception().getStackTrace();
        client(client).sendPacket(new PacketDownloadHostInfo(Collections.singletonList(name), data -> {
            Host host = null;
            if (data.getKeys().size() > 0) {
                host = new Host(client, data.getMap(new LinkedList<String>(data.getKeys()).getFirst()));
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
     * Requests the Server Groups (Group names are case sensitive here)
     *
     * @param client SubData connection
     * @param callback Group Map
     */
    public static void requestGroups(DataClient client, Callback<Map<String, List<Server>>> callback) {
        if (Util.isNull(callback)) throw new NullPointerException();
        StackTraceElement[] origin = new Exception().getStackTrace();
        client(client).sendPacket(new PacketDownloadGroupInfo(null, data -> {
            TreeMap<String, List<Server>> groups = new TreeMap<String, List<Server>>();
            for (String group : data.getKeys()) {
                ArrayList<Server> servers = new ArrayList<Server>();
                for (String server : data.getMap(group).getKeys()) {
                    if (data.getMap(group).getMap(server).getRawString("type", "Server").equals("SubServer")) {
                        servers.add(new SubServer(client, data.getMap(group).getMap(server)));
                    } else {
                        servers.add(new Server(client, data.getMap(group).getMap(server)));
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
     * Requests the Server Groups (Group names are all lowercase here)
     *
     * @param client SubData connection
     * @param callback Group Map
     */
    public static void requestLowercaseGroups(DataClient client, Callback<Map<String, List<Server>>> callback) {
        if (Util.isNull(callback)) throw new NullPointerException();
        requestGroups(client, groups -> {
            TreeMap<String, List<Server>> lowercaseGroups = new TreeMap<String, List<Server>>();
            for (String key : groups.keySet()) {
                lowercaseGroups.put(key.toLowerCase(), groups.get(key));
            }
            callback.run(lowercaseGroups);
        });
    }

    /**
     * Requests a Server Group (Group names are case insensitive here)
     *
     * @param client SubData connection
     * @param name Group name
     * @param callback a Server Group
     */
    public static void requestGroup(DataClient client, String name, Callback<NamedContainer<String, List<Server>>> callback) {
        if (Util.isNull(name, callback)) throw new NullPointerException();
        StackTraceElement[] origin = new Exception().getStackTrace();
        client(client).sendPacket(new PacketDownloadGroupInfo(Collections.singletonList(name), data -> {
            NamedContainer<String, List<Server>> group = null;
            if (data.getKeys().size() > 0) {
                String key = new LinkedList<String>(data.getKeys()).getFirst();
                List<Server> servers = new ArrayList<Server>();
                for (String server : data.getMap(key).getKeys()) {
                    if (data.getMap(key).getMap(server).getRawString("type", "Server").equals("SubServer")) {
                        servers.add(new SubServer(client, data.getMap(key).getMap(server)));
                    } else {
                        servers.add(new Server(client, data.getMap(key).getMap(server)));
                    }
                }
                group = new NamedContainer<>(key, servers);
            }

            try {
                callback.run(group);
            } catch (Throwable e) {
                Throwable ew = new InvocationTargetException(e);
                ew.setStackTrace(origin);
                ew.printStackTrace();
            }
        }));
    }

    /**
     * Requests the Servers (including SubServers)
     *
     * @param client SubData connection
     * @param callback Server Map
     */
    public static void requestServers(DataClient client, Callback<Map<String, Server>> callback) {
        if (Util.isNull(callback)) throw new NullPointerException();
        StackTraceElement[] origin = new Exception().getStackTrace();
        client(client).sendPacket(new PacketDownloadServerInfo(null, data -> {
            TreeMap<String, Server> servers = new TreeMap<String, Server>();
            for (String server : data.getKeys()) {
                if (data.getMap(server).getRawString("type", "Server").equals("SubServer")) {
                    servers.put(server.toLowerCase(), new SubServer(client, data.getMap(server)));
                } else {
                    servers.put(server.toLowerCase(), new Server(client, data.getMap(server)));
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
     * Requests a Server
     *
     * @param client SubData connection
     * @param name Server name
     * @param callback a Server
     */
    public static void requestServer(DataClient client, String name, Callback<Server> callback) {
        if (Util.isNull(name, callback)) throw new NullPointerException();
        StackTraceElement[] origin = new Exception().getStackTrace();
        client(client).sendPacket(new PacketDownloadServerInfo(Collections.singletonList(name), data -> {
            Server server = null;
            if (data.getKeys().size() > 0) {
                String key = new LinkedList<String>(data.getKeys()).getFirst();
                if (data.getMap(key).getRawString("type", "Server").equals("SubServer")) {
                    server = new SubServer(client, data.getMap(key));
                } else {
                    server = new Server(client, data.getMap(key));
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
     * Requests the SubServers
     *
     * @param client SubData connection
     * @param callback SubServer Map
     */
    public static void requestSubServers(DataClient client, Callback<Map<String, SubServer>> callback) {
        if (Util.isNull(callback)) throw new NullPointerException();
        requestServers(client, servers -> {
            TreeMap<String, SubServer> subservers = new TreeMap<String, SubServer>();
            for (String server : servers.keySet()) {
                if (servers.get(server) instanceof SubServer) subservers.put(server, (SubServer) servers.get(server));
            }
            callback.run(subservers);
        });
    }

    /**
     * Requests a SubServer
     *
     * @param client SubData connection
     * @param name SubServer name
     * @param callback a SubServer
     */
    public static void requestSubServer(DataClient client, String name, Callback<SubServer> callback) {
        if (Util.isNull(name, callback)) throw new NullPointerException();
        requestServer(client, name, server -> callback.run((server instanceof SubServer)?(SubServer) server:null));
    }

    /**
     * Requests the known Proxies
     *
     * @param client SubData connection
     * @param callback Proxy Map
     */
    public static void requestProxies(DataClient client, Callback<Map<String, Proxy>> callback) {
        if (Util.isNull(callback)) throw new NullPointerException();
        StackTraceElement[] origin = new Exception().getStackTrace();
        client(client).sendPacket(new PacketDownloadProxyInfo(null, data -> {
            TreeMap<String, Proxy> proxies = new TreeMap<String, Proxy>();
            for (String proxy : data.getKeys()) {
                proxies.put(proxy.toLowerCase(), new Proxy(client, data.getMap(proxy)));
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
     * Requests a Proxy
     *
     * @param client SubData connection
     * @param name Proxy name
     * @param callback a Proxy
     */
    public static void requestProxy(DataClient client, String name, Callback<Proxy> callback) {
        if (Util.isNull(name, callback)) throw new NullPointerException();
        StackTraceElement[] origin = new Exception().getStackTrace();
        client(client).sendPacket(new PacketDownloadProxyInfo(Collections.singletonList(name), data -> {
            Proxy proxy = null;
            if (data.getKeys().size() > 0) {
                proxy = new Proxy(client, data.getMap(new LinkedList<String>(data.getKeys()).getFirst()));
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
     * Request the Master Proxy redis container (null if unavailable)
     *
     * @param client SubData connection
     * @param callback Master Proxy
     */
    public static void requestMasterProxy(DataClient client, Callback<Proxy> callback) {
        if (Util.isNull(callback)) throw new NullPointerException();
        StackTraceElement[] origin = new Exception().getStackTrace();
        client(client).sendPacket(new PacketDownloadProxyInfo(Collections.emptyList(), data -> {
            Proxy proxy = null;
            if (data.getKeys().size() > 0) {
                proxy = new Proxy(client, data.getMap(new LinkedList<String>(data.getKeys()).getFirst()));
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
     * Requests players on this network across all known proxies
     *
     * @param client SubData connection
     * @param callback Remote Player Collection
     */
    public static void requestGlobalPlayers(DataClient client, Callback<Map<UUID, RemotePlayer>> callback) {
        if (Util.isNull(callback)) throw new NullPointerException();
        StackTraceElement[] origin = new Exception().getStackTrace();
        client(client).sendPacket(new PacketDownloadPlayerInfo((List<UUID>) null, data -> {
            TreeMap<UUID, RemotePlayer> players = new TreeMap<UUID, RemotePlayer>();
            for (String player : data.getKeys()) {
                players.put(UUID.fromString(player), new RemotePlayer(client, data.getMap(player)));
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
     * Requests a player on this network by searching across all known proxies
     *
     * @param client SubData connection
     * @param name Player name
     * @param callback Remote Player
     */
    public static void requestGlobalPlayer(DataClient client, String name, Callback<RemotePlayer> callback) {
        if (Util.isNull(name, callback)) throw new NullPointerException();
        StackTraceElement[] origin = new Exception().getStackTrace();
        client(client).sendPacket(new PacketDownloadPlayerInfo(Collections.singletonList(name), data -> {
            RemotePlayer player = null;
            if (data.getKeys().size() > 0) {
                player = new RemotePlayer(client, data.getMap(new LinkedList<String>(data.getKeys()).getFirst()));
            }

            try {
                callback.run(player);
            } catch (Throwable e) {
                Throwable ew = new InvocationTargetException(e);
                ew.setStackTrace(origin);
                ew.printStackTrace();
            }
        }));
    }

    /**
     * Requests a player on this network by searching across all known proxies
     *
     * @param client SubData connection
     * @param id Player UUID
     * @param callback Remote Player
     */
    public static void requestGlobalPlayer(DataClient client, UUID id, Callback<RemotePlayer> callback) {
        if (Util.isNull(id, callback)) throw new NullPointerException();
        StackTraceElement[] origin = new Exception().getStackTrace();
        client(client).sendPacket(new PacketDownloadPlayerInfo(Collections.singletonList(id), data -> {
            RemotePlayer player = null;
            if (data.getKeys().size() > 0) {
                player = new RemotePlayer(client, data.getMap(new LinkedList<String>(data.getKeys()).getFirst()));
            }

            try {
                callback.run(player);
            } catch (Throwable e) {
                Throwable ew = new InvocationTargetException(e);
                ew.setStackTrace(origin);
                ew.printStackTrace();
            }
        }));
    }
}

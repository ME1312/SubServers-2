package net.ME1312.SubServers.Client.Common.Network.API;

import net.ME1312.Galaxi.Library.Container.ContainedPair;
import net.ME1312.Galaxi.Library.Container.Pair;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Client.DataClient;
import net.ME1312.SubData.Client.SubDataClient;
import net.ME1312.SubServers.Client.Common.ClientAPI;
import net.ME1312.SubServers.Client.Common.Network.Packet.*;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.function.Consumer;

/**
 * Simplified Data Requesting Class
 */
public final class SimplifiedData {
    private SimplifiedData() {}
    static SubDataClient client(DataClient client) {
        return (SubDataClient) ((client != null)? client : ClientAPI.getInstance().getSubDataNetwork()[0]);
    }

    /**
     * Requests the Hosts
     *
     * @param client SubData connection
     * @param callback Host Map
     */
    public static void requestHosts(DataClient client, Consumer<Map<String, Host>> callback) {
        Util.nullpo(callback);
        StackTraceElement[] origin = new Throwable().getStackTrace();
        client(client).sendPacket(new PacketDownloadHostInfo(null, data -> {
            TreeMap<String, Host> hosts = new TreeMap<String, Host>();
            for (String host : data.getKeys()) {
                hosts.put(host.toLowerCase(), new Host(client, data.getMap(host)));
            }

            try {
                callback.accept(hosts);
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
    public static void requestHost(DataClient client, String name, Consumer<Host> callback) {
        Util.nullpo(name, callback);
        StackTraceElement[] origin = new Throwable().getStackTrace();
        client(client).sendPacket(new PacketDownloadHostInfo(Collections.singletonList(name), data -> {
            Host host = null;
            if (data.getKeys().size() > 0) {
                host = new Host(client, data.getMap(new LinkedList<String>(data.getKeys()).getFirst()));
            }

            try {
                callback.accept(host);
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
    public static void requestGroups(DataClient client, Consumer<Map<String, List<Server>>> callback) {
        Util.nullpo(callback);
        StackTraceElement[] origin = new Throwable().getStackTrace();
        client(client).sendPacket(new PacketDownloadGroupInfo(null, data -> {
            TreeMap<String, List<Server>> groups = new TreeMap<String, List<Server>>();
            for (String group : data.getKeys()) {
                ArrayList<Server> servers = new ArrayList<Server>();
                for (String server : data.getMap(group).getKeys()) {
                    if (data.getMap(group).getMap(server).getString("type", "Server").equals("SubServer")) {
                        servers.add(new SubServer(client, data.getMap(group).getMap(server)));
                    } else {
                        servers.add(new Server(client, data.getMap(group).getMap(server)));
                    }
                }
                if (servers.size() > 0) groups.put(group, servers);
            }

            try {
                callback.accept(groups);
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
    public static void requestLowercaseGroups(DataClient client, Consumer<Map<String, List<Server>>> callback) {
        Util.nullpo(callback);
        requestGroups(client, groups -> {
            TreeMap<String, List<Server>> lowercaseGroups = new TreeMap<String, List<Server>>();
            for (String key : groups.keySet()) {
                lowercaseGroups.put(key.toLowerCase(), groups.get(key));
            }
            callback.accept(lowercaseGroups);
        });
    }

    /**
     * Requests a Server Group (Group names are case insensitive here)
     *
     * @param client SubData connection
     * @param name Group name
     * @param callback a Server Group
     */
    public static void requestGroup(DataClient client, String name, Consumer<Pair<String, List<Server>>> callback) {
        Util.nullpo(callback);
        StackTraceElement[] origin = new Throwable().getStackTrace();
        client(client).sendPacket(new PacketDownloadGroupInfo((name == null)?Collections.emptyList():Collections.singletonList(name), data -> {
            Pair<String, List<Server>> group = null;
            if (data.getKeys().size() > 0) {
                String key = new LinkedList<String>(data.getKeys()).getFirst();
                List<Server> servers = new LinkedList<Server>();
                for (String server : data.getMap(key).getKeys()) {
                    if (data.getMap(key).getMap(server).getString("type", "Server").equals("SubServer")) {
                        servers.add(new SubServer(client, data.getMap(key).getMap(server)));
                    } else {
                        servers.add(new Server(client, data.getMap(key).getMap(server)));
                    }
                }
                group = new ContainedPair<>(key, servers);
            }

            try {
                callback.accept(group);
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
    public static void requestServers(DataClient client, Consumer<Map<String, Server>> callback) {
        Util.nullpo(callback);
        StackTraceElement[] origin = new Throwable().getStackTrace();
        client(client).sendPacket(new PacketDownloadServerInfo(null, data -> {
            TreeMap<String, Server> servers = new TreeMap<String, Server>();
            for (String server : data.getKeys()) {
                if (data.getMap(server).getString("type", "Server").equals("SubServer")) {
                    servers.put(server.toLowerCase(), new SubServer(client, data.getMap(server)));
                } else {
                    servers.put(server.toLowerCase(), new Server(client, data.getMap(server)));
                }
            }

            try {
                callback.accept(servers);
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
    public static void requestServer(DataClient client, String name, Consumer<Server> callback) {
        Util.nullpo(name, callback);
        StackTraceElement[] origin = new Throwable().getStackTrace();
        client(client).sendPacket(new PacketDownloadServerInfo(Collections.singletonList(name), data -> {
            Server server = null;
            if (data.getKeys().size() > 0) {
                String key = new LinkedList<String>(data.getKeys()).getFirst();
                if (data.getMap(key).getString("type", "Server").equals("SubServer")) {
                    server = new SubServer(client, data.getMap(key));
                } else {
                    server = new Server(client, data.getMap(key));
                }
            }

            try {
                callback.accept(server);
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
    public static void requestSubServers(DataClient client, Consumer<Map<String, SubServer>> callback) {
        Util.nullpo(callback);
        requestServers(client, servers -> {
            TreeMap<String, SubServer> subservers = new TreeMap<String, SubServer>();
            for (String server : servers.keySet()) {
                if (servers.get(server) instanceof SubServer) subservers.put(server, (SubServer) servers.get(server));
            }
            callback.accept(subservers);
        });
    }

    /**
     * Requests a SubServer
     *
     * @param client SubData connection
     * @param name SubServer name
     * @param callback a SubServer
     */
    public static void requestSubServer(DataClient client, String name, Consumer<SubServer> callback) {
        Util.nullpo(name, callback);
        requestServer(client, name, server -> callback.accept((server instanceof SubServer)?(SubServer) server:null));
    }

    /**
     * Requests the known Proxies
     *
     * @param client SubData connection
     * @param callback Proxy Map
     */
    public static void requestProxies(DataClient client, Consumer<Map<String, Proxy>> callback) {
        Util.nullpo(callback);
        StackTraceElement[] origin = new Throwable().getStackTrace();
        client(client).sendPacket(new PacketDownloadProxyInfo(null, data -> {
            TreeMap<String, Proxy> proxies = new TreeMap<String, Proxy>();
            for (String proxy : data.getKeys()) {
                proxies.put(proxy.toLowerCase(), new Proxy(client, data.getMap(proxy)));
            }

            try {
                callback.accept(proxies);
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
    public static void requestProxy(DataClient client, String name, Consumer<Proxy> callback) {
        Util.nullpo(name, callback);
        StackTraceElement[] origin = new Throwable().getStackTrace();
        client(client).sendPacket(new PacketDownloadProxyInfo(Collections.singletonList(name), data -> {
            Proxy proxy = null;
            if (data.getKeys().size() > 0) {
                proxy = new Proxy(client, data.getMap(new LinkedList<String>(data.getKeys()).getFirst()));
            }

            try {
                callback.accept(proxy);
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
    public static void requestMasterProxy(DataClient client, Consumer<Proxy> callback) {
        Util.nullpo(callback);
        StackTraceElement[] origin = new Throwable().getStackTrace();
        client(client).sendPacket(new PacketDownloadProxyInfo(Collections.emptyList(), data -> {
            Proxy proxy = null;
            if (data.getKeys().size() > 0) {
                proxy = new Proxy(client, data.getMap(new LinkedList<String>(data.getKeys()).getFirst()));
            }

            try {
                callback.accept(proxy);
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
    public static void requestRemotePlayers(DataClient client, Consumer<Map<UUID, RemotePlayer>> callback) {
        Util.nullpo(callback);
        StackTraceElement[] origin = new Throwable().getStackTrace();
        client(client).sendPacket(new PacketDownloadPlayerInfo((List<UUID>) null, data -> {
            TreeMap<UUID, RemotePlayer> players = new TreeMap<UUID, RemotePlayer>();
            for (String player : data.getKeys()) {
                players.put(UUID.fromString(player), RemotePlayer.instance.construct(client, data.getMap(player)));
            }

            try {
                callback.accept(players);
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
    public static void requestRemotePlayer(DataClient client, String name, Consumer<RemotePlayer> callback) {
        Util.nullpo(name, callback);
        StackTraceElement[] origin = new Throwable().getStackTrace();
        client(client).sendPacket(new PacketDownloadPlayerInfo(Collections.singletonList(name), data -> {
            RemotePlayer player = null;
            if (data.getKeys().size() > 0) {
                player = RemotePlayer.instance.construct(client, data.getMap(new LinkedList<String>(data.getKeys()).getFirst()));
            }

            try {
                callback.accept(player);
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
    public static void requestRemotePlayer(DataClient client, UUID id, Consumer<RemotePlayer> callback) {
        Util.nullpo(id, callback);
        StackTraceElement[] origin = new Throwable().getStackTrace();
        client(client).sendPacket(new PacketDownloadPlayerInfo(Collections.singletonList(id), data -> {
            RemotePlayer player = null;
            if (data.getKeys().size() > 0) {
                player = RemotePlayer.instance.construct(client, data.getMap(new LinkedList<String>(data.getKeys()).getFirst()));
            }

            try {
                callback.accept(player);
            } catch (Throwable e) {
                Throwable ew = new InvocationTargetException(e);
                ew.setStackTrace(origin);
                ew.printStackTrace();
            }
        }));
    }
}

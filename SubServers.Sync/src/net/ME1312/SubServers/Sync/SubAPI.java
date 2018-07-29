package net.ME1312.SubServers.Sync;

import net.ME1312.SubServers.Sync.Library.*;
import net.ME1312.SubServers.Sync.Library.Version.Version;
import net.ME1312.SubServers.Sync.Network.API.Host;
import net.ME1312.SubServers.Sync.Network.API.Proxy;
import net.ME1312.SubServers.Sync.Network.API.Server;
import net.ME1312.SubServers.Sync.Network.API.SubServer;
import net.ME1312.SubServers.Sync.Network.Packet.PacketDownloadNetworkList;
import net.ME1312.SubServers.Sync.Network.Packet.PacketDownloadPlayerList;
import net.ME1312.SubServers.Sync.Network.Packet.PacketDownloadServerList;
import net.ME1312.SubServers.Sync.Network.SubDataClient;
import net.md_5.bungee.protocol.ProtocolConstants;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * SubAPI Class
 */
public final class SubAPI {
    LinkedList<NamedContainer<Runnable, Runnable>> listeners = new LinkedList<NamedContainer<Runnable, Runnable>>();
    private final SubPlugin plugin;
    private static SubAPI api;

    protected SubAPI(SubPlugin plugin) {
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
    public SubPlugin getInternals() {
        return plugin;
    }

    /**
     * Adds a SubAPI Listener
     *
     * @param enable An Event that will be called when SubAPI is ready
     * @param disable An Event that will be called before SubAPI is disabled
     */
    public void addListener(Runnable enable, Runnable disable) {
        listeners.add(new NamedContainer<Runnable, Runnable>(enable, disable));
    }

    /**
     * Gets the Hosts
     *
     * @param callback Host Map
     */
    public void getHosts(Callback<Map<String, Host>> callback) {
        if (Util.isNull(callback)) throw new NullPointerException();
        StackTraceElement[] origin = new Exception().getStackTrace();
        plugin.subdata.sendPacket(new PacketDownloadServerList(null, null, data -> {
            TreeMap<String, Host> hosts = new TreeMap<String, Host>();
            for (String host : data.getSection("hosts").getKeys()) {
                hosts.put(host.toLowerCase(), new Host(data.getSection("hosts").getSection(host)));
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
        getHosts(hosts -> callback.run(hosts.get(name.toLowerCase())));
    }

    /**
     * Gets the Server Groups (Group names are case sensitive here)
     *
     * @param callback Group Map
     */
    public void getGroups(Callback<Map<String, List<Server>>> callback) {
        if (Util.isNull(callback)) throw new NullPointerException();
        StackTraceElement[] origin = new Exception().getStackTrace();
        plugin.subdata.sendPacket(new PacketDownloadServerList(null, null, data -> {
            TreeMap<String, List<Server>> groups = new TreeMap<String, List<Server>>();
            for (String group : data.getSection("groups").getKeys()) {
                ArrayList<Server> servers = new ArrayList<Server>();
                for (String server : data.getSection("groups").getSection(group).getKeys()) {
                    if (data.getSection("groups").getSection(group).getSection(server).getRawString("type", "Server").equals("SubServer")) {
                        servers.add(new SubServer(data.getSection("groups").getSection(group).getSection(server)));
                    } else {
                        servers.add(new Server(data.getSection("groups").getSection(group).getSection(server)));
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
        getLowercaseGroups(groups -> callback.run(groups.get(name.toLowerCase())));
    }

    /**
     * Gets the Servers (including SubServers)
     *
     * @param callback Server Map
     */
    public void getServers(Callback<Map<String, Server>> callback) {
        if (Util.isNull(callback)) throw new NullPointerException();
        StackTraceElement[] origin = new Exception().getStackTrace();
        plugin.subdata.sendPacket(new PacketDownloadServerList(null, null, data -> {
            TreeMap<String, Server> servers = new TreeMap<String, Server>();
            for (String server : data.getSection("servers").getKeys()) {
                servers.put(server.toLowerCase(), new Server(data.getSection("servers").getSection(server)));
            }
            for (String host : data.getSection("hosts").getKeys()) {
                for (String subserver : data.getSection("hosts").getSection(host).getSection("servers").getKeys()) {
                    servers.put(subserver.toLowerCase(), new SubServer(data.getSection("hosts").getSection(host).getSection("servers").getSection(subserver)));
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
        getServers(servers -> callback.run(servers.get(name.toLowerCase())));
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
        getSubServers(subservers -> callback.run(subservers.get(name.toLowerCase())));
    }

    /**
     * Gets the known Proxies
     *
     * @return Proxy Map
     */
    public void getProxies(Callback<Map<String, Proxy>> callback) {
        if (Util.isNull(callback)) throw new NullPointerException();
        StackTraceElement[] origin = new Exception().getStackTrace();
        plugin.subdata.sendPacket(new PacketDownloadNetworkList(data -> {
            TreeMap<String, Proxy> proxies = new TreeMap<String, Proxy>();
            for (String client : data.getSection("clients").getKeys()) {
                if (data.getSection("clients").getSection(client).getKeys().size() > 0 && data.getSection("clients").getSection(client).getRawString("type", "").equals("Proxy")) {
                    proxies.put(data.getSection("clients").getSection(client).getRawString("name").toLowerCase(), new Proxy(data.getSection("clients").getSection(client)));
                }
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
     * @return a Proxy
     */
    public void getProxy(String name, Callback<Proxy> callback) {
        if (Util.isNull(name, callback)) throw new NullPointerException();
        getProxies(proxies -> callback.run(proxies.get(name.toLowerCase())));
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
        plugin.subdata.sendPacket(new PacketDownloadPlayerList(data -> {
            List<NamedContainer<String, UUID>> players = new ArrayList<NamedContainer<String, UUID>>();
            for (String id : data.getSection("players").getKeys()) {
                players.add(new NamedContainer<String, UUID>(data.getSection("players").getSection(id).getRawString("name"), UUID.fromString(id)));
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
    public SubDataClient getSubDataNetwork() {
        return plugin.subdata;
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
     * Gets the Runtime Directory
     *
     * @return Directory
     */
    public UniversalFile getRuntimeDirectory() {
        return plugin.dir;
    }

    /**
     * Gets the SubServers.Sync Version
     *
     * @return SubServers.Sync Version
     */
    public Version getWrapperVersion() {
        return plugin.version;
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
            } else if (!Util.getDespiteException(() -> ProtocolConstants.SUPPORTED_VERSIONS != null, false)) {
                List<Version> versions = new LinkedList<Version>();
                for (String version : ProtocolConstants.SUPPORTED_VERSIONS) versions.add(new Version(version));
                Collections.sort(versions);
                return versions.toArray(new Version[versions.size()]);
            } else if (!Util.getDespiteException(() -> plugin.getGameVersion() != null, false)) {
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

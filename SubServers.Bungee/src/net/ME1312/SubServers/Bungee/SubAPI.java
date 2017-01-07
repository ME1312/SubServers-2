package net.ME1312.SubServers.Bungee;

import net.ME1312.SubServers.Bungee.Event.SubAddServerEvent;
import net.ME1312.SubServers.Bungee.Host.Server;
import net.ME1312.SubServers.Bungee.Host.Host;
import net.ME1312.SubServers.Bungee.Host.SubServer;
import net.ME1312.SubServers.Bungee.Library.UniversalFile;
import net.ME1312.SubServers.Bungee.Library.Version.Version;
import net.ME1312.SubServers.Bungee.Network.SubDataServer;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/**
 * SubAPI Class
 */
public final class SubAPI {
    private SubPlugin plugin;
    private static SubAPI api;

    protected SubAPI(SubPlugin plugin) {
        this.plugin = plugin;
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
     * Gets the SubData Network Manager
     *
     * @return SubData Network Manager
     */
    public SubDataServer getSubDataNetwork() {
        return plugin.subdata;
    }

    /**
     * Adds a Driver for Hosts
     *
     * @param driver Driver to add
     * @param handle Handle to Bind
     */
    public void addHostDriver(Class<? extends Host> driver, String handle) {
        if (plugin.hostDrivers.keySet().contains(handle.toLowerCase())) throw new IllegalStateException("Driver already exists: " + handle);
        plugin.hostDrivers.put(handle, driver);
    }

    /**
     * Gets the Hosts
     *
     * @return Host Map
     */
    public Map<String, Host> getHosts() {
        return new TreeMap<>(plugin.hosts);
    }

    /**
     * Gets a Host
     *
     * @param name Host name
     * @return a Host
     */
    public Host getHost(String name) {
        return getHosts().get(name.toLowerCase());
    }

    /**
     * Gets the Servers (including SubServers)
     *
     * @return Server Map
     */
    public Map<String, Server> getServers() {
        TreeMap<String, Server> servers = new TreeMap<String, Server>();
        servers.putAll(plugin.exServers);
        for (Host host : plugin.hosts.values()) {
            servers.putAll(host.getSubServers());
        }
        return servers;
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
     * @return The Server
     */
    public Server addServer(String name, InetAddress ip, int port, String motd, boolean hidden, boolean restricted) {
        return addServer(null, name, ip, port, motd, hidden, restricted);
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
     * @return The Server
     */
    public Server addServer(UUID player, String name, InetAddress ip, int port, String motd, boolean hidden, boolean restricted) {
        Server server = new Server(name, new InetSocketAddress(ip, port), motd, hidden, restricted);
        SubAddServerEvent event = new SubAddServerEvent(player, null, server);
        plugin.getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
            plugin.exServers.put(name.toLowerCase(), server);
            return server;
        } else {
            return null;
        }
    }

    /**
     * Gets a Server
     *
     * @param name Server name
     * @return a Server
     */
    public Server getServer(String name) {
        return getServers().get(name.toLowerCase());
    }

    /**
     * Gets the SubServers
     *
     * @return SubServer Map
     */
    public Map<String, SubServer> getSubServers() {
        TreeMap<String, SubServer> servers = new TreeMap<String, SubServer>();
        for (Host host : plugin.hosts.values()) {
            servers.putAll(host.getSubServers());
        }
        return servers;
    }

    /**
     * Gets a SubServer
     *
     * @param name SubServer name
     * @return a SubServer
     */
    public SubServer getSubServer(String name) {
        return getSubServers().get(name.toLowerCase());
    }

    /**
     * Gets the SubServers Lang
     *
     * @return SubServers Lang
     */
    public Map<String, String> getLang() {
        HashMap<String, String> lang = new HashMap<String, String>();
        for (String key : plugin.lang.get().getSection("Lang").getKeys()) {
            if (plugin.lang.get().getSection("Lang").isString(key)) lang.put(key, plugin.lang.get().getSection("Lang").getString(key));
        }
        lang.putAll(plugin.exLang);
        return lang;
    }

    /**
     * Adds to the Language Map
     *
     * @param key Key
     * @param value Lang Value
     */
    public void addLang(String key, String value) {
        plugin.exLang.put(key, value);
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
}

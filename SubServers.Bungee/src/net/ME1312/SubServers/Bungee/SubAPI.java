package net.ME1312.SubServers.Bungee;

import com.google.common.collect.Range;
import net.ME1312.SubData.Server.DataServer;
import net.ME1312.SubServers.Bungee.Event.SubAddHostEvent;
import net.ME1312.SubServers.Bungee.Event.SubAddServerEvent;
import net.ME1312.SubServers.Bungee.Event.SubRemoveHostEvent;
import net.ME1312.SubServers.Bungee.Event.SubRemoveServerEvent;
import net.ME1312.SubServers.Bungee.Host.*;
import net.ME1312.SubServers.Bungee.Library.Exception.InvalidHostException;
import net.ME1312.Galaxi.Library.NamedContainer;
import net.ME1312.Galaxi.Library.UniversalFile;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.SubServers.Bungee.Library.Exception.InvalidServerException;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.protocol.ProtocolConstants;

import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.*;

/**
 * SubAPI Class
 */
public final class SubAPI {
    LinkedList<Runnable> enableListeners = new LinkedList<Runnable>();
    LinkedList<Runnable> reloadListeners = new LinkedList<Runnable>();
    LinkedList<Runnable> disableListeners = new LinkedList<Runnable>();
    private static HashMap<String, Object> knownSignatures = new HashMap<String, Object>();
    boolean ready = false;
    private final SubProxy plugin;
    private static SubAPI api;

    protected SubAPI(SubProxy plugin) {
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
    public SubProxy getInternals() {
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
     * Adds a SubAPI Listener
     *
     * @param enable An Event that will be called when SubAPI is ready
     * @param reload An Event that will be called after SubAPI is soft-reloaded
     * @param disable An Event that will be called before SubAPI is disabled (your plugin should reset it's values in case this is a hard-reset instead of a shutdown)
     */
    public void addListener(Runnable enable, Runnable reload, Runnable disable) {
        addListener(enable, disable);
        if (reload != null) reloadListeners.add(reload);
    }

    /**
     * Gets the SubData Network Manager
     *
     * @return SubData Network Manager
     */
    public DataServer getSubDataNetwork() {
        return plugin.subdata;
    }

    /**
     * Adds a Driver for Hosts
     *
     * @param driver Driver to add
     * @param handle Handle to Bind
     */
    public void addHostDriver(Class<? extends Host> driver, String handle) {
        if (Util.isNull(driver, handle)) throw new NullPointerException();
        if (plugin.hostDrivers.keySet().contains(handle.toUpperCase().replace('-', '_').replace(' ', '_'))) throw new IllegalStateException("Driver already exists: " + handle);
        plugin.hostDrivers.put(handle.toUpperCase().replace('-', '_').replace(' ', '_'), driver);
    }

    /**
     * Get a list of all available Host Drivers
     *
     * @return Host Driver handle list
     */
    public List<String> getHostDrivers() {
        return new LinkedList<String>(plugin.hostDrivers.keySet());
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
        if (Util.isNull(name)) throw new NullPointerException();
        return getHosts().get(name.toLowerCase());
    }

    /**
     * Add a Host to the Network
     *
     * @param driver Driver to initiate
     * @param name The Name of your Host
     * @param ports The range of ports to auto-select from
     * @param log Whether apps like SubCreator should log to console (does not apply to servers)
     * @param enabled If your host is Enabled
     * @param address The address of your Host
     * @param directory The runtime directory of your Host
     * @param gitBash The Git Bash directory
     * @return The Host
     * @throws NoSuchMethodException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     * @throws InstantiationException
     */
    public Host addHost(String driver, String name, boolean enabled, Range<Integer> ports, boolean log, InetAddress address, String directory, String gitBash) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        return addHost(null, driver, name, enabled, ports, log, address, directory, gitBash);
    }

    /**
     * Add a Host to the Network
     *
     * @param player Player who added
     * @param driver Driver to initiate
     * @param name The Name of your Host
     * @param ports The range of ports to auto-select from
     * @param log Whether apps like SubCreator should log to console (does not apply to servers)
     * @param enabled If your host is Enabled
     * @param address The address of your Host
     * @param directory The runtime directory of your Host
     * @param gitBash The Git Bash directory
     * @return The Host
     * @throws NoSuchMethodException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     * @throws InstantiationException
     */
    public Host addHost(UUID player, String driver, String name, boolean enabled, Range<Integer> ports, boolean log, InetAddress address, String directory, String gitBash) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        if (Util.isNull(driver, name, enabled, ports, log, address, directory, gitBash)) throw new NullPointerException();
        if (!getHostDrivers().contains(driver.toUpperCase().replace('-', '_').replace(' ', '_'))) throw new InvalidHostException("Invalid Driver for host: " + name);
        return addHost(player, plugin.hostDrivers.get(driver.toUpperCase().replace('-', '_').replace(' ', '_')), name, enabled, ports, log, address, directory, gitBash);
}

    /**
     * Add a Host with a potentially unregistered driver to the Network
     *
     * @param driver Driver to initiate
     * @param name The Name of your Host
     * @param ports The range of ports to auto-select from
     * @param log Whether apps like SubCreator should log to console (does not apply to servers)
     * @param enabled If your host is Enabled
     * @param address The address of your Host
     * @param directory The runtime directory of your Host
     * @param gitBash The Git Bash directory
     * @return The Host
     * @throws NoSuchMethodException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     * @throws InstantiationException
     */
    public Host addHost(Class<? extends Host> driver, String name, boolean enabled, Range<Integer> ports, boolean log, InetAddress address, String directory, String gitBash) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        return addHost(null, driver, name, enabled, ports, log, address, directory, gitBash);
    }

    /**
     * Add a Host with a potentially unregistered driver to the Network
     *
     * @param player Player who added
     * @param driver Driver to initiate
     * @param name The Name of your Host
     * @param ports The range of ports to auto-select from
     * @param log Whether apps like SubCreator should log to console (does not apply to servers)
     * @param enabled If your host is Enabled
     * @param address The address of your Host
     * @param directory The runtime directory of your Host
     * @param gitBash The Git Bash directory
     * @return The Host
     * @throws NoSuchMethodException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     * @throws InstantiationException
     */
    public Host addHost(UUID player, Class<? extends Host> driver, String name, boolean enabled, Range<Integer> ports, boolean log, InetAddress address, String directory, String gitBash) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        if (Util.isNull(driver, name, enabled, ports, log, address, directory, gitBash)) throw new NullPointerException();
        Host host = driver.getConstructor(SubProxy.class, String.class, boolean.class, Range.class, boolean.class, InetAddress.class, String.class, String.class).newInstance(plugin, name, enabled, ports, log, address, directory, gitBash);
        return addHost(player, host)?host:null;
    }

    /**
     * Add a Host with a potentially invalid/unregistered driver to the Network
     *
     * @param host Host to add
     * @return Success status
     */
    public boolean addHost(Host host) {
        return addHost(null, host);
    }

    /**
     * Add a Host with a potentially invalid/unregistered driver to the Network
     *
     * @param player Player who added
     * @param host Host to add
     * @return Success status
     */
    public boolean addHost(UUID player, Host host) {
        SubAddHostEvent event = new SubAddHostEvent(player, host);
        plugin.getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
            plugin.hosts.put(host.getName().toLowerCase(), host);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Remove a Host from the Network
     *
     * @param name Name of the Host
     * @return Success Status
     */
    public boolean removeHost(String name) {
        return removeHost(null, name);
    }

    /**
     * Remove a Host from the Network
     *
     * @param player Player Removing
     * @param name Name of the Host
     * @return Success Status
     */
    public boolean removeHost(UUID player, String name) {
        if (Util.isNull(name, getHost(name))) throw new NullPointerException();
        SubRemoveHostEvent event = new SubRemoveHostEvent(player, getHost(name));
        plugin.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            if (getHost(name).destroy()) {
                plugin.hosts.remove(name.toLowerCase());
                return true;
            } else {
                return false;
            }
        } else return false;
    }

    /**
     * Force Remove a Host from the Network
     *
     * @param name Name of the Host
     * @return Success Status
     */
    public boolean forceRemoveHost(String name) {
        return forceRemoveHost(null, name);
    }

    /**
     * Force Remove a Host from the Network
     *
     * @param player Player Removing
     * @param name Name of the Host
     * @return Success Status
     */
    public boolean forceRemoveHost(UUID player, String name) {
        if (Util.isNull(name, getHost(name))) throw new NullPointerException();
        SubRemoveHostEvent event = new SubRemoveHostEvent(player, getHost(name));
        plugin.getPluginManager().callEvent(event);
        if (getHost(name).destroy()) {
            plugin.hosts.remove(name.toLowerCase());
            return true;
        } else {
            return false;
        }
    }

    /**
     * Gets the Server Groups (Group names are case sensitive here)
     *
     * @return Group Map
     */
    public Map<String, List<Server>> getGroups() {
        TreeMap<String, List<Server>> groups = new TreeMap<String, List<Server>>();
        HashMap<String, String> conflitresolver = new HashMap<String, String>();
        for (Server server : getServers().values()) {
            for (String name : server.getGroups()) {
                String group = name;
                if (conflitresolver.keySet().contains(name.toLowerCase())) {
                    group = conflitresolver.get(name.toLowerCase());
                } else {
                    conflitresolver.put(name.toLowerCase(), name);
                }
                List<Server> list = (groups.keySet().contains(group))?groups.get(group):new ArrayList<Server>();
                list.add(server);
                groups.put(group, list);
            }
        }
        return groups;
    }

    /**
     * Gets the Server Groups (Group names are all lowercase here)
     *
     * @return Group Map
     */
    public Map<String, List<Server>> getLowercaseGroups() {
        Map<String, List<Server>> groups = getGroups();
        TreeMap<String, List<Server>> lowercaseGroups = new TreeMap<String, List<Server>>();
        for (String key : groups.keySet()) {
            lowercaseGroups.put(key.toLowerCase(), groups.get(key));
        }
        return lowercaseGroups;
    }

    /**
     * Gets a Server Group (Group names are case insensitive here)
     *
     * @param name Group name
     * @return a Server Group
     */
    public List<Server> getGroup(String name) {
        if (Util.isNull(name)) throw new NullPointerException();
        return Util.getCaseInsensitively(getGroups(), name);
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
     * Gets a Server
     *
     * @param name Server name
     * @return a Server
     */
    public Server getServer(String name) {
        if (Util.isNull(name)) throw new NullPointerException();
        return getServers().get(name.toLowerCase());
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
        if (getServers().keySet().contains(name.toLowerCase())) throw new InvalidServerException("A Server already exists with this name!");
        Server server = new ServerContainer(name, new InetSocketAddress(ip, port), motd, hidden, restricted);
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
     * Remove a Server from the Network
     *
     * @param name Name of the Server
     * @return Success Status
     */
    public boolean removeServer(String name) {
        return removeServer(null, name);
    }

    /**
     * Remove a Server from the Network
     *
     * @param player Player Removing
     * @param name Name of the Server
     * @return Success Status
     */
    public boolean removeServer(UUID player, String name) {
        if (Util.isNull(name, getServer(name))) throw new NullPointerException();
        SubRemoveServerEvent event = new SubRemoveServerEvent(player, null, getServer(name));
        plugin.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            plugin.exServers.remove(name.toLowerCase());
            return true;
        } else return false;
    }

    /**
     * Force Remove a Server from the Network
     *
     * @param name Name of the Server
     * @return Success Status
     */
    public boolean forceRemoveServer(String name) {
        return forceRemoveServer(null, name);
    }

    /**
     * Force Remove a Server from the Network
     *
     * @param player Player Removing
     * @param name Name of the Server
     * @return Success Status
     */
    public boolean forceRemoveServer(UUID player, String name) {
        if (Util.isNull(name, getServer(name))) throw new NullPointerException();
        SubRemoveServerEvent event = new SubRemoveServerEvent(player, null, getServer(name));
        plugin.getPluginManager().callEvent(event);
        plugin.exServers.remove(name.toLowerCase());
        return true;
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
        if (Util.isNull(name)) throw new NullPointerException();
        return getSubServers().get(name.toLowerCase());
    }

    /**
     * Gets the known Proxies
     *
     * @return Proxy Map
     */
    public Map<String, Proxy> getProxies() {
        return new TreeMap<String, Proxy>(plugin.proxies);
    }

    /**
     * Gets a Proxy
     *
     * @param name Proxy name
     * @return a Proxy
     */
    public Proxy getProxy(String name) {
        if (Util.isNull(name)) throw new NullPointerException();
        return getProxies().get(name.toLowerCase());
    }

    /**
     * Get the Master Proxy redis container (null if unavailable)
     *
     * @return Master Proxy
     */
    public Proxy getMasterProxy() {
        return plugin.redis;
    }

    /**
     * Get players on this network across all known proxies
     *
     * @return Player Collection
     */
    @SuppressWarnings("unchecked")
    public Collection<NamedContainer<String, UUID>> getGlobalPlayers() {
        List<NamedContainer<String, UUID>> players = new ArrayList<NamedContainer<String, UUID>>();
        if (plugin.redis != null) {
            try {
                for (UUID player : (Set<UUID>) plugin.redis("getPlayersOnline")) players.add(new NamedContainer<>((String) plugin.redis("getNameFromUuid", new NamedContainer<>(UUID.class, player)), player));
            } catch (Exception e) {}
        } else {
            for (ProxiedPlayer player : plugin.getPlayers()) players.add(new NamedContainer<>(player.getName(), player.getUniqueId()));
        }
        return players;
    }

    /**
     * Adds to the SubServers Lang
     *
     * @param channel Lang Channel
     * @param key Key
     * @param value Lang Value
     */
    public void setLang(String channel, String key, String value) {
        if (Util.isNull(channel, key, value)) throw new NullPointerException();
        LinkedHashMap<String, String> map = (plugin.exLang.keySet().contains(channel.toLowerCase()))?plugin.exLang.get(channel.toLowerCase()):new LinkedHashMap<String, String>();
        map.put(key, value);
        plugin.exLang.put(channel.toLowerCase(), map);
    }

    /**
     * Gets the current SubServers Lang Channels
     *
     * @return SubServers Lang Channel list
     */
    public Collection<String> getLangChannels() {
        return plugin.exLang.keySet();
    }

    /**
     * Gets values from the SubServers Lang
     *
     * @param channel Lang Channel
     * @return Lang Value
     */
    public Map<String, String> getLang(String channel) {
        if (Util.isNull(channel)) throw new NullPointerException();
        return new LinkedHashMap<>(plugin.exLang.get(channel.toLowerCase()));
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
     * Get an Object Signature without linking the Signature to any object
     *
     * @return Anonymous Object Signature
     */
    public String signAnonymousObject() {
        return plugin.getNewSignature();
    }

    /**
     * Signs an Object
     *
     * @param object Object to Sign
     * @return Object's Signature (or an empty string if the object was null)
     */
    public String signObject(Object object) {
        if (object == null) {
            return "";
        } else {
            String signature = signAnonymousObject();
            knownSignatures.put(signature, object);
            return signature;
        }
    }

    /**
     * Get an Object by it's Signature
     *
     * @param signature Object's Signature
     * @param <R> Expected Object Type
     * @return Object that is tied to this Signature (or null if the signature is unknown)
     */
    @SuppressWarnings("unchecked")
    public <R> R getObjectBySignature(String signature) {
        if (Util.isNull(signature)) throw new NullPointerException();
        return (R) knownSignatures.get(signature);
    }

    /**
     * Invalidate an Object Signature. This will remove the link between the Signature and the Object
     *
     * @param signature Object's Signature
     */
    public void invalidateObjectSignature(String signature) {
        knownSignatures.remove(signature);
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
        return (SubProxy.class.getPackage().getSpecificationTitle() != null)?new Version(SubProxy.class.getPackage().getSpecificationTitle()):null;
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

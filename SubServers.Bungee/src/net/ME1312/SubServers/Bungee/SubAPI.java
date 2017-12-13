package net.ME1312.SubServers.Bungee;

import net.ME1312.SubServers.Bungee.Event.SubAddHostEvent;
import net.ME1312.SubServers.Bungee.Event.SubAddServerEvent;
import net.ME1312.SubServers.Bungee.Event.SubRemoveHostEvent;
import net.ME1312.SubServers.Bungee.Event.SubRemoveServerEvent;
import net.ME1312.SubServers.Bungee.Host.Server;
import net.ME1312.SubServers.Bungee.Host.Host;
import net.ME1312.SubServers.Bungee.Host.ServerContainer;
import net.ME1312.SubServers.Bungee.Host.SubServer;
import net.ME1312.SubServers.Bungee.Library.Exception.InvalidHostException;
import net.ME1312.SubServers.Bungee.Library.NamedContainer;
import net.ME1312.SubServers.Bungee.Library.UniversalFile;
import net.ME1312.SubServers.Bungee.Library.Util;
import net.ME1312.SubServers.Bungee.Library.Version.Version;
import net.ME1312.SubServers.Bungee.Network.SubDataServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.*;

/**
 * SubAPI Class
 */
public final class SubAPI {
    LinkedList<NamedContainer<Runnable, Runnable>> listeners = new LinkedList<NamedContainer<Runnable, Runnable>>();
    LinkedList<Runnable> reloadListeners = new LinkedList<Runnable>();
    private HashMap<String, Object> knownSignatures = new HashMap<String, Object>();
    boolean ready = false;
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
     * Adds a SubAPI Listener
     *
     * @param enable An Event that will be called when SubAPI is ready
     * @param disable An Event that will be called before SubAPI is disabled (your plugin should reset it's values in case this is a hard-reset instead of a shutdown)
     */
    public void addListener(Runnable enable, Runnable disable) {
        if (!Util.isNull(enable, disable)) listeners.add(new NamedContainer<Runnable, Runnable>(enable, disable));
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
    public SubDataServer getSubDataNetwork() {
        return plugin.subdata;
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
     * Adds a Driver for Hosts
     *
     * @param driver Driver to add
     * @param handle Handle to Bind
     */
    public void addHostDriver(Class<? extends Host> driver, String handle) {
        if (Util.isNull(driver, handle)) throw new NullPointerException();
        if (plugin.hostDrivers.keySet().contains(handle.toLowerCase())) throw new IllegalStateException("Driver already exists: " + handle);
        plugin.hostDrivers.put(handle.toLowerCase(), driver);
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
     * @param name Name of the Host
     * @param enabled Enabled Status
     * @param address Address of the Host
     * @param directory Directory of the Host
     * @param gitBash Git Bash Directory
     * @return The Host
     * @throws NoSuchMethodException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     * @throws InstantiationException
     */
    public Host addHost(String driver, String name, boolean enabled, InetAddress address, String directory, String gitBash) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        return addHost(null, driver, name, enabled, address, directory, gitBash);
    }

    /**
     * Add a Host to the Network
     *
     * @param player Player who added
     * @param driver Driver to initiate
     * @param name Name of the Host
     * @param enabled Enabled Status
     * @param address Address of the Host
     * @param directory Directory of the Host
     * @param gitBash Git Bash Directory
     * @return The Host
     * @throws NoSuchMethodException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     * @throws InstantiationException
     */
    public Host addHost(UUID player, String driver, String name, boolean enabled, InetAddress address, String directory, String gitBash) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        if (Util.isNull(driver, name, enabled, address, directory, gitBash)) throw new NullPointerException();
        if (!getHostDrivers().contains(driver)) throw new InvalidHostException("Invalid Driver for host: " + name);
        Host host = plugin.hostDrivers.get(driver.toLowerCase()).getConstructor(SubPlugin.class, String.class, Boolean.class, InetAddress.class, String.class, String.class).newInstance(plugin, name, (Boolean) enabled, address, directory, gitBash);
        SubAddHostEvent event = new SubAddHostEvent(player, host);
        plugin.getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
            plugin.hosts.put(name.toLowerCase(), host);
            return host;
        } else {
            return null;
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
            try {
                List<String> subservers = new ArrayList<String>();
                subservers.addAll(getHost(name).getSubServers().keySet());

                for (String server : subservers) {
                    getHost(name).removeSubServer(server);
                }
                subservers.clear();
                getHost(name).getCreator().terminate();
                getHost(name).getCreator().waitFor();
                plugin.hosts.remove(name.toLowerCase());
                return true;
            } catch (Exception e) {
                e.printStackTrace();
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
        try {
            List<String> subservers = new ArrayList<String>();
            subservers.addAll(getHost(name).getSubServers().keySet());

            for (String server : subservers) {
                getHost(name).removeSubServer(server);
            }
            subservers.clear();
            getHost(name).getCreator().terminate();
            getHost(name).getCreator().waitFor();
            plugin.hosts.remove(name.toLowerCase());
            return true;
        } catch (Exception e) {
            e.printStackTrace();
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
     * Get players on this network across all known proxies
     *
     * @return Player Collection
     */
    @SuppressWarnings("unchecked")
    public Collection<NamedContainer<String, UUID>> getGlobalPlayers() {
        List<NamedContainer<String, UUID>> players = new ArrayList<NamedContainer<String, UUID>>();
        if (plugin.redis) {
            try {
                for (UUID player : (Set<UUID>) plugin.redis("getPlayersOnline")) players.add(new NamedContainer<>((String) plugin.redis("getNameFromUuid", new NamedContainer<>(UUID.class, player)), player));
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            for (ProxiedPlayer player : plugin.getPlayers()) players.add(new NamedContainer<>(player.getName(), player.getUniqueId()));
        }
        return players;
    }

    /**
     * Adds to the Language Map
     *
     * @param key Key
     * @param value Lang Value
     */
    public void setLang(String key, String value) {
        if (Util.isNull(key, value)) throw new NullPointerException();
        plugin.exLang.put(key, value);
    }

    /**
     * Gets a value from the SubServers Lang
     *
     * @param key Key
     * @return Lang Value
     */
    public String getLang(String key) {
        if (Util.isNull(key)) throw new NullPointerException();
        return getLang().get(key);
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
     * Gets the SubServers Beta Version
     *
     * @return SubServers Beta Version (or null if this is a release version)
     */
    public Version getBetaVersion() {
        return plugin.bversion;
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
     * Gets the BungeeCord Version
     *
     * @return BungeeCord Version
     */
    public Version getProxyVersion() {
        return new Version(plugin.getVersion());
    }

    /**
     * Gets the Recommended Minecraft Version
     *
     * @return Minecraft Version
     */
    public Version getGameVersion() {
        if (System.getProperty("subservers.minecraft.version", "").length() > 0) {
            return new Version(System.getProperty("subservers.minecraft.version"));
        } else {
            String raw = plugin.getGameVersion();
            if (raw.contains(",")) {
                String[] split = raw.split(",\\s*");
                return new Version(split[split.length - 1]);
            } else if (raw.contains("-")) {
                String[] split = raw.split("\\s*-\\s*");
                return new Version(split[split.length - 1]);
            } else {
                return new Version(plugin.getGameVersion());
            }
        }
    }
}

package net.ME1312.SubServers.Sync;

import net.ME1312.Galaxi.Library.Callback.Callback;
import net.ME1312.Galaxi.Library.UniversalFile;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Client.DataClient;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.SubData.Client.DataProtocol;
import net.ME1312.SubServers.Bungee.BungeeAPI;
import net.ME1312.SubServers.Client.Common.ClientAPI;
import net.ME1312.SubData.Client.SubDataClient;
import net.ME1312.SubServers.Sync.Server.CachedPlayer;
import net.md_5.bungee.protocol.ProtocolConstants;

import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.util.*;

/**
 * SubAPI Class
 */
public final class SubAPI extends ClientAPI implements BungeeAPI {
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
     * Gets players on this network across all known proxies (Cached)
     *
     * @return Remote Player Collection
     */
    public Map<UUID, CachedPlayer> getGlobalPlayers() {
        return new HashMap<UUID, CachedPlayer>(plugin.rPlayers);
    }

    /**
     * Gets a player on this network by searching across all known proxies (Cached)
     *
     * @param name Player name
     * @return Remote Player
     */
    public CachedPlayer getGlobalPlayer(String name) {
        if (Util.isNull(name)) throw new NullPointerException();
        for (CachedPlayer player : getGlobalPlayers().values()) {
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
    public CachedPlayer getGlobalPlayer(UUID id) {
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

package net.ME1312.SubServers.Sync;

import net.ME1312.Galaxi.Library.UniversalFile;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.SubData.Client.DataClient;
import net.ME1312.SubData.Client.DataProtocol;
import net.ME1312.SubData.Client.SubDataClient;
import net.ME1312.SubServers.Bungee.BungeeAPI;
import net.ME1312.SubServers.Client.Common.ClientAPI;
import net.ME1312.SubServers.Sync.Server.CachedPlayer;
import net.ME1312.SubServers.Sync.Server.ServerImpl;

import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.protocol.ProtocolConstants;

import java.util.*;

/**
 * SubAPI Class
 */
public final class SubAPI extends ClientAPI implements BungeeAPI {
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
     * Get the Proxy Name
     *
     * @return Proxy Name
     */
    public String getName() {
        return name;
    }

    /**
     * Get the number of players on this network across all known proxies
     *
     * @return Remote Player Count
     */
    public int getRemotePlayerCount() {
        return plugin.rPlayers.size();
    }

    /**
     * Get players on this server across all known proxies (Cached)
     *
     * @param server Server to search
     * @return Remote Player Map
     */
    public Map<UUID, CachedPlayer> getRemotePlayers(ServerInfo server) {
        if (server instanceof ServerImpl) {
            HashMap<UUID, CachedPlayer> players = new HashMap<UUID, CachedPlayer>();
            for (UUID id : Util.getBackwards(plugin.rPlayerLinkS, (ServerImpl) server))
                players.put(id, plugin.rPlayers.get(id));
            return players;
        } else {
            return new HashMap<>();
        }
    }

    /**
     * Gets players on this network across all known proxies (Cached)
     *
     * @return Remote Player Map
     */
    public Map<UUID, CachedPlayer> getRemotePlayers() {
        return new HashMap<UUID, CachedPlayer>(plugin.rPlayers);
    }

    /**
     * Gets a player on this network by searching across all known proxies (Cached)
     *
     * @param name Player name
     * @return Remote Player
     */
    public CachedPlayer getRemotePlayer(String name) {
        if (Util.isNull(name)) throw new NullPointerException();
        for (CachedPlayer player : getRemotePlayers().values()) {
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
    public CachedPlayer getRemotePlayer(UUID id) {
        if (Util.isNull(id)) throw new NullPointerException();
        return getRemotePlayers().getOrDefault(id, null);
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
        return plugin.lang.value().keySet();
    }

    /**
     * Gets values from the SubServers Lang
     *
     * @param channel Lang Channel
     * @return Lang Value
     */
    public Map<String, String> getLang(String channel) {
        if (Util.isNull(channel)) throw new NullPointerException();
        return new LinkedHashMap<>(plugin.lang.value().get(channel.toLowerCase()));
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

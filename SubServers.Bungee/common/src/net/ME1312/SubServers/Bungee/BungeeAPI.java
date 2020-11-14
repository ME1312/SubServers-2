package net.ME1312.SubServers.Bungee;

import net.ME1312.Galaxi.Library.UniversalFile;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.SubServers.Bungee.Library.Compatibility.RemotePlayer;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;

import java.util.*;

/**
 * SubAPI BungeeCord Common Class
 */
public interface BungeeAPI {

    /**
     * Gets the SubAPI BungeeCord Common Methods
     *
     * @return SubAPI BungeeCord Common
     */
    static BungeeAPI getInstance() {
        return ((BungeeCommon) ProxyServer.getInstance()).api.run();
    }

    /**
     * Adds a SubAPI Listener
     *
     * @param enable An Event that will be called when SubAPI is ready
     * @param disable An Event that will be called before SubAPI is disabled (your plugin should reset it's values in case this is a hard-reset instead of a shutdown)
     */
    void addListener(Runnable enable, Runnable disable);

    /**
     * Get the number of players on this network across all known proxies
     *
     * @return Remote Player Collection
     */
    int getRemotePlayerCount();

    /**
     * Get players on this server across all known proxies
     *
     * @param server Server to search
     * @return Remote Player Map
     */
    Map<UUID, ? extends RemotePlayer> getRemotePlayers(ServerInfo server);

    /**
     * Get players on this network across all known proxies
     *
     * @return Remote Player Map
     */
    Map<UUID, ? extends RemotePlayer> getRemotePlayers();

    /**
     * Get a player on this network by searching across all known proxies
     *
     * @param name Player name
     * @return Remote Player
     */
    RemotePlayer getRemotePlayer(String name);

    /**
     * Get a player on this network by searching across all known proxies
     *
     * @param id Player UUID
     * @return Remote Player
     */
    RemotePlayer getRemotePlayer(UUID id);

    /**
     * Gets the current SubServers Lang Channels
     *
     * @return SubServers Lang Channel list
     */
    Collection<String> getLangChannels();

    /**
     * Gets values from the SubServers Lang
     *
     * @param channel Lang Channel
     * @return Lang Value
     */
    Map<String, String> getLang(String channel);

    /**
     * Gets a value from the SubServers Lang
     *
     * @param channel Lang Channel
     * @param key Key
     * @return Lang Values
     */
    default String getLang(String channel, String key) {
        if (Util.isNull(channel, key)) throw new NullPointerException();
        return getLang(channel).get(key);
    }

    /**
     * Gets the Runtime Directory
     *
     * @return Directory
     */
    UniversalFile getRuntimeDirectory();

    /**
     * Gets the SubServers Version
     *
     * @return SubServers Version
     */
    Version getWrapperVersion();

    /**
     * Gets the SubServers Build Signature
     *
     * @return SubServers Build Signature (or null if unsigned)
     */
    Version getWrapperBuild();

    /**
     * Gets the BungeeCord Version
     *
     * @return BungeeCord Version
     */
    Version getProxyVersion();

    /**
     * Get an array of compatible Minecraft Versions
     *
     * @return Minecraft Versions
     */
    Version[] getGameVersion();
}

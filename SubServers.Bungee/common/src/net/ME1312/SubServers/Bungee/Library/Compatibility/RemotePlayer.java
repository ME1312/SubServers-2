package net.ME1312.SubServers.Bungee.Library.Compatibility;

import net.md_5.bungee.api.config.ServerInfo;

import java.net.InetSocketAddress;
import java.util.UUID;

/**
 * RemotePlayer Layout Class
 */
public interface RemotePlayer {

    /**
     * Get the UUID of this player.
     *
     * @return the UUID
     */
    UUID getUniqueId();

    /**
     * Get the unique name of this player.
     *
     * @return the player's username
     */
    String getName();

    /**
     * Gets the remote address of this connection.
     *
     * @return the remote address
     */
    InetSocketAddress getAddress();

    /**
     * Gets the name of the proxy this player is connected to.
     *
     * @return the name of the proxy this player is connected to
     */
    String getProxyName();

    /**
     * Gets the name of the server this player is connected to.
     *
     * @return the name of the server this player is connected to
     */
    String getServerName();

    /**
     * Gets the server this player is connected to.
     *
     * @return the server this player is connected to
     */
    ServerInfo getServer();
}

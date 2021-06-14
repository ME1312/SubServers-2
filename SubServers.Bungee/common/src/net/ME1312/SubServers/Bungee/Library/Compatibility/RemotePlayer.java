package net.ME1312.SubServers.Bungee.Library.Compatibility;

import net.ME1312.Galaxi.Library.Callback.Callback;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.UUID;

/**
 * RemotePlayer Layout Class
 */
public interface RemotePlayer {

    /**
     * Get Local Player
     *
     * @return Local Player (or null when not local)
     */
    ProxiedPlayer get();

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

    /**
     * Sends messages to this player
     *
     * @param messages Messages to send
     */
    default void sendMessage(String... messages) {
        sendMessage(messages, i -> {});
    }

    /**
     * Sends messages to this player
     *
     * @param message Message to send
     * @param response Success Status
     */
    default void sendMessage(String message, Callback<Integer> response) {
        sendMessage(new String[]{ message }, response);
    }

    /**
     * Sends messages to this player
     *
     * @param messages Messages to send
     * @param response Success Status
     */
    void sendMessage(String[] messages, Callback<Integer> response);

    /**
     * Sends messages to this player
     *
     * @param messages Messages to send
     */
    default void sendMessage(BaseComponent... messages) {
        sendMessage(messages, i -> {});
    }

    /**
     * Sends messages to this player
     *
     * @param message Message to send
     * @param response Success Status
     */
    default void sendMessage(BaseComponent message, Callback<Integer> response) {
        sendMessage(new BaseComponent[]{ message }, response);
    }

    /**
     * Sends messages to this player
     *
     * @param messages Messages to send
     * @param response Success Status
     */
    void sendMessage(BaseComponent[] messages, Callback<Integer> response);

    /**
     * Transfers this player to another server
     *
     * @param server Target server
     */
    default void transfer(String server) {
        transfer(server, i -> {});
    }

    /**
     * Transfers this player to another server
     *
     * @param server Target server
     * @param response Success status
     */
    void transfer(String server, Callback<Integer> response);

    /**
     * Transfers this player to another server
     *
     * @param server Target server
     */
    default void transfer(ServerInfo server) {
        transfer(server, i -> {});
    }

    /**
     * Transfers this player to another server
     *
     * @param server Target server
     * @param response Success status
     */
    void transfer(ServerInfo server, Callback<Integer> response);

    /**
     * Disconnects this player from the network
     */
    default void disconnect() {
        disconnect(i -> {});
    }

    /**
     * Disconnects this player from the network
     *
     * @param response Success status
     */
    default void disconnect(Callback<Integer> response) {
        disconnect(null, response);
    }

    /**
     * Disconnects this player from the network
     *
     * @param reason Disconnect Reason
     */
    default void disconnect(String reason) {
        disconnect(reason, i -> {});
    }

    /**
     * Disconnects this player from the network
     *
     * @param reason Disconnect Reason
     * @param response Success status
     */
    void disconnect(String reason, Callback<Integer> response);
}

package net.ME1312.SubServers.Bungee.Library.Compatibility;

import net.ME1312.Galaxi.Library.Callback.Callback;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.net.InetSocketAddress;
import java.util.UUID;

import static net.ME1312.SubServers.Bungee.Library.Compatibility.RPSI.instance;

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
     * Sends messages to all players
     *
     * @param messages Messages to send
     */
    static void broadcastMessage(String... messages) {
        broadcastMessage(messages, i -> {});
    }

    /**
     * Sends messages to all players
     *
     * @param message Message to send
     * @param response Success Status
     */
    static void broadcastMessage(String message, Callback<Integer> response) {
        broadcastMessage(new String[]{ message }, response);
    }

    /**
     * Sends messages to all players
     *
     * @param messages Messages to send
     * @param response Success Status
     */
    static void broadcastMessage(String[] messages, Callback<Integer> response) {
        sendMessage(null, messages, response);
    }

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
    default void sendMessage(String[] messages, Callback<Integer> response) {
        sendMessage(new UUID[]{ getUniqueId() }, messages, response);
    }

    /**
     * Sends messages to this player
     *
     * @param messages Messages to send
     */
    static void sendMessage(UUID[] players, String... messages) {
        sendMessage(players, messages, i -> {});
    }

    /**
     * Sends messages to this player
     *
     * @param message Message to send
     * @param response Success Status
     */
    static void sendMessage(UUID[] players, String message, Callback<Integer> response) {
        sendMessage(players, new String[]{ message }, response);
    }

    /**
     * Sends messages to this player
     *
     * @param messages Messages to send
     * @param response Success Status
     */
    static void sendMessage(UUID[] players, String[] messages, Callback<Integer> response) {
        instance.sendMessage(players, messages, response);
    }

    /**
     * Sends messages to all players
     *
     * @param messages Messages to send
     */
    static void broadcastMessage(BaseComponent... messages) {
        broadcastMessage(messages, i -> {});
    }

    /**
     * Sends messages to all players
     *
     * @param message Message to send
     * @param response Success Status
     */
    static void broadcastMessage(BaseComponent message, Callback<Integer> response) {
        broadcastMessage(new BaseComponent[]{ message }, response);
    }

    /**
     * Sends messages to all players
     *
     * @param messages Messages to send
     * @param response Success Status
     */
    static void broadcastMessage(BaseComponent[] messages, Callback<Integer> response) {
        sendMessage(null, messages, response);
    }

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
    default void sendMessage(BaseComponent[] messages, Callback<Integer> response) {
        sendMessage(new UUID[]{ getUniqueId() }, messages, response);
    }

    /**
     * Sends messages to this player
     *
     * @param players Players to select
     * @param messages Messages to send
     */
    static void sendMessage(UUID[] players, BaseComponent... messages) {
        sendMessage(players, messages, i -> {});
    }

    /**
     * Sends messages to this player
     *
     * @param players Players to select
     * @param message Message to send
     * @param response Success Status
     */
    static void sendMessage(UUID[] players, BaseComponent message, Callback<Integer> response) {
        sendMessage(players, new BaseComponent[]{ message }, response);
    }

    /**
     * Sends messages to this player
     *
     * @param players Players to select
     * @param message Message to send
     * @param response Success Status
     */
    static void sendMessage(UUID[] players, BaseComponent[] message, Callback<Integer> response) {
        instance.sendMessage(players, message, response);
    }

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
    default void transfer(String server, Callback<Integer> response) {
        transfer(new UUID[]{ getUniqueId() }, server, response);
    }

    /**
     * Transfers this player to another server
     *
     * @param players Players to select
     * @param server Target server
     */
    static void transfer(UUID[] players, String server) {
        transfer(players, server, i -> {});
    }

    /**
     * Transfers this player to another server
     *
     * @param players Players to select
     * @param server Target server
     * @param response Success status
     */
    static void transfer(UUID[] players, String server, Callback<Integer> response) {
        instance.transfer(players, server, response);
    }

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
    default void transfer(ServerInfo server, Callback<Integer> response) {
        transfer(new UUID[]{ getUniqueId() }, server, response);
    }

    /**
     * Transfers this player to another server
     *
     * @param players Players to select
     * @param server Target server
     */
    static void transfer(UUID[] players, ServerInfo server) {
        transfer(players, server, i -> {});
    }

    /**
     * Transfers this player to another server
     *
     * @param players Players to select
     * @param server Target server
     * @param response Success status
     */
    static void transfer(UUID[] players, ServerInfo server, Callback<Integer> response) {
        instance.transfer(players, server.getName(), response);
    }

    /**
     * Disconnects this player from the network
     */
    default void disconnect() {
        disconnect((String) null);
    }

    /**
     * Disconnects this player from the network
     *
     * @param response Success status
     */
    default void disconnect(Callback<Integer> response) {
        disconnect((String) null, response);
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
    default void disconnect(String reason, Callback<Integer> response) {
        disconnect(new UUID[]{ getUniqueId() }, reason, response);
    }

    /**
     * Disconnects this player from the network
     *
     * @param players Players to select
     */
    static void disconnect(UUID... players) {
        disconnect(players, (String) null);
    }

    /**
     * Disconnects this player from the network
     *
     * @param players Players to select
     * @param response Success status
     */
    static void disconnect(UUID[] players, Callback<Integer> response) {
        disconnect(players, null, response);
    }

    /**
     * Disconnects this player from the network
     *
     * @param players Players to select
     * @param reason Disconnect Reason
     */
    static void disconnect(UUID[] players, String reason) {
        disconnect(players, reason, i -> {});
    }

    /**
     * Disconnects this player from the network
     *
     * @param players Players to select
     * @param reason Disconnect Reason
     * @param response Success status
     */
    static void disconnect(UUID[] players, String reason, Callback<Integer> response) {
        instance.disconnect(players, reason, response);
    }
}

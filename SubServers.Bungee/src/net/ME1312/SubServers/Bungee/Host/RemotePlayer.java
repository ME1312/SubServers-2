package net.ME1312.SubServers.Bungee.Host;

import net.ME1312.Galaxi.Library.Container.ContainedPair;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Server.SubDataClient;
import net.ME1312.SubData.Server.SubDataSerializable;
import net.ME1312.SubServers.Bungee.Library.Compatibility.RPSI;
import net.ME1312.SubServers.Bungee.Network.Packet.PacketDisconnectPlayer;
import net.ME1312.SubServers.Bungee.Network.Packet.PacketMessagePlayer;
import net.ME1312.SubServers.Bungee.Network.Packet.PacketTransferPlayer;
import net.ME1312.SubServers.Bungee.SubAPI;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.UUID;
import java.util.function.IntConsumer;

/**
 * Remote Player Class
 */
public class RemotePlayer implements net.ME1312.SubServers.Bungee.Library.Compatibility.RemotePlayer, SubDataSerializable {
    private ProxiedPlayer local;
    private UUID id;
    private String name;
    private InetSocketAddress ip;
    private Proxy proxy;
    private Server server;

    /**
     * Translate a Local Player to a Remote Player
     *
     * @param player Local Player
     */
    public RemotePlayer(ProxiedPlayer player) {
        this(player, player.getServer().getInfo());
    }


    /**
     * Translate a Local Player to a Remote Player
     *
     * @param player Local Player
     * @param server Server the player is on
     */
    public RemotePlayer(ProxiedPlayer player, ServerInfo server) {
        Util.nullpo(player);
        this.local = player;
        this.id = player.getUniqueId();
        this.server = (server instanceof Server)? (Server) server : null;
    }

    /**
     * Search for a Remote Player using their ID
     *
     * @param name Player Name
     * @param id Player UUID
     * @param proxy Proxy the player is on
     * @param server Server the player is on
     * @param ip Player IP Address
     */
    public RemotePlayer(String name, UUID id, Proxy proxy, ServerInfo server, InetSocketAddress ip) {
        Util.nullpo(name, id, proxy, ip);
        this.id = id;
        this.name = name;
        this.ip = ip;
        this.proxy = proxy;
        this.server = (server instanceof Server)? (Server) server : null;
    }

    @Override
    public ProxiedPlayer get() {
        return local;
    }

    private static ProxiedPlayer get(UUID player) {
        return ProxyServer.getInstance().getPlayer(player);
    }

    @Override
    public UUID getUniqueId() {
        if (local != null) {
            return local.getUniqueId();
        } else return id;
    }

    @Override
    public String getName() {
        if (local != null) {
            return local.getName();
        } else return name;
    }

    @SuppressWarnings("deprecation")
    @Override
    public InetSocketAddress getAddress() {
        if (local != null) {
            return local.getAddress();
        } else return ip;
    }

    /**
     * Gets the proxy this player is connected to.
     *
     * @return the proxy this player is connected to
     */
    public Proxy getProxy() {
        if (local != null) {
            return SubAPI.getInstance().getMasterProxy();
        } else return proxy;
    }

    @Override
    public String getProxyName() {
        Proxy proxy = getProxy();
        return (proxy == null)? null : proxy.getName();
    }

    private SubDataClient getProxyConnection() {
        Proxy proxy = getProxy();
        return (proxy == null)? null : (SubDataClient) proxy.getSubData()[0];
    }

    @Override
    public Server getServer() {
        return server;
    }

    @Override
    public String getServerName() {
        Server server = getServer();
        return (server == null)? null : server.getName();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof RemotePlayer && getUniqueId().equals(((RemotePlayer) obj).getUniqueId());
    }

    @Override
    public ObjectMap<String> forSubData() {
        ObjectMap<String> pinfo = new ObjectMap<String>();
        pinfo.set("name", getName());
        pinfo.set("id", getUniqueId());
        pinfo.set("address", getAddress().getAddress().getHostAddress() + ':' + getAddress().getPort());
        if (getServer() != null) pinfo.set("server", getServer().getName());
        if (getProxy() != null) pinfo.set("proxy", getProxy().getName());
        return pinfo;
    }

    static {
        // These overrides provide for the static methods in BungeeCommon
        new RPSI() {
            @Override
            protected void sendMessage(UUID[] players, String[] messages, IntConsumer response) {
                StackTraceElement[] origin = new Throwable().getStackTrace();
                PacketMessagePlayer.run(Arrays.asList(players), new ContainedPair<>(messages, null), null, i -> {
                    try {
                        response.accept(i);
                    } catch (Throwable e) {
                        Throwable ew = new InvocationTargetException(e);
                        ew.setStackTrace(origin);
                        ew.printStackTrace();
                    }
                });
            }

            @Override
            protected void sendMessage(UUID[] players, BaseComponent[][] messages, IntConsumer response) {
                StackTraceElement[] origin = new Throwable().getStackTrace();
                PacketMessagePlayer.run(Arrays.asList(players), new ContainedPair<>(null, messages), null, i -> {
                    try {
                        response.accept(i);
                    } catch (Throwable e) {
                        Throwable ew = new InvocationTargetException(e);
                        ew.setStackTrace(origin);
                        ew.printStackTrace();
                    }
                });
            }

            @Override
            protected void transfer(UUID[] players, String server, IntConsumer response) {
                StackTraceElement[] origin = new Throwable().getStackTrace();
                PacketTransferPlayer.run(Arrays.asList(players), server, i -> {
                    try {
                        response.accept(i);
                    } catch (Throwable e) {
                        Throwable ew = new InvocationTargetException(e);
                        ew.setStackTrace(origin);
                        ew.printStackTrace();
                    }
                });
            }

            @Override
            protected void disconnect(UUID[] players, String reason, IntConsumer response) {
                StackTraceElement[] origin = new Throwable().getStackTrace();
                PacketDisconnectPlayer.run(Arrays.asList(players), reason, i -> {
                    try {
                        response.accept(i);
                    } catch (Throwable e) {
                        Throwable ew = new InvocationTargetException(e);
                        ew.setStackTrace(origin);
                        ew.printStackTrace();
                    }
                });
            }
        };
    }

    // The following methods all redirect to their BungeeCommon counterparts
    public static void broadcastMessage(String... messages) {
        net.ME1312.SubServers.Bungee.Library.Compatibility.RemotePlayer.broadcastMessage(messages);
    }

    public static void broadcastMessage(String message, IntConsumer response) {
        net.ME1312.SubServers.Bungee.Library.Compatibility.RemotePlayer.broadcastMessage(message, response);
    }

    public static void broadcastMessage(String[] messages, IntConsumer response) {
        net.ME1312.SubServers.Bungee.Library.Compatibility.RemotePlayer.broadcastMessage(messages, response);
    }

    public static void sendMessage(UUID[] players, String... messages) {
        net.ME1312.SubServers.Bungee.Library.Compatibility.RemotePlayer.sendMessage(players, messages);
    }

    public static void sendMessage(UUID[] players, String message, IntConsumer response) {
        net.ME1312.SubServers.Bungee.Library.Compatibility.RemotePlayer.sendMessage(players, message, response);
    }

    public static void sendMessage(UUID[] players, String[] messages, IntConsumer response) {
        net.ME1312.SubServers.Bungee.Library.Compatibility.RemotePlayer.sendMessage(players, messages, response);
    }

    public static void broadcastMessage(BaseComponent... message) {
        net.ME1312.SubServers.Bungee.Library.Compatibility.RemotePlayer.broadcastMessage(message);
    }

    public static void broadcastMessage(BaseComponent message, IntConsumer response) {
        net.ME1312.SubServers.Bungee.Library.Compatibility.RemotePlayer.broadcastMessage(message, response);
    }

    public static void broadcastMessage(BaseComponent[] message, IntConsumer response) {
        net.ME1312.SubServers.Bungee.Library.Compatibility.RemotePlayer.broadcastMessage(message, response);
    }

    public static void broadcastMessage(BaseComponent[]... messages) {
        net.ME1312.SubServers.Bungee.Library.Compatibility.RemotePlayer.broadcastMessage(messages);
    }

    public static void broadcastMessage(BaseComponent[][] messages, IntConsumer response) {
        net.ME1312.SubServers.Bungee.Library.Compatibility.RemotePlayer.broadcastMessage(messages, response);
    }

    public static void sendMessage(UUID[] players, BaseComponent... message) {
        net.ME1312.SubServers.Bungee.Library.Compatibility.RemotePlayer.sendMessage(players, message);
    }

    public static void sendMessage(UUID[] players, BaseComponent message, IntConsumer response) {
        net.ME1312.SubServers.Bungee.Library.Compatibility.RemotePlayer.sendMessage(players, message, response);
    }

    public static void sendMessage(UUID[] players, BaseComponent[] message, IntConsumer response) {
        net.ME1312.SubServers.Bungee.Library.Compatibility.RemotePlayer.sendMessage(players, message, response);
    }

    public static void sendMessage(UUID[] players, BaseComponent[]... messages) {
        net.ME1312.SubServers.Bungee.Library.Compatibility.RemotePlayer.sendMessage(players, messages);
    }

    public static void sendMessage(UUID[] players, BaseComponent[][] messages, IntConsumer response) {
        net.ME1312.SubServers.Bungee.Library.Compatibility.RemotePlayer.sendMessage(players, messages, response);
    }

    public static void transfer(UUID[] players, String server) {
        net.ME1312.SubServers.Bungee.Library.Compatibility.RemotePlayer.transfer(players, server);
    }

    public static void transfer(UUID[] players, String server, IntConsumer response) {
        net.ME1312.SubServers.Bungee.Library.Compatibility.RemotePlayer.transfer(players, server, response);
    }

    public static void transfer(UUID[] players, ServerInfo server) {
        net.ME1312.SubServers.Bungee.Library.Compatibility.RemotePlayer.transfer(players, server);
    }

    public static void transfer(UUID[] players, ServerInfo server, IntConsumer response) {
        net.ME1312.SubServers.Bungee.Library.Compatibility.RemotePlayer.transfer(players, server, response);
    }

    public static void disconnect(UUID... players) {
        net.ME1312.SubServers.Bungee.Library.Compatibility.RemotePlayer.disconnect(players);
    }

    public static void disconnect(UUID[] players, IntConsumer response) {
        net.ME1312.SubServers.Bungee.Library.Compatibility.RemotePlayer.disconnect(players, response);
    }

    public static void disconnect(UUID[] players, String reason) {
        net.ME1312.SubServers.Bungee.Library.Compatibility.RemotePlayer.disconnect(players, reason);
    }

    public static void disconnect(UUID[] players, String reason, IntConsumer response) {
        net.ME1312.SubServers.Bungee.Library.Compatibility.RemotePlayer.disconnect(players, reason, response);
    }
}

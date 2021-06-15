package net.ME1312.SubServers.Bungee.Host;

import net.ME1312.Galaxi.Library.Callback.Callback;
import net.ME1312.Galaxi.Library.Container.ContainedPair;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Server.SubDataClient;
import net.ME1312.SubData.Server.SubDataSerializable;
import net.ME1312.SubServers.Bungee.Library.Compatibility.RPSI;
import net.ME1312.SubServers.Bungee.Network.Packet.*;
import net.ME1312.SubServers.Bungee.SubAPI;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.chat.ComponentSerializer;

import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

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
        if (Util.isNull(player)) throw new NullPointerException();
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
        if (Util.isNull(name, id, proxy, ip)) throw new NullPointerException();
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

    static {
        // These overrides provide for the static methods in BungeeCommon
        new RPSI() {
            @Override
            protected void sendMessage(UUID[] players, String[] messages, Callback<Integer> response) {
                PacketMessagePlayer.run(Arrays.asList(players), new ContainedPair<>(messages, null), null, response);
            }

            @Override
            protected void sendMessage(UUID[] players, BaseComponent[] messages, Callback<Integer> response) {
                PacketMessagePlayer.run(Arrays.asList(players), new ContainedPair<>(null, messages), null, response);
            }

            @Override
            protected void transfer(UUID[] players, String server, Callback<Integer> response) {
                PacketTransferPlayer.run(Arrays.asList(players), server, response);
            }

            @Override
            protected void disconnect(UUID[] players, String reason, Callback<Integer> response) {
                PacketDisconnectPlayer.run(Arrays.asList(players), reason, response);
            }
        };
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
}

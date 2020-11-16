package net.ME1312.SubServers.Bungee.Host;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Server.SubDataSerializable;
import net.ME1312.SubServers.Bungee.SubAPI;

import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.net.InetSocketAddress;
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
        if (Util.isNull(player)) throw new NullPointerException();
        this.local = player;
        this.id = player.getUniqueId();
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
    public RemotePlayer(String name, UUID id, Proxy proxy, Server server, InetSocketAddress ip) {
        if (Util.isNull(name, id, proxy, ip)) throw new NullPointerException();
        this.id = id;
        this.name = name;
        this.ip = ip;
        this.proxy = proxy;
        this.server = server;
    }

    /**
     * Get Local Player
     *
     * @return Local Player (or null when not local)
     */
    public ProxiedPlayer get() {
        return local;
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

    @Override
    public Server getServer() {
        if (local != null) {
            return (Server) local.getServer().getInfo();
        } else return server;
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
}

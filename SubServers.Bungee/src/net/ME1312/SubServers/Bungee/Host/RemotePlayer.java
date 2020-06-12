package net.ME1312.SubServers.Bungee.Host;

import net.ME1312.Galaxi.Library.Container.NamedContainer;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Server.SubDataSerializable;
import net.ME1312.SubServers.Bungee.SubAPI;
import net.ME1312.SubServers.Bungee.SubProxy;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.net.InetAddress;
import java.util.UUID;

/**
 * Remote Player Class
 */
public class RemotePlayer implements SubDataSerializable {
    private ProxiedPlayer local;
    private UUID id;
    private String name;
    private InetAddress ip;
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
     * @param player Remote Player ID
     */
    public RemotePlayer(UUID player) {
        if (Util.isNull(player)) throw new NullPointerException();

        id = player;
        refresh();
    }

    /**
     * Download a new copy of the data
     */
    @SuppressWarnings({"deprecation", "unchecked"})
    public void refresh() {
        SubProxy plugin = SubAPI.getInstance().getInternals();
        UUID player = id;

        this.local = plugin.getPlayer(player);
        if (local == null) {
            if (plugin.redis != null && Util.getDespiteException(() -> (boolean) plugin.redis("isPlayerOnline", new NamedContainer<>(UUID.class, player)), false)) {
                server = Util.getDespiteException(() -> (Server) plugin.redis("getServerFor", new NamedContainer<>(UUID.class, player)), null);
                proxy = Util.getDespiteException(() -> plugin.api.getProxy((String) plugin.redis("getProxy", new NamedContainer<>(UUID.class, player))), null);
                ip = Util.getDespiteException(() -> (InetAddress) plugin.redis("getPlayerIp", new NamedContainer<>(UUID.class, player)), null);
                name = Util.getDespiteException(() -> (String) plugin.redis("getNameFromUuid", new NamedContainer<>(UUID.class, player), new NamedContainer<>(boolean.class, false)), null);
            }

            if (name == null) throw new IllegalStateException("Player " + id.toString() + " not found!");
        }
    }

    /**
     * Get Local Player
     *
     * @return Local Player (or null when not local)
     */
    public ProxiedPlayer get() {
        return local;
    }

    /**
     * Get this connection's UUID, if set.
     *
     * @return the UUID
     */
    public UUID getUniqueId() {
        if (local != null) {
            return local.getUniqueId();
        } else return id;
    }

    /**
     * Get the unique name of this player.
     *
     * @return the players username
     */
    public String getName() {
        if (local != null) {
            return local.getName();
        } else return name;
    }

    /**
     * Gets the remote address of this connection.
     *
     * @return the remote address
     */
    @SuppressWarnings("deprecation")
    public InetAddress getAddress() {
        if (local != null) {
            return local.getAddress().getAddress();
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

    /**
     * Gets the server this player is connected to.
     *
     * @return the server this player is connected to
     */
    public Server getServer() {
        if (local != null) {
            return (Server) local.getServer().getInfo();
        } else return server;
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
        if (getServer() != null) pinfo.set("server", getServer().getName());
        if (getProxy() != null) pinfo.set("proxy", getProxy().getName());
        return pinfo;
    }
}

package net.ME1312.SubServers.Sync.Server;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.SubServers.Client.Common.Network.API.RemotePlayer;
import net.ME1312.SubServers.Sync.SubAPI;
import net.md_5.bungee.api.connection.ProxiedPlayer;

/**
 * Cached RemotePlayer Data Class
 */
public class CachedPlayer extends RemotePlayer implements net.ME1312.SubServers.Bungee.Library.Compatibility.RemotePlayer {

    /**
     * Convert a Local Player to a Cached Remote Player
     *
     * @param player Local Player
     * @return Raw representation of the Remote Player
     */
    public static ObjectMap<String> translate(ProxiedPlayer player) {
        ObjectMap<String> raw = new ObjectMap<String>();
        raw = new ObjectMap<String>();
        raw.set("name", player.getName());
        raw.set("id", player.getUniqueId());
        raw.set("address", player.getAddress().getAddress().getHostAddress() + ':' + player.getAddress().getPort());
        if (player.getServer() != null) raw.set("server", player.getServer().getInfo().getName());
        if (SubAPI.getInstance().getName() != null) raw.set("proxy", SubAPI.getInstance().getName());
        return raw;
    }

    /**
     * Convert a Local Player to a Cached Remote Player
     *
     * @param player Local Player
     */
    public CachedPlayer(ProxiedPlayer player) {
        this(translate(player));
    }

    /**
     * Cache a Remote Player
     *
     * @param player Remote Player
     */
    public CachedPlayer(RemotePlayer player) {
        this(raw(player));
    }

    /**
     * Create a Cached Remote Player
     *
     * @param raw Raw representation of the Remote Player
     */
    public CachedPlayer(ObjectMap<String> raw) {
        super(raw);
    }


    @Override
    public String getProxyName() {
        return getProxy();
    }

    @Override
    public String getServerName() {
        return getServer();
    }
}

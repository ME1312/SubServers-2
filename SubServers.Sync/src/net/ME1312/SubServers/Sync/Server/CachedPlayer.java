package net.ME1312.SubServers.Sync.Server;

import net.ME1312.Galaxi.Library.Callback.Callback;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.SubServers.Client.Common.Network.API.RemotePlayer;
import net.ME1312.SubServers.Client.Common.Network.API.Server;
import net.ME1312.SubServers.Sync.SubAPI;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.chat.ComponentSerializer;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

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
    public ProxiedPlayer get() {
        return ProxyServer.getInstance().getPlayer(getUniqueId());
    }

    @Override
    public ServerInfo getServer() {
        String name = getServerName();
        return (name == null)? null : ProxyServer.getInstance().getServerInfo(name);
    }

    // These overrides prevent sending unnecessary packets

    @Override
    public void sendMessage(String[] messages, Callback<Integer> response) {
        ProxiedPlayer local = get();
        if (local != null) {
            local.sendMessages(messages);
            response.run(0);
        } else {
            super.sendMessage(messages, response);
        }
    }

    @Override
    public void sendMessage(BaseComponent[] messages, Callback<Integer> response) {
        ProxiedPlayer local = get();
        if (local != null) {
            local.sendMessage(messages);
            response.run(0);
        } else {
            super.sendRawMessage(new String[]{ComponentSerializer.toString(messages)}, response);
        }
    }

    @Override
    public void sendRawMessage(String[] messages, Callback<Integer> response) {
        ProxiedPlayer local = get();
        if (local != null) {
            LinkedList<BaseComponent> components = new LinkedList<BaseComponent>();
            for (String message : messages) components.addAll(Arrays.asList(ComponentSerializer.parse(message)));
            local.sendMessage(components.toArray(new BaseComponent[0]));
            response.run(0);
        } else {
            super.sendRawMessage(messages, response);
        }
    }

    @Override
    public void transfer(String server, Callback<Integer> response) {
        ProxiedPlayer local = get();
        if (local != null) {
            ServerImpl info = SubAPI.getInstance().getInternals().servers.get(server.toLowerCase());
            if (info != null) {
                local.connect(info);
                response.run(0);
            } else response.run(1);
        } else {
            super.transfer(server, response);
        }
    }

    @Override
    public void transfer(ServerInfo server, Callback<Integer> response) {
        ProxiedPlayer local = get();
        if (local != null) {
            local.connect(server);
            response.run(0);
        } else {
            super.transfer(server.getName(), response);
        }
    }

    @Override
    public void disconnect(String reason, Callback<Integer> response) {
        ProxiedPlayer local = get();
        if (local != null) {
            if (reason != null) {
                local.disconnect(reason);
            } else local.disconnect();
            response.run(0);
        } else {
            super.disconnect(reason, response);
        }
    }
}

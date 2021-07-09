package net.ME1312.SubServers.Velocity.Server;

import net.ME1312.Galaxi.Library.Callback.Callback;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.SubData.Client.DataClient;
import net.ME1312.SubData.Client.SubDataClient;
import net.ME1312.SubServers.Client.Common.Network.API.RemotePlayer;
import net.ME1312.SubServers.Velocity.ExProxy;
import net.ME1312.SubServers.Velocity.Library.Compatibility.ChatColor;
import net.ME1312.SubServers.Velocity.SubAPI;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

/**
 * Cached RemotePlayer Data Class
 */
public class CachedPlayer extends RemotePlayer {

    /**
     * Convert a Local Player to a Cached Remote Player
     *
     * @param player Local Player
     * @return Raw representation of the Remote Player
     */
    public static ObjectMap<String> translate(Player player) {
        ObjectMap<String> raw = new ObjectMap<String>();
        raw = new ObjectMap<String>();
        raw.set("name", player.getGameProfile().getName());
        raw.set("id", player.getUniqueId());
        raw.set("address", player.getRemoteAddress().getAddress().getHostAddress() + ':' + player.getRemoteAddress().getPort());
        if (player.getCurrentServer().isPresent()) raw.set("server", player.getCurrentServer().get().getServerInfo().getName());
        if (SubAPI.getInstance().getName() != null) raw.set("proxy", SubAPI.getInstance().getName());
        return raw;
    }

    /**
     * Convert a Local Player to a Cached Remote Player
     *
     * @param player Local Player
     */
    public CachedPlayer(Player player) {
        this(translate(player));
    }

    /**
     * Create a Cached Remote Player
     *
     * @param raw Raw representation of the Remote Player
     */
    public CachedPlayer(ObjectMap<String> raw) {
        this(null, raw);
    }

    /**
     * Create a Cached Remote Player
     *
     * @param client SubData connection
     * @param raw Raw representation of the Remote Player
     */
    CachedPlayer(DataClient client, ObjectMap<String> raw) {
        super(client, raw);
    }

    /**
     * Get Local Player
     *
     * @return Local Player (or null when not local)
     */
    public Player get() {
        return get(getUniqueId());
    }

    private static Player get(UUID player) {
        return ExProxy.getInstance().getPlayer(player).orElse(null);
    }

    /**
     * Gets the server this player is connected to.
     *
     * @return the server this player is connected to
     */
    public ServerInfo getServer() {
        String name = getServerName();
        if (name == null) {
            return null;
        } else {
            Optional<RegisteredServer> server = ExProxy.getInstance().getServer(name);
            return server.map(RegisteredServer::getServerInfo).orElse(null);
        }
    }

    static {
        // These overrides prevent sending unnecessary packets in ClientCommon
        instance = new StaticImpl() {
            @Override
            protected RemotePlayer construct(DataClient client, ObjectMap<String> raw) {
                return new CachedPlayer(client, raw);
            }

            @Override
            protected void sendMessage(SubDataClient client, UUID[] players, String[] messages, Callback<Integer> response) {
                if (players != null && players.length > 0) {
                    ArrayList<UUID> ids = new ArrayList<UUID>();
                    for (UUID id : players) {
                        Player local = get(id);
                        if (local != null) {
                            for (String s : messages) {
                                local.sendMessage(Component.text(s));
                            }
                        } else {
                            ids.add(id);
                        }
                    }

                    if (ids.size() == 0) {
                        response.run(0);
                    } else {
                        super.sendMessage(client, ids.toArray(new UUID[0]), messages, response);
                    }
                } else {
                    super.sendMessage(client, players, messages, response);
                }
            }

            @Override
            protected void sendRawMessage(SubDataClient client, UUID[] players, String[] messages, Callback<Integer> response) {
                if (players != null && players.length > 0) {
                    ArrayList<UUID> ids = new ArrayList<UUID>();
                    Component[] components = null;
                    for (UUID id : players) {
                        Player local = get(id);
                        if (local != null) {
                            if (components == null) {
                                components = new Component[messages.length];
                                for (int i = 0; i < components.length; ++i) components[i] = GsonComponentSerializer.gson().deserialize(messages[i]);
                            }
                            for (Component c : components) {
                                local.sendMessage(c);
                            }
                        } else {
                            ids.add(id);
                        }
                    }

                    if (ids.size() == 0) {
                        response.run(0);
                    } else {
                        super.sendRawMessage(client, ids.toArray(new UUID[0]), messages, response);
                    }
                } else {
                    super.sendRawMessage(client, players, messages, response);
                }
            }

            @Override
            protected void transfer(SubDataClient client, UUID[] players, String server, Callback<Integer> response) {
                ArrayList<UUID> ids = new ArrayList<UUID>();
                Optional<RegisteredServer> rs = ExProxy.getInstance().getServer(server.toLowerCase());
                int failures = 0;
                for (UUID id : players) {
                    Player local = get(id);
                    if (local != null) {
                        if (rs.isPresent()) {
                            local.createConnectionRequest(rs.get()).fireAndForget();
                        } else ++failures;
                    } else {
                        ids.add(id);
                    }
                }

                if (ids.size() == 0) {
                    response.run(failures);
                } else {
                    final int ff = failures;
                    super.transfer(client, ids.toArray(new UUID[0]), server, i -> response.run(i + ff));
                }
            }

            @Override
            protected void disconnect(SubDataClient client, UUID[] players, String reason, Callback<Integer> response) {
                Component message = (reason == null)? Component.text().build() : ChatColor.convertColor(reason);
                ArrayList<UUID> ids = new ArrayList<UUID>();
                for (UUID id : players) {
                    Player local = get(id);
                    if (local != null) {
                        local.disconnect(message);
                    } else {
                        ids.add(id);
                    }
                }

                if (ids.size() == 0) {
                    response.run(0);
                } else {
                    super.disconnect(client, ids.toArray(new UUID[0]), reason, response);
                }
            }
        };
    }
}

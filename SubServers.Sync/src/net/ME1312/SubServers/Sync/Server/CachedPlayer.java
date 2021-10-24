package net.ME1312.SubServers.Sync.Server;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.SubData.Client.DataClient;
import net.ME1312.SubData.Client.SubDataClient;
import net.ME1312.SubServers.Bungee.Library.Compatibility.RPSI;
import net.ME1312.SubServers.Client.Common.Network.API.RemotePlayer;
import net.ME1312.SubServers.Sync.SubAPI;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.chat.ComponentSerializer;

import java.util.ArrayList;
import java.util.UUID;
import java.util.function.IntConsumer;

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

    @Override
    public ProxiedPlayer get() {
        return get(getUniqueId());
    }

    private static ProxiedPlayer get(UUID player) {
        return ProxyServer.getInstance().getPlayer(player);
    }

    @Override
    public ServerInfo getServer() {
        String name = getServerName();
        return (name == null)? null : ProxyServer.getInstance().getServerInfo(name);
    }

    static {
        // These overrides provide for the static methods in BungeeCommon
        new RPSI() {
            @Override
            protected void sendMessage(UUID[] players, String[] messages, IntConsumer response) {
                RemotePlayer.sendMessage(players, messages, response);
            }

            @Override
            protected void sendMessage(UUID[] players, BaseComponent[][] messages, IntConsumer response) {
                String[] raw = new String[messages.length];
                for (int i = 0; i < raw.length; ++i) raw[i] = ComponentSerializer.toString(messages[i]);
                RemotePlayer.sendRawMessage(players, raw, response);
            }

            @Override
            protected void transfer(UUID[] players, String server, IntConsumer response) {
                RemotePlayer.transfer(players, server, response);
            }

            @Override
            protected void disconnect(UUID[] players, String reason, IntConsumer response) {
                RemotePlayer.disconnect(players, reason, response);
            }
        };
        // These overrides prevent sending unnecessary packets in ClientCommon
        instance = new StaticImpl() {
            @Override
            protected RemotePlayer construct(DataClient client, ObjectMap<String> raw) {
                return new CachedPlayer(client, raw);
            }

            @Override
            protected void sendMessage(SubDataClient client, UUID[] players, String[] messages, IntConsumer response) {
                if (players != null && players.length > 0) {
                    ArrayList<UUID> ids = new ArrayList<UUID>();
                    for (UUID id : players) {
                        ProxiedPlayer local = get(id);
                        if (local != null) {
                            local.sendMessages(messages);
                        } else {
                            ids.add(id);
                        }
                    }

                    if (ids.size() == 0) {
                        response.accept(0);
                    } else {
                        super.sendMessage(client, ids.toArray(new UUID[0]), messages, response);
                    }
                } else {
                    super.sendMessage(client, players, messages, response);
                }
            }

            @Override
            protected void sendRawMessage(SubDataClient client, UUID[] players, String[] messages, IntConsumer response) {
                if (players != null && players.length > 0) {
                    ArrayList<UUID> ids = new ArrayList<UUID>();
                    BaseComponent[][] components = null;
                    for (UUID id : players) {
                        ProxiedPlayer local = get(id);
                        if (local != null) {
                            if (components == null) {
                                components = new BaseComponent[messages.length][];
                                for (int i = 0; i < components.length; ++i) components[i] = ComponentSerializer.parse(messages[i]);
                            }
                            for (BaseComponent[] c : components) {
                                local.sendMessage(c);
                            }
                        } else {
                            ids.add(id);
                        }
                    }

                    if (ids.size() == 0) {
                        response.accept(0);
                    } else {
                        super.sendRawMessage(client, ids.toArray(new UUID[0]), messages, response);
                    }
                } else {
                    super.sendRawMessage(client, players, messages, response);
                }
            }

            @Override
            protected void transfer(SubDataClient client, UUID[] players, String server, IntConsumer response) {
                ArrayList<UUID> ids = new ArrayList<UUID>();
                ServerImpl info = SubAPI.getInstance().getInternals().servers.get(server.toLowerCase());
                int failures = 0;
                for (UUID id : players) {
                    ProxiedPlayer local = get(id);
                    if (local != null) {
                        if (info != null) {
                            local.connect(info);
                        } else ++failures;
                    } else {
                        ids.add(id);
                    }
                }

                if (ids.size() == 0) {
                    response.accept(failures);
                } else {
                    final int ff = failures;
                    super.transfer(client, ids.toArray(new UUID[0]), server, i -> response.accept(i + ff));
                }
            }

            @Override
            protected void disconnect(SubDataClient client, UUID[] players, String reason, IntConsumer response) {
                ArrayList<UUID> ids = new ArrayList<UUID>();
                for (UUID id : players) {
                    ProxiedPlayer local = get(id);
                    if (local != null) {
                        if (reason != null) {
                            local.disconnect(reason);
                        } else local.disconnect();
                    } else {
                        ids.add(id);
                    }
                }

                if (ids.size() == 0) {
                    response.accept(0);
                } else {
                    super.disconnect(client, ids.toArray(new UUID[0]), reason, response);
                }
            }
        };
    }

    // The following methods all redirect to their BungeeCommon counterparts
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

    public static void transfer(UUID[] players, ServerInfo server) {
        net.ME1312.SubServers.Bungee.Library.Compatibility.RemotePlayer.transfer(players, server);
    }

    public static void transfer(UUID[] players, ServerInfo server, IntConsumer response) {
        net.ME1312.SubServers.Bungee.Library.Compatibility.RemotePlayer.transfer(players, server, response);
    }

}

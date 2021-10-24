package net.ME1312.SubServers.Client.Common.Network.API;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Client.DataClient;
import net.ME1312.SubData.Client.SubDataClient;
import net.ME1312.SubServers.Client.Common.ClientAPI;
import net.ME1312.SubServers.Client.Common.Network.Packet.PacketDisconnectPlayer;
import net.ME1312.SubServers.Client.Common.Network.Packet.PacketDownloadPlayerInfo;
import net.ME1312.SubServers.Client.Common.Network.Packet.PacketMessagePlayer;
import net.ME1312.SubServers.Client.Common.Network.Packet.PacketTransferPlayer;

import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.util.Calendar;
import java.util.Collections;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * Simplified RemotePlayer Data Class
 */
public class RemotePlayer {
    protected static StaticImpl instance = new StaticImpl();
    ObjectMap<String> raw;
    private Proxy proxy = null;
    private Server server = null;
    DataClient client;
    long timestamp;

    /**
     * Create an API representation of a Remote Player
     *
     * @param raw Raw representation of the Remote Player
     */
    public RemotePlayer(ObjectMap<String> raw) {
        this(null, raw);
    }

    /**
     * Create an API representation of a Remote Player
     *
     * @param client SubData connection
     * @param raw Raw representation of the Remote Player
     */
    protected RemotePlayer(DataClient client, ObjectMap<String> raw) {
        this.client = client;
        load(raw);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof RemotePlayer && getUniqueId().equals(((RemotePlayer) obj).getUniqueId());
    }

    void load(ObjectMap<String>  raw) {
        this.raw = raw;
        this.proxy = null;
        this.server = null;
        this.timestamp = Calendar.getInstance().getTime().getTime();
    }

    private SubDataClient client() {
        return SimplifiedData.client(client);
    }

    /**
     * Download a new copy of the data from SubData
     */
    public void refresh() {
        UUID id = getUniqueId();
        client().sendPacket(new PacketDownloadPlayerInfo(Collections.singletonList(id), data -> load(data.getMap(id.toString()))));
    }

    /**
     * Get the UUID of this player.
     *
     * @return the UUID
     */
    public UUID getUniqueId() {
        return raw.getUUID("id");
    }

    /**
     * Get the unique name of this player.
     *
     * @return the player's username
     */
    public String getName() {
        return raw.getRawString("name");
    }

    /**
     * Gets the remote address of this connection.
     *
     * @return the remote address
     */
    public InetSocketAddress getAddress() {
        return new InetSocketAddress(raw.getRawString("address").split(":")[0], Integer.parseInt(raw.getRawString("address").split(":")[1]));
    }

    /**
     * Gets the proxy this player is connected to.
     *
     * @return the proxy this player is connected to
     */
    public String getProxyName() {
        return raw.getRawString("proxy");
    }

    /**
     * Gets the proxy this player is connected to.
     *
     * @param callback  the proxy this player is connected to
     */
    public void getProxy(Consumer<Proxy> callback) {
        Util.nullpo(callback);
        StackTraceElement[] origin = new Exception().getStackTrace();
        Runnable run = () -> {
            try {
                callback.accept(proxy);
            } catch (Throwable e) {
                Throwable ew = new InvocationTargetException(e);
                ew.setStackTrace(origin);
                ew.printStackTrace();
            }
        };

        if (proxy == null || !proxy.getName().equalsIgnoreCase(raw.getRawString("proxy"))) {
            ClientAPI.getInstance().getProxy(raw.getRawString("proxy"), proxy -> {
                this.proxy = proxy;
                run.run();
            });
        } else {
            run.run();
        }
    }

    /**
     * Gets the server this player is connected to.
     *
     * @return the server this player is connected to
     */
    public String getServerName() {
        return raw.getRawString("server");
    }

    /**
     * Gets the server this player is connected to.
     *
     * @param callback  the server this player is connected to
     */
    public void getServer(Consumer<Server> callback) {
        Util.nullpo(callback);
        StackTraceElement[] origin = new Exception().getStackTrace();
        Runnable run = () -> {
            try {
                callback.accept(server);
            } catch (Throwable e) {
                Throwable ew = new InvocationTargetException(e);
                ew.setStackTrace(origin);
                ew.printStackTrace();
            }
        };

        if (server == null || !server.getName().equalsIgnoreCase(raw.getRawString("server"))) {
            ClientAPI.getInstance().getServer(raw.getRawString("server"), server -> {
                this.server = server;
                run.run();
            });
        } else {
            run.run();
        }
    }

    /**
     * Sends messages to all players
     *
     * @param messages Messages to send
     */
    public static void broadcastMessage(String... messages) {
        broadcastMessage(messages, i -> {});
    }

    /**
     * Sends a message to all players
     *
     * @param message Message to send
     * @param response Success Status
     */
    public static void broadcastMessage(String message, IntConsumer response) {
        broadcastMessage(new String[]{ message }, response);
    }

    /**
     * Sends messages to all players
     *
     * @param messages Messages to send
     * @param response Success Status
     */
    public static void broadcastMessage(String[] messages, IntConsumer response) {
        sendMessage(null, messages, response);
    }

    /**
     * Sends messages to this player
     *
     * @param messages Messages to send
     */
    public void sendMessage(String... messages) {
        sendMessage(messages, i -> {});
    }

    /**
     * Sends a message to this player
     *
     * @param message Message to send
     * @param response Success Status
     */
    public void sendMessage(String message, IntConsumer response) {
        sendMessage(new String[]{ message }, response);
    }

    /**
     * Sends messages to this player
     *
     * @param messages Messages to send
     * @param response Success Status
     */
    public void sendMessage(String[] messages, IntConsumer response) {
        instance.sendMessage(client(), new UUID[]{ getUniqueId() }, messages, response);
    }

    /**
     * Sends messages to these players
     *
     * @param players Players to select
     * @param messages Messages to send
     */
    public static void sendMessage(UUID[] players, String... messages) {
        sendMessage(players, messages, i -> {});
    }

    /**
     * Sends a message to these players
     *
     * @param players Players to select
     * @param message Message to send
     * @param response Success Status
     */
    public static void sendMessage(UUID[] players, String message, IntConsumer response) {
        sendMessage(players, new String[]{ message }, response);
    }

    /**
     * Sends messages to these players
     *
     * @param players Players to select
     * @param messages Messages to send
     * @param response Success Status
     */
    public static void sendMessage(UUID[] players, String[] messages, IntConsumer response) {
        instance.sendMessage(SimplifiedData.client(ClientAPI.getInstance().getSubDataNetwork()[0]), players, messages, response);
    }

    /**
     * Sends JSON format messages to all players
     *
     * @param messages Messages to send
     */
    public static void broadcastRawMessage(String... messages) {
        broadcastRawMessage(messages, i -> {});
    }

    /**
     * Sends a JSON format message to all players
     *
     * @param message Message to send
     * @param response Success Status
     */
    public static void broadcastRawMessage(String message, IntConsumer response) {
        broadcastRawMessage(new String[]{ message }, response);
    }

    /**
     * Sends JSON format messages to all players
     *
     * @param messages Messages to send
     * @param response Success Status
     */
    public static void broadcastRawMessage(String[] messages, IntConsumer response) {
        sendRawMessage(null, messages, response);
    }

    /**
     * Sends JSON format messages to this player
     *
     * @param messages Messages to send
     */
    public void sendRawMessage(String... messages) {
        sendRawMessage(messages, i -> {});
    }

    /**
     * Sends a JSON format message to this player
     *
     * @param message Message to send
     * @param response Success Status
     */
    public void sendRawMessage(String message, IntConsumer response) {
        sendRawMessage(new String[]{ message }, response);
    }

    /**
     * Sends JSON format messages to this player
     *
     * @param message Message to send
     * @param response Success Status
     */
    public void sendRawMessage(String[] message, IntConsumer response) {
        instance.sendRawMessage(client(), new UUID[]{ getUniqueId() }, message, response);
    }

    /**
     * Sends JSON format messages to these players
     *
     * @param players Players to select
     * @param messages Messages to send
     */
    public static void sendRawMessage(UUID[] players, String... messages) {
        sendRawMessage(players, messages, i -> {});
    }

    /**
     * Sends a JSON format message to these players
     *
     * @param players Players to select
     * @param message Message to send
     * @param response Success Status
     */
    public static void sendRawMessage(UUID[] players, String message, IntConsumer response) {
        sendRawMessage(players, new String[]{ message }, response);
    }

    /**
     * Sends JSON format messages to these players
     *
     * @param players Players to select
     * @param messages Messages to send
     * @param response Success Status
     */
    public static void sendRawMessage(UUID[] players, String[] messages, IntConsumer response) {
        instance.sendRawMessage(SimplifiedData.client(ClientAPI.getInstance().getSubDataNetwork()[0]), players, messages, response);
    }

    /**
     * Transfers this player to another server
     *
     * @param server Target server
     */
    public void transfer(String server) {
        transfer(server, i -> {});
    }

    /**
     * Transfers this player to another server
     *
     * @param server Target server
     * @param response Success status
     */
    public void transfer(String server, IntConsumer response) {
        instance.transfer(client(), new UUID[]{ getUniqueId() }, server, response);
    }

    /**
     * Transfers these players to another server
     *
     * @param players Players to select
     * @param server Target server
     */
    public static void transfer(UUID[] players, String server) {
        transfer(players, server, i -> {});
    }

    /**
     * Transfers these players to another server
     *
     * @param players Players to select
     * @param server Target server
     * @param response Success status
     */
    public static void transfer(UUID[] players, String server, IntConsumer response) {
        instance.transfer(SimplifiedData.client(ClientAPI.getInstance().getSubDataNetwork()[0]), players, server, response);
    }

    /**
     * Disconnects this player from the network
     */
    public void disconnect() {
        disconnect((String) null);
    }

    /**
     * Disconnects this player from the network
     *
     * @param response Success status
     */
    public void disconnect(IntConsumer response) {
        disconnect((String) null, response);
    }

    /**
     * Disconnects this player from the network
     *
     * @param reason Disconnect Reason
     */
    public void disconnect(String reason) {
        disconnect(reason, i -> {});
    }

    /**
     * Disconnects this player from the network
     *
     * @param reason Disconnect Reason
     * @param response Success status
     */
    public void disconnect(String reason, IntConsumer response) {
        instance.disconnect(client(), new UUID[]{ getUniqueId() }, reason, response);
    }

    /**
     * Disconnects these players from the network
     *
     * @param players Players to select
     */
    public static void disconnect(UUID... players) {
        disconnect(players, (String) null);
    }

    /**
     * Disconnects these players from the network
     *
     * @param players Players to select
     * @param response Success status
     */
    public static void disconnect(UUID[] players, IntConsumer response) {
        disconnect(players, null, response);
    }

    /**
     * Disconnects these players from the network
     *
     * @param players Players to select
     * @param reason Disconnect Reason
     */
    public static void disconnect(UUID[] players, String reason) {
        disconnect(players, reason, i -> {});
    }

    /**
     * Disconnects these players from the network
     *
     * @param players Players to select
     * @param reason Disconnect Reason
     * @param response Success status
     */
    public static void disconnect(UUID[] players, String reason, IntConsumer response) {
        instance.disconnect(SimplifiedData.client(ClientAPI.getInstance().getSubDataNetwork()[0]), players, reason, response);
    }

    /**
     * Get the Timestamp for when the data was last refreshed
     *
     * @return Data Timestamp
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Get the raw representation of the Server
     *
     * @return Raw Server
     */
    public ObjectMap<String> getRaw() {
        return raw.clone();
    }

    /**
     * RemotePlayer Static Implementation Class
     */
    protected static class StaticImpl {
        /**
         * Create an API representation of a Remote Player
         *
         * @param raw Raw representation of the Remote Player
         */
        protected final RemotePlayer construct(ObjectMap<String> raw) {
            return construct((DataClient) null, raw);
        }

        /**
         * Create an API representation of a Remote Player
         *
         * @param client SubData connection
         * @param raw Raw representation of the Remote Player
         */
        protected RemotePlayer construct(DataClient client, ObjectMap<String> raw) {
            return new RemotePlayer(client, raw);
        }

        /**
         * Create an API representation of a Remote Player
         *
         * @param server Server
         * @param raw Raw representation of the Remote Player
         */
        final RemotePlayer construct(Server server, ObjectMap<String> raw) {
            RemotePlayer player = construct(server.client, raw);
            player.server = server;
            return player;
        }

        /**
         * Create an API representation of a Remote Player
         *
         * @param proxy Proxy
         * @param raw Raw representation of the Remote Player
         */
        final RemotePlayer construct(Proxy proxy, ObjectMap<String> raw) {
            RemotePlayer player = construct(proxy.client, raw);
            player.proxy = proxy;
            return player;
        }

        /**
         * Sends messages to this player
         *
         * @param client SubData Connection
         * @param players Players to send to
         * @param messages Messages to send
         * @param response Success Status
         */
        protected void sendMessage(SubDataClient client, UUID[] players, String[] messages, IntConsumer response) {
            StackTraceElement[] origin = new Exception().getStackTrace();
            client.sendPacket(new PacketMessagePlayer(players, messages, null, data -> {
                try {
                    response.accept(data.getInt(0x0001));
                } catch (Throwable e) {
                    Throwable ew = new InvocationTargetException(e);
                    ew.setStackTrace(origin);
                    ew.printStackTrace();
                }
            }));
        }

        /**
         * Sends JSON format messages to this player
         *
         * @param client SubData Connection
         * @param players Players to send to
         * @param messages Messages to send
         * @param response Success Status
         */
        protected void sendRawMessage(SubDataClient client, UUID[] players, String[] messages, IntConsumer response) {
            StackTraceElement[] origin = new Exception().getStackTrace();
            client.sendPacket(new PacketMessagePlayer(players, null, messages, data -> {
                try {
                    response.accept(data.getInt(0x0001));
                } catch (Throwable e) {
                    Throwable ew = new InvocationTargetException(e);
                    ew.setStackTrace(origin);
                    ew.printStackTrace();
                }
            }));
        }

        /**
         * Transfers this player to another server
         *
         * @param client SubData Connection
         * @param players Players to send to
         * @param server Target server
         * @param response Success Status
         */
        protected void transfer(SubDataClient client, UUID[] players, String server, IntConsumer response) {
            StackTraceElement[] origin = new Exception().getStackTrace();
            client.sendPacket(new PacketTransferPlayer(players, server, data -> {
                try {
                    response.accept(data.getInt(0x0001));
                } catch (Throwable e) {
                    Throwable ew = new InvocationTargetException(e);
                    ew.setStackTrace(origin);
                    ew.printStackTrace();
                }
            }));
        }

        /**
         * Disconnects this player from the network
         *
         * @param client SubData Connection
         * @param players Players to send to
         * @param reason Disconnect Reason
         * @param response Success status
         */
        protected void disconnect(SubDataClient client, UUID[] players, String reason, IntConsumer response) {
            StackTraceElement[] origin = new Exception().getStackTrace();
            client.sendPacket(new PacketDisconnectPlayer(players, reason, data -> {
                try {
                    response.accept(data.getInt(0x0001));
                } catch (Throwable e) {
                    Throwable ew = new InvocationTargetException(e);
                    ew.setStackTrace(origin);
                    ew.printStackTrace();
                }
            }));
        }
    }
}

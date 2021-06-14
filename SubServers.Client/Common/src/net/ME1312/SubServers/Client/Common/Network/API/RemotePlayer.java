package net.ME1312.SubServers.Client.Common.Network.API;

import net.ME1312.Galaxi.Library.Callback.Callback;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Client.DataClient;
import net.ME1312.SubData.Client.SubDataClient;
import net.ME1312.SubServers.Client.Common.ClientAPI;
import net.ME1312.SubServers.Client.Common.Network.Packet.*;

import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.util.*;

/**
 * Simplified RemotePlayer Data Class
 */
public class RemotePlayer {
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
    RemotePlayer(DataClient client, ObjectMap<String> raw) {
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
    public void getProxy(Callback<Proxy> callback) {
        if (Util.isNull(callback)) throw new NullPointerException();
        StackTraceElement[] origin = new Exception().getStackTrace();
        Runnable run = () -> {
            try {
                callback.run(proxy);
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
    public void getServer(Callback<Server> callback) {
        if (Util.isNull(callback)) throw new NullPointerException();
        StackTraceElement[] origin = new Exception().getStackTrace();
        Runnable run = () -> {
            try {
                callback.run(server);
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
     * Sends messages to this player
     *
     * @param messages Messages to send
     */
    public void sendMessage(String... messages) {
        sendMessage(messages, i -> {});
    }

    /**
     * Sends messages to this player
     *
     * @param message Message to send
     * @param response Success Status
     */
    public void sendMessage(String message, Callback<Integer> response) {
        sendMessage(new String[]{ message }, response);
    }

    /**
     * Sends messages to this player
     *
     * @param messages Messages to send
     * @param response Success Status
     */
    public void sendMessage(String[] messages, Callback<Integer> response) {
        StackTraceElement[] origin = new Exception().getStackTrace();
        client().sendPacket(new PacketMessagePlayer(getUniqueId(), messages, null, data -> {
            try {
                response.run(data.getInt(0x0001));
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
     * @param messages Messages to send
     */
    public void sendRawMessage(String... messages) {
        sendRawMessage(messages, i -> {});
    }

    /**
     * Sends JSON format messages to this player
     *
     * @param message Message to send
     * @param response Success Status
     */
    public void sendRawMessage(String message, Callback<Integer> response) {
        sendRawMessage(new String[]{ message }, response);
    }

    /**
     * Sends JSON format messages to this player
     *
     * @param messages Messages to send
     * @param response Success Status
     */
    public void sendRawMessage(String[] messages, Callback<Integer> response) {
        StackTraceElement[] origin = new Exception().getStackTrace();
        client().sendPacket(new PacketMessagePlayer(getUniqueId(), null, messages, data -> {
            try {
                response.run(data.getInt(0x0001));
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
    public void transfer(String server, Callback<Integer> response) {
        StackTraceElement[] origin = new Exception().getStackTrace();
        client().sendPacket(new PacketTransferPlayer(getUniqueId(), server, data -> {
            try {
                response.run(data.getInt(0x0001));
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
     * @param server Target server
     */
    public void transfer(Server server) {
        transfer(server, i -> {});
    }

    /**
     * Transfers this player to another server
     *
     * @param server Target server
     * @param response Success status
     */
    public void transfer(Server server, Callback<Integer> response) {
        transfer(server.getName(), response);
    }

    /**
     * Disconnects this player from the network
     */
    public void disconnect() {
        disconnect(i -> {});
    }

    /**
     * Disconnects this player from the network
     *
     * @param response Success status
     */
    public void disconnect(Callback<Integer> response) {
        disconnect(null, response);
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
    public void disconnect(String reason, Callback<Integer> response) {
        StackTraceElement[] origin = new Exception().getStackTrace();
        client().sendPacket(new PacketDisconnectPlayer(getUniqueId(), reason, data -> {
            try {
                response.run(data.getInt(0x0001));
            } catch (Throwable e) {
                Throwable ew = new InvocationTargetException(e);
                ew.setStackTrace(origin);
                ew.printStackTrace();
            }
        }));
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
     * Get the raw representation of the Server
     *
     * @return Raw Server
     */
    protected static ObjectMap<String> raw(RemotePlayer player) {
        return player.raw;
    }
}

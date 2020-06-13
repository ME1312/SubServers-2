package net.ME1312.SubServers.Client.Bukkit.Network.API;

import net.ME1312.Galaxi.Library.Callback.Callback;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Client.SubDataClient;
import net.ME1312.SubServers.Client.Bukkit.Network.Packet.PacketDownloadPlayerInfo;
import net.ME1312.SubServers.Client.Bukkit.SubAPI;

import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.util.*;

public class RemotePlayer {
    ObjectMap<String> raw;
    private Proxy proxy = null;
    private Server server = null;
    long timestamp;

    /**
     * Create an API representation of a Remote Player
     *
     * @param raw Raw representation of the Remote Player
     */
    public RemotePlayer(ObjectMap<String> raw) {
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

    /**
     * Download a new copy of the data from SubData
     */
    public void refresh() {
        UUID id = getUniqueId();
        ((SubDataClient) SubAPI.getInstance().getSubDataNetwork()[0]).sendPacket(new PacketDownloadPlayerInfo(Collections.singletonList(id), data -> load(data.getMap(id.toString()))));
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
     * @return the players username
     */
    public String getName() {
        return raw.getRawString("name");
    }

    /**
     * Gets the remote address of this connection.
     *
     * @return the remote address
     */
    public InetAddress getAddress() {
        return Util.getDespiteException(() -> InetAddress.getByName(raw.getRawString("address")), null);
    }

    /**
     * Gets the proxy this player is connected to.
     *
     * @return the proxy this player is connected to
     */
    public String getProxy() {
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
            SubAPI.getInstance().getProxy(raw.getRawString("proxy"), proxy -> {
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
    public String getServer() {
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
            SubAPI.getInstance().getServer(raw.getRawString("server"), server -> {
                this.server = server;
                run.run();
            });
        } else {
            run.run();
        }
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
}

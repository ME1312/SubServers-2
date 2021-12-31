package net.ME1312.SubServers.Client.Common.Network.API;

import net.ME1312.Galaxi.Library.Container.ContainedPair;
import net.ME1312.Galaxi.Library.Container.Pair;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Map.ObjectMapValue;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Client.DataClient;
import net.ME1312.SubData.Client.DataSender;
import net.ME1312.SubData.Client.Library.ForwardedDataSender;
import net.ME1312.SubData.Client.SubDataClient;
import net.ME1312.SubServers.Client.Common.ClientAPI;
import net.ME1312.SubServers.Client.Common.Network.Packet.PacketDownloadPlayerInfo;
import net.ME1312.SubServers.Client.Common.Network.Packet.PacketDownloadProxyInfo;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.function.Consumer;

/**
 * Simplified Proxy Data Class
 */
public class Proxy {
    ObjectMap<String> raw;
    private List<RemotePlayer> players = null;
    DataClient client;
    long timestamp;

    /**
     * Create an API representation of a Proxy
     *
     * @param raw Raw representation of the Proxy
     */
    public Proxy(ObjectMap<String> raw) {
        this(null, raw);
    }

    /**
     * Create an API representation of a Proxy
     *
     * @param client SubData connection
     * @param raw Raw representation of the Proxy
     */
    Proxy(DataClient client, ObjectMap<String> raw) {
        this.client = client;
        load(raw);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Proxy && getSignature().equals(((Proxy) obj).getSignature());
    }

    private void load(ObjectMap<String> raw) {
        this.raw = raw;
        this.players = null;
        this.timestamp = Calendar.getInstance().getTime().getTime();
    }

    private SubDataClient client() {
        return SimplifiedData.client(client);
    }

    /**
     * Download a new copy of the data from SubData
     */
    public void refresh() {
        String name = getName();
        client().sendPacket(new PacketDownloadProxyInfo(Collections.singletonList(name), data -> load(data.getMap(name))));
    }

    /**
     * Gets the SubData Client Channel IDs
     *
     * @return SubData Client Channel ID Array
     */
    @SuppressWarnings("unchecked")
    public DataSender[] getSubData() {
        ObjectMap<Integer> subdata = new ObjectMap<Integer>((Map<Integer, ?>) raw.getObject("subdata"));
        Integer[] keys = subdata.getKeys().toArray(new Integer[0]);
        DataSender[] channels = new DataSender[keys.length];
        Arrays.sort(keys);
        for (int i = 0; i < keys.length; ++i) channels[i] = (subdata.isNull(keys[i]))? null : new ForwardedDataSender((SubDataClient) ClientAPI.getInstance().getSubDataNetwork()[0], subdata.getUUID(keys[i]));
        return channels;
    }

    /**
     * Get the Name of this Proxy
     *
     * @return Name
     */
    public String getName() {
        return raw.getString("name");
    }

    /**
     * Get the Display Name of this Proxy
     *
     * @return Display Name
     */
    public String getDisplayName() {
        return raw.getString("display");
    }

    /**
     * Determine if the proxy is the Master Proxy
     *
     * @return Master Proxy Status
     */
    public boolean isMaster() {
        return raw.getBoolean("master");
    }

    /**
     * Get the players on this proxy
     *
     * @return Remote Player Collection
     */
    public Collection<Pair<String, UUID>> getPlayers() {
        List<Pair<String, UUID>> players = new ArrayList<Pair<String, UUID>>();
        for (String id : raw.getMap("players").getKeys()) {
            players.add(new ContainedPair<String, UUID>(raw.getMap("players").getString(id), UUID.fromString(id)));
        }
        return players;
    }

    /**
     * Get the players on this proxy
     *
     * @param callback Remote Player Collection
     */
    public void getPlayers(Consumer<Collection<RemotePlayer>> callback) {
        Util.nullpo(callback);
        StackTraceElement[] origin = new Throwable().getStackTrace();
        Runnable run = () -> {
            try {
                callback.accept(players);
            } catch (Throwable e) {
                Throwable ew = new InvocationTargetException(e);
                ew.setStackTrace(origin);
                ew.printStackTrace();
            }
        };

        if (players == null) {
            LinkedList<UUID> ids = new LinkedList<UUID>();
            for (String id : raw.getMap("players").getKeys()) ids.add(UUID.fromString(id));
            client().sendPacket(new PacketDownloadPlayerInfo(ids, data -> {
                LinkedList<RemotePlayer> players = new LinkedList<RemotePlayer>();
                for (String player : data.getKeys()) {
                    players.add(RemotePlayer.instance.construct(this, data.getMap(player)));
                }

                this.players = players;
                run.run();
            }));
        } else {
            run.run();
        }
    }

    /**
     * Get the Signature of this Object
     *
     * @return Object Signature
     */
    public final String getSignature() {
        return raw.getString("signature");
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
     * Determine if an extra value exists
     *
     * @param handle Handle
     * @return Value Status
     */
    public boolean hasExtra(String handle) {
        Util.nullpo(handle);
        return raw.getMap("extra").getKeys().contains(handle);
    }

    /**
     * Get an extra value
     *
     * @param handle Handle
     * @return Value
     */
    public ObjectMapValue<String> getExtra(String handle) {
        Util.nullpo(handle);
        return raw.getMap("extra").get(handle);
    }

    /**
     * Get the extra value section
     *
     * @return Extra Value Section
     */
    public ObjectMap<String> getExtra() {
        return raw.getMap("extra").clone();
    }

    /**
     * Get the raw representation of the Proxy
     *
     * @return Raw Proxy
     */
    public ObjectMap<String> getRaw() {
        return raw.clone();
    }
}

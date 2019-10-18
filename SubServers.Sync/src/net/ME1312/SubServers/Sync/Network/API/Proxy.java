package net.ME1312.SubServers.Sync.Network.API;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Map.ObjectMapValue;
import net.ME1312.Galaxi.Library.NamedContainer;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Client.DataSender;
import net.ME1312.SubData.Client.Library.ForwardedDataSender;
import net.ME1312.SubData.Client.SubDataClient;
import net.ME1312.SubData.Client.SubDataSender;
import net.ME1312.SubServers.Sync.Network.Packet.PacketDownloadProxyInfo;
import net.ME1312.SubServers.Sync.SubAPI;

import java.util.*;

public class Proxy {
    ObjectMap<String> raw;
    long timestamp;

    /**
     * Create an API representation of a Proxy
     *
     * @param raw Raw representation of the Proxy
     */
    public Proxy(ObjectMap<String> raw) {
        load(raw);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Proxy && getSignature().equals(((Proxy) obj).getSignature());
    }

    private void load(ObjectMap<String> raw) {
        this.raw = raw;
        this.timestamp = Calendar.getInstance().getTime().getTime();
    }

    /**
     * Download a new copy of the data from SubData
     */
    public void refresh() {
        String name = getName();
        ((SubDataClient) SubAPI.getInstance().getSubDataNetwork()[0]).sendPacket(new PacketDownloadProxyInfo(name, data -> load(data.getMap(name))));
    }

    /**
     * Gets the SubData Client Channel IDs
     *
     * @return SubData Client Channel ID Array
     */
    @SuppressWarnings("unchecked")
    public DataSender[] getSubData() {
        ObjectMap<Integer> subdata = new ObjectMap<Integer>((Map<Integer, ?>) raw.getObject("subdata"));
        LinkedList<Integer> keys = new LinkedList<Integer>(subdata.getKeys());
        LinkedList<SubDataSender> channels = new LinkedList<SubDataSender>();
        Collections.sort(keys);
        for (Integer channel : keys) channels.add((subdata.isNull(channel))?null:new ForwardedDataSender((SubDataClient) SubAPI.getInstance().getSubDataNetwork()[0], subdata.getUUID(channel)));
        return channels.toArray(new SubDataSender[0]);
    }

    /**
     * Get the Name of this Proxy
     *
     * @return Name
     */
    public String getName() {
        return raw.getRawString("name");
    }

    /**
     * Get the Display Name of this Proxy
     *
     * @return Display Name
     */
    public String getDisplayName() {
        return raw.getRawString("display");
    }

    /**
     * Test if the proxy is connected to RedisBungee's server
     *
     * @return Redis Status
     */
    public boolean isRedis() {
        return raw.getBoolean("redis");
    }

    /**
     * Get the players on this proxy (via RedisBungee)
     *
     * @return Player Collection
     */
    public Collection<NamedContainer<String, UUID>> getPlayers() {
        List<NamedContainer<String, UUID>> players = new ArrayList<NamedContainer<String, UUID>>();
        for (String id : raw.getMap("players").getKeys()) {
            players.add(new NamedContainer<String, UUID>(raw.getMap("players").getMap(id).getRawString("name"), UUID.fromString(id)));
        }
        return players;
    }

    /**
     * Get the Signature of this Object
     *
     * @return Object Signature
     */
    public final String getSignature() {
        return raw.getRawString("signature");
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
        if (Util.isNull(handle)) throw new NullPointerException();
        return raw.getMap("extra").getKeys().contains(handle);
    }

    /**
     * Get an extra value
     *
     * @param handle Handle
     * @return Value
     */
    public ObjectMapValue<String> getExtra(String handle) {
        if (Util.isNull(handle)) throw new NullPointerException();
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

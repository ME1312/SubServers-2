package net.ME1312.SubServers.Sync.Network.API;

import net.ME1312.SubServers.Sync.Library.Config.YAMLSection;
import net.ME1312.SubServers.Sync.Library.Config.YAMLValue;
import net.ME1312.SubServers.Sync.Library.NamedContainer;
import net.ME1312.SubServers.Sync.Library.Util;
import net.ME1312.SubServers.Sync.Network.Packet.PacketDownloadNetworkList;
import net.ME1312.SubServers.Sync.SubAPI;

import java.util.*;

public class Proxy {
    YAMLSection raw;
    long timestamp;

    /**
     * Create an API representation of a Proxy
     *
     * @param raw Raw representation of the Proxy
     */
    public Proxy(YAMLSection raw) {
        load(raw);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Proxy && getSignature().equals(((Proxy) obj).getSignature());
    }

    private void load(YAMLSection raw) {
        this.raw = raw;
        this.timestamp = Calendar.getInstance().getTime().getTime();
    }

    /**
     * Download a new copy of the data from SubData
     */
    public void refresh() {
        String name = getName();
        SubAPI.getInstance().getSubDataNetwork().sendPacket(new PacketDownloadNetworkList(data -> {
            YAMLSection raw = null;
            for (String client : data.getSection("clients").getKeys()) {
                if (data.getSection("clients").getSection(client).getKeys().size() > 0 && data.getSection("clients").getSection(client).getRawString("type", "").equals("Proxy")) {
                    if (data.getSection("clients").getSection(client).getRawString("name").equals(name)) {
                        raw = data.getSection("clients").getSection(client);
                        load(raw);
                        break;
                    }
                }
            }

            if (raw == null) throw new IllegalStateException("Could not find proxy with name: " + name);
        }));
    }

    /**
     * Gets the SubData Client
     *
     * @return SubData Client (or null if not linked)
     */
    public String getSubData() {
        return raw.getRawString("subdata", null);
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
     * Get the players on this proxy (via RedisBungee)
     *
     * @return Player Collection
     */
    @SuppressWarnings({"deprecation", "unchecked"})
    public Collection<NamedContainer<String, UUID>> getPlayers() {
        List<NamedContainer<String, UUID>> players = new ArrayList<NamedContainer<String, UUID>>();
        for (String id : raw.getSection("players").getKeys()) {
            players.add(new NamedContainer<String, UUID>(raw.getSection("players").getSection(id).getRawString("name"), UUID.fromString(id)));
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
        return raw.getSection("extra").getKeys().contains(handle);
    }

    /**
     * Get an extra value
     *
     * @param handle Handle
     * @return Value
     */
    public YAMLValue getExtra(String handle) {
        if (Util.isNull(handle)) throw new NullPointerException();
        return raw.getSection("extra").get(handle);
    }

    /**
     * Get the extra value section
     *
     * @return Extra Value Section
     */
    public YAMLSection getExtra() {
        return raw.getSection("extra").clone();
    }

    @Override
    @SuppressWarnings("unchecked")
    public String toString() {
        return raw.toJSON().toString();
    }
}

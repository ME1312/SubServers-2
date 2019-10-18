package net.ME1312.SubServers.Client.Sponge.Network.API;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.NamedContainer;
import net.ME1312.SubData.Client.DataSender;
import net.ME1312.SubData.Client.Library.ForwardedDataSender;
import net.ME1312.SubData.Client.SubDataClient;
import net.ME1312.SubData.Client.SubDataSender;
import net.ME1312.SubServers.Client.Sponge.Network.Packet.PacketDownloadServerInfo;
import net.ME1312.SubServers.Client.Sponge.SubAPI;

import java.net.InetSocketAddress;
import java.util.*;

public class Server {
    ObjectMap<String> raw;
    long timestamp;

    /**
     * Create an API representation of a Server
     *
     * @param raw Raw representation of the Server
     */
    public Server(ObjectMap<String> raw) {
        load(raw);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Server && getSignature().equals(((Server) obj).getSignature());
    }

    void load(ObjectMap<String>  raw) {
        this.raw = raw;
        this.timestamp = Calendar.getInstance().getTime().getTime();
    }

    /**
     * Download a new copy of the data from SubData
     */
    public void refresh() {
        String name = getName();
        ((SubDataClient) SubAPI.getInstance().getSubDataNetwork()[0]).sendPacket(new PacketDownloadServerInfo(name, data -> load(data.getMap(name))));
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
     * Get the Name of this Server
     *
     * @return Server Name
     */
    public String getName() {
        return raw.getRawString("name");
    }

    /**
     * Get the Display Name of this Server
     *
     * @return Display Name
     */
    public String getDisplayName() {
        return raw.getRawString("display");
    }
    /**
     * Get the Address of this Server
     *
     * @return Server Address
     */
    public InetSocketAddress getAddress() {
        return new InetSocketAddress(raw.getRawString("address").split(":")[0], Integer.parseInt(raw.getRawString("address").split(":")[1]));
    }

    /**
     * Get this Server's Groups
     *
     * @return Group names
     */
    public List<String> getGroups() {
        return new LinkedList<String>(raw.getRawStringList("group"));
    }

    /**
     * Get the players on this server
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
     * If the server is hidden from players
     *
     * @return Hidden Status
     */
    public boolean isHidden() {
        return raw.getBoolean("hidden");
    }

    /**
     * Gets the MOTD of the Server
     *
     * @return Server MOTD
     */
    public String getMotd() {
        return raw.getRawString("motd");
    }

    /**
     * Gets if the Server is Restricted
     *
     * @return Restricted Status
     */
    public boolean isRestricted() {
        return raw.getBoolean("restricted");
    }

    /**
     * Get a copy of the current whitelist
     *
     * @return Player Whitelist
     */
    public Collection<UUID> getWhitelist() {
        return raw.getUUIDList("whitelist");
    }

    /**
     * See if a player is whitelisted
     *
     * @param player Player to check
     * @return Whitelisted Status
     */
    public boolean isWhitelisted(UUID player) {
        return getWhitelist().contains(player);
    }

    /**
     * Get the Signature of this Object
     *
     * @return Object Signature
     */
    public String getSignature() {
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
     * Get the raw representation of the Server
     *
     * @return Raw Server
     */
    public ObjectMap<String> getRaw() {
        return raw.clone();
    }
}

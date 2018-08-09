package net.ME1312.SubServers.Sync.Network.API;

import net.ME1312.SubServers.Sync.Library.Config.YAMLSection;
import net.ME1312.SubServers.Sync.Library.NamedContainer;
import net.ME1312.SubServers.Sync.Network.Packet.PacketDownloadServerInfo;
import net.ME1312.SubServers.Sync.SubAPI;

import java.net.InetSocketAddress;
import java.util.*;

public class Server {
    YAMLSection raw;
    long timestamp;

    /**
     * Create an API representation of a Server
     *
     * @param raw Raw representation of the Server
     */
    public Server(YAMLSection raw) {
        load(raw);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Server && getSignature().equals(((Server) obj).getSignature());
    }

    void load(YAMLSection raw) {
        this.raw = raw;
        this.timestamp = Calendar.getInstance().getTime().getTime();
    }

    /**
     * Download a new copy of the data from SubData
     */
    public void refresh() {
        String name = getName();
        SubAPI.getInstance().getSubDataNetwork().sendPacket(new PacketDownloadServerInfo(name, data -> load(data.getSection("servers").getSection(name))));
    }

    /**
     * Gets the SubData Client Address
     *
     * @return SubData Client Address (or null if not linked)
     */
    public String getSubData() {
        return raw.getRawString("subdata", null);
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
        for (String id : raw.getSection("players").getKeys()) {
            players.add(new NamedContainer<String, UUID>(raw.getSection("players").getSection(id).getRawString("name"), UUID.fromString(id)));
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

    @Override
    @SuppressWarnings("unchecked")
    public String toString() {
        return raw.toJSON().toString();
    }
}

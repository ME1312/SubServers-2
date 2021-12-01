package net.ME1312.SubServers.Client.Common.Network.API;

import net.ME1312.Galaxi.Library.Container.ContainedPair;
import net.ME1312.Galaxi.Library.Container.Pair;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Client.DataClient;
import net.ME1312.SubData.Client.DataSender;
import net.ME1312.SubData.Client.Library.ForwardedDataSender;
import net.ME1312.SubData.Client.SubDataClient;
import net.ME1312.SubServers.Client.Common.ClientAPI;
import net.ME1312.SubServers.Client.Common.Network.Packet.PacketCommandServer;
import net.ME1312.SubServers.Client.Common.Network.Packet.PacketDownloadPlayerInfo;
import net.ME1312.SubServers.Client.Common.Network.Packet.PacketDownloadServerInfo;

import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * Simplified Server Data Class
 */
public class Server {
    ObjectMap<String> raw;
    private List<RemotePlayer> players = null;
    DataClient client;
    long timestamp;

    /**
     * Create an API representation of a Server
     *
     * @param raw Raw representation of the Server
     */
    public Server(ObjectMap<String> raw) {
        this(null, raw);
    }

    /**
     * Create an API representation of a Server
     *
     * @param client SubData connection
     * @param raw Raw representation of the Server
     */
    Server(DataClient client, ObjectMap<String> raw) {
        this.client = (SubDataClient) client;
        load(raw);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Server && getSignature().equals(((Server) obj).getSignature());
    }

    void load(ObjectMap<String>  raw) {
        this.raw = raw;
        this.players = null;
        this.timestamp = Calendar.getInstance().getTime().getTime();
    }

    SubDataClient client() {
        return SimplifiedData.client(client);
    }

    /**
     * Download a new copy of the data from SubData
     */
    public void refresh() {
        String name = getName();
        client().sendPacket(new PacketDownloadServerInfo(Collections.singletonList(name), data -> load(data.getMap(name))));
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
     * Get the Name of this Server
     *
     * @return Server Name
     */
    public String getName() {
        return raw.getString("name");
    }

    /**
     * Get the Display Name of this Server
     *
     * @return Display Name
     */
    public String getDisplayName() {
        return raw.getString("display");
    }
    /**
     * Get the Address of this Server
     *
     * @return Server Address
     */
    public InetSocketAddress getAddress() {
        return new InetSocketAddress(raw.getString("address").split(":")[0], Integer.parseInt(raw.getString("address").split(":")[1]));
    }

    /**
     * Get this Server's Groups
     *
     * @return Group names
     */
    public List<String> getGroups() {
        return new LinkedList<String>(raw.getStringList("group"));
    }

    /**
     * Commands the Server
     *
     * @param player Player who's Commanding
     * @param target Player who will Send
     * @param command Commmand to Send
     * @param response Response Code
     */
    public void command(UUID player, UUID target, String command, IntConsumer response) {
        Util.nullpo(command, response);
        StackTraceElement[] origin = new Exception().getStackTrace();
        client().sendPacket(new PacketCommandServer(player, target, getName(), command, data -> {
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
     * Commands the Server
     *
     * @param player Player who's Commanding
     * @param command Commmand to Send
     * @param response Response Code
     */
    public void command(UUID player, String command, IntConsumer response) {
        command(player, null, command, response);
    }

    /**
     * Commands the Server
     *
     * @param command Commmand to Send
     * @param response Response Code
     */
    public void command(String command, IntConsumer response) {
        command(null, command, response);
    }

    /**
     * Commands the Server
     *
     * @param player Player who's Commanding
     * @param target Player who's Commanding
     * @param command Command to Send
     */
    public void command(UUID player, UUID target, String command) {
        command(player, target, command, i -> {});
    }

    /**
     * Commands the Server
     *
     * @param player Player who's Commanding
     * @param command Command to Send
     */
    public void command(UUID player, String command) {
        command(player, command, i -> {});
    }

    /**
     * Commands the Server
     *
     * @param command Command to Send
     */
    public void command(String command) {
        command(command, i -> {});
    }

    /**
     * Get players on this server across all known proxies
     *
     * @return Remote Player Collection
     */
    public Collection<Pair<String, UUID>> getRemotePlayers() {
        List<Pair<String, UUID>> players = new ArrayList<Pair<String, UUID>>();
        for (String id : raw.getMap("players").getKeys()) {
            players.add(new ContainedPair<String, UUID>(raw.getMap("players").getString(id), UUID.fromString(id)));
        }
        return players;
    }

    /**
     * Get players on this server across all known proxies
     *
     * @param callback Remote Player Collection
     */
    public void getRemotePlayers(Consumer<Collection<RemotePlayer>> callback) {
        Util.nullpo(callback);
        StackTraceElement[] origin = new Exception().getStackTrace();
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
        return raw.getString("motd");
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
     * Get the raw representation of the Server
     *
     * @return Raw Server
     */
    public ObjectMap<String> getRaw() {
        return raw.clone();
    }
}

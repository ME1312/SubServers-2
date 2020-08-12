package net.ME1312.SubServers.Client.Sponge.Network.API;

import net.ME1312.Galaxi.Library.Callback.Callback;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Container.NamedContainer;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Client.DataClient;
import net.ME1312.SubData.Client.DataSender;
import net.ME1312.SubData.Client.Library.ForwardedDataSender;
import net.ME1312.SubData.Client.SubDataClient;
import net.ME1312.SubData.Client.SubDataSender;
import net.ME1312.SubServers.Client.Sponge.Network.Packet.PacketDownloadPlayerInfo;
import net.ME1312.SubServers.Client.Sponge.Network.Packet.PacketDownloadServerInfo;
import net.ME1312.SubServers.Client.Sponge.SubAPI;
import org.spongepowered.api.service.permission.Subject;

import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.util.*;

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
        LinkedList<Integer> keys = new LinkedList<Integer>(subdata.getKeys());
        LinkedList<SubDataSender> channels = new LinkedList<SubDataSender>();
        Collections.sort(keys);
        for (Integer channel : keys) channels.add((subdata.isNull(channel))?null:new ForwardedDataSender((SubDataClient) SubAPI.getInstance().getSubDataNetwork()[0], subdata.getUUID(channel)));
        return channels.toArray(new SubDataSender[0]);
    }

    /**
     * Determine if an <i>object</i> can perform some action on this server using possible permissions
     *
     * @param object Object to check against
     * @param permissions Permissions to check (use <b>%</b> as a placeholder for the server name)
     * @return Permission Check Result
     */
    public boolean permits(Subject object, String... permissions) {
        if (Util.isNull(object)) throw new NullPointerException();
        boolean permitted = false;

        for (int p = 0; !permitted && p < permissions.length; p++) {
            String perm = permissions[p];
            if (perm != null) {
                // Check all servers & individual servers permission
                permitted = object.hasPermission(perm.replace("%", "*"))
                         || object.hasPermission(perm.replace("%", this.getName().toLowerCase()));

                // Check all hosts & individual hosts permission
                if (this instanceof SubServer) {
                    permitted = permitted || object.hasPermission(perm.replace("%", "::*"))
                                          || object.hasPermission(perm.replace("%", "::" + ((SubServer) this).getHost().toLowerCase()));
                }

                // Check all groups & individual groups permission
                List<String> groups = this.getGroups();
                if (groups.size() > 0) {
                    permitted = permitted || object.hasPermission(perm.replace("%", ":*"));
                    for (int g = 0; !permitted && g < groups.size(); g++) {
                        permitted = object.hasPermission(perm.replace("%", ":" + groups.get(g).toLowerCase()));
                    }
                }
            }
        }

        return permitted;
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
     * Get players on this server across all known proxies
     *
     * @return Remote Player Collection
     */
    public Collection<NamedContainer<String, UUID>> getGlobalPlayers() {
        List<NamedContainer<String, UUID>> players = new ArrayList<NamedContainer<String, UUID>>();
        for (String id : raw.getMap("players").getKeys()) {
            players.add(new NamedContainer<String, UUID>(raw.getMap("players").getRawString(id), UUID.fromString(id)));
        }
        return players;
    }

    /**
     * Get players on this server across all known proxies
     *
     * @param callback Remote Player Collection
     */
    public void getGlobalPlayers(Callback<Collection<RemotePlayer>> callback) {
        if (Util.isNull(callback)) throw new NullPointerException();
        StackTraceElement[] origin = new Exception().getStackTrace();
        Runnable run = () -> {
            try {
                callback.run(players);
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
                    players.add(new RemotePlayer(data.getMap(player)));
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

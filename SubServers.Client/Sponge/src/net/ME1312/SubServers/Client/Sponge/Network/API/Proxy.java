package net.ME1312.SubServers.Client.Sponge.Network.API;

import net.ME1312.Galaxi.Library.Callback.Callback;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Map.ObjectMapValue;
import net.ME1312.Galaxi.Library.Container.NamedContainer;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Client.DataClient;
import net.ME1312.SubData.Client.DataSender;
import net.ME1312.SubData.Client.Library.ForwardedDataSender;
import net.ME1312.SubData.Client.SubDataClient;
import net.ME1312.SubData.Client.SubDataSender;
import net.ME1312.SubServers.Client.Sponge.Network.Packet.PacketDownloadPlayerInfo;
import net.ME1312.SubServers.Client.Sponge.Network.Packet.PacketDownloadProxyInfo;
import net.ME1312.SubServers.Client.Sponge.SubAPI;
import org.spongepowered.api.service.permission.Subject;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

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
        LinkedList<Integer> keys = new LinkedList<Integer>(subdata.getKeys());
        LinkedList<SubDataSender> channels = new LinkedList<SubDataSender>();
        Collections.sort(keys);
        for (Integer channel : keys) channels.add((subdata.isNull(channel))?null:new ForwardedDataSender((SubDataClient) SubAPI.getInstance().getSubDataNetwork()[0], subdata.getUUID(channel)));
        return channels.toArray(new SubDataSender[0]);
    }

    /**
     * Determine if an <i>object</i> can perform some action on this proxy using possible permissions
     *
     * @param object Object to check against
     * @param permissions Permissions to check (use <b>%</b> as a placeholder for the proxy name)
     * @return Permission Check Result
     */
    public boolean permits(Subject object, String... permissions) {
        if (Util.isNull(object)) throw new NullPointerException();
        boolean permitted = false;

        for (int p = 0; !permitted && p < permissions.length; p++) {
            String perm = permissions[p];
            if (perm != null) {
                // Check all proxies & individual proxies permission
                permitted = object.hasPermission(perm.replace("%", "*"))
                        || object.hasPermission(perm.replace("%", this.getName().toLowerCase()));
            }
        }

        return permitted;
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
     * Determine if the proxy is the Master Proxy
     *
     * @return Master Proxy Status
     */
    public boolean isMaster() {
        return raw.getBoolean("master");
    }

    /**
     * Get the players on this proxy (via RedisBungee)
     *
     * @return Remote Player Collection
     */
    public Collection<NamedContainer<String, UUID>> getPlayers() {
        List<NamedContainer<String, UUID>> players = new ArrayList<NamedContainer<String, UUID>>();
        for (String id : raw.getMap("players").getKeys()) {
            players.add(new NamedContainer<String, UUID>(raw.getMap("players").getRawString(id), UUID.fromString(id)));
        }
        return players;
    }

    /**
     * Get the players on this proxy (via RedisBungee)
     *
     * @param callback Remote Player Collection
     */
    public void getPlayers(Callback<Collection<RemotePlayer>> callback) {
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

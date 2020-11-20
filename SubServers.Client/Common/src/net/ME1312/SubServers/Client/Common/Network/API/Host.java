package net.ME1312.SubServers.Client.Common.Network.API;

import net.ME1312.Galaxi.Library.Callback.Callback;
import net.ME1312.Galaxi.Library.Container.ContainedPair;
import net.ME1312.Galaxi.Library.Container.Pair;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Map.ObjectMapValue;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Client.DataClient;
import net.ME1312.SubData.Client.DataSender;
import net.ME1312.SubData.Client.Library.ForwardedDataSender;
import net.ME1312.SubData.Client.SubDataClient;
import net.ME1312.SubData.Client.SubDataSender;
import net.ME1312.SubServers.Client.Common.ClientAPI;
import net.ME1312.SubServers.Client.Common.Network.Packet.*;

import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

/**
 * Simplified Host Data Class
 */
public class Host {
    HashMap<String, SubServer> servers = new HashMap<String, SubServer>();
    private SubCreator creator;
    private List<RemotePlayer> players = null;
    ObjectMap<String> raw;
    DataClient client;
    long timestamp;

    /**
     * Create an API representation of a Host
     *
     * @param raw Raw representation of the Host
     */
    public Host(ObjectMap<String> raw) {
        this(null, raw);
    }

    /**
     * Create an API representation of a Host
     *
     * @param client SubData connection
     * @param raw Raw representation of the Host
     */
    Host(DataClient client, ObjectMap<String> raw) {
        this.client = client;
        load(raw);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Host && getSignature().equals(((Host) obj).getSignature());
    }

    private void load(ObjectMap<String> raw) {
        this.raw = raw;
        this.players = null;
        this.timestamp = Calendar.getInstance().getTime().getTime();

        servers.clear();
        this.creator = new SubCreator(this, raw.getMap("creator"));
        for (String server : raw.getMap("servers").getKeys()) {
            servers.put(server.toLowerCase(), new SubServer(this, raw.getMap("servers").getMap(server)));
        }
    }

    private SubDataClient client() {
        return SimplifiedData.client(client);
    }

    /**
     * Download a new copy of the data from SubData
     */
    public void refresh() {
        String name = getName();
        client().sendPacket(new PacketDownloadHostInfo(Collections.singletonList(name), data -> load(data.getMap(name))));
    }

    /**
     * Gets the SubData Client Channel IDs
     *
     * @return SubData Client Channel ID Array (may be empty if unsupported)
     */
    @SuppressWarnings("unchecked")
    public DataSender[] getSubData() {
        if (raw.contains("subdata")) {
            ObjectMap<Integer> subdata = new ObjectMap<Integer>((Map<Integer, ?>) raw.getObject("subdata"));
            LinkedList<Integer> keys = new LinkedList<Integer>(subdata.getKeys());
            LinkedList<SubDataSender> channels = new LinkedList<SubDataSender>();
            Collections.sort(keys);
            for (Integer channel : keys) channels.add((subdata.isNull(channel))?null:new ForwardedDataSender((SubDataClient) ClientAPI.getInstance().getSubDataNetwork()[0], subdata.getUUID(channel)));
            return channels.toArray(new SubDataSender[0]);
        } else {
            return new SubDataSender[0];
        }
    }

    /**
     * Is this Host Available?
     *
     * @return Availability Status
     */
    public boolean isAvailable() {
        return raw.getBoolean("available");
    }

    /**
     * Is this Host Enabled?
     *
     * @return Enabled Status
     */
    public boolean isEnabled() {
        return raw.getBoolean("enabled");
    }

    /**
     * Get the Address of this Host
     *
     * @return Host Address
     */
    public InetAddress getAddress() {
        try {
            return InetAddress.getByName(raw.getRawString("address"));
        } catch (UnknownHostException e) {
            throw new IllegalStateException("Invalid address response from raw data key: address");
        }
    }

    /**
     * Get the host Directory Path
     *
     * @return Host Directory Path
     */
    public String getPath() {
        return raw.getRawString("dir");
    }

    /**
     * Get the Name of this Host
     *
     * @return Host Name
     */
    public String getName() {
        return raw.getRawString("name");
    }

    /**
     * Get the Display Name of this Host
     *
     * @return Display Name
     */
    public String getDisplayName() {
        return raw.getRawString("display");
    }

    /**
     * Get players on servers provided by this host across all known proxies
     *
     * @return Remote Player Collection
     */
    public Collection<Pair<String, UUID>> getRemotePlayers() {
        List<Pair<String, UUID>> players = new ArrayList<Pair<String, UUID>>();
        for (SubServer server : getSubServers().values()) {
            players.addAll(server.getRemotePlayers());
        }
        return players;
    }

    /**
     * Get the players on servers provided by this host across all known proxies
     *
     * @param callback Remote Player Collection
     */
    public void getRemotePlayers(Callback<Collection<RemotePlayer>> callback) {
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
            for (SubServer server : getSubServers().values()) for (Pair<String, UUID> player : server.getRemotePlayers()) ids.add(player.value());
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
     * Starts the Servers Specified
     *
     * @param servers Servers
     */
    public void start(String... servers) {
        start(null, servers);
    }

    /**
     * Starts the Servers Specified
     *
     * @param player Player who started
     * @param servers Servers
     */
    public void start(UUID player, String... servers) {
        for (String server : servers) {
            getSubServer(server.toLowerCase()).start(player);
        }
    }

    /**
     * Stops the Servers Specified
     *
     * @param servers Servers
     */
    public void stop(String... servers) {
        stop(null, servers);
    }

    /**
     * Stops the Servers Specified
     *
     * @param player Player who started
     * @param servers Servers
     */
    public void stop(UUID player, String... servers) {
        for (String server : servers) {
            getSubServer(server.toLowerCase()).stop(player);
        }
    }

    /**
     * Terminates the Servers Specified
     *
     * @param servers Servers
     */
    public void terminate(String... servers) {
        terminate(null, servers);
    }

    /**
     * Terminates the Servers Specified
     *
     * @param player Player who started
     * @param servers Servers
     */
    public void terminate(UUID player, String... servers) {
        for (String server : servers) {
            getSubServer(server.toLowerCase()).terminate(player);
        }
    }

    /**
     * Commands the Servers Specified
     *
     * @param command Command to send
     * @param servers Servers
     */
    public void command(String command, String... servers) {
        command(null, command, servers);
    }

    /**
     * Commands the Servers Specified
     *
     * @param player Player who started
     * @param command Command to send
     * @param servers Servers
     * @return Success Status
     */
    public void command(UUID player, String command, String... servers) {
        for (String server : servers) {
            getSubServer(server.toLowerCase()).command(player, command);
        }
    }

    /**
     * Gets the SubCreator Instance for this Host
     *
     * @return SubCreator
     */
    public SubCreator getCreator() {
        return creator;
    }

    /**
     * Gets the SubServers on this Host
     *
     * @return SubServer Map
     */
    public Map<String, ? extends SubServer> getSubServers() {
        return new TreeMap<String, SubServer>(servers);
    }

    /**
     * Gets a SubServer
     *
     * @param name SubServer Name
     * @return a SubServer
     */
    public SubServer getSubServer(String name) {
        if (Util.isNull(name)) throw new NullPointerException();
        return getSubServers().get(name.toLowerCase());
    }

    /**
     * Adds a SubServer
     *
     * @param name Name of Server
     * @param enabled Enabled Status
     * @param port Port Number
     * @param motd Motd of the Server
     * @param log Logging Status
     * @param directory Directory
     * @param executable Executable String
     * @param stopcmd Command to Stop the Server
     * @param hidden if the server should be hidden from players
     * @param restricted Players will need a permission to join if true
     * @param response Response Code
     * @return The SubServer
     */
    public void addSubServer(String name, boolean enabled, int port, String motd, boolean log, String directory, String executable, String stopcmd, boolean hidden, boolean restricted, Callback<Integer> response) {
        addSubServer(null, name, enabled, port, motd, log, directory, executable, stopcmd, hidden, restricted, response);
    }

    /**
     * Adds a SubServer
     *
     * @param player Player adding
     * @param name Name of Server
     * @param enabled Enabled Status
     * @param port Port Number
     * @param motd Motd of the Server
     * @param log Logging Status
     * @param directory Directory
     * @param executable Executable String
     * @param stopcmd Command to Stop the Server
     * @param hidden if the server should be hidden from players
     * @param restricted Players will need a permission to join if true
     * @param response Response Code
     * @return The SubServer
     */
    public void addSubServer(UUID player, String name, boolean enabled, int port, String motd, boolean log, String directory, String executable, String stopcmd, boolean hidden, boolean restricted, Callback<Integer> response) {
        if (Util.isNull(response)) throw new NullPointerException();
        StackTraceElement[] origin = new Exception().getStackTrace();
        client().sendPacket(new PacketAddServer(player, name, enabled, getName(), port, motd, log, directory, executable, stopcmd, hidden, restricted, data -> {
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
     * Adds a SubServer
     *
     * @param name Name of Server
     * @param enabled Enabled Status
     * @param port Port Number
     * @param motd Motd of the Server
     * @param log Logging Status
     * @param directory Directory
     * @param executable Executable String
     * @param stopcmd Command to Stop the Server
     * @param hidden if the server should be hidden from players
     * @param restricted Players will need a permission to join if true
     * @return The SubServer
     */
    public void addSubServer(String name, boolean enabled, int port, String motd, boolean log, String directory, String executable, String stopcmd, boolean hidden, boolean restricted) {
        addSubServer(null, name, enabled, port, motd, log, directory, executable, stopcmd, hidden, restricted);
    }

    /**
     * Adds a SubServer
     *
     * @param player Player adding
     * @param name Name of Server
     * @param enabled Enabled Status
     * @param port Port Number
     * @param motd Motd of the Server
     * @param log Logging Status
     * @param directory Directory
     * @param executable Executable String
     * @param stopcmd Command to Stop the Server
     * @param hidden if the server should be hidden from players
     * @param restricted Players will need a permission to join if true
     * @return The SubServer
     */
    public void addSubServer(UUID player, String name, boolean enabled, int port, String motd, boolean log, String directory, String executable, String stopcmd, boolean hidden, boolean restricted) {
        addSubServer(player, name, enabled, port, motd, log, directory, executable, stopcmd, hidden, restricted, i -> {});
    }

    /**
     * Removes a SubServer
     *
     * @param name SubServer Name
     */
    public void removeSubServer(String name) {
        removeSubServer(null, name);
    }

    /**
     * Removes a SubServer
     *
     * @param player Player Removing
     * @param name SubServer Name
     */
    public void removeSubServer(UUID player, String name) {
        if (Util.isNull(name)) throw new NullPointerException();
        removeSubServer(player, name, false, i -> {});
    }

    /**
     * Forces the Removal of a SubServer
     *
     * @param name SubServer Name
     */
    public void forceRemoveSubServer(String name) {
        forceRemoveSubServer(null, name);
    }

    /**
     * Forces the Removal of a SubServer
     *
     * @param player Player Removing
     * @param name SubServer Name
     */
    public void forceRemoveSubServer(UUID player, String name) {
        if (Util.isNull(name)) throw new NullPointerException();
        removeSubServer(player, name, true, i -> {});
    }

    /**
     * Removes a SubServer
     *
     * @param name SubServer Name
     * @param response Response Code
     */
    public void removeSubServer(String name, Callback<Integer> response) {
        removeSubServer(null, name, response);
    }

    /**
     * Removes a SubServer
     *
     * @param player Player Removing
     * @param name SubServer Name
     * @param response Response Code
     */
    public void removeSubServer(UUID player, String name, Callback<Integer> response) {
        if (Util.isNull(name)) throw new NullPointerException();
        removeSubServer(player, name, false, response);
    }

    /**
     * Forces the Removal of a SubServer
     *
     * @param name SubServer Name
     * @param response Response Code
     */
    public void forceRemoveSubServer(String name, Callback<Integer> response) {
        forceRemoveSubServer(null, name, response);
    }

    /**
     * Forces the Removal of a SubServer
     *
     * @param player Player Removing
     * @param name SubServer Name
     * @param response Response Code
     */
    public void forceRemoveSubServer(UUID player, String name, Callback<Integer> response) {
        if (Util.isNull(name)) throw new NullPointerException();
        removeSubServer(player, name, true, response);
    }

    private void removeSubServer(UUID player, String name, boolean force, Callback<Integer> response) {
        if (Util.isNull(response)) throw new NullPointerException();
        StackTraceElement[] origin = new Exception().getStackTrace();
        client().sendPacket(new PacketRemoveServer(player, name, force, data -> {
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
     * Deletes a SubServer (will move to 'Recently Deleted')
     *
     * @param name SubServer Name
     */
    public void recycleSubServer(String name) {
        recycleSubServer(null, name);
    }

    /**
     * Deletes a SubServer (will move to 'Recently Deleted')
     *
     * @param player Player Deleting
     * @param name SubServer Name
     */
    public void recycleSubServer(UUID player, String name) {
        if (Util.isNull(name)) throw new NullPointerException();
        deleteSubServer(player, name, true, false, i -> {});
    }

    /**
     * Forces the Deletion of a SubServer (will move to 'Recently Deleted')
     *
     * @param name SubServer Name
     */
    public void forceRecycleSubServer(String name) {
        forceRecycleSubServer(null, name);
    }

    /**
     * Forces the Deletion of a SubServer (will move to 'Recently Deleted')
     *
     * @param player Player Deleting
     * @param name SubServer Name
     */
    public void forceRecycleSubServer(UUID player, String name) {
        if (Util.isNull(name)) throw new NullPointerException();
        deleteSubServer(player, name, true, true, i -> {});
    }

    /**
     * Deletes a SubServer (will move to 'Recently Deleted')
     *
     * @param name SubServer Name
     * @param response Response Code
     */
    public void recycleSubServer(String name, Callback<Integer> response) {
        recycleSubServer(null, name, response);
    }

    /**
     * Deletes a SubServer (will move to 'Recently Deleted')
     *
     * @param player Player Deleting
     * @param name SubServer Name
     * @param response Response Code
     */
    public void recycleSubServer(UUID player, String name, Callback<Integer> response) {
        if (Util.isNull(name)) throw new NullPointerException();
        deleteSubServer(player, name, true, false, response);
    }

    /**
     * Forces the Deletion of a SubServer (will move to 'Recently Deleted')
     *
     * @param name SubServer Name
     * @param response Response Code
     */
    public void forceRecycleSubServer(String name, Callback<Integer> response) {
        forceRecycleSubServer(null, name, response);
    }

    /**
     * Forces the Deletion of a SubServer (will move to 'Recently Deleted')
     *
     * @param player Player Deleting
     * @param name SubServer Name
     * @param response Response Code
     */
    public void forceRecycleSubServer(UUID player, String name, Callback<Integer> response) {
        if (Util.isNull(name)) throw new NullPointerException();
        deleteSubServer(player, name, true, true, response);
    }

    /**
     * Deletes a SubServer
     *
     * @param name SubServer Name
     * @return Success Status
     */
    public void deleteSubServer(String name) {
        deleteSubServer(null, name);
    }

    /**
     * Deletes a SubServer
     *
     * @param player Player Deleting
     * @param name SubServer Name
     * @return Success Status
     */
    public void deleteSubServer(UUID player, String name) {
        if (Util.isNull(name)) throw new NullPointerException();
        deleteSubServer(player, name, false, false, i -> {});
    }

    /**
     * Forces the Deletion of a SubServer
     *
     * @param name SubServer Name
     * @return Success Status
     */
    public void forceDeleteSubServer(String name) {
        forceDeleteSubServer(null, name);
    }

    /**
     * Forces the Deletion of a SubServer
     *
     * @param player Player Deleting
     * @param name SubServer Name
     * @return Success Status
     */
    public void forceDeleteSubServer(UUID player, String name) {
        if (Util.isNull(name)) throw new NullPointerException();
        deleteSubServer(player, name, false, true, i -> {});
    }

    /**
     * Deletes a SubServer
     *
     * @param name SubServer Name
     * @return Success Status
     */
    public void deleteSubServer(String name, Callback<Integer> response) {
        deleteSubServer(null, name, response);
    }

    /**
     * Deletes a SubServer
     *
     * @param player Player Deleting
     * @param name SubServer Name
     * @return Success Status
     */
    public void deleteSubServer(UUID player, String name, Callback<Integer> response) {
        if (Util.isNull(name)) throw new NullPointerException();
        deleteSubServer(player, name, false, false, response);
    }

    /**
     * Forces the Deletion of a SubServer
     *
     * @param name SubServer Name
     * @return Success Status
     */
    public void forceDeleteSubServer(String name, Callback<Integer> response) {
        forceDeleteSubServer(null, name, response);
    }

    /**
     * Forces the Deletion of a SubServer
     *
     * @param player Player Deleting
     * @param name SubServer Name
     * @return Success Status
     */
    public void forceDeleteSubServer(UUID player, String name, Callback<Integer> response) {
        if (Util.isNull(name)) throw new NullPointerException();
        deleteSubServer(player, name, false, true, response);
    }

    private void deleteSubServer(UUID player, String name, boolean recycle, boolean force, Callback<Integer> response) {
        if (Util.isNull(response)) throw new NullPointerException();
        StackTraceElement[] origin = new Exception().getStackTrace();
        client().sendPacket(new PacketDeleteServer(player, name, recycle, force, data -> {
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
     * Get the raw representation of the Host
     *
     * @return Raw Host
     */
    public ObjectMap<String> getRaw() {
        return raw.clone();
    }
}

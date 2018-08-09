package net.ME1312.SubServers.Client.Sponge.Network.API;

import net.ME1312.SubServers.Client.Sponge.Library.Config.YAMLSection;
import net.ME1312.SubServers.Client.Sponge.Library.Config.YAMLValue;
import net.ME1312.SubServers.Client.Sponge.Library.Util;
import net.ME1312.SubServers.Client.Sponge.Network.Packet.PacketDownloadHostInfo;
import net.ME1312.SubServers.Client.Sponge.SubAPI;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

public class Host {
    HashMap<String, SubServer> servers = new HashMap<String, SubServer>();
    private SubCreator creator;
    YAMLSection raw;
    long timestamp;

    /**
     * Create an API representation of a Host
     *
     * @param raw Raw representation of the Host
     */
    public Host(YAMLSection raw) {
        load(raw);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Host && getSignature().equals(((Host) obj).getSignature());
    }

    private void load(YAMLSection raw) {
        this.raw = raw;
        this.timestamp = Calendar.getInstance().getTime().getTime();

        servers.clear();
        this.creator = new SubCreator(this, raw.getSection("creator"));
        for (String server : raw.getSection("servers").getKeys()) {
            servers.put(server.toLowerCase(), new SubServer(this, raw.getSection("servers").getSection(server)));
        }
    }

    /**
     * Download a new copy of the data from SubData
     */
    public void refresh() {
        String name = getName();
        SubAPI.getInstance().getSubDataNetwork().sendPacket(new PacketDownloadHostInfo(name, data -> load(data.getSection("hosts").getSection(name))));
    }

    /**
     * Gets the SubData Client Address
     *
     * @return SubData Client Address (or null if unlinked/unsupported)
     */
    public String getSubData() {
        return raw.getRawString("subdata", null);
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

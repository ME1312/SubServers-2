package net.ME1312.SubServers.Velocity.Server;

import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Client.DataSender;
import net.ME1312.SubData.Client.Library.ForwardedDataSender;
import net.ME1312.SubData.Client.SubDataClient;
import net.ME1312.SubServers.Velocity.SubAPI;

import com.velocitypowered.api.permission.PermissionSubject;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.ServerInfo;

import java.net.InetSocketAddress;
import java.util.*;

/**
 * Server Class
 */
public class ServerData {
    private final ServerInfo info;
    private String motd;
    private boolean restricted;
    private HashMap<Integer, UUID> subdata = new HashMap<Integer, UUID>();
    public List<UUID> whitelist = new ArrayList<UUID>();
    private String nick = null;
    private boolean hidden;
    private final String signature;

    public ServerData(String signature, String name, String display, InetSocketAddress address, Map<Integer, UUID> subdata, String motd, boolean hidden, boolean restricted, Collection<UUID> whitelist) {
        this.info = new ServerInfo(name, address);
        this.motd = motd;
        this.restricted = restricted;
        this.signature = signature;
        this.whitelist.addAll(whitelist);
        this.hidden = hidden;
        setDisplayName(display);

        for (int channel : subdata.keySet())
            setSubData(subdata.get(channel), channel);
    }

    /**
     * Gets the SubData Client Channel IDs
     *
     * @return SubData Client Channel ID Array
     */
    public DataSender[] getSubData() {
        Integer[] keys = subdata.keySet().toArray(new Integer[0]);
        DataSender[] channels = new DataSender[keys.length];
        Arrays.sort(keys);
        for (int i = 0; i < keys.length; ++i) channels[i] = (subdata.getOrDefault(keys[i], null) == null)? null : new ForwardedDataSender((SubDataClient) SubAPI.getInstance().getSubDataNetwork()[0], subdata.get(keys[i]));
        return channels;
    }

    /**
     * Link a SubData Client to this Object
     *
     * @param client Client to Link
     * @param channel Channel ID
     */
    public void setSubData(UUID client, int channel) {
        if (channel < 0) throw new IllegalArgumentException("Subchannel ID cannot be less than zero");
        if (client != null || channel == 0) {
            if (!subdata.keySet().contains(channel) || (channel == 0 && (client == null || subdata.get(channel) == null))) {
                subdata.put(channel, client);
            }
        } else {
            subdata.remove(channel);
        }
    }

    /**
     * Get the underlying ServerInfo of this Server
     *
     * @return ServerInfo
     */
    public ServerInfo get() {
        return info;
    }

    /**
     * Get the Address of this Server
     *
     * @return Server Address
     */
    public InetSocketAddress getAddress() {
        return info.getAddress();
    }

    /**
     * Get the Name of this Server
     *
     * @return Server Name
     */
    public String getName() {
        return info.getName();
    }

    /**
     * Get the Display Name of this Server
     *
     * @return Display Name
     */
    public String getDisplayName() {
        return (nick == null)?getName():nick;
    }

    /**
     * Sets the Display Name for this Server
     *
     * @param value Value (or null to reset)
     */
    public void setDisplayName(String value) {
        if (value == null || value.length() == 0 || getName().equals(value)) {
            this.nick = null;
        } else {
            this.nick = value;
        }
    }

    /**
     * If the server is hidden from players
     *
     * @return Hidden Status
     */
    public boolean isHidden() {
        return hidden;
    }

    /**
     * Set if the server is hidden from players
     *
     * @param value Value
     */
    public void setHidden(boolean value) {
        Util.nullpo(value);
        this.hidden = value;
    }

    /**
     * Gets the MOTD of the Server
     *
     * @return Server MOTD
     */
    public String getMotd() {
        return motd;
    }

    /**
     * Sets the MOTD of the Server
     *
     * @param value Value
     */
    public void setMotd(String value) {
        this.motd = value;
    }

    /**
     * Gets if the Server is Restricted
     *
     * @return Restricted Status
     */
    public boolean isRestricted() {
        return restricted;
    }

    /**
     * Sets if the Server is Restricted
     *
     * @param value Value
     */
    public void setRestricted(boolean value) {
        this.restricted = value;
    }

    /**
     * See if a player is whitelisted
     *
     * @param player Player
     * @return Whitelisted Status
     */
    public boolean canAccess(PermissionSubject player) {
        return !restricted || player.hasPermission("bungeecord.server." + getName()) || (player instanceof Player && whitelist.contains(((Player) player).getUniqueId()));
    }

    /**
     * Add a player to the whitelist (for use with restricted servers)
     *
     * @param player Player to add
     */
    public void whitelist(UUID player) {
        Util.nullpo(player);
        whitelist.add(player);
    }

    /**
     * Remove a player to the whitelist
     *
     * @param player Player to remove
     */
    public void unwhitelist(UUID player) {
        whitelist.remove(player);
    }

    /**
     * Get the Signature of this Object
     *
     * @return Object Signature
     */
    public final String getSignature() {
        return signature;
    }
}

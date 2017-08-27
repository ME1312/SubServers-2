package net.ME1312.SubServers.Bungee.Host;

import net.ME1312.SubServers.Bungee.Event.SubEditServerEvent;
import net.ME1312.SubServers.Bungee.Library.Config.YAMLSection;
import net.ME1312.SubServers.Bungee.Library.Config.YAMLValue;
import net.ME1312.SubServers.Bungee.Library.Exception.InvalidServerException;
import net.ME1312.SubServers.Bungee.Library.ExtraDataHandler;
import net.ME1312.SubServers.Bungee.Library.NamedContainer;
import net.ME1312.SubServers.Bungee.Library.Util;
import net.ME1312.SubServers.Bungee.Network.Client;
import net.ME1312.SubServers.Bungee.Network.ClientHandler;
import net.ME1312.SubServers.Bungee.Network.SubDataServer;
import net.ME1312.SubServers.Bungee.SubAPI;
import net.md_5.bungee.api.Callback;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.json.JSONObject;

import java.net.InetSocketAddress;
import java.util.*;

/**
 * Server Class
 */
public class Server implements ServerInfo, ClientHandler, ExtraDataHandler {
    private YAMLSection extra = new YAMLSection();
    private Client client = null;
    private List<String> groups = new ArrayList<String>();
    private String nick = null;
    private ServerInfo info;
    private boolean hidden;

    public Server(String name, InetSocketAddress address, String motd, boolean hidden, boolean restricted) throws InvalidServerException {
        if (Util.isNull(name, address, motd, hidden, restricted)) throw new NullPointerException();
        if (name.contains(" ")) throw new InvalidServerException("Server names cannot have spaces: " + name);
        SubDataServer.allowConnection(getAddress().getAddress());
        this.info = new ServerInfo(name, address, motd, restricted);
        this.hidden = hidden;
    }

    private static final class ServerInfo extends net.md_5.bungee.BungeeServerInfo {
        private String motd;
        private boolean restricted;

        public ServerInfo(String name, InetSocketAddress address, String motd, boolean restricted) {
            super(name, address, ChatColor.translateAlternateColorCodes('&', motd), restricted);
            this.motd = motd;
            this.restricted = restricted;
        }

        @Override
        public String getMotd() {
            return motd;
        }

        public void setMotd(String value) {
            this.motd = value;
        }

        @Override
        public boolean isRestricted() {
            return restricted;
        }

        public void setRestricted(boolean value) {
            this.restricted = value;
        }
    }

    @Override
    public Client getSubData() {
        return client;
    }

    @Override
    public void setSubData(Client client) {
        this.client = client;
        if (client != null && (client.getHandler() == null || !equals(client.getHandler()))) client.setHandler(this);
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
            new SubEditServerEvent(null, this, new NamedContainer<String, Object>("display", getName()), false);
            this.nick = null;
        } else {
            new SubEditServerEvent(null, this, new NamedContainer<String, Object>("display", value), false);
            this.nick = value;
        }
    }

    /**
     * Get this Server's Groups
     *
     * @return Group names
     */
    public List<String> getGroups() {
        return groups;
    }

    /**
     * Add this Server to a Group
     *
     * @param value Group name
     */
    @SuppressWarnings("deprecation")
    public void addGroup(String value) {
        if (Util.isNull(value)) throw new NullPointerException();
        if (value.length() > 0 && !groups.contains(value)) {
            List<Server> list = (SubAPI.getInstance().getInternals().groups.keySet().contains(value))?SubAPI.getInstance().getInternals().groups.get(value):new ArrayList<Server>();
            list.add(this);
            SubAPI.getInstance().getInternals().groups.put(value, list);
            groups.add(value);
        }
    }

    /**
     * Remove this Server from a Group
     *
     * @param value value Group name
     */
    @SuppressWarnings("deprecation")
    public void removeGroup(String value) {
        if (Util.isNull(value)) throw new NullPointerException();
        List<Server> list = SubAPI.getInstance().getInternals().groups.get(value);
        list.remove(this);
        SubAPI.getInstance().getInternals().groups.put(value, list);
        groups.remove(value);
    }

    /**
     * If the Server is hidden from players
     *
     * @return Hidden Status
     */
    public boolean isHidden() {
        return hidden;
    }

    /**
     * Set if the Server is hidden from players
     *
     * @param value Value
     */
    public void setHidden(boolean value) {
        if (Util.isNull(value)) throw new NullPointerException();
        new SubEditServerEvent(null, this, new NamedContainer<String, Object>("hidden", value), false);
        this.hidden = value;
    }

    // Methods unrelated to SubServers

    /**
     * Get this Server's Name
     *
     * @return Server Name
     */
    @Override
    public String getName() {
        return info.getName();
    }

    /**
     * Get this Server's Address
     *
     * @return Server Address
     */
    @Override
    public InetSocketAddress getAddress() {
        return info.getAddress();
    }

    /**
     * Get the Players connected to this Server
     *
     * @return Player list
     */
    @Override
    public Collection<ProxiedPlayer> getPlayers() {
        return info.getPlayers();
    }

    /**
     * Get this Server's MOTD
     *
     * @return Server MOTD
     */
    @Override
    public String getMotd() {
        return info.getMotd();
    }

    /**
     * Set this Server's MOTD
     *
     * @param value Value
     */
    public void setMotd(String value) {
        if (Util.isNull(value)) throw new NullPointerException();
        new SubEditServerEvent(null, this, new NamedContainer<String, Object>("motd", value), false);
        info.setMotd(value);
    }

    /**
     * Whether the Player can access this Server
     *
     * @param sender Player
     * @return Player Access Status
     */
    @Override
    public boolean canAccess(CommandSender sender) {
        return info.canAccess(sender);
    }

    /**
     * Send PluginMessageChannel data to the Server
     *
     * @param channel Channel name
     * @param data Data to send
     */
    @Override
    public void sendData(String channel, byte[] data) {
        info.sendData(channel, data);
    }

    /**
     * Send PluginMessageChannel data to the Server
     *
     * @param channel Channel name
     * @param data Data to send
     * @param queue Queue message for later if cannot be sent immediately
     * @return If the message was sent immediately
     */
    @Override
    public boolean sendData(String channel, byte[] data, boolean queue) {
        return info.sendData(channel, data, queue);
    }

    /**
     * Ping the Server
     *
     * @param callback Ping Callback
     */
    @Override
    public void ping(Callback<ServerPing> callback) {
        info.ping(callback);
    }

    /**
     * Get the Server's Restricted Status
     *
     * @return Restricted Status
     */
    public boolean isRestricted() {
        return info.isRestricted();
    }

    public void setRestricted(boolean value) {
        if (Util.isNull(value)) throw new NullPointerException();
        new SubEditServerEvent(null, this, new NamedContainer<String, Object>("restricted", value), false);
        info.setRestricted(value);
    }

    @Override
    public void addExtra(String handle, Object value) {
        if (Util.isNull(handle, value)) throw new NullPointerException();
        extra.set(handle, value);
    }

    @Override
    public boolean hasExtra(String handle) {
        if (Util.isNull(handle)) throw new NullPointerException();
        return extra.getKeys().contains(handle);
    }

    @Override
    public YAMLValue getExtra(String handle) {
        if (Util.isNull(handle)) throw new NullPointerException();
        return extra.get(handle);
    }

    @Override
    public YAMLSection getExtra() {
        return extra.clone();
    }

    @Override
    public void removeExtra(String handle) {
        if (Util.isNull(handle)) throw new NullPointerException();
        extra.remove(handle);
    }

    @Override
    public String toString() {
        JSONObject info = new JSONObject();
        info.put("type", "Server");
        info.put("name", getName());
        info.put("group", getGroups());
        info.put("display", getDisplayName());
        info.put("address", getAddress().getAddress().getHostAddress() + ':' + getAddress().getPort());
        info.put("motd", getMotd());
        info.put("restricted", isRestricted());
        info.put("hidden", isHidden());
        JSONObject players = new JSONObject();
        for (ProxiedPlayer player : getPlayers()) {
            JSONObject pinfo = new JSONObject();
            pinfo.put("name", player.getName());
            pinfo.put("nick", player.getDisplayName());
            players.put(player.getUniqueId().toString(), pinfo);
        }
        info.put("players", players);
        if (getSubData() != null) info.put("subdata", getSubData().getAddress().toString());
        info.put("extra", getExtra().toJSON());
        return info.toString();
    }
}

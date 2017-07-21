package net.ME1312.SubServers.Bungee.Host;

import net.ME1312.SubServers.Bungee.Library.Config.YAMLSection;
import net.ME1312.SubServers.Bungee.Library.Config.YAMLValue;
import net.ME1312.SubServers.Bungee.Library.Exception.InvalidServerException;
import net.ME1312.SubServers.Bungee.Library.ExtraDataHandler;
import net.ME1312.SubServers.Bungee.Library.Util;
import net.ME1312.SubServers.Bungee.Network.Client;
import net.ME1312.SubServers.Bungee.Network.ClientHandler;
import net.md_5.bungee.BungeeServerInfo;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.json.JSONObject;

import java.net.InetSocketAddress;

/**
 * Server Class
 */
public class Server extends BungeeServerInfo implements ClientHandler, ExtraDataHandler {
    private YAMLSection extra = new YAMLSection();
    private Client client = null;
    private String nick = null;
    private String motd;
    private boolean restricted;
    private boolean hidden;

    public Server(String name, InetSocketAddress address, String motd, boolean hidden, boolean restricted) throws InvalidServerException {
        super(name, address, ChatColor.translateAlternateColorCodes('&', motd), restricted);
        if (Util.isNull(name, address, motd, hidden, restricted)) throw new NullPointerException();
        if (name.contains(" ")) throw new InvalidServerException("Server names cannot have spaces: " + name);
        this.motd = motd;
        this.restricted = restricted;
        this.hidden = hidden;
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
        if (Util.isNull(value)) throw new NullPointerException();
        this.hidden = value;
    }

    /**
     * Gets the MOTD of the Server
     *
     * @return Server MOTD
     */
    @Override
    public String getMotd() {
        return motd;
    }

    /**
     * Sets the MOTD of the Server
     *
     * @param value Value
     */
    public void setMotd(String value) {
        if (Util.isNull(value)) throw new NullPointerException();
        this.motd = value;
    }

    /**
     * Gets if the Server is Restricted
     *
     * @return Restricted Status
     */
    @Override
    public boolean isRestricted() {
        return restricted;
    }

    /**
     * Sets if the Server is Restricted
     *
     * @param value Value
     */
    public void setRestricted(boolean value) {
        if (Util.isNull(value)) throw new NullPointerException();
        this.restricted = value;
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
        info.put("display", getDisplayName());
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

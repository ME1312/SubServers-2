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
import net.md_5.bungee.BungeeServerInfo;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.json.JSONObject;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Server Class
 */
public class ServerContainer extends BungeeServerInfo implements Server {
    private YAMLSection extra = new YAMLSection();
    private Client client = null;
    private List<String> groups = new ArrayList<String>();
    private String nick = null;
    private String motd;
    private boolean restricted;
    private boolean hidden;

    public ServerContainer(String name, InetSocketAddress address, String motd, boolean hidden, boolean restricted) throws InvalidServerException {
        super(name, address, ChatColor.translateAlternateColorCodes('&', motd), restricted);
        if (Util.isNull(name, address, motd, hidden, restricted)) throw new NullPointerException();
        if (name.contains(" ")) throw new InvalidServerException("Server names cannot have spaces: " + name);
        SubDataServer.allowConnection(getAddress().getAddress());
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

    @Override
    public String getDisplayName() {
        return (nick == null)?getName():nick;
    }

    @Override
    public void setDisplayName(String value) {
        if (value == null || value.length() == 0 || getName().equals(value)) {
            new SubEditServerEvent(null, this, new NamedContainer<String, Object>("display", getName()), false);
            this.nick = null;
        } else {
            new SubEditServerEvent(null, this, new NamedContainer<String, Object>("display", value), false);
            this.nick = value;
        }
    }

    @Override
    public List<String> getGroups() {
        return groups;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void addGroup(String value) {
        if (Util.isNull(value)) throw new NullPointerException();
        if (value.length() > 0 && !groups.contains(value)) {
            List<Server> list = (SubAPI.getInstance().getInternals().groups.keySet().contains(value))?SubAPI.getInstance().getInternals().groups.get(value):new ArrayList<Server>();
            list.add(this);
            SubAPI.getInstance().getInternals().groups.put(value, list);
            groups.add(value);
            Collections.sort(groups);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void removeGroup(String value) {
        if (Util.isNull(value)) throw new NullPointerException();
        List<Server> list = SubAPI.getInstance().getInternals().groups.get(value);
        list.remove(this);
        SubAPI.getInstance().getInternals().groups.put(value, list);
        groups.remove(value);
        Collections.sort(groups);
    }

    @Override
    public boolean isHidden() {
        return hidden;
    }

    @Override
    public void setHidden(boolean value) {
        if (Util.isNull(value)) throw new NullPointerException();
        new SubEditServerEvent(null, this, new NamedContainer<String, Object>("hidden", value), false);
        this.hidden = value;
    }

    @Override
    public String getMotd() {
        return motd;
    }

    @Override
    public void setMotd(String value) {
        if (Util.isNull(value)) throw new NullPointerException();
        new SubEditServerEvent(null, this, new NamedContainer<String, Object>("motd", value), false);
        this.motd = value;
    }

    @Override
    public boolean isRestricted() {
        return restricted;
    }

    @Override
    public void setRestricted(boolean value) {
        if (Util.isNull(value)) throw new NullPointerException();
        new SubEditServerEvent(null, this, new NamedContainer<String, Object>("restricted", value), false);
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
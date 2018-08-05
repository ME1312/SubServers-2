package net.ME1312.SubServers.Bungee.Host;

import net.ME1312.SubServers.Bungee.Event.SubEditServerEvent;
import net.ME1312.SubServers.Bungee.Library.Config.YAMLSection;
import net.ME1312.SubServers.Bungee.Library.Config.YAMLValue;
import net.ME1312.SubServers.Bungee.Library.Exception.InvalidServerException;
import net.ME1312.SubServers.Bungee.Library.NamedContainer;
import net.ME1312.SubServers.Bungee.Library.Util;
import net.ME1312.SubServers.Bungee.Network.Client;
import net.ME1312.SubServers.Bungee.Network.SubDataServer;
import net.ME1312.SubServers.Bungee.SubAPI;
import net.ME1312.SubServers.Bungee.SubPlugin;
import net.md_5.bungee.BungeeServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.util.*;

/**
 * Server Class
 */
public class ServerContainer extends BungeeServerInfo implements Server {
    private YAMLSection extra = new YAMLSection();
    private final String signature;
    private Client client = null;
    private List<String> groups = new ArrayList<String>();
    private String nick = null;
    private boolean hidden;

    public ServerContainer(String name, InetSocketAddress address, String motd, boolean hidden, boolean restricted) throws InvalidServerException {
        super(name, address, motd, restricted);
        if (Util.isNull(name, address, motd, hidden, restricted)) throw new NullPointerException();
        if (name.contains(" ")) throw new InvalidServerException("Server names cannot have spaces: " + name);
        signature = SubAPI.getInstance().signAnonymousObject();
        SubDataServer.allowConnection(getAddress().getAddress().getHostAddress());
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
            groups.add(value);
            Collections.sort(groups);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void removeGroup(String value) {
        if (Util.isNull(value)) throw new NullPointerException();
        groups.remove(value);
        Collections.sort(groups);
    }

    @SuppressWarnings({"deprecation", "unchecked"})
    @Override
    public Collection<NamedContainer<String, UUID>> getGlobalPlayers() {
        List<NamedContainer<String, UUID>> players = new ArrayList<NamedContainer<String, UUID>>();
        SubPlugin plugin = SubAPI.getInstance().getInternals();
        if (plugin.redis) {
            try {
                for (UUID player : (Set<UUID>) plugin.redis("getPlayersOnServer", new NamedContainer<>(String.class, getName()))) players.add(new NamedContainer<>((String) plugin.redis("getNameFromUuid", new NamedContainer<>(UUID.class, player)), player));
            } catch (Exception e) {}
        } else {
            for (ProxiedPlayer player : getPlayers()) players.add(new NamedContainer<>(player.getName(), player.getUniqueId()));
        }
        return players;
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

    public void setMotd(String value) {
        if (Util.isNull(value)) throw new NullPointerException();
        new SubEditServerEvent(null, this, new NamedContainer<String, Object>("motd", value), false);
        try {
            Field f = BungeeServerInfo.class.getDeclaredField("motd");
            f.setAccessible(true);
            f.set(this, value);
            f.setAccessible(false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setRestricted(boolean value) {
        if (Util.isNull(value)) throw new NullPointerException();
        new SubEditServerEvent(null, this, new NamedContainer<String, Object>("restricted", value), false);
        try {
            Field f = BungeeServerInfo.class.getDeclaredField("restricted");
            f.setAccessible(true);
            f.set(this, value);
            f.setAccessible(false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public final String getSignature() {
        return signature;
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
        YAMLSection info = new YAMLSection();
        info.set("type", "Server");
        info.set("name", getName());
        info.set("display", getDisplayName());
        info.set("group", getGroups());
        info.set("address", getAddress().getAddress().getHostAddress() + ':' + getAddress().getPort());
        info.set("motd", getMotd());
        info.set("restricted", isRestricted());
        info.set("hidden", isHidden());
        if (getSubData() != null) info.set("subdata", getSubData().getAddress().toString());
        YAMLSection players = new YAMLSection();
        for (NamedContainer<String, UUID> player : getGlobalPlayers()) {
            YAMLSection pinfo = new YAMLSection();
            pinfo.set("name", player.name());
            players.set(player.get().toString(), pinfo);
        }
        info.set("players", players);
        if (getSubData() != null) info.set("subdata", getSubData().getAddress().toString());
        info.set("signature", signature);
        info.set("extra", getExtra());
        return info.toJSON();
    }
}
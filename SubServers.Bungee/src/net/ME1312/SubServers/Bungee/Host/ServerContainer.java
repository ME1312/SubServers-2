package net.ME1312.SubServers.Bungee.Host;

import net.ME1312.SubData.Server.DataClient;
import net.ME1312.SubData.Server.SubDataClient;
import net.ME1312.SubServers.Bungee.Event.SubEditServerEvent;
import net.ME1312.SubServers.Bungee.Event.SubNetworkConnectEvent;
import net.ME1312.SubServers.Bungee.Event.SubNetworkDisconnectEvent;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Map.ObjectMapValue;
import net.ME1312.SubServers.Bungee.Library.Exception.InvalidServerException;
import net.ME1312.Galaxi.Library.NamedContainer;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubServers.Bungee.Network.Packet.PacketOutExRunEvent;
import net.ME1312.SubServers.Bungee.Network.Packet.PacketOutExUpdateWhitelist;
import net.ME1312.SubServers.Bungee.SubAPI;
import net.ME1312.SubServers.Bungee.SubPlugin;
import net.md_5.bungee.BungeeServerInfo;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.net.InetSocketAddress;
import java.util.*;

/**
 * Server Class
 */
public class ServerContainer extends BungeeServerInfo implements Server {
    private ObjectMap<String> extra = new ObjectMap<String>();
    private SubDataClient client = null;
    private String nick = null;
    private List<String> groups = new ArrayList<String>();
    private List<UUID> whitelist = new ArrayList<UUID>();
    private boolean hidden;
    private final String signature;

    public ServerContainer(String name, InetSocketAddress address, String motd, boolean hidden, boolean restricted) throws InvalidServerException {
        super(name, address, motd, restricted);
        if (Util.isNull(name, address, motd, hidden, restricted)) throw new NullPointerException();
        if (name.contains(" ")) throw new InvalidServerException("Server names cannot have spaces: " + name);
        signature = SubAPI.getInstance().signAnonymousObject();
        SubAPI.getInstance().getSubDataNetwork().getProtocol().whitelist(getAddress().getAddress().getHostAddress());
        this.hidden = hidden;
    }

    @Override
    public DataClient getSubData() {
        return client;
    }

    @Override
    public void setSubData(DataClient client) {
        this.client = (SubDataClient) client;
        for (Proxy proxy : SubAPI.getInstance().getProxies().values()) if (proxy.getSubData() != null) {
            ObjectMap<String> args = new ObjectMap<String>();
            args.set("server", getName());
            if (client != null) args.set("address", client.getAddress().toString());
            ((SubDataClient) proxy.getSubData()).sendPacket(new PacketOutExRunEvent((client != null)?SubNetworkConnectEvent.class:SubNetworkDisconnectEvent.class, args));
        }
        if (client != null && (client.getHandler() == null || !equals(client.getHandler()))) ((SubDataClient) client).setHandler(this);
    }

    @Override
    public String getDisplayName() {
        return (nick == null)?getName():nick;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void setDisplayName(String value) {
        if (value == null || value.length() == 0 || getName().equals(value)) {
            SubAPI.getInstance().getInternals().getPluginManager().callEvent(new SubEditServerEvent(null, this, new NamedContainer<String, Object>("display", getName()), false));
            this.nick = null;
        } else {
            SubAPI.getInstance().getInternals().getPluginManager().callEvent(new SubEditServerEvent(null, this, new NamedContainer<String, Object>("display", value), false));
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
        if (plugin.redis != null) {
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
    @SuppressWarnings("deprecation")
    public void setHidden(boolean value) {
        if (Util.isNull(value)) throw new NullPointerException();
        SubAPI.getInstance().getInternals().getPluginManager().callEvent(new SubEditServerEvent(null, this, new NamedContainer<String, Object>("hidden", value), false));
        this.hidden = value;
    }

    @SuppressWarnings("deprecation")
    public void setMotd(String value) {
        if (Util.isNull(value)) throw new NullPointerException();
        SubAPI.getInstance().getInternals().getPluginManager().callEvent(new SubEditServerEvent(null, this, new NamedContainer<String, Object>("motd", value), false));
        try {
            Util.reflect(BungeeServerInfo.class.getDeclaredField("motd"), this, value);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("deprecation")
    public void setRestricted(boolean value) {
        if (Util.isNull(value)) throw new NullPointerException();
        SubAPI.getInstance().getInternals().getPluginManager().callEvent(new SubEditServerEvent(null, this, new NamedContainer<String, Object>("restricted", value), false));
        try {
            Util.reflect(BungeeServerInfo.class.getDeclaredField("restricted"), this, value);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public Collection<UUID> getWhitelist() {
        return new ArrayList<UUID>(whitelist);
    }

    /**
     * See if a player can access this server
     *
     * @param player Player
     * @return Whitelisted Status
     */
    @Override
    public boolean canAccess(CommandSender player) {
        return (player instanceof ProxiedPlayer && whitelist.contains(((ProxiedPlayer) player).getUniqueId())) || super.canAccess(player);
    }

    @Override
    public boolean isWhitelisted(UUID player) {
        return whitelist.contains(player);
    }

    @Override
    public void whitelist(UUID player) {
        if (Util.isNull(player)) throw new NullPointerException();
        whitelist.add(player);
        for (Proxy proxy : SubAPI.getInstance().getProxies().values()) if (proxy.getSubData() != null) ((SubDataClient) proxy.getSubData()).sendPacket(new PacketOutExUpdateWhitelist(getName(), true, player));
    }

    @Override
    public void unwhitelist(UUID player) {
        if (Util.isNull(player)) throw new NullPointerException();
        whitelist.remove(player);
        for (Proxy proxy : SubAPI.getInstance().getProxies().values()) if (proxy.getSubData() != null) ((SubDataClient) proxy.getSubData()).sendPacket(new PacketOutExUpdateWhitelist(getName(), false, player));
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
    public ObjectMapValue getExtra(String handle) {
        if (Util.isNull(handle)) throw new NullPointerException();
        return extra.get(handle);
    }

    @Override
    public ObjectMap<String> getExtra() {
        return extra.clone();
    }

    @Override
    public void removeExtra(String handle) {
        if (Util.isNull(handle)) throw new NullPointerException();
        extra.remove(handle);
    }

    @Override
    public ObjectMap<String> forSubData() {
        ObjectMap<String> info = new ObjectMap<String>();
        info.set("type", "Server");
        info.set("name", getName());
        info.set("display", getDisplayName());
        info.set("group", getGroups());
        info.set("address", getAddress().getAddress().getHostAddress() + ':' + getAddress().getPort());
        info.set("motd", getMotd());
        info.set("whitelist", whitelist);
        info.set("restricted", isRestricted());
        info.set("hidden", isHidden());
        ObjectMap<String> players = new ObjectMap<String>();
        for (NamedContainer<String, UUID> player : getGlobalPlayers()) {
            ObjectMap<String> pinfo = new ObjectMap<String>();
            pinfo.set("name", player.name());
            players.set(player.get().toString(), pinfo);
        }
        info.set("players", players);
        if (getSubData() != null) info.set("subdata", getSubData().getID());
        info.set("signature", signature);
        info.set("extra", getExtra());
        return info;
    }
}
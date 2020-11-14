package net.ME1312.SubServers.Bungee.Host;

import net.ME1312.Galaxi.Library.Container.ContainedPair;
import net.ME1312.SubData.Server.DataClient;
import net.ME1312.SubData.Server.SubDataClient;
import net.ME1312.SubServers.Bungee.Event.SubEditServerEvent;
import net.ME1312.SubServers.Bungee.Event.SubNetworkConnectEvent;
import net.ME1312.SubServers.Bungee.Event.SubNetworkDisconnectEvent;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Map.ObjectMapValue;
import net.ME1312.SubServers.Bungee.Library.Exception.InvalidServerException;
import net.ME1312.Galaxi.Library.Container.Pair;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubServers.Bungee.Network.Packet.PacketOutExRunEvent;
import net.ME1312.SubServers.Bungee.Network.Packet.PacketOutExUpdateWhitelist;
import net.ME1312.SubServers.Bungee.SubAPI;
import net.ME1312.SubServers.Bungee.SubProxy;
import net.md_5.bungee.BungeeServerInfo;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.*;

/**
 * Server Class
 */
public class ServerImpl extends BungeeServerInfo implements Server {
    private HashMap<Integer, SubDataClient> subdata = new HashMap<Integer, SubDataClient>();
    private ObjectMap<String> extra = new ObjectMap<String>();
    private String nick = null;
    private List<String> groups = new ArrayList<String>();
    private List<UUID> whitelist = new ArrayList<UUID>();
    private boolean hidden;
    private final String signature = SubAPI.getInstance().signAnonymousObject();

    /**
     * Construct a new Server data type
     *
     * @param name Server name
     * @param address Server Address
     * @param motd Server MOTD
     * @param hidden Hidden Status
     * @param restricted Restricted Status
     * @return
     */
    public static ServerImpl construct(String name, SocketAddress address, String motd, boolean hidden, boolean restricted) throws InvalidServerException {
        try {
            return new ServerImpl(name, address, motd, hidden, restricted);
        } catch (NoSuchMethodError e) {
            return new ServerImpl(name, (InetSocketAddress) address, motd, hidden, restricted);
        }
    }

    /**
     * Super Method 2 (newest)
     * @see #construct(String, SocketAddress, String, boolean, boolean) for method details
     */
    protected ServerImpl(String name, SocketAddress address, String motd, boolean hidden, boolean restricted) throws InvalidServerException {
        super(name, address, motd, restricted);
        init(name, address, motd, hidden, restricted);
    }

    /**
     * Super Method 1 (oldest)
     * @see #construct(String, SocketAddress, String, boolean, boolean) for method details
     */
    protected ServerImpl(String name, InetSocketAddress address, String motd, boolean hidden, boolean restricted) throws InvalidServerException {
        super(name, address, motd, restricted);
        init(name, address, motd, hidden, restricted);
    }

    @SuppressWarnings("deprecation")
    private void init(String name, SocketAddress address, String motd, boolean hidden, boolean restricted) throws InvalidServerException {
        if (Util.isNull(name, address, motd, hidden, restricted)) throw new NullPointerException();
        if (name.contains(" ")) throw new InvalidServerException("Server names cannot have spaces: " + name);
        SubAPI.getInstance().getInternals().subprotocol.whitelist(getAddress().getAddress().getHostAddress());
        this.hidden = hidden;

        subdata.put(0, null);
    }

    @Override
    public DataClient[] getSubData() {
        LinkedList<Integer> keys = new LinkedList<Integer>(subdata.keySet());
        LinkedList<SubDataClient> channels = new LinkedList<SubDataClient>();
        Collections.sort(keys);
        for (Integer channel : keys) channels.add(subdata.get(channel));
        return channels.toArray(new DataClient[0]);
    }

    public void setSubData(DataClient client, int channel) {
        boolean update = false;
        if (channel < 0) throw new IllegalArgumentException("Subchannel ID cannot be less than zero");
        if (client != null || channel == 0) {
            if (!subdata.keySet().contains(channel) || (channel == 0 && (client == null || subdata.get(channel) == null))) {
                update = true;
                subdata.put(channel, (SubDataClient) client);
                if (client != null && (client.getHandler() == null || !equals(client.getHandler()))) ((SubDataClient) client).setHandler(this);
            }
        } else {
            update = true;
            subdata.remove(channel);
        }

        if (update) for (Proxy proxy : SubAPI.getInstance().getProxies().values()) if (proxy.getSubData()[0] != null) {
            ObjectMap<String> args = new ObjectMap<String>();
            args.set("server", getName());
            args.set("channel", channel);
            if (client != null) args.set("id", client.getID());
            ((SubDataClient) proxy.getSubData()[0]).sendPacket(new PacketOutExRunEvent((client != null)?SubNetworkConnectEvent.class:SubNetworkDisconnectEvent.class, args));
        }
    }

    @Override
    public void removeSubData(DataClient client) {
        for (Integer channel : Util.getBackwards(subdata, (SubDataClient) client)) setSubData(null, channel);
    }

    @Override
    public String getDisplayName() {
        return (nick == null)?getName():nick;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void setDisplayName(String value) {
        if (value == null || value.length() == 0 || getName().equals(value)) {
            SubAPI.getInstance().getInternals().getPluginManager().callEvent(new SubEditServerEvent(null, this, new ContainedPair<String, Object>("display", getName()), false));
            this.nick = null;
        } else {
            SubAPI.getInstance().getInternals().getPluginManager().callEvent(new SubEditServerEvent(null, this, new ContainedPair<String, Object>("display", value), false));
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

    @SuppressWarnings("deprecation")
    @Override
    public Collection<RemotePlayer> getGlobalPlayers() {
        SubProxy plugin = SubAPI.getInstance().getInternals();
        ArrayList<RemotePlayer> players = new ArrayList<RemotePlayer>();
        for (UUID id : Util.getBackwards(plugin.rPlayerLinkS, this)) {
            players.add(plugin.rPlayers.get(id));
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
        SubAPI.getInstance().getInternals().getPluginManager().callEvent(new SubEditServerEvent(null, this, new ContainedPair<String, Object>("hidden", value), false));
        this.hidden = value;
    }

    @SuppressWarnings("deprecation")
    public void setMotd(String value) {
        if (Util.isNull(value)) throw new NullPointerException();
        SubAPI.getInstance().getInternals().getPluginManager().callEvent(new SubEditServerEvent(null, this, new ContainedPair<String, Object>("motd", value), false));
        try {
            Util.reflect(BungeeServerInfo.class.getDeclaredField("motd"), this, value);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("deprecation")
    public void setRestricted(boolean value) {
        if (Util.isNull(value)) throw new NullPointerException();
        SubAPI.getInstance().getInternals().getPluginManager().callEvent(new SubEditServerEvent(null, this, new ContainedPair<String, Object>("restricted", value), false));
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
        if (!whitelist.contains(player)) whitelist.add(player);
        for (Proxy proxy : SubAPI.getInstance().getProxies().values()) if (proxy.getSubData()[0] != null) ((SubDataClient) proxy.getSubData()[0]).sendPacket(new PacketOutExUpdateWhitelist(getName(), true, player));
    }

    @Override
    public void unwhitelist(UUID player) {
        if (Util.isNull(player)) throw new NullPointerException();
        whitelist.remove(player);
        for (Proxy proxy : SubAPI.getInstance().getProxies().values()) if (proxy.getSubData()[0] != null) ((SubDataClient) proxy.getSubData()[0]).sendPacket(new PacketOutExUpdateWhitelist(getName(), false, player));
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
        info.set("whitelist", getWhitelist());
        info.set("restricted", isRestricted());
        info.set("hidden", isHidden());
        ObjectMap<String> players = new ObjectMap<String>();
        for (RemotePlayer player : getGlobalPlayers())
            players.set(player.getUniqueId().toString(), player.getName());
        info.set("players", players);
        ObjectMap<Integer> subdata = new ObjectMap<Integer>();
        for (int channel : this.subdata.keySet()) subdata.set(channel, (this.subdata.get(channel) == null)?null:this.subdata.get(channel).getID());
        info.set("subdata", subdata);
        info.set("signature", signature);
        info.set("extra", getExtra());
        return info;
    }
}
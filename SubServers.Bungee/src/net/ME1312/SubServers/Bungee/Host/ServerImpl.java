package net.ME1312.SubServers.Bungee.Host;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Map.ObjectMapValue;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Server.DataClient;
import net.ME1312.SubData.Server.SubDataClient;
import net.ME1312.SubServers.Bungee.Library.Exception.InvalidServerException;
import net.ME1312.SubServers.Bungee.Network.Packet.PacketOutExEditServer;
import net.ME1312.SubServers.Bungee.Network.Packet.PacketOutExEditServer.Edit;
import net.ME1312.SubServers.Bungee.SubAPI;

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
    private volatile boolean persistent = true;

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
        Util.nullpo(name, address, motd, hidden, restricted);
        if (name.contains(" ")) throw new InvalidServerException("Server names cannot have spaces: " + name);
        SubAPI.getInstance().getInternals().subprotocol.whitelist(getAddress().getAddress().getHostAddress());
        this.hidden = hidden;

        subdata.put(0, null);
    }

    /**
     * Get if this server has been registered
     *
     * @return Registered status
     */
    @SuppressWarnings("deprecation")
    protected boolean isRegistered() {
        return SubAPI.getInstance().getInternals().exServers.containsKey(getName().toLowerCase());
    }

    @Override
    public DataClient[] getSubData() {
        Integer[] keys = subdata.keySet().toArray(new Integer[0]);
        DataClient[] channels = new DataClient[keys.length];
        Arrays.sort(keys);
        for (int i = 0; i < keys.length; ++i) channels[i] = subdata.get(keys[i]);
        return channels;
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

        if (update) {
            for (Proxy proxy : SubAPI.getInstance().getProxies().values()) if (proxy.getSubData()[0] != null) {
                if (client != null) {
                    ((SubDataClient) proxy.getSubData()[0]).sendPacket(new PacketOutExEditServer(this, Edit.CONNECTED, channel, client.getID()));
                } else {
                    ((SubDataClient) proxy.getSubData()[0]).sendPacket(new PacketOutExEditServer(this, Edit.DISCONNECTED, channel));
                }
            }
            if (!persistent) {
                DataClient[] subdata = getSubData();
                if (subdata[0] == null && subdata.length <= 1) {
                    SubAPI.getInstance().removeServer(getName());
                }
            }
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
            this.nick = null;
        } else {
            this.nick = value;
        }
        for (Proxy proxy : SubAPI.getInstance().getProxies().values()) if (proxy.getSubData()[0] != null) {
            ((SubDataClient) proxy.getSubData()[0]).sendPacket(new PacketOutExEditServer(this, Edit.DISPLAY_NAME, getDisplayName()));
        }
    }

    @Override
    public List<String> getGroups() {
        return groups;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void addGroup(String value) {
        Util.nullpo(value);
        if (value.length() > 0 && !groups.contains(value)) {
            groups.add(value);
            Collections.sort(groups);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void removeGroup(String value) {
        Util.nullpo(value);
        groups.remove(value);
        Collections.sort(groups);
    }

    @Override
    public Collection<RemotePlayer> getRemotePlayers() {
        return SubAPI.getInstance().getRemotePlayers(this).values();
    }

    @Override
    public boolean isHidden() {
        return hidden;
    }

    @Override
    public void setHidden(boolean value) {
        this.hidden = value;
        if (isRegistered()) for (Proxy proxy : SubAPI.getInstance().getProxies().values()) if (proxy.getSubData()[0] != null) {
            ((SubDataClient) proxy.getSubData()[0]).sendPacket(new PacketOutExEditServer(this, Edit.HIDDEN, isHidden()));
        }
    }

    public void setMotd(String value) {
        Util.nullpo(value);
        try {
            Util.reflect(BungeeServerInfo.class.getDeclaredField("motd"), this, value);
            for (Proxy proxy : SubAPI.getInstance().getProxies().values()) if (proxy.getSubData()[0] != null) {
                ((SubDataClient) proxy.getSubData()[0]).sendPacket(new PacketOutExEditServer(this, Edit.MOTD, getMotd()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setRestricted(boolean value) {
        Util.nullpo(value);
        try {
            Util.reflect(BungeeServerInfo.class.getDeclaredField("restricted"), this, value);

            if (isRegistered()) for (Proxy proxy : SubAPI.getInstance().getProxies().values()) if (proxy.getSubData()[0] != null) {
                ((SubDataClient) proxy.getSubData()[0]).sendPacket(new PacketOutExEditServer(this, Edit.RESTRICTED, isRestricted()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * See if a player can access this server
     *
     * @param player Player
     * @return Whitelisted Status
     */
    @Override
    public boolean canAccess(CommandSender player) {
        return super.canAccess(player) || (player instanceof ProxiedPlayer && whitelist.contains(((ProxiedPlayer) player).getUniqueId()));
    }

    @Override
    public Collection<UUID> getWhitelist() {
        return new ArrayList<UUID>(whitelist);
    }

    @Override
    public boolean isWhitelisted(UUID player) {
        return whitelist.contains(player);
    }

    @Override
    public void whitelist(UUID player) {
        Util.nullpo(player);
        if (!whitelist.contains(player)) whitelist.add(player);
        if (isRegistered()) for (Proxy proxy : SubAPI.getInstance().getProxies().values()) if (proxy.getSubData()[0] != null) {
            ((SubDataClient) proxy.getSubData()[0]).sendPacket(new PacketOutExEditServer(this, Edit.WHITELIST_ADD, player));
        }
    }

    @Override
    public void unwhitelist(UUID player) {
        Util.nullpo(player);
        whitelist.remove(player);
        if (isRegistered()) for (Proxy proxy : SubAPI.getInstance().getProxies().values()) if (proxy.getSubData()[0] != null) {
            ((SubDataClient) proxy.getSubData()[0]).sendPacket(new PacketOutExEditServer(this, Edit.WHITELIST_REMOVE, player));
        }
    }

    @Override
    public final void persist() {
        persistent = true;
    }

    @Override
    public final String getSignature() {
        return signature;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ServerImpl && signature.equals(((ServerImpl) obj).signature);
    }

    @Override
    public void addExtra(String handle, Object value) {
        Util.nullpo(handle, value);
        extra.set(handle, value);
    }

    @Override
    public boolean hasExtra(String handle) {
        Util.nullpo(handle);
        return extra.getKeys().contains(handle);
    }

    @Override
    public ObjectMapValue getExtra(String handle) {
        Util.nullpo(handle);
        return extra.get(handle);
    }

    @Override
    public ObjectMap<String> getExtra() {
        return extra.clone();
    }

    @Override
    public void removeExtra(String handle) {
        Util.nullpo(handle);
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
        for (RemotePlayer player : getRemotePlayers())
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
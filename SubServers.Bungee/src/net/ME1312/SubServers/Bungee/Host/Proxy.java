package net.ME1312.SubServers.Bungee.Host;

import net.ME1312.SubData.Server.DataClient;
import net.ME1312.SubData.Server.SerializableClientHandler;
import net.ME1312.SubData.Server.SubDataClient;
import net.ME1312.SubServers.Bungee.Event.SubRemoveProxyEvent;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Map.ObjectMapValue;
import net.ME1312.Galaxi.Library.ExtraDataHandler;
import net.ME1312.Galaxi.Library.NamedContainer;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Server.ClientHandler;
import net.ME1312.SubServers.Bungee.SubAPI;
import net.ME1312.SubServers.Bungee.SubPlugin;
import net.md_5.bungee.api.ProxyServer;

import java.util.*;

/**
 * Proxy Class
 */
public class Proxy implements SerializableClientHandler, ExtraDataHandler {
    private ObjectMap<String> extra = new ObjectMap<String>();
    private final String signature;
    private boolean persistent = true;
    private SubDataClient client = null;
    private String nick = null;
    private final String name;

    @SuppressWarnings("deprecation")
    public Proxy(String name) throws IllegalArgumentException {
        if (name == null) {
            name = Util.getNew(SubAPI.getInstance().getInternals().proxies.keySet(), () -> UUID.randomUUID().toString());
            persistent = false;
        }
        if (name.contains(" ")) throw new IllegalArgumentException("Proxy names cannot have spaces: " + name);
        this.name = name;
        this.signature = SubAPI.getInstance().signAnonymousObject();
    }

    @Override
    public DataClient getSubData() {
        return client;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void setSubData(DataClient client) {
        this.client = (SubDataClient) client;
        if (client == null && !persistent) {
            ProxyServer.getInstance().getPluginManager().callEvent(new SubRemoveProxyEvent(this));
            SubAPI.getInstance().getInternals().proxies.remove(getName().toLowerCase());
        }
        if (client != null && (client.getHandler() == null || !equals(client.getHandler()))) ((SubDataClient) client).setHandler(this);
    }

    /**
     * Get the Name of this Proxy
     *
     * @return Name
     */
    public String getName() {
        return name;
    }

    /**
     * Get the Display Name of this Proxy
     *
     * @return Display Name
     */
    public String getDisplayName() {
        return (nick == null)?getName():nick;
    }

    /**
     * Sets the Display Name for this Proxy
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
     * Test if the proxy is connected to RedisBungee's server
     *
     * @return Redis Status
     */
    @SuppressWarnings({"deprecation", "unchecked"})
    public boolean isRedis() {
        SubPlugin plugin = SubAPI.getInstance().getInternals();
        return plugin.redis != null && Util.getDespiteException(() -> plugin.redis("getPlayersOnProxy", new NamedContainer<>(String.class, getName())) != null, false);
    }

    /**
     * Get the players on this proxy (via RedisBungee)
     *
     * @return Player Collection
     */
    @SuppressWarnings({"deprecation", "unchecked"})
    public Collection<NamedContainer<String, UUID>> getPlayers() {
        List<NamedContainer<String, UUID>> players = new ArrayList<NamedContainer<String, UUID>>();
        SubPlugin plugin = SubAPI.getInstance().getInternals();
        if (plugin.redis != null) {
            try {
                for (UUID player : (Set<UUID>) plugin.redis("getPlayersOnProxy", new NamedContainer<>(String.class, getName())))
                    players.add(new NamedContainer<>((String) plugin.redis("getNameFromUuid", new NamedContainer<>(UUID.class, player)), player));
            } catch (Exception e) {}
        }
        return players;
    }

    /**
     * Get the Signature of this Object
     *
     * @return Object Signature
     */
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
        info.set("type", "Proxy");
        info.set("name", getName());
        info.set("display", getDisplayName());
        ObjectMap<String> players = new ObjectMap<String>();
        for (NamedContainer<String, UUID> player : getPlayers()) {
            ObjectMap<String> pinfo = new ObjectMap<String>();
            pinfo.set("name", player.name());
            players.set(player.get().toString(), pinfo);
        }
        info.set("players", players);
        info.set("redis", isRedis());
        if (getSubData() != null) info.set("subdata", getSubData().getID());
        info.set("signature", signature);
        info.set("extra", getExtra());
        return info;
    }
}

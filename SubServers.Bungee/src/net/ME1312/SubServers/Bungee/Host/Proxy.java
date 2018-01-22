package net.ME1312.SubServers.Bungee.Host;

import net.ME1312.SubServers.Bungee.Library.Config.YAMLSection;
import net.ME1312.SubServers.Bungee.Library.Config.YAMLValue;
import net.ME1312.SubServers.Bungee.Library.ExtraDataHandler;
import net.ME1312.SubServers.Bungee.Library.NamedContainer;
import net.ME1312.SubServers.Bungee.Library.Util;
import net.ME1312.SubServers.Bungee.Network.Client;
import net.ME1312.SubServers.Bungee.Network.ClientHandler;
import net.ME1312.SubServers.Bungee.SubAPI;
import net.ME1312.SubServers.Bungee.SubPlugin;
import org.json.JSONObject;

import java.util.*;

/**
 * Proxy Class
 */
public class Proxy implements ClientHandler, ExtraDataHandler {
    private YAMLSection extra = new YAMLSection();
    private final String signature;
    private Client client = null;
    private String nick = null;
    private final String name;

    public Proxy(String name) throws IllegalArgumentException {
        if (Util.isNull(name)) throw new NullPointerException();
        if (name.contains(" ")) throw new IllegalArgumentException("Proxy names cannot have spaces: " + name);
        this.name = name;
        this.signature = SubAPI.getInstance().signAnonymousObject();
    }

    @Override
    public Client getSubData() {
        return client;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void setSubData(Client client) {
        this.client = client;
        if (client == null) SubAPI.getInstance().getInternals().proxies.remove(getName().toLowerCase());
        if (client != null && (client.getHandler() == null || !equals(client.getHandler()))) client.setHandler(this);
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
     * Get the players on this proxy (via RedisBungee)
     *
     * @return Player Collection
     */
    @SuppressWarnings({"deprecation", "unchecked"})
    public Collection<NamedContainer<String, UUID>> getPlayers() {
        List<NamedContainer<String, UUID>> players = new ArrayList<NamedContainer<String, UUID>>();
        SubPlugin plugin = SubAPI.getInstance().getInternals();
        if (plugin.redis) {
            try {
                for (UUID player : (Set<UUID>) plugin.redis("getPlayersOnProxy", new NamedContainer<>(String.class, getName())))
                    players.add(new NamedContainer<>((String) plugin.redis("getNameFromUuid", new NamedContainer<>(UUID.class, player)), player));
            } catch (Exception e) {
                e.printStackTrace();
            }
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
        info.put("type", "Proxy");
        info.put("name", getName());
        info.put("display", getDisplayName());
        JSONObject players = new JSONObject();
        for (NamedContainer<String, UUID> player : getPlayers()) {
            JSONObject pinfo = new JSONObject();
            pinfo.put("name", player.name());
            players.put(player.get().toString(), pinfo);
        }
        info.put("players", players);
        if (getSubData() != null) info.put("subdata", getSubData().getAddress().toString());
        info.put("signature", signature);
        info.put("extra", getExtra().toJSON());
        return info.toString();
    }
}

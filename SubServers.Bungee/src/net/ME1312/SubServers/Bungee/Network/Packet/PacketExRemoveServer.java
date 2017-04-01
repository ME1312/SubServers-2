package net.ME1312.SubServers.Bungee.Network.Packet;

import net.ME1312.SubServers.Bungee.Host.SubCreator;
import net.ME1312.SubServers.Bungee.Library.JSONCallback;
import net.ME1312.SubServers.Bungee.Library.Util;
import net.ME1312.SubServers.Bungee.Library.Version.Version;
import net.ME1312.SubServers.Bungee.Network.Client;
import net.ME1312.SubServers.Bungee.Network.PacketIn;
import net.ME1312.SubServers.Bungee.Network.PacketOut;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.UUID;

/**
 * Create Server External Host Packet
 */
public class PacketExRemoveServer implements PacketIn, PacketOut {
    private static HashMap<String, JSONCallback[]> callbacks = new HashMap<String, JSONCallback[]>();
    private String name;
    private String id;

    /**
     * New PacketExRemoveServer (In)
     */
    public PacketExRemoveServer() {}

    /**
     * New PacketExRemoveServer (Out)
     *
     * @param name Server Name
     * @param callback Callbacks
     */
    public PacketExRemoveServer(String name, JSONCallback... callback) {
        if (Util.isNull(name, callback)) throw new NullPointerException();
        this.name = name;
        this.id = Util.getNew(callbacks.keySet(), UUID::randomUUID).toString();
        callbacks.put(id, callback);
    }

    @Override
    public JSONObject generate() {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("server", name);
        return json;
    }

    @Override
    public void execute(Client client, JSONObject data) {
        for (JSONCallback callback : callbacks.get(data.getString("id"))) callback.run(data);
        callbacks.remove(data.getString("id"));
    }

    @Override
    public Version getVersion() {
        return new Version("2.11.0a");
    }
}
package net.ME1312.SubServers.Client.Bukkit.Network.Packet;

import net.ME1312.SubServers.Client.Bukkit.Library.JSONCallback;
import net.ME1312.SubServers.Client.Bukkit.Library.Util;
import net.ME1312.SubServers.Client.Bukkit.Library.Version.Version;
import net.ME1312.SubServers.Client.Bukkit.Network.PacketIn;
import net.ME1312.SubServers.Client.Bukkit.Network.PacketOut;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.UUID;

/**
 * Teleport Player Packet
 */
public class PacketTeleportPlayer implements PacketIn, PacketOut {
    private static HashMap<String, JSONCallback[]> callbacks = new HashMap<String, JSONCallback[]>();
    private UUID player;
    private String server;
    private String id;

    /**
     * New PacketTeleportPlayer (In)
     */
    public PacketTeleportPlayer() {}

    /**
     * New PacketTeleportPlayer (Out)
     *
     * @param player Player to teleport
     * @param server Where to go
     * @param callback Callbacks
     */
    public PacketTeleportPlayer(UUID player, String server, JSONCallback... callback) {
        if (Util.isNull(player, server, callback)) throw new NullPointerException();
        this.player = player;
        this.server = server;
        this.id = Util.getNew(callbacks.keySet(), UUID::randomUUID).toString();
        callbacks.put(id, callback);
    }

    @Override
    public JSONObject generate() {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("player", player.toString());
        json.put("server", server);
        return json;
    }

    @Override
    public void execute(JSONObject data) {
        for (JSONCallback callback : callbacks.get(data.getString("id"))) callback.run(data);
        callbacks.remove(data.getString("id"));
    }

    @Override
    public Version getVersion() {
        return new Version("2.11.0a");
    }
}
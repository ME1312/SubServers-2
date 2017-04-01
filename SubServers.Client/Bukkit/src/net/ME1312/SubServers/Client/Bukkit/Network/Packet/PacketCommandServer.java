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
 * Command Server Packet
 */
public class PacketCommandServer implements PacketIn, PacketOut {
    private static HashMap<String, JSONCallback[]> callbacks = new HashMap<String, JSONCallback[]>();
    private UUID player;
    private String server;
    private String command;
    private String id;

    /**
     * New PacketCommandServer (In)
     */
    public PacketCommandServer() {}

    /**
     * New PacketCommandServer (Out)
     *
     * @param player Player Sending
     * @param server Server to send to
     * @param command Command to send
     * @param callback Callbacks
     */
    public PacketCommandServer(UUID player, String server, String command, JSONCallback... callback) {
        if (Util.isNull(server, command, callback)) throw new NullPointerException();
        this.player = player;
        this.server = server;
        this.command = command;
        this.id = Util.getNew(callbacks.keySet(), UUID::randomUUID).toString();
        callbacks.put(id, callback);
    }

    @Override
    public JSONObject generate() {
        JSONObject json = new JSONObject();
        json.put("id", id);
        if (player != null) json.put("player", player.toString());
        json.put("server", server);
        json.put("command", command);
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

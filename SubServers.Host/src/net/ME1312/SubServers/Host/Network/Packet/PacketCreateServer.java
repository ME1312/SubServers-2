package net.ME1312.SubServers.Host.Network.Packet;

import net.ME1312.SubServers.Host.Executable.SubCreator;
import net.ME1312.SubServers.Host.Library.JSONCallback;
import net.ME1312.SubServers.Host.Library.Util;
import net.ME1312.SubServers.Host.Library.Version.Version;
import net.ME1312.SubServers.Host.Network.PacketIn;
import net.ME1312.SubServers.Host.Network.PacketOut;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.UUID;

/**
 * Create Server Packet
 */
public class PacketCreateServer implements PacketIn, PacketOut {
    private static HashMap<String, JSONCallback[]> callbacks = new HashMap<String, JSONCallback[]>();
    private UUID player;
    private String name;
    private String host;
    private SubCreator.ServerType type;
    private Version version;
    private int port;
    private int ram;
    private String id;

    /**
     * New PacketCreateServer (In)
     */
    public PacketCreateServer() {}

    /**
     * New PacketCreateServer (Out)
     *
     * @param player Player Creating
     * @param name Server Name
     * @param host Host to use
     * @param type Server Type
     * @param version Server Version
     * @param port Server Port
     * @param memory Server Memory
     * @param callback Callbacks
     */
    public PacketCreateServer(UUID player, String name, String host, SubCreator.ServerType type, Version version, int port, int memory, JSONCallback... callback) {
        if (Util.isNull(name, host, type, version, port, memory, callback)) throw new NullPointerException();
        this.player = player;
        this.name = name;
        this.host = host;
        this.type = type;
        this.version = version;
        this.port = port;
        this.ram = memory;
        this.id = Util.getNew(callbacks.keySet(), UUID::randomUUID).toString();
        callbacks.put(id, callback);
    }

    @Override
    public JSONObject generate() {
        JSONObject json = new JSONObject();
        json.put("id", id);
        if (player != null) json.put("player", player.toString());
        JSONObject creator = new JSONObject();
        creator.put("name", name);
        creator.put("host", host);
        creator.put("type", type.toString());
        creator.put("version", version.toString());
        creator.put("port", port);
        creator.put("ram", ram);
        json.put("creator", creator);
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

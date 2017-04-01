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
public class PacketExDeleteServer implements PacketIn, PacketOut {
    private static HashMap<String, JSONCallback[]> callbacks = new HashMap<String, JSONCallback[]>();
    private String name;
    private SubCreator.ServerType type;
    private Version version;
    private int port;
    private int ram;
    private UUID log;
    private String id = null;

    /**
     * New PacketExCreateServer
     */
    public PacketExDeleteServer() {}

    /**
     * New PacketExCreateServer (Out)
     *
     * @param name Server Name
     * @param type Server Type
     * @param version Server Version
     * @param memory Server Memory Amount (in MB)
     * @param port Server Port Number
     * @param callback Callbacks
     */
    public PacketExDeleteServer(String name, SubCreator.ServerType type, Version version, int memory, int port, UUID log, JSONCallback... callback) {
        if (Util.isNull(name, type, version, port, memory, log, callback)) throw new NullPointerException();
        this.name = name;
        this.type = type;
        this.version = version;
        this.port = port;
        this.ram = memory;
        this.log = log;
        this.id = Util.getNew(callbacks.keySet(), UUID::randomUUID).toString();
        callbacks.put(id, callback);
    }

    @Override
    public JSONObject generate() {
        if (id == null) {
            return null;
        } else {
            JSONObject json = new JSONObject();
            json.put("id", id);
            JSONObject creator = new JSONObject();
            creator.put("name", name);
            creator.put("type", type.toString());
            creator.put("version", version.toString());
            creator.put("port", port);
            creator.put("ram", ram);
            creator.put("log", log.toString());
            json.put("creator", creator);
            return json;
        }
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
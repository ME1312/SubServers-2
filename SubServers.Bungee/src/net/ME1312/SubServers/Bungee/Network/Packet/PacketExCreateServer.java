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
public class PacketExCreateServer implements PacketIn, PacketOut {
    private static HashMap<String, JSONCallback[]> callbacks = new HashMap<String, JSONCallback[]>();
    private String name;
    private SubCreator.ServerTemplate template;
    private Version version;
    private int port;
    private UUID log;
    private String id = null;

    /**
     * New PacketExCreateServer
     */
    public PacketExCreateServer(String name) {
        this.name = name;
    }

    /**
     * New PacketExCreateServer (Out)
     *
     * @param name Server Name
     * @param template Server Template
     * @param version Server Version
     * @param port Server Port Number
     * @param log Log Address
     * @param callback Callbacks
     */
    public PacketExCreateServer(String name, SubCreator.ServerTemplate template, Version version, int port, UUID log, JSONCallback... callback) {
        if (Util.isNull(name, template, version, port, log, callback)) throw new NullPointerException();
        this.name = name;
        this.template = template;
        this.version = version;
        this.port = port;
        this.log = log;
        this.id = Util.getNew(callbacks.keySet(), UUID::randomUUID).toString();
        callbacks.put(id, callback);
    }

    @Override
    public JSONObject generate() {
        if (id == null) {
            JSONObject json = new JSONObject();
            json.put("thread", name);
            return json;
        } else {
            JSONObject json = new JSONObject();
            json.put("id", id);
            JSONObject creator = new JSONObject();
            creator.put("name", name);
            creator.put("template", template.getName());
            creator.put("version", version.toString());
            creator.put("port", port);
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
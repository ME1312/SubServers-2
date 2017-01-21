package net.ME1312.SubServers.Client.Bukkit.Network.Packet;

import net.ME1312.SubServers.Client.Bukkit.Graphic.InternalRenderer;
import net.ME1312.SubServers.Client.Bukkit.Graphic.UIRenderer;
import net.ME1312.SubServers.Client.Bukkit.Library.JSONCallback;
import net.ME1312.SubServers.Client.Bukkit.Library.Version.Version;
import net.ME1312.SubServers.Client.Bukkit.Network.PacketIn;
import net.ME1312.SubServers.Client.Bukkit.Network.PacketOut;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.UUID;

public class PacketCreateServer implements PacketIn, PacketOut {
    private static HashMap<String, JSONCallback> callbacks = new HashMap<String, JSONCallback>();
    public enum ServerType {
        SPIGOT,
        VANILLA,
        SPONGE,;

        @Override
        public String toString() {
            return super.toString().substring(0, 1).toUpperCase()+super.toString().substring(1).toLowerCase();
        }
    }
    private UUID player;
    private String name;
    private String host;
    private ServerType type;
    private Version version;
    private int port;
    private int ram;
    private String id;

    public PacketCreateServer() {}
    public PacketCreateServer(String name, String host, ServerType type, Version version, int port, int memory, String id, JSONCallback callback) {
        this.player = null;
        this.name = name;
        this.host = host;
        this.type = type;
        this.version = version;
        this.port = port;
        this.ram = memory;
        this.id = id;
        callbacks.put(id, callback);
    }
    public PacketCreateServer(UUID player, String name, String host, ServerType type, Version version, int port, int memory, String id, JSONCallback callback) {
        this.player = player;
        this.name = name;
        this.host = host;
        this.type = type;
        this.version = version;
        this.port = port;
        this.ram = memory;
        this.id = id;
        callbacks.put(id, callback);
    }
    public PacketCreateServer(UIRenderer.CreatorOptions options, String id, JSONCallback callback) {
        this.player = null;
        this.name = options.getName();
        this.host = options.getHost();
        this.type = options.getType();
        this.version = options.getVersion();
        this.port = options.getPort();
        this.ram = options.getMemory();
        this.id = id;
        callbacks.put(id, callback);

    }
    public PacketCreateServer(UUID player, UIRenderer.CreatorOptions options, String id, JSONCallback callback) {
        this.player = player;
        this.name = options.getName();
        this.host = options.getHost();
        this.type = options.getType();
        this.version = options.getVersion();
        this.port = options.getPort();
        this.ram = options.getMemory();
        this.id = id;
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
        callbacks.get(data.getString("id")).run(data);
        callbacks.remove(data.getString("id"));
    }

    @Override
    public Version getVersion() {
        return new Version("2.11.0a");
    }
}

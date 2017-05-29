package net.ME1312.SubServers.Client.Bukkit.Network.Packet;

import net.ME1312.SubServers.Client.Bukkit.Graphic.UIRenderer;
import net.ME1312.SubServers.Client.Bukkit.Library.JSONCallback;
import net.ME1312.SubServers.Client.Bukkit.Library.Util;
import net.ME1312.SubServers.Client.Bukkit.Library.Version.Version;
import net.ME1312.SubServers.Client.Bukkit.Network.PacketIn;
import net.ME1312.SubServers.Client.Bukkit.Network.PacketOut;
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
    private String template;
    private Version version;
    private int port;
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
     * @param template Server Template
     * @param version Server Version
     * @param port Server Port
     * @param callback Callbacks
     */
    public PacketCreateServer(UUID player, String name, String host, String template, Version version, int port, JSONCallback... callback) {
        if (Util.isNull(name, host, template, version, port, callback)) throw new NullPointerException();
        this.player = player;
        this.name = name;
        this.host = host;
        this.template = template;
        this.version = version;
        this.port = port;
        this.id = Util.getNew(callbacks.keySet(), UUID::randomUUID).toString();
        callbacks.put(id, callback);
    }

    /**
     * New PacketCreateServer (Out)
     *
     * @param player Player Creating
     * @param options Creator UI Options
     * @param callback Callbacks
     */
    public PacketCreateServer(UUID player, UIRenderer.CreatorOptions options, JSONCallback... callback) {
        if (Util.isNull(options, callback)) throw new NullPointerException();
        this.player = player;
        this.name = options.getName();
        this.host = options.getHost();
        this.template = options.getTemplate();
        this.version = options.getVersion();
        this.port = options.getPort();
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
        creator.put("template", template);
        creator.put("version", version.toString());
        creator.put("port", port);
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

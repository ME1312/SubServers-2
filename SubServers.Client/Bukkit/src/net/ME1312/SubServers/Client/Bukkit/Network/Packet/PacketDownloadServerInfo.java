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
 * Download Server Info Packet
 */
public class PacketDownloadServerInfo implements PacketIn, PacketOut {
    private static HashMap<String, JSONCallback[]> callbacks = new HashMap<String, JSONCallback[]>();
    private String server;
    private String id;

    /**
     * New PacketDownloadServerInfo (In)
     */
    public PacketDownloadServerInfo() {}

    /**
     * New PacketDownloadServerInfo (Out)
     *
     * @param server Server name
     * @param callback Callbacks
     */
    public PacketDownloadServerInfo(String server, JSONCallback... callback) {
        if (Util.isNull(server, callback)) throw new NullPointerException();
        this.server = server;
        this.id = Util.getNew(callbacks.keySet(), UUID::randomUUID).toString();
        callbacks.put(id, callback);
    }

    @Override
    public JSONObject generate() {
        JSONObject json = new JSONObject();
        json.put("id", id);
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

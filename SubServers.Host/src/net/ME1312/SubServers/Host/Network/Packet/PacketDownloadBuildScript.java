package net.ME1312.SubServers.Host.Network.Packet;

import net.ME1312.SubServers.Host.Library.JSONCallback;
import net.ME1312.SubServers.Host.Library.Util;
import net.ME1312.SubServers.Host.Library.Version.Version;
import net.ME1312.SubServers.Host.Network.PacketIn;
import net.ME1312.SubServers.Host.Network.PacketOut;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.UUID;

/**
 * Download Build Script Packet
 */
public class PacketDownloadBuildScript implements PacketIn, PacketOut {
    private static HashMap<String, JSONCallback[]> callbacks = new HashMap<String, JSONCallback[]>();
    private String id;

    /**
     * New PacketDownloadBuildScript (In)
     */
    public PacketDownloadBuildScript() {}

    /**
     * New PacketDownloadBuildScript (Out)
     *
     * @param callback Callbacks
     */
    public PacketDownloadBuildScript(JSONCallback... callback) {
        if (Util.isNull((Object) callback)) throw new NullPointerException();
        this.id = Util.getNew(callbacks.keySet(), UUID::randomUUID).toString();
        callbacks.put(id, callback);
    }

    @Override
    public JSONObject generate() {
        JSONObject json = new JSONObject();
        json.put("id", id);
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
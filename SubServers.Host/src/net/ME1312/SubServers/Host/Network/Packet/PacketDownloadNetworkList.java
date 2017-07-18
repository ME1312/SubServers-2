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
 * Download Network List Packet
 */
public class PacketDownloadNetworkList implements PacketIn, PacketOut {
    private static HashMap<String, JSONCallback[]> callbacks = new HashMap<String, JSONCallback[]>();
    private String id;

    /**
     * New PacketDownloadNetworkList (In)
     */
    public PacketDownloadNetworkList() {}

    /**
     * New PacketDownloadNetworkList (Out)
     *
     * @param callback Callbacks
     */
    public PacketDownloadNetworkList(JSONCallback... callback) {
        if (Util.isNull((Object) callback)) throw new NullPointerException();
        this.id = Util.getNew(callbacks.keySet(), UUID::randomUUID).toString();
        callbacks.put(id, callback);
    }
    @Override
    public JSONObject generate() {
        if (id != null) {
            JSONObject json = new JSONObject();
            json.put("id", id);
            return json;
        } else {
            return null;
        }
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

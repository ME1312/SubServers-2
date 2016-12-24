package net.ME1312.SubServers.Client.Bukkit.Network.Packet;

import net.ME1312.SubServers.Client.Bukkit.Library.JSONCallback;
import net.ME1312.SubServers.Client.Bukkit.Library.Version.Version;
import net.ME1312.SubServers.Client.Bukkit.Network.PacketIn;
import net.ME1312.SubServers.Client.Bukkit.Network.PacketOut;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PacketDownloadPlayerList implements PacketIn, PacketOut {
    private static HashMap<String, JSONCallback> callbacks = new HashMap<String, JSONCallback>();
    private String id;

    public PacketDownloadPlayerList() {}
    public PacketDownloadPlayerList(String id, JSONCallback callback) {
        this.id = id;
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
        callbacks.get(data.getString("id")).run(data);
        callbacks.remove(data.getString("id"));
    }

    @Override
    public Version getVersion() {
        return new Version("2.11.0a");
    }
}

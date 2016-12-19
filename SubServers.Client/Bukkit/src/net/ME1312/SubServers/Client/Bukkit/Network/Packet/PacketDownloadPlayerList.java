package net.ME1312.SubServers.Client.Bukkit.Network.Packet;

import net.ME1312.SubServers.Client.Bukkit.Library.JSONCallback;
import net.ME1312.SubServers.Client.Bukkit.Library.Version.Version;
import net.ME1312.SubServers.Client.Bukkit.Network.PacketIn;
import net.ME1312.SubServers.Client.Bukkit.Network.PacketOut;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class PacketDownloadPlayerList implements PacketIn, PacketOut {
    private List<JSONCallback> callbacks = new ArrayList<JSONCallback>();

    public PacketDownloadPlayerList() {}

    @Override
    public JSONObject generate() {
        return null;
    }

    @Override
    public void execute(JSONObject data) {
        callbacks.get(0).run(data);
        callbacks.remove(0);
    }

    @Override
    public Version getVersion() {
        return new Version("2.11.0a");
    }

    public void callback(String id, JSONCallback callback) {
        callbacks.add(callback);
    }
}

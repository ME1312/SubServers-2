package net.ME1312.SubServers.Proxy.Network.Packet;

import net.ME1312.SubServers.Proxy.Library.Version.Version;
import net.ME1312.SubServers.Proxy.Network.Client;
import net.ME1312.SubServers.Proxy.Network.PacketIn;
import net.ME1312.SubServers.Proxy.Network.PacketOut;
import net.ME1312.SubServers.Proxy.SubPlugin;
import org.json.JSONObject;

public class PacketDownloadLang implements PacketIn, PacketOut {
    private SubPlugin plugin;
    private String id;

    public PacketDownloadLang(SubPlugin plugin) {
        this.plugin = plugin;
    }
    public PacketDownloadLang(SubPlugin plugin, String id) {
        this.plugin = plugin;
        this.id = id;
    }

    @Override
    public JSONObject generate() {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("Lang", plugin.api.getLang());
        return json;
    }

    @Override
    public void execute(Client client, JSONObject data) {
        client.sendPacket(new PacketDownloadLang(plugin, (data != null && data.keySet().contains("id"))?data.getString("id"):null));
    }

    @Override
    public Version getVersion() {
        return new Version("2.11.0a");
    }
}

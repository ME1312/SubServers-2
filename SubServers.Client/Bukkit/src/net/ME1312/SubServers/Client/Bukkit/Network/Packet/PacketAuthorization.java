package net.ME1312.SubServers.Client.Bukkit.Network.Packet;

import net.ME1312.SubServers.Client.Bukkit.Library.Version.Version;
import net.ME1312.SubServers.Client.Bukkit.Network.PacketIn;
import net.ME1312.SubServers.Client.Bukkit.Network.PacketOut;
import net.ME1312.SubServers.Client.Bukkit.SubPlugin;
import org.bukkit.Bukkit;
import org.json.JSONObject;

import java.io.IOException;

public class PacketAuthorization implements PacketIn, PacketOut {
    private SubPlugin plugin;

    public PacketAuthorization(SubPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public JSONObject generate() {
        JSONObject json = new JSONObject();
        json.put("password", plugin.pluginconf.get().getSection("Settings").getSection("SubData").getString("Password"));
        return json;
    }

    @Override
    public void execute(JSONObject data) {
        try {
            if (data.getInt("r") == 0) {
                plugin.subdata.sendPacket(new PacketLinkServer(plugin));
                plugin.subdata.sendPacket(new PacketDownloadLang());
            } else {
                Bukkit.getLogger().info("SubServers > Could not authorize SubData connection: " + data.getString("m"));
                plugin.subdata.destroy(false);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Version getVersion() {
        return new Version("2.11.0a");
    }
}

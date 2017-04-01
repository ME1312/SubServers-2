package net.ME1312.SubServers.Client.Bukkit.Network.Packet;

import net.ME1312.SubServers.Client.Bukkit.Library.Util;
import net.ME1312.SubServers.Client.Bukkit.Library.Version.Version;
import net.ME1312.SubServers.Client.Bukkit.Network.PacketIn;
import net.ME1312.SubServers.Client.Bukkit.Network.PacketOut;
import net.ME1312.SubServers.Client.Bukkit.SubPlugin;
import org.bukkit.Bukkit;
import org.json.JSONObject;

/**
 * Link Server Packet
 */
public class PacketLinkServer implements PacketIn, PacketOut {
    private SubPlugin plugin;

    /**
     * New PacketLinkServer
     *
     * @param plugin SubServers.Client
     */
    public PacketLinkServer(SubPlugin plugin) {
        if (Util.isNull(plugin)) throw new NullPointerException();
        this.plugin = plugin;
    }

    @Override
    public JSONObject generate() {
        JSONObject json = new JSONObject();
        json.put("name", plugin.subdata.getName());
        return json;
    }

    @Override
    public void execute(JSONObject data) {
        if (data.getInt("r") == 0) {
            plugin.subdata.sendPacket(new PacketDownloadLang());
        } else {
            Bukkit.getLogger().info("SubData > Could not link name with server: " + data.getString("m"));
        }
    }

    @Override
    public Version getVersion() {
        return new Version("2.11.0a");
    }
}

package net.ME1312.SubServers.Host.Network.Packet;

import net.ME1312.SubServers.Host.Library.Util;
import net.ME1312.SubServers.Host.Library.Version.Version;
import net.ME1312.SubServers.Host.Network.PacketIn;
import net.ME1312.SubServers.Host.Network.PacketOut;
import net.ME1312.SubServers.Host.SubServers;
import org.json.JSONObject;

import java.io.IOException;

public final class PacketAuthorization implements PacketIn, PacketOut {
    private SubServers plugin;

    public PacketAuthorization(SubServers plugin) {
        if (Util.isNull(plugin)) throw new NullPointerException();
        this.plugin = plugin;
    }

    @Override
    public JSONObject generate() {
        JSONObject json = new JSONObject();
        json.put("password", plugin.config.get().getSection("Settings").getSection("SubData").getString("Password"));
        return json;
    }

    @Override
    public void execute(JSONObject data) {
        try {
            if (data.getInt("r") == 0) {
                //plugin.subdata.sendPacket(new PacketLinkServer(plugin));
                plugin.subdata.sendPacket(new PacketDownloadLang());
            } else {
                plugin.log.info("SubServers > Could not authorize SubData connection: " + data.getString("m"));
                plugin.subdata.destroy(false);
            }
        } catch (IOException e) {
            plugin.log.error(e);
        }
    }

    @Override
    public Version getVersion() {
        return new Version("2.11.0a");
    }
}

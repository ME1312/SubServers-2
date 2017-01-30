package net.ME1312.SubServers.Host.Network.Packet;

import net.ME1312.SubServers.Host.Library.Config.YAMLSection;
import net.ME1312.SubServers.Host.Library.Util;
import net.ME1312.SubServers.Host.Library.Version.Version;
import net.ME1312.SubServers.Host.Network.PacketIn;
import net.ME1312.SubServers.Host.Network.PacketOut;
import net.ME1312.SubServers.Host.SubServers;
import org.json.JSONObject;

import java.util.Calendar;

public class PacketDownloadLang implements PacketIn, PacketOut {
    private SubServers plugin;

    public PacketDownloadLang() {};

    public PacketDownloadLang(SubServers plugin) {
        if (Util.isNull(plugin)) throw new NullPointerException();
        this.plugin = plugin;
    }

    @Override
    public JSONObject generate() {
        return null;
    }

    @Override
    public void execute(JSONObject data) {
        data.put("Updated", Calendar.getInstance().getTime().getTime());
        plugin.lang = new YAMLSection(data);
        plugin.log.info("SubData > Lang Settings Downloaded");
    }

    @Override
    public Version getVersion() {
        return new Version("2.11.0a");
    }
}

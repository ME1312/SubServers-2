package net.ME1312.SubServers.Host.Network.Packet;

import net.ME1312.SubServers.Host.Library.Config.YAMLSection;
import net.ME1312.SubServers.Host.Library.Log.Logger;
import net.ME1312.SubServers.Host.Library.Util;
import net.ME1312.SubServers.Host.Library.Version.Version;
import net.ME1312.SubServers.Host.Network.PacketIn;
import net.ME1312.SubServers.Host.Network.PacketOut;
import net.ME1312.SubServers.Host.Network.SubDataClient;
import net.ME1312.SubServers.Host.SubServers;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.util.Calendar;

public class PacketDownloadLang implements PacketIn, PacketOut {
    private SubServers plugin;
    private Logger log = null;

    public PacketDownloadLang() {};

    public PacketDownloadLang(SubServers plugin) {
        if (Util.isNull(plugin)) throw new NullPointerException();
        this.plugin = plugin;
        try {
            Field f = SubDataClient.class.getDeclaredField("log");
            f.setAccessible(true);
            this.log = (Logger) f.get(null);
            f.setAccessible(false);
        } catch (IllegalAccessException | NoSuchFieldException e) {}
    }

    @Override
    public JSONObject generate() {
        return null;
    }

    @Override
    public void execute(JSONObject data) {
        data.put("Updated", Calendar.getInstance().getTime().getTime());
        plugin.lang = new YAMLSection(data);
        log.info("Lang Settings Downloaded");
    }

    @Override
    public Version getVersion() {
        return new Version("2.11.0a");
    }
}

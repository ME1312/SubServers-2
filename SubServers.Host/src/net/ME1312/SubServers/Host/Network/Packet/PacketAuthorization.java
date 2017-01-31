package net.ME1312.SubServers.Host.Network.Packet;

import net.ME1312.SubServers.Host.Library.Log.Logger;
import net.ME1312.SubServers.Host.Library.Util;
import net.ME1312.SubServers.Host.Library.Version.Version;
import net.ME1312.SubServers.Host.Network.PacketIn;
import net.ME1312.SubServers.Host.Network.PacketOut;
import net.ME1312.SubServers.Host.Network.SubDataClient;
import net.ME1312.SubServers.Host.SubServers;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Field;

public final class PacketAuthorization implements PacketIn, PacketOut {
    private SubServers plugin;
    private Logger log = null;

    public PacketAuthorization(SubServers plugin) {
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
                log.info("SubServers > Could not authorize SubData connection: " + data.getString("m"));
                plugin.subdata.destroy(false);
            }
        } catch (IOException e) {
            log.error(e);
        }
    }

    @Override
    public Version getVersion() {
        return new Version("2.11.0a");
    }
}

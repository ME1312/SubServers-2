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

/**
 * Authorization Packet
 */
public final class PacketAuthorization implements PacketIn, PacketOut {
    private SubServers host;
    private Logger log = null;

    /**
     * New PacketAuthorization
     *
     * @param host SubServers.Host
     */
    public PacketAuthorization(SubServers host) {
        if (Util.isNull(host)) throw new NullPointerException();
        this.host = host;
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
        json.put("password", host.config.get().getSection("Settings").getSection("SubData").getString("Password"));
        return json;
    }

    @Override
    public void execute(JSONObject data) {
        try {
            if (data.getInt("r") == 0) {
                host.subdata.sendPacket(new PacketLinkExHost(host));
                host.subdata.sendPacket(new PacketDownloadLang());
                host.subdata.sendPacket(new PacketOutExRequestQueue());
            } else {
                log.info.println("SubServers > Could not authorize SubData connection: " + data.getString("m"));
                host.subdata.destroy(false);
            }
        } catch (IOException e) {
            log.error.println(e);
        }
    }

    @Override
    public Version getVersion() {
        return new Version("2.11.0a");
    }
}

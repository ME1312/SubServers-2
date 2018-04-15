package net.ME1312.SubServers.Host.Network.Packet;

import net.ME1312.SubServers.Host.Library.Config.YAMLSection;
import net.ME1312.SubServers.Host.Library.Log.Logger;
import net.ME1312.SubServers.Host.Library.NamedContainer;
import net.ME1312.SubServers.Host.Library.Util;
import net.ME1312.SubServers.Host.Library.Version.Version;
import net.ME1312.SubServers.Host.Network.PacketIn;
import net.ME1312.SubServers.Host.Network.PacketOut;
import net.ME1312.SubServers.Host.Network.SubDataClient;
import net.ME1312.SubServers.Host.ExHost;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Authorization Packet
 */
public final class PacketAuthorization implements PacketIn, PacketOut {
    private ExHost host;
    private Logger log = null;

    /**
     * New PacketAuthorization
     *
     * @param host SubServers.Host
     */
    public PacketAuthorization(ExHost host) {
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
    public YAMLSection generate() {
        YAMLSection json = new YAMLSection();
        json.set("password", host.config.get().getSection("Settings").getSection("SubData").getString("Password"));
        return json;
    }

    @Override
    public void execute(YAMLSection data) {
        try {
            if (data.getInt("r") == 0) {
                try {
                    Method m = SubDataClient.class.getDeclaredMethod("sendPacket", NamedContainer.class);
                    m.setAccessible(true);
                    m.invoke(host.subdata, new NamedContainer<String, PacketOut>(null, new PacketLinkExHost(host)));
                    m.setAccessible(false);
                } catch (Exception e) {}
            } else {
                log.info.println("SubServers > Could not authorize SubData connection: " + data.getRawString("m"));
                host.subdata.destroy(0);
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

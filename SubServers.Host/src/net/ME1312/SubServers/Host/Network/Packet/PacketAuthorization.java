package net.ME1312.SubServers.Host.Network.Packet;

import net.ME1312.Galaxi.Library.Config.YAMLSection;
import net.ME1312.Galaxi.Library.Log.Logger;
import net.ME1312.Galaxi.Library.NamedContainer;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.Galaxi.Library.Version.Version;
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
    private String password;

    public PacketAuthorization(ExHost host, String password) {
        if (Util.isNull(host)) throw new NullPointerException();
        this.host = host;
        this.password = password;
    }

    @Override
    public YAMLSection generate() {
        YAMLSection json = new YAMLSection();
        json.set("password", password);
        return json;
    }

    @Override
    public void execute(YAMLSection data) {
        try {
            if (data.getInt("r") == 0) {
                Util.isException(() -> Util.reflect(SubDataClient.class.getDeclaredMethod("sendPacket", NamedContainer.class), host.subdata, new NamedContainer<String, PacketOut>(null, new PacketLinkExHost(host))));
            } else {
                log.info.println("Could not authorize SubData connection: " + data.getRawString("m"));
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

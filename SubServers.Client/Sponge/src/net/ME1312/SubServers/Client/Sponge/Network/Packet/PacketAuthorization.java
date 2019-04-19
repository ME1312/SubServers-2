package net.ME1312.SubServers.Client.Sponge.Network.Packet;

import net.ME1312.SubServers.Client.Sponge.Library.Config.YAMLSection;
import net.ME1312.SubServers.Client.Sponge.Library.NamedContainer;
import net.ME1312.SubServers.Client.Sponge.Library.Util;
import net.ME1312.SubServers.Client.Sponge.Library.Version.Version;
import net.ME1312.SubServers.Client.Sponge.Network.PacketIn;
import net.ME1312.SubServers.Client.Sponge.Network.PacketOut;
import net.ME1312.SubServers.Client.Sponge.Network.SubDataClient;
import net.ME1312.SubServers.Client.Sponge.SubPlugin;
import org.slf4j.Logger;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class PacketAuthorization implements PacketIn, PacketOut {
    private SubPlugin plugin;
    private Logger log = null;
    private String password;

    public PacketAuthorization(SubPlugin plugin, String password) {
        if (Util.isNull(plugin)) throw new NullPointerException();
        this.plugin = plugin;
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
                Util.isException(() -> Util.reflect(SubDataClient.class.getDeclaredMethod("sendPacket", NamedContainer.class), plugin.subdata, new NamedContainer<String, PacketOut>(null, new PacketLinkServer(plugin))));
            } else {
                log.info("Could not authorize SubData connection: " + data.getRawString("m"));
                plugin.subdata.destroy(0);
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

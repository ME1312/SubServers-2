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

    public PacketAuthorization(SubPlugin plugin) {
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
    public YAMLSection generate() {
        YAMLSection json = new YAMLSection();
        json.set("password", plugin.config.get().getSection("Settings").getSection("SubData").getString("Password"));
        return json;
    }

    @Override
    public void execute(YAMLSection data) {
        try {
            if (data.getInt("r") == 0) {
                try {
                    Method m = SubDataClient.class.getDeclaredMethod("sendPacket", NamedContainer.class);
                    m.setAccessible(true);
                    m.invoke(plugin.subdata, new NamedContainer<String, PacketOut>(null, new PacketLinkServer(plugin)));
                    m.setAccessible(false);
                } catch (Exception e) {}
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

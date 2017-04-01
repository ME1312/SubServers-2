package net.ME1312.SubServers.Host.Network.Packet;

import net.ME1312.SubServers.Host.Library.Config.YAMLSection;
import net.ME1312.SubServers.Host.Library.Log.Logger;
import net.ME1312.SubServers.Host.Library.Version.Version;
import net.ME1312.SubServers.Host.Network.PacketIn;
import net.ME1312.SubServers.Host.Network.PacketOut;
import net.ME1312.SubServers.Host.Network.SubDataClient;
import net.ME1312.SubServers.Host.SubServers;
import org.json.JSONObject;

import java.lang.reflect.Field;

/**
 * External Host Configuration Packet
 */
public class PacketExConfigureHost implements PacketIn, PacketOut {
    private SubServers host;
    private Logger log = null;

    /**
     * New PacketExConfigureHost
     */
    public PacketExConfigureHost(SubServers host) {
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
        return null;
    }

    @Override
    public void execute(JSONObject data) {
        host.host = new YAMLSection(data.getJSONObject("host"));
        log.info.println("Host Settings Downloaded");
    }

    @Override
    public Version getVersion() {
        return new Version("2.11.0a");
    }
}

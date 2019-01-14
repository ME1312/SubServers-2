package net.ME1312.SubServers.Client.Sponge.Network.Packet;

import net.ME1312.SubServers.Client.Sponge.Library.Config.YAMLSection;
import net.ME1312.SubServers.Client.Sponge.Library.Util;
import net.ME1312.SubServers.Client.Sponge.Library.Version.Version;
import net.ME1312.SubServers.Client.Sponge.Network.PacketIn;
import net.ME1312.SubServers.Client.Sponge.Network.SubDataClient;
import net.ME1312.SubServers.Client.Sponge.SubPlugin;
import org.slf4j.Logger;

import java.lang.reflect.Field;

/**
 * Reload Packet
 */
public class PacketInReload implements PacketIn {
    private SubPlugin plugin;
    private Logger log = null;

    /**
     * New PacketOutReload
     *
     * @param plugin Plugin
     */
    public PacketInReload(SubPlugin plugin) {
        this.plugin = plugin;
        Util.isException(() -> Util.reflect(SubDataClient.class.getDeclaredField("log"), null));
    }

    @Override
    public void execute(YAMLSection data) {
        if (data != null && data.contains("m")) log.warn("Received request for a plugin reload: " + data.getString("m"));
        else log.warn("Received request for a plugin reload");
        try {
            plugin.reload(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public Version getVersion() {
        return new Version("2.11.0a");
    }
}

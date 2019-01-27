package net.ME1312.SubServers.Client.Sponge.Network.Packet;

import net.ME1312.SubServers.Client.Sponge.Library.Config.YAMLSection;
import net.ME1312.SubServers.Client.Sponge.Library.Util;
import net.ME1312.SubServers.Client.Sponge.Library.Version.Version;
import net.ME1312.SubServers.Client.Sponge.Network.PacketIn;
import net.ME1312.SubServers.Client.Sponge.Network.PacketOut;
import net.ME1312.SubServers.Client.Sponge.Network.SubDataClient;
import net.ME1312.SubServers.Client.Sponge.SubPlugin;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Link Server Packet
 */
public class PacketLinkServer implements PacketIn, PacketOut {
    private SubPlugin plugin;
    private Logger log = null;

    /**
     * New PacketLinkServer
     *
     * @param plugin SubServers.Client
     */
    public PacketLinkServer(SubPlugin plugin) {
        if (Util.isNull(plugin)) throw new NullPointerException();
        this.plugin = plugin;
        Util.isException(() -> this.log = Util.reflect(SubDataClient.class.getDeclaredField("log"), null));
    }

    @Override
    public YAMLSection generate() {
        YAMLSection json = new YAMLSection();
        if (plugin.subdata.getName() != null) json.set("name", plugin.subdata.getName());
        if (plugin.game.getServer().getBoundAddress().isPresent()) json.set("port", plugin.game.getServer().getBoundAddress().get());
        return json;
    }

    @Override
    public void execute(YAMLSection data) {
        if (data.getInt("r") == 0) {
            try {
                if (data.contains("n")) {
                    Util.reflect(SubDataClient.class.getDeclaredField("name"), plugin.subdata, data.getRawString("n"));
                }
                Util.reflect(SubDataClient.class.getDeclaredMethod("init"), plugin.subdata);
            } catch (Exception e) {}
        } else {
            log.info("Could not link name with server: " + data.getRawString("m"));
            try {
                if (data.getInt("r") == 2) {
                    if (!plugin.config.get().getSection("Settings").getSection("SubData").contains("Name")) {
                        plugin.config.get().getSection("Settings").getSection("SubData").set("Name", "");
                        plugin.config.save();
                    }
                    if (plugin.config.get().getSection("Settings").getSection("SubData").getRawString("Name").length() <= 0)
                        log.info("Use the server \"Name\" option to override auto-linking");
                }
            } catch (Exception e) {}
            plugin.disable(null);
        }
    }

    @Override
    public Version getVersion() {
        return new Version("2.11.0a");
    }
}

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

import java.lang.reflect.Field;
import java.util.Calendar;

/**
 * Download Lang Packet
 */
public class PacketDownloadLang implements PacketIn, PacketOut {
    private SubPlugin plugin;
    private Logger log = null;

    /**
     * New PacketDownloadLang (In)
     *
     * @param plugin SubServers.Client
     */
    public PacketDownloadLang(SubPlugin plugin) {
        if (Util.isNull(plugin)) throw new NullPointerException();
        this.plugin = plugin;
        Util.isException(() -> this.log = Util.reflect(SubDataClient.class.getDeclaredField("log"), null));
    }

    /**
     * New PacketDownloadLang (Out)
     */
    public PacketDownloadLang() {}

    @Override
    public YAMLSection generate() {
        return null;
    }

    @Override
    public void execute(YAMLSection data) {
        try {
            Util.reflect(SubPlugin.class.getDeclaredField("lang"), plugin, new NamedContainer<>(Calendar.getInstance().getTime().getTime(), data.getSection("Lang").get()));
            log.info("Lang Settings Downloaded");
        } catch (IllegalAccessException | NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Version getVersion() {
        return new Version("2.11.0a");
    }
}

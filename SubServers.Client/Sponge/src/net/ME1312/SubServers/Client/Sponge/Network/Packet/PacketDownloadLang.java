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
        try {
            Field f = SubDataClient.class.getDeclaredField("log");
            f.setAccessible(true);
            this.log = (Logger) f.get(null);
            f.setAccessible(false);
        } catch (IllegalAccessException | NoSuchFieldException e) {}
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
            Field f = SubPlugin.class.getDeclaredField("lang");
            f.setAccessible(true);
            f.set(plugin, new NamedContainer<>(Calendar.getInstance().getTime().getTime(), data.getSection("Lang").get()));
            f.setAccessible(false);
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

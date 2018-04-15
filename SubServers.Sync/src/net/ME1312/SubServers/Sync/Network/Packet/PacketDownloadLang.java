package net.ME1312.SubServers.Sync.Network.Packet;

import net.ME1312.SubServers.Sync.Library.Config.YAMLSection;
import net.ME1312.SubServers.Sync.Library.NamedContainer;
import net.ME1312.SubServers.Sync.Library.Util;
import net.ME1312.SubServers.Sync.Library.Version.Version;
import net.ME1312.SubServers.Sync.Network.PacketIn;
import net.ME1312.SubServers.Sync.Network.PacketOut;
import net.ME1312.SubServers.Sync.SubPlugin;

import java.lang.reflect.Field;
import java.util.Calendar;

/**
 * Download Lang Packet
 */
public class PacketDownloadLang implements PacketIn, PacketOut {
    private SubPlugin plugin;

    /**
     * New PacketDownloadLang (In)
     */
    public PacketDownloadLang() {}

    /**
     * New PacketDownloadLang (Out)
     *
     * @param plugin SubServers.Client
     */
    public PacketDownloadLang(SubPlugin plugin) {
        if (Util.isNull(plugin)) throw new NullPointerException();
        this.plugin = plugin;
    }

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
            System.out.println("SubData > Lang Settings Downloaded");
        } catch (IllegalAccessException | NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Version getVersion() {
        return new Version("2.11.0a");
    }
}

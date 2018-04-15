package net.ME1312.SubServers.Client.Bukkit.Network.Packet;

import net.ME1312.SubServers.Client.Bukkit.Library.Config.YAMLSection;
import net.ME1312.SubServers.Client.Bukkit.Library.NamedContainer;
import net.ME1312.SubServers.Client.Bukkit.Library.Util;
import net.ME1312.SubServers.Client.Bukkit.Library.Version.Version;
import net.ME1312.SubServers.Client.Bukkit.Network.PacketIn;
import net.ME1312.SubServers.Client.Bukkit.Network.PacketOut;
import net.ME1312.SubServers.Client.Bukkit.SubPlugin;
import org.bukkit.Bukkit;

import java.lang.reflect.Field;
import java.util.Calendar;

/**
 * Download Lang Packet
 */
public class PacketDownloadLang implements PacketIn, PacketOut {
    private SubPlugin plugin;

    /**
     * New PacketDownloadLang (In)
     *
     * @param plugin SubServers.Client
     */
    public PacketDownloadLang(SubPlugin plugin) {
        if (Util.isNull(plugin)) throw new NullPointerException();
        this.plugin = plugin;
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
            Bukkit.getLogger().info("SubData > Lang Settings Downloaded");
        } catch (IllegalAccessException | NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Version getVersion() {
        return new Version("2.11.0a");
    }
}

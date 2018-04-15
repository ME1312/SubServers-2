package net.ME1312.SubServers.Client.Bukkit.Network.Packet;

import net.ME1312.SubServers.Client.Bukkit.Library.Config.YAMLSection;
import net.ME1312.SubServers.Client.Bukkit.Library.Version.Version;
import net.ME1312.SubServers.Client.Bukkit.Network.PacketIn;
import net.ME1312.SubServers.Client.Bukkit.SubPlugin;
import org.bukkit.Bukkit;

/**
 * Reload Packet
 */
public class PacketInReload implements PacketIn {
    private SubPlugin plugin;

    /**
     * New PacketOutReload
     *
     * @param plugin Plugin
     */
    public PacketInReload(SubPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(YAMLSection data) {
        if (data != null && data.contains("m")) Bukkit.getLogger().warning("SubData > Received request for a plugin reload: " + data.getString("m"));
        else Bukkit.getLogger().warning("SubData > Received request for a plugin reload");
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

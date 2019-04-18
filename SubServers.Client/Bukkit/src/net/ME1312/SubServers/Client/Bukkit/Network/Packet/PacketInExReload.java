package net.ME1312.SubServers.Client.Bukkit.Network.Packet;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.SubData.Client.Protocol.PacketObjectIn;
import net.ME1312.SubData.Client.SubDataClient;
import net.ME1312.SubServers.Client.Bukkit.SubPlugin;
import org.bukkit.Bukkit;

/**
 * Reload Packet
 */
public class PacketInExReload implements PacketObjectIn<Integer> {
    private SubPlugin plugin;

    /**
     * New PacketInExReload
     *
     * @param plugin Plugin
     */
    public PacketInExReload(SubPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void receive(SubDataClient client, ObjectMap<Integer> data) {
        if (data != null && data.contains(0x0000)) Bukkit.getLogger().warning("SubData > Received request for a plugin reload: " + data.getString(0x0000));
        else Bukkit.getLogger().warning("SubData > Received request for a plugin reload");
        try {
            plugin.reload(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int version() {
        return 0x0001;
    }
}

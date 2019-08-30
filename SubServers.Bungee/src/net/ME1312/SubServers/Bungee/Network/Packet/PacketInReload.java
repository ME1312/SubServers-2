package net.ME1312.SubServers.Bungee.Network.Packet;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.SubData.Server.Protocol.PacketObjectIn;
import net.ME1312.SubData.Server.SubDataClient;
import net.ME1312.SubServers.Bungee.SubProxy;

/**
 * Reload Packet
 */
public class PacketInReload implements PacketObjectIn<Integer> {
    private SubProxy plugin;

    /**
     * New PacketInReload
     *
     * @param plugin Plugin
     */
    public PacketInReload(SubProxy plugin) {
        this.plugin = plugin;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void receive(SubDataClient client, ObjectMap<Integer> data) throws Throwable {
        if (data != null && data.contains(0x0000)) plugin.getLogger().warning("SubData > Received request for a proxy reload: " + data.getString(0x0000));
        else plugin.getLogger().warning("SubData > Received request for a proxy reload");
        try {
            plugin.reload();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int version() {
        return 0x0001;
    }
}

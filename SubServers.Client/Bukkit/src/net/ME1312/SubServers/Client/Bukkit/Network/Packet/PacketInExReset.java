package net.ME1312.SubServers.Client.Bukkit.Network.Packet;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.SubData.Client.Protocol.PacketObjectIn;
import net.ME1312.SubData.Client.SubDataClient;
import org.bukkit.Bukkit;

/**
 * Reset Packet
 */
public class PacketInExReset implements PacketObjectIn<Integer> {

    @Override
    public void receive(SubDataClient client, ObjectMap<Integer> data) {
        if (data != null && data.contains(0x0000)) Bukkit.getLogger().warning("SubData > Received shutdown signal: " + data.getString(0x0000));
        else Bukkit.getLogger().warning("SubData > Received shutdown signal");
        Bukkit.shutdown();
    }

    @Override
    public int version() {
        return 0x0001;
    }
}

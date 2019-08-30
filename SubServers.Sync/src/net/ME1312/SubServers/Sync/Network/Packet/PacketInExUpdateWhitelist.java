package net.ME1312.SubServers.Sync.Network.Packet;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.SubData.Client.Protocol.PacketObjectIn;
import net.ME1312.SubData.Client.SubDataClient;
import net.ME1312.SubServers.Sync.ExProxy;

/**
 * Update External Whitelist Packet
 */
public class PacketInExUpdateWhitelist implements PacketObjectIn<Integer> {
    private ExProxy plugin;

    /**
     * New PacketInExUpdateWhitelist
     */
    public PacketInExUpdateWhitelist(ExProxy plugin) {
        this.plugin = plugin;
    }

    @Override
    public void receive(SubDataClient client, ObjectMap<Integer> data) {
        if (data.getBoolean(0x0001)) {
            plugin.servers.get(data.getRawString(0x0000)).whitelist(data.getUUID(0x0002));
        } else {
            plugin.servers.get(data.getRawString(0x0000)).unwhitelist(data.getUUID(0x0002));
        }
    }

    @Override
    public int version() {
        return 0x0001;
    }
}

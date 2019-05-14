package net.ME1312.SubServers.Sync.Network.Packet;

import net.ME1312.SubData.Client.Protocol.PacketIn;
import net.ME1312.SubData.Client.SubDataClient;
import net.ME1312.SubServers.Sync.SubAPI;

/**
 * Reset Packet
 */
public class PacketInExReset implements PacketIn {

    @Override
    public void receive(SubDataClient client) {
        SubAPI.getInstance().getInternals().servers.clear();
    }

    @Override
    public int version() {
        return 0x0001;
    }
}

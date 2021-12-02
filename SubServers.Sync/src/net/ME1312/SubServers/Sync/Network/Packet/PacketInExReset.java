package net.ME1312.SubServers.Sync.Network.Packet;

import net.ME1312.SubData.Client.Protocol.PacketIn;
import net.ME1312.SubData.Client.SubDataSender;
import net.ME1312.SubServers.Sync.SubAPI;

/**
 * Reset Packet
 */
public class PacketInExReset implements PacketIn {

    @Override
    public void receive(SubDataSender client) {
        SubAPI.getInstance().getInternals().servers.clear();
    }
}

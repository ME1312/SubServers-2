package net.ME1312.SubServers.Host.Network.Packet;

import net.ME1312.SubData.Client.Protocol.PacketOut;

/**
 * Queue Request Packet
 */
public class PacketOutExRequestQueue implements PacketOut {

    /**
     * New PacketOutExRequestQueue
     */
    public PacketOutExRequestQueue() {}


    @Override
    public int version() {
        return 0x0001;
    }
}

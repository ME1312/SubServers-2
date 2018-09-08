package net.ME1312.SubServers.Host.Network.Packet;

import net.ME1312.Galaxi.Library.Config.YAMLSection;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.SubServers.Host.Network.PacketOut;

/**
 * Queue Request Packet
 */
public class PacketOutExRequestQueue implements PacketOut {

    /**
     * New PacketOutExRequestQueue
     */
    public PacketOutExRequestQueue() {
    }

    @Override
    public YAMLSection generate() {
        return null;
    }

    @Override
    public Version getVersion() {
        return new Version("2.11.0a");
    }
}

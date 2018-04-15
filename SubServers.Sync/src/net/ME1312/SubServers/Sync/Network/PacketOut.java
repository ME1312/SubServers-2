package net.ME1312.SubServers.Sync.Network;

import net.ME1312.SubServers.Sync.Library.Config.YAMLSection;
import net.ME1312.SubServers.Sync.Library.Version.Version;

/**
 * PacketOut Layout Class
 */
public interface PacketOut {
    /**
     * Generate Packet Contents
     *
     * @return Packet Contents
     */
    YAMLSection generate() throws Throwable;

    /**
     * Get Packet Version
     *
     * @return Packet Version
     */
    Version getVersion();
}

package net.ME1312.SubServers.Host.Network;

import net.ME1312.SubServers.Host.Library.Config.YAMLSection;
import net.ME1312.SubServers.Host.Library.Version.Version;

/**
 * PacketOut Layout Class
 */
public interface PacketOut {
    /**
     * Generate JSON Packet Contents
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

package net.ME1312.SubServers.Host.Network;

import net.ME1312.Galaxi.Library.Config.YAMLSection;
import net.ME1312.Galaxi.Library.Version.Version;

/**
 * PacketIn Layout Class
 */
public interface PacketIn {
    /**
     * Execute Incoming Packet
     *
     * @param data Incoming Data
     */
    void execute(YAMLSection data) throws Throwable;

    /**
     * Get Packet Version
     *
     * @return Packet Version
     */
    Version getVersion();

    /**
     * Check Compatibility with oncoming packet
     *
     * @param version Version of oncoming packet
     * @return Compatibility Status
     */
    default boolean isCompatible(Version version) {
        return getVersion().equals(version);
    }
}

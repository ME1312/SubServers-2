package net.ME1312.SubServers.Sync.Network;

import net.ME1312.SubServers.Sync.Library.Version.Version;
import org.json.JSONObject;

/**
 * PacketOut Layout Class
 */
public interface PacketOut {
    /**
     * Generate JSON Packet Contents
     *
     * @return Packet Contents
     */
    JSONObject generate() throws Throwable;

    /**
     * Get Packet Version
     *
     * @return Packet Version
     */
    Version getVersion();
}

package net.ME1312.SubServers.Client.Bukkit.Network;

import net.ME1312.SubServers.Client.Bukkit.Library.Version.Version;
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

package net.ME1312.SubServers.Client.Bukkit.Network;

import net.ME1312.SubServers.Client.Bukkit.Library.Version.Version;
import org.json.JSONObject;

/**
 * PacketIn Layout Class
 */
public interface PacketIn {
    /**
     * Execute Incoming Packet
     *
     * @param data Incoming Data
     */
    void execute(JSONObject data);

    /**
     * Get Packet Version
     *
     * @return Packet Version
     */
    Version getVersion();
}

package net.ME1312.SubServers.Proxy.Network;

import net.ME1312.SubServers.Proxy.Library.Version.Version;
import org.json.JSONObject;

/**
 * PacketIn Layout Class
 *
 * @author ME1312
 */
public interface PacketIn {
    /**
     * Execute Incoming Packet
     *
     * @param client Client Accepting
     * @param data Incoming Data
     */
    void execute(Client client, JSONObject data);

    /**
     * Get Packet Version
     *
     * @return Packet Version
     */
    Version getVersion();
}

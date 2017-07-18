package net.ME1312.SubServers.Bungee.Network;

import net.ME1312.SubServers.Bungee.Library.Version.Version;
import org.json.JSONObject;

/**
 * PacketIn Layout Class
 */
public interface PacketIn {
    /**
     * Execute Incoming Packet
     *
     * @param client Client Accepting
     * @param data Incoming Data
     */
    void execute(Client client, JSONObject data) throws Throwable;

    /**
     * Get Packet Version
     *
     * @return Packet Version
     */
    Version getVersion();
}

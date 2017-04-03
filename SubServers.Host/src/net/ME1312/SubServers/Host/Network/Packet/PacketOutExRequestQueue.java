package net.ME1312.SubServers.Host.Network.Packet;

import net.ME1312.SubServers.Host.Library.Version.Version;
import net.ME1312.SubServers.Host.Network.PacketOut;
import org.json.JSONObject;

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
    public JSONObject generate() {
        return null;
    }

    @Override
    public Version getVersion() {
        return new Version("2.11.0a");
    }
}

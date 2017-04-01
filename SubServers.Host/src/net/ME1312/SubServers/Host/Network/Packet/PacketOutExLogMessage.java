package net.ME1312.SubServers.Host.Network.Packet;

import net.ME1312.SubServers.Host.Library.Version.Version;
import net.ME1312.SubServers.Host.Network.PacketOut;
import org.json.JSONObject;

import java.util.UUID;

/**
 * Message Log External Host Packet
 */
public class PacketOutExLogMessage implements PacketOut {
    private UUID address;
    private String line;

    /**
     * New PacketInExLogMessage (Out)
     */
    public PacketOutExLogMessage(UUID address, String line) {
        this.address = address;
        this.line = line;
    }

    @Override
    public JSONObject generate() {
        JSONObject json = new JSONObject();
        json.put("h", address.toString());
        json.put("m", line);
        return json;
    }

    @Override
    public Version getVersion() {
        return new Version("2.11.0a");
    }
}

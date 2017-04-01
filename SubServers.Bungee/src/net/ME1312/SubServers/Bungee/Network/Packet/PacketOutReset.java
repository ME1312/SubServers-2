package net.ME1312.SubServers.Bungee.Network.Packet;

import net.ME1312.SubServers.Bungee.Library.Util;
import net.ME1312.SubServers.Bungee.Library.Version.Version;
import net.ME1312.SubServers.Bungee.Network.PacketOut;
import org.json.JSONObject;

/**
 * Reset Packet
 */
public class PacketOutReset implements PacketOut {
    private String message;

    /**
     * New PacketOutReset
     *
     * @param message Message
     */
    public PacketOutReset(String message) {
        if (Util.isNull(message)) throw new NullPointerException();
        this.message = message;
    }

    @Override
    public JSONObject generate() {
        if (message == null) {
            return null;
        } else {
            JSONObject json = new JSONObject();
            json.put("m", message);
            return json;
        }
    }

    @Override
    public Version getVersion() {
        return new Version("2.11.0a");
    }
}

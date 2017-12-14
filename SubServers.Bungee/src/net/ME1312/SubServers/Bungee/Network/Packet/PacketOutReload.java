package net.ME1312.SubServers.Bungee.Network.Packet;

import net.ME1312.SubServers.Bungee.Library.Util;
import net.ME1312.SubServers.Bungee.Library.Version.Version;
import net.ME1312.SubServers.Bungee.Network.PacketOut;
import org.json.JSONObject;

/**
 * Reload Packet
 */
public class PacketOutReload implements PacketOut {
    private String message;

    /**
     * New PacketOutReload
     *
     * @param message Message
     */
    public PacketOutReload(String message) {
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

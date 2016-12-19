package net.ME1312.SubServers.Proxy.Network.Packet;

import net.ME1312.SubServers.Proxy.Library.Version.Version;
import net.ME1312.SubServers.Proxy.Network.PacketOut;
import org.json.JSONObject;

public class PacketOutShutdown implements PacketOut {
    private String message;

    public PacketOutShutdown(String message) {
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

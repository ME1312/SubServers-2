package net.ME1312.SubServers.Proxy.Network.Packet;

import net.ME1312.SubServers.Proxy.Library.Version.Version;
import net.ME1312.SubServers.Proxy.Network.Client;
import net.ME1312.SubServers.Proxy.Network.PacketIn;
import net.ME1312.SubServers.Proxy.Network.PacketOut;
import net.ME1312.SubServers.Proxy.SubPlugin;
import org.json.JSONObject;

public class PacketAuthorization implements PacketIn, PacketOut {
    private SubPlugin plugin;
    private int response;
    private String message;

    public PacketAuthorization(SubPlugin plugin) {
        this.plugin = plugin;
    }

    public PacketAuthorization(int response, String message) {
        this.response = response;
        this.message = message;
    }

    @Override
    public JSONObject generate() {
        JSONObject json = new JSONObject();
        json.put("r", response);
        json.put("m", message);
        return json;
    }

    @Override
    public void execute(Client client, JSONObject data) {
        try {
            if (data.getString("password").equals(plugin.config.get().getSection("Settings").getSection("SubData").getString("Password"))) {
                client.authorize();
                client.sendPacket(new PacketAuthorization(0, "Successfully Logged in"));
            } else {
                client.sendPacket(new PacketAuthorization(2, "Invalid Password"));
            }
        } catch (Exception e) {
            client.sendPacket(new PacketAuthorization(1, e.getClass().getCanonicalName() + ": " + e.getMessage()));
        }
    }

    @Override
    public Version getVersion() {
        return new Version("2.11.0a");
    }
}

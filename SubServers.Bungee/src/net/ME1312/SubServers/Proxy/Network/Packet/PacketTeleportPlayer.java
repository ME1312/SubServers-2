package net.ME1312.SubServers.Proxy.Network.Packet;

import net.ME1312.SubServers.Proxy.Host.Server;
import net.ME1312.SubServers.Proxy.Library.Version.Version;
import net.ME1312.SubServers.Proxy.Network.Client;
import net.ME1312.SubServers.Proxy.Network.PacketIn;
import net.ME1312.SubServers.Proxy.Network.PacketOut;
import net.ME1312.SubServers.Proxy.SubPlugin;
import org.json.JSONObject;

import java.util.Map;
import java.util.UUID;

public class PacketTeleportPlayer implements PacketIn, PacketOut {
    private SubPlugin plugin;
    private int response;
    private String message;
    private String id;

    public PacketTeleportPlayer(SubPlugin plugin) {
        this.plugin = plugin;
    }

    public PacketTeleportPlayer(int response, String message, String id) {
        this.response = response;
        this.message = message;
        this.id = id;
    }

    @Override
    public JSONObject generate() {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("r", response);
        json.put("m", message);
        return json;
    }

    @Override
    public void execute(Client client, JSONObject data) {
        try {
            Map<String, Server> servers = plugin.api.getServers();
            if (!servers.keySet().contains(data.getString("server").toLowerCase())) {
                client.sendPacket(new PacketTeleportPlayer(2, "There is no server with that name", (data.keySet().contains("id"))?data.getString("id"):null));
            } else if (plugin.getPlayer(UUID.fromString(data.getString("player"))) == null) {
                client.sendPacket(new PacketTeleportPlayer(3, "There is no player with that id", (data.keySet().contains("id"))?data.getString("id"):null));
            } else {
                plugin.getPlayer(UUID.fromString(data.getString("player"))).connect(servers.get(data.getString("server").toLowerCase()));
                client.sendPacket(new PacketTeleportPlayer(0, "Teleporting Player", (data.keySet().contains("id"))?data.getString("id"):null));
            }
        } catch (Throwable e) {
            client.sendPacket(new PacketTeleportPlayer(1, e.getClass().getCanonicalName() + ": " + e.getMessage(), (data.keySet().contains("id"))?data.getString("id"):null));
        }
    }

    @Override
    public Version getVersion() {
        return new Version("2.11.0a");
    }
}

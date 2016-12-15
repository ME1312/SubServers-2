package net.ME1312.SubServers.Proxy.Network.Packet;

import net.ME1312.SubServers.Proxy.Host.Server;
import net.ME1312.SubServers.Proxy.Host.SubServer;
import net.ME1312.SubServers.Proxy.Library.Version.Version;
import net.ME1312.SubServers.Proxy.Network.Client;
import net.ME1312.SubServers.Proxy.Network.PacketIn;
import net.ME1312.SubServers.Proxy.Network.PacketOut;
import net.ME1312.SubServers.Proxy.SubPlugin;
import org.json.JSONObject;

import java.util.Map;

public class PacketLinkServer implements PacketIn, PacketOut {
    private SubPlugin plugin;
    private boolean response;
    private String message;

    public PacketLinkServer(SubPlugin plugin) {
        this.plugin = plugin;
    }

    public PacketLinkServer(boolean response, String message) {
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
            Map<String, Server> servers = plugin.api.getServers();
            if (servers.keySet().contains(data.getString("name").toLowerCase())) {
                Server server = servers.get(data.getString("name").toLowerCase());
                if (server.getSubDataClient() == null) {
                    server.linkSubDataClient(client);
                    System.out.println("SubData > " + client.getAddress().toString() + " has been defined as " + ((server instanceof SubServer) ? "SubServer" : "Server") + ": " + server.getName());
                    client.sendPacket(new PacketLinkServer(true, "Definition Successful"));
                } else {
                    client.sendPacket(new PacketLinkServer(false, "Server already linked"));
                }
            } else {
                client.sendPacket(new PacketLinkServer(false, "There is no server with that name"));
            }
        } catch (Exception e) {
            client.sendPacket(new PacketLinkServer(false, e.getClass().getCanonicalName() + ": " + e.getMessage()));
        }
    }

    @Override
    public Version getVersion() {
        return new Version("2.11.0a");
    }
}

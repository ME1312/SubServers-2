package net.ME1312.SubServers.Proxy.Network.Packet;

import net.ME1312.SubServers.Proxy.Host.Server;
import net.ME1312.SubServers.Proxy.Host.SubServer;
import net.ME1312.SubServers.Proxy.Libraries.Version.Version;
import net.ME1312.SubServers.Proxy.Network.Client;
import net.ME1312.SubServers.Proxy.Network.PacketIn;
import net.ME1312.SubServers.Proxy.Network.PacketOut;
import net.ME1312.SubServers.Proxy.SubPlugin;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

public class PacketRequestServers implements PacketIn, PacketOut {
    private SubPlugin plugin;

    public PacketRequestServers(SubPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public JSONObject generate() {
        JSONObject json = new JSONObject();

        List<String> exServers = new ArrayList<String>();
        for (Server server : plugin.exServers.values()) {
            exServers.add(server.getName());
        }
        json.put("servers", exServers);

        TreeMap<String, List<String>> hosts = new TreeMap<String, List<String>>();
        for (SubServer server : plugin.api.getSubServers().values()) {
            List<String> servers = (hosts.keySet().contains(server.getHost().getName()))?hosts.get(server.getHost().getName()):new ArrayList<String>();
            servers.add(server.getName());
            hosts.put(server.getHost().getName(), servers);
        }
        json.put("hosts", hosts);

        return json;
    }

    @Override
    public void execute(Client client, JSONObject data) {
        client.sendPacket(this);
    }

    @Override
    public Version getVersion() {
        return new Version("2.11.0a");
    }
}

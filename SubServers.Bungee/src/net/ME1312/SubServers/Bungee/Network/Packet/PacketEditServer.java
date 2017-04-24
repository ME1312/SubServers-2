package net.ME1312.SubServers.Bungee.Network.Packet;

import net.ME1312.SubServers.Bungee.Host.Server;
import net.ME1312.SubServers.Bungee.Host.SubServer;
import net.ME1312.SubServers.Bungee.Library.Config.YAMLSection;
import net.ME1312.SubServers.Bungee.Library.Util;
import net.ME1312.SubServers.Bungee.Library.Version.Version;
import net.ME1312.SubServers.Bungee.Network.Client;
import net.ME1312.SubServers.Bungee.Network.PacketIn;
import net.ME1312.SubServers.Bungee.Network.PacketOut;
import net.ME1312.SubServers.Bungee.SubPlugin;
import org.json.JSONObject;

import java.util.Map;
import java.util.UUID;

/**
 * Edit Server Packet
 */
public class PacketEditServer implements PacketIn, PacketOut {
    private SubPlugin plugin;
    private int response;
    private String id;

    /**
     * New PacketEditServer (In)
     *
     * @param plugin SubPlugin
     */
    public PacketEditServer(SubPlugin plugin) {
        if (Util.isNull(plugin)) throw new NullPointerException();
        this.plugin = plugin;
    }

    /**
     * New PacketEditServer (Out)
     *
     * @param response Response ID
     * @param id Receiver ID
     */
    public PacketEditServer(int response, String id) {
        if (Util.isNull(response)) throw new NullPointerException();
        this.response = response;
        this.id = id;
    }

    @Override
    public JSONObject generate() {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("r", response);
        return json;
    }

    @Override
    public void execute(Client client, JSONObject data) {
        try {
            Map<String, Server> servers = plugin.api.getServers();
            if (!servers.keySet().contains(data.getString("server").toLowerCase()) || !(servers.get(data.getString("server").toLowerCase()) instanceof SubServer)) {
                client.sendPacket(new PacketEditServer(0, (data.keySet().contains("id"))?data.getString("id"):null));
            } else {
                new Thread(() -> client.sendPacket(new PacketEditServer(((SubServer) servers.get(data.getString("server").toLowerCase())).edit((data.keySet().contains("player"))?UUID.fromString(data.getString("player")):null, new YAMLSection(data.getJSONObject("edit"))) * -1, (data.keySet().contains("id"))?data.getString("id"):null))).start();
            }
        } catch (Throwable e) {
            client.sendPacket(new PacketEditServer(0, (data.keySet().contains("id"))?data.getString("id"):null));
            e.printStackTrace();
        }
    }

    @Override
    public Version getVersion() {
        return new Version("2.11.0a");
    }
}

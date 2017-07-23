package net.ME1312.SubServers.Bungee.Network.Packet;

import net.ME1312.SubServers.Bungee.Host.Server;
import net.ME1312.SubServers.Bungee.Host.SubServer;
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
 * Server Command Packet
 */
public class PacketCommandServer implements PacketIn, PacketOut {
    private SubPlugin plugin;
    private int response;
    private String message;
    private String id;

    /**
     * New PacketCommandServer (In)
     *
     * @param plugin SubPlugin
     */
    public PacketCommandServer(SubPlugin plugin) {
        if (Util.isNull(plugin)) throw new NullPointerException();
        this.plugin = plugin;
    }

    /**
     * New PacketCommandServer (Out)
     *
     * @param response Response ID
     * @param message Message
     * @param id Receiver ID
     */
    public PacketCommandServer(int response, String message, String id) {
        if (Util.isNull(response, message)) throw new NullPointerException();
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
            if (!data.getString("server").equals("*") && !servers.keySet().contains(data.getString("server").toLowerCase())) {
                client.sendPacket(new PacketCommandServer(3, "There is no server with that name", (data.keySet().contains("id")) ? data.getString("id") : null));
            } else if (!data.getString("server").equals("*") && !(servers.get(data.getString("server").toLowerCase()) instanceof SubServer)) {
                client.sendPacket(new PacketCommandServer(4, "That Server is not a SubServer", (data.keySet().contains("id")) ? data.getString("id") : null));
            } else if (!data.getString("server").equals("*") && !((SubServer) servers.get(data.getString("server").toLowerCase())).isRunning()) {
                client.sendPacket(new PacketCommandServer(5, "That SubServer is not running", (data.keySet().contains("id")) ? data.getString("id") : null));
            } else {
                if (data.getString("server").equals("*")) {
                    boolean sent = false;
                    for (Server server : servers.values()) {
                        if (server instanceof SubServer && ((SubServer) server).isRunning()) {
                            if (((SubServer) server).command((data.keySet().contains("player"))?UUID.fromString(data.getString("player")):null, data.getString("command"))) {
                                sent = true;
                            }
                        }
                    }
                    if (sent) {
                        client.sendPacket(new PacketCommandServer(0, "Sending Command", (data.keySet().contains("id")) ? data.getString("id") : null));
                    } else {
                        client.sendPacket(new PacketCommandServer(1, "Couldn't send command", (data.keySet().contains("id")) ? data.getString("id") : null));
                    }
                } else {
                    if (((SubServer) servers.get(data.getString("server").toLowerCase())).command((data.keySet().contains("player")) ? UUID.fromString(data.getString("player")) : null, data.getString("command"))) {
                        client.sendPacket(new PacketCommandServer(0, "Sending Command", (data.keySet().contains("id")) ? data.getString("id") : null));
                    } else {
                        client.sendPacket(new PacketCommandServer(1, "Couldn't send command", (data.keySet().contains("id")) ? data.getString("id") : null));
                    }
                }
            }
        } catch (Throwable e) {
            client.sendPacket(new PacketCommandServer(2, e.getClass().getCanonicalName() + ": " + e.getMessage(), (data.keySet().contains("id")) ? data.getString("id") : null));
            e.printStackTrace();
        }
    }

    @Override
    public Version getVersion() {
        return new Version("2.11.0a");
    }
}

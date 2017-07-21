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
 * Start Server Packet
 */
public class PacketStartServer implements PacketIn, PacketOut {
    private SubPlugin plugin;
    private int response;
    private String message;
    private String id;

    /**
     * New PacketStartServer (In)
     *
     * @param plugin SubPlugin
     */
    public PacketStartServer(SubPlugin plugin) {
        if (Util.isNull(plugin)) throw new NullPointerException();
        this.plugin = plugin;
    }

    /**
     * New PacketStartServer (Out)
     *
     * @param response Response ID
     * @param message Message
     * @param id Receiver ID
     */
    public PacketStartServer(int response, String message, String id) {
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
            if (!servers.keySet().contains(data.getString("server").toLowerCase())) {
                client.sendPacket(new PacketStartServer(3, "There is no server with that name", (data.keySet().contains("id"))?data.getString("id"):null));
            } else if (!(servers.get(data.getString("server").toLowerCase()) instanceof SubServer)) {
                client.sendPacket(new PacketStartServer(4, "That Server is not a SubServer", (data.keySet().contains("id"))?data.getString("id"):null));
            } else if (!((SubServer) servers.get(data.getString("server").toLowerCase())).getHost().isEnabled()) {
                client.sendPacket(new PacketStartServer(5, "That SubServer's Host is not enabled", (data.keySet().contains("id"))?data.getString("id"):null));
            } else if (!((SubServer) servers.get(data.getString("server").toLowerCase())).isEnabled()) {
                client.sendPacket(new PacketStartServer(5, "That SubServer is not enabled", (data.keySet().contains("id"))?data.getString("id"):null));
            } else if (((SubServer) servers.get(data.getString("server").toLowerCase())).isRunning()) {
                client.sendPacket(new PacketStartServer(6, "That SubServer is already running", (data.keySet().contains("id")) ? data.getString("id") : null));
            } else if (((SubServer) servers.get(data.getString("server").toLowerCase())).getCurrentIncompatibilities().size() != 0) {
                String list = "";
                for (SubServer server : ((SubServer) servers.get(data.getString("server").toLowerCase())).getCurrentIncompatibilities()) {
                    if (list.length() != 0) list += ", ";
                    list += server.getName();
                }
                client.sendPacket(new PacketStartServer(7, "Cannot start SubServer while these servers are running: " + list, (data.keySet().contains("id")) ? data.getString("id") : null));
            } else {
                if (((SubServer) servers.get(data.getString("server").toLowerCase())).start((data.keySet().contains("player"))?UUID.fromString(data.getString("player")):null)) {
                    client.sendPacket(new PacketStartServer(0, "Starting SubServer", (data.keySet().contains("id"))?data.getString("id"):null));
                } else {
                    client.sendPacket(new PacketStartServer(1, "Couldn't start SubServer", (data.keySet().contains("id"))?data.getString("id"):null));
                }
            }
        } catch (Throwable e) {
            client.sendPacket(new PacketStartServer(2, e.getClass().getCanonicalName() + ": " + e.getMessage(), (data.keySet().contains("id"))?data.getString("id"):null));
        }
    }

    @Override
    public Version getVersion() {
        return new Version("2.11.0a");
    }
}

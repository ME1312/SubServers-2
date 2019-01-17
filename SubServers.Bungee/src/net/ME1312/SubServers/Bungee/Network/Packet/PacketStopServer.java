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

import java.util.Map;
import java.util.UUID;

/**
 * Stop Server Packet
 */
public class PacketStopServer implements PacketIn, PacketOut {
    private SubPlugin plugin;
    private int response;
    private String message;
    private String id;

    /**
     * New PacketStopServer (In)
     *
     * @param plugin SubPlugin
     */
    public PacketStopServer(SubPlugin plugin) {
        if (Util.isNull(plugin)) throw new NullPointerException();
        this.plugin = plugin;
    }

    /**
     * New PacketStopServer (Out)
     *
     * @param response Response ID
     * @param message Message
     * @param id Receiver ID
     */
    public PacketStopServer(int response, String message, String id) {
        if (Util.isNull(response, message)) throw new NullPointerException();
        this.response = response;
        this.message = message;
        this.id = id;
    }

    @Override
    public YAMLSection generate() {
        YAMLSection json = new YAMLSection();
        if (id != null) json.set("id", id);
        json.set("r", response);
        json.set("m", message);
        return json;
    }

    @Override
    public void execute(Client client, YAMLSection data) {
        try {
            Map<String, Server> servers = plugin.api.getServers();
            if (!data.getRawString("server").equals("*") && !servers.keySet().contains(data.getRawString("server").toLowerCase())) {
                client.sendPacket(new PacketStopServer(3, "There is no server with that name", (data.contains("id"))?data.getRawString("id"):null));
            } else if (!data.getRawString("server").equals("*") && !(servers.get(data.getRawString("server").toLowerCase()) instanceof SubServer)) {
                client.sendPacket(new PacketStopServer(4, "That Server is not a SubServer", (data.contains("id"))?data.getRawString("id"):null));
            } else if (!data.getRawString("server").equals("*") && !((SubServer) servers.get(data.getRawString("server").toLowerCase())).isRunning()) {
                client.sendPacket(new PacketStopServer(5, "That SubServer is not running", (data.contains("id"))?data.getRawString("id"):null));
            } else if (data.getRawString("server").equals("*")) {
                boolean sent = false;
                if (data.contains("force") && data.getBoolean("force")) {
                    for (Server server : servers.values()) {
                        if (server instanceof SubServer && ((SubServer) server).isRunning()) {
                            if (((SubServer) server).terminate((data.contains("player"))?data.getUUID("player"):null)) {
                                sent = true;
                            }
                        }
                    }
                    if (sent) {
                        client.sendPacket(new PacketStopServer(0, "Terminating SubServers", (data.contains("id"))?data.getRawString("id"):null));
                    } else {
                        client.sendPacket(new PacketStopServer(1, "Couldn't terminate SubServers", (data.contains("id"))?data.getRawString("id"):null));
                    }
                } else {
                    for (Server server : servers.values()) {
                        if (server instanceof SubServer && ((SubServer) server).isRunning()) {
                            if (((SubServer) server).stop((data.contains("player"))?data.getUUID("player"):null)) {
                                sent = true;
                            }
                        }
                    }
                    if (sent) {
                        client.sendPacket(new PacketStopServer(0, "Stopping SubServers", (data.contains("id"))?data.getRawString("id"):null));
                    } else {
                        client.sendPacket(new PacketStopServer(1, "Couldn't stop SubServers", (data.contains("id"))?data.getRawString("id"):null));
                    }
                }
            } else {
                if (data.contains("force") && data.getBoolean("force")) {
                    if (((SubServer) servers.get(data.getRawString("server").toLowerCase())).terminate((data.contains("player"))?data.getUUID("player"):null)) {
                        client.sendPacket(new PacketStopServer(0, "Terminating SubServer", (data.contains("id"))?data.getRawString("id"):null));
                    } else {
                        client.sendPacket(new PacketStopServer(1, "Couldn't terminate SubServer", (data.contains("id"))?data.getRawString("id"):null));
                    }
                } else {
                    if (((SubServer) servers.get(data.getRawString("server").toLowerCase())).stop((data.contains("player"))?data.getUUID("player"):null)) {
                        client.sendPacket(new PacketStopServer(0, "Stopping SubServer", (data.contains("id"))?data.getRawString("id"):null));
                    } else {
                        client.sendPacket(new PacketStopServer(1, "Couldn't stop SubServer", (data.contains("id"))?data.getRawString("id"):null));
                    }
                }
            }
        } catch (Throwable e) {
            client.sendPacket(new PacketStopServer(2, e.getClass().getCanonicalName() + ": " + e.getMessage(), (data.contains("id"))?data.getRawString("id"):null));
            e.printStackTrace();
        }
    }

    @Override
    public Version getVersion() {
        return new Version("2.11.0a");
    }
}

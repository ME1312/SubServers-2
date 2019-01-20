package net.ME1312.SubServers.Bungee.Network.Packet;

import net.ME1312.SubServers.Bungee.Host.Server;
import net.ME1312.SubServers.Bungee.Host.SubServer;
import net.ME1312.SubServers.Bungee.Library.Config.YAMLSection;
import net.ME1312.SubServers.Bungee.Library.Util;
import net.ME1312.SubServers.Bungee.Library.Version.Version;
import net.ME1312.SubServers.Bungee.Network.Client;
import net.ME1312.SubServers.Bungee.Network.PacketIn;
import net.ME1312.SubServers.Bungee.Network.PacketOut;
import net.ME1312.SubServers.Bungee.SubAPI;
import net.ME1312.SubServers.Bungee.SubPlugin;

import java.util.Map;

/**
 * Restart Server Packet
 */
public class PacketRestartServer implements PacketIn, PacketOut {
    private SubPlugin plugin;
    private int response;
    private String message;
    private String id;

    /**
     * New PacketRestartServer (In)
     *
     * @param plugin SubPlugin
     */
    public PacketRestartServer(SubPlugin plugin) {
        if (Util.isNull(plugin)) throw new NullPointerException();
        this.plugin = plugin;
    }

    /**
     * New PacketRestartServer (Out)
     *
     * @param response Response ID
     * @param message Message
     * @param id Receiver ID
     */
    public PacketRestartServer(int response, String message, String id) {
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
            Runnable starter = () -> {
                Map<String, Server> servers = plugin.api.getServers();
                if (!servers.keySet().contains(data.getRawString("server").toLowerCase())) {
                } else if (!(servers.get(data.getRawString("server").toLowerCase()) instanceof SubServer)) {
                } else if (!((SubServer) servers.get(data.getRawString("server").toLowerCase())).getHost().isAvailable()) {
                } else if (!((SubServer) servers.get(data.getRawString("server").toLowerCase())).getHost().isEnabled()) {
                } else if (!((SubServer) servers.get(data.getRawString("server").toLowerCase())).isEnabled()) {
                } else if (((SubServer) servers.get(data.getRawString("server").toLowerCase())).isRunning()) {
                } else if (((SubServer) servers.get(data.getRawString("server").toLowerCase())).getCurrentIncompatibilities().size() != 0) {
                } else {
                    ((SubServer) servers.get(data.getRawString("server").toLowerCase())).start((data.contains("player"))?data.getUUID("player"):null);
                }
            };

            Map<String, Server> servers = plugin.api.getServers();
            if (!servers.keySet().contains(data.getRawString("server").toLowerCase())) {
                client.sendPacket(new PacketRestartServer(3, "There is no server with that name", (data.contains("id"))?data.getRawString("id"):null));
            } else if (!(servers.get(data.getRawString("server").toLowerCase()) instanceof SubServer)) {
                client.sendPacket(new PacketRestartServer(4, "That Server is not a SubServer", (data.contains("id"))?data.getRawString("id"):null));
            } else {
                client.sendPacket(new PacketRestartServer(0, "Restarting SubServer", (data.contains("id"))?data.getRawString("id"):null));
                if (((SubServer) servers.get(data.getRawString("server").toLowerCase())).isRunning()) {
                    new Thread(() -> {
                        try {
                            ((SubServer) servers.get(data.getRawString("server").toLowerCase())).stop();
                            ((SubServer) servers.get(data.getRawString("server").toLowerCase())).waitFor();
                            Thread.sleep(100);
                            starter.run();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }, "SubServers.Bungee::Server_Restart_Packet_Handler(" + servers.get(data.getRawString("server").toLowerCase()).getName() + ')').start();
                } else {
                    starter.run();
                }
            }
        } catch (Throwable e) {
            client.sendPacket(new PacketRestartServer(2, e.getClass().getCanonicalName() + ": " + e.getMessage(), (data.contains("id"))?data.getRawString("id"):null));
            e.printStackTrace();
        }
    }

    @Override
    public Version getVersion() {
        return new Version("2.11.0a");
    }
}

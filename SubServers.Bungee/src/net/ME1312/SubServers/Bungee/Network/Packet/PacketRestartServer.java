package net.ME1312.SubServers.Bungee.Network.Packet;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Server.Protocol.PacketObjectIn;
import net.ME1312.SubData.Server.Protocol.PacketObjectOut;
import net.ME1312.SubData.Server.SubDataClient;
import net.ME1312.SubServers.Bungee.Host.Server;
import net.ME1312.SubServers.Bungee.Host.SubServer;
import net.ME1312.SubServers.Bungee.SubProxy;

import java.util.Map;
import java.util.UUID;

/**
 * Restart Server Packet
 */
public class PacketRestartServer implements PacketObjectIn<Integer>, PacketObjectOut<Integer> {
    private SubProxy plugin;
    private int response;
    private UUID tracker;

    /**
     * New PacketRestartServer (In)
     *
     * @param plugin SubPlugin
     */
    public PacketRestartServer(SubProxy plugin) {
        Util.nullpo(plugin);
        this.plugin = plugin;
    }

    /**
     * New PacketRestartServer (Out)
     *
     * @param response Response ID
     * @param tracker Receiver ID
     */
    public PacketRestartServer(int response, UUID tracker) {
        this.response = response;
        this.tracker = tracker;
    }

    @Override
    public ObjectMap<Integer> send(SubDataClient client) {
        ObjectMap<Integer> json = new ObjectMap<Integer>();
        if (tracker != null) json.set(0x0000, tracker);
        json.set(0x0001, response);
        return json;
    }

    @Override
    public void receive(SubDataClient client, ObjectMap<Integer> data) {
        UUID tracker =       (data.contains(0x0000)?data.getUUID(0x0000):null);
        try {
            String name = data.getString(0x0001);
            UUID player =    (data.contains(0x0002)?data.getUUID(0x0002):null);

            Runnable starter = () -> {
                Map<String, Server> servers = plugin.api.getServers();
                if (!servers.containsKey(name.toLowerCase())) {
                } else if (!(servers.get(name.toLowerCase()) instanceof SubServer)) {
                } else if (!((SubServer) servers.get(name.toLowerCase())).getHost().isAvailable()) {
                } else if (!((SubServer) servers.get(name.toLowerCase())).getHost().isEnabled()) {
                } else if (!((SubServer) servers.get(name.toLowerCase())).isAvailable()) {
                } else if (!((SubServer) servers.get(name.toLowerCase())).isEnabled()) {
                } else if (((SubServer) servers.get(name.toLowerCase())).isRunning()) {
                } else if (((SubServer) servers.get(name.toLowerCase())).getCurrentIncompatibilities().size() != 0) {
                } else {
                    ((SubServer) servers.get(name.toLowerCase())).start(player);
                }
            };

            Map<String, Server> servers = plugin.api.getServers();
            if (!servers.containsKey(name.toLowerCase())) {
                client.sendPacket(new PacketRestartServer(3, tracker));
            } else if (!(servers.get(name.toLowerCase()) instanceof SubServer)) {
                client.sendPacket(new PacketRestartServer(4, tracker));
            } else {
                client.sendPacket(new PacketRestartServer(0, tracker));
                if (((SubServer) servers.get(name.toLowerCase())).isRunning()) {
                    new Thread(() -> {
                        try {
                            ((SubServer) servers.get(name.toLowerCase())).stop();
                            ((SubServer) servers.get(name.toLowerCase())).waitFor();
                            Thread.sleep(100);
                            starter.run();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }, "SubServers.Bungee::Server_Restart_Packet_Handler(" + servers.get(name.toLowerCase()).getName() + ')').start();
                } else {
                    starter.run();
                }
            }
        } catch (Throwable e) {
            client.sendPacket(new PacketRestartServer(2, tracker));
            e.printStackTrace();
        }
    }
}

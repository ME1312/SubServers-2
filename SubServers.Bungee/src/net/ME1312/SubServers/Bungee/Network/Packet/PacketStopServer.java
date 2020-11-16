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
 * Stop Server Packet
 */
public class PacketStopServer implements PacketObjectIn<Integer>, PacketObjectOut<Integer> {
    private SubProxy plugin;
    private int response;
    private UUID tracker;

    /**
     * New PacketStopServer (In)
     *
     * @param plugin SubPlugin
     */
    public PacketStopServer(SubProxy plugin) {
        if (Util.isNull(plugin)) throw new NullPointerException();
        this.plugin = plugin;
    }

    /**
     * New PacketStopServer (Out)
     *
     * @param response Response ID
     * @param tracker Receiver ID
     */
    public PacketStopServer(int response, UUID tracker) {
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
            String name = data.getRawString(0x0001);
            boolean force =  (data.contains(0x0002)?data.getBoolean(0x0002):false);
            UUID player =    (data.contains(0x0003)?data.getUUID(0x0003):null);

            Map<String, Server> servers = plugin.api.getServers();
            if (!name.equals("*") && !servers.keySet().contains(name.toLowerCase())) {
                client.sendPacket(new PacketStopServer(3, tracker));
            } else if (!name.equals("*") && !(servers.get(name.toLowerCase()) instanceof SubServer)) {
                client.sendPacket(new PacketStopServer(4, tracker));
            } else if (!name.equals("*") && !((SubServer) servers.get(name.toLowerCase())).isRunning()) {
                client.sendPacket(new PacketStopServer(5, tracker));
            } else if (name.equals("*")) {
                boolean sent = false;
                if (force) {
                    for (Server server : servers.values()) {
                        if (server instanceof SubServer && ((SubServer) server).isRunning()) {
                            if (((SubServer) server).terminate(player)) {
                                sent = true;
                            }
                        }
                    }
                    if (sent) {
                        client.sendPacket(new PacketStopServer(0, tracker));
                    } else {
                        client.sendPacket(new PacketStopServer(1, tracker));
                    }
                } else {
                    for (Server server : servers.values()) {
                        if (server instanceof SubServer && ((SubServer) server).isRunning()) {
                            if (((SubServer) server).stop(player)) {
                                sent = true;
                            }
                        }
                    }
                    if (sent) {
                        client.sendPacket(new PacketStopServer(0, tracker));
                    } else {
                        client.sendPacket(new PacketStopServer(1, tracker));
                    }
                }
            } else {
                if (force) {
                    if (((SubServer) servers.get(name.toLowerCase())).terminate(player)) {
                        client.sendPacket(new PacketStopServer(0, tracker));
                    } else {
                        client.sendPacket(new PacketStopServer(1, tracker));
                    }
                } else {
                    if (((SubServer) servers.get(name.toLowerCase())).stop(player)) {
                        client.sendPacket(new PacketStopServer(0, tracker));
                    } else {
                        client.sendPacket(new PacketStopServer(1, tracker));
                    }
                }
            }
        } catch (Throwable e) {
            client.sendPacket(new PacketStopServer(2, tracker));
            e.printStackTrace();
        }
    }

    @Override
    public int version() {
        return 0x0001;
    }
}

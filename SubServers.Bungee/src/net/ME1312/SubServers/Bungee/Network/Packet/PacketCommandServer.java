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
 * Server Command Packet
 */
public class PacketCommandServer implements PacketObjectIn<Integer>, PacketObjectOut<Integer> {
    private SubProxy plugin;
    private int response;
    private UUID tracker;

    /**
     * New PacketCommandServer (In)
     *
     * @param plugin SubPlugin
     */
    public PacketCommandServer(SubProxy plugin) {
        this.plugin = Util.nullpo(plugin);
    }

    /**
     * New PacketCommandServer (Out)
     *
     * @param response Response ID
     * @param tracker Tracker ID
     */
    public PacketCommandServer(int response, UUID tracker) {
        this.response = response;
        this.tracker = tracker;
    }

    @Override
    public ObjectMap<Integer> send(SubDataClient client) {
        ObjectMap<Integer> data = new ObjectMap<Integer>();
        if (tracker != null) data.set(0x0000, tracker);
        data.set(0x0001, response);
        return data;
    }

    @Override
    public void receive(SubDataClient client, ObjectMap<Integer> data) {
        UUID tracker =       (data.contains(0x0000)?data.getUUID(0x0000):null);
        try {
            String server =  data.getString(0x0001);
            String command = data.getString(0x0002);
            UUID player =    (data.contains(0x0003)?data.getUUID(0x0003):null);
            UUID target =    (data.contains(0x0004)?data.getUUID(0x0004):null);

            Map<String, Server> servers = plugin.api.getServers();
            if (!server.equals("*") && !servers.containsKey(server.toLowerCase())) {
                client.sendPacket(new PacketCommandServer(3, tracker));
            } else if (!server.equals("*") && !(target == null && servers.get(server.toLowerCase()) instanceof SubServer) && servers.get(server.toLowerCase()).getSubData()[0] == null) {
                client.sendPacket(new PacketCommandServer(4, tracker));
            } else if (!server.equals("*") && servers.get(server.toLowerCase()) instanceof SubServer && !((SubServer) servers.get(server.toLowerCase())).isRunning()) {
                client.sendPacket(new PacketCommandServer(5, tracker));
            } else {
                if (server.equals("*")) {
                    boolean sent = false;
                    for (Server next : servers.values()) {
                        if (target == null) {
                            sent |= next.command(player, command);
                        } else {
                            sent |= next.command(player, target, command);
                        }
                    }
                    if (sent) {
                        client.sendPacket(new PacketCommandServer(0, tracker));
                    } else {
                        client.sendPacket(new PacketCommandServer(1, tracker));
                    }
                } else {
                    if (target == null) {
                        client.sendPacket(new PacketCommandServer((servers.get(server.toLowerCase()).command(player, command))? 0 : 1, tracker));
                    } else {
                        client.sendPacket(new PacketCommandServer((servers.get(server.toLowerCase()).command(player, target, command))? 0 : 1, tracker));
                    }
                }
            }
        } catch (Throwable e) {
            client.sendPacket(new PacketCommandServer(2, tracker));
            e.printStackTrace();
        }
    }
}

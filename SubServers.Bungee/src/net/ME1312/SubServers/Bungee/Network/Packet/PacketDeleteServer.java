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
 * Delete Server Packet
 */
public class PacketDeleteServer implements PacketObjectIn<Integer>, PacketObjectOut<Integer> {
    private SubProxy plugin;
    private int response;
    private UUID tracker;

    /**
     * New PacketDeleteServer (In)
     *
     * @param plugin SubPlugin
     */
    public PacketDeleteServer(SubProxy plugin) {
        Util.nullpo(plugin);
        this.plugin = plugin;
    }

    /**
     * New PacketDeleteServer (Out)
     *
     * @param response Response ID
     * @param tracker Receiver ID
     */
    public PacketDeleteServer(int response, UUID tracker) {
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
        UUID tracker =         (data.contains(0x0000)?data.getUUID(0x0000):null);
        try {
            String name =   data.getString(0x0001);
            boolean recycle = data.getBoolean(0x0002);
            boolean force =   data.getBoolean(0x0003);
            UUID player =      (data.contains(0x0004)?data.getUUID(0x0004):null);

            Map<String, Server> servers = plugin.api.getServers();
            if (!servers.containsKey(name.toLowerCase())) {
                client.sendPacket(new PacketDeleteServer(3, tracker));
            } else if (!(servers.get(name.toLowerCase()) instanceof SubServer)) {
                client.sendPacket(new PacketDeleteServer(4, tracker));
            } else {
                if (recycle) {
                    if (force) {
                        if (((SubServer) servers.get(name.toLowerCase())).getHost().forceRecycleSubServer(player, name.toLowerCase())) {
                            client.sendPacket(new PacketDeleteServer(0, tracker));
                        } else {
                            client.sendPacket(new PacketDeleteServer(1, tracker));
                        }
                    } else {
                        if (((SubServer) servers.get(name.toLowerCase())).getHost().recycleSubServer(player, name.toLowerCase())) {
                            client.sendPacket(new PacketDeleteServer(0, tracker));
                        } else {
                            client.sendPacket(new PacketDeleteServer(1, tracker));
                        }
                    }
                } else {
                    if (force) {
                        if (((SubServer) servers.get(name.toLowerCase())).getHost().forceDeleteSubServer(player, name.toLowerCase())) {
                            client.sendPacket(new PacketDeleteServer(0, tracker));
                        } else {
                            client.sendPacket(new PacketDeleteServer(1, tracker));
                        }
                    } else {
                        if (((SubServer) servers.get(name.toLowerCase())).getHost().deleteSubServer(player, name.toLowerCase())) {
                            client.sendPacket(new PacketDeleteServer(0, tracker));
                        } else {
                            client.sendPacket(new PacketDeleteServer(1, tracker));
                        }
                    }
                }
            }
        } catch (Throwable e) {
            client.sendPacket(new PacketDeleteServer(2, tracker));
            e.printStackTrace();
        }
    }
}

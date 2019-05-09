package net.ME1312.SubServers.Bungee.Network.Packet;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Server.Protocol.PacketObjectIn;
import net.ME1312.SubData.Server.Protocol.PacketObjectOut;
import net.ME1312.SubData.Server.SubDataClient;
import net.ME1312.SubServers.Bungee.Host.Server;
import net.ME1312.SubServers.Bungee.Host.SubServer;
import net.ME1312.SubServers.Bungee.SubAPI;
import net.ME1312.SubServers.Bungee.SubPlugin;

import java.util.Map;
import java.util.UUID;

/**
 * Edit Server Packet
 */
public class PacketEditServer implements PacketObjectIn<Integer>, PacketObjectOut<Integer> {
    private SubPlugin plugin;
    private int response;
    private int status;
    private UUID tracker;

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
     * @param tracker Receiver ID
     */
    public PacketEditServer(int response, UUID tracker) {
        this(response, -1, tracker);
    }

    /**
     * New PacketEditServer (Out)
     *
     * @param response Response ID
     * @param status Success Status
     * @param tracker Receiver ID
     */
    public PacketEditServer(int response, int status, UUID tracker) {
        this.response = response;
        this.status = status;
        this.tracker = tracker;
    }

    @Override
    public ObjectMap<Integer> send(SubDataClient client) {
        ObjectMap<Integer> json = new ObjectMap<Integer>();
        if (tracker != null) json.set(0x0000, tracker);
        json.set(0x0001, response);
        json.set(0x0002, status);
        return json;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void receive(SubDataClient client, ObjectMap<Integer> data) {
        UUID tracker =     (data.contains(0x0000)?data.getUUID(0x0000):null);
        try {
            String name =                    data.getRawString(0x0001);
            ObjectMap<String> edit = new ObjectMap<>((Map<String, ?>)data.getObject(0x0002));
            boolean perma =                    data.getBoolean(0x0003);
            UUID player =                       (data.contains(0x0004)?data.getUUID(0x0004):null);

            Map<String, Server> servers = plugin.api.getServers();
            if (!servers.keySet().contains(name.toLowerCase())) {
                client.sendPacket(new PacketEditServer(3, tracker));
            } else if (!(servers.get(name.toLowerCase()) instanceof SubServer)) {
                client.sendPacket(new PacketEditServer(4, tracker));
            } else {
                int success;
                if (perma) {
                    success = ((SubServer) servers.get(name.toLowerCase())).permaEdit(player, edit);
                } else {
                    success = ((SubServer) servers.get(name.toLowerCase())).edit(player, edit);
                }
                if (success < 0) {
                    client.sendPacket(new PacketEditServer(0, success, tracker));
                } else {
                    client.sendPacket(new PacketEditServer(1, success, tracker));
                }
            }
        } catch (Throwable e) {
            client.sendPacket(new PacketEditServer(2, tracker));
            e.printStackTrace();
        }
    }

    @Override
    public int version() {
        return 0x0001;
    }
}

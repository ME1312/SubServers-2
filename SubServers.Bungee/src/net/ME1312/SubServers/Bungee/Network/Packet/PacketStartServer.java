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
 * Start Server Packet
 */
public class PacketStartServer implements PacketObjectIn<Integer>, PacketObjectOut<Integer> {
    private SubProxy plugin;
    private int response;
    private String extra;
    private UUID tracker;

    /**
     * New PacketStartServer (In)
     *
     * @param plugin SubPlugin
     */
    public PacketStartServer(SubProxy plugin) {
        this.plugin = Util.nullpo(plugin);
    }

    /**
     * New PacketStartServer (Out)
     *
     * @param response Response ID
     * @param tracker Receiver ID
     */
    public PacketStartServer(int response, UUID tracker) {
        this(response, null, tracker);
    }

    /**
     * New PacketStartServer (Out)
     *
     * @param response Response ID
     * @param tracker Receiver ID
     */
    public PacketStartServer(int response, String extra, UUID tracker) {
        this.response = response;
        this.extra = extra;
        this.tracker = tracker;
    }

    @Override
    public ObjectMap<Integer> send(SubDataClient client) {
        ObjectMap<Integer> json = new ObjectMap<Integer>();
        if (tracker != null) json.set(0x0000, tracker);
        json.set(0x0001, response);
        if (extra != null) json.set(0x0002, extra);
        return json;
    }

    @Override
    public void receive(SubDataClient client, ObjectMap<Integer> data) {
        UUID tracker =       (data.contains(0x0000)?data.getUUID(0x0000):null);
        try {
            String name = data.getString(0x0001);
            UUID player =    (data.contains(0x0002)?data.getUUID(0x0002):null);

            Map<String, Server> servers = plugin.api.getServers();
            if (!servers.containsKey(name.toLowerCase())) {
                client.sendPacket(new PacketStartServer(3, tracker));
            } else if (!(servers.get(name.toLowerCase()) instanceof SubServer)) {
                client.sendPacket(new PacketStartServer(4, tracker));
            } else if (!((SubServer) servers.get(name.toLowerCase())).getHost().isAvailable()) {
                client.sendPacket(new PacketStartServer(5, tracker));
            } else if (!((SubServer) servers.get(name.toLowerCase())).getHost().isEnabled()) {
                client.sendPacket(new PacketStartServer(6, tracker));
            } else if (!((SubServer) servers.get(name.toLowerCase())).isAvailable()) {
                client.sendPacket(new PacketStartServer(7, tracker));
            } else if (!((SubServer) servers.get(name.toLowerCase())).isEnabled()) {
                client.sendPacket(new PacketStartServer(8, tracker));
            } else if (((SubServer) servers.get(name.toLowerCase())).isRunning()) {
                client.sendPacket(new PacketStartServer(9, tracker));
            } else if (((SubServer) servers.get(name.toLowerCase())).getCurrentIncompatibilities().size() != 0) {
                String list = "";
                for (SubServer server : ((SubServer) servers.get(name.toLowerCase())).getCurrentIncompatibilities()) {
                    if (list.length() != 0) list += ", ";
                    list += server.getName();
                }
                client.sendPacket(new PacketStartServer(10, list, tracker));
            } else {
                if (((SubServer) servers.get(name.toLowerCase())).start(player)) {
                    client.sendPacket(new PacketStartServer(0, tracker));
                } else {
                    client.sendPacket(new PacketStartServer(1, tracker));
                }
            }
        } catch (Throwable e) {
            client.sendPacket(new PacketStartServer(2, tracker));
            e.printStackTrace();
        }
    }
}

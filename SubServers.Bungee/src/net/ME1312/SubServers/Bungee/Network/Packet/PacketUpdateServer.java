package net.ME1312.SubServers.Bungee.Network.Packet;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.SubData.Server.Protocol.PacketObjectIn;
import net.ME1312.SubData.Server.Protocol.PacketObjectOut;
import net.ME1312.SubData.Server.SubDataClient;
import net.ME1312.SubServers.Bungee.Host.Server;
import net.ME1312.SubServers.Bungee.Host.SubCreator;
import net.ME1312.SubServers.Bungee.Host.SubServer;
import net.ME1312.SubServers.Bungee.SubPlugin;

import java.util.Map;
import java.util.UUID;

/**
 * Update Server Packet
 */
public class PacketUpdateServer implements PacketObjectIn<Integer>, PacketObjectOut<Integer> {
    private SubPlugin plugin;
    private int response;
    private UUID tracker;

    /**
     * New PacketUpdateServer (In)
     *
     * @param plugin SubPlugin
     */
    public PacketUpdateServer(SubPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * New PacketUpdateServer (Out)
     *
     * @param response Response ID
     * @param id Receiver ID
     */
    public PacketUpdateServer(int response, UUID id) {
        this.response = response;
        this.tracker = id;
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
        UUID tracker =        (data.contains(0x0000)?data.getUUID(0x0000):null);
        try {
            String name =  data.getRawString(0x0001);
            Version version = (data.contains(0x0002)?data.getVersion(0x0002):null);
            UUID player =     (data.contains(0x0003)?data.getUUID(0x0003):null);
            boolean waitfor = (data.contains(0x0004)?data.getBoolean(0x0004):false);

            Map<String, Server> servers = plugin.api.getServers();
            if (!servers.keySet().contains(name.toLowerCase())) {
                client.sendPacket(new PacketUpdateServer(3, tracker));
            } else if (!(servers.get(name.toLowerCase()) instanceof SubServer)) {
                client.sendPacket(new PacketUpdateServer(4, tracker));
            } else if (!((SubServer) servers.get(name.toLowerCase())).getHost().isAvailable()) {
                client.sendPacket(new PacketUpdateServer(5, tracker));
            } else if (!((SubServer) servers.get(name.toLowerCase())).getHost().isEnabled()) {
                client.sendPacket(new PacketUpdateServer(6, tracker));
            } else if (!((SubServer) servers.get(name.toLowerCase())).isAvailable()) {
                client.sendPacket(new PacketUpdateServer(7, tracker));
            } else if (((SubServer) servers.get(name.toLowerCase())).isRunning()) {
                client.sendPacket(new PacketUpdateServer(8, tracker));
            } else if (((SubServer) servers.get(name.toLowerCase())).getTemplate() == null) {
                client.sendPacket(new PacketUpdateServer(9, tracker));
            } else if (!((SubServer) servers.get(name.toLowerCase())).getTemplate().isEnabled()) {
                client.sendPacket(new PacketUpdateServer(10, tracker));
            } else if (!((SubServer) servers.get(name.toLowerCase())).getTemplate().canUpdate()) {
                client.sendPacket(new PacketUpdateServer(11, tracker));
            } else if (version == null && ((SubServer) servers.get(name.toLowerCase())).getTemplate().requiresVersion()) {
                client.sendPacket(new PacketUpdateServer(12, tracker));
            } else {
                if (((SubServer) servers.get(name.toLowerCase())).getHost().getCreator().update(player, (SubServer) servers.get(name.toLowerCase()), version)) {
                    if (waitfor) {
                        new Thread(() -> {
                            try {
                                ((SubServer) servers.get(name.toLowerCase())).getHost().getCreator().waitFor();
                                client.sendPacket(new PacketUpdateServer(0, tracker));
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }, "SubServers.Bungee::SubData_SubCreator_Update_Handler(" + client.getAddress().toString() + ')').start();
                    } else {
                        client.sendPacket(new PacketUpdateServer(0, tracker));
                    }
                } else {
                    client.sendPacket(new PacketUpdateServer(1, tracker));
                }

            }
        } catch (Throwable e) {
            client.sendPacket(new PacketUpdateServer(2, tracker));
            e.printStackTrace();
        }
    }

    @Override
    public int version() {
        return 0x0001;
    }
}

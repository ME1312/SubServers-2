package net.ME1312.SubServers.Bungee.Network.Packet;

import net.ME1312.SubData.Server.SubDataClient;
import net.ME1312.SubServers.Bungee.Host.SubCreator;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.SubData.Server.Protocol.PacketObjectIn;
import net.ME1312.SubData.Server.Protocol.PacketObjectOut;
import net.ME1312.SubServers.Bungee.SubPlugin;

import java.util.UUID;

/**
 * Create Server Packet
 */
public class PacketCreateServer implements PacketObjectIn<Integer>, PacketObjectOut<Integer> {
    private SubPlugin plugin;
    private int response;
    private UUID tracker;

    /**
     * New PacketCreateServer (In)
     *
     * @param plugin SubPlugin
     */
    public PacketCreateServer(SubPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * New PacketCreateServer (Out)
     *
     * @param response Response ID
     * @param id Receiver ID
     */
    public PacketCreateServer(int response, UUID id) {
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
        UUID tracker =       (data.contains(0x0000)?data.getUUID(0x0000):null);
        try {
            String name =     data.getRawString(0x0001);
            String host =     data.getRawString(0x0002);
            String template = data.getRawString(0x0003);
            Version version =       (data.contains(0x0004)?data.getVersion(0x0004):null);
            Integer port =       (data.contains(0x0005)?data.getInt(0x0005):null);
            UUID player =        (data.contains(0x0006)?data.getUUID(0x0006):null);
            boolean waitfor =    (data.contains(0x0007)?data.getBoolean(0x0007):false);

            if (name.contains(" ")) {
                client.sendPacket(new PacketCreateServer(3, tracker));
            } else if (plugin.api.getSubServers().keySet().contains(name.toLowerCase()) || SubCreator.isReserved(name)) {
                client.sendPacket(new PacketCreateServer(4, tracker));
            } else if (!plugin.hosts.keySet().contains(host.toLowerCase())) {
                client.sendPacket(new PacketCreateServer(5, tracker));
            } else if (!plugin.hosts.get(host.toLowerCase()).isAvailable()) {
                client.sendPacket(new PacketCreateServer(6, tracker));
            } else if (!plugin.hosts.get(host.toLowerCase()).isEnabled()) {
                client.sendPacket(new PacketCreateServer(7, tracker));
            } else if (!plugin.hosts.get(host.toLowerCase()).getCreator().getTemplates().keySet().contains(template.toLowerCase())) {
                client.sendPacket(new PacketCreateServer(8, tracker));
            } else if (!plugin.hosts.get(host.toLowerCase()).getCreator().getTemplate(template).isEnabled()) {
                client.sendPacket(new PacketCreateServer(9, tracker));
            } else if (version == null && plugin.hosts.get(host.toLowerCase()).getCreator().getTemplate(template).requiresVersion()) {
                client.sendPacket(new PacketCreateServer(10, tracker));
            } else if (port != null && (port <= 0 || port > 65535)) {
                client.sendPacket(new PacketCreateServer(11, tracker));
            } else {
                if (plugin.hosts.get(host.toLowerCase()).getCreator().create(player, name, plugin.hosts.get(host.toLowerCase()).getCreator().getTemplate(template), version, port)) {
                    if (waitfor) {
                        new Thread(() -> {
                            try {
                                plugin.hosts.get(host.toLowerCase()).getCreator().waitFor();
                                client.sendPacket(new PacketCreateServer(0, tracker));
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }, "SubServers.Bungee::SubData_SubCreator_Handler(" + client.getAddress().toString() + ')').start();
                    } else {
                        client.sendPacket(new PacketCreateServer(0, tracker));
                    }
                } else {
                    client.sendPacket(new PacketCreateServer(1, tracker));
                }

            }
        } catch (Throwable e) {
            client.sendPacket(new PacketCreateServer(2, tracker));
            e.printStackTrace();
        }
    }

    @Override
    public int version() {
        return 0x0001;
    }
}

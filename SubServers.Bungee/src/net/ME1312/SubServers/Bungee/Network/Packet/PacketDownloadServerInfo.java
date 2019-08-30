package net.ME1312.SubServers.Bungee.Network.Packet;

import net.ME1312.SubData.Server.SubDataClient;
import net.ME1312.SubServers.Bungee.Host.Server;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Server.Protocol.PacketObjectIn;
import net.ME1312.SubData.Server.Protocol.PacketObjectOut;
import net.ME1312.SubServers.Bungee.SubProxy;

import java.util.UUID;

/**
 * Download Server Info Packet
 */
public class PacketDownloadServerInfo implements PacketObjectIn<Integer>, PacketObjectOut<Integer> {
    private SubProxy plugin;
    private String server;
    private UUID tracker;

    /**
     * New PacketDownloadServerInfo (In)
     *
     * @param plugin SubPlugin
     */
    public PacketDownloadServerInfo(SubProxy plugin) {
        if (Util.isNull(plugin)) throw new NullPointerException();
        this.plugin = plugin;
    }

    /**
     * New PacketDownloadServerInfo (Out)
     *
     * @param plugin SubPlugin
     * @param server Server (or null for all)
     * @param tracker Receiver ID
     */
    public PacketDownloadServerInfo(SubProxy plugin, String server, UUID tracker) {
        if (Util.isNull(plugin)) throw new NullPointerException();
        this.plugin = plugin;
        this.server = server;
        this.tracker = tracker;
    }

    @SuppressWarnings("unchecked")
    @Override
    public ObjectMap<Integer> send(SubDataClient client) {
        ObjectMap<Integer> data = new ObjectMap<Integer>();
        if (tracker != null) data.set(0x0000, tracker);

        ObjectMap<String> servers = new ObjectMap<String>();
        for (Server server : plugin.api.getServers().values()) {
            if (this.server == null || this.server.length() <= 0 || this.server.equalsIgnoreCase(server.getName())) {
                servers.set(server.getName(), server.forSubData());
            }
        }
        data.set(0x0001, servers);
        return data;
    }

    @Override
    public void receive(SubDataClient client, ObjectMap<Integer> data) {
        client.sendPacket(new PacketDownloadServerInfo(plugin, (data.contains(0x0001))?data.getRawString(0x0001):null, (data.contains(0x0000))?data.getUUID(0x0000):null));
    }

    @Override
    public int version() {
        return 0x0001;
    }
}

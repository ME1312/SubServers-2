package net.ME1312.SubServers.Bungee.Network.Packet;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Server.Protocol.PacketObjectIn;
import net.ME1312.SubData.Server.Protocol.PacketObjectOut;
import net.ME1312.SubData.Server.SubDataClient;
import net.ME1312.SubServers.Bungee.Host.Server;
import net.ME1312.SubServers.Bungee.SubProxy;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Download Server Info Packet
 */
public class PacketDownloadServerInfo implements PacketObjectIn<Integer>, PacketObjectOut<Integer> {
    private SubProxy plugin;
    private String[] servers;
    private UUID tracker;

    /**
     * New PacketDownloadServerInfo (In)
     *
     * @param plugin SubPlugin
     */
    public PacketDownloadServerInfo(SubProxy plugin) {
        this.plugin = Util.nullpo(plugin);
    }

    /**
     * New PacketDownloadServerInfo (Out)
     *
     * @param plugin SubPlugin
     * @param servers Servers (or null for all)
     * @param tracker Receiver ID
     */
    public PacketDownloadServerInfo(SubProxy plugin, List<String> servers, UUID tracker) {
        this.plugin = Util.nullpo(plugin);
        this.tracker = tracker;

        if (servers != null) {
            this.servers = new String[servers.size()];
            for (int i = 0; i < this.servers.length; ++i) this.servers[i] = servers.get(i).toLowerCase();
            Arrays.sort(this.servers);
        }
    }

    @Override
    public ObjectMap<Integer> send(SubDataClient client) {
        ObjectMap<Integer> data = new ObjectMap<Integer>();
        if (tracker != null) data.set(0x0000, tracker);

        ObjectMap<String> servers = new ObjectMap<String>();
        for (Server server : plugin.api.getServers().values()) {
            if (this.servers == null || this.servers.length <= 0 || Arrays.binarySearch(this.servers, server.getName().toLowerCase()) >= 0) {
                servers.set(server.getName(), server.forSubData());
            }
        }
        data.set(0x0001, servers);
        return data;
    }

    @Override
    public void receive(SubDataClient client, ObjectMap<Integer> data) {
        client.sendPacket(new PacketDownloadServerInfo(plugin, (data.contains(0x0001))?data.getStringList(0x0001):null, (data.contains(0x0000))?data.getUUID(0x0000):null));
    }
}

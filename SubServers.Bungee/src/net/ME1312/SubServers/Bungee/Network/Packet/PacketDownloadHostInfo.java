package net.ME1312.SubServers.Bungee.Network.Packet;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Server.Protocol.PacketObjectIn;
import net.ME1312.SubData.Server.Protocol.PacketObjectOut;
import net.ME1312.SubData.Server.SubDataClient;
import net.ME1312.SubServers.Bungee.Host.Host;
import net.ME1312.SubServers.Bungee.SubProxy;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Download Host Info Packet
 */
public class PacketDownloadHostInfo implements PacketObjectIn<Integer>, PacketObjectOut<Integer> {
    private SubProxy plugin;
    private String[] hosts;
    private UUID tracker;

    /**
     * New PacketDownloadHostInfo (In)
     *
     * @param plugin SubPlugin
     */
    public PacketDownloadHostInfo(SubProxy plugin) {
        Util.nullpo(plugin);
        this.plugin = plugin;
    }

    /**
     * New PacketDownloadHostInfo (Out)
     *
     * @param plugin SubPlugin
     * @param hosts Hosts (or null for all)
     * @param tracker Receiver ID
     */
    public PacketDownloadHostInfo(SubProxy plugin, List<String> hosts, UUID tracker) {
        Util.nullpo(plugin);
        this.plugin = plugin;
        this.tracker = tracker;

        if (hosts != null) {
            this.hosts = new String[hosts.size()];
            for (int i = 0; i < this.hosts.length; ++i) this.hosts[i] = hosts.get(i).toLowerCase();
            Arrays.sort(this.hosts);
        }
    }

    @Override
    public ObjectMap<Integer> send(SubDataClient client) {
        ObjectMap<Integer> data = new ObjectMap<Integer>();
        if (tracker != null) data.set(0x0000, tracker);

        ObjectMap<String> hosts = new ObjectMap<String>();
        for (Host host : plugin.api.getHosts().values()) {
            if (this.hosts == null || this.hosts.length <= 0 || Arrays.binarySearch(this.hosts, host.getName().toLowerCase()) >= 0) {
                hosts.set(host.getName(), host.forSubData());
            }
        }
        data.set(0x0001, hosts);
        return data;
    }

    @Override
    public void receive(SubDataClient client, ObjectMap<Integer> data) {
        client.sendPacket(new PacketDownloadHostInfo(plugin, (data.contains(0x0001))?data.getRawStringList(0x0001):null, (data.contains(0x0000))?data.getUUID(0x0000):null));
    }

    @Override
    public int version() {
        return 0x0001;
    }
}

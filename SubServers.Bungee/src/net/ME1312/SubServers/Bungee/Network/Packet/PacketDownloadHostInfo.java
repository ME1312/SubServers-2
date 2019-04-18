package net.ME1312.SubServers.Bungee.Network.Packet;

import net.ME1312.SubData.Server.SubDataClient;
import net.ME1312.SubServers.Bungee.Host.Host;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Server.Protocol.PacketObjectIn;
import net.ME1312.SubData.Server.Protocol.PacketObjectOut;
import net.ME1312.SubServers.Bungee.SubPlugin;

import java.util.UUID;

/**
 * Download Host Info Packet
 */
public class PacketDownloadHostInfo implements PacketObjectIn<Integer>, PacketObjectOut<Integer> {
    private SubPlugin plugin;
    private String host;
    private UUID tracker;

    /**
     * New PacketDownloadHostInfo (In)
     *
     * @param plugin SubPlugin
     */
    public PacketDownloadHostInfo(SubPlugin plugin) {
        if (Util.isNull(plugin)) throw new NullPointerException();
        this.plugin = plugin;
    }

    /**
     * New PacketDownloadHostInfo (Out)
     *
     * @param plugin SubPlugin
     * @param host Host (or null for all)
     * @param tracker Receiver ID
     */
    public PacketDownloadHostInfo(SubPlugin plugin, String host, UUID tracker) {
        if (Util.isNull(plugin)) throw new NullPointerException();
        this.plugin = plugin;
        this.host = host;
        this.tracker = tracker;
    }

    @SuppressWarnings("unchecked")
    @Override
    public ObjectMap<Integer> send(SubDataClient client) {
        ObjectMap<Integer> data = new ObjectMap<Integer>();
        if (tracker != null) data.set(0x0000, tracker);

        ObjectMap<String> hosts = new ObjectMap<String>();
        for (Host host : plugin.api.getHosts().values()) {
            if (this.host == null || this.host.length() <= 0 || this.host.equalsIgnoreCase(host.getName())) {
                hosts.set(host.getName(), host.forSubData());
            }
        }
        data.set(0x0001, hosts);
        return data;
    }

    @Override
    public void receive(SubDataClient client, ObjectMap<Integer> data) {
        client.sendPacket(new PacketDownloadHostInfo(plugin, (data.contains(0x0001))?data.getRawString(0x0001):null, (data.contains(0x0000))?data.getUUID(0x0000):null));
    }

    @Override
    public int version() {
        return 0x0001;
    }
}

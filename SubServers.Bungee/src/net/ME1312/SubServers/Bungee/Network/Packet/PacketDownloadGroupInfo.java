package net.ME1312.SubServers.Bungee.Network.Packet;

import net.ME1312.SubServers.Bungee.Host.Server;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Server.SubDataClient;
import net.ME1312.SubData.Server.Protocol.PacketObjectIn;
import net.ME1312.SubData.Server.Protocol.PacketObjectOut;
import net.ME1312.SubServers.Bungee.SubPlugin;

import java.util.UUID;

/**
 * Download Group Info Packet
 */
public class PacketDownloadGroupInfo implements PacketObjectIn<Integer>, PacketObjectOut<Integer> {
    private SubPlugin plugin;
    private String group;
    private UUID tracker;

    /**
     * New PacketDownloadGroupInfo (In)
     *
     * @param plugin SubPlugin
     */
    public PacketDownloadGroupInfo(SubPlugin plugin) {
        if (Util.isNull(plugin)) throw new NullPointerException();
        this.plugin = plugin;
    }

    /**
     * New PacketDownloadGroupInfo (Out)
     *
     * @param plugin SubPlugin
     * @param group Group (or null for all)
     * @param tracker Receiver ID
     */
    public PacketDownloadGroupInfo(SubPlugin plugin, String group, UUID tracker) {
        if (Util.isNull(plugin)) throw new NullPointerException();
        this.plugin = plugin;
        this.group = group;
        this.tracker = tracker;
    }

    @SuppressWarnings("unchecked")
    @Override
    public ObjectMap<Integer> send(SubDataClient client) {
        ObjectMap<Integer> data = new ObjectMap<Integer>();
        if (tracker != null) data.set(0x0000, tracker);

        ObjectMap<String> groups = new ObjectMap<String>();
        for (String group : plugin.api.getGroups().keySet()) {
            if (this.group == null || this.group.length() <= 0 || this.group.equalsIgnoreCase(group)) {
                ObjectMap<String> servers = new ObjectMap<String>();
                for (Server server : plugin.api.getGroup(group)) {
                    servers.set(server.getName(), server.forSubData());
                }
                groups.set(group, servers);
            }
        }
        data.set(0x0001, groups);
        return data;
    }

    @Override
    public void receive(SubDataClient client, ObjectMap<Integer> data) {
        client.sendPacket(new PacketDownloadGroupInfo(plugin, (data.contains(0x0001))?data.getRawString(0x0001):null, (data.contains(0x0000))?data.getUUID(0x0000):null));
    }

    @Override
    public int version() {
        return 0x0001;
    }
}

package net.ME1312.SubServers.Bungee.Network.Packet;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Server.Protocol.PacketObjectIn;
import net.ME1312.SubData.Server.Protocol.PacketObjectOut;
import net.ME1312.SubData.Server.SubDataClient;
import net.ME1312.SubServers.Bungee.Host.Server;
import net.ME1312.SubServers.Bungee.SubProxy;

import java.util.*;

/**
 * Download Group Info Packet
 */
public class PacketDownloadGroupInfo implements PacketObjectIn<Integer>, PacketObjectOut<Integer> {
    private SubProxy plugin;
    private String[] groups;
    private UUID tracker;

    /**
     * New PacketDownloadGroupInfo (In)
     *
     * @param plugin SubPlugin
     */
    public PacketDownloadGroupInfo(SubProxy plugin) {
        this.plugin = Util.nullpo(plugin);
    }

    /**
     * New PacketDownloadGroupInfo (Out)
     *
     * @param plugin SubPlugin
     * @param groups Groups (or null for all)
     * @param tracker Receiver ID
     */
    public PacketDownloadGroupInfo(SubProxy plugin, List<String> groups, UUID tracker) {
        this.plugin = Util.nullpo(plugin);
        this.tracker = tracker;

        if (groups != null) {
            this.groups = new String[groups.size()];
            for (int i = 0; i < this.groups.length; ++i) this.groups[i] = groups.get(i).toLowerCase();
            Arrays.sort(this.groups);
        }
    }

    @Override
    public ObjectMap<Integer> send(SubDataClient client) {
        ObjectMap<Integer> data = new ObjectMap<Integer>();
        if (tracker != null) data.set(0x0000, tracker);

        if (this.groups == null || this.groups.length > 0) {
            ObjectMap<String> groups = new ObjectMap<String>();
            for (Map.Entry<String, List<Server>> group : plugin.api.getGroups().entrySet()) {
                if (this.groups == null || Arrays.binarySearch(this.groups, group.getKey().toLowerCase()) >= 0) {
                    ObjectMap<String> servers = new ObjectMap<String>();
                    for (Server server : group.getValue()) {
                        servers.set(server.getName(), server.forSubData());
                    }
                    groups.set(group.getKey(), servers);
                }
            }
            data.set(0x0001, groups);
        } else {
            ObjectMap<String> ungrouped = new ObjectMap<String>();
            for (Server server : plugin.api.getServers().values()) {
                if (server.getGroups().size() <= 0) ungrouped.set(server.getName(), server.forSubData());
            }

            data.set(0x0001, Collections.emptyMap());
            data.set(0x0002, ungrouped);
        }
        return data;
    }

    @Override
    public void receive(SubDataClient client, ObjectMap<Integer> data) {
        client.sendPacket(new PacketDownloadGroupInfo(plugin, (data.contains(0x0001))?data.getStringList(0x0001):null, (data.contains(0x0000))?data.getUUID(0x0000):null));
    }
}

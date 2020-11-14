package net.ME1312.SubServers.Bungee.Network.Packet;

import net.ME1312.SubData.Server.SubDataClient;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Server.Protocol.PacketObjectIn;
import net.ME1312.SubData.Server.Protocol.PacketObjectOut;
import net.ME1312.SubServers.Bungee.Host.RemotePlayer;
import net.ME1312.SubServers.Bungee.SubProxy;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Download Player Info Packet
 */
public class PacketDownloadPlayerInfo implements PacketObjectIn<Integer>, PacketObjectOut<Integer> {
    private SubProxy plugin;
    private String[] names;
    private UUID[] ids;
    private UUID tracker;

    /**
     * New PacketDownloadPlayerInfo (In)
     *
     * @param plugin SubPlugin
     */
    public PacketDownloadPlayerInfo(SubProxy plugin) {
        if (Util.isNull(plugin)) throw new NullPointerException();
        this.plugin = plugin;
    }

    /**
     * New PacketDownloadPlayerInfo (Out)
     *
     * @param plugin SubPlugin
     * @param names Player names (or null for all)
     * @param ids Player IDs (or null for all)
     * @param tracker Receiver ID
     */
    public PacketDownloadPlayerInfo(SubProxy plugin, List<String> names, List<UUID> ids, UUID tracker) {
        if (Util.isNull(plugin)) throw new NullPointerException();
        this.plugin = plugin;
        this.tracker = tracker;

        if (ids != null) {
            this.ids = new UUID[ids.size()];
            for (int i = 0; i < this.ids.length; ++i) this.ids[i] = ids.get(i);
        }
        if (names != null) {
            this.names = new String[names.size()];
            for (int i = 0; i < this.names.length; ++i) this.names[i] = names.get(i);
        }
    }

    @Override
    public ObjectMap<Integer> send(SubDataClient client) {
        ObjectMap<Integer> data = new ObjectMap<Integer>();
        if (tracker != null) data.set(0x0000, tracker);

        ObjectMap<String> players = new ObjectMap<String>();
        if (ids == null && names == null) {
            for (RemotePlayer player : plugin.api.getRemotePlayers().values()) {
                players.set(player.getUniqueId().toString(), player.forSubData());
            }
        } else {
            if (ids != null) for (UUID id : ids) {
                RemotePlayer player = plugin.api.getRemotePlayer(id);
                if (player != null) players.set(player.getUniqueId().toString(), player.forSubData());
            }
            if (names != null) for (String name : names) {
                RemotePlayer player = plugin.api.getRemotePlayer(name);
                if (player != null) players.set(player.getUniqueId().toString(), player.forSubData());
            }
        }
        data.set(0x0001, players);
        return data;
    }

    @Override
    public void receive(SubDataClient client, ObjectMap<Integer> data) {
        client.sendPacket(new PacketDownloadPlayerInfo(plugin, (data.contains(0x0001))?data.getRawStringList(0x0001):null, (data.contains(0x0002))?data.getUUIDList(0x0002):null, (data.contains(0x0000))?data.getUUID(0x0000):null));
    }

    @Override
    public int version() {
        return 0x0001;
    }
}

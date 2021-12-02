package net.ME1312.SubServers.Sync.Network.Packet;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Client.Protocol.PacketObjectIn;
import net.ME1312.SubData.Client.Protocol.PacketObjectOut;
import net.ME1312.SubData.Client.SubDataSender;
import net.ME1312.SubServers.Sync.ExProxy;
import net.ME1312.SubServers.Sync.Server.CachedPlayer;
import net.ME1312.SubServers.Sync.Server.ServerImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * External Player Sync Packet
 */
public class PacketExSyncPlayer implements PacketObjectIn<Integer>, PacketObjectOut<Integer> {
    private ExProxy plugin;
    private Boolean mode;
    private CachedPlayer[] values;

    /**
     * New PacketExSyncPlayer (In)
     *
     * @param plugin SubPlugin
     */
    public PacketExSyncPlayer(ExProxy plugin) {
        Util.nullpo(plugin);
        this.plugin = plugin;
    }

    /**
     * New PacketExSyncPlayer (Out)
     *
     * @param mode Update Mode (true for add, false for remove, null for reset)
     * @param values RemotePlayers
     */
    public PacketExSyncPlayer(Boolean mode, CachedPlayer... values) {
        this.mode = mode;
        this.values = values;
    }

    @Override
    public ObjectMap<Integer> send(SubDataSender client) {
        ObjectMap<Integer> data = new ObjectMap<Integer>();
        data.set(0x0001, mode);
        if (values != null) {
            ArrayList<ObjectMap<String>> list = new ArrayList<ObjectMap<String>>();
            for (CachedPlayer value : values) list.add(value.getRaw());
            data.set(0x0002, list);
        }
        return data;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void receive(SubDataSender client, ObjectMap<Integer> data) {
        String proxy = (data.contains(0x0000)?data.getString(0x0000).toLowerCase():null);
        synchronized (plugin.rPlayers) {
            if (data.getBoolean(0x0001) == null) {
                for (UUID id : Util.getBackwards(plugin.rPlayerLinkP, proxy)) {
                    plugin.rPlayerLinkS.remove(id);
                    plugin.rPlayerLinkP.remove(id);
                    plugin.rPlayers.remove(id);
                }
            }
            if (data.getBoolean(0x0001) != Boolean.FALSE) {
                if (data.contains(0x0002)) for (ObjectMap<String> object : (List<ObjectMap<String>>) (List<?>) data.getMapList(0x0002)) {
                    ServerImpl server = (object.contains("server"))?plugin.servers.getOrDefault(object.getString("server").toLowerCase(), null):null;
                    CachedPlayer player = new CachedPlayer(object);

                    plugin.rPlayerLinkP.put(player.getUniqueId(), proxy);
                    plugin.rPlayers.put(player.getUniqueId(), player);
                    if (server != null) plugin.rPlayerLinkS.put(player.getUniqueId(), server);
                }
            } else {
                if (data.contains(0x0002)) for (ObjectMap<String> object : (List<ObjectMap<String>>) (List<?>) data.getMapList(0x0002)) {
                    UUID id = object.getUUID("id");

                    // Don't accept removal requests when we're managing players
                    if ((!plugin.rPlayerLinkP.containsKey(id) || !plugin.rPlayerLinkP.get(id).equalsIgnoreCase(plugin.api.getName().toLowerCase()))) {
                        plugin.rPlayerLinkS.remove(id);
                        plugin.rPlayerLinkP.remove(id);
                        plugin.rPlayers.remove(id);
                    }
                }
            }
        }
    }
}

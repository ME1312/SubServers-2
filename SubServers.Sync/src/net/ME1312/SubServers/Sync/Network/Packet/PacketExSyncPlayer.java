package net.ME1312.SubServers.Sync.Network.Packet;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Client.Protocol.PacketObjectIn;
import net.ME1312.SubData.Client.Protocol.PacketObjectOut;
import net.ME1312.SubData.Client.SubDataSender;
import net.ME1312.SubServers.Sync.Network.API.RemotePlayer;
import net.ME1312.SubServers.Sync.Server.ServerImpl;
import net.ME1312.SubServers.Sync.ExProxy;

import java.util.*;

/**
 * External Player Sync Packet
 */
public class PacketExSyncPlayer implements PacketObjectIn<Integer>, PacketObjectOut<Integer> {
    private ExProxy plugin;
    private Boolean mode;
    private RemotePlayer[] values;

    /**
     * New PacketExSyncPlayer (In)
     *
     * @param plugin SubPlugin
     */
    public PacketExSyncPlayer(ExProxy plugin) {
        if (Util.isNull(plugin)) throw new NullPointerException();
        this.plugin = plugin;
    }

    /**
     * New PacketExSyncPlayer (Out)
     *
     * @param mode Update Mode (true for add, false for remove, null for reset)
     * @param values RemotePlayers
     */
    public PacketExSyncPlayer(Boolean mode, RemotePlayer... values) {
        this.mode = mode;
        this.values = values;
    }

    @Override
    public ObjectMap<Integer> send(SubDataSender client) {
        ObjectMap<Integer> data = new ObjectMap<Integer>();
        data.set(0x0001, mode);
        if (values != null) {
            ArrayList<ObjectMap<String>> list = new ArrayList<ObjectMap<String>>();
            for (RemotePlayer value : values) list.add(value.getRaw());
            data.set(0x0002, list);
        }
        return data;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void receive(SubDataSender client, ObjectMap<Integer> data) {
        String proxy = (data.contains(0x0000)?data.getRawString(0x0000).toLowerCase():null);
        if (data.getBoolean(0x0001) == null) {
            for (UUID id : Util.getBackwards(plugin.rPlayerLinkP, proxy)) {
                plugin.rPlayerLinkS.remove(id);
                plugin.rPlayerLinkP.remove(id);
                plugin.rPlayers.remove(id);
            }
        }
        if (data.getBoolean(0x0001) != Boolean.FALSE) {
            if (data.contains(0x0002)) for (Map<String, Object> object : (List<Map<String, Object>>) data.getObjectList(0x0002)) {
                ServerImpl server = (object.getOrDefault("server", null) != null)?plugin.servers.getOrDefault(object.get("server").toString().toLowerCase(), null):null;
                RemotePlayer player = new RemotePlayer(new ObjectMap<>(object));

                plugin.rPlayers.put(player.getUniqueId(), player);
                plugin.rPlayerLinkP.put(player.getUniqueId(), proxy);
                if (server != null) plugin.rPlayerLinkS.put(player.getUniqueId(), server);
            }
        } else {
            if (data.contains(0x0002)) for (Map<String, Object> object : (List<Map<String, Object>>) data.getObjectList(0x0002)) {
                UUID id = UUID.fromString(object.get("id").toString());
                plugin.rPlayerLinkS.remove(id);
                plugin.rPlayerLinkP.remove(id);
                plugin.rPlayers.remove(id);
            }
        }
    }

    @Override
    public int version() {
        return 0x0001;
    }
}

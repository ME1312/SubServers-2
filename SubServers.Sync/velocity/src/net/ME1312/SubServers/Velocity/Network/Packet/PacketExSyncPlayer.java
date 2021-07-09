package net.ME1312.SubServers.Velocity.Network.Packet;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Client.Protocol.PacketObjectIn;
import net.ME1312.SubData.Client.Protocol.PacketObjectOut;
import net.ME1312.SubData.Client.SubDataSender;
import net.ME1312.SubServers.Velocity.ExProxy;
import net.ME1312.SubServers.Velocity.Server.CachedPlayer;
import net.ME1312.SubServers.Velocity.Server.ServerData;

import com.velocitypowered.api.proxy.server.RegisteredServer;

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
        if (Util.isNull(plugin)) throw new NullPointerException();
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
        String proxy = (data.contains(0x0000)?data.getRawString(0x0000).toLowerCase():null);
        synchronized (plugin.rPlayers) {
            if (data.getBoolean(0x0001) == null) {
                for (UUID id : Util.getBackwards(plugin.rPlayerLinkP, proxy)) {
                    plugin.rPlayerLinkS.remove(id);
                    plugin.rPlayerLinkP.remove(id);
                    plugin.rPlayers.remove(id);
                }
            }
            if (data.getBoolean(0x0001) != Boolean.FALSE) {
                if (data.contains(0x0002)) for (Map<String, Object> object : (List<Map<String, Object>>) data.getObjectList(0x0002)) {
                    ServerData server = (object.getOrDefault("server", null) != null)?ExProxy.getInstance().getServer(object.get("server").toString()).map(RegisteredServer::getServerInfo).map(plugin::getData).orElse(null):null;
                    CachedPlayer player = new CachedPlayer(new ObjectMap<>(object));

                    plugin.rPlayerLinkP.put(player.getUniqueId(), proxy);
                    plugin.rPlayers.put(player.getUniqueId(), player);
                    if (server != null) plugin.rPlayerLinkS.put(player.getUniqueId(), server);
                }
            } else {
                if (data.contains(0x0002)) for (Map<String, Object> object : (List<Map<String, Object>>) data.getObjectList(0x0002)) {
                    UUID id = UUID.fromString(object.get("id").toString());

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

    @Override
    public int version() {
        return 0x0001;
    }
}

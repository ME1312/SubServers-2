package net.ME1312.SubServers.Bungee.Network.Packet;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Server.Protocol.PacketObjectIn;
import net.ME1312.SubData.Server.Protocol.PacketObjectOut;
import net.ME1312.SubData.Server.SubDataClient;
import net.ME1312.SubServers.Bungee.Host.Proxy;
import net.ME1312.SubServers.Bungee.Host.RemotePlayer;
import net.ME1312.SubServers.Bungee.Host.Server;
import net.ME1312.SubServers.Bungee.SubAPI;
import net.ME1312.SubServers.Bungee.SubProxy;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * External Player Sync Packet
 */
public class PacketExSyncPlayer implements PacketObjectIn<Integer>, PacketObjectOut<Integer> {
    private SubProxy plugin;
    private String name;
    private Boolean mode;
    private RemotePlayer[] values;

    /**
     * New PacketExSyncPlayer (In)
     *
     * @param plugin SubPlugin
     */
    public PacketExSyncPlayer(SubProxy plugin) {
        Util.nullpo(plugin);
        this.plugin = plugin;
    }

    /**
     * New PacketExSyncPlayer (Out)
     *
     * @param name Proxy Name
     * @param mode Update Mode (true for add, false for remove, null for reset)
     * @param values RemotePlayers
     */
    public PacketExSyncPlayer(String name, Boolean mode, RemotePlayer... values) {
        this.name = name;
        this.mode = mode;
        this.values = values;
    }

    @Override
    public ObjectMap<Integer> send(SubDataClient client) {
        ObjectMap<Integer> data = new ObjectMap<Integer>();
        data.set(0x0000, name);
        data.set(0x0001, mode);
        if (values != null) {
            ArrayList<ObjectMap<String>> list = new ArrayList<ObjectMap<String>>();
            for (RemotePlayer value : values) list.add(value.forSubData());
            data.set(0x0002, list);
        }
        return data;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void receive(SubDataClient client, ObjectMap<Integer> data) {
        if (client.getHandler() instanceof Proxy) {
            ArrayList<RemotePlayer> forward = new ArrayList<RemotePlayer>();
            synchronized (plugin.rPlayers) {
                if (data.getBoolean(0x0001) == null) {
                    for (UUID id : Util.getBackwards(plugin.rPlayerLinkP, (Proxy) client.getHandler())) {
                        plugin.rPlayerLinkS.remove(id);
                        plugin.rPlayerLinkP.remove(id);
                        plugin.rPlayers.remove(id);
                    }
                }
                if (data.getBoolean(0x0001) != Boolean.FALSE) {
                    if (data.contains(0x0002)) for (ObjectMap<String> object : (List<ObjectMap<String>>) (List<?>) data.getMapList(0x0002)) {
                        Server server = (object.contains("server"))?plugin.api.getServer(object.getString("server")):null;
                        RemotePlayer player = new RemotePlayer(object.getString("name"), object.getUUID("id"), (Proxy) client.getHandler(), server,
                                new InetSocketAddress(object.getString("address").split(":")[0], Integer.parseInt(object.getString("address").split(":")[1])));

                        forward.add(player);
                        plugin.rPlayerLinkP.put(player.getUniqueId(), (Proxy) client.getHandler());
                        plugin.rPlayers.put(player.getUniqueId(), player);
                        if (server != null) plugin.rPlayerLinkS.put(player.getUniqueId(), server);
                    }
                } else {
                    if (data.contains(0x0002)) for (ObjectMap<String> object : (List<ObjectMap<String>>) (List<?>) data.getMapList(0x0002)) {
                        UUID id = object.getUUID("id");
                        RemotePlayer player = plugin.rPlayers.get(id);

                        // Don't accept removal requests from non-managing proxies
                        if (player == null || player.getProxy() == null || client.getHandler().equals(plugin.rPlayerLinkP.get(id))) {
                            if (player != null) forward.add(player);
                            plugin.rPlayerLinkS.remove(id);
                            plugin.rPlayerLinkP.remove(id);
                            plugin.rPlayers.remove(id);
                        }
                    }
                }

                if (data.getBoolean(0x0001) == null || forward.size() > 0) {
                    PacketExSyncPlayer packet = new PacketExSyncPlayer(((Proxy) client.getHandler()).getName(), data.getBoolean(0x0001), forward.toArray(new RemotePlayer[0]));
                    for (Proxy proxy : SubAPI.getInstance().getProxies().values()) if (proxy.getSubData()[0] != null && proxy != client.getHandler()) {
                        ((SubDataClient) proxy.getSubData()[0]).sendPacket(packet);
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

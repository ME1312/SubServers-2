package net.ME1312.SubServers.Bungee.Network.Packet;

import net.ME1312.Galaxi.Library.AsyncConsolidator;
import net.ME1312.Galaxi.Library.Callback.Callback;
import net.ME1312.Galaxi.Library.Container.Container;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.SubData.Server.Protocol.PacketObjectIn;
import net.ME1312.SubData.Server.Protocol.PacketObjectOut;
import net.ME1312.SubData.Server.SubDataClient;
import net.ME1312.SubServers.Bungee.Host.Proxy;
import net.ME1312.SubServers.Bungee.Host.RemotePlayer;
import net.ME1312.SubServers.Bungee.Host.Server;
import net.ME1312.SubServers.Bungee.SubAPI;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.*;

/**
 * Disconnect Player Packet
 */
public class PacketDisconnectPlayer implements PacketObjectIn<Integer>, PacketObjectOut<Integer> {
    private int response;
    private UUID tracker;

    /**
     * New PacketDisconnectPlayer (In)
     */
    public PacketDisconnectPlayer() {

    }

    /**
     * New PacketDisconnectPlayer (Out)
     *
     * @param response Response ID
     * @param tracker Receiver ID
     */
    public PacketDisconnectPlayer(int response, UUID tracker) {
        this.response = response;
        this.tracker = tracker;
    }

    @Override
    public ObjectMap<Integer> send(SubDataClient client) {
        ObjectMap<Integer> json = new ObjectMap<Integer>();
        if (tracker != null) json.set(0x0000, tracker);
        json.set(0x0001, response);
        return json;
    }

    @Override
    public void receive(SubDataClient client, ObjectMap<Integer> data) {
        UUID tracker = (data.contains(0x0000)?data.getUUID(0x0000):null);
        run(data.getUUIDList(0x0001), data.contains(0x0002)?data.getRawString(0x0002):null, i -> {
            client.sendPacket(new PacketDisconnectPlayer(i, tracker));
        });
    }

    @SuppressWarnings("deprecation")
    public static void run(List<UUID> ids, String reason, Callback<Integer> callback) {
        try {
            Container<Integer> failures = new Container<>(0);
            HashMap<Proxy, List<UUID>> requests = new HashMap<Proxy, List<UUID>>();
            for (UUID id : ids) {
                ProxiedPlayer local;
                RemotePlayer remote;
                if ((local = ProxyServer.getInstance().getPlayer(id)) != null) {
                    if (reason != null) {
                        local.disconnect(reason);
                    } else local.disconnect();
                } else if ((remote = SubAPI.getInstance().getRemotePlayer(id)) != null && remote.getProxy().getSubData()[0] != null) {
                    Proxy proxy = remote.getProxy();
                    List<UUID> list = requests.getOrDefault(proxy, new ArrayList<>());
                    list.add(id);
                    requests.put(proxy, list);
                } else {
                    ++failures.value;
                }
            }

            if (requests.size() == 0) {
                callback.run(failures.value);
            } else {
                AsyncConsolidator merge = new AsyncConsolidator(() -> {
                    callback.run(failures.value);
                });
                for (Map.Entry<Proxy, List<UUID>> entry : requests.entrySet()) {
                    merge.reserve();
                    ((SubDataClient) entry.getKey().getSubData()[0]).sendPacket(new PacketExDisconnectPlayer(entry.getValue(), reason, r -> {
                        failures.value += r.getInt(0x0001);
                        merge.release();
                    }));
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
            callback.run(-1);
        }
    }

    @Override
    public int version() {
        return 0x0001;
    }
}

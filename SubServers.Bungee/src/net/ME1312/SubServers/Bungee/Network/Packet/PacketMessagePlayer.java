package net.ME1312.SubServers.Bungee.Network.Packet;

import net.ME1312.Galaxi.Library.AsyncConsolidator;
import net.ME1312.Galaxi.Library.Callback.Callback;
import net.ME1312.Galaxi.Library.Container.ContainedPair;
import net.ME1312.Galaxi.Library.Container.Container;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Server.Protocol.PacketObjectIn;
import net.ME1312.SubData.Server.Protocol.PacketObjectOut;
import net.ME1312.SubData.Server.SubDataClient;
import net.ME1312.SubServers.Bungee.Host.Proxy;
import net.ME1312.SubServers.Bungee.Host.RemotePlayer;
import net.ME1312.SubServers.Bungee.SubAPI;
import net.ME1312.SubServers.Bungee.SubProxy;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.chat.ComponentSerializer;

import java.util.*;

/**
 * Message Player Packet
 */
public class PacketMessagePlayer implements PacketObjectIn<Integer>, PacketObjectOut<Integer> {
    private int response;
    private UUID tracker;

    /**
     * New PacketMessagePlayer (In)
     */
    public PacketMessagePlayer() {

    }

    /**
     * New PacketMessagePlayer (Out)
     *
     * @param response Response ID
     * @param tracker Receiver ID
     */
    public PacketMessagePlayer(int response, UUID tracker) {
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
        run(data.contains(0x0001)?data.getUUIDList(0x0001):null, null, data, i -> {
            client.sendPacket(new PacketMessagePlayer(i, tracker));
        });
    }

    @SuppressWarnings("deprecation")
    public static void run(List<UUID> ids, ContainedPair<String[], BaseComponent[]> message, ObjectMap<Integer> data, Callback<Integer> callback) {
        try {
            Container<Integer> failures = new Container<>(0);
            HashMap<Proxy, List<UUID>> requests = new HashMap<Proxy, List<UUID>>();
            if (ids == null || ids.size() == 0) {
                if (ProxyServer.getInstance().getPlayers().size() > 0) {
                    if (message == null) message = parseMessage(data);

                    if (message.key != null) for (String s : message.key)
                        ProxyServer.getInstance().broadcast(s);
                    if (message.value != null)
                        ProxyServer.getInstance().broadcast(message.value);
                }
                for (Proxy proxy : SubAPI.getInstance().getProxies().values()) {
                    if (proxy.getPlayers().size() > 0) requests.put(proxy, null);
                }
            } else {
                for (UUID id : ids) {
                    ProxiedPlayer local;
                    RemotePlayer remote;
                    if ((local = ProxyServer.getInstance().getPlayer(id)) != null) {
                        if (message == null) message = parseMessage(data);
                        if (message.key != null)
                            local.sendMessages(message.key);
                        if (message.value != null)
                            local.sendMessage(message.value);
                    } else if ((remote = SubAPI.getInstance().getRemotePlayer(id)) != null && remote.getProxy().getSubData()[0] != null) {
                        Proxy proxy = remote.getProxy();
                        List<UUID> list = requests.getOrDefault(proxy, new ArrayList<>());
                        list.add(id);
                        requests.put(proxy, list);
                    } else {
                        ++failures.value;
                    }
                }
            }

            if (requests.size() == 0) {
                callback.run(failures.value);
            } else {
                AsyncConsolidator merge = new AsyncConsolidator(() -> {
                    callback.run(failures.value);
                });
                List<String> legacy, raw;
                if (data == null) {
                    legacy = (message.key != null?Arrays.asList(message.key):null);
                    raw = (message.value != null?Collections.singletonList(ComponentSerializer.toString(message.value)):null);
                } else {
                    legacy = (data.contains(0x0002)?data.getRawStringList(0x0002):null);
                    raw =    (data.contains(0x0003)?data.getRawStringList(0x0003):null);
                }
                for (Map.Entry<Proxy, List<UUID>> entry : requests.entrySet()) {
                    merge.reserve();
                    ((SubDataClient) entry.getKey().getSubData()[0]).sendPacket(new PacketExMessagePlayer(entry.getValue(), legacy, raw, r -> {
                        failures.value += r.getInt(0x0001);
                        merge.release();
                    }));
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
            callback.run((ids == null || ids.size() == 0)? 1 : ids.size());
        }
    }

    private static ContainedPair<String[], BaseComponent[]> parseMessage(ObjectMap<Integer> data) {
        ContainedPair<String[], BaseComponent[]> value = new ContainedPair<>();
        if (data.contains(0x0002))
            value.key = data.getRawStringList(0x0002).toArray(new String[0]);
        if (data.contains(0x0003)) {
            List<String> messages = data.getRawStringList(0x0003);
            LinkedList<BaseComponent> components = new LinkedList<BaseComponent>();
            for (String message : messages) components.addAll(Arrays.asList(ComponentSerializer.parse(message)));
            value.value = components.toArray(new BaseComponent[0]);
        }
        return value;
    }

    @Override
    public int version() {
        return 0x0001;
    }
}

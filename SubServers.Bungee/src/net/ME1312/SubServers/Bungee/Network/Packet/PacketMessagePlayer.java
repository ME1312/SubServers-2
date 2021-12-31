package net.ME1312.SubServers.Bungee.Network.Packet;

import net.ME1312.Galaxi.Library.AsyncConsolidator;
import net.ME1312.Galaxi.Library.Container.ContainedPair;
import net.ME1312.Galaxi.Library.Container.Container;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.SubData.Server.Protocol.PacketObjectIn;
import net.ME1312.SubData.Server.Protocol.PacketObjectOut;
import net.ME1312.SubData.Server.SubDataClient;
import net.ME1312.SubServers.Bungee.Host.Proxy;
import net.ME1312.SubServers.Bungee.Host.RemotePlayer;
import net.ME1312.SubServers.Bungee.SubAPI;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.chat.ComponentSerializer;

import java.util.*;
import java.util.function.IntConsumer;

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
    public static void run(List<UUID> ids, ContainedPair<String[], BaseComponent[][]> message, ObjectMap<Integer> data, IntConsumer callback) {
        try {
            Container<Integer> failures = new Container<>(0);
            HashMap<Proxy, List<UUID>> requests = new HashMap<Proxy, List<UUID>>();
            if (ids == null || ids.size() == 0) {
                if (ProxyServer.getInstance().getPlayers().size() > 0) {
                    if (message == null) message = parseMessage(data);

                    if (message.key != null) for (String s : message.key)
                        ProxyServer.getInstance().broadcast(s);
                    if (message.value != null) for (BaseComponent[] c : message.value)
                        ProxyServer.getInstance().broadcast(c);
                }
                for (Proxy proxy : SubAPI.getInstance().getProxies().values()) {
                    if (proxy.getPlayers().size() > 0 && proxy.getSubData()[0] != null) requests.put(proxy, null);
                }
            } else {
                for (UUID id : ids) {
                    ProxiedPlayer local;
                    RemotePlayer remote;
                    if ((local = ProxyServer.getInstance().getPlayer(id)) != null) {
                        if (message == null) message = parseMessage(data);
                        if (message.key != null)
                            local.sendMessages(message.key);
                        if (message.value != null) for (BaseComponent[] c : message.value)
                            local.sendMessage(c);
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
                callback.accept(failures.value);
            } else {
                AsyncConsolidator merge = new AsyncConsolidator(() -> {
                    callback.accept(failures.value);
                });
                List<String> legacy, raw;
                if (data == null) {
                    legacy = (message.key != null?Arrays.asList(message.key):null);
                    if (message.value != null) {
                        raw = new LinkedList<String>();
                        for (BaseComponent[] c : message.value) raw.add(ComponentSerializer.toString(c));
                    } else {
                        raw = null;
                    }
                } else {
                    legacy = (data.contains(0x0002)?data.getStringList(0x0002):null);
                    raw =    (data.contains(0x0003)?data.getStringList(0x0003):null);
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
            callback.accept((ids == null || ids.size() == 0)? 1 : ids.size());
        }
    }

    private static ContainedPair<String[], BaseComponent[][]> parseMessage(ObjectMap<Integer> data) {
        ContainedPair<String[], BaseComponent[][]> value = new ContainedPair<>();
        if (data.contains(0x0002))
            value.key = data.getStringList(0x0002).toArray(new String[0]);
        if (data.contains(0x0003)) {
            List<String> messages = data.getStringList(0x0003);
            BaseComponent[][] components = new BaseComponent[messages.size()][];
            for (int i = 0; i < components.length; ++i) components[i] = ComponentSerializer.parse(messages.get(i));
            value.value = components;
        }
        return value;
    }
}

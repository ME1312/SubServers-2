package net.ME1312.SubServers.Bungee.Network.Packet;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Server.Protocol.PacketObjectIn;
import net.ME1312.SubData.Server.Protocol.PacketObjectOut;
import net.ME1312.SubData.Server.SubDataClient;
import net.ME1312.SubServers.Bungee.Host.RemotePlayer;
import net.ME1312.SubServers.Bungee.SubProxy;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.chat.ComponentSerializer;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

/**
 * Message Player Packet
 */
public class PacketMessagePlayer implements PacketObjectIn<Integer>, PacketObjectOut<Integer> {
    private SubProxy plugin;
    private int response;
    private UUID tracker;

    /**
     * New PacketMessagePlayer (In)
     *
     * @param plugin SubPlugin
     */
    public PacketMessagePlayer(SubProxy plugin) {
        if (Util.isNull(plugin)) throw new NullPointerException();
        this.plugin = plugin;
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

    @SuppressWarnings("deprecation")
    @Override
    public void receive(SubDataClient client, ObjectMap<Integer> data) {
        UUID tracker = (data.contains(0x0000)?data.getUUID(0x0000):null);
        try {
            UUID id =    data.getUUID(0x0001);

            ProxiedPlayer local;
            RemotePlayer remote;
            if ((local = plugin.getPlayer(id)) != null) {
                if (data.contains(0x0002))
                    local.sendMessages(data.getRawStringList(0x0002).toArray(new String[0]));
                if (data.contains(0x0003)) {
                    List<String> messages = data.getRawStringList(0x0003);
                    LinkedList<BaseComponent> components = new LinkedList<BaseComponent>();
                    for (String message : messages) components.addAll(Arrays.asList(ComponentSerializer.parse(message)));
                    local.sendMessage(components.toArray(new BaseComponent[0]));
                }
                client.sendPacket(new PacketMessagePlayer(0, tracker));
            } else if ((remote = plugin.api.getRemotePlayer(id)) != null) {
                if (remote.getProxy().getSubData()[0] != null) {
                    ((SubDataClient) remote.getProxy().getSubData()[0]).sendPacket(new PacketExMessagePlayer(remote.getUniqueId(), (data.contains(0x0002)?data.getRawStringList(0x0002).toArray(new String[0]):null), (data.contains(0x0003)?data.getRawStringList(0x0003).toArray(new String[0]):null), r -> {
                        client.sendPacket(new PacketMessagePlayer(r.getInt(0x0001), tracker));
                    }));
                } else {
                    client.sendPacket(new PacketMessagePlayer(4, tracker));
                }
            } else {
                client.sendPacket(new PacketMessagePlayer(3, tracker));
            }
        } catch (Throwable e) {
            client.sendPacket(new PacketMessagePlayer(2, tracker));
            e.printStackTrace();
        }
    }

    @Override
    public int version() {
        return 0x0001;
    }
}

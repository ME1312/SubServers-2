package net.ME1312.SubServers.Sync.Network.Packet;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.SubData.Client.Protocol.PacketObjectIn;
import net.ME1312.SubData.Client.Protocol.PacketObjectOut;
import net.ME1312.SubData.Client.SubDataSender;
import net.ME1312.SubServers.Sync.ExProxy;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.chat.ComponentSerializer;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

/**
 * Message External Player Packet
 */
public class PacketExMessagePlayer implements PacketObjectIn<Integer>, PacketObjectOut<Integer> {
    private ExProxy plugin;
    private int response;
    private UUID tracker;

    /**
     * New PacketExMessagePlayer (In)
     */
    public PacketExMessagePlayer(ExProxy plugin) {
        this.plugin = plugin;
    }

    /**
     * New PacketExMessagePlayer (Out)
     *
     * @param response Response ID
     * @param tracker Receiver ID
     */
    public PacketExMessagePlayer(int response, UUID tracker) {
        this.response = response;
        this.tracker = tracker;
    }

    @Override
    public ObjectMap<Integer> send(SubDataSender client) {
        ObjectMap<Integer> json = new ObjectMap<Integer>();
        if (tracker != null) json.set(0x0000, tracker);
        json.set(0x0001, response);
        return json;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void receive(SubDataSender client, ObjectMap<Integer> data) {
        UUID tracker = (data.contains(0x0000)?data.getUUID(0x0000):null);
        try {
            UUID id =    data.getUUID(0x0001);

            ProxiedPlayer local;
            if ((local = plugin.getPlayer(id)) != null) {
                if (data.contains(0x0002))
                    local.sendMessages(data.getRawStringList(0x0002).toArray(new String[0]));
                if (data.contains(0x0003)) {
                    List<String> messages = data.getRawStringList(0x0003);
                    LinkedList<BaseComponent> components = new LinkedList<BaseComponent>();
                    for (String message : messages) components.addAll(Arrays.asList(ComponentSerializer.parse(message)));
                    local.sendMessage(components.toArray(new BaseComponent[0]));
                }
                client.sendPacket(new PacketExMessagePlayer(0, tracker));
            } else {
                client.sendPacket(new PacketExMessagePlayer(3, tracker));
            }
        } catch (Throwable e) {
            client.sendPacket(new PacketExMessagePlayer(2, tracker));
            e.printStackTrace();
        }
    }

    @Override
    public int version() {
        return 0x0001;
    }
}

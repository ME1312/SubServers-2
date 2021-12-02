package net.ME1312.SubServers.Client.Common.Network.Packet;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Client.Protocol.PacketObjectIn;
import net.ME1312.SubData.Client.Protocol.PacketObjectOut;
import net.ME1312.SubData.Client.SubDataSender;

import java.util.HashMap;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Command Server Packet
 */
public class PacketCommandServer implements PacketObjectIn<Integer>, PacketObjectOut<Integer> {
    private static HashMap<UUID, Consumer<ObjectMap<Integer>>[]> callbacks = new HashMap<UUID, Consumer<ObjectMap<Integer>>[]>();
    private UUID player;
    private String server;
    private String command;
    private UUID target;
    private UUID tracker;

    /**
     * New PacketCommandServer (In)
     */
    public PacketCommandServer() {}

    /**
     * New PacketCommandServer (Out)
     *
     * @param player Player Sending
     * @param target Target Player
     * @param server Server to send to
     * @param command Command to send
     * @param callback Callbacks
     */
    @SafeVarargs
    public PacketCommandServer(UUID player, UUID target, String server, String command, Consumer<ObjectMap<Integer>>... callback) {
        Util.nullpo(server, command, callback);
        this.player = player;
        this.target = target;
        this.server = server;
        this.command = command;
        this.tracker = Util.getNew(callbacks.keySet(), UUID::randomUUID);
        callbacks.put(tracker, callback);
    }

    @Override
    public ObjectMap<Integer> send(SubDataSender client) {
        ObjectMap<Integer> data = new ObjectMap<Integer>();
        data.set(0x0000, tracker);
        data.set(0x0001, server);
        data.set(0x0002, command);
        if (player != null) data.set(0x0003, player);
        if (target != null) data.set(0x0004, target);
        return data;
    }

    @Override
    public void receive(SubDataSender client, ObjectMap<Integer> data) {
        for (Consumer<ObjectMap<Integer>> callback : callbacks.get(data.getUUID(0x0000))) callback.accept(data);
        callbacks.remove(data.getUUID(0x0000));
    }
}

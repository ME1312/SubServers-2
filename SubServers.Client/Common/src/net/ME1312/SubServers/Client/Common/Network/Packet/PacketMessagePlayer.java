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
 * Message Player Packet
 */
public class PacketMessagePlayer implements PacketObjectIn<Integer>, PacketObjectOut<Integer> {
    private static HashMap<UUID, Consumer<ObjectMap<Integer>>[]> callbacks = new HashMap<UUID, Consumer<ObjectMap<Integer>>[]>();
    private UUID[] players;
    private String[] legacy, raw;
    private UUID id;

    /**
     * New PacketMessagePlayer (In)
     */
    public PacketMessagePlayer() {}

    /**
     * New PacketMessagePlayer (Out)
     *
     * @param players Players
     * @param legacy Messages (Legacy)
     * @param raw Messages (JSON)
     * @param callback Callbacks
     */
    @SafeVarargs
    public PacketMessagePlayer(UUID[] players, String[] legacy, String[] raw, Consumer<ObjectMap<Integer>>... callback) {
        this.players = players;
        this.legacy = legacy;
        this.raw = raw;
        this.id = Util.getNew(callbacks.keySet(), UUID::randomUUID);
        callbacks.put(id, callback);
    }

    @Override
    public ObjectMap<Integer> send(SubDataSender client) {
        ObjectMap<Integer> data = new ObjectMap<Integer>();
        data.set(0x0000, id);
        if (players != null) data.set(0x0001, players);
        if (legacy != null) data.set(0x0002, legacy);
        if (raw != null) data.set(0x0003, raw);
        return data;
    }

    @Override
    public void receive(SubDataSender client, ObjectMap<Integer> data) {
        for (Consumer<ObjectMap<Integer>> callback : callbacks.get(data.getUUID(0x0000))) callback.accept(data);
        callbacks.remove(data.getUUID(0x0000));
    }
}

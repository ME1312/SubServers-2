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
 * Disconnect Player Packet
 */
public class PacketDisconnectPlayer implements PacketObjectIn<Integer>, PacketObjectOut<Integer> {
    private static HashMap<UUID, Consumer<ObjectMap<Integer>>[]> callbacks = new HashMap<UUID, Consumer<ObjectMap<Integer>>[]>();
    private UUID[] players;
    private String reason;
    private UUID id;

    /**
     * New PacketDisconnectPlayer (In)
     */
    public PacketDisconnectPlayer() {}

    /**
     * New PacketDisconnectPlayer (Out)
     *
     * @param players Players
     * @param reason Reason
     * @param callback Callbacks
     */
    @SafeVarargs
    public PacketDisconnectPlayer(UUID[] players, String reason, Consumer<ObjectMap<Integer>>... callback) {
        Util.nullpo(players, callback);
        this.players = players;
        this.reason = reason;
        this.id = Util.getNew(callbacks.keySet(), UUID::randomUUID);
        callbacks.put(id, callback);
    }

    @Override
    public ObjectMap<Integer> send(SubDataSender client) {
        ObjectMap<Integer> data = new ObjectMap<Integer>();
        data.set(0x0000, id);
        data.set(0x0001, players);
        if (reason != null) data.set(0x0002, reason);
        return data;
    }

    @Override
    public void receive(SubDataSender client, ObjectMap<Integer> data) {
        for (Consumer<ObjectMap<Integer>> callback : callbacks.get(data.getUUID(0x0000))) callback.accept(data);
        callbacks.remove(data.getUUID(0x0000));
    }
}

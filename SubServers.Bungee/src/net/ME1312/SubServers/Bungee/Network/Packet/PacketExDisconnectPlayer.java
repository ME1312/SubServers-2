package net.ME1312.SubServers.Bungee.Network.Packet;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Server.Protocol.PacketObjectIn;
import net.ME1312.SubData.Server.Protocol.PacketObjectOut;
import net.ME1312.SubData.Server.SubDataClient;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Disconnect External Player Packet
 */
public class PacketExDisconnectPlayer implements PacketObjectIn<Integer>, PacketObjectOut<Integer> {
    private static HashMap<UUID, Consumer<ObjectMap<Integer>>[]> callbacks = new HashMap<UUID, Consumer<ObjectMap<Integer>>[]>();
    private List<UUID> players;
    private String reason;
    private UUID id;

    /**
     * New PacketExDisconnectPlayer (In)
     */
    public PacketExDisconnectPlayer() {}

    /**
     * New PacketExDisconnectPlayer (Out)
     *
     * @param players Players
     * @param reason Reason
     * @param callback Callbacks
     */
    @SafeVarargs
    public PacketExDisconnectPlayer(List<UUID> players, String reason, Consumer<ObjectMap<Integer>>... callback) {
        Util.nullpo(players, callback);
        this.players = players;
        this.reason = reason;
        this.id = Util.getNew(callbacks.keySet(), UUID::randomUUID);
        callbacks.put(id, callback);
    }

    @Override
    public ObjectMap<Integer> send(SubDataClient client) {
        ObjectMap<Integer> data = new ObjectMap<Integer>();
        data.set(0x0000, id);
        data.set(0x0001, players);
        if (reason != null) data.set(0x0002, reason);
        return data;
    }

    @Override
    public void receive(SubDataClient client, ObjectMap<Integer> data) {
        for (Consumer<ObjectMap<Integer>> callback : callbacks.get(data.getUUID(0x0000))) callback.accept(data);
        callbacks.remove(data.getUUID(0x0000));
    }
}

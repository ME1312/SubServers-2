package net.ME1312.SubServers.Bungee.Network.Packet;

import net.ME1312.Galaxi.Library.Callback.Callback;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Server.Protocol.PacketObjectIn;
import net.ME1312.SubData.Server.Protocol.PacketObjectOut;
import net.ME1312.SubData.Server.SubDataClient;

import java.util.HashMap;
import java.util.UUID;

/**
 * Message External Player Packet
 */
public class PacketExMessagePlayer implements PacketObjectIn<Integer>, PacketObjectOut<Integer> {
    private static HashMap<UUID, Callback<ObjectMap<Integer>>[]> callbacks = new HashMap<UUID, Callback<ObjectMap<Integer>>[]>();
    private UUID player;
    private String[] legacy, raw;
    private UUID id;

    /**
     * New PacketExMessagePlayer (In)
     */
    public PacketExMessagePlayer() {}

    /**
     * New PacketExMessagePlayer (Out)
     *
     * @param player Player
     * @param legacy Messages (Legacy)
     * @param raw Messages (JSON)
     * @param callback Callbacks
     */
    @SafeVarargs
    public PacketExMessagePlayer(UUID player, String[] legacy, String[] raw, Callback<ObjectMap<Integer>>... callback) {
        if (Util.isNull(player, callback)) throw new NullPointerException();
        this.player = player;
        this.legacy = legacy;
        this.raw = raw;
        this.id = Util.getNew(callbacks.keySet(), UUID::randomUUID);
        callbacks.put(id, callback);
    }

    @Override
    public ObjectMap<Integer> send(SubDataClient client) {
        ObjectMap<Integer> data = new ObjectMap<Integer>();
        data.set(0x0000, id);
        data.set(0x0001, player);
        if (legacy != null) data.set(0x0002, legacy);
        if (raw != null) data.set(0x0003, raw);
        return data;
    }

    @Override
    public void receive(SubDataClient client, ObjectMap<Integer> data) {
        for (Callback<ObjectMap<Integer>> callback : callbacks.get(data.getUUID(0x0000))) callback.run(data);
        callbacks.remove(data.getUUID(0x0000));
    }

    @Override
    public int version() {
        return 0x0001;
    }
}

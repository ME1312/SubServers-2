package net.ME1312.SubServers.Bungee.Network.Packet;

import net.ME1312.SubData.Server.SubDataClient;
import net.ME1312.Galaxi.Library.Callback.Callback;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Server.Protocol.PacketObjectIn;
import net.ME1312.SubData.Server.Protocol.PacketObjectOut;

import java.util.HashMap;
import java.util.UUID;

/**
 * Create Server External Host Packet
 */
public class PacketExRemoveServer implements PacketObjectIn<Integer>, PacketObjectOut<Integer> {
    private static HashMap<UUID, Callback<ObjectMap<Integer>>[]> callbacks = new HashMap<UUID, Callback<ObjectMap<Integer>>[]>();
    private String name;
    private UUID tracker;

    /**
     * New PacketExRemoveServer (In)
     */
    public PacketExRemoveServer() {}

    /**
     * New PacketExRemoveServer (Out)
     *
     * @param name Server Name
     * @param callback Callbacks
     */
    @SafeVarargs
    public PacketExRemoveServer(String name, Callback<ObjectMap<Integer>>... callback) {
        if (Util.isNull(name, callback)) throw new NullPointerException();
        this.name = name;
        this.tracker = Util.getNew(callbacks.keySet(), UUID::randomUUID);
        callbacks.put(tracker, callback);
    }

    @Override
    public ObjectMap<Integer> send(SubDataClient client) {
        ObjectMap<Integer> data = new ObjectMap<Integer>();
        if (tracker != null) data.set(0x0000, tracker);
        data.set(0x0001, name);
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
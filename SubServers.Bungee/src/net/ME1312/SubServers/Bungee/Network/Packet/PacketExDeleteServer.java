package net.ME1312.SubServers.Bungee.Network.Packet;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Server.Protocol.PacketObjectIn;
import net.ME1312.SubData.Server.Protocol.PacketObjectOut;
import net.ME1312.SubData.Server.SubDataClient;

import java.util.HashMap;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Delete Server External Host Packet
 */
public class PacketExDeleteServer implements PacketObjectIn<Integer>, PacketObjectOut<Integer> {
    private static HashMap<UUID, Consumer<ObjectMap<Integer>>[]> callbacks = new HashMap<UUID, Consumer<ObjectMap<Integer>>[]>();
    private String name;
    private ObjectMap<String> info;
    private boolean recycle;
    private UUID tracker = null;

    /**
     * New PacketExDeleteServer
     */
    public PacketExDeleteServer() {}

    /**
     * New PacketExDeleteServer (Out)
     *
     * @param name Server Name
     * @param info Info.json Contents
     * @param callback Callbacks
     */
    @SafeVarargs
    public PacketExDeleteServer(String name, ObjectMap<String> info, boolean recycle, Consumer<ObjectMap<Integer>>... callback) {
        Util.nullpo(name, info, callback);
        this.name = name;
        this.info = info;
        this.recycle = recycle;
        this.tracker = Util.getNew(callbacks.keySet(), UUID::randomUUID);
        callbacks.put(tracker, callback);
    }

    @Override
    public ObjectMap<Integer> send(SubDataClient client) {
        ObjectMap<Integer> data = new ObjectMap<Integer>();
        data.set(0x0000, tracker);
        data.set(0x0001, name);
        data.set(0x0002, info);
        if (recycle) data.set(0x0003, true);
        return data;
    }

    @Override
    public void receive(SubDataClient client, ObjectMap<Integer> data) {
        for (Consumer<ObjectMap<Integer>> callback : callbacks.get(data.getUUID(0x0000))) callback.accept(data);
        callbacks.remove(data.getUUID(0x0000));
    }

    @Override
    public int version() {
        return 0x0001;
    }
}
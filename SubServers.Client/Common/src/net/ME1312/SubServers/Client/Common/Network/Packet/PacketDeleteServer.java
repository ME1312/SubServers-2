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
 * Delete Server Packet
 */
public class PacketDeleteServer implements PacketObjectIn<Integer>, PacketObjectOut<Integer> {
    private static HashMap<UUID, Consumer<ObjectMap<Integer>>[]> callbacks = new HashMap<UUID, Consumer<ObjectMap<Integer>>[]>();
    private UUID player;
    private String server;
    private boolean recycle;
    private boolean force;
    private UUID tracker;

    /**
     * New PacketDeleteServer (In)
     */
    public PacketDeleteServer() {}

    /**
     * New PacketDeleteServer (Out)
     *
     * @param player Player Deleting
     * @param server Server
     * @param callback Callbacks
     */
    @SafeVarargs
    public PacketDeleteServer(UUID player, String server, boolean recycle, boolean force, Consumer<ObjectMap<Integer>>... callback) {
        Util.nullpo(server, callback);
        this.player = player;
        this.server = server;
        this.recycle = recycle;
        this.force = force;
        this.tracker = Util.getNew(callbacks.keySet(), UUID::randomUUID);
        callbacks.put(tracker, callback);
    }

    @Override
    public ObjectMap<Integer> send(SubDataSender client) {
        ObjectMap<Integer> data = new ObjectMap<Integer>();
        data.set(0x0000, tracker);
        data.set(0x0001, server);
        data.set(0x0002, recycle);
        data.set(0x0003, force);
        if (player != null) data.set(0x0004, player);
        return data;
    }

    @Override
    public void receive(SubDataSender client, ObjectMap<Integer> data) {
        for (Consumer<ObjectMap<Integer>> callback : callbacks.remove(data.getUUID(0x0000))) callback.accept(data);
    }
}

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
 * Stop Server Packet
 */
public class PacketStopServer implements PacketObjectIn<Integer>, PacketObjectOut<Integer> {
    private static HashMap<UUID, Consumer<ObjectMap<Integer>>[]> callbacks = new HashMap<UUID, Consumer<ObjectMap<Integer>>[]>();
    private UUID player;
    private boolean force;
    private String server;
    private UUID id;

    /**
     * New PacketStopServer (In)
     */
    public PacketStopServer() {}

    /**
     * New PacketStopServer (Out)
     *
     * @param player Player Starting
     * @param server Server
     * @param force Force Stop
     * @param callback Callbacks
     */
    @SafeVarargs
    public PacketStopServer(UUID player, String server, boolean force, Consumer<ObjectMap<Integer>>... callback) {
        Util.nullpo(server, force, callback);
        this.player = player;
        this.server = server;
        this.force = force;
        this.id = Util.getNew(callbacks.keySet(), UUID::randomUUID);
        callbacks.put(id, callback);
    }

    @Override
    public ObjectMap<Integer> send(SubDataSender client) {
        ObjectMap<Integer> data = new ObjectMap<Integer>();
        data.set(0x0000, id);
        data.set(0x0001, server);
        data.set(0x0002, force);
        if (player != null) data.set(0x0003, player);
        return data;
    }

    @Override
    public void receive(SubDataSender client, ObjectMap<Integer> data) {
        for (Consumer<ObjectMap<Integer>> callback : callbacks.get(data.getUUID(0x0000))) callback.accept(data);
        callbacks.remove(data.getUUID(0x0000));
    }
}

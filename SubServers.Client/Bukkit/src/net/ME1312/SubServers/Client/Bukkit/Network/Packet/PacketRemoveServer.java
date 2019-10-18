package net.ME1312.SubServers.Client.Bukkit.Network.Packet;

import net.ME1312.Galaxi.Library.Callback.Callback;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Client.Protocol.PacketObjectIn;
import net.ME1312.SubData.Client.Protocol.PacketObjectOut;
import net.ME1312.SubData.Client.SubDataSender;

import java.util.HashMap;
import java.util.UUID;

/**
 * Remove Server Packet
 */
public class PacketRemoveServer implements PacketObjectIn<Integer>, PacketObjectOut<Integer> {
    private static HashMap<UUID, Callback<ObjectMap<Integer>>[]> callbacks = new HashMap<UUID, Callback<ObjectMap<Integer>>[]>();
    private UUID player;
    private String server;
    private boolean force;
    private UUID tracker;

    /**
     * New PacketRemoveServer (In)
     */
    public PacketRemoveServer() {}

    /**
     * New PacketRemoveServer (Out)
     *
     * @param player Player Removing
     * @param server Server
     * @param callback Callbacks
     */
    @SafeVarargs
    public PacketRemoveServer(UUID player, String server, boolean force, Callback<ObjectMap<Integer>>... callback) {
        if (Util.isNull(server, callback)) throw new NullPointerException();
        this.player = player;
        this.server = server;
        this.force = force;
        this.tracker = Util.getNew(callbacks.keySet(), UUID::randomUUID);
        callbacks.put(tracker, callback);
    }

    @Override
    public ObjectMap<Integer> send(SubDataSender client) {
        ObjectMap<Integer> data = new ObjectMap<Integer>();
        data.set(0x0000, tracker);
        data.set(0x0001, server);
        data.set(0x0002, force);
        if (player != null) data.set(0x0003, player.toString());
        return data;
    }

    @Override
    public void receive(SubDataSender client, ObjectMap<Integer> data) {
        for (Callback<ObjectMap<Integer>> callback : callbacks.get(data.getUUID(0x0000))) callback.run(data);
        callbacks.remove(data.getUUID(0x0000));
    }

    @Override
    public int version() {
        return 0x0001;
    }
}

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
 * External Player Control Packet
 */
public class PacketExControlPlayer implements PacketObjectIn<Integer>, PacketObjectOut<Integer> {
    private static HashMap<UUID, Consumer<ObjectMap<Integer>>[]> callbacks = new HashMap<UUID, Consumer<ObjectMap<Integer>>[]>();
    private String command;
    private UUID target;
    private UUID tracker;

    /**
     * New PacketExControlPlayer (In)
     */
    public PacketExControlPlayer() {}

    /**
     * New PacketExControlPlayer (Out)
     *
     * @param target Target Player
     * @param command Command to send
     * @param callback Callbacks
     */
    @SafeVarargs
    public PacketExControlPlayer(UUID target, String command, Consumer<ObjectMap<Integer>>... callback) {
        Util.nullpo(command, callback);
        this.target = target;
        this.command = command;
        this.tracker = Util.getNew(callbacks.keySet(), UUID::randomUUID);
        callbacks.put(tracker, callback);
    }

    @Override
    public ObjectMap<Integer> send(SubDataClient client) {
        ObjectMap<Integer> data = new ObjectMap<Integer>();
        data.set(0x0000, tracker);
        data.set(0x0001, command);
        if (target != null) data.set(0x0002, target);
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

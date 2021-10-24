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
 * Transfer Player Packet
 */
public class PacketTransferPlayer implements PacketObjectIn<Integer>, PacketObjectOut<Integer> {
    private static HashMap<UUID, Consumer<ObjectMap<Integer>>[]> callbacks = new HashMap<UUID, Consumer<ObjectMap<Integer>>[]>();
    private UUID[] players;
    private String server;
    private UUID id;

    /**
     * New PacketTransferPlayer (In)
     */
    public PacketTransferPlayer() {}

    /**
     * New PacketTransferPlayer (Out)
     *
     * @param players Players
     * @param server Server
     * @param callback Callbacks
     */
    @SafeVarargs
    public PacketTransferPlayer(UUID[] players, String server, Consumer<ObjectMap<Integer>>... callback) {
        Util.nullpo(players, server);
        this.players = players;
        this.server = server;
        this.id = Util.getNew(callbacks.keySet(), UUID::randomUUID);
        callbacks.put(id, callback);
    }

    @Override
    public ObjectMap<Integer> send(SubDataSender client) {
        ObjectMap<Integer> data = new ObjectMap<Integer>();
        data.set(0x0000, id);
        data.set(0x0001, players);
        if (server != null) data.set(0x0002, server);
        return data;
    }

    @Override
    public void receive(SubDataSender client, ObjectMap<Integer> data) {
        for (Consumer<ObjectMap<Integer>> callback : callbacks.get(data.getUUID(0x0000))) callback.accept(data);
        callbacks.remove(data.getUUID(0x0000));
    }

    @Override
    public int version() {
        return 0x0001;
    }
}

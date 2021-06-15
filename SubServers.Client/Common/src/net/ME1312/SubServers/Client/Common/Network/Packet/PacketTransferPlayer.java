package net.ME1312.SubServers.Client.Common.Network.Packet;

import net.ME1312.Galaxi.Library.Callback.Callback;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Client.Protocol.PacketObjectIn;
import net.ME1312.SubData.Client.Protocol.PacketObjectOut;
import net.ME1312.SubData.Client.SubDataSender;

import java.util.HashMap;
import java.util.UUID;

/**
 * Transfer Player Packet
 */
public class PacketTransferPlayer implements PacketObjectIn<Integer>, PacketObjectOut<Integer> {
    private static HashMap<UUID, Callback<ObjectMap<Integer>>[]> callbacks = new HashMap<UUID, Callback<ObjectMap<Integer>>[]>();
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
    public PacketTransferPlayer(UUID[] players, String server, Callback<ObjectMap<Integer>>... callback) {
        if (Util.isNull(players, server)) throw new NullPointerException();
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
        for (Callback<ObjectMap<Integer>> callback : callbacks.get(data.getUUID(0x0000))) callback.run(data);
        callbacks.remove(data.getUUID(0x0000));
    }

    @Override
    public int version() {
        return 0x0001;
    }
}

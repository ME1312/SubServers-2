package net.ME1312.SubServers.Bungee.Network.Packet;

import net.ME1312.Galaxi.Library.Callback.Callback;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Server.Protocol.PacketObjectIn;
import net.ME1312.SubData.Server.Protocol.PacketObjectOut;
import net.ME1312.SubData.Server.SubDataClient;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * Transfer External Player Packet
 */
public class PacketExTransferPlayer implements PacketObjectIn<Integer>, PacketObjectOut<Integer> {
    private static HashMap<UUID, Callback<ObjectMap<Integer>>[]> callbacks = new HashMap<UUID, Callback<ObjectMap<Integer>>[]>();
    private List<UUID> players;
    private String server;
    private UUID id;

    /**
     * New PacketExTransferPlayer (In)
     */
    public PacketExTransferPlayer() {}

    /**
     * New PacketExTransferPlayer (Out)
     *
     * @param players Players
     * @param server Server
     * @param callback Callbacks
     */
    @SafeVarargs
    public PacketExTransferPlayer(List<UUID> players, String server, Callback<ObjectMap<Integer>>... callback) {
        if (Util.isNull(players, server, callback)) throw new NullPointerException();
        this.players = players;
        this.server = server;
        this.id = Util.getNew(callbacks.keySet(), UUID::randomUUID);
        callbacks.put(id, callback);
    }

    @Override
    public ObjectMap<Integer> send(SubDataClient client) {
        ObjectMap<Integer> data = new ObjectMap<Integer>();
        data.set(0x0000, id);
        data.set(0x0001, players);
        data.set(0x0002, server);
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

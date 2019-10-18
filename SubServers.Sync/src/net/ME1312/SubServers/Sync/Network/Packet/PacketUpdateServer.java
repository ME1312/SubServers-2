package net.ME1312.SubServers.Sync.Network.Packet;

import net.ME1312.Galaxi.Library.Callback.Callback;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.SubData.Client.Protocol.PacketObjectIn;
import net.ME1312.SubData.Client.Protocol.PacketObjectOut;
import net.ME1312.SubData.Client.SubDataSender;

import java.util.HashMap;
import java.util.UUID;

/**
 * Create Server Packet
 */
public class PacketUpdateServer implements PacketObjectIn<Integer>, PacketObjectOut<Integer> {
    private static HashMap<UUID, Callback<ObjectMap<Integer>>[]> callbacks = new HashMap<UUID, Callback<ObjectMap<Integer>>[]>();
    private UUID player;
    private String name;
    private Version version;
    private boolean waitfor;
    private UUID tracker;

    /**
     * New PacketCreateServer (In)
     */
    public PacketUpdateServer() {}

    /**
     * New PacketCreateServer (Out)
     *
     * @param player Player Creating
     * @param name Server Name
     * @param version Server Version
     * @param callback Callbacks
     */
    @SafeVarargs
    public PacketUpdateServer(UUID player, String name, Version version, Callback<ObjectMap<Integer>>... callback) {
        this(player, name, version, false, callback);
    }

    /**
     * New PacketCreateServer (Out)
     *
     * @param player Player Creating
     * @param name Server Name
     * @param version Server Version
     * @param waitfor Wait until completion to send callback
     * @param callback Callbacks
     */
    @SafeVarargs
    public PacketUpdateServer(UUID player, String name, Version version, boolean waitfor, Callback<ObjectMap<Integer>>... callback) {
        if (Util.isNull(name, callback)) throw new NullPointerException();
        this.player = player;
        this.name = name;
        this.version = version;
        this.waitfor = waitfor;
        this.tracker = Util.getNew(callbacks.keySet(), UUID::randomUUID);
        callbacks.put(tracker, callback);
    }

    @Override
    public ObjectMap<Integer> send(SubDataSender client) {
        ObjectMap<Integer> data = new ObjectMap<Integer>();
        data.set(0x0000, tracker);
        data.set(0x0001, name);
        if (version != null) data.set(0x0002, version);
        if (player != null) data.set(0x0003, player);
        if (waitfor) data.set(0x0004, true);
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

package net.ME1312.SubServers.Velocity.Network.Packet;

import net.ME1312.Galaxi.Library.Callback.Callback;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Client.Protocol.Forwardable;
import net.ME1312.SubData.Client.Protocol.PacketObjectIn;
import net.ME1312.SubData.Client.Protocol.PacketObjectOut;
import net.ME1312.SubData.Client.SubDataSender;

import java.util.HashMap;
import java.util.UUID;

/**
 * Check Permission Packet
 */
public class PacketCheckPermission implements Forwardable, PacketObjectIn<Integer>, PacketObjectOut<Integer> {
    static HashMap<UUID, Callback<Boolean>[]> callbacks = new HashMap<UUID, Callback<Boolean>[]>();
    private UUID player;
    private String permission;
    private UUID tracker;

    /**
     * New PacketCheckPermission (In)
     */
    public PacketCheckPermission() {}

    /**
     * New PacketCheckPermission (Out)
     *
     * @param player Player to check on
     * @param permission Permission to check
     * @param callback Callbacks
     */
    @SafeVarargs
    public PacketCheckPermission(UUID player, String permission, Callback<Boolean>... callback) {
        this.player = player;
        this.permission = permission;
        this.tracker = Util.getNew(callbacks.keySet(), UUID::randomUUID);
        callbacks.put(tracker, callback);
    }

    @Override
    public ObjectMap<Integer> send(SubDataSender client) throws Throwable {
        ObjectMap<Integer> data = new ObjectMap<Integer>();
        data.set(0x0000, tracker);
        data.set(0x0001, player);
        data.set(0x0002, permission);
        return data;
    }

    @Override
    public void receive(SubDataSender client, ObjectMap<Integer> data) throws Throwable {
        client.sendPacket(new PacketCheckPermissionResponse(data.getUUID(0x0001), data.getRawString(0x0002), (data.contains(0x0000))?data.getUUID(0x0000):null));
    }

    @Override
    public int version() {
        return 0x0001;
    }
}

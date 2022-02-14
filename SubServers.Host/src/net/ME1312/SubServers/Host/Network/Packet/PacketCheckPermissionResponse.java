package net.ME1312.SubServers.Host.Network.Packet;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.SubData.Client.Protocol.Forwardable;
import net.ME1312.SubData.Client.Protocol.PacketObjectIn;
import net.ME1312.SubData.Client.Protocol.PacketObjectOut;
import net.ME1312.SubData.Client.SubDataSender;

import java.util.UUID;
import java.util.function.Consumer;

import static net.ME1312.SubServers.Host.Network.Packet.PacketCheckPermission.callbacks;


/**
 * Check Permission Response Packet
 */
public class PacketCheckPermissionResponse implements Forwardable, PacketObjectIn<Integer>, PacketObjectOut<Integer> {
    private boolean result;
    private UUID tracker;

    /**
     * New PacketCheckPermissionResponse (In)
     */
    public PacketCheckPermissionResponse() {}

    /**
     * New PacketCheckPermissionResponse (Out)
     *
     * @param player Player to check on
     * @param permission Permission to check
     * @param tracker Receiver ID
     */
    public PacketCheckPermissionResponse(UUID player, String permission, UUID tracker) {
        this.result = false; // TODO
        this.tracker = tracker;
    }

    @Override
    public ObjectMap<Integer> send(SubDataSender client) throws Throwable {
        ObjectMap<Integer> data = new ObjectMap<Integer>();
        data.set(0x0000, tracker);
        data.set(0x0001, result);
        return data;
    }

    @Override
    public void receive(SubDataSender client, ObjectMap<Integer> data) throws Throwable {
        for (Consumer<Boolean> callback : callbacks.remove(data.getUUID(0x0000))) callback.accept(data.getBoolean(0x0001));
    }
}

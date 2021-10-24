package net.ME1312.SubServers.Velocity.Network.Packet;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Try;
import net.ME1312.SubData.Client.Protocol.Forwardable;
import net.ME1312.SubData.Client.Protocol.PacketObjectIn;
import net.ME1312.SubData.Client.Protocol.PacketObjectOut;
import net.ME1312.SubData.Client.SubDataSender;
import net.ME1312.SubServers.Velocity.ExProxy;

import java.util.UUID;
import java.util.function.Consumer;

import static net.ME1312.SubServers.Velocity.Network.Packet.PacketCheckPermission.callbacks;


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
        this.result = Try.all.get(() -> ExProxy.getInstance().getPlayer(player).get().hasPermission(permission), false);
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
        for (Consumer<Boolean> callback : callbacks.get(data.getUUID(0x0000))) callback.accept(data.getBoolean(0x0001));
        callbacks.remove(data.getUUID(0x0000));
    }

    @Override
    public int version() {
        return 0x0001;
    }
}

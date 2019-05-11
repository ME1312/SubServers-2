package net.ME1312.SubServers.Client.Bukkit.Network.Packet;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Client.Protocol.PacketObjectIn;
import net.ME1312.SubData.Client.Protocol.PacketObjectOut;
import net.ME1312.SubData.Client.SubDataClient;
import org.bukkit.Bukkit;

import java.util.UUID;

/**
 * External Check Permission Packet
 */
public class PacketExCheckPermission implements PacketObjectIn<Integer>, PacketObjectOut<Integer> {
    private UUID player;
    private String permission;
    private UUID tracker;

    /**
     * New PacketExCheckPermission (In)
     */
    public PacketExCheckPermission() {}

    /**
     * New PacketExCheckPermission (Out)
     *
     * @param player Player to check on
     * @param permission Permission to check
     * @param tracker Receiver ID
     */
    public PacketExCheckPermission(UUID player, String permission, UUID tracker) {
        this.player = player;
        this.permission = permission;
        this.tracker = tracker;
    }

    @Override
    public ObjectMap<Integer> send(SubDataClient client) throws Throwable {
        ObjectMap<Integer> data = new ObjectMap<Integer>();
        data.set(0x0000, tracker);
        data.set(0x0001, Util.getDespiteException(() -> Bukkit.getServer().getPlayer(player).hasPermission(permission), false));
        return data;
    }

    @Override
    public void receive(SubDataClient client, ObjectMap<Integer> data) throws Throwable {
        client.sendPacket(new PacketExCheckPermission(data.getUUID(0x0001), data.getRawString(0x0002), (data.contains(0x0000))?data.getUUID(0x0000):null));
    }

    @Override
    public int version() {
        return 0x0001;
    }
}

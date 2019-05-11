package net.ME1312.SubServers.Bungee.Network.Packet;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Server.Protocol.PacketObjectIn;
import net.ME1312.SubData.Server.Protocol.PacketObjectOut;
import net.ME1312.SubData.Server.SubDataClient;
import net.ME1312.SubServers.Bungee.Host.Server;
import net.ME1312.SubServers.Bungee.SubAPI;
import net.md_5.bungee.api.ProxyServer;

import java.util.UUID;

/**
 * Check Permission Packet
 */
public class PacketCheckPermission implements PacketObjectIn<Integer>, PacketObjectOut<Integer> {
    private boolean result;
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
     * @param tracker Receiver ID
     */
    public PacketCheckPermission(UUID player, String permission, UUID tracker) {
        this(Util.getDespiteException(() -> ProxyServer.getInstance().getPlayer(player).hasPermission(permission), false), tracker);
    }

    PacketCheckPermission(boolean result, UUID tracker) {
        this.result = result;
        this.tracker = tracker;
    }

    @Override
    public ObjectMap<Integer> send(SubDataClient client) throws Throwable {
        ObjectMap<Integer> data = new ObjectMap<Integer>();
        data.set(0x0000, tracker);
        data.set(0x0001, result);
        return data;
    }

    @Override
    public void receive(SubDataClient client, ObjectMap<Integer> data) throws Throwable {
        Server server = data.contains(0x0003)?SubAPI.getInstance().getServer(data.getRawString(0x0003)):null;
        if (server != null && server.getSubData()[0] != null) {
            ((SubDataClient) server.getSubData()[0]).sendPacket(new PacketExCheckPermission(data.getUUID(0x0001), data.getRawString(0x0002), result -> {
                client.sendPacket(new PacketCheckPermission(result, (data.contains(0x0000))?data.getUUID(0x0000):null));
            }));
        } else {
            client.sendPacket(new PacketCheckPermission(data.getUUID(0x0001), data.getRawString(0x0002), (data.contains(0x0000))?data.getUUID(0x0000):null));
        }
    }

    @Override
    public int version() {
        return 0x0001;
    }
}

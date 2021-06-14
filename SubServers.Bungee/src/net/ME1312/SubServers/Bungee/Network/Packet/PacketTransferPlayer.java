package net.ME1312.SubServers.Bungee.Network.Packet;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Server.Protocol.PacketObjectIn;
import net.ME1312.SubData.Server.Protocol.PacketObjectOut;
import net.ME1312.SubData.Server.SubDataClient;
import net.ME1312.SubServers.Bungee.Host.RemotePlayer;
import net.ME1312.SubServers.Bungee.Host.Server;
import net.ME1312.SubServers.Bungee.SubProxy;

import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.UUID;

/**
 * Transfer Player Packet
 */
public class PacketTransferPlayer implements PacketObjectIn<Integer>, PacketObjectOut<Integer> {
    private SubProxy plugin;
    private int response;
    private UUID tracker;

    /**
     * New PacketTransferPlayer (In)
     *
     * @param plugin SubPlugin
     */
    public PacketTransferPlayer(SubProxy plugin) {
        if (Util.isNull(plugin)) throw new NullPointerException();
        this.plugin = plugin;
    }

    /**
     * New PacketTransferPlayer (Out)
     *
     * @param response Response ID
     * @param tracker Receiver ID
     */
    public PacketTransferPlayer(int response, UUID tracker) {
        this.response = response;
        this.tracker = tracker;
    }

    @Override
    public ObjectMap<Integer> send(SubDataClient client) {
        ObjectMap<Integer> json = new ObjectMap<Integer>();
        if (tracker != null) json.set(0x0000, tracker);
        json.set(0x0001, response);
        return json;
    }

    @Override
    public void receive(SubDataClient client, ObjectMap<Integer> data) {
        UUID tracker = (data.contains(0x0000)?data.getUUID(0x0000):null);
        try {
            UUID id =    data.getUUID(0x0001);

            ProxiedPlayer local;
            RemotePlayer remote;
            if ((local = plugin.getPlayer(id)) != null) {
                Server server;
                if (data.contains(0x0002) && (server = plugin.api.getServer(data.getRawString(0x0002))) != null) {
                    local.connect(server);
                    client.sendPacket(new PacketTransferPlayer(0, tracker));
                } else {
                    client.sendPacket(new PacketTransferPlayer(1, tracker));
                }
            } else if ((remote = plugin.api.getRemotePlayer(id)) != null) {
                if (remote.getProxy().getSubData()[0] != null) {
                    ((SubDataClient) remote.getProxy().getSubData()[0]).sendPacket(new PacketExTransferPlayer(remote.getUniqueId(), (data.contains(0x0002)?data.getRawString(0x0002):null), r -> {
                        client.sendPacket(new PacketTransferPlayer(r.getInt(0x0001), tracker));
                    }));
                } else {
                    client.sendPacket(new PacketTransferPlayer(4, tracker));
                }
            } else {
                client.sendPacket(new PacketTransferPlayer(3, tracker));
            }
        } catch (Throwable e) {
            client.sendPacket(new PacketTransferPlayer(2, tracker));
            e.printStackTrace();
        }
    }

    @Override
    public int version() {
        return 0x0001;
    }
}

package net.ME1312.SubServers.Sync.Network.Packet;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.SubData.Client.Protocol.PacketObjectIn;
import net.ME1312.SubData.Client.Protocol.PacketObjectOut;
import net.ME1312.SubData.Client.SubDataSender;
import net.ME1312.SubServers.Sync.ExProxy;

import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.List;
import java.util.UUID;

/**
 * Disconnect External Player Packet
 */
public class PacketExDisconnectPlayer implements PacketObjectIn<Integer>, PacketObjectOut<Integer> {
    private ExProxy plugin;
    private int response;
    private UUID tracker;

    /**
     * New PacketExDisconnectPlayer (In)
     */
    public PacketExDisconnectPlayer(ExProxy plugin) {
        this.plugin = plugin;
    }

    /**
     * New PacketExDisconnectPlayer (Out)
     *
     * @param response Response ID
     * @param tracker Receiver ID
     */
    public PacketExDisconnectPlayer(int response, UUID tracker) {
        this.response = response;
        this.tracker = tracker;
    }

    @Override
    public ObjectMap<Integer> send(SubDataSender client) {
        ObjectMap<Integer> json = new ObjectMap<Integer>();
        if (tracker != null) json.set(0x0000, tracker);
        json.set(0x0001, response);
        return json;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void receive(SubDataSender client, ObjectMap<Integer> data) {
        UUID tracker =     (data.contains(0x0000)?data.getUUID(0x0000):null);
        List<UUID> ids = data.getUUIDList(0x0001);
        try {
            int failures = 0;
            for (UUID id : ids) {
                ProxiedPlayer local;
                if ((local = plugin.getPlayer(id)) != null) {
                    if (data.contains(0x0002)) {
                        local.disconnect(data.getString(0x0002));
                    } else local.disconnect();
                } else {
                    ++failures;
                }
            }
            client.sendPacket(new PacketExDisconnectPlayer(failures, tracker));
        } catch (Throwable e) {
            client.sendPacket(new PacketExDisconnectPlayer(ids.size(), tracker));
            e.printStackTrace();
        }
    }

    @Override
    public int version() {
        return 0x0001;
    }
}

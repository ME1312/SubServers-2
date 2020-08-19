package net.ME1312.SubServers.Bungee.Network.Packet;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Server.Protocol.PacketObjectIn;
import net.ME1312.SubData.Server.Protocol.PacketObjectOut;
import net.ME1312.SubData.Server.SubDataClient;
import net.ME1312.SubServers.Bungee.Host.RemotePlayer;
import net.ME1312.SubServers.Bungee.Host.Server;
import net.ME1312.SubServers.Bungee.Host.SubServer;
import net.ME1312.SubServers.Bungee.SubProxy;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.Map;
import java.util.UUID;

/**
 * Disconnect Player Packet
 */
public class PacketDisconnectPlayer implements PacketObjectIn<Integer>, PacketObjectOut<Integer> {
    private SubProxy plugin;
    private int response;
    private UUID tracker;

    /**
     * New PacketDisconnectPlayer (In)
     *
     * @param plugin SubPlugin
     */
    public PacketDisconnectPlayer(SubProxy plugin) {
        if (Util.isNull(plugin)) throw new NullPointerException();
        this.plugin = plugin;
    }

    /**
     * New PacketDisconnectPlayer (Out)
     *
     * @param response Response ID
     * @param tracker Receiver ID
     */
    public PacketDisconnectPlayer(int response, UUID tracker) {
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
        UUID tracker =      (data.contains(0x0000)?data.getUUID(0x0000):null);
        try {
            UUID id =       data.getUUID(0x0001);

            ProxiedPlayer local;
            RemotePlayer remote;
            if ((local = plugin.getPlayer(id)) != null) {
                if (data.contains(0x0002)) {
                    local.disconnect(data.getRawString(0x0002));
                } else {
                    local.disconnect();
                }
                client.sendPacket(new PacketDisconnectPlayer(2, tracker));
            } else if ((remote = plugin.api.getGlobalPlayer(id)) != null) {

                client.sendPacket(new PacketDisconnectPlayer(2, tracker));
            } else {
                client.sendPacket(new PacketDisconnectPlayer(3, tracker));
            }
        } catch (Throwable e) {
            client.sendPacket(new PacketDisconnectPlayer(2, tracker));
            e.printStackTrace();
        }
    }

    @Override
    public int version() {
        return 0x0001;
    }
}

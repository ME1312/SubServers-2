package net.ME1312.SubServers.Bungee.Network.Packet;

import net.ME1312.SubData.Server.SubDataClient;
import net.ME1312.SubServers.Bungee.Host.External.ExternalHost;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Server.Protocol.PacketIn;
import net.ME1312.SubServers.Bungee.SubPlugin;

import java.lang.reflect.InvocationTargetException;

/**
 * External Host Queue Request Packet
 */
public class PacketInExRequestQueue implements PacketIn {
    private SubPlugin plugin;

    /**
     * New PacketInExRequestQueue
     */
    public PacketInExRequestQueue(SubPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void receive(SubDataClient client) {
        if (client.getHandler() != null && client.getHandler() instanceof ExternalHost && plugin.config.get().getMap("Hosts").getKeys().contains(((ExternalHost) client.getHandler()).getName())) {
            try {
                Util.reflect(ExternalHost.class.getDeclaredMethod("requeue"), client.getHandler());
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public int version() {
        return 0x0001;
    }
}

package net.ME1312.SubServers.Bungee.Network.Packet;

import net.ME1312.SubServers.Bungee.Host.External.ExternalHost;
import net.ME1312.SubServers.Bungee.Library.Config.YAMLSection;
import net.ME1312.SubServers.Bungee.Library.Version.Version;
import net.ME1312.SubServers.Bungee.Network.Client;
import net.ME1312.SubServers.Bungee.Network.PacketIn;
import net.ME1312.SubServers.Bungee.SubPlugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

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
    public void execute(Client client, YAMLSection data) {
        if (client.getHandler() != null && client.getHandler() instanceof ExternalHost && plugin.config.get().getSection("Hosts").getKeys().contains(((ExternalHost) client.getHandler()).getName())) {
            try {
                Method requeue = ExternalHost.class.getDeclaredMethod("requeue");
                requeue.setAccessible(true);
                requeue.invoke(client.getHandler());
                requeue.setAccessible(false);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public Version getVersion() {
        return new Version("2.11.0a");
    }
}

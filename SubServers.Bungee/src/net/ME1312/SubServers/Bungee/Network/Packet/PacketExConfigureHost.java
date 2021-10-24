package net.ME1312.SubServers.Bungee.Network.Packet;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Try;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Server.Protocol.PacketObjectIn;
import net.ME1312.SubData.Server.Protocol.PacketObjectOut;
import net.ME1312.SubData.Server.SubDataClient;
import net.ME1312.SubServers.Bungee.Host.External.ExternalHost;
import net.ME1312.SubServers.Bungee.Host.External.ExternalSubCreator;
import net.ME1312.SubServers.Bungee.SubProxy;

/**
 * External Host Configuration Packet
 */
public class PacketExConfigureHost implements PacketObjectIn<Integer>, PacketObjectOut<Integer> {
    private SubProxy plugin;
    private ExternalHost host;

    /**
     * New PacketExConfigureHost (In)
     */
    public PacketExConfigureHost(SubProxy plugin) {
        this.plugin = plugin;
    }

    /**
     * New PacketExConfigureHost (Out)
     */
    public PacketExConfigureHost(SubProxy plugin, ExternalHost host) {
        this.plugin = plugin;
        this.host = host;
    }

    @Override
    public ObjectMap<Integer> send(SubDataClient client) {
        ObjectMap<Integer> data = new ObjectMap<Integer>();
        data.set(0x0000, plugin.config.get().getMap("Hosts").getMap(host.getName()).clone());
        return data;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void receive(SubDataClient client, ObjectMap<Integer> data) {
        if (client.getHandler() != null && client.getHandler() instanceof ExternalHost && plugin.config.get().getMap("Hosts").getKeys().contains(((ExternalHost) client.getHandler()).getName())) {
            client.sendPacket(new PacketExConfigureHost(plugin, (ExternalHost) client.getHandler()));
            Try.all.run(() -> Util.reflect(ExternalSubCreator.class.getDeclaredField("enableRT"), ((ExternalHost) client.getHandler()).getCreator(), ((data == null || data.getBoolean(0x0000, false))?null:false)));
        }
    }

    @Override
    public int version() {
        return 0x0001;
    }
}

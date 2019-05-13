package net.ME1312.SubServers.Bungee.Network.Packet;

import net.ME1312.Galaxi.Library.UniversalFile;
import net.ME1312.SubServers.Bungee.Host.External.ExternalHost;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Server.SubDataClient;
import net.ME1312.SubData.Server.Protocol.PacketIn;
import net.ME1312.SubData.Server.Protocol.PacketStreamOut;
import net.ME1312.SubServers.Bungee.SubPlugin;

import java.io.*;

/**
 * External Host Template Download Packet
 */
public class PacketExDownloadTemplates implements PacketIn, PacketStreamOut {
    private SubPlugin plugin;
    private ExternalHost host;

    /**
     * New PacketExDownloadTemplates (In)
     */
    public PacketExDownloadTemplates(SubPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * New PacketExDownloadTemplates (Out)
     */
    public PacketExDownloadTemplates(SubPlugin plugin, ExternalHost host) {
        this.plugin = plugin;
        this.host = host;
    }

    @Override
    public void send(SubDataClient client, OutputStream stream) throws Throwable {
        try {
            Util.zip(new UniversalFile(plugin.dir, "SubServers:Templates"), stream);
            stream.close();
        } catch (Exception e) {
            System.out.println("SubData > Problem encoding template files for Host: " + host.getName());
            e.printStackTrace();
        }
    }

    @Override
    public void receive(SubDataClient client) {
        if (client.getHandler() != null && client.getHandler() instanceof ExternalHost && plugin.config.get().getMap("Hosts").getKeys().contains(((ExternalHost) client.getHandler()).getName())) {
            client.sendPacket(new PacketExDownloadTemplates(plugin, (ExternalHost) client.getHandler()));
        }
    }

    @Override
    public int version() {
        return 0x0001;
    }
}

package net.ME1312.SubServers.Bungee.Network.Packet;

import net.ME1312.Galaxi.Library.UniversalFile;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Server.Library.DataSize;
import net.ME1312.SubData.Server.Protocol.PacketIn;
import net.ME1312.SubData.Server.Protocol.PacketStreamOut;
import net.ME1312.SubData.Server.SubDataClient;
import net.ME1312.SubServers.Bungee.Host.External.ExternalHost;
import net.ME1312.SubServers.Bungee.Host.External.ExternalSubCreator;
import net.ME1312.SubServers.Bungee.Library.Compatibility.Logger;
import net.ME1312.SubServers.Bungee.SubProxy;

import java.io.OutputStream;

/**
 * External Host Template Download Packet
 */
public class PacketExDownloadTemplates implements PacketIn, PacketStreamOut {
    private SubProxy plugin;
    private ExternalHost host;

    /**
     * New PacketExDownloadTemplates (In)
     */
    public PacketExDownloadTemplates(SubProxy plugin) {
        this.plugin = plugin;
    }

    /**
     * New PacketExDownloadTemplates (Out)
     */
    public PacketExDownloadTemplates(SubProxy plugin, ExternalHost host) {
        this.plugin = plugin;
        this.host = host;
    }

    @Override
    public void send(SubDataClient client, OutputStream stream) throws Throwable {
        try {
            int initial = client.getBlockSize();
            client.setBlockSize(DataSize.MBB);
            Util.zip(new UniversalFile(plugin.dir, "SubServers:Templates"), stream);
            client.setBlockSize(initial);
            stream.close();

            Util.isException(() -> Util.reflect(ExternalSubCreator.class.getDeclaredField("enableRT"), host.getCreator(), true));
        } catch (Exception e) {
            Logger.get("SubData").info("Problem encoding template files for Host: " + host.getName());
            e.printStackTrace();
        }
    }

    @Override
    public void receive(SubDataClient client) {
        if (client.getHandler() != null && client.getHandler() instanceof ExternalHost) {
            client.sendPacket(new PacketExDownloadTemplates(plugin, (ExternalHost) client.getHandler()));
        }
    }

    @Override
    public int version() {
        return 0x0001;
    }
}

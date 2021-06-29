package net.ME1312.SubServers.Bungee.Network.Packet;

import net.ME1312.Galaxi.Library.UniversalFile;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Server.Library.DataSize;
import net.ME1312.SubData.Server.Protocol.PacketIn;
import net.ME1312.SubData.Server.Protocol.PacketStreamOut;
import net.ME1312.SubData.Server.SubDataClient;
import net.ME1312.SubServers.Bungee.Host.External.ExternalHost;
import net.ME1312.SubServers.Bungee.Host.External.ExternalSubCreator;
import net.ME1312.SubServers.Bungee.Host.SubCreator.ServerTemplate;
import net.ME1312.SubServers.Bungee.Library.Compatibility.Logger;
import net.ME1312.SubServers.Bungee.SubProxy;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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
            if (client.getBlockSize() < DataSize.MBB) client.tempBlockSize(DataSize.MBB);
            HashMap<String, ServerTemplate> map = Util.getDespiteException(() -> Util.reflect(ExternalSubCreator.class.getDeclaredField("templates"), ((ExternalHost) client.getHandler()).getCreator()), new HashMap<>());
            File dir = new UniversalFile(plugin.dir, "SubServers:Templates");
            ZipOutputStream zip = new ZipOutputStream(stream);

            byte[] buffer = new byte[4096];
            for (String file : Util.searchDirectory(dir)) {
                int index = file.indexOf(File.separatorChar);
                if (index != -1 && !map.containsKey(file.substring(0, index).toLowerCase())) {

                    zip.putNextEntry(new ZipEntry(file.replace(File.separatorChar, '/')));
                    FileInputStream in = new FileInputStream(dir.getAbsolutePath() + File.separator + file);

                    int len;
                    while ((len = in.read(buffer)) != -1) {
                        zip.write(buffer, 0, len);
                    }

                    in.close();
                }
            }
            zip.close();

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
        return 0x0002;
    }
}

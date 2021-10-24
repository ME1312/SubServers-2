package net.ME1312.SubServers.Host.Network.Packet;

import net.ME1312.Galaxi.Engine.GalaxiEngine;
import net.ME1312.Galaxi.Library.Directories;
import net.ME1312.SubData.Client.Protocol.PacketOut;
import net.ME1312.SubData.Client.Protocol.PacketStreamIn;
import net.ME1312.SubData.Client.SubDataSender;
import net.ME1312.SubServers.Host.ExHost;
import net.ME1312.SubServers.Host.SubAPI;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * External Host Template Download Packet
 */
public class PacketExDownloadTemplates implements PacketOut, PacketStreamIn {
    private static boolean first = false;
    private ExHost host;

    /**
     * New PacketExDownloadTemplates
     */
    public PacketExDownloadTemplates(ExHost host) {
        this.host = host;
    }

    @Override
    public void sending(SubDataSender client) throws Throwable {
        host.log.info.println("Downloading Remote Template Files...");
        first = true;
    }

    @Override
    public void receive(SubDataSender client, InputStream stream) {
        File dir = new File(GalaxiEngine.getInstance().getRuntimeDirectory(), "Cache/Remote/Templates");
        try {
            if (dir.exists()) Directories.delete(dir);
        } catch (Exception e) {
            SubAPI.getInstance().getAppInfo().getLogger().error.println(e);
        }

        try {
            dir.mkdirs();
            ZipInputStream zip = new ZipInputStream(stream);

            byte[] buffer = new byte[4096];
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                File newFile = new File(dir + File.separator + entry.getName().replace('/', File.separatorChar));
                if (newFile.exists()) {
                    if (newFile.isDirectory()) {
                        Directories.delete(newFile);
                    } else {
                        newFile.delete();
                    }
                }

                if (entry.isDirectory()) {
                    newFile.mkdirs();
                    continue;
                } else if (!newFile.getParentFile().exists()) {
                    newFile.getParentFile().mkdirs();
                }

                FileOutputStream fos = new FileOutputStream(newFile);
                int len;
                while ((len = zip.read(buffer)) != -1) {
                    fos.write(buffer, 0, len);
                }
                fos.close();
            }
            zip.close();

            host.creator.load(true);
            host.log.info.println(((first)?"":"New ") + "Remote Template Files Downloaded");
        } catch (Exception e) {
            SubAPI.getInstance().getAppInfo().getLogger().error.println("Problem decoding template files");
            SubAPI.getInstance().getAppInfo().getLogger().error.println(e);
        }
        first = false;
    }

    @Override
    public int version() {
        return 0x0002;
    }
}

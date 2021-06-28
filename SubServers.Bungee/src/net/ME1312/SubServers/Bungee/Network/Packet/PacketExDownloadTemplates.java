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

import org.kamranzafar.jtar.TarEntry;
import org.kamranzafar.jtar.TarOutputStream;
import org.tukaani.xz.LZMA2Options;
import org.tukaani.xz.XZOutputStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.util.HashMap;

import static org.tukaani.xz.LZMA2Options.MODE_FAST;
import static org.tukaani.xz.XZ.CHECK_SHA256;

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
            TarOutputStream tar = new TarOutputStream(new XZOutputStream(stream, new LZMA2Options(MODE_FAST), CHECK_SHA256));
            File dir = new UniversalFile(plugin.dir, "SubServers:Templates");

            byte[] buffer = new byte[4096];
            for (String file : Util.searchDirectory(dir)) {
                int index = file.indexOf(File.separatorChar);
                if (index != -1 && !map.containsKey(file.substring(0, index).toLowerCase())) {

                    tar.putNextEntry(new TarEntry(new File(dir, file), file.replace(File.separatorChar, '/')));
                    FileInputStream in = new FileInputStream(dir.getAbsolutePath() + File.separator + file);

                    int len;
                    while ((len = in.read(buffer)) != -1) {
                        tar.write(buffer, 0, len);
                    }

                    in.close();
                }
            }
            tar.close();

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

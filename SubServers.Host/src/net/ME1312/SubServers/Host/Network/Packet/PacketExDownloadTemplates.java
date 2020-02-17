package net.ME1312.SubServers.Host.Network.Packet;

import net.ME1312.Galaxi.Engine.GalaxiEngine;
import net.ME1312.Galaxi.Library.UniversalFile;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Client.Protocol.PacketOut;
import net.ME1312.SubData.Client.Protocol.PacketStreamIn;
import net.ME1312.SubData.Client.SubDataSender;
import net.ME1312.SubServers.Host.ExHost;
import net.ME1312.SubServers.Host.SubAPI;

import java.io.InputStream;

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
        host.log.info.println("Downloading Template Files...");
        first = true;
    }

    @Override
    public void receive(SubDataSender client, InputStream stream) {
        UniversalFile dir = new UniversalFile(GalaxiEngine.getInstance().getRuntimeDirectory(), "Templates");
        try {
            if (dir.exists()) Util.deleteDirectory(dir);
        } catch (Exception e) {
            SubAPI.getInstance().getAppInfo().getLogger().error.println(e);
        }

        try {
            Util.unzip(stream, dir);
            host.log.info.println(((first)?"":"New ") + "Template Files Downloaded");
        } catch (Exception e) {
            SubAPI.getInstance().getAppInfo().getLogger().error.println("Problem decoding template files");
            SubAPI.getInstance().getAppInfo().getLogger().error.println(e);
        }
        first = false;
    }

    @Override
    public int version() {
        return 0x0001;
    }
}

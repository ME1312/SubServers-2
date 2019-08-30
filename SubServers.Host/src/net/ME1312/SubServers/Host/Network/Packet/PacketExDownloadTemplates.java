package net.ME1312.SubServers.Host.Network.Packet;

import net.ME1312.Galaxi.Engine.GalaxiEngine;
import net.ME1312.Galaxi.Library.UniversalFile;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Client.Protocol.PacketOut;
import net.ME1312.SubData.Client.Protocol.PacketStreamIn;
import net.ME1312.SubData.Client.SubDataClient;
import net.ME1312.SubServers.Host.ExHost;
import net.ME1312.SubServers.Host.SubAPI;

import java.io.InputStream;

/**
 * External Host Template Download Packet
 */
public class PacketExDownloadTemplates implements PacketOut, PacketStreamIn {
    private ExHost host;

    /**
     * New PacketExDownloadTemplates
     */
    public PacketExDownloadTemplates(ExHost host) {
        this.host = host;
    }

    @Override
    public void sending(SubDataClient client) throws Throwable {
        UniversalFile dir = new UniversalFile(GalaxiEngine.getInstance().getRuntimeDirectory(), "Templates");
        if (dir.exists()) Util.deleteDirectory(dir);
        host.log.info.println("Downloading Template Files...");
    }

    @Override
    public void receive(SubDataClient client, InputStream stream) {
        try {
            Util.unzip(stream, new UniversalFile(GalaxiEngine.getInstance().getRuntimeDirectory(), "Templates"));
            host.log.info.println("Template Files Downloaded");
        } catch (Exception e) {
            SubAPI.getInstance().getAppInfo().getLogger().error.println("Problem decoding template files");
            SubAPI.getInstance().getAppInfo().getLogger().error.println(e);
        }
    }

    @Override
    public int version() {
        return 0x0001;
    }
}

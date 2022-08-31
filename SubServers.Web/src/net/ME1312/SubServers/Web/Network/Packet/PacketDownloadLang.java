package net.ME1312.SubServers.Web.Network.Packet;

import net.ME1312.Galaxi.Library.Container.ContainedPair;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Try;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Client.Protocol.PacketObjectIn;
import net.ME1312.SubData.Client.Protocol.PacketOut;
import net.ME1312.SubData.Client.SubDataClient;
import net.ME1312.SubData.Client.SubDataSender;
import net.ME1312.SubServers.Host.ExHost;
import net.ME1312.SubServers.Web.JettyServer;

import java.util.Calendar;
import java.util.logging.Logger;

/**
 * Download Lang Packet
 */
public class PacketDownloadLang implements PacketObjectIn<Integer>, PacketOut {
    private JettyServer host;

    /**
     * New PacketDownloadLang (In)
     *
     * @param host ExHost
     */
    public PacketDownloadLang(JettyServer host) {
        Util.nullpo(host);
        this.host = host;
    }

    /**
     * New PacketDownloadLang (Out)
     */
    public PacketDownloadLang() {}

    @Override
    public void receive(SubDataSender client, ObjectMap<Integer> data) {
        Logger log = Try.all.get(() -> Util.reflect(SubDataClient.class.getDeclaredField("log"), client.getConnection()));
        try {
            Util.reflect(JettyServer.class.getDeclaredField("lang"), host, new ContainedPair<>(Calendar.getInstance().getTime().getTime(), data.getObject(0x0001)));
            log.info("Lang Settings Downloaded");
        } catch (IllegalAccessException | NoSuchFieldException e) {
            e.printStackTrace();
        }
    }
}

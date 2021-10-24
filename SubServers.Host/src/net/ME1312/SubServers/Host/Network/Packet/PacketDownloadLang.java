package net.ME1312.SubServers.Host.Network.Packet;

import net.ME1312.Galaxi.Library.Container.ContainedPair;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Try;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Client.Protocol.PacketObjectIn;
import net.ME1312.SubData.Client.Protocol.PacketOut;
import net.ME1312.SubData.Client.SubDataClient;
import net.ME1312.SubData.Client.SubDataSender;
import net.ME1312.SubServers.Host.ExHost;

import java.util.Calendar;
import java.util.logging.Logger;

/**
 * Download Lang Packet
 */
public class PacketDownloadLang implements PacketObjectIn<Integer>, PacketOut {
    private ExHost host;

    /**
     * New PacketDownloadLang (In)
     *
     * @param host ExHost
     */
    public PacketDownloadLang(ExHost host) {
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
            Util.reflect(ExHost.class.getDeclaredField("lang"), host, new ContainedPair<>(Calendar.getInstance().getTime().getTime(), data.getObject(0x0001)));
            log.info("Lang Settings Downloaded");
        } catch (IllegalAccessException | NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int version() {
        return 0x0001;
    }
}

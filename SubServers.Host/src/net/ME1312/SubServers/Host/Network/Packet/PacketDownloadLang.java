package net.ME1312.SubServers.Host.Network.Packet;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.NamedContainer;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Client.Protocol.PacketObjectIn;
import net.ME1312.SubData.Client.Protocol.PacketOut;
import net.ME1312.SubData.Client.SubDataClient;
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
        if (Util.isNull(host)) throw new NullPointerException();
        this.host = host;
    }

    /**
     * New PacketDownloadLang (Out)
     */
    public PacketDownloadLang() {}

    @Override
    public void receive(SubDataClient client, ObjectMap<Integer> data) {
        Logger log = Util.getDespiteException(() -> Util.reflect(SubDataClient.class.getDeclaredField("log"), client), null);
        try {
            Util.reflect(ExHost.class.getDeclaredField("lang"), host, new NamedContainer<>(Calendar.getInstance().getTime().getTime(), data.getObject(0x0001)));
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

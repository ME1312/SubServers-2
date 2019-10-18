package net.ME1312.SubServers.Host.Network.Packet;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Client.Protocol.PacketObjectIn;
import net.ME1312.SubData.Client.SubDataClient;
import net.ME1312.SubData.Client.SubDataSender;
import net.ME1312.SubServers.Host.ExHost;

import java.util.logging.Logger;

/**
 * Reload Packet
 */
public class PacketInExReload implements PacketObjectIn<Integer> {
    private ExHost host;

    /**
     * New PacketInExReload
     *
     * @param host Plugin
     */
    public PacketInExReload(ExHost host) {
        this.host = host;
    }

    @Override
    public void receive(SubDataSender client, ObjectMap<Integer> data) {
        Logger log = Util.getDespiteException(() -> Util.reflect(SubDataClient.class.getDeclaredField("log"), client.getConnection()), null);
        if (data != null && data.contains(0x0000)) log.warning("Received request for a plugin reload: " + data.getString(0x0000));
        else log.warning("Received request for a plugin reload");
        new Thread(() -> {
            try {
                host.reload(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, "SubServers.Host::Network_Reload_Handler").start();
    }

    @Override
    public int version() {
        return 0x0001;
    }
}

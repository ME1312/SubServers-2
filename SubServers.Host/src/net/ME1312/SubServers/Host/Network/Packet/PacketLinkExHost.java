package net.ME1312.SubServers.Host.Network.Packet;

import net.ME1312.Galaxi.Engine.GalaxiEngine;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Try;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Client.Library.DebugUtil;
import net.ME1312.SubData.Client.Protocol.Initial.InitialPacket;
import net.ME1312.SubData.Client.Protocol.PacketObjectIn;
import net.ME1312.SubData.Client.Protocol.PacketObjectOut;
import net.ME1312.SubData.Client.SubDataClient;
import net.ME1312.SubData.Client.SubDataSender;
import net.ME1312.SubServers.Host.ExHost;

import java.util.logging.Logger;

/**
 * Link Host Packet
 */
public class PacketLinkExHost implements InitialPacket, PacketObjectIn<Integer>, PacketObjectOut<Integer> {
    private ExHost host;
    private int channel;

    /**
     * New PacketLinkHost (In)
     *
     * @param host SubServers.Host
     */
    public PacketLinkExHost(ExHost host) {
        Util.nullpo(host);
        this.host = host;
    }

    /**
     * New PacketLinkHost (Out)
     *
     * @param host SubServers.Host
     */
    public PacketLinkExHost(ExHost host, int channel) {
        Util.nullpo(host);
        this.host = host;
        this.channel = channel;
    }

    @Override
    public ObjectMap<Integer> send(SubDataSender client) {
        ObjectMap<Integer> data = new ObjectMap<Integer>();
        data.set(0x0000, host.api.getName());
        data.set(0x0001, channel);
        return data;
    }

    @Override
    public void receive(SubDataSender client, ObjectMap<Integer> data) throws Throwable {
        Logger log = Try.all.get(() -> Util.reflect(SubDataClient.class.getDeclaredField("log"), client.getConnection()));
        if (data.getInt(0x0001) == 0) {
            setReady(client.getConnection());
        } else {
            log.severe("Could not link name with host" + ((data.contains(0x0002))?": "+data.getString(0x0002):'.'));
            DebugUtil.logException(new IllegalStateException(), log);
            GalaxiEngine.getInstance().stop();
        }
    }

    @Override
    public int version() {
        return 0x0001;
    }
}

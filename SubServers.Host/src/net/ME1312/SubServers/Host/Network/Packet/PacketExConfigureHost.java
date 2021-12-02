package net.ME1312.SubServers.Host.Network.Packet;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.SubData.Client.Protocol.PacketObjectIn;
import net.ME1312.SubData.Client.Protocol.PacketObjectOut;
import net.ME1312.SubData.Client.SubDataSender;
import net.ME1312.SubServers.Host.ExHost;

import java.util.Map;

/**
 * External Host Configuration Packet
 */
public class PacketExConfigureHost implements PacketObjectIn<Integer>, PacketObjectOut<Integer> {
    private static boolean first = false;
    private ExHost host;

    /**
     * New PacketExConfigureHost
     */
    public PacketExConfigureHost(ExHost host) {
        this.host = host;
    }

    @Override
    public ObjectMap<Integer> send(SubDataSender client) {
        host.log.info.println("Downloading Host Settings...");
        first = true;

        ObjectMap<Integer> data = new ObjectMap<Integer>();
        data.set(0x0000, host.config.get().getMap("Settings").getBoolean("Download-Templates", true));
        return data;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void receive(SubDataSender client, ObjectMap<Integer> data) {
        host.host = new ObjectMap<>((Map<String, ?>) data.getObject(0x0000));

        host.log.info.println(((first)?"":"New ") + "Host Settings Downloaded");
        first = false;
    }
}

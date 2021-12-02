package net.ME1312.SubServers.Host.Network.Packet;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.SubData.Client.Protocol.PacketObjectIn;
import net.ME1312.SubData.Client.SubDataSender;
import net.ME1312.SubServers.Host.ExHost;

/**
 * Reset Packet
 */
public class PacketInExReset implements PacketObjectIn<Integer> {
    private ExHost host;

    public PacketInExReset(ExHost host) {
        this.host = host;
    }

    @Override
    public void receive(SubDataSender client, ObjectMap<Integer> data) {/*
        List<String> subservers = new ArrayList<String>();
        subservers.addAll(host.servers.keySet());

        for (String server : subservers) {
            host.servers.get(server).stop();
            try {
                host.servers.get(server).waitFor();
            } catch (Exception e) {
                host.log.error.println(e);
            }
        }
        subservers.clear();
        host.servers.clear(); */

        host.creator.terminate();
        try {
            host.creator.waitFor();
        } catch (Exception e) {
            host.log.error.println(e);
        }
    }
}

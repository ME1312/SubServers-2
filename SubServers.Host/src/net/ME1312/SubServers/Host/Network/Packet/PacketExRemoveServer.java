package net.ME1312.SubServers.Host.Network.Packet;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Client.Protocol.PacketObjectIn;
import net.ME1312.SubData.Client.Protocol.PacketObjectOut;
import net.ME1312.SubData.Client.SubDataClient;
import net.ME1312.SubData.Client.SubDataSender;
import net.ME1312.SubServers.Host.ExHost;
import net.ME1312.SubServers.Host.Executable.SubServerImpl;
import net.ME1312.SubServers.Host.SubAPI;

import com.dosse.upnp.UPnP;

import java.util.UUID;
import java.util.logging.Logger;

/**
 * Create Server Packet
 */
public class PacketExRemoveServer implements PacketObjectIn<Integer>, PacketObjectOut<Integer> {
    private ExHost host;
    private int response;
    private UUID tracker;

    /**
     * New PacketExRemoveServer (In)
     *
     * @param host ExHost
     */
    public PacketExRemoveServer(ExHost host) {
        if (Util.isNull(host)) throw new NullPointerException();
        this.host = host;
    }

    /**
     * New PacketExRemoveServer (Out)
     *
     * @param response Response ID
     * @param tracker Receiver ID
     */
    public PacketExRemoveServer(int response, UUID tracker) {
        if (Util.isNull(response)) throw new NullPointerException();
        this.response = response;
        this.tracker = tracker;
    }

    @Override
    public ObjectMap<Integer> send(SubDataSender client) {
        ObjectMap<Integer> data = new ObjectMap<Integer>();
        if (tracker != null) data.set(0x0000, tracker);
        data.set(0x0001, response);
        return data;
    }

    @Override
    public void receive(SubDataSender client, ObjectMap<Integer> data) {
        Logger log = Util.getDespiteException(() -> Util.reflect(SubDataClient.class.getDeclaredField("log"), client.getConnection()), null);
        UUID tracker =       (data.contains(0x0000)?data.getUUID(0x0000):null);
        try {
            String name = data.getRawString(0x0001);
            if (!host.servers.keySet().contains(name.toLowerCase())) {
                ((SubDataClient) SubAPI.getInstance().getSubDataNetwork()[0]).sendPacket(new PacketExRemoveServer(1, tracker));
            } else if (host.servers.get(name.toLowerCase()).isRunning()) {
                ((SubDataClient) SubAPI.getInstance().getSubDataNetwork()[0]).sendPacket(new PacketExRemoveServer(3, tracker));
            } else {
                SubServerImpl server  = host.servers.get(name.toLowerCase());
                if (UPnP.isUPnPAvailable() && UPnP.isMappedTCP(server.getPort())) UPnP.closePortTCP(server.getPort());
                host.servers.remove(name.toLowerCase());
                log.info("Removed SubServer: " + server.getName());
                ((SubDataClient) SubAPI.getInstance().getSubDataNetwork()[0]).sendPacket(new PacketExRemoveServer(0, tracker));
            }
        } catch (Throwable e) {
            ((SubDataClient) SubAPI.getInstance().getSubDataNetwork()[0]).sendPacket(new PacketExRemoveServer(2, tracker));
            host.log.error.println(e);
        }
    }

    @Override
    public int version() {
        return 0x0001;
    }
}
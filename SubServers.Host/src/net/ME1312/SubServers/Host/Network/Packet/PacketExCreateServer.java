package net.ME1312.SubServers.Host.Network.Packet;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.SubData.Client.Protocol.PacketObjectIn;
import net.ME1312.SubData.Client.Protocol.PacketObjectOut;
import net.ME1312.SubData.Client.SubDataClient;
import net.ME1312.SubServers.Host.ExHost;

import java.util.UUID;

/**
 * Create Server Packet
 */
public class PacketExCreateServer implements PacketObjectIn<Integer>, PacketObjectOut<Integer> {
    private ExHost host;
    private int response;
    private String message;
    private ObjectMap<String> info;
    private String address;
    private UUID tracker;

    /**
     * New PacketExCreateServer (In)
     *
     * @param host ExHost
     */
    public PacketExCreateServer(ExHost host) {
        if (Util.isNull(host)) throw new NullPointerException();
        this.host = host;
    }

    /**
     * New PacketCreateServer (Out)
     *
     * @param response Response ID
     * @param message Message
     * @param tracker Receiver ID
     */
    public PacketExCreateServer(int response, String message, UUID tracker) {
        if (Util.isNull(response)) throw new NullPointerException();
        this.response = response;
        this.message = message;
        this.tracker = tracker;
    }

    /**
     * New PacketCreateServer (Out)
     *
     * @param response Response ID
     * @param message Message
     * @param info Creator Info
     * @param address Internal Server Address
     * @param tracker Receiver ID
     */
    public PacketExCreateServer(int response, String message, ObjectMap<String> info, String address, UUID tracker) {
        if (Util.isNull(response)) throw new NullPointerException();
        this.response = response;
        this.message = message;
        this.info = info;
        this.address = address;
        this.tracker = tracker;
    }

    @Override
    public ObjectMap<Integer> send(SubDataClient client) {
        ObjectMap<Integer> data = new ObjectMap<Integer>();
        if (tracker != null) data.set(0x0000, tracker);
        data.set(0x0001, response);
        if (info != null) data.set(0x0002, info);
        if (address != null) data.set(0x0003, address);
        if (message != null) data.set(0x0004, message);
        return data;
    }

    @Override
    public void receive(SubDataClient client, ObjectMap<Integer> data) {
        UUID tracker =          (data.contains(0x0000)?data.getUUID(0x0000):null);
        try {
            if (data.contains(0x0001)) {
                if (data.contains(0x0001)) {
                    host.creator.terminate(data.getRawString(0x0001).toLowerCase());
                } else {
                    host.creator.terminate();
                }
                client.sendPacket(new PacketExCreateServer(1, null, tracker));
            } else {
                String name =     data.getRawString(0x0002);
                String template = data.getRawString(0x0003);
                Version version =    (data.contains(0x0004)?data.getVersion(0x0004):null);
                Integer port =          data.getInt(0x0005);
                String dir =      data.getRawString(0x0006);
                UUID log =             data.getUUID(0x0007);

                host.creator.create(name, host.templates.get(template.toLowerCase()), version,
                        port, dir, log, tracker);
            }
        } catch (Throwable e) {
            host.log.error.println(e);
        }
    }

    @Override
    public int version() {
        return 0x0001;
    }
}
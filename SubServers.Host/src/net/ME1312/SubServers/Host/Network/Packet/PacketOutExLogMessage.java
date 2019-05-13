package net.ME1312.SubServers.Host.Network.Packet;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.SubData.Client.Protocol.PacketObjectOut;
import net.ME1312.SubData.Client.SubDataClient;

import java.util.UUID;

/**
 * Message Log External Host Packet
 */
public class PacketOutExLogMessage implements PacketObjectOut<Integer> {
    private UUID address;
    private String line;
    private boolean terminate;

    /**
     * New PacketInExLogMessage (Out)
     */
    public PacketOutExLogMessage(UUID address, String line) {
        this.address = address;
        this.line = line;
        this.terminate = false;
    }

    /**
     * New PacketInExLogMessage (Out)
     */
    public PacketOutExLogMessage(UUID address, boolean terminate) {
        this.address = address;
        this.line = null;
        this.terminate = terminate;
    }

    @Override
    public ObjectMap<Integer> send(SubDataClient client) throws Exception {
        if (terminate) client.close();

        ObjectMap<Integer> data = new ObjectMap<Integer>();
        data.set(0x0000, address);
        if (line != null) data.set(0x0001, line);
        return data;
    }

    @Override
    public int version() {
        return 0x0001;
    }
}

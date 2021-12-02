package net.ME1312.SubServers.Host.Network.Packet;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.SubData.Client.Protocol.PacketObjectOut;
import net.ME1312.SubData.Client.SubDataSender;

import java.util.UUID;

/**
 * Message Log External Host Packet
 */
public class PacketOutExLogMessage implements PacketObjectOut<Integer> {
    private UUID address;
    private Byte level;
    private String line;

    /**
     * New PacketInExLogMessage (Out)
     */
    public PacketOutExLogMessage(UUID address, byte level, String line) {
        this.address = address;
        this.level = level;
        this.line = line;
    }

    /**
     * New PacketInExLogMessage (Reset)
     */
    public PacketOutExLogMessage(UUID address) {
        this.address = address;
    }

    @Override
    public ObjectMap<Integer> send(SubDataSender client) throws Exception {
        ObjectMap<Integer> data = new ObjectMap<Integer>();
        data.set(0x0000, address);
        if (level != null) data.set(0x0001, level);
        if (line != null)  data.set(0x0002, line);
        return data;
    }
}

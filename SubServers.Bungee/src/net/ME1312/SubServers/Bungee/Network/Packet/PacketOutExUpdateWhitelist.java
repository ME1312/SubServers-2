package net.ME1312.SubServers.Bungee.Network.Packet;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Server.Protocol.PacketObjectOut;
import net.ME1312.SubData.Server.SubDataClient;

import java.util.UUID;

/**
 * Update External Whitelist Packet
 */
public class PacketOutExUpdateWhitelist implements PacketObjectOut<Integer> {
    private String name;
    private boolean mode;
    private UUID value;

    /**
     * New PacketOutExUpdateWhitelist
     *
     * @param name Server Name
     * @param mode Update Mode (true for add, false for remove)
     * @param value Whitelist Value
     */
    public PacketOutExUpdateWhitelist(String name, boolean mode, UUID value) {
        if (Util.isNull(name, mode, value)) throw new NullPointerException();
        this.name = name;
        this.mode = mode;
        this.value = value;
    }

    @Override
    public ObjectMap<Integer> send(SubDataClient client) {
        ObjectMap<Integer> data = new ObjectMap<Integer>();
        data.set(0x0000, name);
        data.set(0x0001, mode);
        data.set(0x0002, value);
        return data;
    }

    @Override
    public int version() {
        return 0x0001;
    }
}

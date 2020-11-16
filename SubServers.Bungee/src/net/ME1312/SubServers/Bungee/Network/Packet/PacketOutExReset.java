package net.ME1312.SubServers.Bungee.Network.Packet;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.SubData.Server.Protocol.PacketObjectOut;
import net.ME1312.SubData.Server.SubDataClient;

/**
 * Reset Packet
 */
public class PacketOutExReset implements PacketObjectOut<Integer> {
    private String message;

    /**
     * New PacketOutExReset
     *
     * @param message Message
     */
    public PacketOutExReset(String message) {
        this.message = message;
    }

    @Override
    public ObjectMap<Integer> send(SubDataClient client) {
        if (message == null) {
            return null;
        } else {
            ObjectMap<Integer> json = new ObjectMap<Integer>();
            json.set(0x0000, message);
            return json;
        }
    }

    @Override
    public int version() {
        return 0x0001;
    }
}

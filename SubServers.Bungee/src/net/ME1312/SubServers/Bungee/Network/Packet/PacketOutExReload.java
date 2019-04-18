package net.ME1312.SubServers.Bungee.Network.Packet;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.SubData.Server.Protocol.PacketObjectOut;
import net.ME1312.SubData.Server.SubDataClient;

/**
 * Reload Packet
 */
public class PacketOutExReload implements PacketObjectOut<Integer> {
    private String message;

    /**
     * New PacketOutExReload
     *
     * @param message Message
     */
    public PacketOutExReload(String message) {
        this.message = message;
    }

    @Override
    public ObjectMap<Integer> send(SubDataClient client) {
        if (message == null) {
            return null;
        } else {
            ObjectMap<Integer> data = new ObjectMap<Integer>();
            data.set(0x0000, message);
            return data;
        }
    }

    @Override
    public int version() {
        return 0x0001;
    }
}

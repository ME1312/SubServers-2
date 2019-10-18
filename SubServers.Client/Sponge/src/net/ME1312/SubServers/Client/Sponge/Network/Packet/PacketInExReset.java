package net.ME1312.SubServers.Client.Sponge.Network.Packet;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Client.Protocol.PacketObjectIn;
import net.ME1312.SubData.Client.SubDataClient;
import net.ME1312.SubData.Client.SubDataSender;
import org.spongepowered.api.Sponge;

import java.util.logging.Logger;

/**
 * Reset Packet
 */
public class PacketInExReset implements PacketObjectIn<Integer> {

    @Override
    public void receive(SubDataSender client, ObjectMap<Integer> data) {
        Logger log = Util.getDespiteException(() -> Util.reflect(SubDataClient.class.getDeclaredField("log"), client.getConnection()), null);
        if (data != null && data.contains(0x0000)) log.warning("Received shutdown signal: " + data.getString(0x0000));
        else log.warning("Received shutdown signal");
        Sponge.getServer().shutdown();
    }

    @Override
    public int version() {
        return 0x0001;
    }
}

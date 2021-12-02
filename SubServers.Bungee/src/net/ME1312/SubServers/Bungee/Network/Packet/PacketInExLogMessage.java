package net.ME1312.SubServers.Bungee.Network.Packet;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Server.Protocol.PacketObjectIn;
import net.ME1312.SubData.Server.SubDataClient;
import net.ME1312.SubServers.Bungee.Host.External.ExternalSubLogger;

import java.util.HashMap;
import java.util.UUID;

/**
 * Message Log External Host Packet
 */
public class PacketInExLogMessage implements PacketObjectIn<Integer> {
    private static HashMap<UUID, ExternalSubLogger> loggers = new HashMap<UUID, ExternalSubLogger>();

    /**
     * New PacketInExLogMessage (Registerer)
     */
    public PacketInExLogMessage() {}

    @Override
    public void receive(SubDataClient client, ObjectMap<Integer> data) {
        try {
            if (data.contains(0x0000)) {
                if (data.contains(0x0002) && data.contains(0x0001)) {
                    if (loggers.containsKey(data.getUUID(0x0000))) {
                        Util.reflect(ExternalSubLogger.class.getDeclaredMethod("log", int.class, String.class), loggers.get(data.getUUID(0x0000)), data.getInt(0x0001), data.getString(0x0002));
                    }
                } else {
                    unregister(data.getUUID(0x0000));
                    client.close();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Register External Logger
     *
     * @param logger Logger
     * @return External Address
     */
    public static UUID register(ExternalSubLogger logger) {
        UUID id = Util.getNew(loggers.keySet(), UUID::randomUUID);
        loggers.put(id, logger);
        return id;
    }

    /**
     * Unregister External Logger
     *
     * @param id External Address
     */
    public static void unregister(UUID id) {
        if (loggers.containsKey(id)) loggers.remove(id).stop();
    }
}

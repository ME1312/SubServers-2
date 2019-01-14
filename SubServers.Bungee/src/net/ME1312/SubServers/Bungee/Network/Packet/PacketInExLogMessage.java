package net.ME1312.SubServers.Bungee.Network.Packet;

import net.ME1312.SubServers.Bungee.Host.External.ExternalSubLogger;
import net.ME1312.SubServers.Bungee.Library.Config.YAMLSection;
import net.ME1312.SubServers.Bungee.Library.Util;
import net.ME1312.SubServers.Bungee.Library.Version.Version;
import net.ME1312.SubServers.Bungee.Network.Client;
import net.ME1312.SubServers.Bungee.Network.PacketIn;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.UUID;

/**
 * Message Log External Host Packet
 */
public class PacketInExLogMessage implements PacketIn {
    private static HashMap<UUID, ExternalSubLogger> loggers = new HashMap<UUID, ExternalSubLogger>();

    /**
     * New PacketInExLogMessage (Registerer)
     */
    public PacketInExLogMessage() {}

    @Override
    public void execute(Client client, YAMLSection data) {
        try {
            if (data.contains("h") && data.contains("m") && data.getRawString("m").length() != 0 && loggers.keySet().contains(UUID.fromString(data.getRawString("h")))) {
                Util.reflect(ExternalSubLogger.class.getDeclaredMethod("log", String.class), loggers.get(UUID.fromString(data.getRawString("h"))), data.getRawString("m"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public Version getVersion() {
        return new Version("2.11.0a");
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
        loggers.remove(id);
    }
}

package net.ME1312.SubServers.Bungee.Network.Packet;

import net.ME1312.SubServers.Bungee.Library.Config.YAMLSection;
import net.ME1312.SubServers.Bungee.Library.Version.Version;
import net.ME1312.SubServers.Bungee.Network.PacketOut;

/**
 * Reset Packet
 */
public class PacketOutReset implements PacketOut {
    private String message;

    /**
     * New PacketOutReset
     *
     * @param message Message
     */
    public PacketOutReset(String message) {
        this.message = message;
    }

    @Override
    public YAMLSection generate() {
        if (message == null) {
            return null;
        } else {
            YAMLSection json = new YAMLSection();
            json.set("m", message);
            return json;
        }
    }

    @Override
    public Version getVersion() {
        return new Version("2.11.0a");
    }
}

package net.ME1312.SubServers.Bungee.Network.Packet;

import net.ME1312.SubServers.Bungee.Library.Config.YAMLSection;
import net.ME1312.SubServers.Bungee.Library.Version.Version;
import net.ME1312.SubServers.Bungee.Network.PacketOut;

/**
 * Reload Packet
 */
public class PacketOutReload implements PacketOut {
    private String message;

    /**
     * New PacketOutReload
     *
     * @param message Message
     */
    public PacketOutReload(String message) {
        this.message = message;
    }

    @Override
    public YAMLSection generate() {
        if (message == null) {
            return null;
        } else {
            YAMLSection data = new YAMLSection();
            data.set("m", message);
            return data;
        }
    }

    @Override
    public Version getVersion() {
        return new Version("2.11.0a");
    }
}

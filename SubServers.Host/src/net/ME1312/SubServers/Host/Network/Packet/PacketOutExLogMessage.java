package net.ME1312.SubServers.Host.Network.Packet;

import net.ME1312.SubServers.Host.Library.Config.YAMLSection;
import net.ME1312.SubServers.Host.Library.Version.Version;
import net.ME1312.SubServers.Host.Network.PacketOut;

import java.util.UUID;

/**
 * Message Log External Host Packet
 */
public class PacketOutExLogMessage implements PacketOut {
    private UUID address;
    private String line;

    /**
     * New PacketInExLogMessage (Out)
     */
    public PacketOutExLogMessage(UUID address, String line) {
        this.address = address;
        this.line = line;
    }

    @Override
    public YAMLSection generate() {
        YAMLSection data = new YAMLSection();
        data.set("h", address.toString());
        data.set("m", line);
        return data;
    }

    @Override
    public Version getVersion() {
        return new Version("2.11.0a");
    }
}

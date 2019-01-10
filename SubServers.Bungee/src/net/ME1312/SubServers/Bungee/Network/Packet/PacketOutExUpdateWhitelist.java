package net.ME1312.SubServers.Bungee.Network.Packet;

import net.ME1312.SubServers.Bungee.Library.Config.YAMLSection;
import net.ME1312.SubServers.Bungee.Library.Util;
import net.ME1312.SubServers.Bungee.Library.Version.Version;
import net.ME1312.SubServers.Bungee.Network.PacketOut;

import java.util.UUID;

/**
 * Update External Whitelist Packet
 */
public class PacketOutExUpdateWhitelist implements PacketOut {
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
    public YAMLSection generate() {
        YAMLSection data = new YAMLSection();
        data.set("server", name);
        data.set("mode", mode);
        data.set("value", value);
        return data;
    }
    @Override
    public Version getVersion() {
        return new Version("2.13.2c");
    }
}

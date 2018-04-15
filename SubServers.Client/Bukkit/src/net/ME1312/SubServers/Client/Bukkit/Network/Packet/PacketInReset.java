package net.ME1312.SubServers.Client.Bukkit.Network.Packet;

import net.ME1312.SubServers.Client.Bukkit.Library.Config.YAMLSection;
import net.ME1312.SubServers.Client.Bukkit.Library.Version.Version;
import net.ME1312.SubServers.Client.Bukkit.Network.PacketIn;
import org.bukkit.Bukkit;

/**
 * Reset Packet
 */
public class PacketInReset implements PacketIn {

    @Override
    public void execute(YAMLSection data) {
        if (data != null && data.contains("m")) Bukkit.getLogger().warning("SubData > Received request for a server shutdown: " + data.getString("m"));
        else Bukkit.getLogger().warning("SubData > Received request for a server shutdown");
        Bukkit.shutdown();
    }

    @Override
    public Version getVersion() {
        return new Version("2.11.0a");
    }
}

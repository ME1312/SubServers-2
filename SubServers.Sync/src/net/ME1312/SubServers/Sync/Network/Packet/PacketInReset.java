package net.ME1312.SubServers.Sync.Network.Packet;

import net.ME1312.SubServers.Sync.Library.Config.YAMLSection;
import net.ME1312.SubServers.Sync.Library.Version.Version;
import net.ME1312.SubServers.Sync.Network.PacketIn;
import net.ME1312.SubServers.Sync.SubAPI;

/**
 * Reset Packet
 */
public class PacketInReset implements PacketIn {

    @SuppressWarnings("deprecation")
    @Override
    public void execute(YAMLSection data) {
        SubAPI.getInstance().getInternals().servers.clear();
    }

    @Override
    public Version getVersion() {
        return new Version("2.11.0a");
    }
}

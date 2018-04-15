package net.ME1312.SubServers.Host.Network.Packet;

import net.ME1312.SubServers.Host.ExHost;
import net.ME1312.SubServers.Host.Library.Config.YAMLSection;
import net.ME1312.SubServers.Host.Library.Version.Version;
import net.ME1312.SubServers.Host.Network.PacketIn;

import java.io.IOException;

/**
 * Reload Packet
 */
public class PacketInReload implements PacketIn {
    private ExHost host;

    public PacketInReload(ExHost host) {
        this.host = host;
    }

    @Override
    public void execute(YAMLSection data) {
        try {
            host.reload();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Version getVersion() {
        return new Version("2.11.0a");
    }
}

package net.ME1312.SubServers.Sync.Network.Packet;

import net.ME1312.SubServers.Sync.Library.Config.YAMLSection;
import net.ME1312.SubServers.Sync.Library.Version.Version;
import net.ME1312.SubServers.Sync.Network.PacketIn;
import net.ME1312.SubServers.Sync.SubPlugin;

/**
 * Update External Whitelist Packet
 */
public class PacketInExUpdateWhitelist implements PacketIn {
    private SubPlugin plugin;

    /**
     * New PacketInExUpdateWhitelist
     */
    public PacketInExUpdateWhitelist(SubPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(YAMLSection data) throws Throwable {
        if (data.getBoolean("mode")) {
            plugin.servers.get(data.getRawString("name")).whitelist(data.getUUID("value"));
        } else {
            plugin.servers.get(data.getRawString("name")).unwhitelist(data.getUUID("value"));
        }
    }

    @Override
    public Version getVersion() {
        return new Version("2.13.2c");
    }
}

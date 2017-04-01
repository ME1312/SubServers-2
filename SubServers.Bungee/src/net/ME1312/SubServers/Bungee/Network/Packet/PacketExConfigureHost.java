package net.ME1312.SubServers.Bungee.Network.Packet;

import net.ME1312.SubServers.Bungee.Host.External.ExternalHost;
import net.ME1312.SubServers.Bungee.Library.Version.Version;
import net.ME1312.SubServers.Bungee.Network.Client;
import net.ME1312.SubServers.Bungee.Network.PacketIn;
import net.ME1312.SubServers.Bungee.Network.PacketOut;
import net.ME1312.SubServers.Bungee.SubPlugin;
import org.json.JSONObject;

/**
 * External Host Configuration Packet
 */
public class PacketExConfigureHost implements PacketIn, PacketOut {
    private SubPlugin plugin;
    private ExternalHost host;

    /**
     * New PacketExConfigureHost (In)
     */
    public PacketExConfigureHost(SubPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * New PacketExConfigureHost (Out)
     */
    public PacketExConfigureHost(SubPlugin plugin, ExternalHost host) {
        this.plugin = plugin;
        this.host = host;
    }

    @Override
    public JSONObject generate() {
        JSONObject json = new JSONObject();
        json.put("host", plugin.config.get().getSection("Hosts").getSection(host.getName()).toJSON());
        return json;
    }

    @Override
    public void execute(Client client, JSONObject data) {
        if (client.getHandler() != null && client.getHandler() instanceof ExternalHost && plugin.config.get().getSection("Hosts").getKeys().contains(((ExternalHost) client.getHandler()).getName())) {
            client.sendPacket(new PacketExConfigureHost(plugin, (ExternalHost) client.getHandler()));
        }
    }

    @Override
    public Version getVersion() {
        return new Version("2.11.0a");
    }
}

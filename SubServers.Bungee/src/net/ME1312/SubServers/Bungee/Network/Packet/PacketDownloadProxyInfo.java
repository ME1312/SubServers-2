package net.ME1312.SubServers.Bungee.Network.Packet;

import net.ME1312.SubServers.Bungee.Library.Version.Version;
import net.ME1312.SubServers.Bungee.Network.Client;
import net.ME1312.SubServers.Bungee.Network.PacketIn;
import net.ME1312.SubServers.Bungee.Network.PacketOut;
import net.ME1312.SubServers.Bungee.SubPlugin;
import org.json.JSONObject;

/**
 * Download Proxy Info Packet
 */
public class PacketDownloadProxyInfo implements PacketIn, PacketOut {
    private SubPlugin plugin;
    private String id;

    /**
     * New PacketDownloadProxyInfo (In)
     *
     * @param plugin SubPlugin
     */
    public PacketDownloadProxyInfo(SubPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * New PacketDownloadProxyInfo (Out)
     *
     * @param plugin SubPlugin
     * @param id Receiver ID
     */
    public PacketDownloadProxyInfo(SubPlugin plugin, String id) {
        this.plugin = plugin;
        this.id = id;
    }

    @Override
    public JSONObject generate() {
        JSONObject json = new JSONObject();
        if (id != null) json.put("id", id);
        JSONObject subservers = new JSONObject();
        subservers.put("version", plugin.version.toString());
        if (plugin.bversion != null) subservers.put("beta", plugin.bversion.toString());
        subservers.put("hosts", plugin.api.getHosts().size());
        subservers.put("subservers", plugin.api.getSubServers().size());
        json.put("subservers", subservers);
        JSONObject bungee = new JSONObject();
        bungee.put("version", plugin.api.getProxyVersion());
        bungee.put("servers", plugin.api.getServers().size());
        json.put("bungee", bungee);
        JSONObject minecraft = new JSONObject();
        minecraft.put("version", plugin.api.getGameVersion());
        minecraft.put("players", plugin.getPlayers().size());
        json.put("minecraft", minecraft);
        JSONObject system = new JSONObject();
        JSONObject os = new JSONObject();
        os.put("name", System.getProperty("os.name"));
        os.put("version", System.getProperty("os.version"));
        system.put("os", os);
        JSONObject java = new JSONObject();
        java.put("version",  System.getProperty("java.version"));
        system.put("java", java);
        json.put("system", system);
        return json;
    }

    @Override
    public void execute(Client client, JSONObject data) {
        client.sendPacket(new PacketDownloadProxyInfo(plugin, (data != null && data.keySet().contains("id"))?data.getString("id"):null));
    }

    @Override
    public Version getVersion() {
        return new Version("2.11.0a");
    }
}

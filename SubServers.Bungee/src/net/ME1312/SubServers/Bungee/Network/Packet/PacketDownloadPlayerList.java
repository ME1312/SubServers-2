package net.ME1312.SubServers.Bungee.Network.Packet;

import net.ME1312.SubServers.Bungee.Library.Version.Version;
import net.ME1312.SubServers.Bungee.Network.Client;
import net.ME1312.SubServers.Bungee.Network.PacketIn;
import net.ME1312.SubServers.Bungee.Network.PacketOut;
import net.ME1312.SubServers.Bungee.SubPlugin;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.json.JSONObject;

public class PacketDownloadPlayerList implements PacketIn, PacketOut {
    private SubPlugin plugin;
    private String id;

    public PacketDownloadPlayerList(SubPlugin plugin) {
        this.plugin = plugin;
    }
    public PacketDownloadPlayerList(SubPlugin plugin, String id) {
        this.plugin = plugin;
        this.id = id;
    }

    @Override
    public JSONObject generate() {
        JSONObject json = new JSONObject();
        json.put("id", id);
        JSONObject players = new JSONObject();
        for (ProxiedPlayer player : plugin.getPlayers()) {
            JSONObject pinfo = new JSONObject();
            pinfo.put("name", player.getName());
            pinfo.put("nick", player.getDisplayName());
            pinfo.put("server", player.getServer().getInfo().getName());
            players.put(player.getUniqueId().toString(), pinfo);
        }
        json.put("players", players);
        return json;
    }

    @Override
    public void execute(Client client, JSONObject data) {
        client.sendPacket(new PacketDownloadPlayerList(plugin, (data != null && data.keySet().contains("id"))?data.getString("id"):null));
    }

    @Override
    public Version getVersion() {
        return new Version("2.11.0a");
    }
}

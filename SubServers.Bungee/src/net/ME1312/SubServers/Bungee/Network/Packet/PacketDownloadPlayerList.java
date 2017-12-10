package net.ME1312.SubServers.Bungee.Network.Packet;

import net.ME1312.SubServers.Bungee.Library.NamedContainer;
import net.ME1312.SubServers.Bungee.Library.Util;
import net.ME1312.SubServers.Bungee.Library.Version.Version;
import net.ME1312.SubServers.Bungee.Network.Client;
import net.ME1312.SubServers.Bungee.Network.PacketIn;
import net.ME1312.SubServers.Bungee.Network.PacketOut;
import net.ME1312.SubServers.Bungee.SubPlugin;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.json.JSONObject;

import java.util.UUID;

/**
 * Download Player List Packet
 */
public class PacketDownloadPlayerList implements PacketIn, PacketOut {
    private SubPlugin plugin;
    private String id;

    /**
     * New PacketDownloadPlayerList (In)
     *
     * @param plugin SubPlugin
     */
    public PacketDownloadPlayerList(SubPlugin plugin) {
        if (Util.isNull(plugin)) throw new NullPointerException();
        this.plugin = plugin;
    }

    /**
     * New PacketDownloadPlayerList (Out)
     *
     * @param plugin SubPlugin
     * @param id Receiver ID
     */
    public PacketDownloadPlayerList(SubPlugin plugin, String id) {
        if (Util.isNull(plugin)) throw new NullPointerException();
        this.plugin = plugin;
        this.id = id;
    }

    @Override
    @SuppressWarnings("unchecked")
    public JSONObject generate() {
        JSONObject json = new JSONObject();
        json.put("id", id);
        JSONObject players = new JSONObject();
        for (NamedContainer<String, UUID> player : plugin.api.getGlobalPlayers()) {
            JSONObject pinfo = new JSONObject();
            pinfo.put("name", player.get());
            if (plugin.redis) {
                try {
                    pinfo.put("server", ((ServerInfo) plugin.redis("getServerFor", new NamedContainer<>(UUID.class, player.get()))).getName());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                pinfo.put("server", plugin.getPlayer(player.get()).getServer().getInfo().getName());
            }
            players.put(player.get().toString(), pinfo);
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

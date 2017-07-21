package net.ME1312.SubServers.Bungee.Network.Packet;

import net.ME1312.SubServers.Bungee.Host.Server;
import net.ME1312.SubServers.Bungee.Host.SubServer;
import net.ME1312.SubServers.Bungee.Library.Util;
import net.ME1312.SubServers.Bungee.Library.Version.Version;
import net.ME1312.SubServers.Bungee.Network.Client;
import net.ME1312.SubServers.Bungee.Network.PacketIn;
import net.ME1312.SubServers.Bungee.Network.PacketOut;
import net.ME1312.SubServers.Bungee.SubPlugin;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Download Server Info Packet
 */
public class PacketDownloadServerInfo implements PacketIn, PacketOut {
    private SubPlugin plugin;
    private Server server;
    private String id;

    /**
     * New PacketDownloadServerInfo (In)
     *
     * @param plugin SubPlugin
     */
    public PacketDownloadServerInfo(SubPlugin plugin) {
        if (Util.isNull(plugin)) throw new NullPointerException();
        this.plugin = plugin;
    }

    /**
     * New PacketDownloadServerInfo (Out)
     *
     * @param plugin SubPlugin
     * @param server Server
     * @param id Receiver ID
     */
    public PacketDownloadServerInfo(SubPlugin plugin, Server server, String id) {
        if (Util.isNull(plugin)) throw new NullPointerException();
        this.plugin = plugin;
        this.server = server;
        this.id = id;
    }

    @Override
    public JSONObject generate() {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("type", (server == null)?"invalid":((server instanceof SubServer)?"subserver":"server"));
        JSONObject info = new JSONObject();

        if (server != null && server instanceof SubServer) {
            info.put("host", ((SubServer) server).getHost().getName());
            info.put("enabled", ((SubServer) server).isEnabled() && ((SubServer) server).getHost().isEnabled());
            info.put("log", ((SubServer) server).isLogging());
            info.put("dir", ((SubServer) server).getPath());
            info.put("exec", ((SubServer) server).getExecutable());
            info.put("running", ((SubServer) server).isRunning());
            info.put("stop-cmd", ((SubServer) server).getStopCommand());
            info.put("auto-run", plugin.config.get().getSection("Servers").getSection(server.getName()).getKeys().contains("Run-On-Launch") && plugin.config.get().getSection("Servers").getSection(server.getName()).getBoolean("Run-On-Launch"));
            info.put("auto-restart", ((SubServer) server).willAutoRestart());
            List<String> incompatibleCurrent = new ArrayList<String>();
            List<String> incompatible = new ArrayList<String>();
            for (SubServer server : ((SubServer) this.server).getCurrentIncompatibilities()) incompatibleCurrent.add(server.getName());
            for (SubServer server : ((SubServer) this.server).getIncompatibilities()) incompatible.add(server.getName());
            info.put("incompatible", incompatibleCurrent);
            info.put("incompatible-list", incompatible);
            info.put("temp", ((SubServer) server).isTemporary());
        } if (server != null) {
            info.put("name", server.getName());
            info.put("display", server.getDisplayName());
            info.put("address", server.getAddress().toString());
            info.put("restricted", server.isRestricted());
            info.put("hidden", server.isHidden());
            info.put("motd", server.getMotd());
            if (server.getSubData() != null) info.put("subdata", server.getSubData().getAddress().toString());
            info.put("extra", server.getExtra().toJSON());

            JSONObject players = new JSONObject();
            for (ProxiedPlayer player : server.getPlayers()) {
                JSONObject pinfo = new JSONObject();
                pinfo.put("name", player.getName());
                pinfo.put("nick", player.getDisplayName());
                players.put(player.getUniqueId().toString(), pinfo);
            }
            info.put("players", players);
        }

        json.put("server", info);
        return json;
    }

    @Override
    public void execute(Client client, JSONObject data) {
        client.sendPacket(new PacketDownloadServerInfo(plugin, plugin.api.getServer(data.getString("server")), (data.keySet().contains("id"))?data.getString("id"):null));
    }

    @Override
    public Version getVersion() {
        return new Version("2.11.0a");
    }
}

package net.ME1312.SubServers.Bungee.Network.Packet;

import net.ME1312.SubServers.Bungee.Host.Server;
import net.ME1312.SubServers.Bungee.Host.SubServer;
import net.ME1312.SubServers.Bungee.Library.Version.Version;
import net.ME1312.SubServers.Bungee.Network.Client;
import net.ME1312.SubServers.Bungee.Network.PacketIn;
import net.ME1312.SubServers.Bungee.Network.PacketOut;
import net.ME1312.SubServers.Bungee.SubPlugin;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.json.JSONObject;

public class PacketDownloadServerInfo implements PacketIn, PacketOut {
    private SubPlugin plugin;
    private Server server;
    private String id;

    public PacketDownloadServerInfo(SubPlugin plugin) {
        this.plugin = plugin;
    }
    public PacketDownloadServerInfo(SubPlugin plugin, Server server, String id) {
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
            info.put("editable", ((SubServer) server).isEditable());
            info.put("log", ((SubServer) server).isLogging());
            info.put("dir", plugin.config.get().getSection("Servers").getSection(server.getName()).getString("Directory"));
            info.put("exec", plugin.config.get().getSection("Servers").getSection(server.getName()).getString("Executable"));
            info.put("running", ((SubServer) server).isRunning());
            info.put("stop-cmd", ((SubServer) server).getStopCommand());
            info.put("auto-run", plugin.config.get().getSection("Servers").getSection(server.getName()).getBoolean("Run-On-Launch"));
            info.put("auto-restart", ((SubServer) server).willAutoRestart());
            info.put("temp", ((SubServer) server).isTemporary());
        } if (server != null) {
            info.put("name", server.getName());
            info.put("address", server.getAddress().toString());
            info.put("restricted", server.isRestricted());
            info.put("hidden", server.isHidden());
            info.put("motd", server.getMotd());
            info.put("subdata", server.getSubDataClient() == null);

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

package net.ME1312.SubServers.Proxy.Network.Packet;

import net.ME1312.SubServers.Proxy.Host.Host;
import net.ME1312.SubServers.Proxy.Host.SubServer;
import net.ME1312.SubServers.Proxy.Library.Version.Version;
import net.ME1312.SubServers.Proxy.Network.Client;
import net.ME1312.SubServers.Proxy.Network.PacketIn;
import net.ME1312.SubServers.Proxy.Network.PacketOut;

import net.ME1312.SubServers.Proxy.SubPlugin;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.json.JSONObject;

public class PacketDownloadHostInfo implements PacketIn, PacketOut {
    private SubPlugin plugin;
    private Host host;
    private String id;

    public PacketDownloadHostInfo(SubPlugin plugin) {
        this.plugin = plugin;
    }
    public PacketDownloadHostInfo(SubPlugin plugin, Host host, String id) {
        this.plugin = plugin;
        this.host = host;
        this.id = id;
    }

    @Override
    public JSONObject generate() {
        JSONObject json = new JSONObject();
        json.put("id", id);
        JSONObject info = new JSONObject();

        if (host != null) {
            json.put("valid", true);

            info.put("name", host.getName());
            info.put("enabled", host.isEnabled());
            info.put("editable", host.isEditable());
            info.put("address", host.getAddress().toString());
            info.put("dir", host.getDirectory());

            JSONObject cinfo = new JSONObject();
            cinfo.put("busy", host.getCreator().isBusy());
            cinfo.put("git-bash", host.getCreator().getGitBashDirectory());
            info.put("creator", cinfo);

            JSONObject servers = new JSONObject();
            for (SubServer server : host.getSubServers().values()) {
                JSONObject sinfo = new JSONObject();
                sinfo.put("enabled", server.isEnabled());
                sinfo.put("running", server.isRunning());
                sinfo.put("temp", server.isTemporary());
                JSONObject players = new JSONObject();
                for (ProxiedPlayer player : server.getPlayers()) {
                    JSONObject pinfo = new JSONObject();
                    pinfo.put("name", player.getName());
                    pinfo.put("nick", player.getDisplayName());
                    players.put(player.getUniqueId().toString(), pinfo);
                }
                sinfo.put("players", players);
                servers.put(server.getName(), sinfo);
            }
            info.put("servers", servers);
        } else json.put("valid", false);

        json.put("host", info);
        return json;
    }

    @Override
    public void execute(Client client, JSONObject data) {
        client.sendPacket(new PacketDownloadHostInfo(plugin, plugin.api.getHost(data.getString("host")), (data.keySet().contains("id"))?data.getString("id"):null));
    }

    @Override
    public Version getVersion() {
        return new Version("2.11.0a");
    }
}

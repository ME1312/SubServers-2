package net.ME1312.SubServers.Proxy.Network.Packet;

import net.ME1312.SubServers.Proxy.Host.Server;
import net.ME1312.SubServers.Proxy.Host.SubServer;
import net.ME1312.SubServers.Proxy.Libraries.Version.Version;
import net.ME1312.SubServers.Proxy.Network.Client;
import net.ME1312.SubServers.Proxy.Network.PacketIn;
import net.ME1312.SubServers.Proxy.Network.PacketOut;
import net.ME1312.SubServers.Proxy.SubPlugin;
import org.json.JSONObject;

public class PacketRequestServerInfo implements PacketIn, PacketOut {
    private SubPlugin plugin;
    private Server server;

    public PacketRequestServerInfo(SubPlugin plugin, Server server) {
        this.server = server;
    }

    public PacketRequestServerInfo(SubPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public JSONObject generate() {
        JSONObject json = new JSONObject();
        JSONObject info = new JSONObject();
        json.put("type", (server == null)?"invalid":((server instanceof SubServer)?"subserver":"server"));

        if (server != null && server instanceof SubServer) {
            info.put("host", ((SubServer) server).getHost().getName());
            info.put("enabled", ((SubServer) server).isEnabled());
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
            info.put("motd", server.getMotd());
            info.put("subdata", server.getSubDataClient() == null);
        }

        json.put("server", info);
        return json;
    }

    @Override
    public void execute(Client client, JSONObject data) {
        client.sendPacket(new PacketRequestServerInfo(plugin, plugin.api.getServer(data.getString("server"))));
    }

    @Override
    public Version getVersion() {
        return new Version("2.11.0a");
    }
}

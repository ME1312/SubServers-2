package net.ME1312.SubServers.Sync.Network.Packet;

import net.ME1312.SubServers.Sync.Library.Util;
import net.ME1312.SubServers.Sync.Library.Version.Version;
import net.ME1312.SubServers.Sync.Network.PacketIn;
import net.ME1312.SubServers.Sync.Network.PacketOut;
import net.ME1312.SubServers.Sync.Server.Server;
import net.ME1312.SubServers.Sync.Server.SubServer;
import net.ME1312.SubServers.Sync.SubPlugin;

import org.json.JSONObject;

import java.io.IOException;
import java.net.InetSocketAddress;

public final class PacketAuthorization implements PacketIn, PacketOut {
    private SubPlugin plugin;

    public PacketAuthorization(SubPlugin plugin) {
        if (Util.isNull(plugin)) throw new NullPointerException();
        this.plugin = plugin;
    }

    @Override
    public JSONObject generate() {
        JSONObject json = new JSONObject();
        json.put("password", plugin.config.get().getSection("Settings").getSection("SubData").getString("Password"));
        return json;
    }

    @Override
    public void execute(JSONObject data) {
        try {
            if (data.getInt("r") == 0) {
                plugin.subdata.sendPacket(new PacketDownloadLang(plugin));
                plugin.subdata.sendPacket(new PacketDownloadServerList(null, json -> {
                    System.out.println("SubServers > Resetting Server Data");
                    plugin.servers.clear();
                    for (String host : json.getJSONObject("hosts").keySet()) {
                        for (String subserver : json.getJSONObject("hosts").getJSONObject(host).getJSONObject("servers").keySet()) {
                            plugin.servers.put(subserver.toLowerCase(), new SubServer(subserver, json.getJSONObject("hosts").getJSONObject(host).getJSONObject("servers").getJSONObject(subserver).getString("display"),
                                    new InetSocketAddress(json.getJSONObject("hosts").getJSONObject(host).getJSONObject("servers").getJSONObject(subserver).getString("address").split(":")[0], Integer.parseInt(json.getJSONObject("hosts").getJSONObject(host).getJSONObject("servers").getJSONObject(subserver).getString("address").split(":")[1])),
                                    json.getJSONObject("hosts").getJSONObject(host).getJSONObject("servers").getJSONObject(subserver).getString("motd"), json.getJSONObject("hosts").getJSONObject(host).getJSONObject("servers").getJSONObject(subserver).getBoolean("hidden"),
                                    json.getJSONObject("hosts").getJSONObject(host).getJSONObject("servers").getJSONObject(subserver).getBoolean("restricted"), json.getJSONObject("hosts").getJSONObject(host).getJSONObject("servers").getJSONObject(subserver).getBoolean("running")));
                            System.out.println("SubServers > Added SubServer: " + subserver);
                        }
                    }
                    for (String server : json.getJSONObject("servers").keySet()) {
                        plugin.servers.put(server.toLowerCase(), new Server(server, json.getJSONObject("servers").getJSONObject(server).getString("display"), new InetSocketAddress(json.getJSONObject("servers").getJSONObject(server).getString("address").split(":")[0], Integer.parseInt(json.getJSONObject("servers").getJSONObject(server).getString("address").split(":")[1])),
                                json.getJSONObject("servers").getJSONObject(server).getString("motd"), json.getJSONObject("servers").getJSONObject(server).getBoolean("hidden"), json.getJSONObject("servers").getJSONObject(server).getBoolean("restricted")));
                        System.out.println("SubServers > Added Server: " + server);
                    }
                }));
            } else {
                System.out.println("SubServers > Could not authorize SubData connection: " + data.getString("m"));
                plugin.subdata.destroy(0);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Version getVersion() {
        return new Version("2.11.0a");
    }
}

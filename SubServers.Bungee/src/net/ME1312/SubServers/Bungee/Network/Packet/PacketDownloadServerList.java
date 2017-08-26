package net.ME1312.SubServers.Bungee.Network.Packet;

import net.ME1312.SubServers.Bungee.Host.Host;
import net.ME1312.SubServers.Bungee.Host.Server;
import net.ME1312.SubServers.Bungee.Host.SubServer;
import net.ME1312.SubServers.Bungee.Library.Util;
import net.ME1312.SubServers.Bungee.Library.Version.Version;
import net.ME1312.SubServers.Bungee.Network.Client;
import net.ME1312.SubServers.Bungee.Network.ClientHandler;
import net.ME1312.SubServers.Bungee.Network.PacketIn;
import net.ME1312.SubServers.Bungee.Network.PacketOut;
import net.ME1312.SubServers.Bungee.SubPlugin;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Download Server List Packet
 */
public class PacketDownloadServerList implements PacketIn, PacketOut {
    private SubPlugin plugin;
    private String host;
    private String group;
    private String id;

    /**
     * New PacketDownloadServerList (In)
     *
     * @param plugin SubPlugin
     */
    public PacketDownloadServerList(SubPlugin plugin) {
        if (Util.isNull(plugin)) throw new NullPointerException();
        this.plugin = plugin;
    }

    /**
     * New PacketDownloadServerList (Out)
     *
     * @param plugin SubPlugin
     * @param host Host (or null for all)
     * @param group Group (or null for all)
     * @param id Receiver ID
     */
    public PacketDownloadServerList(SubPlugin plugin, String host, String group, String id) {
        if (Util.isNull(plugin)) throw new NullPointerException();
        this.plugin = plugin;
        this.host = host;
        this.group = group;
        this.id = id;
    }

    @Override
    public JSONObject generate() {
        JSONObject json = new JSONObject();
        json.put("id", id);

        JSONObject exServers = new JSONObject();
        for (Server server : plugin.exServers.values()) {
            exServers.put(server.getName(), new JSONObject(server.toString()));
        }
        json.put("servers", exServers);

        if (this.host == null || !this.host.equals("")) {
            JSONObject hosts = new JSONObject();
            for (Host host : plugin.api.getHosts().values()) {
                if (this.host == null || this.host.equalsIgnoreCase(host.getName())) {
                    hosts.put(host.getName(), new JSONObject(host.toString()));
                }
            }
            json.put("hosts", hosts);
        }

        if (this.group == null || !this.group.equals("")) {
            JSONObject groups = new JSONObject();
            for (String group : plugin.api.getGroups().keySet()) {
                if (this.group == null || this.group.equalsIgnoreCase(group)) {
                    JSONObject servers = new JSONObject();
                    for (Server server : plugin.api.getGroup(group)) {
                        servers.put(server.getName(), new JSONObject(server.toString()));
                    }
                    groups.put(group, servers);
                }
            }
            json.put("groups", groups);
        }
        return json;
    }

    @Override
    public void execute(Client client, JSONObject data) {
        client.sendPacket(new PacketDownloadServerList(plugin, (data.keySet().contains("host"))?data.getString("host"):null, (data.keySet().contains("group"))?data.getString("group"):null, (data.keySet().contains("id"))?data.getString("id"):null));
    }

    @Override
    public Version getVersion() {
        return new Version("2.11.0a");
    }
}

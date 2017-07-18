package net.ME1312.SubServers.Bungee.Network.Packet;

import net.ME1312.SubServers.Bungee.Library.Util;
import net.ME1312.SubServers.Bungee.Library.Version.Version;
import net.ME1312.SubServers.Bungee.Network.Client;
import net.ME1312.SubServers.Bungee.Network.PacketIn;
import net.ME1312.SubServers.Bungee.Network.PacketOut;
import net.ME1312.SubServers.Bungee.SubPlugin;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Download Network List Packet
 */
public class PacketDownloadNetworkList implements PacketIn, PacketOut {
    private SubPlugin plugin;
    private String id;

    /**
     * New PacketDownloadNetworkList (In)
     *
     * @param plugin SubPlugin
     */
    public PacketDownloadNetworkList(SubPlugin plugin) {
        if (Util.isNull(plugin)) throw new NullPointerException();
        this.plugin = plugin;
    }

    /**
     * New PacketDownloadNetworkList (Out)
     *
     * @param plugin SubPlugin
     * @param id Receiver ID
     */
    public PacketDownloadNetworkList(SubPlugin plugin, String id) {
        if (Util.isNull(plugin)) throw new NullPointerException();
        this.plugin = plugin;
        this.id = id;
    }

    @Override
    public JSONObject generate() {
        JSONObject json = new JSONObject();
        json.put("id", id);
        JSONObject clients = new JSONObject();
        for (Client client : plugin.subdata.getClients()) {
            try {
                clients.put(client.getAddress().toString(), new JSONObject(client.getHandler().toString()));
            } catch (JSONException | NullPointerException e) {
                clients.put(client.getAddress().toString(), new JSONObject());
            }
        }
        json.put("clients", clients);
        return json;
    }

    @Override
    public void execute(Client client, JSONObject data) {
        client.sendPacket(new PacketDownloadNetworkList(plugin, (data.keySet().contains("id"))?data.getString("id"):null));
    }

    @Override
    public Version getVersion() {
        return new Version("2.11.0a");
    }
}

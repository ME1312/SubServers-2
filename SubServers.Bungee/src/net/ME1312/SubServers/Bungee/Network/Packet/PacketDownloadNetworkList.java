package net.ME1312.SubServers.Bungee.Network.Packet;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import net.ME1312.SubServers.Bungee.Library.Config.YAMLSection;
import net.ME1312.SubServers.Bungee.Library.Util;
import net.ME1312.SubServers.Bungee.Library.Version.Version;
import net.ME1312.SubServers.Bungee.Network.Client;
import net.ME1312.SubServers.Bungee.Network.PacketIn;
import net.ME1312.SubServers.Bungee.Network.PacketOut;
import net.ME1312.SubServers.Bungee.SubPlugin;

import java.util.Map;

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

    @SuppressWarnings("unchecked")
    @Override
    public YAMLSection generate() {
        YAMLSection json = new YAMLSection();
        if (id != null) json.set("id", id);
        YAMLSection clients = new YAMLSection();
        for (Client client : plugin.subdata.getClients()) {
            try {
                clients.set(client.getAddress().toString(), new YAMLSection(new Gson().fromJson(client.getHandler().toString(), Map.class)));
            } catch (JsonParseException | NullPointerException e) {
                clients.set(client.getAddress().toString(), new YAMLSection());
            }
        }
        json.set("clients", clients);
        return json;
    }

    @Override
    public void execute(Client client, YAMLSection data) {
        client.sendPacket(new PacketDownloadNetworkList(plugin, (data.contains("id"))?data.getRawString("id"):null));
    }

    @Override
    public Version getVersion() {
        return new Version("2.11.0a");
    }
}

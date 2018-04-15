package net.ME1312.SubServers.Bungee.Network.Packet;

import net.ME1312.SubServers.Bungee.Library.Config.YAMLSection;
import net.ME1312.SubServers.Bungee.Library.NamedContainer;
import net.ME1312.SubServers.Bungee.Library.Util;
import net.ME1312.SubServers.Bungee.Library.Version.Version;
import net.ME1312.SubServers.Bungee.Network.Client;
import net.ME1312.SubServers.Bungee.Network.PacketIn;
import net.ME1312.SubServers.Bungee.Network.PacketOut;
import net.ME1312.SubServers.Bungee.SubPlugin;
import net.md_5.bungee.api.config.ServerInfo;

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
    public YAMLSection generate() {
        YAMLSection data = new YAMLSection();
        data.set("id", id);
        YAMLSection players = new YAMLSection();
        for (NamedContainer<String, UUID> player : plugin.api.getGlobalPlayers()) {
            YAMLSection pinfo = new YAMLSection();
            pinfo.set("name", player.get());
            if (plugin.redis) {
                try {
                    pinfo.set("server", ((ServerInfo) plugin.redis("getServerFor", new NamedContainer<>(UUID.class, player.get()))).getName());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                pinfo.set("server", plugin.getPlayer(player.get()).getServer().getInfo().getName());
            }
            players.set(player.get().toString(), pinfo);
        }
        data.set("players", players);
        return data;
    }

    @Override
    public void execute(Client client, YAMLSection data) {
        client.sendPacket(new PacketDownloadPlayerList(plugin, (data != null && data.contains("id"))?data.getRawString("id"):null));
    }

    @Override
    public Version getVersion() {
        return new Version("2.11.0a");
    }
}

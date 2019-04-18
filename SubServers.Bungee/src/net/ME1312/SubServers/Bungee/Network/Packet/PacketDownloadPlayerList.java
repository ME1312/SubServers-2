package net.ME1312.SubServers.Bungee.Network.Packet;

import net.ME1312.SubData.Server.SubDataClient;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.NamedContainer;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Server.Protocol.PacketObjectIn;
import net.ME1312.SubData.Server.Protocol.PacketObjectOut;
import net.ME1312.SubServers.Bungee.SubPlugin;
import net.md_5.bungee.api.config.ServerInfo;

import java.util.UUID;

/**
 * Download Player List Packet
 */
public class PacketDownloadPlayerList implements PacketObjectIn<Integer>, PacketObjectOut<Integer> {
    private SubPlugin plugin;
    private UUID tracker;

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
     * @param tracker Receiver ID
     */
    public PacketDownloadPlayerList(SubPlugin plugin, UUID tracker) {
        if (Util.isNull(plugin)) throw new NullPointerException();
        this.plugin = plugin;
        this.tracker = tracker;
    }

    @SuppressWarnings("unchecked")
    @Override
    public ObjectMap<Integer> send(SubDataClient client) {
        ObjectMap<Integer> data = new ObjectMap<Integer>();
        if (tracker != null) data.set(0x0000, tracker);
        ObjectMap<String> players = new ObjectMap<String>();
        for (NamedContainer<String, UUID> player : plugin.api.getGlobalPlayers()) {
            ObjectMap<String> pinfo = new ObjectMap<String>();
            pinfo.set("name", player.get());
            if (plugin.redis != null) {
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
        data.set(0x0001, players);
        return data;
    }

    @Override
    public void receive(SubDataClient client, ObjectMap<Integer> data) {
        client.sendPacket(new PacketDownloadPlayerList(plugin, (data.contains(0x0000))?data.getUUID(0x0000):null));
    }

    @Override
    public int version() {
        return 0x0001;
    }
}

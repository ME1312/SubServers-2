package net.ME1312.SubServers.Client.Sponge.Network.Packet;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Client.Protocol.Initial.InitialPacket;
import net.ME1312.SubData.Client.Protocol.PacketObjectIn;
import net.ME1312.SubData.Client.Protocol.PacketObjectOut;
import net.ME1312.SubData.Client.SubDataClient;
import net.ME1312.SubData.Client.SubDataSender;
import net.ME1312.SubServers.Client.Sponge.SubAPI;
import net.ME1312.SubServers.Client.Sponge.SubPlugin;

import java.util.logging.Logger;

/**
 * Link Server Packet
 */
public class PacketLinkServer implements InitialPacket, PacketObjectIn<Integer>, PacketObjectOut<Integer> {
    private SubPlugin plugin;
    private int channel;

    /**
     * New PacketLinkServer (In)
     *
     * @param plugin SubServers.Client
     */
    public PacketLinkServer(SubPlugin plugin) {
        if (Util.isNull(plugin)) throw new NullPointerException();
        this.plugin = plugin;
    }
    /**
     * New PacketLinkServer (Out)
     *
     * @param plugin SubServers.Client
     * @param channel Channel ID
     */
    public PacketLinkServer(SubPlugin plugin, int channel) {
        if (Util.isNull(plugin)) throw new NullPointerException();
        this.plugin = plugin;
        this.channel = channel;
    }

    @Override
    public ObjectMap<Integer> send(SubDataSender client) {
        ObjectMap<Integer> json = new ObjectMap<Integer>();
        if (plugin.api.getName() != null) json.set(0x0000, plugin.api.getName());
        if (plugin.game.getServer().getBoundAddress().isPresent()) json.set(0x0001, plugin.game.getServer().getBoundAddress().get().getPort());
        json.set(0x0002, channel);
        return json;
    }

    @Override
    public void receive(SubDataSender client, ObjectMap<Integer> data) {
        Logger log = Util.getDespiteException(() -> Util.reflect(SubDataClient.class.getDeclaredField("log"), client), null);
        if (data.getInt(0x0001) == 0) {
            try {
                if (data.contains(0x0000)) Util.reflect(SubAPI.class.getDeclaredField("name"), plugin.api, data.getRawString(0x0000));
                setReady(client.getConnection(), true);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        } else {
            log.info("Could not link name with server" + ((data.contains(0x0002))?": "+data.getRawString(0x0002):'.'));
            try {
                if (data.getInt(0x0001) == 2) {
                    if (!plugin.config.get().getMap("Settings").getMap("SubData").contains("Name")) {
                        plugin.config.get().getMap("Settings").getMap("SubData").set("Name", "");
                        plugin.config.save();
                    }
                    if (plugin.config.get().getMap("Settings").getMap("SubData").getRawString("Name").length() <= 0)
                        log.info("Use the server \"Name\" option to override auto-linking");
                }
            } catch (Exception e) {}
            new IllegalStateException().printStackTrace();
            plugin.disable(null);
        }
    }

    @Override
    public int version() {
        return 0x0001;
    }
}

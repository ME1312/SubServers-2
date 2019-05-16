package net.ME1312.SubServers.Sync.Network.Packet;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Client.Protocol.Initial.InitialPacket;
import net.ME1312.SubData.Client.Protocol.PacketObjectIn;
import net.ME1312.SubData.Client.Protocol.PacketObjectOut;
import net.ME1312.SubData.Client.SubDataClient;
import net.ME1312.SubServers.Sync.Library.Compatibility.Logger;
import net.ME1312.SubServers.Sync.SubAPI;
import net.ME1312.SubServers.Sync.SubPlugin;

/**
 * Link Proxy Packet
 */
public class PacketLinkProxy implements InitialPacket, PacketObjectIn<Integer>, PacketObjectOut<Integer> {
    private SubPlugin plugin;
    private int channel;

    /**
     * New PacketLinkProxy (In)
     *
     * @param plugin SubServers.Client
     */
    public PacketLinkProxy(SubPlugin plugin) {
        if (Util.isNull(plugin)) throw new NullPointerException();
        this.plugin = plugin;
    }
    /**
     * New PacketLinkProxy (Out)
     *
     * @param plugin SubServers.Client
     * @param channel Channel ID
     */
    public PacketLinkProxy(SubPlugin plugin, int channel) {
        if (Util.isNull(plugin)) throw new NullPointerException();
        this.plugin = plugin;
        this.channel = channel;
    }

    @Override
    public ObjectMap<Integer> send(SubDataClient client) {
        ObjectMap<Integer> json = new ObjectMap<Integer>();
        json.set(0x0000, plugin.api.getName());
        json.set(0x0001, channel);
        return json;
    }

    @Override
    public void receive(SubDataClient client, ObjectMap<Integer> data) {
        if (data.getInt(0x0001) == 0) {
            try {
                if (data.contains(0x0000)) Util.reflect(SubAPI.class.getDeclaredField("name"), plugin.api, data.getRawString(0x0000));
                setReady(client, true);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        } else {
            Logger.get("SubData").info("Could not link name with proxy" + ((data.contains(0x0002))?": "+data.getRawString(0x0002):'.'));
            try {
                if (data.getInt(0x0001) == 2) {
                    if (!plugin.config.get().getMap("Settings").getMap("SubData").contains("Name")) {
                        plugin.config.get().getMap("Settings").getMap("SubData").set("Name", "");
                        plugin.config.save();
                    }
                    if (plugin.config.get().getMap("Settings").getMap("SubData").getRawString("Name").length() <= 0)
                        Logger.get("SubData").info("Use the proxy \"Name\" option to override auto-linking");
                }
            } catch (Exception e) {}
            new IllegalStateException().printStackTrace();
        }
    }

    @Override
    public int version() {
        return 0x0001;
    }
}

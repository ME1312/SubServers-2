package net.ME1312.SubServers.Client.Sponge.Network.Packet;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Client.Protocol.PacketObjectIn;
import net.ME1312.SubData.Client.SubDataClient;
import net.ME1312.SubData.Client.SubDataSender;
import net.ME1312.SubServers.Client.Sponge.SubPlugin;

import org.spongepowered.api.Sponge;

import java.util.logging.Logger;

/**
 * Reload Packet
 */
public class PacketInExReload implements PacketObjectIn<Integer> {
    private SubPlugin plugin;

    /**
     * New PacketInExReload
     *
     * @param plugin Plugin
     */
    public PacketInExReload(SubPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void receive(SubDataSender client, ObjectMap<Integer> data) {
        Logger log = Util.getDespiteException(() -> Util.reflect(SubDataClient.class.getDeclaredField("log"), client.getConnection()), null);
        if (data != null && data.contains(0x0000)) log.warning("Received request for a plugin reload: " + data.getString(0x0000));
        else log.warning("Received request for a plugin reload");
        Sponge.getScheduler().createTaskBuilder().async().execute(() -> {
            try {
                plugin.reload(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).submit(plugin);
    }

    @Override
    public int version() {
        return 0x0001;
    }
}

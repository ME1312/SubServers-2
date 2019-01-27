package net.ME1312.SubServers.Client.Bukkit.Network.Packet;

import net.ME1312.SubServers.Client.Bukkit.Library.Callback;
import net.ME1312.SubServers.Client.Bukkit.Library.Config.YAMLSection;
import net.ME1312.SubServers.Client.Bukkit.Library.Util;
import net.ME1312.SubServers.Client.Bukkit.Library.Version.Version;
import net.ME1312.SubServers.Client.Bukkit.Network.PacketIn;
import net.ME1312.SubServers.Client.Bukkit.Network.PacketOut;
import net.ME1312.SubServers.Client.Bukkit.Network.SubDataClient;
import net.ME1312.SubServers.Client.Bukkit.SubAPI;
import net.ME1312.SubServers.Client.Bukkit.SubPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.EventExecutor;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Link Server Packet
 */
public class PacketLinkServer implements PacketIn, PacketOut, Listener {
    private SubPlugin plugin;

    /**
     * New PacketLinkServer
     *
     * @param plugin SubServers.Client
     */
    public PacketLinkServer(SubPlugin plugin) {
        if (Util.isNull(plugin)) throw new NullPointerException();
        this.plugin = plugin;
    }

    @Override
    public YAMLSection generate() {
        YAMLSection json = new YAMLSection();
        if (plugin.subdata.getName() != null) json.set("name", plugin.subdata.getName());
        json.set("port", Bukkit.getServer().getPort());
        return json;
    }

    @Override
    public void execute(YAMLSection data) {
        if (data.getInt("r") == 0) {
            try {
                if (data.contains("n")) {
                    Util.reflect(SubDataClient.class.getDeclaredField("name"), plugin.subdata, data.getRawString("n"));
                }
                Util.reflect(SubDataClient.class.getDeclaredMethod("init"), plugin.subdata);
            } catch (Exception e) {}
        } else {
            Bukkit.getLogger().info("SubData > Could not link name with server: " + data.getRawString("m"));
            try {
                if (data.getInt("r") == 2) {
                    if (!plugin.config.get().getSection("Settings").getSection("SubData").contains("Name")) {
                        plugin.config.get().getSection("Settings").getSection("SubData").set("Name", "");
                        plugin.config.save();
                    }
                    if (plugin.config.get().getSection("Settings").getSection("SubData").getRawString("Name").length() <= 0)
                        Bukkit.getLogger().info("SubData > Use the server \"Name\" option to override auto-linking");
                }
            } catch (Exception e) {}
            plugin.onDisable();
        }
    }

    @Override
    public Version getVersion() {
        return new Version("2.11.0a");
    }
}

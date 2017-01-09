package net.ME1312.SubServers.Console;

import net.ME1312.SubServers.Bungee.Event.SubStartEvent;
import net.ME1312.SubServers.Bungee.Event.SubStoppedEvent;
import net.ME1312.SubServers.Bungee.Library.Config.YAMLConfig;
import net.ME1312.SubServers.Bungee.SubPlugin;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;

public class ConsolePlugin extends Plugin implements Listener {
    public HashMap<String, ConsoleWindow> current = new HashMap<String, ConsoleWindow>();
    public YAMLConfig config;

    @Override
    public void onEnable() {
        try {
            getDataFolder().mkdirs();
            config = new YAMLConfig(new File(getDataFolder(), "config.yml"));
            if (!config.get().getKeys().contains("Enabled-Servers")) {
                config.get().set("Enabled-Servers", Collections.emptyList());
                config.save();
            }

            getProxy().getPluginManager().registerListener(this, this);
            getProxy().getPluginManager().registerCommand(this, new PopoutCommand(this, "popout"));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onServerStart(SubStartEvent event) {
        if (!event.isCancelled() && config.get().getStringList("Enabled-Servers").contains(event.getServer().getName().toLowerCase())) {
            ConsoleWindow window = new ConsoleWindow(event.getServer());
            current.put(event.getServer().getName().toLowerCase(), window);
            window.open();
        }
    }

    @EventHandler
    public void onServerStop(SubStoppedEvent event) {
        if (current.keySet().contains(event.getServer().getName().toLowerCase())) {
            current.get(event.getServer().getName().toLowerCase()).close();
            current.remove(event.getServer().getName().toLowerCase());
        }
    }

    @Override
    public SubPlugin getProxy() {
        return (SubPlugin) super.getProxy();
    }
}

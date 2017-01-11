package net.ME1312.SubServers.Console;

import net.ME1312.SubServers.Bungee.Event.SubCreateEvent;
import net.ME1312.SubServers.Bungee.Event.SubSendCommandEvent;
import net.ME1312.SubServers.Bungee.Event.SubStartEvent;
import net.ME1312.SubServers.Bungee.Library.Config.YAMLConfig;
import net.ME1312.SubServers.Bungee.SubPlugin;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;

public class ConsolePlugin extends Plugin implements Listener {
    public HashMap<String, ConsoleWindow> cCurrent = new HashMap<String, ConsoleWindow>();
    public HashMap<String, ConsoleWindow> sCurrent = new HashMap<String, ConsoleWindow>();
    public YAMLConfig config;

    @Override
    public void onEnable() {
        try {
            getDataFolder().mkdirs();
            config = new YAMLConfig(new File(getDataFolder(), "config.yml"));
            boolean save = false;
            if (!config.get().getKeys().contains("Enabled-Servers")) {
                config.get().set("Enabled-Servers", Collections.emptyList());
                save = true;
            } if (!config.get().getKeys().contains("Enabled-Creators")) {
                config.get().set("Enabled-Creators", Collections.emptyList());
            }

            getProxy().getPluginManager().registerListener(this, this);
            getProxy().getPluginManager().registerCommand(this, new PopoutCommand.SERVER(this, "popout"));
            getProxy().getPluginManager().registerCommand(this, new PopoutCommand.SERVER(this, "popouts"));
            getProxy().getPluginManager().registerCommand(this, new PopoutCommand.CREATOR(this, "popoutc"));

            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                new JFrame("SubServers 2");
            } catch (ClassNotFoundException | InstantiationException | UnsupportedLookAndFeelException | IllegalAccessException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onServerCreate(SubCreateEvent event) {
        if (!event.isCancelled() && config.get().getStringList("Enabled-Creators").contains(event.getHost().getName().toLowerCase())) {
            if (!cCurrent.keySet().contains(event.getHost().getName().toLowerCase())) {
                cCurrent.put(event.getHost().getName().toLowerCase(), new ConsoleWindow(event.getHost().getCreator().getLogger()));
            } else {
                cCurrent.get(event.getHost().getName().toLowerCase()).clear();
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onServerStart(SubStartEvent event) {
        if (!event.isCancelled() && config.get().getStringList("Enabled-Servers").contains(event.getServer().getName().toLowerCase())) {
            if (!sCurrent.keySet().contains(event.getServer().getName().toLowerCase())) {
                sCurrent.put(event.getServer().getName().toLowerCase(), new ConsoleWindow(event.getServer().getLogger()));
            } else {
                sCurrent.get(event.getServer().getName().toLowerCase()).clear();
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onServerCommand(SubSendCommandEvent event) {
        if (!event.isCancelled() && sCurrent.keySet().contains(event.getServer().getName().toLowerCase())) {
            sCurrent.get(event.getServer().getName().toLowerCase()).log('<' + ((event.getPlayer() == null)?"CONSOLE":((getProxy().getPlayer(event.getPlayer()) == null)?event.getPlayer().toString():getProxy().getPlayer(event.getPlayer()).getName())) + "> /" + event.getCommand());
        }
    }

    @Override
    public SubPlugin getProxy() {
        return (SubPlugin) super.getProxy();
    }
}

package net.ME1312.SubServers.Console;

import net.ME1312.SubServers.Bungee.Event.SubCreateEvent;
import net.ME1312.SubServers.Bungee.Event.SubSendCommandEvent;
import net.ME1312.SubServers.Bungee.Event.SubStartEvent;
import net.ME1312.SubServers.Bungee.Host.Host;
import net.ME1312.SubServers.Bungee.Host.SubCreator;
import net.ME1312.SubServers.Bungee.Host.SubServer;
import net.ME1312.SubServers.Bungee.Library.Config.YAMLConfig;
import net.ME1312.SubServers.Bungee.SubAPI;
import net.ME1312.SubServers.Bungee.SubPlugin;
import net.ME1312.SubServers.Console.Library.Metrics;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;

public final class ConsolePlugin extends Plugin implements Listener {
    public HashMap<String, ConsoleWindow> cCurrent = new HashMap<String, ConsoleWindow>();
    public HashMap<String, ConsoleWindow> sCurrent = new HashMap<String, ConsoleWindow>();
    public YAMLConfig config;
    public boolean init = false;

    @Override
    public void onEnable() {
        SubAPI.getInstance().addListener(new Runnable() {
            @Override
            public void run() {
                enable();
            }
        }, new Runnable() {
            @Override
            public void run() {
                disable();
            }
        });
    }

    public void enable() {
        try {this.
            getDataFolder().mkdirs();
            config = new YAMLConfig(new File(getDataFolder(), "config.yml"));
            boolean save = false;
            if (!config.get().getKeys().contains("Enabled-Servers")) {
                config.get().set("Enabled-Servers", Collections.emptyList());
                save = true;
            } if (!config.get().getKeys().contains("Enabled-Creators")) {
                config.get().set("Enabled-Creators", Collections.emptyList());
                save = true;
            }
            if (save) config.save();

            if (!init) {
                init = true;

                getProxy().getPluginManager().registerListener(this, this);
                getProxy().getPluginManager().registerCommand(this, new ConsoleCommand.POPOUT(this, "popout"));
                getProxy().getPluginManager().registerCommand(this, new ConsoleCommand.AUTO_POPOUT(this, "autopopout"));

                new Metrics(this);

                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                    new JFrame("SubServers 2");
                } catch (ClassNotFoundException | InstantiationException | UnsupportedLookAndFeelException | IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onServerCreate(SubCreateEvent event) {
        if (!event.isCancelled()) {
            if (!cCurrent.keySet().contains(event.getHost().getName().toLowerCase())) {
                cCurrent.put(event.getName().toLowerCase(), new ConsoleWindow(this, event.getHost().getCreator().getLogger(event.getName().toLowerCase())));
            } else {
                cCurrent.get(event.getName().toLowerCase()).clear();
            }

            if (config.get().getStringList("Enabled-Creators").contains(event.getHost().getName().toLowerCase()))
                cCurrent.get(event.getName().toLowerCase()).open();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onServerStart(SubStartEvent event) {
        if (!event.isCancelled()) {
            if (!sCurrent.keySet().contains(event.getServer().getName().toLowerCase())) {
                sCurrent.put(event.getServer().getName().toLowerCase(), new ConsoleWindow(this, event.getServer().getLogger()));
            } else {
                sCurrent.get(event.getServer().getName().toLowerCase()).clear();
            }

            if (config.get().getStringList("Enabled-Servers").contains(event.getServer().getName().toLowerCase()))
                sCurrent.get(event.getServer().getName().toLowerCase()).open();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onServerCommand(SubSendCommandEvent event) {
        if (!event.isCancelled() && sCurrent.keySet().contains(event.getServer().getName().toLowerCase())) {
            sCurrent.get(event.getServer().getName().toLowerCase()).log('<' + ((event.getPlayer() == null)?"CONSOLE":((getProxy().getPlayer(event.getPlayer()) == null)?event.getPlayer().toString():getProxy().getPlayer(event.getPlayer()).getName())) + "> /" + event.getCommand());
        }
    }

    public void onClose(ConsoleWindow window) {
        if (window.getLogger().getHandler() instanceof SubServer) {
            SubServer server = (SubServer) window.getLogger().getHandler();
            window.destroy();
            sCurrent.remove(server.getName().toLowerCase());
        } else if (window.getLogger().getHandler() instanceof SubCreator) {
            Host host = ((SubCreator) window.getLogger().getHandler()).getHost();
            window.destroy();
            sCurrent.remove(host.getName().toLowerCase());
        } else {
            window.destroy();
        }
    }

    public void disable() {
        for (ConsoleWindow window : sCurrent.values()) {
            window.destroy();
        }
        sCurrent.clear();

        for (ConsoleWindow window : cCurrent.values()) {
            window.destroy();
        }
        cCurrent.clear();
    }

    @Override
    public SubPlugin getProxy() {
        return (SubPlugin) super.getProxy();
    }
}

package net.ME1312.SubServers.Web;

import net.ME1312.Galaxi.Library.Config.YAMLConfig;
import net.ME1312.SubServers.Bungee.Event.SubCreateEvent;
import net.ME1312.SubServers.Bungee.Event.SubSendCommandEvent;
import net.ME1312.SubServers.Bungee.Event.SubStartEvent;
import net.ME1312.SubServers.Bungee.Host.Host;
import net.ME1312.SubServers.Bungee.Host.RemotePlayer;
import net.ME1312.SubServers.Bungee.Host.SubCreator;
import net.ME1312.SubServers.Bungee.Host.SubServer;
import net.ME1312.SubServers.Bungee.Library.Metrics;
import net.ME1312.SubServers.Bungee.SubAPI;
import net.ME1312.SubServers.Bungee.SubProxy;

import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.logging.Level;

public final class ConsolePlugin extends Plugin implements Listener {
    public YAMLConfig config;
    private JettyServer jettyServer;

    @Override
    public void onEnable() {
        reload();

        new Metrics(this, 3853).addPlatformCharts();

        jettyServer = new JettyServer();
        try {
            jettyServer.start();
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "An error occurred when enabling the webserver, Plugin disabling...", e);
            this.onDisable();
            getProxy().getPluginManager().unregisterListeners(this);
            getProxy().getPluginManager().unregisterCommands(this);
        }

        SubAPI.getInstance().addListener(new Runnable() {
            @Override
            public void run() {
                reload();
            }
        });
    }

    private void reload() {

    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onServerCreate(SubCreateEvent event) {
        if (!event.isCancelled()) {

        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onServerStart(SubStartEvent event) {
        if (!event.isCancelled()) {

        }
    }

    @Override
    public void onDisable() {
        try {
            jettyServer.server.stop();
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "An error occurred when disabling the plugin", e);
        }
    }

    @Override
    public SubProxy getProxy() {
        return (SubProxy) super.getProxy();
    }
}

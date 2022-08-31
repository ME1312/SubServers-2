package net.ME1312.SubServers.Web;

import net.ME1312.SubServers.Bungee.Library.Metrics;
import net.ME1312.SubServers.Bungee.SubAPI;
import net.ME1312.SubServers.Bungee.SubProxy;

import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;

import java.io.IOException;
import java.util.logging.Level;

public final class ConsolePlugin extends Plugin implements Listener {
    private JettyServer jettyServer;

    @Override
    public void onEnable() {
        try {
            reload();
        } catch (IOException e) {
            e.printStackTrace();
        }

        new Metrics(this, 3853).addPlatformCharts();

        jettyServer = new JettyServer();
        try {
            jettyServer.start(getProxy());
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "An error occurred when enabling the webserver, Plugin disabling...", e);
            this.onDisable();
            getProxy().getPluginManager().unregisterListeners(this);
            getProxy().getPluginManager().unregisterCommands(this);
        }

        SubAPI.getInstance().addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    reload();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void reload() throws IOException {
        jettyServer.reload(true);
    }


    @Override
    public void onDisable() {
        try {
            jettyServer.stop();
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "An error occurred when disabling the plugin", e);
        }
    }

    @Override
    public SubProxy getProxy() {
        return (SubProxy) super.getProxy();
    }
}

package net.ME1312.SubServers.Bungee.Library.Compatibility.Galaxi;

import net.ME1312.Galaxi.Event.Engine.ConsoleInputEvent;
import net.ME1312.Galaxi.Galaxi;
import net.ME1312.Galaxi.Event.ListenerOrder;
import net.ME1312.Galaxi.Event.Subscribe;
import net.ME1312.SubServers.Bungee.Library.Compatibility.Logger;
import net.ME1312.SubServers.Bungee.SubProxy;

/**
 * Galaxi Event Listener Class
 */
public class GalaxiEventListener {
    private SubProxy plugin;

    /**
     * Create & Register a Galaxi Event Listener
     *
     * @param plugin Plugin
     */
    public GalaxiEventListener(SubProxy plugin) throws Throwable {
        this.plugin = plugin;

        Galaxi.getInstance().getPluginManager().registerListeners(Galaxi.getInstance().getAppInfo(), this);
        plugin.canSudo = true;
    }

    @Subscribe(order = ListenerOrder.FIRST, override = true)
    public void sudo(ConsoleInputEvent e) {
        if (plugin.sudo != null) {
            e.setCancelled(true);
            if (e.getInput().equalsIgnoreCase("exit")) {
                plugin.sudo = null;
                Logger.get("SubServers").info("Reverting to the BungeeCord Console");
            } else {
                plugin.sudo.command(e.getInput());
            }
        }
    }
}

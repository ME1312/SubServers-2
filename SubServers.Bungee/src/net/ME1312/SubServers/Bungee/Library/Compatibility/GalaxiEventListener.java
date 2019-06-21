package net.ME1312.SubServers.Bungee.Library.Compatibility;

import net.ME1312.Galaxi.Event.ConsoleInputEvent;
import net.ME1312.Galaxi.Galaxi;
import net.ME1312.Galaxi.Library.Event.ListenerOrder;
import net.ME1312.Galaxi.Library.Event.Subscribe;
import net.ME1312.SubServers.Bungee.SubPlugin;

/**
 * Galaxi Event Listener Class
 */
public class GalaxiEventListener {
    private SubPlugin plugin;

    /**
     * Create & Register a Galaxi Event Listener
     *
     * @param plugin Plugin
     */
    public GalaxiEventListener(SubPlugin plugin) throws Throwable {
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

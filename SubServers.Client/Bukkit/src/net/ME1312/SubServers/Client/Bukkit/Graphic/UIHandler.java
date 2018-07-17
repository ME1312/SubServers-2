package net.ME1312.SubServers.Client.Bukkit.Graphic;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * GUI Listener Layout Class
 */
public interface UIHandler {
    /**
     * Grabs the current Renderer for the player
     *
     * @param player Player
     * @return UIRenderer
     */
    UIRenderer getRenderer(Player player);

    /**
     * Disable Listener
     */
    void disable();

}

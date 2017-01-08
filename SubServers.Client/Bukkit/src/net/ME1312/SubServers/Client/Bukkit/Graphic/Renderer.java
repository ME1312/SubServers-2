package net.ME1312.SubServers.Client.Bukkit.Graphic;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * GUI Renderer Layout Class
 */
public interface Renderer {

    /**
     * Open the GUI
     *
     * @param player Player Opening
     * @param object Object Name
     */
    void open(Player player, String object);

    /**
     * Get Renderer Icon
     *
     * @return Icon
     */
    ItemStack getIcon();

    /**
     * Check if this Renderer is enabled for this Object
     *
     * @param object Object Name
     * @return Enabled Status
     */
    boolean isEnabled(String object);
}

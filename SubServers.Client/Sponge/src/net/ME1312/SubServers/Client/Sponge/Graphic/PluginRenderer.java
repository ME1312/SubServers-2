package net.ME1312.SubServers.Client.Sponge.Graphic;

import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.inventory.ItemStack;

/**
 * Plugin GUI Renderer Layout Class
 */
public interface PluginRenderer<T> {

    /**
     * Open the GUI
     *
     * @param player Player Opening
     * @param object Object passed
     */
    void open(Player player, T object);

    /**
     * Get Renderer Icon
     *
     * @return Icon
     */
    ItemStack getIcon();

    /**
     * Check if this Renderer is enabled for this Object
     *
     * @param object Object passed
     * @return Enabled Status
     */
    boolean isEnabled(T object);
}

package net.ME1312.SubServers.Client.Sponge.Graphic;

import net.ME1312.SubServers.Client.Sponge.Library.Config.YAMLSection;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.inventory.ItemStack;

/**
 * GUI Renderer Layout Class
 */
public interface Renderer {

    /**
     * Open the GUI
     *
     * @param player Player Opening
     * @param object Raw Representation of an Object
     */
    void open(Player player, YAMLSection object);

    /**
     * Get Renderer Icon
     *
     * @return Icon
     */
    ItemStack getIcon();

    /**
     * Check if this Renderer is enabled for this Object
     *
     * @param object Raw Representation of an Object
     * @return Enabled Status
     */
    boolean isEnabled(YAMLSection object);
}

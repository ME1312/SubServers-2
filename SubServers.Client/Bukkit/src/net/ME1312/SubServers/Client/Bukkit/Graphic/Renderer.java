package net.ME1312.SubServers.Client.Bukkit.Graphic;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.json.JSONObject;

/**
 * GUI Renderer Layout Class
 */
public interface Renderer {

    /**
     * Open the GUI
     *
     * @param player Player Opening
     * @param object JSON Representation of an Object
     */
    void open(Player player, JSONObject object);

    /**
     * Get Renderer Icon
     *
     * @return Icon
     */
    ItemStack getIcon();

    /**
     * Check if this Renderer is enabled for this Object
     *
     * @param object JSON Representation of an Object
     * @return Enabled Status
     */
    boolean isEnabled(JSONObject object);
}

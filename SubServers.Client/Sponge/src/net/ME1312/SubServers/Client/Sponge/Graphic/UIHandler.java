package net.ME1312.SubServers.Client.Sponge.Graphic;

import org.spongepowered.api.entity.living.player.Player;

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

package net.ME1312.SubServers.Velocity.Library.Fallback;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;

/**
 * Fallback Server Inspector Layout Class
 */
public interface FallbackInspector {

    /**
     * Inspect a fallback server and modify its confidence score
     *
     * @param player Player that requested (may be null)
     * @param server Server to inspect
     * @return A Positive Value to add points, a Negative Value to subtract points, a Null Value to invalidate the server, or a Zero Value to do nothing
     */
    Double inspect(Player player, RegisteredServer server);
}

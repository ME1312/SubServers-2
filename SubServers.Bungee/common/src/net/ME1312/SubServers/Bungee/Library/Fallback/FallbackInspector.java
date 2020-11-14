package net.ME1312.SubServers.Bungee.Library.Fallback;

import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;

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
    Double inspect(ProxiedPlayer player, ServerInfo server);
}

package net.ME1312.SubServers.Bungee;

import net.ME1312.Galaxi.Library.Callback.ReturnRunnable;

import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.config.ServerInfo;

import java.io.IOException;
import java.util.Map;

/**
 * BungeeCord Common Layout Class
 */
public abstract class BungeeCommon extends BungeeCord {
    final ReturnRunnable<BungeeAPI> api;

    protected BungeeCommon(ReturnRunnable<BungeeAPI> api) throws IOException {
        this.api = api;
    }

    /**
     * Get the name from BungeeCord's original signature (for determining which fork is being used)
     *
     * @return BungeeCord Software Name
     */
    public abstract String getBungeeName();

    /**
     * Waterfall's getServersCopy()
     *
     * @return Server Map Copy
     */
    public abstract Map<String, ServerInfo> getServersCopy();
}

package net.ME1312.SubServers.Bungee;

import net.ME1312.Galaxi.Library.Util;

import io.netty.channel.Channel;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.config.ServerInfo;

import java.util.Collection;
import java.util.Map;
import java.util.function.Supplier;

/**
 * BungeeCord Common Layout Class
 */
public abstract class BungeeCommon extends BungeeCord {
    private static BungeeCommon instance;
    final Supplier<BungeeAPI> api;
    protected final Collection<Channel> listeners;

    protected BungeeCommon(Supplier<BungeeAPI> api) throws Exception {
        listeners= Util.reflect(BungeeCord.class.getDeclaredField("listeners"), this);
        this.api = api;
        instance = this;
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

    /**
     * Gets the ProxyServer Common Object
     *
     * @return ProxyServer Common
     */
    public static BungeeCommon getInstance() {
        return instance;
    }
}

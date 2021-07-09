package net.ME1312.SubServers.Velocity.Library.Fallback;

import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;

import java.util.*;

/**
 * Fallback Player State Class
 */
public class FallbackState {
    public final UUID player;
    public final LinkedList<String> names;
    public final LinkedList<RegisteredServer> servers;
    public final Component reason;
    private Map<String, RegisteredServer> map;
    private Timer finish;

    /**
     * Smart Fallback State Container
     *
     * @param player Player
     * @param servers Fallback Servers
     * @param reason Original Disconnect Reason
     */
    public FallbackState(UUID player, Map<String, RegisteredServer> servers, Component reason) {
        this.player = player;
        this.map = servers;
        this.names = new LinkedList<>(servers.keySet());
        this.servers = new LinkedList<>(servers.values());
        this.reason = reason;
    }

    /**
     * <i>Use</i> a server
     *
     * @param name Server name to remove
     */
    public void remove(String name) {
        servers.remove(map.get(name));
        names.remove(name);
        map.remove(name);
    }

    /**
     * <i>Use</i> a server
     *
     * @param server Server to remove
     */
    public void remove(RegisteredServer server) {
        map.remove(server.getServerInfo().getName());
        names.remove(server.getServerInfo().getName());
        servers.remove(server);
    }

    /**
     * Finish the process
     *
     * @param callback Finishing callback
     * @param delay Delay for determining stability
     */
    public void done(Runnable callback, long delay) {
        if (finish != null) finish.cancel();
        (finish = new Timer("SubServers.Bungee::Fallback_Limbo_Timer(" + player + ')')).schedule(new TimerTask() {
            @Override
            public void run() {
                if (callback != null) callback.run();
                finish.cancel();
            }
        }, delay);
    }
}

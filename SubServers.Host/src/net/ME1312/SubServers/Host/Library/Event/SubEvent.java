package net.ME1312.SubServers.Host.Library.Event;

import net.ME1312.SubServers.Host.API.SubPluginInfo;
import net.ME1312.SubServers.Host.SubAPI;

/**
 * SubEvent Layout Class
 */
public abstract class SubEvent {
    private SubPluginInfo plugin = null;

    /**
     * Gets SubAPI
     *
     * @return SubAPI
     */
    public SubAPI getAPI() {
        return SubAPI.getInstance();
    }

    /**
     * Gets your Plugin's Info
     *
     * @return Plugin Info
     */
    public SubPluginInfo getPlugin() {
        return plugin;
    }
}

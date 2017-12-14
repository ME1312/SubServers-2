package net.ME1312.SubServers.Host.API.Event;

import net.ME1312.SubServers.Host.ExHost;
import net.ME1312.SubServers.Host.Library.Event.Event;
import net.ME1312.SubServers.Host.Library.Util;

/**
 * SubServers.Host Reload Event Class
 */
public class SubReloadEvent extends Event {

    /**
     * SubServers.Host Reload Event
     *
     * @param host SubServers.Host
     */
    public SubReloadEvent(ExHost host) {
        if (Util.isNull(host)) throw new NullPointerException();
    }
}

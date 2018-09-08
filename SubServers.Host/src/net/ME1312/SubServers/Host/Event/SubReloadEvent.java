package net.ME1312.SubServers.Host.Event;

import net.ME1312.Galaxi.Library.Event.Event;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubServers.Host.ExHost;

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

package net.ME1312.SubServers.Host.API.Event;

import net.ME1312.SubServers.Host.Library.Event.Event;
import net.ME1312.SubServers.Host.Library.Util;
import net.ME1312.SubServers.Host.ExHost;

/**
 * SubServers.Host Enable Event Class
 */
public class SubEnableEvent extends Event {

    /**
     * SubServers.Host Enable Event
     *
     * @param host SubServers.Host
     */
    public SubEnableEvent(ExHost host) {
        if (Util.isNull(host)) throw new NullPointerException();
    }
}

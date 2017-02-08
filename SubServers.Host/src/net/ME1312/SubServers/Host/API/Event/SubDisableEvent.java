package net.ME1312.SubServers.Host.API.Event;

import net.ME1312.SubServers.Host.Library.Event.Event;
import net.ME1312.SubServers.Host.Library.Util;
import net.ME1312.SubServers.Host.SubServers;

/**
 * SubServers.Host Disable Event Class
 */
public class SubDisableEvent extends Event {
    private int exit;

    /**
     * SubServers.Host Disable Event
     *
     * @param host SubServers.Host
     * @param exit Exit Code
     */
    public SubDisableEvent(SubServers host, int exit) {
        if (Util.isNull(host, exit)) throw new NullPointerException();
        this.exit = exit;
    }

    /**
     * Get the Exit Code
     *
     * @return Exit Code
     */
    public int getExitCode() {
        return exit;
    }

    /**
     * Set the Exit Code
     *
     * @param value Value
     */
    public void setExitCode(int value) {
        this.exit = value;
    }
}

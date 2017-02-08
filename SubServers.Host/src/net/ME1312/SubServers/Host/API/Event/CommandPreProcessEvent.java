package net.ME1312.SubServers.Host.API.Event;

import net.ME1312.SubServers.Host.Library.Event.Cancellable;
import net.ME1312.SubServers.Host.Library.Event.Event;
import net.ME1312.SubServers.Host.Library.Util;
import net.ME1312.SubServers.Host.SubServers;

/**
 * Command Pre-Process Event Class
 */
public class CommandPreProcessEvent extends Event implements Cancellable {
    private boolean cancelled = false;
    private String command;

    /**
     * Command Pre-Process Event
     *
     * @param host SubServers.Host
     * @param command Command
     */
    public CommandPreProcessEvent(SubServers host, String command) {
        if (Util.isNull(host, command)) throw new NullPointerException();
        this.command = command;
    }

    /**
     * Gets the full Command String
     *
     * @return Command
     */
    public String getCommand() {
        return this.command;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean value) {
        this.cancelled = value;
    }
}

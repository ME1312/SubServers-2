package net.ME1312.SubServers.Host.API.Event;

import net.ME1312.SubServers.Host.Library.Event.Cancellable;
import net.ME1312.SubServers.Host.Library.Event.SubEvent;
import net.ME1312.SubServers.Host.Library.Util;
import net.ME1312.SubServers.Host.SubServers;

/**
 * Command Pre-Process Event
 */
public class CommandPreProcessEvent extends SubEvent implements Cancellable {
    private boolean cancelled = false;
    private String command;

    public CommandPreProcessEvent(SubServers plugin, String command) {
        if (Util.isNull(plugin, command)) throw new NullPointerException();
        this.command = command;
    }

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

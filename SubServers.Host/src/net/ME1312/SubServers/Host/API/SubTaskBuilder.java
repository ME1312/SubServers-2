package net.ME1312.SubServers.Host.API;

import net.ME1312.SubServers.Host.Library.Util;
import net.ME1312.SubServers.Host.SubAPI;

import java.util.UUID;

/**
 * SubServers Task Builder Class
 */
public abstract class SubTaskBuilder implements Runnable {
    private long repeat = -1L;
    private long delay = -1L;
    private SubPluginInfo plugin;

    /**
     * Create a new Task
     *
     * @param plugin Plugin Creating
     */
    public SubTaskBuilder(SubPluginInfo plugin) {
        if (Util.isNull(plugin)) throw new NullPointerException();
        this.plugin = plugin;
    }

    /**
     * Get the Plugin that created this task
     *
     * @return Plugin Info
     */
    public SubPluginInfo getPlugin() {
        return this.plugin;
    }

    /**
     * Set the Repeat Interval for this task
     *
     * @param value Value
     * @return Task Builder
     */
    public SubTaskBuilder repeat(long value) {
        if (Util.isNull(value)) throw new NullPointerException();
        this.repeat = value;
        return this;
    }

    /**
     * Get the Repeat Interval for this task
     *
     * @return Repeat Interval
     */
    public long getRepeat() {
        return this.repeat;
    }

    /**
     * Delay this task
     *
     * @param value Value
     * @return Task Builder
     */
    public SubTaskBuilder delay(long value) {
        if (Util.isNull(value)) throw new NullPointerException();
        this.delay = value;
        return this;
    }

    /**
     * Get the Delay for this task
     *
     * @return Task Delay
     */
    public long getDelay() {
        return this.delay;
    }

    /**
     * Schedule this task
     *
     * @return Task ID
     */
    public UUID schedule() {
        return SubAPI.getInstance().schedule(this);
    }
}

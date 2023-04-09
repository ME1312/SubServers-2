package net.ME1312.SubServers.Client.Bukkit.Library.Compatibility;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Folia Regional Scheduling Compatibility Class
 */
public abstract class AgnosticScheduler {

    /**
     * Provides an asynchronous thread scheduler (in both folia and bukkit)
     */
    public static final AgnosticScheduler async = async();
    private static native AgnosticScheduler async();

    /**
     * Provides the folia global region scheduler / bukkit main thread scheduler
     */
    public static final AgnosticScheduler global = global();
    private static native AgnosticScheduler global();

    /**
     * Provides a folia region scheduler / bukkit main thread scheduler
     *
     * @param block Block
     * @return Platform-agnostic Scheduler
     */
    public static native AgnosticScheduler at(Block block);

    /**
     * Provides a folia region scheduler / bukkit main thread scheduler
     *
     * @param location Block location
     * @return Platform-agnostic Scheduler
     */
    public static native AgnosticScheduler at(Location location);
    /**
     * Provides a folia region scheduler / bukkit main thread scheduler
     *
     * @param world Block world
     * @param x Block x coordinate
     * @param z Block z coordinate
     * @return Platform-agnostic Scheduler
     */
    public static native AgnosticScheduler at(World world, int x, int z);

    /**
     * Provides a folia region scheduler / bukkit main thread scheduler
     *
     * @param chunk Chunk
     * @return Platform-agnostic Scheduler
     */
    public static native AgnosticScheduler atChunk(Chunk chunk);

    /**
     * Provides a folia region scheduler / bukkit main thread scheduler
     *
     * @param world Chunk world
     * @param cx Chunk x coordinate
     * @param cz Chunk z coordinate
     * @return Platform-agnostic Scheduler
     */
    public static native AgnosticScheduler atChunk(World world, int cx, int cz);

    /**
     * Provides a folia entity scheduler / bukkit main thread scheduler
     *
     * @param entity Entity
     * @return Platform-agnostic Scheduler
     */
    public static native AgnosticScheduler following(Entity entity);

    /**
     * Schedules a 1-time task that runs immediately
     *
     * @param plugin Plugin
     * @param task Task (consumes a task cancellation runnable)
     * @return A Runnable that can currently be used to cancel the task
     */
    public abstract Runnable runs(Plugin plugin, Consumer<Runnable> task);

    /**
     * Schedules a 1-time task that runs after a number of ticks
     *
     * @param plugin Plugin
     * @param task Task (consumes a task cancellation runnable)
     * @param delay Delay in ticks
     * @return A Runnable that can currently be used to cancel the task
     */
    public abstract Runnable runs(Plugin plugin, Consumer<Runnable> task, long delay);

    /**
     * Schedules a 1-time task that runs after a number of timeunits
     *
     * @param plugin Plugin
     * @param task Task (consumes a task cancellation runnable)
     * @param delay Delay
     * @param units Time units
     * @return A Runnable that can currently be used to cancel the task
     */
    public native Runnable runs(Plugin plugin, Consumer<Runnable> task, long delay, TimeUnit units);

    /**
     * Schedules a repeating task that runs after a number of ticks
     *
     * @param plugin Plugin
     * @param task Task (consumes a task cancellation runnable)
     * @param repeat Repeat delay in ticks
     * @return A Runnable that can currently be used to cancel the task
     */
    public native Runnable repeats(Plugin plugin, Consumer<Runnable> task, long repeat);

    /**
     * Schedules a repeating task that runs after a number of timeunits
     *
     * @param plugin Plugin
     * @param task Task (consumes a task cancellation runnable)
     * @param repeat Repeat delay
     * @param units Time units
     * @return A Runnable that can currently be used to cancel the task
     */
    public native Runnable repeats(Plugin plugin, Consumer<Runnable> task, long repeat, TimeUnit units);

    /**
     * Schedules a repeating task that runs after a number of ticks
     *
     * @param plugin Plugin
     * @param task Task (consumes a task cancellation runnable)
     * @param delay Initial delay in ticks
     * @param repeat Repeat delay in ticks
     * @return A Runnable that can currently be used to cancel the task
     */
    public abstract Runnable repeats(Plugin plugin, Consumer<Runnable> task, long delay, long repeat);

    /**
     * Schedules a repeating task that runs after a number of timeunits
     *
     * @param plugin Plugin
     * @param task Task (consumes a task cancellation runnable)
     * @param delay Initial delay
     * @param repeat Repeat delay
     * @param units Time units
     * @return A Runnable that can currently be used to cancel the task
     */
    public native Runnable repeats(Plugin plugin, Consumer<Runnable> task, long delay, long repeat, TimeUnit units);
}

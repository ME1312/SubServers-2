package net.ME1312.SubServers.Client.Bukkit.Library.Compatibility;

import net.ME1312.Galaxi.Library.Container.Container;
import net.ME1312.Galaxi.Library.Try;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public abstract class AgnosticScheduler {
    private static final boolean regional = Try.all.get(() -> Class.forName("io.papermc.paper.threadedregions.scheduler.RegionScheduler") != null, false);
    private static final Runnable empty = () -> {};

    public static final AgnosticScheduler async = ((regional)?
            new AgnosticScheduler() {
                @Override
                public Runnable runs(Plugin plugin, Consumer<Runnable> task) {
                    return Bukkit.getAsyncScheduler().runNow(plugin, t -> task.accept(t::cancel))::cancel;
                }

                @Override
                public Runnable runs(Plugin plugin, Consumer<Runnable> task, long delay) {
                    return Bukkit.getAsyncScheduler().runDelayed(plugin, t -> task.accept(t::cancel), delay * 50, TimeUnit.MILLISECONDS)::cancel;
                }

                @Override
                public Runnable runs(Plugin plugin, Consumer<Runnable> task, long delay, TimeUnit units) {
                    return Bukkit.getAsyncScheduler().runDelayed(plugin, t -> task.accept(t::cancel), delay, units)::cancel;
                }

                @Override
                public Runnable repeats(Plugin plugin, Consumer<Runnable> task, long repeat) {
                    return repeats(plugin, task, repeat *= 50, repeat, TimeUnit.MILLISECONDS);
                }

                @Override
                public Runnable repeats(Plugin plugin, Consumer<Runnable> task, long repeat, TimeUnit units) {
                    return repeats(plugin, task, repeat, repeat, units);
                }

                @Override
                public Runnable repeats(Plugin plugin, Consumer<Runnable> task, long delay, long repeat) {
                    return repeats(plugin, task, delay * 50, repeat * 50, TimeUnit.MILLISECONDS);
                }

                @Override
                public Runnable repeats(Plugin plugin, Consumer<Runnable> task, long delay, long repeat, TimeUnit units) {
                    if (repeat != 0) {
                        return Bukkit.getAsyncScheduler().runAtFixedRate(plugin, t -> task.accept(t::cancel), delay, repeat, units)::cancel;
                    } else if (units.ordinal() >= TimeUnit.MILLISECONDS.ordinal()) {
                        return Bukkit.getAsyncScheduler().runAtFixedRate(plugin, t -> task.accept(t::cancel), units.toMillis(delay), 50, TimeUnit.MILLISECONDS)::cancel;
                    } else {
                        return Bukkit.getAsyncScheduler().runAtFixedRate(plugin, t -> task.accept(t::cancel), delay, units.convert(50, TimeUnit.MILLISECONDS), units)::cancel;
                    }
                }
            }
    :
            new AgnosticScheduler() {
                @Override
                public Runnable runs(Plugin plugin, Consumer<Runnable> task) {
                    final Container<Runnable> cancel = new Container<>(empty);
                    return cancel.value = Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> task.accept(cancel.value))::cancel;
                }

                @Override
                public Runnable runs(Plugin plugin, Consumer<Runnable> task, long delay) {
                    final Container<Runnable> cancel = new Container<>(empty);
                    return cancel.value = Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> task.accept(cancel.value), check(delay, 0))::cancel;
                }

                @Override
                public Runnable repeats(Plugin plugin, Consumer<Runnable> task, long delay, long repeat) {
                    final Container<Runnable> cancel = new Container<>(empty);
                    return cancel.value = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> task.accept(cancel.value), check(delay, 0), check(repeat, 0))::cancel;
                }
            }
    );

    public static final AgnosticScheduler global = ((regional)?
            new AgnosticScheduler() {
                @Override
                public Runnable runs(Plugin plugin, Consumer<Runnable> task) {
                    return Bukkit.getGlobalRegionScheduler().run(plugin, t -> task.accept(t::cancel))::cancel;
                }

                @Override
                public Runnable runs(Plugin plugin, Consumer<Runnable> task, long delay) {
                    return Bukkit.getGlobalRegionScheduler().runDelayed(plugin, t -> task.accept(t::cancel), check(delay, 1))::cancel;
                }

                @Override
                public Runnable repeats(Plugin plugin, Consumer<Runnable> task, long delay, long repeat) {
                    return Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, t -> task.accept(t::cancel), check(delay, 1), check(repeat, 1))::cancel;
                }
            }
    :
            new AgnosticScheduler() {
                @Override
                public Runnable runs(Plugin plugin, Consumer<Runnable> task) {
                    final Container<Runnable> cancel = new Container<>(empty);
                    return cancel.value = Bukkit.getScheduler().runTask(plugin, () -> task.accept(cancel.value))::cancel;
                }

                @Override
                public Runnable runs(Plugin plugin, Consumer<Runnable> task, long delay) {
                    final Container<Runnable> cancel = new Container<>(empty);
                    return cancel.value = Bukkit.getScheduler().runTaskLater(plugin, () -> task.accept(cancel.value), check(delay, 0))::cancel;
                }

                @Override
                public Runnable repeats(Plugin plugin, Consumer<Runnable> task, long delay, long repeat) {
                    final Container<Runnable> cancel = new Container<>(empty);
                    return cancel.value = Bukkit.getScheduler().runTaskTimer(plugin, () -> task.accept(cancel.value), check(delay, 0), check(repeat, 0))::cancel;
                }
            }
    );

    public static AgnosticScheduler at(Block block) {
        return atChunk(block.getWorld(), block.getX() >> 4, block.getZ() >> 4);
    }

    public static AgnosticScheduler at(Location location) {
        return atChunk(location.getWorld(), location.getBlockX() >> 4, location.getBlockZ() >> 4);
    }

    public static AgnosticScheduler at(World world, int x, int z) {
        return atChunk(world, x >> 4, z >> 4);
    }

    public static AgnosticScheduler atChunk(Chunk chunk) {
        return atChunk(chunk.getWorld(), chunk.getX(), chunk.getZ());
    }

    public static AgnosticScheduler atChunk(World world, int cx, int cz) {
        return (regional)? new AgnosticScheduler() {
            @Override
            public Runnable runs(Plugin plugin, Consumer<Runnable> task) {
                return Bukkit.getRegionScheduler().run(plugin, world, cx, cz, t -> task.accept(t::cancel))::cancel;
            }

            @Override
            public Runnable runs(Plugin plugin, Consumer<Runnable> task, long delay) {
                return Bukkit.getRegionScheduler().runDelayed(plugin, world, cx, cz, t -> task.accept(t::cancel), check(delay, 1))::cancel;
            }

            @Override
            public Runnable repeats(Plugin plugin, Consumer<Runnable> task, long delay, long repeat) {
                return Bukkit.getRegionScheduler().runAtFixedRate(plugin, world, cx, cz, t -> task.accept(t::cancel), check(delay, 1), check(repeat, 1))::cancel;
            }
        } : global;
    }

    public static AgnosticScheduler following(Entity entity) {
        return (regional)? new AgnosticScheduler() {
            @Override
            public Runnable runs(Plugin plugin, Consumer<Runnable> task) {
                final io.papermc.paper.threadedregions.scheduler.ScheduledTask value = entity.getScheduler().run(plugin, t -> task.accept(t::cancel), () -> {
                    at(entity.getLocation()).runs(plugin, task);
                });
                return (value != null)? value::cancel : at(entity.getLocation()).runs(plugin, task);
            }

            @Override
            public Runnable runs(Plugin plugin, Consumer<Runnable> task, long delay) {
                return runs(plugin, task, delay * 50, delay);
            }

            @Override
            public Runnable runs(Plugin plugin, Consumer<Runnable> task, long delay, TimeUnit units) {
                return runs(plugin, task, delay = units.toMillis(delay), delay / 50);
            }

            private Runnable runs(Plugin plugin, Consumer<Runnable> task, long dMS, long dT) {
                if (dMS < 0) throw new IllegalStateException("Delay may not be < 0");
                final Instant next = Instant.now().plusMillis(dMS);
                final io.papermc.paper.threadedregions.scheduler.ScheduledTask value = entity.getScheduler().runDelayed(plugin, t -> task.accept(t::cancel), () -> {
                    at(entity.getLocation()).runs(plugin, task, Math.min(0, Duration.between(Instant.now(), next).toMillis()), TimeUnit.MILLISECONDS);
                }, (dT == 0)? 1 : dT);
                return (value != null)? value::cancel : at(entity.getLocation()).runs(plugin, task, dT);
            }
            @Override
            public Runnable repeats(Plugin plugin, Consumer<Runnable> task, long repeat) {
                final long rMS;
                return repeats(plugin, task, rMS = repeat * 50, repeat, rMS, repeat);
            }

            @Override
            public Runnable repeats(Plugin plugin, Consumer<Runnable> task, long repeat, TimeUnit units) {
                final long rT;
                return repeats(plugin, task, repeat = units.toMillis(repeat), rT = repeat / 50, repeat, rT);
            }

            @Override
            public Runnable repeats(Plugin plugin, Consumer<Runnable> task, long delay, long repeat) {
                return repeats(plugin, task, delay * 50, delay, repeat * 50, repeat);
            }

            @Override
            public Runnable repeats(Plugin plugin, Consumer<Runnable> task, long delay, long repeat, TimeUnit units) {
                return repeats(plugin, task, delay = units.toMillis(delay), delay / 50, repeat = units.toMillis(repeat), repeat / 50);
            }

            private Runnable repeats(Plugin plugin, Consumer<Runnable> task, long dMS, long dT, long rMS, long rT) {
                if (dMS < 0 || rMS < 0) throw new IllegalStateException("Delay may not be < 0");
                final Container<Instant> next = new Container<>(Instant.now().plusMillis(dMS));
                final io.papermc.paper.threadedregions.scheduler.ScheduledTask value = entity.getScheduler().runAtFixedRate(plugin, t -> {
                    next.value = Instant.now().plusMillis(rMS);
                    task.accept(t::cancel);
                }, () -> {
                    at(entity.getLocation()).repeats(plugin, task, Math.min(0, Duration.between(Instant.now(), next.value).toMillis()), rMS, TimeUnit.MILLISECONDS);
                }, (dT == 0)? 1 : dT, (rT == 0)? 1 : rT);
                return (value != null)? value::cancel : at(entity.getLocation()).repeats(plugin, task, dT, rT);
            }
        } : global;
    }

    private static long check(long amount, int minimum) {
        if (amount < 0) throw new IllegalArgumentException("Delay ticks may not be < 0");
        return (amount == 0)? minimum : amount;
    }

    public abstract Runnable runs(Plugin plugin, Consumer<Runnable> task);

    public abstract Runnable runs(Plugin plugin, Consumer<Runnable> task, long delay);

    public Runnable runs(Plugin plugin, Consumer<Runnable> task, long delay, TimeUnit units) {
        return runs(plugin, task, units.toMillis(delay) / 50);
    }

    public Runnable repeats(Plugin plugin, Consumer<Runnable> task, long repeat) {
        return repeats(plugin, task, repeat, repeat);
    }

    public Runnable repeats(Plugin plugin, Consumer<Runnable> task, long repeat, TimeUnit units) {
        return repeats(plugin, task, repeat = units.toMillis(repeat) / 50, repeat);
    }

    public abstract Runnable repeats(Plugin plugin, Consumer<Runnable> task, long delay, long repeat);

    public Runnable repeats(Plugin plugin, Consumer<Runnable> task, long delay, long repeat, TimeUnit units) {
        return repeats(plugin, task, units.toMillis(delay) / 50, units.toMillis(repeat) / 50);
    }
}

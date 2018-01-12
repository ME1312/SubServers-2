package net.ME1312.SubServers.Host;

import net.ME1312.SubServers.Host.API.Command;
import net.ME1312.SubServers.Host.API.SubPluginInfo;
import net.ME1312.SubServers.Host.API.SubTask;
import net.ME1312.SubServers.Host.Library.Event.*;
import net.ME1312.SubServers.Host.Library.UniversalFile;
import net.ME1312.SubServers.Host.Library.Util;
import net.ME1312.SubServers.Host.Library.Version.Version;
import net.ME1312.SubServers.Host.Network.SubDataClient;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * SubAPI Class
 */
public final class SubAPI {
    final TreeMap<Short, HashMap<Class<? extends Event>, HashMap<SubPluginInfo, HashMap<Object, List<Method>>>>> listeners = new TreeMap<Short, HashMap<Class<? extends Event>, HashMap<SubPluginInfo, HashMap<Object, List<Method>>>>>();
    final HashMap<UUID, Timer> schedule = new HashMap<UUID, Timer>();
    final TreeMap<String, Command> commands = new TreeMap<String, Command>();
    final HashMap<String, SubPluginInfo> plugins = new LinkedHashMap<String, SubPluginInfo>();
    private final ExHost host;
    private static SubAPI api;

    protected SubAPI(ExHost host) {
        this.host = host;
        api = this;
    }

    /**
     * Gets the SubAPI Methods
     *
     * @return SubAPI
     */
    public static SubAPI getInstance() {
        return api;
    }

    /**
     * Gets the SubServers Internals
     *
     * @return SubServers.Host Internals
     * @deprecated Use SubAPI Methods when available
     */
    @Deprecated
    public ExHost getInternals() {
        return host;
    }

    /**
     * Gets the SubData Network Manager
     *
     * @return SubData Network Manager
     */
    public SubDataClient getSubDataNetwork() {
        return host.subdata;
    }

    /**
     * Get a map of the Plugins
     *
     * @return PluginInfo Map
     */
    public Map<String, SubPluginInfo> getPlugins() {
        return new LinkedHashMap<String, SubPluginInfo>(plugins);
    }

    /**
     * Gets a Plugin
     *
     * @param plugin Plugin Name
     * @return PluginInfo
     */
    public SubPluginInfo getPlugin(String plugin) {
        if (Util.isNull(plugin)) throw new NullPointerException();
        return getPlugins().get(plugin.toLowerCase());
    }

    /**
     * Registers a Command
     *
     * @param command Command
     * @param handles Aliases
     */
    public void addCommand(Command command, String... handles) {
        for (String handle : handles) {
            commands.put(handle, command);
        }
    }

    /**
     * Unregisters a Command
     *
     * @param handles Aliases
     */
    public void removeCommand(String... handles) {
        for (String handle : handles) {
            commands.remove(handle.toLowerCase());
        }
    }

    private UUID getFreeSID() {
        UUID sid = null;
        do {
            UUID id = UUID.randomUUID();
            if (!schedule.keySet().contains(id)) {
                sid = id;
            }
        } while (sid == null);
        return sid;
    }

    /**
     * Schedule a task
     *
     * @param builder SubTaskBuilder
     * @return Task ID
     */
    public UUID schedule(SubTask builder) {
        if (Util.isNull(builder)) throw new NullPointerException();
        UUID sid = getFreeSID();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                try {
                    builder.run();
                } catch (Throwable e) {
                    host.log.error.println(new InvocationTargetException(e, "Unhandled exception while running SubTask " + sid.toString()));
                }
                if (builder.repeat() <= 0) schedule.remove(sid);
            }
        };

        schedule.put(sid, new Timer("SubTask_" + sid.toString()));
        if (builder.repeat() > 0) {
            if (builder.delay() > 0) {
                schedule.get(sid).scheduleAtFixedRate(task, builder.delay(), builder.repeat());
            } else {
                schedule.get(sid).scheduleAtFixedRate(task, new Date(), builder.repeat());
            }
        } else {
            if (builder.delay() > 0) {
                schedule.get(sid).schedule(task, builder.delay());
            } else {
                new Thread(task).start();
            }
        }
        return sid;
    }

    /**
     * Schedule a task
     *
     * @param plugin Plugin Scheduling
     * @param run What to run
     * @return Task ID
     */
    public UUID schedule(SubPluginInfo plugin, Runnable run) {
        return schedule(plugin, run, -1L);
    }

    /**
     * Schedule a task
     *
     * @param plugin Plugin Scheduling
     * @param run What to Run
     * @param delay Task Delay
     * @return Task ID
     */
    public UUID schedule(SubPluginInfo plugin, Runnable run, long delay) {
        return schedule(plugin, run, delay, -1L);
    }

    /**
     * Schedule a task
     *
     * @param plugin Plugin Scheduling
     * @param run What to Run
     * @param delay Task Delay
     * @param repeat Task Repeat Interval
     * @return Task ID
     */
    public UUID schedule(SubPluginInfo plugin, Runnable run, long delay, long repeat) {
        return schedule(plugin, run, TimeUnit.MILLISECONDS, delay, repeat);
    }

    /**
     * Schedule a task
     *
     * @param plugin Plugin Scheduling
     * @param run What to Run
     * @param unit TimeUnit to use
     * @param delay Task Delay
     * @param repeat Task Repeat Interval
     * @return Task ID
     */
    public UUID schedule(SubPluginInfo plugin, Runnable run, TimeUnit unit, long delay, long repeat) {
        if (Util.isNull(plugin, run, unit, delay, repeat)) throw new NullPointerException();
        return schedule(new SubTask(plugin) {
            @Override
            public void run() {
                run.run();
            }
        }.delay(unit.toMillis(delay)).repeat(unit.toMillis(repeat)));
    }

    /**
     * Cancel a task
     *
     * @param sid Task ID
     */
    public void cancelTask(UUID sid) {
        if (Util.isNull(sid)) throw new NullPointerException();
        if (schedule.keySet().contains(sid)) {
            schedule.get(sid).cancel();
            schedule.remove(sid);
        }
    }

    /**
     * Register SubEvent Listeners
     *
     * @param plugin PluginInfo
     * @param listeners Listeners
     */
    @SuppressWarnings("unchecked")
    void addListener(SubPluginInfo plugin, Object... listeners) {
        for (Object listener : listeners) {
            if (Util.isNull(plugin, listener)) throw new NullPointerException();
            for (Method method : Arrays.asList(listener.getClass().getMethods())) {
                if (!method.isAnnotationPresent(EventHandler.class)) {
                    if (method.getParameterTypes().length == 1) {
                        if (Event.class.isAssignableFrom(method.getParameterTypes()[0])) {
                            HashMap<Class<? extends Event>, HashMap<SubPluginInfo, HashMap<Object, List<Method>>>> events = (this.listeners.keySet().contains(method.getAnnotation(EventHandler.class).order()))?this.listeners.get(method.getAnnotation(EventHandler.class).order()):new LinkedHashMap<Class<? extends Event>, HashMap<SubPluginInfo, HashMap<Object, List<Method>>>>();
                            HashMap<SubPluginInfo, HashMap<Object, List<Method>>> plugins = (events.keySet().contains((Class<Event>) method.getParameterTypes()[0]))?events.get((Class<Event>) method.getParameterTypes()[0]):new LinkedHashMap<SubPluginInfo, HashMap<Object, List<Method>>>();
                            HashMap<Object, List<Method>> objects = (plugins.keySet().contains(plugin))?plugins.get(plugin):new LinkedHashMap<Object, List<Method>>();
                            List<Method> methods = (objects.keySet().contains(listener))?objects.get(listener):new LinkedList<Method>();
                            methods.add(method);
                            objects.put(listener, methods);
                            plugins.put(plugin, objects);
                            events.put((Class<Event>) method.getParameterTypes()[0], plugins);
                            this.listeners.put(method.getAnnotation(EventHandler.class).order(), events);
                        } else {
                            this.host.log.error.println(
                                    "Cannot register EventHandler in class \"" + listener.getClass().getCanonicalName() + "\" using method \"" + method.getName() + "\":",
                                    "\"" + method.getParameterTypes()[0].getCanonicalName() + "\" is not an  Event");
                        }
                    } else {
                        this.host.log.error.println(
                                "Cannot register EventHandler in class \"" + listener.getClass().getCanonicalName() + "\" using method \"" + method.getName() + "\":",
                                ((method.getParameterTypes().length > 0) ? "Too many" : "No") + " parameters for SubEvent to execute");
                    }
                }
            }
        }
    }

    /**
     * Unregister SubEvent Listeners
     *
     * @param plugin PluginInfo
     * @param listeners Listeners
     */
    public void removeListener(SubPluginInfo plugin, Object... listeners) {
        for (Object listener : listeners) {
            if (Util.isNull(plugin, listener)) throw new NullPointerException();
            TreeMap<Short, HashMap<Class<? extends Event>, HashMap<SubPluginInfo, HashMap<Object, List<Method>>>>> map = new TreeMap<Short, HashMap<Class<? extends Event>, HashMap<SubPluginInfo, HashMap<Object, List<Method>>>>>(this.listeners);
            for (Short order : map.keySet()) {
                for (Class<? extends Event> event : map.get(order).keySet()) {
                    if (map.get(order).get(event).keySet().contains(plugin) && map.get(order).get(event).get(plugin).keySet().contains(listener)) {
                        HashMap<Class<? extends Event>, HashMap<SubPluginInfo, HashMap<Object, List<Method>>>> events = this.listeners.get(order);
                        HashMap<SubPluginInfo, HashMap<Object, List<Method>>> plugins = this.listeners.get(order).get(event);
                        HashMap<Object, List<Method>> objects = this.listeners.get(order).get(event).get(plugin);
                        objects.remove(listener);
                        plugins.put(plugin, objects);
                        events.put(event, plugins);
                        this.listeners.put(order, events);
                    }
                }
            }
        }
    }

    /**
     * Run a SubEvent
     *
     * @param event SubEvent
     */
    public void executeEvent(Event event) {
        if (Util.isNull(event)) throw new NullPointerException();
        for (Short order : listeners.keySet()) {
            if (!listeners.get(order).keySet().contains(event.getClass())) {
                for (SubPluginInfo plugin : listeners.get(order).get(event.getClass()).keySet()) {
                    try {
                        Field pf = Event.class.getDeclaredField("plugin");
                        pf.setAccessible(true);
                        pf.set(event, plugin);
                        pf.setAccessible(false);
                    } catch (Exception e) {
                        this.host.log.error.println(e);
                    }
                    for (Object listener : listeners.get(order).get(event.getClass()).get(plugin).keySet()) {
                        for (Method method : listeners.get(order).get(event.getClass()).get(plugin).get(listener)) {
                            if (event instanceof Cancellable && ((Cancellable) event).isCancelled() && !method.getAnnotation(EventHandler.class).override()) {
                                try {
                                    method.invoke(listener, event);
                                } catch (InvocationTargetException e) {
                                    this.host.log.error.println("Event \"" + method.getName() + "(" + event.getClass().getTypeName() + ")\" in class \"" + listener.getClass().getCanonicalName() + "\" had an unhandled exception:");
                                    this.host.log.error.println(e.getTargetException());
                                } catch (IllegalAccessException e) {
                                    this.host.log.error.println("Cannot access method \"" + method.getName() + "\" in class \"" + listener.getClass().getCanonicalName() + "\"");
                                    this.host.log.error.println(e);
                                }
                            }
                        }
                    }
                }
            }
        }
        try {
            Field pf = Event.class.getDeclaredField("plugin");
            pf.setAccessible(true);
            pf.set(event, null);
            pf.setAccessible(false);
        } catch (Exception e) {
            this.host.log.error.println(e);
        }
    }

    /**
     * Gets the current SubServers Lang Channels
     *
     * @return SubServers Lang Channel list
     */
    public Collection<String> getLangChannels() {
        return host.lang.get().keySet();
    }

    /**
     * Gets values from the SubServers Lang
     *
     * @param channel Lang Channel
     * @return Lang Value
     */
    public Map<String, String> getLang(String channel) {
        if (Util.isNull(channel)) throw new NullPointerException();
        return new LinkedHashMap<>(host.lang.get().get(channel.toLowerCase()));
    }

    /**
     * Gets a value from the SubServers Lang
     *
     * @param channel Lang Channel
     * @param key Key
     * @return Lang Values
     */
    public String getLang(String channel, String key) {
        if (Util.isNull(channel, key)) throw new NullPointerException();
        return getLang(channel).get(key);
    }

    /**
     * Gets the Runtime Directory
     *
     * @return Directory
     */
    public UniversalFile getRuntimeDirectory() {
        return host.dir;
    }

    /**
     * Gets the SubServers Beta Version
     *
     * @return SubServers Beta Version (or null if this is a release version)
     */
    public Version getBetaVersion() {
        return host.bversion;
    }

    /**
     * Gets the SubServers Version
     *
     * @return SubServers Version
     */
    public Version getAppVersion() {
        return host.version;
    }
}

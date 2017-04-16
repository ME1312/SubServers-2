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

/**
 * SubAPI Class
 */
public final class SubAPI {
    final HashMap<EventPriority, HashMap<Class<? extends Event>, HashMap<SubPluginInfo, HashMap<Object, List<Method>>>>> listeners = new LinkedHashMap<EventPriority, HashMap<Class<? extends Event>, HashMap<SubPluginInfo, HashMap<Object, List<Method>>>>>();
    final HashMap<UUID, Timer> schedule = new HashMap<UUID, Timer>();
    final TreeMap<String, Command> commands = new TreeMap<String, Command>();
    final HashMap<String, SubPluginInfo> plugins = new LinkedHashMap<String, SubPluginInfo>();
    private ExHost host;
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
        return schedule(plugin, run, -1L, -1L);
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
        if (Util.isNull(plugin, run, delay, repeat)) throw new NullPointerException();
        return schedule(new SubTask(plugin) {
            @Override
            public void run() {
                run.run();
            }
        }.delay(delay).repeat(repeat));
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
    public void addListener(SubPluginInfo plugin, Listener... listeners) {
        for (Listener listener : listeners) addListener(plugin, (Object) listener);
    }
    @SuppressWarnings("unchecked")
    void addListener(SubPluginInfo plugin, Object listener) {
        if (Util.isNull(plugin, listener)) throw new NullPointerException();
        for (Method method : Arrays.asList(listener.getClass().getMethods())) {
            if (!method.isAnnotationPresent(EventHandler.class)) continue;
            if (method.getParameterTypes().length == 1) {
                if (Event.class.isAssignableFrom(method.getParameterTypes()[0])) {
                    HashMap<Class<? extends Event>, HashMap<SubPluginInfo, HashMap<Object, List<Method>>>> events = (listeners.keySet().contains(method.getAnnotation(EventHandler.class).priority()))?listeners.get(method.getAnnotation(EventHandler.class).priority()):new LinkedHashMap<Class<? extends Event>, HashMap<SubPluginInfo, HashMap<Object, List<Method>>>>();
                    HashMap<SubPluginInfo, HashMap<Object, List<Method>>> plugins = (events.keySet().contains((Class<Event>) method.getParameterTypes()[0]))?events.get((Class<Event>) method.getParameterTypes()[0]):new LinkedHashMap<SubPluginInfo, HashMap<Object, List<Method>>>();
                    HashMap<Object, List<Method>> listeners = (plugins.keySet().contains(plugin))?plugins.get(plugin):new LinkedHashMap<Object, List<Method>>();
                    List<Method> methods = (listeners.keySet().contains(listener))?listeners.get(listener):new LinkedList<Method>();
                    methods.add(method);
                    listeners.put(listener, methods);
                    plugins.put(plugin, listeners);
                    events.put((Class<Event>) method.getParameterTypes()[0], plugins);
                    this.listeners.put(method.getAnnotation(EventHandler.class).priority(), events);
                } else {
                    this.host.log.error.println(
                            "Cannot register EventHandler in class \"" + listener.getClass().getCanonicalName() + "\" using method \"" + method.getName() + "\":",
                            "\"" + method.getParameterTypes()[0].getCanonicalName() + "\" is not a SubEvent");
                }
            } else {
                this.host.log.error.println(
                        "Cannot register EventHandler in class \"" + listener.getClass().getCanonicalName() + "\" using method \"" + method.getName() + "\":",
                        ((method.getParameterTypes().length > 0) ? "Too many" : "No") + " parameters for SubEvent to execute");
            }
        }
    }

    /**
     * Unregister SubEvent Listeners
     *
     * @param plugin PluginInfo
     * @param listeners Listeners
     */
    public void removeListener(SubPluginInfo plugin, Listener... listeners) {
        for (Listener listener : listeners) removeListener(plugin, (Object) listener);
    }
    void removeListener(SubPluginInfo plugin, Object listener) {
        if (Util.isNull(plugin, listener)) throw new NullPointerException();
        HashMap<EventPriority, HashMap<Class<? extends Event>, HashMap<SubPluginInfo, HashMap<Object, List<Method>>>>> map = new LinkedHashMap<EventPriority, HashMap<Class<? extends Event>, HashMap<SubPluginInfo, HashMap<Object, List<Method>>>>>(listeners);
        for (EventPriority priority : map.keySet()) {
            for (Class<? extends Event> event : map.get(priority).keySet()) {
                if (map.get(priority).get(event).keySet().contains(plugin) && map.get(priority).get(event).get(plugin).keySet().contains(listener)) {
                    HashMap<Class<? extends Event>, HashMap<SubPluginInfo, HashMap<Object, List<Method>>>> events = listeners.get(priority);
                    HashMap<SubPluginInfo, HashMap<Object, List<Method>>> plugins = listeners.get(priority).get(event);
                    HashMap<Object, List<Method>> listeners = this.listeners.get(priority).get(event).get(plugin);
                    listeners.remove(listener);
                    plugins.put(plugin, listeners);
                    events.put(event, plugins);
                    this.listeners.put(priority, events);
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
        for (EventPriority priority : listeners.keySet()) {
            if (!listeners.get(priority).keySet().contains(event.getClass())) continue;
            for (SubPluginInfo plugin : listeners.get(priority).get(event.getClass()).keySet()) {
                try {
                    Field pf = Event.class.getDeclaredField("plugin");
                    pf.setAccessible(true);
                    pf.set(event, plugin);
                    pf.setAccessible(false);
                } catch (Exception e) {
                    this.host.log.error.println(e);
                }
                for (Object listener : listeners.get(priority).get(event.getClass()).get(plugin).keySet()) {
                    for (Method method : listeners.get(priority).get(event.getClass()).get(plugin).get(listener)) {
                        if (event instanceof Cancellable && ((Cancellable) event).isCancelled() && !method.getAnnotation(EventHandler.class).override()) continue;
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
     * Gets a value from the SubServers Lang
     *
     * @param key Key
     * @return Lang Value
     */
    public String getLang(String key) {
        if (Util.isNull(key)) throw new NullPointerException();
        return getLang().get(key);
    }

    /**
     * Gets the SubServers Lang
     *
     * @return SubServers Lang
     */
    public Map<String, String> getLang() {
        HashMap<String, String> lang = new HashMap<String, String>();
        for (String key : host.lang.getSection("Lang").getKeys()) {
            if (host.lang.getSection("Lang").isString(key)) lang.put(key, host.lang.getSection("Lang").getString(key));
        }
        return lang;
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

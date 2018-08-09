package net.ME1312.SubServers.Host;

import net.ME1312.SubServers.Host.API.Command;
import net.ME1312.SubServers.Host.API.SubPluginInfo;
import net.ME1312.SubServers.Host.API.SubTask;
import net.ME1312.SubServers.Host.Library.Callback;
import net.ME1312.SubServers.Host.Library.Event.*;
import net.ME1312.SubServers.Host.Library.NamedContainer;
import net.ME1312.SubServers.Host.Library.UniversalFile;
import net.ME1312.SubServers.Host.Library.Util;
import net.ME1312.SubServers.Host.Library.Version.Version;
import net.ME1312.SubServers.Host.Network.API.Host;
import net.ME1312.SubServers.Host.Network.API.Proxy;
import net.ME1312.SubServers.Host.Network.API.Server;
import net.ME1312.SubServers.Host.Network.API.SubServer;
import net.ME1312.SubServers.Host.Network.Packet.*;
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
    final List<String> knownClasses = new ArrayList<String>();
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
     * Gets the Hosts
     *
     * @param callback Host Map
     */
    public void getHosts(Callback<Map<String, Host>> callback) {
        if (Util.isNull(callback)) throw new NullPointerException();
        StackTraceElement[] origin = new Exception().getStackTrace();
        host.subdata.sendPacket(new PacketDownloadHostInfo(null, data -> {
            TreeMap<String, Host> hosts = new TreeMap<String, Host>();
            for (String host : data.getSection("hosts").getKeys()) {
                hosts.put(host.toLowerCase(), new Host(data.getSection("hosts").getSection(host)));
            }

            try {
                callback.run(hosts);
            } catch (Throwable e) {
                Throwable ew = new InvocationTargetException(e);
                ew.setStackTrace(origin);
                host.log.error.println(ew);
            }
        }));
    }

    /**
     * Gets a Host
     *
     * @param name Host name
     * @param callback a Host
     */
    public void getHost(String name, Callback<Host> callback) {
        if (Util.isNull(name, callback)) throw new NullPointerException();
        StackTraceElement[] origin = new Exception().getStackTrace();
        host.subdata.sendPacket(new PacketDownloadHostInfo(name, data -> {
            Host host = null;
            if (data.getSection("hosts").getKeys().size() > 0) {
                host = new Host(data.getSection("hosts").getSection(new LinkedList<String>(data.getSection("hosts").getKeys()).getFirst()));
            }

            try {
                callback.run(host);
            } catch (Throwable e) {
                Throwable ew = new InvocationTargetException(e);
                ew.setStackTrace(origin);
                this.host.log.error.println(ew);
            }
        }));
    }

    /**
     * Gets the Server Groups (Group names are case sensitive here)
     *
     * @param callback Group Map
     */
    public void getGroups(Callback<Map<String, List<Server>>> callback) {
        if (Util.isNull(callback)) throw new NullPointerException();
        StackTraceElement[] origin = new Exception().getStackTrace();
        host.subdata.sendPacket(new PacketDownloadGroupInfo(null, data -> {
            TreeMap<String, List<Server>> groups = new TreeMap<String, List<Server>>();
            for (String group : data.getSection("groups").getKeys()) {
                ArrayList<Server> servers = new ArrayList<Server>();
                for (String server : data.getSection("groups").getSection(group).getKeys()) {
                    if (data.getSection("groups").getSection(group).getSection(server).getRawString("type", "Server").equals("SubServer")) {
                        servers.add(new SubServer(data.getSection("groups").getSection(group).getSection(server)));
                    } else {
                        servers.add(new Server(data.getSection("groups").getSection(group).getSection(server)));
                    }
                }
                if (servers.size() > 0) groups.put(group, servers);
            }

            try {
                callback.run(groups);
            } catch (Throwable e) {
                Throwable ew = new InvocationTargetException(e);
                ew.setStackTrace(origin);
                host.log.error.println(ew);
            }
        }));
    }

    /**
     * Gets the Server Groups (Group names are all lowercase here)
     *
     * @param callback Group Map
     */
    public void getLowercaseGroups(Callback<Map<String, List<Server>>> callback) {
        if (Util.isNull(callback)) throw new NullPointerException();
        getGroups(groups -> {
            TreeMap<String, List<Server>> lowercaseGroups = new TreeMap<String, List<Server>>();
            for (String key : groups.keySet()) {
                lowercaseGroups.put(key.toLowerCase(), groups.get(key));
            }
            callback.run(lowercaseGroups);
        });
    }

    /**
     * Gets a Server Group (Group names are case insensitive here)
     *
     * @param name Group name
     * @param callback a Server Group
     */
    public void getGroup(String name, Callback<List<Server>> callback) {
        if (Util.isNull(name, callback)) throw new NullPointerException();
        StackTraceElement[] origin = new Exception().getStackTrace();
        host.subdata.sendPacket(new PacketDownloadGroupInfo(name, data -> {
            List<Server> servers = null;
            if (data.getSection("groups").getKeys().size() > 0) {
                String key = new LinkedList<String>(data.getSection("groups").getKeys()).getFirst();
                servers = new ArrayList<Server>();
                for (String server : data.getSection("groups").getSection(key).getKeys()) {
                    if (data.getSection("groups").getSection(key).getSection(server).getRawString("type", "Server").equals("SubServer")) {
                        servers.add(new SubServer(data.getSection("groups").getSection(key).getSection(server)));
                    } else {
                        servers.add(new Server(data.getSection("groups").getSection(key).getSection(server)));
                    }
                }
            }

            try {
                callback.run(servers);
            } catch (Throwable e) {
                Throwable ew = new InvocationTargetException(e);
                ew.setStackTrace(origin);
                host.log.error.println(ew);
            }
        }));
    }

    /**
     * Gets the Servers (including SubServers)
     *
     * @param callback Server Map
     */
    public void getServers(Callback<Map<String, Server>> callback) {
        if (Util.isNull(callback)) throw new NullPointerException();
        StackTraceElement[] origin = new Exception().getStackTrace();
        host.subdata.sendPacket(new PacketDownloadServerInfo(null, data -> {
            TreeMap<String, Server> servers = new TreeMap<String, Server>();
            for (String server : data.getSection("servers").getKeys()) {
                if (data.getSection("servers").getSection(server).getRawString("type", "Server").equals("SubServer")) {
                    servers.put(server.toLowerCase(), new SubServer(data.getSection("servers").getSection(server)));
                } else {
                    servers.put(server.toLowerCase(), new Server(data.getSection("servers").getSection(server)));
                }
            }

            try {
                callback.run(servers);
            } catch (Throwable e) {
                Throwable ew = new InvocationTargetException(e);
                ew.setStackTrace(origin);
                host.log.error.println(ew);
            }
        }));
    }

    /**
     * Gets a Server
     *
     * @param name Server name
     * @param callback a Server
     */
    public void getServer(String name, Callback<Server> callback) {
        if (Util.isNull(name, callback)) throw new NullPointerException();
        StackTraceElement[] origin = new Exception().getStackTrace();
        host.subdata.sendPacket(new PacketDownloadServerInfo(name, data -> {
            Server server = null;
            if (data.getSection("servers").getKeys().size() > 0) {
                String key = new LinkedList<String>(data.getSection("servers").getKeys()).getFirst();
                if (data.getSection("servers").getSection(key).getRawString("type", "Server").equals("SubServer")) {
                    server = new SubServer(data.getSection("servers").getSection(key));
                } else {
                    server = new Server(data.getSection("servers").getSection(key));
                }
            }

            try {
                callback.run(server);
            } catch (Throwable e) {
                Throwable ew = new InvocationTargetException(e);
                ew.setStackTrace(origin);
                host.log.error.println(ew);
            }
        }));
    }

    /**
     * Gets the SubServers
     *
     * @param callback SubServer Map
     */
    public void getSubServers(Callback<Map<String, SubServer>> callback) {
        if (Util.isNull(callback)) throw new NullPointerException();
        getServers(servers -> {
            TreeMap<String, SubServer> subservers = new TreeMap<String, SubServer>();
            for (String server : servers.keySet()) {
                if (servers.get(server) instanceof SubServer) subservers.put(server, (SubServer) servers.get(server));
            }
            callback.run(subservers);
        });
    }

    /**
     * Gets a SubServer
     *
     * @param name SubServer name
     * @param callback a SubServer
     */
    public void getSubServer(String name, Callback<SubServer> callback) {
        if (Util.isNull(name, callback)) throw new NullPointerException();
        getServer(name, server -> callback.run((server instanceof SubServer)?(SubServer) server:null));
    }

    /**
     * Gets the known Proxies
     *
     * @param callback Proxy Map
     */
    public void getProxies(Callback<Map<String, Proxy>> callback) {
        if (Util.isNull(callback)) throw new NullPointerException();
        StackTraceElement[] origin = new Exception().getStackTrace();
        host.subdata.sendPacket(new PacketDownloadProxyInfo(null, data -> {
            TreeMap<String, Proxy> proxies = new TreeMap<String, Proxy>();
            for (String proxy : data.getSection("proxies").getKeys()) {
                proxies.put(proxy.toLowerCase(), new Proxy(data.getSection("proxies").getSection(proxy)));
            }

            try {
                callback.run(proxies);
            } catch (Throwable e) {
                Throwable ew = new InvocationTargetException(e);
                ew.setStackTrace(origin);
                host.log.error.println(ew);
            }
        }));
    }

    /**
     * Gets a Proxy
     *
     * @param name Proxy name
     * @param callback a Proxy
     */
    public void getProxy(String name, Callback<Proxy> callback) {
        if (Util.isNull(name, callback)) throw new NullPointerException();
        StackTraceElement[] origin = new Exception().getStackTrace();
        host.subdata.sendPacket(new PacketDownloadProxyInfo(name, data -> {
            Proxy proxy = null;
            if (data.getSection("proxies").getKeys().size() > 0) {
                proxy = new Proxy(data.getSection("proxies").getSection(new LinkedList<String>(data.getSection("proxies").getKeys()).getFirst()));
            }

            try {
                callback.run(proxy);
            } catch (Throwable e) {
                Throwable ew = new InvocationTargetException(e);
                ew.setStackTrace(origin);
                host.log.error.println(ew);
            }
        }));
    }

    /**
     * Get the Master Proxy redis container (null if unavailable)
     *
     * @param callback Master Proxy
     */
    public void getMasterProxy(Callback<Proxy> callback) {
        if (Util.isNull(callback)) throw new NullPointerException();
        StackTraceElement[] origin = new Exception().getStackTrace();
        host.subdata.sendPacket(new PacketDownloadProxyInfo("", data -> {
            Proxy proxy = null;
            if (data.getKeys().contains("master")) {
                proxy = new Proxy(data.getSection("master"));
            }

            try {
                callback.run(proxy);
            } catch (Throwable e) {
                Throwable ew = new InvocationTargetException(e);
                ew.setStackTrace(origin);
                host.log.error.println(ew);
            }
        }));
    }

    /**
     * Get players on this network across all known proxies
     *
     * @param callback Player Collection
     */
    @SuppressWarnings("unchecked")
    public void getGlobalPlayers(Callback<Collection<NamedContainer<String, UUID>>> callback) {
        if (Util.isNull(callback)) throw new NullPointerException();
        StackTraceElement[] origin = new Exception().getStackTrace();
        host.subdata.sendPacket(new PacketDownloadPlayerList(data -> {
            List<NamedContainer<String, UUID>> players = new ArrayList<NamedContainer<String, UUID>>();
            for (String id : data.getSection("players").getKeys()) {
                players.add(new NamedContainer<String, UUID>(data.getSection("players").getSection(id).getRawString("name"), UUID.fromString(id)));
            }

            try {
                callback.run(players);
            } catch (Throwable e) {
                Throwable ew = new InvocationTargetException(e);
                ew.setStackTrace(origin);
                host.log.error.println(ew);
            }
        }));
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
            commands.put(handle.toLowerCase(), command);
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
    public void addListener(SubPluginInfo plugin, Object... listeners) {
        for (Object listener : listeners) {
            if (Util.isNull(plugin, listener)) throw new NullPointerException();
            for (Method method : Arrays.asList(listener.getClass().getMethods())) {
                if (method.isAnnotationPresent(EventHandler.class)) {
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
                            plugin.getLogger().error.println(
                                    "Cannot register listener \"" + listener.getClass().getCanonicalName() + '.' + method.getName() + "(" + method.getParameterTypes()[0].getCanonicalName() + ")\":",
                                    "\"" + method.getParameterTypes()[0].getCanonicalName() + "\" is not an Event");
                        }
                    } else {
                        LinkedList<String> args = new LinkedList<String>();
                        for (Class<?> clazz : method.getParameterTypes()) args.add(clazz.getCanonicalName());
                        plugin.getLogger().error.println(
                                "Cannot register listener \"" + listener.getClass().getCanonicalName() + '.' + method.getName() + "(" + args.toString().substring(1, args.toString().length() - 1) + ")\":",
                                ((method.getParameterTypes().length > 0) ? "Too many" : "No") + " parameters for method to be executed");
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
            if (listeners.get(order).keySet().contains(event.getClass())) {
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
                            if (!(event instanceof Cancellable) || !((Cancellable) event).isCancelled() || method.getAnnotation(EventHandler.class).override()) {
                                try {
                                    method.invoke(listener, event);
                                } catch (InvocationTargetException e) {
                                    plugin.getLogger().error.println("Event listener \"" + listener.getClass().getCanonicalName() + '.' + method.getName() + "(" + event.getClass().getTypeName() + ")\" had an unhandled exception:");
                                    plugin.getLogger().error.println(e.getTargetException());
                                } catch (IllegalAccessException e) {
                                    plugin.getLogger().error.println("Cannot access method \"" + listener.getClass().getCanonicalName() + '.' + method.getName() + "(" + event.getClass().getTypeName() + ")\"");
                                    plugin.getLogger().error.println(e);
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
     * Gets the SubServers Version
     *
     * @return SubServers Version
     */
    public Version getAppVersion() {
        return host.version;
    }

    /**
     * Gets the SubServers Build Signature
     *
     * @return SubServers Build Signature (or null if unsigned)
     */
    public Version getAppBuild() {
        return (ExHost.class.getPackage().getSpecificationTitle() != null)?new Version(ExHost.class.getPackage().getSpecificationTitle()):null;
    }
}

package net.ME1312.SubServers.Client.Bukkit.Library;

import net.ME1312.Galaxi.Library.Access;
import net.ME1312.Galaxi.Library.AsyncConsolidator;
import net.ME1312.Galaxi.Library.Container.ContainedPair;
import net.ME1312.Galaxi.Library.Container.Container;
import net.ME1312.Galaxi.Library.Container.Pair;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Try;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubServers.Client.Bukkit.Event.*;
import net.ME1312.SubServers.Client.Bukkit.SubAPI;
import net.ME1312.SubServers.Client.Bukkit.SubPlugin;
import net.ME1312.SubServers.Client.Common.Network.API.*;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitTask;

import java.lang.invoke.MethodHandle;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Placeholder Executor Class
 */
public final class Placeholders {
    private final CopyOnWriteArrayList<Runnable> listeners;
    private final SubPlugin plugin;
    public final Cache cache;
    private MethodHandle papi;
    private BukkitTask task;
    private boolean init;

    /**
     * Create a Placeholder Executor Instance
     *
     * @param plugin SubPlugin
     */
    public Placeholders(SubPlugin plugin) {
        this.plugin = plugin;
        this.listeners = new CopyOnWriteArrayList<>();
        this.cache = new Cache();
        this.init = false;
    }

    public void start() {
        if (!init) {
            init = true;
            papi = Try.all.get(() -> Access.shared.type(Class.forName("me.clip.placeholderapi.PlaceholderAPI")).method(String.class, "setPlaceholders").parameters(OfflinePlayer.class, String.class).handle());
            Bukkit.getPluginManager().registerEvents(cache.events, plugin);
            Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
                if (task == null) {
                    int interval = plugin.config.get().getMap("Settings").getInt("PlaceholderAPI-Cache-Interval", 30);
                    int start = interval - new Random().nextInt(interval + 1); // Don't have all servers request at the same time
                    Runnable task = () -> cache.refresh(() -> {
                        for (Runnable listener : listeners) {
                            try {
                                listener.run();
                            } catch (Throwable e) {
                                e.printStackTrace();
                            }
                        }
                    });
                    this.task = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, task, 20L * start, 20L * interval);
                    task.run();
                }
            }, 120L);
        }
    }

    public void stop() {
        if (task != null) {
            try {
                task.cancel();
            } catch (Throwable exception) {}
            task = null;
        }
    }

    public void clear() {
        cache.reset();
    }

    public void listen(Runnable task) {
        listeners.add(task);
    }

    public String request(OfflinePlayer player, String request) {
        boolean colored = !request.startsWith("plain_");
        if (!colored || request.startsWith("color_")) request = request.substring(6);
        if (!init) start();

        String response = runMethod(player, request);

        if (response != null && !colored) {
            return ChatColor.stripColor(response);
        } else {
            return response;
        }
    }

    private static final Pattern replacements = Pattern.compile("#?([^\\s#]+?\\(.*?\\))|\\$([^$]+)\\$", Pattern.CASE_INSENSITIVE);
    private String[] arguments(OfflinePlayer player, String text, boolean replace) {
        LinkedList<String> arguments = new LinkedList<>();

        if (text != null && !text.isEmpty()) {
            Pattern p = replacements;
            Matcher m = p.matcher(text);

            StringBuilder argument = new StringBuilder();
            while (m.find()) {
                String[] replacement = findMethod(player, text, m.start(), replace);

                if (replacement[0].contains(",")) {
                    String[] s = replacement[0].split(",");
                    argument.append(s[0]);
                    arguments.add(argument.toString().trim());

                    for (int i = 1; i < s.length - 1; ++i) {
                        arguments.add(s[i].trim());
                    }

                    argument = new StringBuilder();
                    argument.append(s[s.length - 1]);
                } else {
                    argument.append(replacement[0]);
                }

                argument.append(replacement[1]);

                text = replacement[2];
                m = p.matcher(text);
            }

            if (text.contains(",")) {
                String[] s = text.split(",");
                argument.append(s[0]);
                arguments.add(argument.toString().trim());
                argument = null;

                for (int i = 1; i < s.length; ++i) {
                    arguments.add(s[i].trim());
                }
            } else if (text.length() > 0) {
                argument.append(text);
            }

            if (argument != null && argument.length() > 0) {
                arguments.add(argument.toString().trim());
            }
        }

        return arguments.toArray(new String[0]);
    }

    public String replace(OfflinePlayer player, String text) {
        if (text != null) {
            Pattern p = replacements;
            Matcher m = p.matcher(text);

            StringBuilder str = new StringBuilder();
            while (m.find()) {
                String[] replacement = findMethod(player, text, m.start(), true);

                str.append(replacement[0]);
                str.append(replacement[1]);

                text = replacement[2];
                m = p.matcher(text);
            }

            str.append(text);
            return str.toString();
        } else {
            return null;
        }
    }

    private String[] findMethod(OfflinePlayer player, String text, int start, boolean run) {
        String[] values = new String[3];
        values[0] = text.substring(0, start);
        text = text.substring(start);

        int[] open =  {'(', '<'};
        int[] close = {')', '>'};
        Arrays.sort(open);
        Arrays.sort(close);

        int i = -1;
        if (text.codePointAt(0) == '$') {
            Matcher m = Pattern.compile("^\\$([^$]+)\\$", Pattern.CASE_INSENSITIVE).matcher(text);
            if (m.find()) {
                String str = '%' + m.group(1) + '%';
                text = text.substring(m.end());
                if (run) {
                    String response = (papi == null)?null:Try.all.get(() -> (String) papi.invokeExact(player, str));
                    values[1] = (response == null)?m.group():response;
                } else {
                    values[1] = m.group();
                }
            }
        } else {
            ++i;
            boolean responses = false;
            StringBuilder str = new StringBuilder();
            for (int level = 0; i < text.codePoints().count(); ++i) {
                int c = text.codePointAt(i);
                str.appendCodePoint(c);

                if (Arrays.binarySearch(open, c) >= 0) {
                    ++level;
                } else if (Arrays.binarySearch(close, c) >= 0) {
                    --level;
                    if (level <= 0) {
                        if (responses) break;
                        boolean more = false;
                        for (int ix = i + 1; ix < text.codePoints().count(); ++ix) {
                            int cx = text.codePointAt(ix);
                            if (!Character.isWhitespace(cx) && cx != '_') {
                                more = cx == '<';
                                break;
                            }
                        }
                        if (!more) break;
                        else responses = true;
                    }
                }
            }
            if (run) {
                String response = runMethod(player, str.toString());
                values[1] = (response == null)?str.toString():response;
            } else {
                values[1] = str.toString();
            }
        }

        StringBuilder str = new StringBuilder();
        for (i += 1; i < text.codePoints().count(); ++i) {
            str.appendCodePoint(text.codePointAt(i));
        }
        values[2] = str.toString();

        return values;
    }

    private String[] parseMethod(OfflinePlayer player, String text) {
        Matcher m = Pattern.compile("^#?(.+?)(?:[\\s_]*\\((.*?)\\))?(?:[\\s_]*<(.*)>)?$", Pattern.CASE_INSENSITIVE).matcher(text);

        if (m.find()) {
            String[] values = new String[3];

            values[0] = m.group(1);
            if (m.group(2) == null || m.group(2).trim().isEmpty() ||
                    m.group(3) == null || m.group(3).trim().isEmpty()) {
                // Simple parsing: () or <>
                values[1] = m.group(2);
                values[2] = m.group(3);
            } else {
                // Complex parsing: () and <>
                text = text.substring(m.end(1));
                int stage = 1, level = 0, i = 0;
                char open = '(', close = ')';
                boolean responses = false;
                StringBuilder str = new StringBuilder();
                for (; i < text.codePoints().count(); ++i) {
                    int c = text.codePointAt(i);
                    if (c == open) {
                        if (level > 0) str.appendCodePoint(c);
                        ++level;
                    } else if (c == close) {
                        --level;
                        if (level > 0) str.appendCodePoint(c);
                        else {
                            if (responses) break;
                            boolean more = false;
                            for (int ix = i + 1; ix < text.codePoints().count(); ++ix) {
                                int cx = text.codePointAt(ix);
                                if (!Character.isWhitespace(cx) && cx != '_') {
                                    more = cx == '<';
                                    break;
                                }
                            }
                            if (!more) break;
                            else {
                                responses = true;
                                open = '<'; close = '>';
                                values[stage++] = str.toString();
                                str = new StringBuilder();
                            }
                        }
                    } else {
                        if (level > 0) str.appendCodePoint(c);
                    }
                }
                values[stage] = str.toString();
                if (level > 0 || ++i < text.codePoints().count()) {
                    return null;
                }
            }

            return values;
        } else {
            return null;
        }
    }

    private String runMethod(OfflinePlayer player, String text) {
        String[] parsed = parseMethod(player, text);

        if (parsed != null) {
            String method = parsed[0];
            String[] args = arguments(player, parsed[1], true);
            String[] responses = arguments(player, parsed[2], false);

            for (int i = 0; i < responses.length; ++i)
                responses[i] = ChatColor.translateAlternateColorCodes('&', responses[i].trim());

            return replace(player, runMethod(player, method, args, responses));
        } else {
            return null;
        }
    }

    @SuppressWarnings("ConstantConditions")
    private String runMethod(OfflinePlayer player, String method, String[] args, String[] responses) {
        Server server = (plugin.api.getName() != null)? cache.getServer(plugin.api.getName()) : null;
        SubServer subserver = (server instanceof SubServer)? (SubServer) server : null;
        Pair<String, List<Server>> group = null;
        Host host = (subserver != null)? cache.getHost(subserver.getHost()) : null;
        Proxy proxy = cache.getMasterProxy();

        method = method.toLowerCase();
        if (method.startsWith("proxy.")) {
            if (args.length > 0 && !args[0].isEmpty()) proxy = cache.getProxy(args[0]);
            if (proxy == null) return null;
        } else if (method.startsWith("host.")) {
            if (args.length > 0 && !args[0].isEmpty()) host = cache.getHost(args[0]);
            if (host == null) return null;
        } else if (method.startsWith("group.")) {
            if (args.length > 0 && !args[0].isEmpty()) group = cache.getGroup(args[0]);
//          if (group == null) return null; // Empty groups are null
        } else if (method.startsWith("server.")) {
            if (args.length > 0 && !args[0].isEmpty()) server = cache.getServer(args[0]);
            if (server == null) return null;
        } else if (method.startsWith("subserver.")) {
            if (args.length > 0 && !args[0].isEmpty()) server = subserver = cache.getSubServer(args[0]);
            if (subserver == null) return null;
        }


        // --- Methods where Objects link to other Objects --
        if (method.startsWith("subserver.host")) {
            if (method.equals("subserver.host")) {
                return subserver.getHost();
            } else {
                LinkedList<String> arguments = new LinkedList<String>();
                arguments.addAll(Arrays.asList(args));
                if (args.length > 0) arguments.removeFirst();
                arguments.addFirst(subserver.getHost());
                return runMethod(player, method.substring(10), arguments.toArray(new String[0]), responses);
            }
        } else if (method.startsWith("subserver.template")) {
            if (method.equals("subserver.template")) {
                return (subserver.getTemplate() != null)?subserver.getTemplate():defaults(responses, "(custom)")[0];
            } else if (subserver.getTemplate() != null) {
                LinkedList<String> arguments = new LinkedList<String>();
                arguments.addAll(Arrays.asList(args));
                if (args.length > 0) arguments.removeFirst();
                arguments.addFirst(subserver.getTemplate());
                arguments.addFirst(subserver.getHost());
                return runMethod(player, "host.creator." + method.substring(10), arguments.toArray(new String[0]), responses);
            } else {
                return null;
            }
        } else switch (method) { // --- Straight up Methods ---
            case "example": {
                return defaults(responses, ChatColor.LIGHT_PURPLE+"Example!")[0];
            }
            case "players": {
                int i = cache.getMasterProxy().getPlayers().size();
                for (Proxy p : cache.getProxies().values()) i += p.getPlayers().size();
                return Integer.toString(i);
            }
            case "proxy":
            case "proxies": {
                return Integer.toString(cache.getProxies().size() + 1);
            }
            case "proxy.displayname": {
                return proxy.getDisplayName();
            }
            case "proxy.type": {
                return defaults(responses, "Master Proxy", "Proxy") [((proxy.isMaster())?0:1)];
            }
            case "proxy.players": {
                return Integer.toString(proxy.getPlayers().size());
            }
            case "proxy.subdata": {
                return defaults(responses, ChatColor.GREEN+"Connected", ChatColor.RED+"Disconnected") [(proxy.getSubData()[0] == null)?1:0];
            }
            case "proxy.subdata.channels":
            case "proxy.subdata.subchannels": {
                return Integer.toString(proxy.getSubData().length - ((method.endsWith(".subchannels"))?1:0));
            }
            case "proxy.signature": {
                return proxy.getSignature();
            }
            case "host":
            case "hosts": {
                return Integer.toString(cache.getHosts().size());
            }
            case "host.displayname": {
                return host.getDisplayName();
            }
            case "host.available": {
                return defaults(responses, ChatColor.GREEN+"Available", ChatColor.RED+"Unavailable") [(host.isAvailable())?0:1];
            }
            case "host.enabled": {
                return defaults(responses, ChatColor.GREEN+"Enabled", ChatColor.RED+"Disabled") [(host.isEnabled())?0:1];
            }
            case "host.address": {
                return host.getAddress().getHostAddress();
            }
            case "host.creator.template":
            case "host.subcreator.template":
            case "host.creator.templates":
            case "host.subcreator.templates": {
                return Integer.toString(host.getCreator().getTemplates().size());
            }
            case "host.creator.template.displayname":
            case "host.subcreator.template.displayname": {
                SubCreator.ServerTemplate template = (args.length > 1 && !args[1].isEmpty())? host.getCreator().getTemplate(args[1]) : null;
                if (template != null) return template.getDisplayName();
                else return null;
            }
            case "host.creator.template.enabled":
            case "host.subcreator.template.enabled": {
                SubCreator.ServerTemplate template = (args.length > 1 && !args[1].isEmpty())? host.getCreator().getTemplate(args[1]) : null;
                if (template != null) return defaults(responses, ChatColor.GREEN+"Enabled", ChatColor.RED+"Disabled") [((template.isEnabled())?0:1)];
                else return null;
            }
            case "host.creator.template.type":
            case "host.subcreator.template.type": {
                SubCreator.ServerTemplate template = (args.length > 1 && !args[1].isEmpty())? host.getCreator().getTemplate(args[1]) : null;
                if (template != null) return template.getType().toString();
                else return null;
            }
            case "host.creator.template.requiresversion":
            case "host.subcreator.template.requiresversion": {
                SubCreator.ServerTemplate template = (args.length > 1 && !args[1].isEmpty())? host.getCreator().getTemplate(args[1]) : null;
                if (template != null) return defaults(responses, ChatColor.GREEN+"Optional", ChatColor.YELLOW+"Required") [((template.requiresVersion())?1:0)];
                else return null;
            }
            case "host.creator.template.updatable":
            case "host.subcreator.template.updatable": {
                SubCreator.ServerTemplate template = (args.length > 1 && !args[1].isEmpty())? host.getCreator().getTemplate(args[1]) : null;
                if (template != null) return defaults(responses, ChatColor.GREEN+"Updatable", ChatColor.RED+"Not Updatable") [((template.canUpdate())?0:1)];
                else return null;
            }
            case "host.servers":
            case "host.subservers": {
                return Integer.toString(host.getSubServers().size());
            }
            case "host.players": {
                return Integer.toString(host.getRemotePlayers().size());
            }
            case "host.subdata": {
                return defaults(responses, ChatColor.GREEN+"Connected", ChatColor.YELLOW+"Unsupported", ChatColor.RED+"Disconnected") [(host.getSubData().length <= 0)?1:((host.getSubData()[0] == null)?2:0)];
            }
            case "host.subdata.channels":
            case "host.subdata.subchannels": {
                return Integer.toString(Math.max(host.getSubData().length - ((method.endsWith(".subchannels"))?1:0), 0));
            }
            case "host.signature": {
                return host.getSignature();
            }
            case "group":
            case "groups": {
                return Integer.toString(cache.getGroups().size());
            }
            case "group.servers": {
                return Integer.toString((group == null)?0:group.value().size());
            }
            case "group.players": {
                int i = 0;
                if (group != null) for (Server s : group.value()) i += s.getRemotePlayers().size();
                return Integer.toString(i);
            }
            case "server":
            case "servers": {
                return Integer.toString(cache.getServers().size());
            }
            case "server.displayname":
            case "subserver.displayname": {
                return server.getDisplayName();
            }
            case "server.type":
            case "subserver.type": {
                return defaults(responses, "Subserver", "Server") [((server instanceof SubServer)?0:1)];
            }
            case "server.groups":
            case "subserver.groups": {
                return Integer.toString(server.getGroups().size());
            }
            case "server.address":
            case "subserver.address": {
                return server.getAddress().getAddress().getHostAddress() + ':' + server.getAddress().getPort();
            }
            case "server.motd":
            case "subserver.motd": {
                return server.getMotd();
            }
            case "server.restricted":
            case "subserver.restricted": {
                return defaults(responses, ChatColor.GREEN+"Public", ChatColor.RED+"Private") [(server.isRestricted())?1:0];
            }
            case "server.hidden":
            case "subserver.hidden": {
                return defaults(responses, ChatColor.GREEN+"Visible", ChatColor.RED+"Hidden") [(server.isHidden())?1:0];
            }
            case "server.players":
            case "subserver.players": {
                return Integer.toString(server.getRemotePlayers().size());
            }
            case "server.subdata":
            case "subserver.subdata": {
                return defaults(responses, ChatColor.GREEN+"Connected", ChatColor.RED+"Disconnected") [(server.getSubData()[0] == null)?1:0];
            }
            case "server.subdata.channels":
            case "subserver.subdata.channels":
            case "server.subdata.subchannels":
            case "subserver.subdata.subchannels": {
                return Integer.toString(server.getSubData().length - ((method.endsWith(".subchannels"))?1:0));
            }
            case "server.signature":
            case "subserver.signature": {
                return server.getSignature();
            }
            case "subserver":
            case "subservers": {
                return Integer.toString(cache.getSubServers().size());
            }
            case "subserver.available": {
                return defaults(responses, ChatColor.GREEN+"Available", ChatColor.RED+"Unavailable") [(subserver.isAvailable())?0:1];
            }
            case "subserver.enabled": {
                return defaults(responses, ChatColor.GREEN+"Enabled", ChatColor.RED+"Disabled") [(subserver.isEnabled())?0:1];
            }
            case "subserver.editable": {
                return defaults(responses, ChatColor.GREEN+"Editable", ChatColor.RED+"Locked") [(subserver.isEditable())?0:1];
            }
            case "subserver.running": {
                return defaults(responses, ChatColor.GREEN+"Running", ChatColor.RED+"Offline") [(subserver.isRunning())?0:1];
            }
            case "subserver.online": {
                return defaults(responses, ChatColor.GREEN+"Online", ChatColor.YELLOW+"Starting", ChatColor.RED+"Offline") [(subserver.isOnline())?0:((subserver.isRunning())?1:2)];
            }
            case "subserver.logging": {
                return defaults(responses, ChatColor.GREEN+"Logging", ChatColor.RED+"Muted") [(subserver.isLogging())?0:1];
            }
            case "subserver.temporary": {
                return defaults(responses, ChatColor.GREEN+"Permanent", ChatColor.AQUA+"Temporary") [
                        (subserver.getStopAction() == SubServer.StopAction.REMOVE_SERVER || subserver.getStopAction() == SubServer.StopAction.RECYCLE_SERVER || subserver.getStopAction() == SubServer.StopAction.DELETE_SERVER)?1:0
                ];
            }
            case "subserver.stopaction": {
                return subserver.getStopAction().toString();
            }
            case "subserver.incompatibilities":
            case "subserver.incompatibilities.current": {
                List<String> list = (method.endsWith(".current"))?subserver.getCurrentIncompatibilities():subserver.getIncompatibilities();
                return Integer.toString(list.size());
            }
            default: {
                return null;
            }
        }
    }

    private static String[] defaults(String[] overrides, String... defaults) {
        for (int i = 0; i < defaults.length; ++i) {
            defaults[i] = (((i < overrides.length && overrides[i].length() > 0)?overrides:defaults)[i]);
        }
        return defaults;
    }

    public final class Cache {
        private Map<String, Proxy> proxies = new TreeMap<>();
        private Map<String, Host> hosts = new TreeMap<>();
        private Map<String, Server> servers = new TreeMap<>();
        private Proxy master = null;
        private Listener events = new Events();

        private void reset() {
            proxies.clear();
            hosts.clear();
            servers.clear();
            master = null;
        }

        private void refresh(Runnable callback) {
            if (SubAPI.getInstance().getSubDataNetwork()[0] != null) {
                Container<Boolean> order = new Container<>(null);
                AsyncConsolidator async = new AsyncConsolidator(() -> {
                    try {
                        Map<String, SubServer> servers;
                        for (Host host : hosts.values()) {
                            servers = Util.reflect(Host.class.getDeclaredField("servers"), host);
                            if (order.value) {
                                this.servers.putAll(servers);
                            } else {
                                servers.replaceAll((name, existing) -> {
                                    Server server = this.servers.getOrDefault(name, null);
                                    return (server instanceof SubServer)? (SubServer) server : existing;
                                });
                            }
                        }
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                    callback.run();
                });
                async.reserve(4);
                SubAPI.getInstance().getProxies(proxies -> {
                    this.proxies = proxies;
                    async.release();
                });
                SubAPI.getInstance().getMasterProxy(master -> {
                    this.master = master;
                    async.release();
                });
                SubAPI.getInstance().getHosts(hosts -> {
                    order.value = true;
                    this.hosts = hosts;
                    async.release();
                });
                SubAPI.getInstance().getServers(servers -> {
                    order.value = false;
                    this.servers = servers;
                    async.release();
                });
            }
        }

        private final class Events implements Listener {
            private HashMap<String, BukkitTask> edits = new HashMap<String, BukkitTask>();

            @EventHandler(priority = EventPriority.LOWEST)
            public void add(SubAddProxyEvent e) {
                SubAPI.getInstance().getProxy(e.getProxy(), proxy -> {
                    if (proxy != null) proxies.put(proxy.getName().toLowerCase(), proxy);
                });
            }

            @EventHandler(priority = EventPriority.LOWEST)
            public void add(SubAddHostEvent e) {
                SubAPI.getInstance().getHost(e.getHost(), host -> {
                    if (host != null) hosts.put(host.getName().toLowerCase(), host);
                });
            }

            @EventHandler(priority = EventPriority.LOWEST)
            public void add(SubAddServerEvent e) {
                add(e.getServer());
            }
            public void add(String s) {
                SubAPI.getInstance().getServer(s, server -> {
                    if (server != null) servers.put(server.getName().toLowerCase(), server);
                });
            }

            @EventHandler(priority = EventPriority.LOWEST)
            public void edit(SubEditServerEvent e) {
                String s = e.getServer().toLowerCase();
                if (edits.containsKey(s)) edits.get(s).cancel();
                edits.put(s, Bukkit.getScheduler().runTaskLater(plugin, servers.containsKey(s)? servers.get(s)::refresh : () -> add(s), 120L));
            }

            @EventHandler(priority = EventPriority.LOWEST)
            public void start(SubStartEvent e) {
                Server server = getServer(e.getServer());
                if (server != null) {
                    Try.all.run(() -> Util.<ObjectMap<String>>reflect(Server.class.getDeclaredField("raw"), server).set("running", true));
                    server.refresh();
                } else add(e.getServer());
            }

            @EventHandler(priority = EventPriority.LOWEST)
            public void started(SubStartedEvent e) {
                Server server = getServer(e.getServer());
                if (server != null) {
                    Try.all.run(() -> Util.<ObjectMap<String>>reflect(Server.class.getDeclaredField("raw"), server).set("online", true));
                    server.refresh();
                } else add(e.getServer());
            }

            @EventHandler(priority = EventPriority.LOWEST)
            public void stopping(SubStopEvent e) {
                Server server = getServer(e.getServer());
                if (server != null) {
                    Try.all.run(() -> Util.<ObjectMap<String>>reflect(Server.class.getDeclaredField("raw"), server).set("stopping", true));
                    server.refresh();
                } else add(e.getServer());
            }

            @EventHandler(priority = EventPriority.LOWEST)
            public void stopped(SubStoppedEvent e) {
                Server server = getServer(e.getServer());
                if (server != null) Try.all.run(() -> {
                    ObjectMap<String> raw = Util.reflect(Server.class.getDeclaredField("raw"), server);
                    raw.set("online", false);
                    raw.set("running", false);
                    raw.set("stopping", false);
                    server.refresh();
                }); else add(e.getServer());
            }
        }

        public Map<String, Proxy> getProxies() {
            return proxies;
        }

        public Proxy getProxy(String name) {
            Util.nullpo(name);
            Proxy proxy = getProxies().getOrDefault(name.toLowerCase(), null);
            if (proxy == null && master != null && master.getName().equalsIgnoreCase(name)) proxy = master;
            return proxy;
        }

        public Proxy getMasterProxy() {
            return master;
        }

        public Map<String, Host> getHosts() {
            return hosts;
        }

        public Host getHost(String name) {
            Util.nullpo(name);
            return getHosts().getOrDefault(name.toLowerCase(), null);
        }

        public Map<String, List<Server>> getGroups() {
            TreeMap<String, List<Server>> groups = new TreeMap<String, List<Server>>();
            HashMap<String, String> conflitresolver = new HashMap<String, String>();
            for (Server server : getServers().values()) {
                for (String name : server.getGroups()) {
                    String group = name;
                    if (conflitresolver.containsKey(name.toLowerCase())) {
                        group = conflitresolver.get(name.toLowerCase());
                    } else {
                        conflitresolver.put(name.toLowerCase(), name);
                    }
                    List<Server> list = (groups.containsKey(group))?groups.get(group):new ArrayList<Server>();
                    list.add(server);
                    groups.put(group, list);
                }
            }
            return groups;
        }

        public Map<String, List<Server>> getLowercaseGroups() {
            Map<String, List<Server>> groups = getGroups();
            TreeMap<String, List<Server>> lowercaseGroups = new TreeMap<String, List<Server>>();
            for (String key : groups.keySet()) {
                lowercaseGroups.put(key.toLowerCase(), groups.get(key));
            }
            return lowercaseGroups;
        }

        public Pair<String, List<Server>> getGroup(String name) {
            Util.nullpo(name);
            for (Map.Entry<String, List<Server>> group : getLowercaseGroups().entrySet()) {
                if (group.getKey().equalsIgnoreCase(name)) return new ContainedPair<>(group.getKey(), group.getValue());
            }
            return null;
        }

        public Map<String, Server> getServers() {
            return servers;
        }

        public Server getServer(String name) {
            Util.nullpo(name);
            return getServers().getOrDefault(name.toLowerCase(), null);
        }

        public Map<String, SubServer> getSubServers() {
            TreeMap<String, SubServer> servers = new TreeMap<String, SubServer>();
            for (Map.Entry<String, Server> server : this.servers.entrySet()) {
                if (server.getValue() instanceof SubServer) servers.put(server.getKey(), (SubServer) server.getValue());
            }
            return servers;
        }

        public SubServer getSubServer(String name) {
            Util.nullpo(name);
            return getSubServers().getOrDefault(name.toLowerCase(), null);
        }
    }
}

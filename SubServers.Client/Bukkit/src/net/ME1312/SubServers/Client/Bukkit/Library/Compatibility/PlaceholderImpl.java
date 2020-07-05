package net.ME1312.SubServers.Client.Bukkit.Library.Compatibility;

import me.clip.placeholderapi.expansion.Cacheable;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.clip.placeholderapi.expansion.Taskable;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubServers.Client.Bukkit.Event.*;
import net.ME1312.SubServers.Client.Bukkit.Network.API.*;
import net.ME1312.SubServers.Client.Bukkit.SubAPI;
import net.ME1312.SubServers.Client.Bukkit.SubPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PlaceholderAPI Implementation Class
 */
public class PlaceholderImpl extends PlaceholderExpansion implements Taskable, Cacheable {
    private SubPlugin plugin;
    private BukkitTask task;
    private Cache cache;
    private boolean init;

    /**
     * Create a PlaceholderAPI Implementation Instance
     *
     * @param plugin SubPlugin
     */
    public PlaceholderImpl(SubPlugin plugin) {
        this.plugin = plugin;
        this.cache = new Cache();
        this.init = false;

        if (plugin.config.get().getMap("Settings").getBoolean("PlaceholderAPI-Ready", false)) init();
    }

    @Override
    public String getIdentifier() {
        return "subservers";
    }

    @Override
    public String getAuthor() {
        return plugin.getDescription().getAuthors().get(0);
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    private void init() {
        if (!init) {
            init = true;
            Bukkit.getPluginManager().registerEvents(cache.events, plugin);
            Bukkit.getScheduler().runTaskLater(plugin, this::start, 120L);
        }
    }

    @Override
    public void start() {
        if (task == null) {
            int interval = plugin.config.get().getMap("Settings").getInt("PlaceholderAPI-Cache-Interval", 300);
            int start = interval - new Random().nextInt((interval / 3) + 1); // Don't have all servers request at the same time
            task = Bukkit.getScheduler().runTaskTimer(plugin, cache::refresh, 20L * start, 20L * interval);
            cache.refresh();
        }
    }

    @Override
    public void stop() {
        if (task != null) {
            try {
                task.cancel();
            } catch (Throwable exception) {}
            task = null;
        }
    }

    @Override
    public void clear() {
        cache.reset();
    }

    @Override
    public String onPlaceholderRequest(Player player, String request) {
        boolean colored = request.startsWith("color_");
        if (colored) request = request.substring(6);

        String response = parse(request);
        if (!init) init();

        if (response != null && !colored) {
            return ChatColor.stripColor(response);
        } else {
            return response;
        }
    }

    private String parse(String placeholder) {
        Matcher m = Pattern.compile("^(.+?)(?:\\((.*)\\))?$", Pattern.CASE_INSENSITIVE).matcher(placeholder);

        if (m.find()) {
            String method = m.group(1);
            String arguments = m.group(2);
            String[] args;

            if (arguments == null || arguments.isEmpty()) {
                args = new String[0];
            } else if (!arguments.contains(",")) {
                args = new String[]{ arguments };
            } else {
                args = arguments.split(",\\s*");
            }

            return run(method, args);
        } else {
            return null;
        }
    }

    @SuppressWarnings("ConstantConditions")
    private String run(String method, String... args) {
        Server server = (plugin.api.getName() != null)? cache.getServer(plugin.api.getName()) : null;
        SubServer subserver = (server instanceof SubServer)? (SubServer) server : null;
        Host host = (subserver != null)? cache.getHost(subserver.getHost()) : null;
        Proxy proxy = cache.getMasterProxy();

        method = method.toLowerCase();
        if (method.startsWith("proxy.")) {
            if (args.length > 0 && !args[0].isEmpty()) proxy = cache.getProxy(args[0]);
            if (proxy == null) return null;
        } else if (method.startsWith("host.")) {
            if (args.length > 0 && !args[0].isEmpty()) host = cache.getHost(args[0]);
            if (host == null) return null;
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
                return run(method.substring(10), arguments.toArray(new String[0]));
            }
        } else if (method.startsWith("subserver.template")) {
            if (method.equals("subserver.template")) {
                return (subserver.getTemplate() != null)?subserver.getTemplate():"(custom)";
            } else if (subserver.getTemplate() != null) {
                LinkedList<String> arguments = new LinkedList<String>();
                arguments.addAll(Arrays.asList(args));
                if (args.length > 0) arguments.removeFirst();
                arguments.addFirst(subserver.getTemplate());
                arguments.addFirst(subserver.getHost());
                return run("host.creator." + method.substring(10), arguments.toArray(new String[0]));
            } else {
                return null;
            }
        } else switch (method) { // --- Straight up Methods ---
            case "example": {
                return ChatColor.LIGHT_PURPLE + "Example!";
            }
            case "proxy":
            case "proxies": {
                return ChatColor.AQUA + Integer.toString(cache.getProxies().size() + 1);
            }
            case "proxy.displayname": {
                return proxy.getDisplayName();
            }
            case "proxy.type": {
                return ((proxy.isMaster())?"Master ":"") + "Proxy";
            }
            case "proxy.redis": {
                return (proxy.isRedis())?ChatColor.GREEN+"Available":ChatColor.RED+"Unavailable";
            }
            case "proxy.players": {
                return ChatColor.AQUA + Integer.toString(proxy.getPlayers().size());
            }
            case "proxy.subdata": {
                return (proxy.getSubData()[0] == null)?ChatColor.RED+"Disconnected":ChatColor.GREEN+"Connected";
            }
            case "proxy.subdata.channels":
            case "proxy.subdata.subchannels": {
                return ChatColor.AQUA + Integer.toString(proxy.getSubData().length - ((method.endsWith(".subchannels"))?1:0));
            }
            case "proxy.signature": {
                return ChatColor.AQUA + proxy.getSignature();
            }
            case "host":
            case "hosts": {
                return ChatColor.AQUA + Integer.toString(cache.getHosts().size());
            }
            case "host.displayname": {
                return host.getDisplayName();
            }
            case "host.available": {
                return (host.isAvailable())?ChatColor.GREEN+"Available":ChatColor.RED+"Unavailable";
            }
            case "host.enabled": {
                return (host.isEnabled())?ChatColor.GREEN+"Enabled":ChatColor.RED+"Disabled";
            }
            case "host.address": {
                return host.getAddress().getHostAddress();
            }
            case "host.creator.template":
            case "host.subcreator.template":
            case "host.creator.templates":
            case "host.subcreator.templates": {
                return ChatColor.AQUA + Integer.toString(host.getCreator().getTemplates().size());
            }
            case "host.creator.template.displayname":
            case "host.subcreator.template.displayname": {
                SubCreator.ServerTemplate template = (args.length > 1 && !args[1].isEmpty())? host.getCreator().getTemplate(args[1]) : null;
                if (template != null) return template.getDisplayName();
                else return null;
            }
            case "host.creator.template.type":
            case "host.subcreator.template.type": {
                SubCreator.ServerTemplate template = (args.length > 1 && !args[1].isEmpty())? host.getCreator().getTemplate(args[1]) : null;
                if (template != null) return template.getType().toString();
                else return null;
            }
            case "host.creator.template.updatable":
            case "host.subcreator.template.updatable": {
                SubCreator.ServerTemplate template = (args.length > 1 && !args[1].isEmpty())? host.getCreator().getTemplate(args[1]) : null;
                if (template != null) return ((template.canUpdate())?ChatColor.GREEN:ChatColor.RED+"Not ") + "Updatable";
                else return null;
            }
            case "host.servers":
            case "host.subservers": {
                return ChatColor.AQUA + Integer.toString(host.getSubServers().size());
            }
            case "host.players": {
                return ChatColor.AQUA + Integer.toString(host.getGlobalPlayers().size());
            }
            case "host.subdata": {
                return (host.getSubData().length <= 0)?ChatColor.YELLOW+"Unsupported":((host.getSubData()[0] == null)?ChatColor.RED+"Disconnected":ChatColor.GREEN+"Connected");
            }
            case "host.subdata.channels":
            case "host.subdata.subchannels": {
                return ChatColor.AQUA + Integer.toString(Math.max(host.getSubData().length - ((method.endsWith(".subchannels"))?1:0), 0));
            }
            case "host.signature": {
                return ChatColor.AQUA + host.getSignature();
            }
            case "server":
            case "servers": {
                return ChatColor.AQUA + Integer.toString(cache.getServers().size());
            }
            case "server.displayname":
            case "subserver.displayname": {
                return server.getDisplayName();
            }
            case "server.type":
            case "subserver.type": {
                return ((server instanceof SubServer)?"Subs":"S")+"erver";
            }
            case "server.groups":
            case "subserver.groups": {
                return ChatColor.AQUA + Integer.toString(server.getGroups().size());
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
                return (server.isRestricted())?ChatColor.RED+"Private":ChatColor.GREEN+"Public";
            }
            case "server.hidden":
            case "subserver.hidden": {
                return (server.isHidden())?ChatColor.RED+"Hidden":ChatColor.GREEN+"Visible";
            }
            case "server.players":
            case "subserver.players": {
                return ChatColor.AQUA + Integer.toString(server.getGlobalPlayers().size());
            }
            case "server.subdata":
            case "subserver.subdata": {
                return (server.getSubData()[0] == null)?ChatColor.RED+"Disconnected":ChatColor.GREEN+"Connected";
            }
            case "server.subdata.channels":
            case "subserver.subdata.channels":
            case "server.subdata.subchannels":
            case "subserver.subdata.subchannels": {
                return ChatColor.AQUA + Integer.toString(server.getSubData().length - ((method.endsWith(".subchannels"))?1:0));
            }
            case "server.signature":
            case "subserver.signature": {
                return ChatColor.AQUA + server.getSignature();
            }
            case "subserver":
            case "subservers": {
                return ChatColor.AQUA + Integer.toString(cache.getSubServers().size());
            }
            case "subserver.available": {
                return (subserver.isAvailable())?ChatColor.GREEN+"Available":ChatColor.RED+"Unavailable";
            }
            case "subserver.enabled": {
                return (subserver.isEnabled())?ChatColor.GREEN+"Enabled":ChatColor.RED+"Disabled";
            }
            case "subserver.editable": {
                return (subserver.isEditable())?ChatColor.GREEN+"Editable":ChatColor.RED+"Locked";
            }
            case "subserver.running": {
                return (subserver.isRunning())?ChatColor.GREEN+"Running":ChatColor.RED+"Offline";
            }
            case "subserver.online": {
                return (subserver.isOnline())?ChatColor.GREEN+"Online":((subserver.isRunning())?ChatColor.YELLOW+"Starting":ChatColor.RED+"Offline");
            }
            case "subserver.logging": {
                return (subserver.isLogging())?ChatColor.GREEN+"Logging":ChatColor.RED+"Muted";
            }
            case "subserver.temporary": {
                return (subserver.getStopAction() == SubServer.StopAction.REMOVE_SERVER || subserver.getStopAction() == SubServer.StopAction.RECYCLE_SERVER || subserver.getStopAction() == SubServer.StopAction.DELETE_SERVER)?
                        ChatColor.AQUA+"Temporary":ChatColor.GREEN+"Permanent";
            }
            case "subserver.stopaction": {
                return subserver.getStopAction().toString();
            }
            case "subserver.incompatibilities":
            case "subserver.incompatibilities.current": {
                List<String> list = (method.endsWith(".current"))?subserver.getCurrentIncompatibilities():subserver.getIncompatibilities();
                return ((list.isEmpty())?ChatColor.AQUA:ChatColor.RED) + Integer.toString(list.size());
            }
            default: {
                return null;
            }
        }
    }

    private static final class Cache {
        private static HashMap<String, Proxy> proxies = new HashMap<String, Proxy>();
        private static HashMap<String, Host> hosts = new HashMap<String, Host>();
        private static HashMap<String, Server> servers = new HashMap<String, Server>();
        private static Proxy master = null;
        private Listener events = new Events();

        private void reset() {
            proxies.clear();
            hosts.clear();
            servers.clear();
            master = null;
        }

        private void refresh() {
            if (SubAPI.getInstance().getSubDataNetwork()[0] != null) {
                SubAPI.getInstance().getProxies(proxies -> {
                    Cache.proxies = new HashMap<>(proxies);
                });
                SubAPI.getInstance().getMasterProxy(master -> {
                    Cache.master = master;
                });
                SubAPI.getInstance().getHosts(hosts -> {
                    Cache.hosts = new HashMap<>(hosts);
                });
                SubAPI.getInstance().getServers(servers -> {
                    Cache.servers = new HashMap<>(servers);
                });
            }
        }

        private final class Events implements Listener {
            @EventHandler
            public void add(SubAddProxyEvent e) {
                SubAPI.getInstance().getProxy(e.getProxy(), proxy -> {
                    proxies.put(proxy.getName().toLowerCase(), proxy);
                });
            }

            @EventHandler
            public void add(SubAddHostEvent e) {
                SubAPI.getInstance().getHost(e.getHost(), host -> {
                    hosts.put(host.getName().toLowerCase(), host);
                });
            }

            @EventHandler
            public void add(SubAddServerEvent e) {
                SubAPI.getInstance().getServer(e.getServer(), server -> {
                    servers.put(server.getName().toLowerCase(), server);
                });
            }

            @EventHandler
            public void start(SubStartEvent e) {
                Server server = getServer(e.getServer());
                if (server != null) Util.isException(() -> Util.<ObjectMap<String>>reflect(Server.class.getDeclaredField("raw"), server).set("running", true));
            }

            @EventHandler
            public void started(SubStartedEvent e) {
                Server server = getServer(e.getServer());
                if (server != null) Util.isException(() -> Util.<ObjectMap<String>>reflect(Server.class.getDeclaredField("raw"), server).set("online", true));
            }

            @EventHandler
            public void stopped(SubStoppedEvent e) {
                Server server = getServer(e.getServer());
                if (server != null) Util.isException(() -> {
                    ObjectMap<String> raw = Util.reflect(Server.class.getDeclaredField("raw"), server);
                    raw.set("online", false);
                    raw.set("running", false);
                });
            }
        }

        public Map<String, Proxy> getProxies() {
            return proxies;
        }

        public Proxy getProxy(String name) {
            if (Util.isNull(name)) throw new NullPointerException();
            Proxy proxy = getProxies().getOrDefault(name.toLowerCase(), null);
            if (proxy == null && master != null && master.getName().equalsIgnoreCase(name)) proxy = master;
            return proxy;
        }

        public Proxy getMasterProxy() {
            return master;
        }

        private Map<String, Host> getHosts() {
            return hosts;
        }

        private Host getHost(String name) {
            if (Util.isNull(name)) throw new NullPointerException();
            return getHosts().get(name.toLowerCase());
        }

        public Map<String, Server> getServers() {
            return servers;
        }

        public Server getServer(String name) {
            if (Util.isNull(name)) throw new NullPointerException();
            return getServers().get(name.toLowerCase());
        }

        public Map<String, SubServer> getSubServers() {
            TreeMap<String, SubServer> servers = new TreeMap<String, SubServer>();
            for (Map.Entry<String, Server> server : Cache.servers.entrySet()) {
                if (server.getValue() instanceof SubServer) servers.put(server.getKey(), (SubServer) server.getValue());
            }
            return servers;
        }

        public SubServer getSubServer(String name) {
            if (Util.isNull(name)) throw new NullPointerException();
            return getSubServers().get(name.toLowerCase());
        }
    }
}

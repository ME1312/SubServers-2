package net.ME1312.SubServers.Sync;

import net.ME1312.Galaxi.Library.AsyncConsolidator;
import net.ME1312.Galaxi.Library.Callback.Callback;
import net.ME1312.Galaxi.Library.Callback.ReturnRunnable;
import net.ME1312.Galaxi.Library.Container.ContainedPair;
import net.ME1312.Galaxi.Library.Container.Container;
import net.ME1312.Galaxi.Library.Container.Pair;
import net.ME1312.Galaxi.Library.Container.Value;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Platform;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.SubData.Client.SubDataClient;
import net.ME1312.SubData.Client.SubDataSender;
import net.ME1312.SubServers.Client.Common.Network.API.*;
import net.ME1312.SubServers.Client.Common.Network.Packet.PacketCreateServer;
import net.ME1312.SubServers.Client.Common.Network.Packet.PacketUpdateServer;
import net.ME1312.SubServers.Sync.Network.Packet.PacketCheckPermission;
import net.ME1312.SubServers.Sync.Network.Packet.PacketInExRunEvent;
import net.ME1312.SubServers.Sync.Server.CachedPlayer;
import net.ME1312.SubServers.Sync.Server.ServerImpl;
import net.ME1312.SubServers.Sync.Server.SubServerImpl;

import com.google.gson.Gson;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;
import net.md_5.bungee.command.ConsoleCommandSender;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("deprecation")
public final class SubCommand extends Command implements TabExecutor {
    static HashMap<UUID, HashMap<ServerInfo, Pair<Long, Boolean>>> permitted = new HashMap<UUID, HashMap<ServerInfo, Pair<Long, Boolean>>>();
    private TreeMap<String, Proxy> proxyCache = new TreeMap<String, Proxy>();
    private TreeMap<String, Host> hostCache = new TreeMap<String, Host>();
    private TreeMap<String, List<Server>> groupCache = new TreeMap<String, List<Server>>();
    private Proxy proxyMasterCache = null;
    private long cacheDate = 0;
    private ExProxy plugin;
    private String label;

    SubCommand(ExProxy plugin, String command) {
        super(command);
        this.plugin = plugin;
        this.label = '/' + command;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof ProxiedPlayer)) {
            if (plugin.api.getSubDataNetwork()[0] == null || plugin.api.getSubDataNetwork()[0].isClosed()) {
                new IllegalStateException("SubData is not connected").printStackTrace();
                if (!(sender instanceof ConsoleCommandSender)) sender.sendMessage(ChatColor.RED + "An exception has occurred while running this command");
            } else {
                if (args.length > 0) {
                    if (args[0].equalsIgnoreCase("help") || args[0].equalsIgnoreCase("?")) {
                        sender.sendMessages(printHelp());
                    } else if (args[0].equalsIgnoreCase("version") || args[0].equalsIgnoreCase("ver")) {
                        sender.sendMessage("SubServers > These are the platforms and versions that are running SubServers.Sync:");
                        sender.sendMessage("  " + Platform.getSystemName() + ' ' + Platform.getSystemVersion() + ((Platform.getSystemBuild() != null)?" (" + Platform.getSystemBuild() + ')':"") + ((!Platform.getSystemArchitecture().equals("unknown"))?" [" + Platform.getSystemArchitecture() + ']':"") + ',');
                        sender.sendMessage("  Java " + Platform.getJavaVersion() + ((!Platform.getJavaArchitecture().equals("unknown"))?" [" + Platform.getJavaArchitecture() + ']':"") + ',');
                        sender.sendMessage("  " + plugin.getBungeeName() + ' ' + plugin.getVersion() + ((plugin.isPatched)?" [Patched]":"") + ',');
                        sender.sendMessage("  SubServers.Sync v" + ExProxy.version.toExtendedString() + ((plugin.api.getWrapperBuild() != null)?" (" + plugin.api.getWrapperBuild() + ')':""));
                        sender.sendMessage("");
                        new Thread(() -> {
                            try {
                                ObjectMap<String> tags = new ObjectMap<String>(new Gson().fromJson("{\"tags\":" + Util.readAll(new BufferedReader(new InputStreamReader(new URL("https://api.github.com/repos/ME1312/SubServers-2/git/refs/tags").openStream(), Charset.forName("UTF-8")))) + '}', Map.class));
                                List<Version> versions = new LinkedList<Version>();

                                Version updversion = plugin.version;
                                int updcount = 0;
                                for (ObjectMap<String> tag : tags.getMapList("tags")) versions.add(Version.fromString(tag.getString("ref").substring(10)));
                                Collections.sort(versions);
                                for (Version version : versions) {
                                    if (version.compareTo(updversion) > 0) {
                                        updversion = version;
                                        updcount++;
                                    }
                                }
                                if (updcount == 0) {
                                    sender.sendMessage("You are on the latest version.");
                                } else {
                                    sender.sendMessage("SubServers.Sync v" + updversion + " is available. You are " + updcount + " version" + ((updcount == 1)?"":"s") + " behind.");
                                }
                            } catch (Exception e) {
                            }
                        }, "SubServers.Sync::Update_Check").start();
                    } else if (args[0].equalsIgnoreCase("list")) {
                        plugin.api.getGroups(groups -> plugin.api.getHosts(hosts -> plugin.api.getServers(servers -> plugin.api.getMasterProxy(proxymaster -> plugin.api.getProxies(proxies -> {
                            int i = 0;
                            boolean sent = false;
                            String div = ChatColor.RESET + ", ";
                            if (groups.keySet().size() > 0) {
                                sender.sendMessage("Group/Server List:");
                                for (String group : groups.keySet()) {
                                    String message = "  ";
                                    message += ChatColor.GOLD + group + ChatColor.RESET + ": ";
                                    for (Server server : groups.get(group)) {
                                        if (i != 0) message += div;
                                        if (!(server instanceof SubServer)) {
                                            message += ChatColor.WHITE;
                                        } else if (((SubServer) server).isRunning()) {
                                            if (((SubServer) server).getStopAction() == SubServer.StopAction.REMOVE_SERVER || ((SubServer) server).getStopAction() == SubServer.StopAction.RECYCLE_SERVER || ((SubServer) server).getStopAction() == SubServer.StopAction.DELETE_SERVER) {
                                                message += ChatColor.AQUA;
                                            } else {
                                                message += ChatColor.GREEN;
                                            }
                                        } else if (((SubServer) server).isAvailable() && ((SubServer) server).isEnabled() && ((SubServer) server).getCurrentIncompatibilities().size() == 0) {
                                            message += ChatColor.YELLOW;
                                        } else {
                                            message += ChatColor.RED;
                                        }
                                        message += server.getDisplayName() + ((server.getName().equals(server.getDisplayName()))?"":" ["+server.getName()+']');
                                        i++;
                                    }
                                    if (i == 0) message += ChatColor.RESET + "(none)";
                                    sender.sendMessage(message);
                                    i = 0;
                                    sent = true;
                                }
                                if (!sent) sender.sendMessage(ChatColor.RESET + "(none)");
                                sent = false;
                            }
                            sender.sendMessage("Host/SubServer List:");
                            for (Host host : hosts.values()) {
                                String message = "  ";
                                if (host.isAvailable() && host.isEnabled()) {
                                    message += ChatColor.AQUA;
                                } else {
                                    message += ChatColor.RED;
                                }
                                message += host.getDisplayName() + " [" + ((host.getName().equals(host.getDisplayName()))?"":host.getName()+ChatColor.stripColor(div)) + host.getAddress().getHostAddress() + "]" + ChatColor.RESET + ": ";
                                for (SubServer subserver : host.getSubServers().values()) {
                                    if (i != 0) message += div;
                                    if (subserver.isRunning()) {
                                        if (subserver.getStopAction() == SubServer.StopAction.REMOVE_SERVER || subserver.getStopAction() == SubServer.StopAction.RECYCLE_SERVER || subserver.getStopAction() == SubServer.StopAction.DELETE_SERVER) {
                                            message += ChatColor.AQUA;
                                        } else {
                                            message += ChatColor.GREEN;
                                        }
                                    } else if (subserver.isAvailable() && subserver.isEnabled() && subserver.getCurrentIncompatibilities().size() == 0) {
                                        message += ChatColor.YELLOW;
                                    } else {
                                        message += ChatColor.RED;
                                    }
                                    message += subserver.getDisplayName() + " [" + ((subserver.getName().equals(subserver.getDisplayName()))?"":subserver.getName()+ChatColor.stripColor(div)) + subserver.getAddress().getPort() + "]";
                                    i++;
                                }
                                if (i == 0) message += ChatColor.RESET + "(none)";
                                sender.sendMessage(message);
                                i = 0;
                                sent = true;
                            }
                            if (!sent) sender.sendMessage(ChatColor.RESET + "(none)");
                            sender.sendMessage("Server List:");
                            String message = "  ";
                            for (Server server : servers.values()) if (!(server instanceof SubServer)) {
                                if (i != 0) message += div;
                                message += ChatColor.WHITE + server.getDisplayName() + " [" + ((server.getName().equals(server.getDisplayName()))?"":server.getName()+ChatColor.stripColor(div)) + server.getAddress().getAddress().getHostAddress()+':'+server.getAddress().getPort() + "]";
                                i++;
                            }
                            if (i == 0) message += ChatColor.RESET + "(none)";
                            sender.sendMessage(message);
                            if (proxies.keySet().size() > 0) {
                                sender.sendMessage("Proxy List:");
                                message = "  (master)";
                                for (Proxy proxy : proxies.values()) {
                                    message += div;
                                    if (proxy.getSubData()[0] != null) {
                                        message += ChatColor.AQUA;
                                    } else {
                                        message += ChatColor.RED;
                                    }
                                    message += proxy.getDisplayName() + ((proxy.getName().equals(proxy.getDisplayName()))?"":" ["+proxy.getName()+']');
                                }
                                sender.sendMessage(message);
                            }
                        })))));
                    } else if (args[0].equalsIgnoreCase("info") || args[0].equalsIgnoreCase("status")) {
                        if (args.length > 1) {
                            String type = (args.length > 2)?args[1]:null;
                            String name = args[(type != null)?2:1];

                            Runnable getPlayer = () -> plugin.api.getRemotePlayer(name, player -> {
                                if (player != null) {
                                    sender.sendMessage("SubServers > Info on player: " + ChatColor.WHITE + player.getName());
                                    if (player.getProxyName() != null) sender.sendMessage(" -> Proxy: " + ChatColor.WHITE + player.getProxyName());
                                    if (player.getServerName() != null) sender.sendMessage(" -> Server: " + ChatColor.WHITE + player.getServerName());
                                    if (player.getAddress() != null) sender.sendMessage(" -> Address: " + ChatColor.WHITE + player.getAddress().getAddress().getHostAddress() + ':' + player.getAddress().getPort());
                                    sender.sendMessage(" -> UUID: " + ChatColor.AQUA + player.getUniqueId());
                                } else {
                                    if (type == null) {
                                        sender.sendMessage("SubServers > There is no object with that name");
                                    } else {
                                        sender.sendMessage("SubServers > There is no player with that name");
                                    }
                                }
                            });
                            Runnable getServer = () -> plugin.api.getServer(name, server -> {
                                if (server != null) {
                                    sender.sendMessage("SubServers > Info on " + ((server instanceof SubServer)?"sub":"") + "server: " + ChatColor.WHITE + server.getDisplayName());
                                    if (!server.getName().equals(server.getDisplayName())) sender.sendMessage(" -> System Name: " + ChatColor.WHITE + server.getName());
                                    if (server instanceof SubServer) {
                                        sender.sendMessage(" -> Available: " + ((((SubServer) server).isAvailable())?ChatColor.GREEN+"yes":ChatColor.RED+"no"));
                                        sender.sendMessage(" -> Enabled: " + ((((SubServer) server).isEnabled())?ChatColor.GREEN+"yes":ChatColor.RED+"no"));
                                        if (!((SubServer) server).isEditable()) sender.sendMessage(" -> Editable: " + ChatColor.RED + "no");
                                        sender.sendMessage(" -> Host: " + ChatColor.WHITE + ((SubServer) server).getHost());
                                        if (((SubServer) server).getTemplate() != null) sender.sendMessage(" -> Template: " + ChatColor.WHITE + ((SubServer) server).getHost());
                                    }
                                    if (server.getGroups().size() > 0) sender.sendMessage(" -> Group" + ((server.getGroups().size() > 1)?"s:":": " + ChatColor.WHITE + server.getGroups().get(0)));
                                    if (server.getGroups().size() > 1) for (String group : server.getGroups()) sender.sendMessage("      - " + ChatColor.WHITE + group);
                                    sender.sendMessage(" -> Address: " + ChatColor.WHITE + server.getAddress().getAddress().getHostAddress()+':'+server.getAddress().getPort());
                                    if (server instanceof SubServer) sender.sendMessage(" -> " + ((((SubServer) server).isOnline())?"Online":"Running") + ": " + ((((SubServer) server).isRunning())?ChatColor.GREEN+"yes":ChatColor.RED+"no"));
                                    if (!(server instanceof SubServer) || ((SubServer) server).isRunning()) {
                                        sender.sendMessage(" -> Connected: " + ((server.getSubData()[0] != null)?ChatColor.GREEN+"yes"+((server.getSubData().length > 1)?ChatColor.AQUA+" +"+(server.getSubData().length-1)+" subchannel"+((server.getSubData().length == 2)?"":"s"):""):ChatColor.RED+"no"));
                                        sender.sendMessage(" -> Players: " + ChatColor.AQUA + server.getRemotePlayers().size() + " online");
                                    }
                                    sender.sendMessage(" -> MOTD: " + ChatColor.WHITE + ChatColor.stripColor(server.getMotd()));
                                    if (server instanceof SubServer && ((SubServer) server).getStopAction() != SubServer.StopAction.NONE) sender.sendMessage(" -> Stop Action: " + ChatColor.WHITE + ((SubServer) server).getStopAction().toString());
                                    sender.sendMessage(" -> Signature: " + ChatColor.AQUA + server.getSignature());
                                    if (server instanceof SubServer) sender.sendMessage(" -> Logging: " + ((((SubServer) server).isLogging())?ChatColor.GREEN+"yes":ChatColor.RED+"no"));
                                    sender.sendMessage(" -> Restricted: " + ((server.isRestricted())?ChatColor.GREEN+"yes":ChatColor.RED+"no"));
                                    if (server instanceof SubServer && ((SubServer) server).getIncompatibilities().size() > 0) {
                                        List<String> current = new ArrayList<String>();
                                        for (String other : ((SubServer) server).getCurrentIncompatibilities()) current.add(other.toLowerCase());
                                        sender.sendMessage(" -> Incompatibilities:");
                                        for (String other : ((SubServer) server).getIncompatibilities()) sender.sendMessage("      - " + ((current.contains(other.toLowerCase()))?ChatColor.WHITE:ChatColor.GRAY) + other);
                                    }
                                    sender.sendMessage(" -> Hidden: " + ((server.isHidden())?ChatColor.GREEN+"yes":ChatColor.RED+"no"));
                                } else {
                                    if (type == null) {
                                        getPlayer.run();
                                    } else {
                                        sender.sendMessage("SubServers > There is no server with that name");
                                    }
                                }
                            });
                            Runnable getGroup = () -> plugin.api.getGroup(name, group -> {
                                if (group != null) {
                                    sender.sendMessage("SubServers > Info on group: " + ChatColor.WHITE + group.key());
                                    sender.sendMessage(" -> Servers: " + ((group.value().size() <= 0)?ChatColor.GRAY + "(none)":ChatColor.AQUA.toString() + group.value().size()));
                                    for (Server server : group.value()) sender.sendMessage("      - " + ChatColor.WHITE + server.getDisplayName() + ((server.getName().equals(server.getDisplayName()))?"":" ["+server.getName()+']'));
                                } else {
                                    if (type == null) {
                                        getServer.run();
                                    } else {
                                        sender.sendMessage("SubServers > There is no group with that name");
                                    }
                                }
                            });
                            Runnable getHost = () -> plugin.api.getHost(name, host -> {
                                if (host != null) {
                                    sender.sendMessage("SubServers > Info on host: " + ChatColor.WHITE + host.getDisplayName());
                                    if (!host.getName().equals(host.getDisplayName())) sender.sendMessage(" -> System Name: " + ChatColor.WHITE + host.getName());
                                    sender.sendMessage(" -> Available: " + ((host.isAvailable())?ChatColor.GREEN+"yes":ChatColor.RED+"no"));
                                    sender.sendMessage(" -> Enabled: " + ((host.isEnabled())?ChatColor.GREEN+"yes":ChatColor.RED+"no"));
                                    sender.sendMessage(" -> Address: " + ChatColor.WHITE + host.getAddress().getHostAddress());
                                    if (host.getSubData().length > 0) sender.sendMessage(" -> Connected: " + ((host.getSubData()[0] != null)?ChatColor.GREEN+"yes"+((host.getSubData().length > 1)?ChatColor.AQUA+" +"+(host.getSubData().length-1)+" subchannel"+((host.getSubData().length == 2)?"":"s"):""):ChatColor.RED+"no"));
                                    sender.sendMessage(" -> SubServers: " + ((host.getSubServers().keySet().size() <= 0)?ChatColor.GRAY + "(none)":ChatColor.AQUA.toString() + host.getSubServers().keySet().size()));
                                    for (SubServer subserver : host.getSubServers().values()) sender.sendMessage("      - " + ((subserver.isEnabled())?ChatColor.WHITE:ChatColor.GRAY) + subserver.getDisplayName() + ((subserver.getName().equals(subserver.getDisplayName()))?"":" ["+subserver.getName()+']'));
                                    sender.sendMessage(" -> Templates: " + ((host.getCreator().getTemplates().keySet().size() <= 0)?ChatColor.GRAY + "(none)":ChatColor.AQUA.toString() + host.getCreator().getTemplates().keySet().size()));
                                    for (SubCreator.ServerTemplate template : host.getCreator().getTemplates().values()) sender.sendMessage("      - " + ((template.isEnabled())?ChatColor.WHITE:ChatColor.GRAY) + template.getDisplayName() + ((template.getName().equals(template.getDisplayName()))?"":" ["+template.getName()+']'));
                                    sender.sendMessage(" -> Signature: " + ChatColor.AQUA + host.getSignature());
                                } else {
                                    if (type == null) {
                                        getGroup.run();
                                    } else {
                                        sender.sendMessage("SubServers > There is no host with that name");
                                    }
                                }
                            });
                            Runnable getProxy = () -> plugin.api.getProxy(name, proxy -> {
                                if (proxy != null) {
                                    sender.sendMessage("SubServers > Info on proxy: " + ChatColor.WHITE + proxy.getDisplayName());
                                    if (!proxy.getName().equals(proxy.getDisplayName())) sender.sendMessage(" -> System Name: " + ChatColor.WHITE + proxy.getName());
                                    if (!proxy.isMaster()) sender.sendMessage(" -> Connected: " + ((proxy.getSubData()[0] != null)?ChatColor.GREEN+"yes"+((proxy.getSubData().length > 1)?ChatColor.AQUA+" +"+(proxy.getSubData().length-1)+" subchannel"+((proxy.getSubData().length == 2)?"":"s"):""):ChatColor.RED+"no"));
                                    else if (!proxy.getDisplayName().toLowerCase().contains("master")) sender.sendMessage(" -> Type: " + ChatColor.WHITE + "Master");
                                    sender.sendMessage(" -> Players: " + ChatColor.AQUA + proxy.getPlayers().size() + " online");
                                    sender.sendMessage(" -> Signature: " + ChatColor.AQUA + proxy.getSignature());
                                } else {
                                    if (type == null) {
                                        getHost.run();
                                    } else {
                                        sender.sendMessage("SubServers > There is no proxy with that name");
                                    }
                                }
                            });

                            if (type == null) {
                                getProxy.run();
                            } else {
                                switch (type.toLowerCase()) {
                                    case "p":
                                    case "proxy":
                                        getProxy.run();
                                        break;
                                    case "h":
                                    case "host":
                                        getHost.run();
                                        break;
                                    case "g":
                                    case "group":
                                        getGroup.run();
                                        break;
                                    case "s":
                                    case "server":
                                    case "subserver":
                                        getServer.run();
                                        break;
                                    case "player":
                                        getPlayer.run();
                                        break;
                                    default:
                                        sender.sendMessage("SubServers > There is no object type with that name");
                                }
                            }
                        } else {
                            sender.sendMessage("SubServers > Usage: " + label + " " + args[0].toLowerCase() + " [proxy|host|group|server|player] <Name>");
                        }
                    } else if (args[0].equalsIgnoreCase("start")) {
                        if (args.length > 1) {
                            selectServers(sender, args, 1, true, select -> {
                                if (select.subservers.length > 0) {
                                    Container<Integer> success = new Container<Integer>(0);
                                    Container<Integer> running = new Container<Integer>(0);
                                    AsyncConsolidator merge = new AsyncConsolidator(() -> {
                                        if (running.value > 0) sender.sendMessage("SubServers > " + running.value + " subserver"+((running.value == 1)?" was":"s were") + " already running");
                                        if (success.value > 0) sender.sendMessage("SubServers > Started " + success.value + " subserver"+((success.value == 1)?"":"s"));
                                    });
                                    for (SubServer server : select.subservers) {
                                        merge.reserve();
                                        server.start(null, response -> {
                                            switch (response) {
                                                case 3:
                                                case 4:
                                                    sender.sendMessage("SubServers > Subserver " + server.getName() + " has disappeared");
                                                    break;
                                                case 5:
                                                    sender.sendMessage("SubServers > The host for " + server.getName() + " is not available");
                                                    break;
                                                case 6:
                                                    sender.sendMessage("SubServers > The host for " + server.getName() + " is not enabled");
                                                    break;
                                                case 7:
                                                    sender.sendMessage("SubServers > Subserver " + server.getName() + " is not available");
                                                    break;
                                                case 8:
                                                    sender.sendMessage("SubServers > SubServer " + server.getName() + " is not enabled");
                                                    break;
                                                case 9:
                                                    running.value++;
                                                    break;
                                                case 10:
                                                    sender.sendMessage("SubServers > Subserver " + server.getName() + " cannot start while incompatible server(s) are running");
                                                    break;
                                                case 0:
                                                    success.value++;
                                                    break;
                                            }
                                            merge.release();
                                        });
                                    }
                                }
                            });
                        } else {
                            sender.sendMessage("Usage: " + label + " " + args[0].toLowerCase() + " <Subservers>");
                        }
                    } else if (args[0].equalsIgnoreCase("restart")) {
                        if (args.length > 1) {
                            selectServers(sender, args, 1, true, select -> {
                                if (select.subservers.length > 0) {
                                    // Step 5: Start the stopped Servers once more
                                    Callback<SubServer> starter = server -> server.start(response -> {
                                        switch (response) {
                                            case 3:
                                            case 4:
                                                sender.sendMessage("SubServers > Could not restart server: Subserver " + server.getName() + " has disappeared");
                                                break;
                                            case 5:
                                                sender.sendMessage("SubServers > Could not restart server: The host for " + server.getName() + " is no longer available");
                                                break;
                                            case 6:
                                                sender.sendMessage("SubServers > Could not restart server: The host for " + server.getName() + " is no longer enabled");
                                                break;
                                            case 7:
                                                sender.sendMessage("SubServers > Could not restart server: Subserver " + server.getName() + " is no longer available");
                                                break;
                                            case 8:
                                                sender.sendMessage("SubServers > Could not restart server: Subserver " + server.getName() + " is no longer enabled");
                                                break;
                                            case 10:
                                                sender.sendMessage("SubServers > Could not restart server: Subserver " + server.getName() + " cannot start while incompatible server(s) are running");
                                                break;
                                            case 9:
                                            case 0:
                                                // success!
                                                break;
                                        }
                                    });

                                    // Step 4: Listen for stopped Servers
                                    final HashMap<String, SubServer> listening = new HashMap<String, SubServer>();
                                    PacketInExRunEvent.callback("SubStoppedEvent", new Callback<ObjectMap<String>>() {
                                        @Override
                                        public void run(ObjectMap<String> json) {
                                            try {
                                                if (listening.size() > 0) {
                                                    PacketInExRunEvent.callback("SubStoppedEvent", this);
                                                    String name = json.getString("server").toLowerCase();
                                                    if (listening.keySet().contains(name)) {
                                                        Timer timer = new Timer("SubServers.Sync::Server_Restart_Command_Handler(" + name + ")");
                                                        timer.schedule(new TimerTask() {
                                                            @Override
                                                            public void run() {
                                                                starter.run(listening.get(name));
                                                                listening.remove(name);
                                                                timer.cancel();
                                                            }
                                                        }, 100);
                                                    }
                                                }
                                            } catch (Exception e) {}
                                        }
                                    });


                                    // Step 1-3: Restart Servers / Receive command Responses
                                    Container<Integer> success = new Container<Integer>(0);
                                    AsyncConsolidator merge = new AsyncConsolidator(() -> {
                                        if (success.value > 0) sender.sendMessage("SubServers > Restarting " + success.value + " subserver"+((success.value == 1)?"":"s"));
                                    });
                                    for (SubServer server : select.subservers) {
                                        merge.reserve();
                                        listening.put(server.getName().toLowerCase(), server);
                                        server.stop(response -> {
                                            if (response != 0) listening.remove(server.getName().toLowerCase());
                                            switch (response) {
                                                case 3:
                                                case 4:
                                                    sender.sendMessage("Could not restart server: Subserver " + server.getName() + " has disappeared");
                                                    break;
                                                case 5:
                                                    starter.run(server);
                                                case 0:
                                                    success.value++;
                                                    break;
                                            }
                                            merge.release();
                                        });
                                    }
                                }
                            });
                        } else {
                            sender.sendMessage("Usage: " + label + " " + args[0].toLowerCase() + " <Subservers>");
                        }
                    } else if (args[0].equalsIgnoreCase("stop")) {
                        if (args.length > 1) {
                            selectServers(sender, args, 1, true, select -> {
                                if (select.subservers.length > 0) {
                                    Container<Integer> success = new Container<Integer>(0);
                                    Container<Integer> running = new Container<Integer>(0);
                                    AsyncConsolidator merge = new AsyncConsolidator(() -> {
                                        if (running.value > 0) sender.sendMessage("SubServers > " + running.value + " subserver"+((running.value == 1)?" was":"s were") + " already offline");
                                        if (success.value > 0) sender.sendMessage("SubServers > Stopping " + success.value + " subserver"+((success.value == 1)?"":"s"));
                                    });
                                    for (SubServer server : select.subservers) {
                                        merge.reserve();
                                        server.stop(response -> {
                                            switch (response) {
                                                case 3:
                                                case 4:
                                                    sender.sendMessage("SubServers > Subserver " + server.getName() + " has disappeared");
                                                    break;
                                                case 5:
                                                    running.value++;
                                                    break;
                                                case 0:
                                                    success.value++;
                                                    break;
                                            }
                                            merge.release();
                                        });
                                    }
                                }
                            });
                        } else {
                            sender.sendMessage("Usage: " + label + " " + args[0].toLowerCase() + " <Subservers>");
                        }
                    } else if (args[0].equalsIgnoreCase("kill") || args[0].equalsIgnoreCase("terminate")) {
                        if (args.length > 1) {
                            selectServers(sender, args, 1, true, select -> {
                                if (select.subservers.length > 0) {
                                    Container<Integer> success = new Container<Integer>(0);
                                    Container<Integer> running = new Container<Integer>(0);
                                    AsyncConsolidator merge = new AsyncConsolidator(() -> {
                                        if (running.value > 0) sender.sendMessage("SubServers > " + running.value + " subserver"+((running.value == 1)?" was":"s were") + " already offline");
                                        if (success.value > 0) sender.sendMessage("SubServers > Terminated " + success.value + " subserver"+((success.value == 1)?"":"s"));
                                    });
                                    for (SubServer server : select.subservers) {
                                        merge.reserve();
                                        server.terminate(response -> {
                                            switch (response) {
                                                case 3:
                                                case 4:
                                                    sender.sendMessage("SubServers > Subserver " + server.getName() + " has disappeared");
                                                    break;
                                                case 5:
                                                    running.value++;
                                                    break;
                                                case 0:
                                                    success.value++;
                                                    break;
                                            }
                                            merge.release();
                                        });
                                    }
                                }
                            });
                        } else {
                            sender.sendMessage("Usage: " + label + " " + args[0].toLowerCase() + " <Subservers>");
                        }
                    } else if (args[0].equalsIgnoreCase("cmd") || args[0].equalsIgnoreCase("command")) {
                        if (args.length > 1) {
                            selectServers(sender, args, 1, true, select -> {
                                if (select.subservers.length > 0) {
                                    if (select.args.length > 2) {
                                        StringBuilder builder = new StringBuilder(select.args[2]);
                                        for (int i = 3; i < select.args.length; i++) {
                                            builder.append(' ');
                                            builder.append(select.args[i]);
                                        }

                                        Container<Integer> success = new Container<Integer>(0);
                                        Container<Integer> running = new Container<Integer>(0);
                                        AsyncConsolidator merge = new AsyncConsolidator(() -> {
                                            if (running.value > 0) sender.sendMessage("SubServers > " + running.value + " subserver"+((running.value == 1)?" was":"s were") + " offline");
                                            if (success.value > 0) sender.sendMessage("SubServers > Sent command to " + success.value + " subserver"+((success.value == 1)?"":"s"));
                                        });
                                        for (SubServer server : select.subservers) {
                                            merge.reserve();
                                            server.command(builder.toString(), response -> {
                                                switch (response) {
                                                    case 3:
                                                    case 4:
                                                        sender.sendMessage("SubServers > Subserver " + server.getName() + " has disappeared");
                                                        break;
                                                    case 5:
                                                        running.value++;
                                                        break;
                                                    case 0:
                                                        success.value++;
                                                        break;
                                                }
                                                merge.release();
                                            });
                                        }
                                    } else {
                                        sender.sendMessage("SubServers > No command was entered");
                                    }
                                }
                            });
                        } else {
                            sender.sendMessage("Usage: " + label + " " + args[0].toLowerCase() + " <Subservers> <Command> [Args...]");
                        }
                    } else if (args[0].equalsIgnoreCase("create")) {
                        if (args.length > 3) {
                            if (args.length > 5 && Util.isException(() -> Integer.parseInt(args[5]))) {
                                sender.sendMessage("SubServers > Invalid port number");
                            } else {
                                ((SubDataClient) SubAPI.getInstance().getSubDataNetwork()[0]).sendPacket(new PacketCreateServer(null, args[1], args[2],args[3], (args.length > 4)?new Version(args[4]):null, (args.length > 5)?Integer.parseInt(args[5]):null, data -> {
                                    switch (data.getInt(0x0001)) {
                                        case 3:
                                        case 4:
                                            sender.sendMessage("SubServers > There is already a subserver with that name");
                                            break;
                                        case 5:
                                            sender.sendMessage("SubServers > There is no host with that name");
                                            break;
                                        case 6:
                                            sender.sendMessage("SubServers > That host is not available");
                                            break;
                                        case 7:
                                            sender.sendMessage("SubServers > That host is not enabled");
                                            break;
                                        case 8:
                                            sender.sendMessage("SubServers > There is no template with that name");
                                            break;
                                        case 9:
                                            sender.sendMessage("SubServers > That template is not enabled");
                                            break;
                                        case 10:
                                            sender.sendMessage("SubServers > That template requires a Minecraft version to be specified");
                                            break;
                                        case 11:
                                            sender.sendMessage("SubServers > Invalid port number");
                                            break;
                                        case 0:
                                            sender.sendMessage("SubServers > Creating subserver " + args[1]);
                                            break;
                                    }
                                }));
                            }
                        } else {
                            sender.sendMessage("SubServers > Usage: " + label + " " + args[0].toLowerCase() + " <Name> <Host> <Template> [Version] [Port]");
                        }
                    } else if (args[0].equalsIgnoreCase("update") || args[0].equalsIgnoreCase("upgrade")) {
                        if (args.length > 1) {
                            selectServers(sender, args, 1, true, select -> {
                                if (select.subservers.length > 0) {
                                    String template = (select.args.length > 3)?select.args[2].toLowerCase():null;
                                    Version version = (select.args.length > 2)?new Version(select.args[(template == null)?2:3]):null;
                                    boolean ts = template == null;

                                    Container<Integer> success = new Container<Integer>(0);
                                    AsyncConsolidator merge = new AsyncConsolidator(() -> {
                                        if (success.value > 0) sender.sendMessage("SubServers > Updating " + success.value + " subserver"+((success.value == 1)?"":"s"));
                                    });
                                    for (SubServer server : select.subservers) {
                                        merge.reserve();
                                        ((SubDataClient) plugin.api.getSubDataNetwork()[0]).sendPacket(new PacketUpdateServer(null, server.getName(), template, version, data -> {
                                            switch (data.getInt(0x0001)) {
                                                case 3:
                                                case 4:
                                                    sender.sendMessage("SubServers > Subserver " + server.getName() + " has disappeared");
                                                    break;
                                                case 5:
                                                    sender.sendMessage("SubServers > The host for " + server.getName() + " is not available");
                                                    break;
                                                case 6:
                                                    sender.sendMessage("SubServers > The host for " + server.getName() + " is not enabled");
                                                    break;
                                                case 7:
                                                    sender.sendMessage("SubServers > Subserver " + server.getName() + " is not available");
                                                    break;
                                                case 8:
                                                    sender.sendMessage("SubServers > Cannot update " + server.getName() + " while it is still running");
                                                    break;
                                                case 9:
                                                    if (ts) sender.sendMessage("SubServers > We don't know which template built " + server.getName());
                                                    else    sender.sendMessage("SubServers > There is no template with that name");
                                                    break;
                                                case 10:
                                                    if (ts) sender.sendMessage("SubServers > The template that created " + server.getName() + " is not enabled");
                                                    else    sender.sendMessage("SubServers > That template is not enabled");
                                                    break;
                                                case 11:
                                                    if (ts) sender.sendMessage("SubServers > The template that created " + server.getName() + " does not support subserver updating");
                                                    else    sender.sendMessage("SubServers > That template does not support subserver updating");
                                                    break;
                                                case 12:
                                                    sender.sendMessage("SubServers > The template that created " + server.getName() + " requires a Minecraft version to be specified");
                                                    break;
                                                case 0:
                                                    success.value++;
                                                    break;
                                            }
                                            merge.release();
                                        }));
                                    }
                                }
                            });
                        } else {
                            sender.sendMessage("Usage: " + label + " " + args[0].toLowerCase() + " <Subservers> [[Template] <Version>]");
                        }
                    } else if (args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("del") || args[0].equalsIgnoreCase("delete")) {
                        if (args.length > 1) {
                            selectServers(sender, args, 1, true, select -> {
                                if (select.subservers.length > 0) {
                                    Container<Integer> success = new Container<Integer>(0);
                                    Container<Integer> running = new Container<Integer>(0);
                                    AsyncConsolidator merge = new AsyncConsolidator(() -> {
                                        if (success.value > 0) sender.sendMessage("SubServers > Removing " + success.value + " subserver"+((success.value == 1)?"":"s"));
                                    });
                                    for (SubServer server : select.subservers) {
                                        if (server.isRunning()) {
                                            sender.sendMessage("SubServers > Subserver " + server.getName() + " is still running");
                                        } else {
                                            server.getHost(host -> {
                                                if (host == null) {
                                                    sender.sendMessage("SubServers > Subserver " + server.getName() + " has disappeared");
                                                } else {
                                                    merge.reserve();
                                                    host.recycleSubServer(server.getName(), response -> {
                                                        switch (response) {
                                                            case 3:
                                                            case 4:
                                                                sender.sendMessage("SubServers > Subserver " + server.getName() + " has disappeared");
                                                                break;
                                                            case 0:
                                                                success.value++;
                                                                break;
                                                        }
                                                        merge.release();
                                                    });
                                                }
                                            });
                                        }
                                    }
                                }
                            });
                        } else {
                            sender.sendMessage("Usage: " + label + " " + args[0].toLowerCase() + " <Subservers>");
                        }
                    } else if (args[0].equalsIgnoreCase("restore")) {
                        // TODO
                    } else {
                        sender.sendMessage("SubServers > Unknown sub-command: " + args[0]);
                    }
                } else {
                    sender.sendMessages(printHelp());
                }
            }
        } else {
            String str = label;
            for (String arg : args) str += ' ' + arg;
            ((ProxiedPlayer) sender).chat(str);
        }
    }

    private void selectServers(CommandSender sender, String[] rargs, int index, boolean mode, Callback<ServerSelection> callback) {
        StackTraceElement[] origin = new Exception().getStackTrace();
        LinkedList<String> msgs = new LinkedList<String>();
        LinkedList<String> args = new LinkedList<String>();
        LinkedList<String> selection = new LinkedList<String>();
        LinkedList<Server> select = new LinkedList<Server>();
        Value<String> last = new Container<String>(null);

        // Step 1
        Value<Integer> ic = new Container<Integer>(0);
        while (ic.value() < index) {
            args.add(rargs[ic.value()]);
            ic.value(ic.value() + 1);
        }

        // Step 3
        StringBuilder completed = new StringBuilder();
        Runnable finished = () -> {
            args.add(completed.toString());

            int i = ic.value();
            while (i < rargs.length) {
                args.add(rargs[i]);
                last.value(null);
                i++;
            }

            LinkedList<Server> history = new LinkedList<Server>();
            LinkedList<Server> servers = new LinkedList<Server>();
            LinkedList<SubServer> subservers = new LinkedList<SubServer>();
            for (Server server : select) {
                if (!history.contains(server)) {
                    history.add(server);
                    servers.add(server);
                    if (server instanceof SubServer)
                        subservers.add((SubServer) server);
                }
            }

            if ((!mode && servers.size() <= 0) || (mode && subservers.size() <= 0)) {
                String msg = "SubServers > No " + ((mode)?"sub":"") + "servers were selected";
                if (sender != null) sender.sendMessage(msg);
                msgs.add(msg);
            }

            try {
                callback.run(new ServerSelection(msgs, selection, servers, subservers, args, last.value()));
            } catch (Throwable e) {
                Throwable ew = new InvocationTargetException(e);
                ew.setStackTrace(origin);
                ew.printStackTrace();
            }
        };

        // Step 2
        AsyncConsolidator merge = new AsyncConsolidator(finished);
        for (boolean run = true; run && ic.value() < rargs.length; ic.value(ic.value() + 1)) {
            String current = rargs[ic.value()];
            last.value(current);
            completed.append(current);
            if (current.endsWith(",")) {
                current = current.substring(0, current.length() - 1);
                completed.append(' ');
            } else run = false;
            selection.add(current.toLowerCase());

            if (current.length() > 0) {
                merge.reserve();

                if (current.startsWith("::") && current.length() > 2) {
                    current = current.substring(2);

                    if (current.equals("*")) {
                        plugin.api.getHosts(hostMap -> {
                            for (Host host : hostMap.values()) {
                                select.addAll(host.getSubServers().values());
                            }
                            merge.release();
                        });
                    } else {
                        final String fcurrent = current;
                        plugin.api.getHost(current, host -> {
                            if (host != null) {
                                if (!select.addAll(host.getSubServers().values())) {
                                    String msg = "SubServers > There are no " + ((mode)?"sub":"") + "servers on host: " + host.getName();
                                    if (sender != null) sender.sendMessage(msg);
                                    msgs.add(msg);
                                }
                            } else {
                                String msg = "SubServers > There is no host with name: " + fcurrent;
                                if (sender != null) sender.sendMessage(msg);
                                msgs.add(msg);
                            }
                            merge.release();
                        });
                    }
                } else if (current.startsWith(":") && current.length() > 1) {
                    current = current.substring(1);

                    if (current.equals("*")) {
                        plugin.api.getGroups(groupMap -> {
                            for (List<Server> group : groupMap.values()) for (Server server : group) {
                                if (!mode || server instanceof SubServer) select.add(server);
                            }
                            merge.release();
                        });
                    } else {
                        final String fcurrent = current;
                        plugin.api.getGroup(current, group -> {
                            if (group != null) {
                                int i = 0;
                                for (Server server : group.value()) {
                                    if (!mode || server instanceof SubServer) {
                                        select.add(server);
                                        i++;
                                    }
                                }
                                if (i <= 0) {
                                    String msg = "SubServers > There are no " + ((mode)?"sub":"") + "servers in group: " + group.key();
                                    if (sender != null) sender.sendMessage(msg);
                                    msgs.add(msg);
                                }
                            } else {
                                String msg = "SubServers > There is no group with name: " + fcurrent;
                                if (sender != null) sender.sendMessage(msg);
                                msgs.add(msg);
                            }
                            merge.release();
                        });
                    }
                } else {

                    if (current.equals("*")) {
                        plugin.api.getServers(serverMap -> {
                            for (Server server : serverMap.values()) {
                                if (!mode || server instanceof SubServer) select.add(server);
                            }
                            merge.release();
                        });
                    } else {
                        final String fcurrent = current;
                        plugin.api.getServer(current, server -> {
                            if (server != null) {
                                select.add(server);
                            } else {
                                String msg = "SubServers > There is no " + ((mode)?"sub":"") + "server with name: " + fcurrent;
                                if (sender != null) sender.sendMessage(msg);
                                msgs.add(msg);
                            }
                            merge.release();
                        });
                    }
                }
            }
        }
    }
    private static final class ServerSelection {
        private final String[] msgs;
        private final String[] selection;
        private final Server[] servers;
        private final SubServer[] subservers;
        private final String[] args;
        private final String last;

        private ServerSelection(List<String> msgs, List<String> selection, List<Server> servers, List<SubServer> subservers, List<String> args, String last) {
            this.msgs = msgs.toArray(new String[0]);
            this.selection = selection.toArray(new String[0]);
            this.servers = servers.toArray(new Server[0]);
            this.subservers = subservers.toArray(new SubServer[0]);
            this.args = args.toArray(new String[0]);
            this.last = last;

            Arrays.sort(this.selection);
        }
    }

    private String[] printHelp() {
        return new String[]{
                "SubServers > Console Command Help:",
                "   Help: /sub help",
                "   List: /sub list",
                "   Version: /sub version",
                "   Info: /sub info [proxy|host|group|server|player] <Name>",
                "   Start Server: /sub start <Subservers>",
                "   Restart Server: /sub restart <Subservers>",
                "   Stop Server: /sub stop <Subservers>",
                "   Terminate Server: /sub kill <Subservers>",
                "   Command Server: /sub cmd <Subservers> <Command> [Args...]",
                "   Create Server: /sub create <Name> <Host> <Template> [Version] [Port]",
                "   Update Server: /sub update <Subservers> [[Template] <Version>]",
                "   Remove Server: /sub delete <Subservers>",
                "",
                "   To see BungeeCord Supplied Commands, please visit:",
                "   https://www.spigotmc.org/wiki/bungeecord-commands/"
        };
    }

    /**
     * Suggest command arguments
     *
     * @param sender Sender
     * @param args Arguments
     * @return The validator's response and list of possible arguments
     */
    @SuppressWarnings("unchecked")
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        String Last = (args.length > 0)?args[args.length - 1]:"";
        String last = Last.toLowerCase();

        if (plugin.api.getSubDataNetwork()[0] == null) {
            if (sender instanceof ConsoleCommandSender)
                new IllegalStateException("SubData is not connected").printStackTrace();
            return Collections.emptyList();
        } else if (sender instanceof ProxiedPlayer && (!permitted.keySet().contains(((ProxiedPlayer) sender).getUniqueId()) || !permitted.get(((ProxiedPlayer) sender).getUniqueId()).keySet().contains(((ProxiedPlayer) sender).getServer().getInfo())
                || !permitted.get(((ProxiedPlayer) sender).getUniqueId()).get(((ProxiedPlayer) sender).getServer().getInfo()).value())) {
            if (permitted.keySet().contains(((ProxiedPlayer) sender).getUniqueId()) && permitted.get(((ProxiedPlayer) sender).getUniqueId()).keySet().contains(((ProxiedPlayer) sender).getServer().getInfo())
                    && permitted.get(((ProxiedPlayer) sender).getUniqueId()).get(((ProxiedPlayer) sender).getServer().getInfo()).key() == null) {
                // do nothing
            } else if (!permitted.keySet().contains(((ProxiedPlayer) sender).getUniqueId()) || !permitted.get(((ProxiedPlayer) sender).getUniqueId()).keySet().contains(((ProxiedPlayer) sender).getServer().getInfo())
                    || Calendar.getInstance().getTime().getTime() - permitted.get(((ProxiedPlayer) sender).getUniqueId()).get(((ProxiedPlayer) sender).getServer().getInfo()).key() >= TimeUnit.MINUTES.toMillis(1)) {
                if (!(((ProxiedPlayer) sender).getServer().getInfo() instanceof ServerImpl) || ((ServerImpl) ((ProxiedPlayer) sender).getServer().getInfo()).getSubData()[0] == null) {
                    HashMap<ServerInfo, Pair<Long, Boolean>> map = (permitted.keySet().contains(((ProxiedPlayer) sender).getUniqueId()))? permitted.get(((ProxiedPlayer) sender).getUniqueId()):new HashMap<ServerInfo, Pair<Long, Boolean>>();
                    map.put(((ProxiedPlayer) sender).getServer().getInfo(), new ContainedPair<>(Calendar.getInstance().getTime().getTime(), false));
                    permitted.put(((ProxiedPlayer) sender).getUniqueId(), map);
                } else {
                    HashMap<ServerInfo, Pair<Long, Boolean>> map = (permitted.keySet().contains(((ProxiedPlayer) sender).getUniqueId()))? permitted.get(((ProxiedPlayer) sender).getUniqueId()):new HashMap<ServerInfo, Pair<Long, Boolean>>();
                    map.put(((ProxiedPlayer) sender).getServer().getInfo(), new ContainedPair<>(null, false));
                    permitted.put(((ProxiedPlayer) sender).getUniqueId(), map);
                    ((SubDataSender) ((ServerImpl) ((ProxiedPlayer) sender).getServer().getInfo()).getSubData()[0]).sendPacket(new PacketCheckPermission(((ProxiedPlayer) sender).getUniqueId(), "subservers.command", result -> {
                        map.put(((ProxiedPlayer) sender).getServer().getInfo(), new ContainedPair<>(Calendar.getInstance().getTime().getTime(), result));
                    }));
                }
            }
            return Collections.emptyList();
        } else if (args.length <= 1) {
            List<String> cmds = new ArrayList<>();
            cmds.addAll(Arrays.asList("help", "list", "info", "status", "version", "start", "restart", "stop", "kill", "terminate", "cmd", "command", "create", "update", "upgrade"));
            if (!(sender instanceof ProxiedPlayer)) cmds.addAll(Arrays.asList("reload", "sudo", "screen", "remove", "delete", "restore"));

            updateCache();

            List<String> list = new ArrayList<String>();
            for (String cmd : cmds) {
                if (cmd.startsWith(last)) list.add(Last + cmd.substring(last.length()));
            }
            return list;
        } else {
            if (args[0].equals("info") || args[0].equals("status")) {
                ReturnRunnable<Collection<String>> getPlayers = () -> {
                    LinkedList<String> names = new LinkedList<String>();
                    for (ProxiedPlayer player : plugin.getPlayers()) names.add(player.getName());
                    for (CachedPlayer player : plugin.api.getRemotePlayers().values()) if (!names.contains(player.getName())) names.add(player.getName());
                    Collections.sort(names);
                    return names;
                };

                updateCache();

                if (args.length == 2) {
                    List<String> list = new ArrayList<String>();
                    List<String> subcommands = new ArrayList<String>();
                    subcommands.add("proxy");
                    subcommands.add("host");
                    subcommands.add("group");
                    subcommands.add("server");
                    subcommands.add("subserver");
                    subcommands.add("player");
                    for (String command : subcommands) {
                        if (!list.contains(command) && command.toLowerCase().startsWith(last))
                            list.add(Last + command.substring(last.length()));
                    }
                    Proxy master = proxyMasterCache;
                    if (master != null && !list.contains(master.getName()) && master.getName().toLowerCase().startsWith(last))
                        list.add(Last + master.getName().substring(last.length()));
                    for (Proxy proxy : proxyCache.values()) {
                        if (!list.contains(proxy.getName()) && proxy.getName().toLowerCase().startsWith(last))
                            list.add(Last + proxy.getName().substring(last.length()));
                    }
                    for (Host host : hostCache.values()) {
                        if (!list.contains(host.getName()) && host.getName().toLowerCase().startsWith(last))
                            list.add(Last + host.getName().substring(last.length()));
                    }
                    for (String group : groupCache.keySet()) {
                        if (!list.contains(group) && group.toLowerCase().startsWith(last))
                            list.add(Last + group.substring(last.length()));
                    }
                    for (ServerImpl server : plugin.servers.values()) {
                        if (!list.contains(server.getName()) && server.getName().toLowerCase().startsWith(last))
                            list.add(Last + server.getName().substring(last.length()));
                    }
                    for (String player : getPlayers.run()) {
                        if (!list.contains(player) && player.toLowerCase().startsWith(last))
                            list.add(Last + player.substring(last.length()));
                    }
                    return list;
                } else if (args.length == 3) {
                    List<String> list = new ArrayList<String>();

                    switch (args[1].toLowerCase()) {
                        case "p":
                        case "proxy":
                            Proxy master = proxyMasterCache;
                            if (master != null && master.getName().toLowerCase().startsWith(last))
                                list.add(Last + master.getName().substring(last.length()));
                            for (Proxy proxy : proxyCache.values()) {
                                if (!list.contains(proxy.getName()) && proxy.getName().toLowerCase().startsWith(last))
                                    list.add(Last + proxy.getName().substring(last.length()));
                            }
                            break;
                        case "h":
                        case "host":
                            for (Host host : hostCache.values()) {
                                if (host.getName().toLowerCase().startsWith(last))
                                    list.add(Last + host.getName().substring(last.length()));
                            }
                            break;
                        case "g":
                        case "group":
                            for (String group : groupCache.keySet()) {
                                if (group.toLowerCase().startsWith(last))
                                    list.add(Last + group.substring(last.length()));
                            }
                            break;
                        case "s":
                        case "server":
                        case "subserver":
                            for (ServerImpl server : plugin.servers.values()) {
                                if ((!args[1].equalsIgnoreCase("subserver") || server instanceof SubServerImpl) && server.getName().toLowerCase().startsWith(last))
                                    list.add(Last + server.getName().substring(last.length()));
                            }
                            break;
                        case "player":
                            for (String player : getPlayers.run()) {
                                if (player.toLowerCase().startsWith(last))
                                    list.add(Last + player.substring(last.length()));
                            }
                            break;
                    }
                    return list;
                } else {
                    return Collections.emptyList();
                }
            } else if (!(sender instanceof ProxiedPlayer) && (args[0].equals("restore"))) {
             /* if (args[0].equals("restore")) */ {
                    if (args.length == 2) {
                        return Collections.singletonList("<Subserver>");
                    } else {
                        return Collections.emptyList();
                    }
                }
            } else if (args[0].equals("start") ||
                    args[0].equals("restart") ||
                    args[0].equals("stop") ||
                    args[0].equals("kill") || args[0].equals("terminate") ||
                    args[0].equals("cmd") || args[0].equals("command") ||
                    args[0].equals("update") || args[0].equals("upgrade") ||
                    (!(sender instanceof ProxiedPlayer) && (
                            args[0].equals("sudo") || args[0].equals("screen") ||
                                    args[0].equals("remove") || args[0].equals("del") || args[0].equals("delete")
                    ))) {
                List<String> list = new ArrayList<String>();
                RawServerSelection select = selectRawServers(null, args, 1, true);
                if (select.last != null) {
                    if (last.startsWith("::")) {
                        Map<String, Host> hosts = hostCache;
                        if (hosts.size() > 0) {
                            if (Arrays.binarySearch(select.selection, "::*") < 0 && "::*".startsWith(last)) list.add("::*");
                            if (sender instanceof ProxiedPlayer && Arrays.binarySearch(select.selection, "::.") < 0 && "::.".startsWith(last)) list.add("::.");
                            for (Host host : hosts.values()) {
                                String name = "::" + host.getName();
                                if (Arrays.binarySearch(select.selection, name.toLowerCase()) < 0 && name.toLowerCase().startsWith(last)) list.add(Last + name.substring(last.length()));
                            }
                        }
                        return list;
                    } else if (last.startsWith(":")) {
                        Map<String, List<Server>> groups = groupCache;
                        if (groups.size() > 0) {
                            if (Arrays.binarySearch(select.selection, ":*") < 0 && ":*".startsWith(last)) list.add(":*");
                            if (sender instanceof ProxiedPlayer && Arrays.binarySearch(select.selection, ":.") < 0 && ":.".startsWith(last)) list.add(":.");
                            for (String group : groups.keySet()) {
                                group = ":" + group;
                                if (Arrays.binarySearch(select.selection, group.toLowerCase()) < 0 && group.toLowerCase().startsWith(last)) list.add(Last + group.substring(last.length()));
                            }
                        }
                        return list;
                    } else {
                        Map<String, ServerImpl> subservers = plugin.servers;
                        if (subservers.size() > 0) {
                            if (Arrays.binarySearch(select.selection, "*") < 0 && "*".startsWith(last)) list.add("*");
                            if (sender instanceof ProxiedPlayer && Arrays.binarySearch(select.selection, ".") < 0 && ".".startsWith(last)) list.add(".");
                            for (ServerImpl server : subservers.values()) {
                                if (server instanceof SubServerImpl && Arrays.binarySearch(select.selection, server.getName().toLowerCase()) < 0 && server.getName().toLowerCase().startsWith(last)) list.add(Last + server.getName().substring(last.length()));
                            }
                        }
                        return list;
                    }
                } else if (args[0].equals("cmd") || args[0].equals("command")) {
                    if (select.args.length == 3) {
                        return Collections.singletonList("<Command>");
                    } else {
                        return Collections.singletonList("[Args...]");
                    }
                } else if (args[0].equals("update") || args[0].equals("upgrade")) {
                    if (select.args.length == 3) {
                        return Arrays.asList("[Template]", "[Version]");
                    } else if (select.args.length == 4) {
                        return Collections.singletonList("<Version>");
                    }
                }
                return Collections.emptyList();
            } else if (args[0].equals("create")) {
                updateCache();
                if (args.length == 2) {
                    return Collections.singletonList("<Name>");
                } else if (args.length == 3) {
                    List<String> list = new ArrayList<String>();
                    for (Host host : hostCache.values()) {
                        if (host.getName().toLowerCase().startsWith(last)) list.add(Last + host.getName().substring(last.length()));
                    }
                    return list;
                } else if (args.length == 4) {
                    List<String> list = new ArrayList<String>();
                    Map<String, Host> hosts = hostCache;
                    if (!hosts.keySet().contains(args[2].toLowerCase())) {
                        list.add("<Template>");
                    } else {
                        for (SubCreator.ServerTemplate template : hosts.get(args[2].toLowerCase()).getCreator().getTemplates().values()) {
                            if (template.getName().toLowerCase().startsWith(last)) list.add(Last + template.getName().substring(last.length()));
                        }
                    }
                    return list;
                } else if (args.length == 5) {
                    return Collections.singletonList("[Version]");
                } else if (args.length == 6) {
                    if (last.length() > 0) {
                        if (Util.isException(() -> Integer.parseInt(last)) || Integer.parseInt(last) <= 0 || Integer.parseInt(last) > 65535) {
                            return Collections.emptyList();
                        }
                    }
                    return Collections.singletonList("[Port]");
                } else {
                    return Collections.emptyList();
                }
            } else if (sender instanceof ProxiedPlayer && (args[0].equals("tp") || args[0].equals("teleport"))) {
                if (args.length == 2 || args.length == 3) {
                    List<String> list = new ArrayList<String>();
                    if (args.length == 2) {
                        list.add("@p");
                        list.add("@a");
                        list.add("@r");
                        list.add("@s");

                        List<UUID> used = new ArrayList<UUID>();
                        for (ProxiedPlayer player : ((ProxiedPlayer) sender).getServer().getInfo().getPlayers()) {
                            if (player.getName().toLowerCase().startsWith(last)) list.add(Last + player.getName().substring(last.length()));
                            used.add(player.getUniqueId());
                        }

                        if (((ProxiedPlayer) sender).getServer().getInfo() instanceof ServerImpl) {
                            for (CachedPlayer player : ((ServerImpl) ((ProxiedPlayer) sender).getServer().getInfo()).getRemotePlayers()) {
                                if (!used.contains(player.getUniqueId())) {
                                    if (player.getName().toLowerCase().startsWith(last)) list.add(Last + player.getName().substring(last.length()));
                                    used.add(player.getUniqueId());
                                }
                            }
                        }
                    }
                    for (ServerImpl server : plugin.servers.values()) {
                        if (server.getName().toLowerCase().startsWith(last)) list.add(Last + server.getName().substring(last.length()));
                    }
                    return list;
                } else {
                    return Collections.emptyList();
                }
            } else {
                return Collections.emptyList();
            }
        }
    }

    private RawServerSelection selectRawServers(CommandSender sender, String[] rargs, int index, boolean mode) {
        LinkedList<String> msgs = new LinkedList<String>();
        LinkedList<String> args = new LinkedList<String>();
        LinkedList<String> selection = new LinkedList<>();
        LinkedList<ServerImpl> servers = new LinkedList<ServerImpl>();
        String last = null;

        updateCache();

        int i = 0;
        while (i < index) {
            args.add(rargs[i]);
            ++i;
        }

        Map<String, Host> hostMap = null;
        Map<String, List<Server>> groupMap = null;
        Map<String, ServerImpl> serverMap = null;

        StringBuilder completed = new StringBuilder();
        for (boolean run = true; run && i < rargs.length; i++) {
            String current = last = rargs[i];
            completed.append(current);
            if (current.endsWith(",")) {
                current = current.substring(0, current.length() - 1);
                completed.append(' ');
            } else run = false;
            selection.add(current.toLowerCase());

            if (current.length() > 0) {
                LinkedList<ServerImpl> select = new LinkedList<ServerImpl>();
                if (serverMap == null) serverMap = plugin.servers;

                if (current.startsWith("::") && current.length() > 2) {
                    current = current.substring(2);
                    if (hostMap == null) hostMap = hostCache;

                    if (current.equals("*")) {
                        for (Host host : hostMap.values()) {
                            for (SubServer server : host.getSubServers().values()) {
                                ServerImpl translated = serverMap.getOrDefault(server.getName().toLowerCase(), null);
                                if (translated != null) select.add(translated);
                            }
                        }
                    } else {
                        Host host = hostMap.getOrDefault(current.toLowerCase(), null);
                        if (host != null) {
                            for (SubServer server : host.getSubServers().values()) {
                                ServerImpl translated = serverMap.getOrDefault(server.getName().toLowerCase(), null);
                                if (translated != null) select.add(translated);
                            }
                            if (select.size() <= 0) {
                                String msg = "SubServers > There are no " + ((mode)?"sub":"") + "servers on host: " + host.getName();
                                if (sender != null) sender.sendMessage(msg);
                                msgs.add(msg);
                            }
                        } else {
                            String msg = "SubServers > There is no host with name: " + current;
                            if (sender != null) sender.sendMessage(msg);
                            msgs.add(msg);
                        }
                    }
                } else if (current.startsWith(":") && current.length() > 1) {
                    current = current.substring(1);
                    if (groupMap == null) groupMap = groupCache;

                    if (current.equals("*")) {
                        for (List<Server> group : groupMap.values()) for (Server server : group) {
                            if (!mode || server instanceof SubServer) {
                                ServerImpl translated = serverMap.getOrDefault(server.getName().toLowerCase(), null);
                                if (translated != null) select.add(translated);
                            }
                        }
                    } else {
                        Map.Entry<String, List<Server>> group = null;
                        for (Map.Entry<String, List<Server>> entry : groupMap.entrySet()) if (current.equalsIgnoreCase(entry.getKey())) {
                            group = entry;
                            break;
                        }
                        if (group != null) {
                            for (Server server : group.getValue()) {
                                if (!mode || server instanceof SubServer) {
                                    ServerImpl translated = serverMap.getOrDefault(server.getName().toLowerCase(), null);
                                    if (translated != null) select.add(translated);
                                }
                            }
                            if (select.size() <= 0) {
                                String msg = "SubServers > There are no " + ((mode)?"sub":"") + "servers in group: " + group.getKey();
                                if (sender != null) sender.sendMessage(msg);
                                msgs.add(msg);
                            }
                        } else {
                            String msg = "SubServers > There is no group with name: " + current;
                            if (sender != null) sender.sendMessage(msg);
                            msgs.add(msg);
                        }
                    }
                } else {

                    if (current.equals("*")) {
                        for (ServerImpl server : serverMap.values()) {
                            if (!mode || server instanceof SubServerImpl) select.add(server);
                        }
                    } else {
                        ServerImpl server = serverMap.getOrDefault(current.toLowerCase(), null);
                        if (server != null) {
                            select.add(server);
                        } else {
                            String msg = "SubServers > There is no " + ((mode)?"sub":"") + "server with name: " + current;
                            if (sender != null) sender.sendMessage(msg);
                            msgs.add(msg);
                        }
                    }
                }

                for (ServerImpl server : select) {
                    if (!servers.contains(server)) servers.add(server);
                }
            }
        }
        args.add(completed.toString());

        while (i < rargs.length) {
            args.add(rargs[i]);
            last = null;
            i++;
        }

        LinkedList<SubServerImpl> subservers = new LinkedList<SubServerImpl>();
        for (ServerImpl server : servers) if (server instanceof SubServerImpl) subservers.add((SubServerImpl) server);

        if ((!mode && servers.size() <= 0) || (mode && subservers.size() <= 0)) {
            String msg = "SubServers > No " + ((mode)?"sub":"") + "servers were selected";
            if (sender != null) sender.sendMessage(msg);
            msgs.add(msg);
        }

        return new RawServerSelection(msgs, selection, servers, subservers, args, last);
    }
    private static final class RawServerSelection {
        private final String[] msgs;
        private final String[] selection;
        private final ServerImpl[] servers;
        private final SubServerImpl[] subservers;
        private final String[] args;
        private final String last;

        private RawServerSelection(List<String> msgs, List<String> selection, List<ServerImpl> servers, List<SubServerImpl> subservers, List<String> args, String last) {
            this.msgs = msgs.toArray(new String[0]);
            this.selection = selection.toArray(new String[0]);
            this.servers = servers.toArray(new ServerImpl[0]);
            this.subservers = subservers.toArray(new SubServerImpl[0]);
            this.args = args.toArray(new String[0]);
            this.last = last;

            Arrays.sort(this.selection);
        }
    }

    private void updateCache() {
        if (Calendar.getInstance().getTime().getTime() - cacheDate >= TimeUnit.MINUTES.toMillis(1)) {
            cacheDate = Calendar.getInstance().getTime().getTime();
            plugin.api.getProxies(proxies -> {
                proxyCache = new TreeMap<>(proxies);
                cacheDate = Calendar.getInstance().getTime().getTime();
            });
            plugin.api.getMasterProxy(master -> {
                proxyMasterCache = master;
                cacheDate = Calendar.getInstance().getTime().getTime();
            });
            plugin.api.getHosts(hosts -> {
                hostCache = new TreeMap<>(hosts);
                cacheDate = Calendar.getInstance().getTime().getTime();
            });
            plugin.api.getGroups(groups -> {
                groupCache = new TreeMap<>(groups);
                cacheDate = Calendar.getInstance().getTime().getTime();
            });
        }
    }

    /**
     * BungeeCord /server
     */
    @SuppressWarnings("unchecked")
    public static final class BungeeServer extends Command implements TabExecutor {
        private ExProxy plugin;
        BungeeServer(ExProxy plugin, String command) {
            super(command, "bungeecord.command.server");
            this.plugin = plugin;
        }

        /**
         * Override /server
         *
         * @param sender Sender
         * @param args Arguments
         */
        @SuppressWarnings("deprecation")
        @Override
        public void execute(CommandSender sender, String[] args) {
            if (plugin.lang == null) {
                throw new IllegalStateException("There are no lang options available at this time");
            } else {
                if (sender instanceof ProxiedPlayer) {
                    if (args.length > 0) {
                        Map<String, ServerImpl> servers = plugin.servers;
                        if (servers.keySet().contains(args[0].toLowerCase())) {
                            ((ProxiedPlayer) sender).connect(servers.get(args[0].toLowerCase()));
                        } else {
                            sender.sendMessage(plugin.api.getLang("SubServers", "Bungee.Server.Invalid"));
                        }
                    } else {
                        int i = 0;
                        TextComponent serverm = new TextComponent(ChatColor.RESET.toString());
                        TextComponent div = new TextComponent(plugin.api.getLang("SubServers", "Bungee.Server.Divider"));
                        for (ServerImpl server : plugin.servers.values()) {
                            if (!server.isHidden() && server.canAccess(sender) && (!(server instanceof SubServerImpl) || ((SubServerImpl) server).isRunning())) {
                                if (i != 0) serverm.addExtra(div);
                                TextComponent message = new TextComponent(plugin.api.getLang("SubServers", "Bungee.Server.List").replace("$str$", server.getDisplayName()));
                                try {
                                    message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponent[]{new TextComponent(plugin.api.getLang("SubServers", "Bungee.Server.Hover").replace("$int$", Integer.toString(server.getRemotePlayers().size())))}));
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                message.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/server " + server.getName()));
                                serverm.addExtra(message);
                                i++;
                            }
                        }
                        sender.sendMessages(
                                plugin.api.getLang("SubServers", "Bungee.Server.Current").replace("$str$", ((ProxiedPlayer) sender).getServer().getInfo().getName()),
                                plugin.api.getLang("SubServers", "Bungee.Server.Available"));
                        sender.sendMessage(serverm);
                    }
                } else {
                    sender.sendMessage(plugin.api.getLang("SubServers", "Command.Generic.Player-Only"));
                }
            }
        }

        /**
         * Suggest command arguments
         *
         * @param sender Sender
         * @param args Arguments
         * @return The validator's response and list of possible arguments
         */
        public List<String> onTabComplete(CommandSender sender, String[] args) {
            if (plugin.lang != null && args.length <= 1) {
                String last = (args.length > 0)?args[args.length - 1].toLowerCase():"";
                List<String> list = new ArrayList<String>();
                if (last.length() == 0) {
                    for (ServerImpl server : plugin.servers.values()) {
                        if (!server.isHidden()) list.add(server.getName());
                    }
                    return list;
                } else {
                    for (ServerImpl server : plugin.servers.values()) {
                        if (server.getName().toLowerCase().startsWith(last) && !server.isHidden()) list.add(server.getName());
                    }
                    return list;
                }
            } else {
                return Collections.emptyList();
            }
        }
    }

    /**
     * BungeeCord /glist
     */
    public static final class BungeeList extends Command {
        private ExProxy plugin;
        BungeeList(ExProxy plugin, String command) {
            super(command, "bungeecord.command.list");
            this.plugin = plugin;
        }

        /**
         * Override /glist
         *
         * @param sender
         * @param args
         */
        @SuppressWarnings("deprecation")
        @Override
        public void execute(CommandSender sender, String[] args) {
            if (plugin.lang == null) {
                throw new IllegalStateException("There are no lang options available at this time");
            } else {
                List<String> messages = new LinkedList<String>();
                int players = 0;
                for (ServerImpl server : plugin.servers.values()) {
                    List<String> playerlist = new ArrayList<String>();
                    for (CachedPlayer player : server.getRemotePlayers()) playerlist.add(player.getName());
                    Collections.sort(playerlist);

                    players += playerlist.size();
                    if (!server.isHidden() && (!(server instanceof SubServerImpl) || ((SubServerImpl) server).isRunning())) {
                        int i = 0;
                        String message = plugin.api.getLang("SubServers", "Bungee.List.Format").replace("$str$", server.getDisplayName()).replace("$int$", Integer.toString(playerlist.size()));
                        for (String player : playerlist) {
                            if (i != 0) message += plugin.api.getLang("SubServers", "Bungee.List.Divider");
                            message += plugin.api.getLang("SubServers", "Bungee.List.List").replace("$str$", player);
                            i++;
                        }
                        messages.add(message);
                    }
                }
                sender.sendMessages(messages.toArray(new String[messages.size()]));
                sender.sendMessage(plugin.api.getLang("SubServers", "Bungee.List.Total").replace("$int$", Integer.toString(players)));
            }
        }
    }
}
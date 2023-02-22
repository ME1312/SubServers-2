package net.ME1312.SubServers.Velocity;

import net.ME1312.Galaxi.Library.Container.ContainedPair;
import net.ME1312.Galaxi.Library.Container.Container;
import net.ME1312.Galaxi.Library.Container.Pair;
import net.ME1312.Galaxi.Library.Container.Value;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Merger;
import net.ME1312.Galaxi.Library.Platform;
import net.ME1312.Galaxi.Library.Try;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.SubData.Client.SubDataClient;
import net.ME1312.SubData.Client.SubDataSender;
import net.ME1312.SubServers.Client.Common.Network.API.*;
import net.ME1312.SubServers.Client.Common.Network.Packet.PacketCreateServer;
import net.ME1312.SubServers.Client.Common.Network.Packet.PacketUpdateServer;
import net.ME1312.SubServers.Velocity.Library.Compatibility.ChatColor;
import net.ME1312.SubServers.Velocity.Network.Packet.PacketCheckPermission;
import net.ME1312.SubServers.Velocity.Network.Packet.PacketInExRunEvent;
import net.ME1312.SubServers.Velocity.Server.CachedPlayer;
import net.ME1312.SubServers.Velocity.Server.ServerData;
import net.ME1312.SubServers.Velocity.Server.SubServerData;

import com.google.gson.Gson;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.RawCommand;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

@SuppressWarnings("deprecation")
public final class SubCommand implements SimpleCommand {
    static final MinecraftChannelIdentifier pmc = MinecraftChannelIdentifier.create("subservers", "input");
    static HashMap<UUID, HashMap<ServerInfo, Pair<Long, Boolean>>> permitted = new HashMap<UUID, HashMap<ServerInfo, Pair<Long, Boolean>>>();
    private Map<String, Proxy> proxyCache = Collections.emptyMap();
    private Map<String, Host> hostCache = Collections.emptyMap();
    private Map<String, List<Server>> groupCache = Collections.emptyMap();
    private Proxy proxyMasterCache = null;
    private long cacheDate = 0;
    private ExProxy plugin;

    SubCommand(ExProxy plugin) {
        this.plugin = plugin;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void execute(Invocation invocation) {
        CommandSource sender = invocation.source();
        String label = '/' + invocation.alias();
        String[] args = invocation.arguments();

        if (!(sender instanceof Player)) {
            if (plugin.api.getSubDataNetwork()[0] == null || plugin.api.getSubDataNetwork()[0].isClosed()) {
                new IllegalStateException("SubData is not connected").printStackTrace();
                if (!(sender instanceof ConsoleCommandSource)) sender.sendMessage(Component.text("An exception has occurred while running this command", NamedTextColor.RED));
            } else {
                if (args.length > 0) {
                    if (args[0].equalsIgnoreCase("help") || args[0].equalsIgnoreCase("?")) {
                        for (String s : printHelp()) sender.sendMessage(Component.text(s));
                    } else if (args[0].equalsIgnoreCase("version") || args[0].equalsIgnoreCase("ver")) {
                        sender.sendMessage(Component.text("SubServers > These are the platforms and versions that are running SubServers.Sync:"));
                        sender.sendMessage(Component.text("  " + Platform.getSystemName() + ' ' + Platform.getSystemVersion() + ((Platform.getSystemBuild() != null)?" (" + Platform.getSystemBuild() + ')':"") + ((!Platform.getSystemArchitecture().equals("unknown"))?" [" + Platform.getSystemArchitecture() + ']':"") + ','));
                        sender.sendMessage(Component.text("  Java " + Platform.getJavaVersion() + ((!Platform.getJavaArchitecture().equals("unknown"))?" [" + Platform.getJavaArchitecture() + ']':"") + ','));
                        sender.sendMessage(Component.text("  " + ExProxy.getInstance().getVersion().getName() + ' ' + ExProxy.getInstance().getVersion().getVersion() + ','));
                        sender.sendMessage(Component.text("  SubServers.Sync v" + plugin.version.toExtendedString() + ((plugin.api.getPluginBuild() != null)?" (" + plugin.api.getPluginBuild() + ')':"")));
                        sender.sendMessage(Component.text(""));
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
                                    sender.sendMessage(Component.text("You are on the latest version."));
                                } else {
                                    sender.sendMessage(Component.text("SubServers.Sync v" + updversion + " is available. You are " + updcount + " version" + ((updcount == 1)?"":"s") + " behind."));
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
                                sender.sendMessage(Component.text("Group/Server List:"));
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
                                    sender.sendMessage(ChatColor.convertColor(message));
                                    i = 0;
                                    sent = true;
                                }
                                if (!sent) sender.sendMessage(Component.text("(none)"));
                                sent = false;
                            }
                            sender.sendMessage(Component.text("Host/SubServer List:"));
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
                                sender.sendMessage(ChatColor.convertColor(message));
                                i = 0;
                                sent = true;
                            }
                            if (!sent) sender.sendMessage(Component.text("(none)"));
                            sender.sendMessage(Component.text("Server List:"));
                            String message = "  ";
                            for (Server server : servers.values()) if (!(server instanceof SubServer)) {
                                if (i != 0) message += div;
                                message += ChatColor.WHITE + server.getDisplayName() + " [" + ((server.getName().equals(server.getDisplayName()))?"":server.getName()+ChatColor.stripColor(div)) + server.getAddress().getAddress().getHostAddress()+':'+server.getAddress().getPort() + "]";
                                i++;
                            }
                            if (i == 0) message += ChatColor.RESET + "(none)";
                            sender.sendMessage(ChatColor.convertColor(message));
                            if (proxies.keySet().size() > 0) {
                                sender.sendMessage(Component.text("Proxy List:"));
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
                                sender.sendMessage(ChatColor.convertColor(message));
                            }
                        })))));
                    } else if (args[0].equalsIgnoreCase("info") || args[0].equalsIgnoreCase("status")) {
                        if (args.length > 1) {
                            String type = (args.length > 2)?args[1]:null;
                            String name = args[(type != null)?2:1];

                            Runnable getPlayer = () -> plugin.api.getRemotePlayer(name, player -> {
                                if (player != null) {
                                    sender.sendMessage(ChatColor.convertColor("SubServers > Info on player: " + ChatColor.WHITE + player.getName()));
                                    if (player.getProxyName() != null) sender.sendMessage(ChatColor.convertColor(" -> Proxy: " + ChatColor.WHITE + player.getProxyName()));
                                    if (player.getServerName() != null) sender.sendMessage(ChatColor.convertColor(" -> Server: " + ChatColor.WHITE + player.getServerName()));
                                    if (player.getAddress() != null) sender.sendMessage(ChatColor.convertColor(" -> Address: " + ChatColor.WHITE + player.getAddress().getAddress().getHostAddress() + ':' + player.getAddress().getPort()));
                                    sender.sendMessage(ChatColor.convertColor(" -> UUID: " + ChatColor.AQUA + player.getUniqueId()));
                                } else {
                                    if (type == null) {
                                        sender.sendMessage(Component.text("SubServers > There is no object with that name"));
                                    } else {
                                        sender.sendMessage(Component.text("SubServers > There is no player with that name"));
                                    }
                                }
                            });
                            Runnable getServer = () -> plugin.api.getServer(name, server -> {
                                if (server != null) {
                                    sender.sendMessage(ChatColor.convertColor("SubServers > Info on " + ((server instanceof SubServer)?"sub":"") + "server: " + ChatColor.WHITE + server.getDisplayName()));
                                    if (!server.getName().equals(server.getDisplayName())) sender.sendMessage(ChatColor.convertColor(" -> System Name: " + ChatColor.WHITE + server.getName()));
                                    if (server instanceof SubServer) {
                                        sender.sendMessage(ChatColor.convertColor(" -> Available: " + ((((SubServer) server).isAvailable())?ChatColor.GREEN+"yes":ChatColor.RED+"no")));
                                        sender.sendMessage(ChatColor.convertColor(" -> Enabled: " + ((((SubServer) server).isEnabled())?ChatColor.GREEN+"yes":ChatColor.RED+"no")));
                                        if (!((SubServer) server).isEditable()) sender.sendMessage(ChatColor.convertColor(" -> Editable: " + ChatColor.RED + "no"));
                                        sender.sendMessage(ChatColor.convertColor(" -> Host: " + ChatColor.WHITE + ((SubServer) server).getHost()));
                                        if (((SubServer) server).getTemplate() != null) sender.sendMessage(ChatColor.convertColor(" -> Template: " + ChatColor.WHITE + ((SubServer) server).getHost()));
                                    }
                                    if (server.getGroups().size() > 0) sender.sendMessage(ChatColor.convertColor(" -> Group" + ((server.getGroups().size() > 1)?"s:":": " + ChatColor.WHITE + server.getGroups().get(0))));
                                    if (server.getGroups().size() > 1) for (String group : server.getGroups()) sender.sendMessage(ChatColor.convertColor("      - " + ChatColor.WHITE + group));
                                    sender.sendMessage(ChatColor.convertColor(" -> Address: " + ChatColor.WHITE + server.getAddress().getAddress().getHostAddress()+':'+server.getAddress().getPort()));
                                    if (server instanceof SubServer) sender.sendMessage(ChatColor.convertColor(" -> " + ((((SubServer) server).isOnline())?"Online":"Running") + ": " + ((((SubServer) server).isRunning())?ChatColor.GREEN+"yes":ChatColor.RED+"no")));
                                    if (!(server instanceof SubServer) || ((SubServer) server).isRunning()) {
                                        sender.sendMessage(ChatColor.convertColor(" -> Connected: " + ((server.getSubData()[0] != null)?ChatColor.GREEN+"yes"+((server.getSubData().length > 1)?ChatColor.AQUA+" +"+(server.getSubData().length-1)+" subchannel"+((server.getSubData().length == 2)?"":"s"):""):ChatColor.RED+"no")));
                                        sender.sendMessage(ChatColor.convertColor(" -> Players: " + ChatColor.AQUA + server.getRemotePlayers().size() + " online"));
                                    }
                                    sender.sendMessage(ChatColor.convertColor(" -> MOTD: " + ChatColor.WHITE + ChatColor.stripColor(server.getMotd())));
                                    if (server instanceof SubServer) {
                                        if (((SubServer) server).getStopAction() != SubServer.StopAction.NONE) sender.sendMessage(ChatColor.convertColor(" -> Stop Action: " + ChatColor.WHITE + ((SubServer) server).getStopAction().toString()));
                                        if (((SubServer) server).isStopping()) sender.sendMessage(ChatColor.convertColor(" -> Stopping: " + ChatColor.GREEN+"yes"));
                                    }
                                    sender.sendMessage(ChatColor.convertColor(" -> Signature: " + ChatColor.AQUA + server.getSignature()));
                                    if (server instanceof SubServer) sender.sendMessage(ChatColor.convertColor(" -> Logging: " + ((((SubServer) server).isLogging())?ChatColor.GREEN+"yes":ChatColor.RED+"no")));
                                    sender.sendMessage(ChatColor.convertColor(" -> Restricted: " + ((server.isRestricted())?ChatColor.GREEN+"yes":ChatColor.RED+"no")));
                                    if (server instanceof SubServer && ((SubServer) server).getIncompatibilities().size() > 0) {
                                        List<String> current = new ArrayList<String>();
                                        for (String other : ((SubServer) server).getCurrentIncompatibilities()) current.add(other.toLowerCase());
                                        sender.sendMessage(Component.text(" -> Incompatibilities:"));
                                        for (String other : ((SubServer) server).getIncompatibilities()) sender.sendMessage(ChatColor.convertColor("      - " + ((current.contains(other.toLowerCase()))?ChatColor.WHITE:ChatColor.GRAY) + other));
                                    }
                                    sender.sendMessage(ChatColor.convertColor(" -> Hidden: " + ((server.isHidden())?ChatColor.GREEN+"yes":ChatColor.RED+"no")));
                                } else {
                                    if (type == null) {
                                        getPlayer.run();
                                    } else {
                                        sender.sendMessage(Component.text("SubServers > There is no server with that name"));
                                    }
                                }
                            });
                            Runnable getGroup = () -> plugin.api.getGroup(name, group -> {
                                if (group != null) {
                                    sender.sendMessage(ChatColor.convertColor("SubServers > Info on group: " + ChatColor.WHITE + group.key()));
                                    sender.sendMessage(ChatColor.convertColor(" -> Servers: " + ((group.value().size() <= 0)?ChatColor.GRAY + "(none)":ChatColor.AQUA.toString() + group.value().size())));
                                    for (Server server : group.value()) sender.sendMessage(ChatColor.convertColor("      - " + ChatColor.WHITE + server.getDisplayName() + ((server.getName().equals(server.getDisplayName()))?"":" ["+server.getName()+']')));
                                } else {
                                    if (type == null) {
                                        getServer.run();
                                    } else {
                                        sender.sendMessage(Component.text("SubServers > There is no group with that name"));
                                    }
                                }
                            });
                            Runnable getHost = () -> plugin.api.getHost(name, host -> {
                                if (host != null) {
                                    sender.sendMessage(ChatColor.convertColor("SubServers > Info on host: " + ChatColor.WHITE + host.getDisplayName()));
                                    if (!host.getName().equals(host.getDisplayName())) sender.sendMessage(ChatColor.convertColor(" -> System Name: " + ChatColor.WHITE + host.getName()));
                                    sender.sendMessage(ChatColor.convertColor(" -> Available: " + ((host.isAvailable())?ChatColor.GREEN+"yes":ChatColor.RED+"no")));
                                    sender.sendMessage(ChatColor.convertColor(" -> Enabled: " + ((host.isEnabled())?ChatColor.GREEN+"yes":ChatColor.RED+"no")));
                                    sender.sendMessage(ChatColor.convertColor(" -> Address: " + ChatColor.WHITE + host.getAddress().getHostAddress()));
                                    if (host.getSubData().length > 0) sender.sendMessage(ChatColor.convertColor(" -> Connected: " + ((host.getSubData()[0] != null)?ChatColor.GREEN+"yes"+((host.getSubData().length > 1)?ChatColor.AQUA+" +"+(host.getSubData().length-1)+" subchannel"+((host.getSubData().length == 2)?"":"s"):""):ChatColor.RED+"no")));
                                    sender.sendMessage(ChatColor.convertColor(" -> SubServers: " + ((host.getSubServers().keySet().size() <= 0)?ChatColor.GRAY + "(none)":ChatColor.AQUA.toString() + host.getSubServers().keySet().size())));
                                    for (SubServer subserver : host.getSubServers().values()) sender.sendMessage(ChatColor.convertColor("      - " + ((subserver.isEnabled())?ChatColor.WHITE:ChatColor.GRAY) + subserver.getDisplayName() + ((subserver.getName().equals(subserver.getDisplayName()))?"":" ["+subserver.getName()+']')));
                                    sender.sendMessage(ChatColor.convertColor(" -> Templates: " + ((host.getCreator().getTemplates().keySet().size() <= 0)?ChatColor.GRAY + "(none)":ChatColor.AQUA.toString() + host.getCreator().getTemplates().keySet().size())));
                                    for (SubCreator.ServerTemplate template : host.getCreator().getTemplates().values()) sender.sendMessage(ChatColor.convertColor("      - " + ((template.isEnabled())?ChatColor.WHITE:ChatColor.GRAY) + template.getDisplayName() + ((template.getName().equals(template.getDisplayName()))?"":" ["+template.getName()+']')));
                                    sender.sendMessage(ChatColor.convertColor(" -> Signature: " + ChatColor.AQUA + host.getSignature()));
                                } else {
                                    if (type == null) {
                                        getGroup.run();
                                    } else {
                                        sender.sendMessage(Component.text("SubServers > There is no host with that name"));
                                    }
                                }
                            });
                            Runnable getProxy = () -> plugin.api.getProxy(name, proxy -> {
                                if (proxy != null) {
                                    sender.sendMessage(ChatColor.convertColor("SubServers > Info on proxy: " + ChatColor.WHITE + proxy.getDisplayName()));
                                    if (!proxy.getName().equals(proxy.getDisplayName())) sender.sendMessage(ChatColor.convertColor(" -> System Name: " + ChatColor.WHITE + proxy.getName()));
                                    if (!proxy.isMaster()) sender.sendMessage(ChatColor.convertColor(" -> Connected: " + ((proxy.getSubData()[0] != null)?ChatColor.GREEN+"yes"+((proxy.getSubData().length > 1)?ChatColor.AQUA+" +"+(proxy.getSubData().length-1)+" subchannel"+((proxy.getSubData().length == 2)?"":"s"):""):ChatColor.RED+"no")));
                                    else if (!proxy.getDisplayName().toLowerCase().contains("master")) sender.sendMessage(ChatColor.convertColor(" -> Type: " + ChatColor.WHITE + "Master"));
                                    sender.sendMessage(ChatColor.convertColor(" -> Players: " + ChatColor.AQUA + proxy.getPlayers().size() + " online"));
                                    sender.sendMessage(ChatColor.convertColor(" -> Signature: " + ChatColor.AQUA + proxy.getSignature()));
                                } else {
                                    if (type == null) {
                                        getHost.run();
                                    } else {
                                        sender.sendMessage(Component.text("SubServers > There is no proxy with that name"));
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
                                    case "u":
                                    case "user":
                                    case "player":
                                        getPlayer.run();
                                        break;
                                    default:
                                        sender.sendMessage(Component.text("SubServers > There is no object type with that name"));
                                }
                            }
                        } else {
                            sender.sendMessage(Component.text("SubServers > Usage: " + label + " " + args[0].toLowerCase() + " [proxy|host|group|server|player] <Name>"));
                        }
                    } else if (args[0].equalsIgnoreCase("start")) {
                        if (args.length > 1) {
                            selectServers(sender, args, 1, true, select -> {
                                if (select.subservers.length > 0) {
                                    Container<Integer> success = new Container<Integer>(0);
                                    Container<Integer> running = new Container<Integer>(0);
                                    Merger merge = new Merger(() -> {
                                        if (running.value > 0) sender.sendMessage(Component.text("SubServers > " + running.value + " subserver"+((running.value == 1)?" was":"s were") + " already running"));
                                        if (success.value > 0) sender.sendMessage(Component.text("SubServers > Started " + success.value + " subserver"+((success.value == 1)?"":"s")));
                                    });
                                    for (SubServer server : select.subservers) {
                                        merge.reserve();
                                        server.start(null, response -> {
                                            switch (response) {
                                                case 3:
                                                case 4:
                                                    sender.sendMessage(Component.text("SubServers > Subserver " + server.getName() + " has disappeared"));
                                                    break;
                                                case 5:
                                                    sender.sendMessage(Component.text("SubServers > The host for " + server.getName() + " is not available"));
                                                    break;
                                                case 6:
                                                    sender.sendMessage(Component.text("SubServers > The host for " + server.getName() + " is not enabled"));
                                                    break;
                                                case 7:
                                                    sender.sendMessage(Component.text("SubServers > Subserver " + server.getName() + " is not available"));
                                                    break;
                                                case 8:
                                                    sender.sendMessage(Component.text("SubServers > SubServer " + server.getName() + " is not enabled"));
                                                    break;
                                                case 9:
                                                    running.value++;
                                                    break;
                                                case 10:
                                                    sender.sendMessage(Component.text("SubServers > Subserver " + server.getName() + " cannot start while incompatible server(s) are running"));
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
                            sender.sendMessage(Component.text("Usage: " + label + " " + args[0].toLowerCase() + " <Subservers>"));
                        }
                    } else if (args[0].equalsIgnoreCase("restart")) {
                        if (args.length > 1) {
                            selectServers(sender, args, 1, true, select -> {
                                if (select.subservers.length > 0) {
                                    // Step 5: Start the stopped Servers once more
                                    Consumer<SubServer> starter = server -> server.start(response -> {
                                        switch (response) {
                                            case 3:
                                            case 4:
                                                sender.sendMessage(Component.text("SubServers > Could not restart server: Subserver " + server.getName() + " has disappeared"));
                                                break;
                                            case 5:
                                                sender.sendMessage(Component.text("SubServers > Could not restart server: The host for " + server.getName() + " is no longer available"));
                                                break;
                                            case 6:
                                                sender.sendMessage(Component.text("SubServers > Could not restart server: The host for " + server.getName() + " is no longer enabled"));
                                                break;
                                            case 7:
                                                sender.sendMessage(Component.text("SubServers > Could not restart server: Subserver " + server.getName() + " is no longer available"));
                                                break;
                                            case 8:
                                                sender.sendMessage(Component.text("SubServers > Could not restart server: Subserver " + server.getName() + " is no longer enabled"));
                                                break;
                                            case 10:
                                                sender.sendMessage(Component.text("SubServers > Could not restart server: Subserver " + server.getName() + " cannot start while incompatible server(s) are running"));
                                                break;
                                            case 9:
                                            case 0:
                                                // success!
                                                break;
                                        }
                                    });

                                    // Step 4: Listen for stopped Servers
                                    final HashMap<String, SubServer> listening = new HashMap<String, SubServer>();
                                    PacketInExRunEvent.callback("SubStoppedEvent", new Consumer<ObjectMap<String>>() {
                                        @Override
                                        public void accept(ObjectMap<String> json) {
                                            try {
                                                if (listening.size() > 0) {
                                                    PacketInExRunEvent.callback("SubStoppedEvent", this);
                                                    String name = json.getString("server").toLowerCase();
                                                    if (listening.containsKey(name)) {
                                                        Timer timer = new Timer("SubServers.Sync::Server_Restart_Command_Handler(" + name + ")");
                                                        timer.schedule(new TimerTask() {
                                                            @Override
                                                            public void run() {
                                                                starter.accept(listening.get(name));
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
                                    Merger merge = new Merger(() -> {
                                        if (success.value > 0) sender.sendMessage(Component.text("SubServers > Restarting " + success.value + " subserver"+((success.value == 1)?"":"s")));
                                    });
                                    for (SubServer server : select.subservers) {
                                        merge.reserve();
                                        listening.put(server.getName().toLowerCase(), server);
                                        server.stop(response -> {
                                            if (response != 0) listening.remove(server.getName().toLowerCase());
                                            switch (response) {
                                                case 3:
                                                case 4:
                                                    sender.sendMessage(Component.text("Could not restart server: Subserver " + server.getName() + " has disappeared"));
                                                    break;
                                                case 5:
                                                    starter.accept(server);
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
                            sender.sendMessage(Component.text("Usage: " + label + " " + args[0].toLowerCase() + " <Subservers>"));
                        }
                    } else if (args[0].equalsIgnoreCase("stop")) {
                        if (args.length > 1) {
                            selectServers(sender, args, 1, true, select -> {
                                if (select.subservers.length > 0) {
                                    Container<Integer> success = new Container<Integer>(0);
                                    Container<Integer> running = new Container<Integer>(0);
                                    Merger merge = new Merger(() -> {
                                        if (running.value > 0) sender.sendMessage(Component.text("SubServers > " + running.value + " subserver"+((running.value == 1)?" was":"s were") + " already offline"));
                                        if (success.value > 0) sender.sendMessage(Component.text("SubServers > Stopping " + success.value + " subserver"+((success.value == 1)?"":"s")));
                                    });
                                    for (SubServer server : select.subservers) {
                                        merge.reserve();
                                        server.stop(response -> {
                                            switch (response) {
                                                case 3:
                                                case 4:
                                                    sender.sendMessage(Component.text("SubServers > Subserver " + server.getName() + " has disappeared"));
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
                            sender.sendMessage(Component.text("Usage: " + label + " " + args[0].toLowerCase() + " <Subservers>"));
                        }
                    } else if (args[0].equalsIgnoreCase("kill") || args[0].equalsIgnoreCase("terminate")) {
                        if (args.length > 1) {
                            selectServers(sender, args, 1, true, select -> {
                                if (select.subservers.length > 0) {
                                    Container<Integer> success = new Container<Integer>(0);
                                    Container<Integer> running = new Container<Integer>(0);
                                    Merger merge = new Merger(() -> {
                                        if (running.value > 0) sender.sendMessage(Component.text("SubServers > " + running.value + " subserver"+((running.value == 1)?" was":"s were") + " already offline"));
                                        if (success.value > 0) sender.sendMessage(Component.text("SubServers > Terminated " + success.value + " subserver"+((success.value == 1)?"":"s")));
                                    });
                                    for (SubServer server : select.subservers) {
                                        merge.reserve();
                                        server.terminate(response -> {
                                            switch (response) {
                                                case 3:
                                                case 4:
                                                    sender.sendMessage(Component.text("SubServers > Subserver " + server.getName() + " has disappeared"));
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
                            sender.sendMessage(Component.text("Usage: " + label + " " + args[0].toLowerCase() + " <Subservers>"));
                        }
                    } else if (args[0].equalsIgnoreCase("cmd") || args[0].equalsIgnoreCase("command")) {
                        if (args.length > 1) {
                            selectServers(sender, args, 1, false, select -> {
                                if (select.servers.length > 0) {
                                    if (select.args.length > 2) {
                                        StringBuilder builder = new StringBuilder(select.args[2]);
                                        for (int i = 3; i < select.args.length; i++) {
                                            builder.append(' ');
                                            builder.append(select.args[i]);
                                        }

                                        Container<Integer> success = new Container<Integer>(0);
                                        Container<Integer> running = new Container<Integer>(0);
                                        Merger merge = new Merger(() -> {
                                            if (running.value > 0) sender.sendMessage(Component.text("SubServers > " + running.value + " server"+((running.value == 1)?" was":"s were") + " offline"));
                                            if (success.value > 0) sender.sendMessage(Component.text("SubServers > Sent command to " + success.value + " server"+((success.value == 1)?"":"s")));
                                        });
                                        for (Server server : select.servers) {
                                            merge.reserve();
                                            server.command(builder.toString(), response -> {
                                                switch (response) {
                                                    case 3:
                                                    case 4:
                                                        sender.sendMessage(Component.text("SubServers > Server " + server.getName() + " has disappeared"));
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
                                        sender.sendMessage(Component.text("SubServers > No command was entered"));
                                    }
                                }
                            });
                        } else {
                            sender.sendMessage(Component.text("Usage: " + label + " " + args[0].toLowerCase() + " <Servers> <Command> [Args...]"));
                        }
                    } else if (args[0].equalsIgnoreCase("create")) {
                        if (args.length > 3) {
                            if (args.length > 5 && !Try.all.run(() -> Integer.parseInt(args[5]))) {
                                sender.sendMessage(Component.text("SubServers > Invalid port number"));
                            } else {
                                ((SubDataClient) SubAPI.getInstance().getSubDataNetwork()[0]).sendPacket(new PacketCreateServer(null, args[1], args[2],args[3], (args.length > 4)?new Version(args[4]):null, (args.length > 5)?Integer.parseInt(args[5]):null, data -> {
                                    switch (data.getInt(0x0001)) {
                                        case 3:
                                        case 4:
                                            sender.sendMessage(Component.text("SubServers > There is already a subserver with that name"));
                                            break;
                                        case 5:
                                            sender.sendMessage(Component.text("SubServers > There is no host with that name"));
                                            break;
                                        case 6:
                                            sender.sendMessage(Component.text("SubServers > That host is not available"));
                                            break;
                                        case 7:
                                            sender.sendMessage(Component.text("SubServers > That host is not enabled"));
                                            break;
                                        case 8:
                                            sender.sendMessage(Component.text("SubServers > There is no template with that name"));
                                            break;
                                        case 9:
                                            sender.sendMessage(Component.text("SubServers > That template is not enabled"));
                                            break;
                                        case 10:
                                            sender.sendMessage(Component.text("SubServers > That template requires a Minecraft version to be specified"));
                                            break;
                                        case 11:
                                            sender.sendMessage(Component.text("SubServers > Invalid port number"));
                                            break;
                                        case 0:
                                            sender.sendMessage(Component.text("SubServers > Creating subserver " + args[1]));
                                            break;
                                    }
                                }));
                            }
                        } else {
                            sender.sendMessage(Component.text("SubServers > Usage: " + label + " " + args[0].toLowerCase() + " <Name> <Host> <Template> [Version] [Port]"));
                        }
                    } else if (args[0].equalsIgnoreCase("update") || args[0].equalsIgnoreCase("upgrade")) {
                        if (args.length > 1) {
                            selectServers(sender, args, 1, true, select -> {
                                if (select.subservers.length > 0) {
                                    String template = (select.args.length > 3)?select.args[2].toLowerCase():null;
                                    Version version = (select.args.length > 2)?new Version(select.args[(template == null)?2:3]):null;
                                    boolean ts = template == null;

                                    Container<Integer> success = new Container<Integer>(0);
                                    Merger merge = new Merger(() -> {
                                        if (success.value > 0) sender.sendMessage(Component.text("SubServers > Updating " + success.value + " subserver"+((success.value == 1)?"":"s")));
                                    });
                                    for (SubServer server : select.subservers) {
                                        merge.reserve();
                                        ((SubDataClient) plugin.api.getSubDataNetwork()[0]).sendPacket(new PacketUpdateServer(null, server.getName(), template, version, data -> {
                                            switch (data.getInt(0x0001)) {
                                                case 3:
                                                case 4:
                                                    sender.sendMessage(Component.text("SubServers > Subserver " + server.getName() + " has disappeared"));
                                                    break;
                                                case 5:
                                                    sender.sendMessage(Component.text("SubServers > The host for " + server.getName() + " is not available"));
                                                    break;
                                                case 6:
                                                    sender.sendMessage(Component.text("SubServers > The host for " + server.getName() + " is not enabled"));
                                                    break;
                                                case 7:
                                                    sender.sendMessage(Component.text("SubServers > Subserver " + server.getName() + " is not available"));
                                                    break;
                                                case 8:
                                                    sender.sendMessage(Component.text("SubServers > Cannot update " + server.getName() + " while it is still running"));
                                                    break;
                                                case 9:
                                                    if (ts) sender.sendMessage(Component.text("SubServers > We don't know which template built " + server.getName()));
                                                    else    sender.sendMessage(Component.text("SubServers > There is no template with that name"));
                                                    break;
                                                case 10:
                                                    if (ts) sender.sendMessage(Component.text("SubServers > The template that created " + server.getName() + " is not enabled"));
                                                    else    sender.sendMessage(Component.text("SubServers > That template is not enabled"));
                                                    break;
                                                case 11:
                                                    if (ts) sender.sendMessage(Component.text("SubServers > The template that created " + server.getName() + " does not support subserver updating"));
                                                    else    sender.sendMessage(Component.text("SubServers > That template does not support subserver updating"));
                                                    break;
                                                case 12:
                                                    sender.sendMessage(Component.text("SubServers > The template that created " + server.getName() + " requires a Minecraft version to be specified"));
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
                            sender.sendMessage(Component.text("Usage: " + label + " " + args[0].toLowerCase() + " <Subservers> [[Template] <Version>]"));
                        }
                    } else if (args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("del") || args[0].equalsIgnoreCase("delete")) {
                        if (args.length > 1) {
                            selectServers(sender, args, 1, true, select -> {
                                if (select.subservers.length > 0) {
                                    Container<Integer> success = new Container<Integer>(0);
                                    Merger merge = new Merger(() -> {
                                        if (success.value > 0) sender.sendMessage(Component.text("SubServers > Removing " + success.value + " subserver"+((success.value == 1)?"":"s")));
                                    });
                                    for (SubServer server : select.subservers) {
                                        if (server.isRunning()) {
                                            sender.sendMessage(Component.text("SubServers > Cannot delete " + server.getName() + " while it is still running"));
                                        } else {
                                            server.getHost(host -> {
                                                if (host == null) {
                                                    sender.sendMessage(Component.text("SubServers > Subserver " + server.getName() + " has disappeared"));
                                                } else {
                                                    merge.reserve();
                                                    host.recycleSubServer(server.getName(), response -> {
                                                        switch (response) {
                                                            case 3:
                                                            case 4:
                                                                sender.sendMessage(Component.text("SubServers > Subserver " + server.getName() + " has disappeared"));
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
                            sender.sendMessage(Component.text("Usage: " + label + " " + args[0].toLowerCase() + " <Subservers>"));
                        }
                    } else if (args[0].equalsIgnoreCase("restore")) {
                        // TODO
                    } else {
                        sender.sendMessage(Component.text("SubServers > Unknown sub-command: " + args[0]));
                    }
                } else {
                    for (String s : printHelp()) sender.sendMessage(Component.text(s));
                }
            }
        } else {
            Player player = (Player) sender;
            if (player.getProtocolVersion().getProtocol() < 759) { // player < 1.19
                player.spoofChatInput((args.length == 0)? label : label + ' ' + String.join(" ", args));
            } else {
                player.getCurrentServer().ifPresent(server -> server.sendPluginMessage(pmc, ((args.length == 0)? label : label + ' ' + String.join(" ", args)).getBytes(StandardCharsets.UTF_8)));
            }
        }
    }

    private void selectServers(CommandSource sender, String[] rargs, int index, boolean mode, Consumer<ServerSelection> callback) {
        StackTraceElement[] origin = new Throwable().getStackTrace();
        LinkedList<Component> msgs = new LinkedList<Component>();
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

            LinkedList<Server> servers = new LinkedList<Server>();
            LinkedList<SubServer> subservers = new LinkedList<SubServer>();
            for (Server server : select) {
                if (!servers.contains(server)) {
                    servers.add(server);
                    if (server instanceof SubServer)
                        subservers.add((SubServer) server);
                }
            }

            if ((!mode && servers.size() <= 0) || (mode && subservers.size() <= 0)) {
                Component msg = Component.text("SubServers > No " + ((mode)?"sub":"") + "servers were selected");
                if (sender != null) sender.sendMessage(msg);
                msgs.add(msg);
            }

            try {
                callback.accept(new ServerSelection(msgs, selection, servers, subservers, args, last.value()));
            } catch (Throwable e) {
                Throwable ew = new InvocationTargetException(e);
                ew.setStackTrace(origin);
                ew.printStackTrace();
            }
        };

        // Step 2
        Merger merge = new Merger(finished);
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
                                    Component msg = Component.text("SubServers > There are no " + ((mode)?"sub":"") + "servers on host: " + host.getName());
                                    if (sender != null) sender.sendMessage(msg);
                                    msgs.add(msg);
                                }
                            } else {
                                Component msg = Component.text("SubServers > There is no host with name: " + fcurrent);
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
                                    Component msg = Component.text("SubServers > There are no " + ((mode)?"sub":"") + "servers in group: " + group.key());
                                    if (sender != null) sender.sendMessage(msg);
                                    msgs.add(msg);
                                }
                            } else {
                                Component msg = Component.text("SubServers > There is no group with name: " + fcurrent);
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
                                Component msg = Component.text("SubServers > There is no " + ((mode)?"sub":"") + "server with name: " + fcurrent);
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
        private final Component[] msgs;
        private final String[] selection;
        private final Server[] servers;
        private final SubServer[] subservers;
        private final String[] args;
        private final String last;

        private ServerSelection(List<Component> msgs, List<String> selection, List<Server> servers, List<SubServer> subservers, List<String> args, String last) {
            this.msgs = msgs.toArray(new Component[0]);
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
                "   Command Server: /sub cmd <Servers> <Command> [Args...]",
                "   Create Server: /sub create <Name> <Host> <Template> [Version] [Port]",
                "   Update Server: /sub update <Subservers> [[Template] <Version>]",
                "   Remove Server: /sub delete <Subservers>",
                "",
                "   To see Velocity Supplied Commands, please visit:",
                "   https://velocitypowered.com/wiki/users/built-in-commands/"
        };
    }


    /**
     * Suggest command arguments
     *
     * @param invocation Command invocation
     * @return The validator's response and list of possible arguments
     */
    @SuppressWarnings("unchecked")
    public List<String> suggest(Invocation invocation) {
        CommandSource sender = invocation.source();
        String[] args = invocation.arguments();
        String Last = (args.length > 0)?args[args.length - 1]:"";
        String last = Last.toLowerCase();

        ServerInfo pcs = (sender instanceof Player)? ((Player) sender).getCurrentServer().map(ServerConnection::getServerInfo).orElse(null) : null;
        ServerData pcsd = plugin.getData(pcs);
        if (plugin.api.getSubDataNetwork()[0] == null) {
            if (sender instanceof ConsoleCommandSource)
                new IllegalStateException("SubData is not connected").printStackTrace();
            return Collections.emptyList();
        } else if (sender instanceof Player && (!permitted.containsKey(((Player) sender).getUniqueId()) || !permitted.get(((Player) sender).getUniqueId()).containsKey(pcs)
                || !permitted.get(((Player) sender).getUniqueId()).get(pcs).value())) {
            if (permitted.containsKey(((Player) sender).getUniqueId()) && permitted.get(((Player) sender).getUniqueId()).containsKey(pcs)
                    && permitted.get(((Player) sender).getUniqueId()).get(pcs).key() == null) {
                // do nothing
            } else if (!permitted.containsKey(((Player) sender).getUniqueId()) || !permitted.get(((Player) sender).getUniqueId()).containsKey(pcs)
                    || Calendar.getInstance().getTime().getTime() - permitted.get(((Player) sender).getUniqueId()).get(pcs).key() >= TimeUnit.MINUTES.toMillis(1)) {
                if (pcsd == null || pcsd.getSubData()[0] == null) {
                    HashMap<ServerInfo, Pair<Long, Boolean>> map = (permitted.containsKey(((Player) sender).getUniqueId()))? permitted.get(((Player) sender).getUniqueId()):new HashMap<ServerInfo, Pair<Long, Boolean>>();
                    map.put(pcs, new ContainedPair<>(Calendar.getInstance().getTime().getTime(), false));
                    permitted.put(((Player) sender).getUniqueId(), map);
                } else {
                    HashMap<ServerInfo, Pair<Long, Boolean>> map = (permitted.containsKey(((Player) sender).getUniqueId()))? permitted.get(((Player) sender).getUniqueId()):new HashMap<ServerInfo, Pair<Long, Boolean>>();
                    map.put(pcs, new ContainedPair<>(null, false));
                    permitted.put(((Player) sender).getUniqueId(), map);
                    ((SubDataSender) pcsd.getSubData()[0]).sendPacket(new PacketCheckPermission(((Player) sender).getUniqueId(), "subservers.command", result -> {
                        map.put(pcs, new ContainedPair<>(Calendar.getInstance().getTime().getTime(), result));
                    }));
                }
            }
            return Collections.emptyList();
        } else if (args.length <= 1) {
            List<String> cmds = new ArrayList<>();
            cmds.addAll(Arrays.asList("help", "list", "info", "status", "version", "start", "restart", "stop", "kill", "terminate", "cmd", "command", "create", "update", "upgrade", "restore"));
            if (!(sender instanceof Player)) cmds.addAll(Arrays.asList("remove", "delete"));

            updateCache();

            List<String> list = new ArrayList<String>();
            for (String cmd : cmds) {
                if (cmd.startsWith(last)) list.add(Last + cmd.substring(last.length()));
            }
            return list;
        } else {
            if (args[0].equals("info") || args[0].equals("status")) {
                Supplier<Collection<String>> getPlayers = () -> {
                    LinkedList<String> names = new LinkedList<String>();
                    for (Player player : ExProxy.getInstance().getAllPlayers()) names.add(player.getGameProfile().getName());
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
                    for (ServerData server : plugin.servers.values()) {
                        if (!list.contains(server.getName()) && server.getName().toLowerCase().startsWith(last))
                            list.add(Last + server.getName().substring(last.length()));
                    }
                    for (String player : getPlayers.get()) {
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
                            for (ServerData server : plugin.servers.values()) {
                                if ((!args[1].equalsIgnoreCase("subserver") || server instanceof SubServerData) && server.getName().toLowerCase().startsWith(last))
                                    list.add(Last + server.getName().substring(last.length()));
                            }
                            break;
                        case "u":
                        case "user":
                        case "player":
                            for (String player : getPlayers.get()) {
                                if (player.toLowerCase().startsWith(last))
                                    list.add(Last + player.substring(last.length()));
                            }
                            break;
                    }
                    return list;
                } else {
                    return Collections.emptyList();
                }
            } else if (args[0].equals("start") ||
                    args[0].equals("restart") ||
                    args[0].equals("stop") ||
                    args[0].equals("kill") || args[0].equals("terminate") ||
                    args[0].equals("cmd") || args[0].equals("command") ||
                    args[0].equals("update") || args[0].equals("upgrade") ||
                    args[0].equals("remove") || args[0].equals("del") || args[0].equals("delete")) {
                List<String> list = new ArrayList<String>();
                boolean mode = !args[0].equals("cmd") && !args[0].equals("command");
                RawServerSelection select = selectRawServers(null, args, 1, mode);
                if (select.last != null) {
                    if (last.startsWith("::")) {
                        Map<String, Host> hosts = hostCache;
                        if (hosts.size() > 0) {
                            if (Arrays.binarySearch(select.selection, "::*") < 0 && "::*".startsWith(last)) list.add("::*");
                            if (sender instanceof Player && Arrays.binarySearch(select.selection, "::.") < 0 && "::.".startsWith(last)) list.add("::.");
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
                            if (sender instanceof Player && Arrays.binarySearch(select.selection, ":.") < 0 && ":.".startsWith(last)) list.add(":.");
                            for (String group : groups.keySet()) {
                                group = ":" + group;
                                if (Arrays.binarySearch(select.selection, group.toLowerCase()) < 0 && group.toLowerCase().startsWith(last)) list.add(Last + group.substring(last.length()));
                            }
                        }
                        return list;
                    } else {
                        Map<ServerInfo, ServerData> subservers = plugin.servers;
                        if (subservers.size() > 0) {
                            if (Arrays.binarySearch(select.selection, "*") < 0 && "*".startsWith(last)) list.add("*");
                            if (sender instanceof Player && Arrays.binarySearch(select.selection, ".") < 0 && ".".startsWith(last)) list.add(".");
                            for (ServerData server : subservers.values()) {
                                if ((!mode || server instanceof SubServerData) && Arrays.binarySearch(select.selection, server.getName().toLowerCase()) < 0 && server.getName().toLowerCase().startsWith(last)) list.add(Last + server.getName().substring(last.length()));
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
                    if (!hosts.containsKey(args[2].toLowerCase())) {
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
                        if (!Try.all.run(() -> Integer.parseInt(last)) || Integer.parseInt(last) <= 0 || Integer.parseInt(last) > 65535) {
                            return Collections.emptyList();
                        }
                    }
                    return Collections.singletonList("[Port]");
                } else {
                    return Collections.emptyList();
                }
            } else if (args[0].equals("restore")) {
                if (args.length == 2) {
                    return Collections.singletonList("<Subserver>");
                } else {
                    return Collections.emptyList();
                }
            } else if (sender instanceof Player && (args[0].equals("tp") || args[0].equals("teleport"))) {
                if (args.length == 2 || args.length == 3) {
                    List<String> list = new ArrayList<String>();
                    if (args.length == 2) {
                        list.add("@p");
                        list.add("@a");
                        list.add("@r");
                        list.add("@s");

                        List<UUID> used = new ArrayList<UUID>();
                        Optional<ServerConnection> server = ((Player) sender).getCurrentServer();
                        if (server.isPresent()) {
                            for (Player player : server.get().getServer().getPlayersConnected()) {
                                if (player.getGameProfile().getName().toLowerCase().startsWith(last))
                                    list.add(Last + player.getGameProfile().getName().substring(last.length()));
                                used.add(player.getUniqueId());
                            }

                            for (CachedPlayer player : SubAPI.getInstance().getRemotePlayers(server.get().getServerInfo()).values()) {
                                if (!used.contains(player.getUniqueId())) {
                                    if (player.getName().toLowerCase().startsWith(last))
                                        list.add(Last + player.getName().substring(last.length()));
                                    used.add(player.getUniqueId());
                                }
                            }
                        }
                    }
                    for (ServerData server : plugin.servers.values()) {
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

    private RawServerSelection selectRawServers(CommandSource sender, String[] rargs, int index, boolean mode) {
        LinkedList<Component> msgs = new LinkedList<Component>();
        LinkedList<String> args = new LinkedList<String>();
        LinkedList<String> selection = new LinkedList<>();
        LinkedList<ServerData> servers = new LinkedList<ServerData>();
        String last = null;

        updateCache();

        int i = 0;
        while (i < index) {
            args.add(rargs[i]);
            ++i;
        }

        Map<String, Host> hostMap = null;
        Map<String, List<Server>> groupMap = null;

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
                LinkedList<ServerData> select = new LinkedList<ServerData>();

                if (current.startsWith("::") && current.length() > 2) {
                    current = current.substring(2);
                    if (hostMap == null) hostMap = hostCache;

                    if (current.equals("*")) {
                        for (Host host : hostMap.values()) {
                            for (SubServer server : host.getSubServers().values()) {
                                ExProxy.getInstance().getServer(server.getName()).map(RegisteredServer::getServerInfo).map(plugin::getData).ifPresent(select::add);
                            }
                        }
                    } else {
                        Host host = hostMap.getOrDefault(current.toLowerCase(), null);
                        if (host != null) {
                            for (SubServer server : host.getSubServers().values()) {
                                ExProxy.getInstance().getServer(server.getName()).map(RegisteredServer::getServerInfo).map(plugin::getData).ifPresent(select::add);
                            }
                            if (select.size() <= 0) {
                                Component msg = Component.text("SubServers > There are no " + ((mode)?"sub":"") + "servers on host: " + host.getName());
                                if (sender != null) sender.sendMessage(msg);
                                msgs.add(msg);
                            }
                        } else {
                            Component msg = Component.text("SubServers > There is no host with name: " + current);
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
                                ExProxy.getInstance().getServer(server.getName()).map(RegisteredServer::getServerInfo).map(plugin::getData).ifPresent(select::add);
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
                                    ExProxy.getInstance().getServer(server.getName()).map(RegisteredServer::getServerInfo).map(plugin::getData).ifPresent(select::add);
                                }
                            }
                            if (select.size() <= 0) {
                                Component msg = Component.text("SubServers > There are no " + ((mode)?"sub":"") + "servers in group: " + group.getKey());
                                if (sender != null) sender.sendMessage(msg);
                                msgs.add(msg);
                            }
                        } else {
                            Component msg = Component.text("SubServers > There is no group with name: " + current);
                            if (sender != null) sender.sendMessage(msg);
                            msgs.add(msg);
                        }
                    }
                } else {

                    if (current.equals("*")) {
                        for (ServerData server : plugin.servers.values()) {
                            if (!mode || server instanceof SubServerData) select.add(server);
                        }
                    } else {
                        ServerData server = ExProxy.getInstance().getServer(current).map(RegisteredServer::getServerInfo).map(plugin::getData).orElse(null);
                        if (server != null) {
                            select.add(server);
                        } else {
                            Component msg = Component.text("SubServers > There is no " + ((mode)?"sub":"") + "server with name: " + current);
                            if (sender != null) sender.sendMessage(msg);
                            msgs.add(msg);
                        }
                    }
                }

                for (ServerData server : select) {
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

        LinkedList<SubServerData> subservers = new LinkedList<SubServerData>();
        for (ServerData server : servers) if (server instanceof SubServerData) subservers.add((SubServerData) server);

        if ((!mode && servers.size() <= 0) || (mode && subservers.size() <= 0)) {
            Component msg = Component.text("SubServers > No " + ((mode)?"sub":"") + "servers were selected");
            if (sender != null) sender.sendMessage(msg);
            msgs.add(msg);
        }

        return new RawServerSelection(msgs, selection, servers, subservers, args, last);
    }
    private static final class RawServerSelection {
        private final Component[] msgs;
        private final String[] selection;
        private final ServerData[] servers;
        private final SubServerData[] subservers;
        private final String[] args;
        private final String last;

        private RawServerSelection(List<Component> msgs, List<String> selection, List<ServerData> servers, List<SubServerData> subservers, List<String> args, String last) {
            this.msgs = msgs.toArray(new Component[0]);
            this.selection = selection.toArray(new String[0]);
            this.servers = servers.toArray(new ServerData[0]);
            this.subservers = subservers.toArray(new SubServerData[0]);
            this.args = args.toArray(new String[0]);
            this.last = last;

            Arrays.sort(this.selection);
        }
    }

    private void updateCache() {
        if (Calendar.getInstance().getTime().getTime() - cacheDate >= TimeUnit.MINUTES.toMillis(1)) {
            cacheDate = Calendar.getInstance().getTime().getTime();
            plugin.api.getProxies(proxies -> {
                proxyCache = proxies;
                cacheDate = Calendar.getInstance().getTime().getTime();
            });
            plugin.api.getMasterProxy(master -> {
                proxyMasterCache = master;
                cacheDate = Calendar.getInstance().getTime().getTime();
            });
            plugin.api.getHosts(hosts -> {
                hostCache = hosts;
                cacheDate = Calendar.getInstance().getTime().getTime();
            });
            plugin.api.getGroups(groups -> {
                groupCache = groups;
                cacheDate = Calendar.getInstance().getTime().getTime();
            });
        }
    }

    /**
     * BungeeCord /server
     */
    @SuppressWarnings("unchecked")
    public static final class BungeeServer implements SimpleCommand {
        private ExProxy plugin;
        BungeeServer(ExProxy plugin) {
            this.plugin = plugin;
        }

        /**
         * Override /server
         *
         * @param invocation Command invocation
         */
        @SuppressWarnings("deprecation")
        @Override
        public void execute(Invocation invocation) {
            CommandSource sender = invocation.source();
            String[] args = invocation.arguments();

            if (sender.hasPermission("velocity.command.server")) {
                if (plugin.lang == null) {
                    throw new IllegalStateException("There are no lang options available at this time");
                } else {
                    if (sender instanceof Player) {
                        if (args.length > 0) {
                            Optional<RegisteredServer> server = ExProxy.getInstance().getServer(args[0]);
                            if (server.isPresent()) {
                                ((Player) sender).createConnectionRequest(server.get()).fireAndForget();
                            } else {
                                sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", "Bungee.Server.Invalid")));
                            }
                        } else {
                            int i = 0;
                            TextComponent.Builder serverm = Component.text();
                            TextComponent div = ChatColor.convertColor(plugin.api.getLang("SubServers", "Bungee.Server.Divider"));
                            for (ServerData server : plugin.servers.values()) {
                                if (!server.isHidden() && server.canAccess(sender) && (!(server instanceof SubServerData) || ((SubServerData) server).isRunning())) {
                                    if (i != 0) serverm.append(div);
                                    TextComponent message = ChatColor.convertColor(plugin.api.getLang("SubServers", "Bungee.Server.List").replace("$str$", server.getDisplayName()));
                                    try {
                                        message = message.hoverEvent(HoverEvent.showText(ChatColor.convertColor(plugin.api.getLang("SubServers", "Bungee.Server.Hover").replace("$int$", Integer.toString(SubAPI.getInstance().getRemotePlayers(server.get()).size())))));
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                    message = message.clickEvent(ClickEvent.runCommand("/server " + server.getName()));
                                    serverm.append(message);
                                    i++;
                                }
                            }
                            sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", "Bungee.Server.Current").replace("$str$", ((Player) sender).getCurrentServer().map(ServerConnection::getServerInfo).map(ServerInfo::getName).orElse("???"))));
                            sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", "Bungee.Server.Available")));
                            sender.sendMessage(serverm);
                        }
                    } else {
                        sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Generic.Player-Only")));
                    }
                }
            } else if (sender instanceof Player) {
                String str = '/' + invocation.alias();
                for (String arg : args) str += ' ' + arg;
                ((Player) sender).spoofChatInput(str);
            }
        }

        /**
         * Suggest command arguments
         *
         * @param invocation Command invocation
         * @return The validator's response and list of possible arguments
         */
        public List<String> suggest(Invocation invocation) {
            String[] args = invocation.arguments();

            if (plugin.lang != null && args.length <= 1) {
                String last = (args.length > 0)?args[args.length - 1].toLowerCase():"";
                List<String> list = new ArrayList<String>();

                if (last.length() == 0) {
                    for (ServerData server : plugin.servers.values()) {
                        if (!server.isHidden()) list.add(server.getName());
                    }
                    return list;
                } else {
                    for (ServerData server : plugin.servers.values()) {
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
    public static final class BungeeList implements RawCommand {
        private ExProxy plugin;
        BungeeList(ExProxy plugin) {
            this.plugin = plugin;
        }

        /**
         * Override /glist
         *
         * @param invocation Command invocation
         */
        @SuppressWarnings("deprecation")
        @Override
        public void execute(Invocation invocation) {
            CommandSource sender = invocation.source();

            if (sender.hasPermission("velocity.command.glist")) {
                if (plugin.lang == null) {
                    throw new IllegalStateException("There are no lang options available at this time");
                } else {
                    int players = 0;
                    for (ServerData server : plugin.servers.values()) {
                        List<String> playerlist = new ArrayList<String>();
                        for (CachedPlayer player : SubAPI.getInstance().getRemotePlayers(server.get()).values()) playerlist.add(player.getName());
                        Collections.sort(playerlist);

                        players += playerlist.size();
                        if (!server.isHidden() && (!(server instanceof SubServerData) || ((SubServerData) server).isRunning())) {
                            int i = 0;
                            String message = plugin.api.getLang("SubServers", "Bungee.List.Format").replace("$str$", server.getDisplayName()).replace("$int$", Integer.toString(playerlist.size()));
                            for (String player : playerlist) {
                                if (i != 0) message += plugin.api.getLang("SubServers", "Bungee.List.Divider");
                                message += plugin.api.getLang("SubServers", "Bungee.List.List").replace("$str$", player);
                                i++;
                            }
                            sender.sendMessage(ChatColor.convertColor(message));
                        }
                    }
                    sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", "Bungee.List.Total").replace("$int$", Integer.toString(players))));
                }
            } else if (sender instanceof Player) {
                ((Player) sender).spoofChatInput('/' + invocation.alias() + ' ' + invocation.arguments());
            }
        }
    }
}
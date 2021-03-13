package net.ME1312.SubServers.Client.Bukkit;

import net.ME1312.Galaxi.Library.AsyncConsolidator;
import net.ME1312.Galaxi.Library.Callback.Callback;
import net.ME1312.Galaxi.Library.Config.YAMLSection;
import net.ME1312.Galaxi.Library.Container.ContainedPair;
import net.ME1312.Galaxi.Library.Container.Container;
import net.ME1312.Galaxi.Library.Container.Pair;
import net.ME1312.Galaxi.Library.Container.Value;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Platform;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.SubData.Client.SubDataClient;
import net.ME1312.SubServers.Client.Bukkit.Graphic.UIRenderer;
import net.ME1312.SubServers.Client.Bukkit.Network.Packet.PacketInExRunEvent;
import net.ME1312.SubServers.Client.Common.Network.API.*;
import net.ME1312.SubServers.Client.Common.Network.Packet.PacketCreateServer;
import net.ME1312.SubServers.Client.Common.Network.Packet.PacketRestartServer;
import net.ME1312.SubServers.Client.Common.Network.Packet.PacketUpdateServer;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.*;

import static net.ME1312.SubServers.Client.Bukkit.Library.ObjectPermission.permits;

public final class SubCommand extends BukkitCommand {
    private SubPlugin plugin;

    public SubCommand(SubPlugin plugin, String name) {
        super(
                name,
                "The SubServers Command",
                "/" + name + " is currently unavailable",
                Collections.emptyList()
        );

        this.plugin = plugin;
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        label = "/" + label;
        if (plugin.api.getSubDataNetwork()[0] == null) {
            new IllegalStateException("SubData is not connected").printStackTrace();
            if (!(sender instanceof ConsoleCommandSender)) sender.sendMessage(ChatColor.RED + "An exception has occurred while running this command");
        } else if (plugin.lang == null) {
            new IllegalStateException("There are no lang options available at this time").printStackTrace();
            if (!(sender instanceof ConsoleCommandSender)) sender.sendMessage(ChatColor.RED + "An exception has occurred while running this command");
        } else {
            if (sender.hasPermission("subservers.command")) {
                if (args.length > 0) {
                    if (args[0].equalsIgnoreCase("help") || args[0].equalsIgnoreCase("?")) {
                        sender.sendMessage(printHelp(sender, label));
                    } else if (args[0].equalsIgnoreCase("version") || args[0].equalsIgnoreCase("ver")) {
                        sender.sendMessage(plugin.api.getLang("SubServers", "Command.Version").replace("$str$", "SubServers.Client.Bukkit"));
                        sender.sendMessage(ChatColor.WHITE + "  " + Platform.getSystemName() + ' ' + Platform.getSystemVersion() + ((Platform.getSystemBuild() != null)?" (" + Platform.getSystemBuild() + ')':"") + ((!Platform.getSystemArchitecture().equals("unknown"))?" [" + Platform.getSystemArchitecture() + ']':"") + ChatColor.RESET + ',');
                        sender.sendMessage(ChatColor.WHITE + "  Java " + Platform.getJavaVersion() + ((!Platform.getJavaArchitecture().equals("unknown"))?" [" + Platform.getJavaArchitecture() + ']':"") + ChatColor.RESET + ',');
                        sender.sendMessage(ChatColor.WHITE + "  " + Bukkit.getName() + ' ' + Bukkit.getVersion() + ChatColor.RESET + ',');
                        sender.sendMessage(ChatColor.WHITE + "  SubServers.Client.Bukkit v" + plugin.version.toExtendedString() + ((plugin.api.getPluginBuild() != null)?" (" + plugin.api.getPluginBuild() + ')':""));
                        sender.sendMessage("");
                        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                            try {
                                YAMLSection tags = new YAMLSection(plugin.parseJSON("{\"tags\":" + Util.readAll(new BufferedReader(new InputStreamReader(new URL("https://api.github.com/repos/ME1312/SubServers-2/git/refs/tags").openStream(), Charset.forName("UTF-8")))) + '}'));
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
                                    sender.sendMessage(plugin.api.getLang("SubServers", "Command.Version.Latest"));
                                } else {
                                    sender.sendMessage(plugin.api.getLang("SubServers", "Command.Version.Outdated").replace("$name$", "SubServers.Client.Bukkit").replace("$str$", updversion.toString()).replace("$int$", Integer.toString(updcount)));
                                }
                            } catch (Exception e) {}
                        });
                    } else if (args[0].equalsIgnoreCase("list")) {
                        if (Util.getDespiteException(() -> Class.forName("net.md_5.bungee.api.chat.BaseComponent") != null, false) && sender instanceof Player) {
                            new net.ME1312.SubServers.Client.Bukkit.Library.Compatibility.BungeeChat(plugin).listCommand(sender, label);
                        } else {
                            final String fLabel = label;
                            plugin.api.getGroups(groups -> plugin.api.getHosts(hosts -> plugin.api.getServers(servers -> plugin.api.getProxies(proxies -> {
                                int i = 0;
                                boolean sent = false;
                                String div = plugin.api.getLang("SubServers", "Command.List.Divider");
                                if (groups.keySet().size() > 0) {
                                    sender.sendMessage(plugin.api.getLang("SubServers", "Command.List.Group-Header"));

                                    for (String group : groups.keySet()) {
                                        String message = "  ";
                                        message += ChatColor.GOLD + group + plugin.api.getLang("SubServers", "Command.List.Header");
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
                                        if (i == 0) message += plugin.api.getLang("SubServers", "Command.List.Empty");
                                        sender.sendMessage(message);
                                        i = 0;
                                        sent = true;
                                    }
                                    if (!sent) sender.sendMessage("  " + plugin.api.getLang("SubServers", "Command.List.Empty"));
                                    sent = false;
                                }
                                sender.sendMessage(plugin.api.getLang("SubServers", "Command.List.Host-Header"));
                                for (Host host : hosts.values()) {
                                    String message = "  ";
                                    if (host.isAvailable() && host.isEnabled()) {
                                        message += ChatColor.AQUA;
                                    } else {
                                        message += ChatColor.RED;
                                    }
                                    message += host.getDisplayName();
                                    if (plugin.config.get().getMap("Settings").getBoolean("Show-Addresses", false)) {
                                        message += " [" + ((host.getName().equals(host.getDisplayName()))?"":host.getName()+ChatColor.stripColor(div)) + host.getAddress() + "]";
                                    } else if (!host.getName().equals(host.getDisplayName())) {
                                        message += " [" + host.getName() + "]";
                                    }
                                    message += plugin.api.getLang("SubServers", "Command.List.Header");
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
                                    if (i == 0) message += plugin.api.getLang("SubServers", "Command.List.Empty");
                                    sender.sendMessage(message);
                                    i = 0;
                                    sent = true;
                                }
                                if (!sent) sender.sendMessage("  " + plugin.api.getLang("SubServers", "Command.List.Empty"));
                                sender.sendMessage(plugin.api.getLang("SubServers", "Command.List.Server-Header"));
                                String message = "  ";
                                for (Server server : servers.values()) if (!(server instanceof SubServer)) {
                                    if (i != 0) message += div;
                                    message += ChatColor.WHITE + server.getDisplayName() + " [" + ((server.getName().equals(server.getDisplayName()))?"":server.getName()+ChatColor.stripColor(div)) + ((plugin.config.get().getMap("Settings").getBoolean("Show-Addresses", false))?server.getAddress().getAddress().getHostAddress()+':':"") + server.getAddress().getPort() + "]";
                                    i++;
                                }
                                if (i == 0) sender.sendMessage("  " + plugin.api.getLang("SubServers", "Command.List.Empty"));
                                else sender.sendMessage(message);
                                if (proxies.keySet().size() > 0) {
                                    sender.sendMessage(plugin.api.getLang("SubServers", "Command.List.Proxy-Header"));
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
                            }))));
                        }
                    } else if (args[0].equalsIgnoreCase("info") || args[0].equalsIgnoreCase("status")) {
                        if (args.length > 1) {
                            String type = (args.length > 2)?args[1]:null;
                            String name = args[(type != null)?2:1];

                            Runnable getPlayer = () -> plugin.api.getRemotePlayer(name, player -> {
                                if (player != null) {
                                    sender.sendMessage(plugin.api.getLang("SubServers", "Command.Info").replace("$str$", "player") + ChatColor.WHITE + player.getName());
                                    if (player.getProxyName() != null) sender.sendMessage(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Proxy") + ChatColor.WHITE + player.getProxyName());
                                    if (player.getServerName() != null) sender.sendMessage(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Server") + ChatColor.WHITE + player.getServerName());
                                    if (player.getAddress() != null && plugin.config.get().getMap("Settings").getBoolean("Show-Addresses", false))
                                        sender.sendMessage(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Address") + ChatColor.WHITE + player.getAddress().getAddress().getHostAddress() + ':' + player.getAddress().getPort());
                                    sender.sendMessage(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "UUID") + ChatColor.AQUA + player.getUniqueId());
                                } else {
                                    if (type == null) {
                                        sender.sendMessage(plugin.api.getLang("SubServers", "Command.Info.Unknown"));
                                    } else {
                                        sender.sendMessage(plugin.api.getLang("SubServers", "Command.Info.Unknown-Player"));
                                    }
                                }
                            });
                            Runnable getServer = () -> plugin.api.getServer(name, server -> {
                                if (server != null) {
                                    sender.sendMessage(plugin.api.getLang("SubServers", "Command.Info").replace("$str$", ((server instanceof SubServer)?"sub":"") + "server") + ChatColor.WHITE + server.getDisplayName());
                                    if (!server.getName().equals(server.getDisplayName())) sender.sendMessage(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "System Name") + ChatColor.WHITE + server.getName());
                                    if (server instanceof SubServer) {
                                        sender.sendMessage(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Available") + ((((SubServer) server).isAvailable())?ChatColor.GREEN+"yes":ChatColor.RED+"no"));
                                        sender.sendMessage(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Enabled") + ((((SubServer) server).isEnabled())?ChatColor.GREEN+"yes":ChatColor.RED+"no"));
                                        if (!((SubServer) server).isEditable()) sender.sendMessage(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Editable") + ChatColor.RED + "no");
                                        sender.sendMessage(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Host") + ChatColor.WHITE + ((SubServer) server).getHost());
                                        sender.sendMessage(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Template") + ChatColor.WHITE + ((SubServer) server).getTemplate());
                                    }
                                    if (server.getGroups().size() > 0) sender.sendMessage(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Group" + ((server.getGroups().size() > 1)?"s":"")) + ((server.getGroups().size() > 1)?"":ChatColor.WHITE + server.getGroups().get(0)));
                                    if (server.getGroups().size() > 1) for (String group : server.getGroups()) sender.sendMessage("    " + plugin.api.getLang("SubServers", "Command.Info.List") + ChatColor.WHITE + group);
                                    if (plugin.config.get().getMap("Settings").getBoolean("Show-Addresses", false)) sender.sendMessage(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Address") + ChatColor.WHITE + server.getAddress().getAddress().getHostAddress()+':'+server.getAddress().getPort());
                                    else sender.sendMessage(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Port") + ChatColor.AQUA.toString() + server.getAddress().getPort());
                                    if (server instanceof SubServer) sender.sendMessage(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", ((((SubServer) server).isOnline())?"Online":"Running")) + ((((SubServer) server).isRunning())?ChatColor.GREEN+"yes":ChatColor.RED+"no"));
                                    if (!(server instanceof SubServer) || ((SubServer) server).isRunning()) {
                                        sender.sendMessage(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Connected") + ((server.getSubData()[0] != null)?ChatColor.GREEN+"yes"+((server.getSubData().length > 1)?ChatColor.AQUA+" +"+(server.getSubData().length-1)+" subchannel"+((server.getSubData().length == 2)?"":"s"):""):ChatColor.RED+"no"));
                                        sender.sendMessage(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Players") + ChatColor.AQUA + server.getRemotePlayers().size() + " online");
                                    }
                                    sender.sendMessage(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "MOTD") + ChatColor.WHITE + ChatColor.stripColor(server.getMotd()));
                                    if (server instanceof SubServer && ((SubServer) server).getStopAction() != SubServer.StopAction.NONE) sender.sendMessage(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Stop Action") + ChatColor.WHITE + ((SubServer) server).getStopAction().toString());
                                    sender.sendMessage(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Signature") + ChatColor.AQUA + server.getSignature());
                                    if (server instanceof SubServer) sender.sendMessage(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Logging") + ((((SubServer) server).isLogging())?ChatColor.GREEN+"yes":ChatColor.RED+"no"));
                                    sender.sendMessage(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Restricted") + ((server.isRestricted())?ChatColor.GREEN+"yes":ChatColor.RED+"no"));
                                    if (server instanceof SubServer && ((SubServer) server).getIncompatibilities().size() > 0) {
                                        List<String> current = new ArrayList<String>();
                                        for (String other : ((SubServer) server).getCurrentIncompatibilities()) current.add(other.toLowerCase());
                                        sender.sendMessage(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Incompatibilities"));
                                        for (String other : ((SubServer) server).getIncompatibilities()) sender.sendMessage("    " + plugin.api.getLang("SubServers", "Command.Info.List") + ((current.contains(other.toLowerCase()))?ChatColor.WHITE:ChatColor.GRAY) + other);
                                    }
                                    sender.sendMessage(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Hidden") + ((server.isHidden())?ChatColor.GREEN+"yes":ChatColor.RED+"no"));
                                } else {
                                    if (type == null) {
                                        getPlayer.run();
                                    } else {
                                        sender.sendMessage(plugin.api.getLang("SubServers", "Command.Info.Unknown-Server"));
                                    }
                                }
                            });
                            Runnable getGroup = () -> plugin.api.getGroup(name, group -> {
                                if (group != null) {
                                    sender.sendMessage(plugin.api.getLang("SubServers", "Command.Info").replace("$str$", "group") + ChatColor.WHITE + group.key());
                                    sender.sendMessage(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Servers") + ((group.value().size() <= 0)?ChatColor.GRAY + "(none)":ChatColor.AQUA.toString() + group.value().size()));
                                    for (Server server : group.value()) sender.sendMessage("    " + plugin.api.getLang("SubServers", "Command.Info.List") + ChatColor.WHITE + server.getDisplayName() + ((server.getName().equals(server.getDisplayName()))?"":" ["+server.getName()+']'));
                                } else {
                                    if (type == null) {
                                        getServer.run();
                                    } else {
                                        sender.sendMessage(plugin.api.getLang("SubServers", "Command.Info.Unknown-Group"));
                                    }
                                }
                            });
                            Runnable getHost = () -> plugin.api.getHost(name, host -> {
                                if (host != null) {
                                    sender.sendMessage(plugin.api.getLang("SubServers", "Command.Info").replace("$str$", "host") + ChatColor.WHITE + host.getDisplayName());
                                    if (!host.getName().equals(host.getDisplayName())) sender.sendMessage(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "System Name") + ChatColor.WHITE + host.getName());
                                    sender.sendMessage(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Available") + ((host.isAvailable())?ChatColor.GREEN+"yes":ChatColor.RED+"no"));
                                    sender.sendMessage(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Enabled") + ((host.isEnabled())?ChatColor.GREEN+"yes":ChatColor.RED+"no"));
                                    if (plugin.config.get().getMap("Settings").getBoolean("Show-Addresses", false)) sender.sendMessage(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Address") + ChatColor.WHITE + host.getAddress().getHostAddress());
                                    if (host.getSubData().length > 0) sender.sendMessage(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Connected") + ((host.getSubData()[0] != null)?ChatColor.GREEN+"yes"+((host.getSubData().length > 1)?ChatColor.AQUA+" +"+(host.getSubData().length-1)+" subchannel"+((host.getSubData().length == 2)?"":"s"):""):ChatColor.RED+"no"));
                                    sender.sendMessage(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "SubServers") + ((host.getSubServers().keySet().size() <= 0)?ChatColor.GRAY + "(none)":ChatColor.AQUA.toString() + host.getSubServers().keySet().size()));
                                    for (SubServer subserver : host.getSubServers().values()) sender.sendMessage("    " + plugin.api.getLang("SubServers", "Command.Info.List") + ((subserver.isEnabled())?ChatColor.WHITE:ChatColor.GRAY) + subserver.getDisplayName() + ((subserver.getName().equals(subserver.getDisplayName()))?"":" ["+subserver.getName()+']'));
                                    sender.sendMessage(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Templates") + ((host.getCreator().getTemplates().keySet().size() <= 0)?ChatColor.GRAY + "(none)":ChatColor.AQUA.toString() + host.getCreator().getTemplates().keySet().size()));
                                    for (SubCreator.ServerTemplate template : host.getCreator().getTemplates().values()) sender.sendMessage("    " + plugin.api.getLang("SubServers", "Command.Info.List") + ((template.isEnabled())?ChatColor.WHITE:ChatColor.GRAY) + template.getDisplayName() + ((template.getName().equals(template.getDisplayName()))?"":" ["+template.getName()+']'));
                                    sender.sendMessage(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Signature") + ChatColor.AQUA + host.getSignature());
                                } else {
                                    if (type == null) {
                                        getGroup.run();
                                    } else {
                                        sender.sendMessage(plugin.api.getLang("SubServers", "Command.Info.Unknown-Host"));
                                    }
                                }
                            });
                            Runnable getProxy = () -> plugin.api.getProxy(name, proxy -> {
                                if (proxy != null) {
                                    sender.sendMessage(plugin.api.getLang("SubServers", "Command.Info").replace("$str$", "proxy") + ChatColor.WHITE + proxy.getDisplayName());
                                    if (!proxy.getName().equals(proxy.getDisplayName())) sender.sendMessage(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "System Name") + ChatColor.WHITE + proxy.getName());
                                    if (!proxy.isMaster()) sender.sendMessage(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Connected") + ((proxy.getSubData()[0] != null)?ChatColor.GREEN+"yes"+((proxy.getSubData().length > 1)?ChatColor.AQUA+" +"+(proxy.getSubData().length-1)+" subchannel"+((proxy.getSubData().length == 2)?"":"s"):""):ChatColor.RED+"no"));
                                    else if (!proxy.getDisplayName().toLowerCase().contains("master")) sender.sendMessage(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Type") + ChatColor.WHITE + "Master");
                                    sender.sendMessage(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Players") + ChatColor.AQUA + proxy.getPlayers().size() + " online");
                                    sender.sendMessage(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Signature") + ChatColor.AQUA + proxy.getSignature());
                                } else {
                                    if (type == null) {
                                        getHost.run();
                                    } else {
                                        sender.sendMessage(plugin.api.getLang("SubServers", "Command.Info.Unknown-Proxy"));
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
                                        sender.sendMessage(plugin.api.getLang("SubServers", "Command.Info.Unknown-Type"));
                                }
                            }
                        } else {
                            sender.sendMessage(plugin.api.getLang("SubServers", "Command.Generic.Usage").replace("$str$", label.toLowerCase() + " " + args[0].toLowerCase() + " [proxy|host|group|server|player] <Name>"));
                        }
                    } else if (args[0].equalsIgnoreCase("start")) {
                        if (args.length > 1) {
                            selectServers(sender, args, 1, true, Arrays.asList("subservers.subserver.%.*", "subservers.subserver.%.start"), select -> {
                                if (select.subservers.length > 0) {
                                    Container<Integer> success = new Container<Integer>(0);
                                    Container<Integer> running = new Container<Integer>(0);
                                    AsyncConsolidator merge = new AsyncConsolidator(() -> {
                                        if (running.value > 0) sender.sendMessage(plugin.api.getLang("SubServers", "Command.Start.Running").replace("$int$", running.value.toString()));
                                        if (success.value > 0) sender.sendMessage(plugin.api.getLang("SubServers", "Command.Start").replace("$int$", success.value.toString()));
                                    });
                                    for (SubServer server : select.subservers) {
                                        merge.reserve();
                                        server.start((sender instanceof Player)?((Player) sender).getUniqueId():null, response -> {
                                            switch (response) {
                                                case 3:
                                                case 4:
                                                    sender.sendMessage(plugin.api.getLang("SubServers", "Command.Start.Disappeared").replace("$str$", server.getName()));
                                                    break;
                                                case 5:
                                                    sender.sendMessage(plugin.api.getLang("SubServers", "Command.Start.Host-Unavailable").replace("$str$", server.getName()));
                                                    break;
                                                case 6:
                                                    sender.sendMessage(plugin.api.getLang("SubServers", "Command.Start.Host-Disabled").replace("$str$", server.getName()));
                                                    break;
                                                case 7:
                                                    sender.sendMessage(plugin.api.getLang("SubServers", "Command.Start.Server-Unavailable").replace("$str$", server.getName()));
                                                    break;
                                                case 8:
                                                    sender.sendMessage(plugin.api.getLang("SubServers", "Command.Start.Server-Disabled").replace("$str$", server.getName()));
                                                    break;
                                                case 9:
                                                    running.value++;
                                                    break;
                                                case 10:
                                                    sender.sendMessage(plugin.api.getLang("SubServers", "Command.Start.Server-Incompatible").replace("$str$", server.getName()));
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
                            sender.sendMessage(plugin.api.getLang("SubServers", "Command.Generic.Usage").replace("$str$", label.toLowerCase() + " " + args[0].toLowerCase() + " <Subservers>"));
                        }
                    } else if (args[0].equalsIgnoreCase("restart")) {
                        if (args.length > 1) {
                            selectServers(sender, args, 1, true, Arrays.asList(Arrays.asList("subservers.subserver.%.*", "subservers.subserver.%.start"), Arrays.asList("subservers.subserver.%.*", "subservers.subserver.%.stop")), select -> {
                                if (select.subservers.length > 0) {
                                    // Step 5: Start the stopped Servers once more
                                    final UUID player = (sender instanceof Player)?((Player) sender).getUniqueId():null;
                                    Callback<SubServer> starter = server -> server.start(player, response -> {
                                        switch (response) {
                                            case 3:
                                            case 4:
                                                sender.sendMessage(plugin.api.getLang("SubServers", "Command.Restart.Disappeared").replace("$str$", server.getName()));
                                                break;
                                            case 5:
                                                sender.sendMessage(plugin.api.getLang("SubServers", "Command.Restart.Host-Unavailable").replace("$str$", server.getName()));
                                                break;
                                            case 6:
                                                sender.sendMessage(plugin.api.getLang("SubServers", "Command.Restart.Host-Disabled").replace("$str$", server.getName()));
                                                break;
                                            case 7:
                                                sender.sendMessage(plugin.api.getLang("SubServers", "Command.Restart.Server-Unavailable").replace("$str$", server.getName()));
                                                break;
                                            case 8:
                                                sender.sendMessage(plugin.api.getLang("SubServers", "Command.Restart.Server-Disabled").replace("$str$", server.getName()));
                                                break;
                                            case 10:
                                                sender.sendMessage(plugin.api.getLang("SubServers", "Command.Restart.Server-Incompatible").replace("$str$", server.getName()));
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
                                                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                                                            starter.run(listening.get(name));
                                                            listening.remove(name);
                                                        }, 5);
                                                    }
                                                }
                                            } catch (Exception e) {}
                                        }
                                    });

                                    // Step 3: Receive command Responses
                                    Container<Integer> success = new Container<Integer>(0);
                                    AsyncConsolidator merge = new AsyncConsolidator(() -> {
                                        if (success.value > 0) sender.sendMessage(plugin.api.getLang("SubServers", "Command.Restart").replace("$int$", success.value.toString()));
                                    });
                                    Callback<Pair<Integer, SubServer>> stopper = data -> {
                                        if (data.key() != 0) listening.remove(data.value().getName().toLowerCase());
                                        switch (data.key()) {
                                            case 3:
                                            case 4:
                                                sender.sendMessage(plugin.api.getLang("SubServers", "Command.Restart.Disappeared").replace("$str$", data.value().getName()));
                                                break;
                                            case 5:
                                                starter.run(data.value());
                                            case 0:
                                                success.value++;
                                                break;
                                        }
                                        merge.release();
                                    };

                                    // Step 1: Detect Self
                                    SubServer self = null;
                                    for (SubServer server : select.subservers) {
                                        if (server.getName().equalsIgnoreCase(plugin.api.getName())) {
                                            self = server;
                                            break;
                                        }
                                    }

                                    // Step 2: Restart Servers
                                    for (SubServer server : select.subservers) {
                                        merge.reserve();
                                        if (self == null) {
                                            listening.put(server.getName().toLowerCase(), server);
                                            server.stop(player, response -> stopper.run(new ContainedPair<>(response, server)));
                                        } else if (self != server) {
                                            ((SubDataClient) plugin.api.getSubDataNetwork()[0]).sendPacket(new PacketRestartServer(player, server.getName(), data -> stopper.run(new ContainedPair<>(data.getInt(0x0001), server))));
                                        }
                                    }
                                    if (self != null) {
                                        final SubServer fself = self;
                                        ((SubDataClient) plugin.api.getSubDataNetwork()[0]).sendPacket(new PacketRestartServer(player, self.getName(), data -> stopper.run(new ContainedPair<>(data.getInt(0x0001), fself))));
                                    }
                                }
                            });
                        } else {
                            sender.sendMessage(plugin.api.getLang("SubServers", "Command.Generic.Usage").replace("$str$", label.toLowerCase() + " " + args[0].toLowerCase() + " <Subservers>"));
                        }
                    } else if (args[0].equalsIgnoreCase("stop")) {
                        if (args.length > 1) {
                            selectServers(sender, args, 1, true, Arrays.asList("subservers.subserver.%.*", "subservers.subserver.%.stop"), select -> {
                                if (select.subservers.length > 0) {
                                    Container<Integer> success = new Container<Integer>(0);
                                    Container<Integer> running = new Container<Integer>(0);
                                    AsyncConsolidator merge = new AsyncConsolidator(() -> {
                                        if (running.value > 0) sender.sendMessage(plugin.api.getLang("SubServers", "Command.Stop.Not-Running").replace("$int$", running.value.toString()));
                                        if (success.value > 0) sender.sendMessage(plugin.api.getLang("SubServers", "Command.Stop").replace("$int$", success.value.toString()));
                                    });
                                    Callback<Pair<Integer, SubServer>> stopper = data -> {
                                        switch (data.key()) {
                                            case 3:
                                            case 4:
                                                sender.sendMessage(plugin.api.getLang("SubServers", "Command.Stop.Disappeared").replace("$str$", data.value().getName()));
                                                break;
                                            case 5:
                                                running.value++;
                                                break;
                                            case 0:
                                                success.value++;
                                                break;
                                        }
                                        merge.release();
                                    };

                                    SubServer self = null;
                                    for (SubServer server : select.subservers) {
                                        if (server.getName().equalsIgnoreCase(plugin.api.getName())) {
                                            self = server;
                                            break;
                                        }
                                    }

                                    for (SubServer server : select.subservers) {
                                        merge.reserve();
                                        if (self != server) server.stop((sender instanceof Player)?((Player) sender).getUniqueId():null, response -> stopper.run(new ContainedPair<>(response, server)));
                                    }
                                    if (self != null) {
                                        final SubServer fself = self;
                                        fself.stop((sender instanceof Player) ? ((Player) sender).getUniqueId() : null, response -> stopper.run(new ContainedPair<>(response, fself)));
                                    }
                                }
                            });
                        } else {
                            sender.sendMessage(plugin.api.getLang("SubServers", "Command.Generic.Usage").replace("$str$", label.toLowerCase() + " " + args[0].toLowerCase() + " <Subservers>"));
                        }
                    } else if (args[0].equalsIgnoreCase("kill") || args[0].equalsIgnoreCase("terminate")) {
                        if (args.length > 1) {
                            selectServers(sender, args, 1, true, Arrays.asList("subservers.subserver.%.*", "subservers.subserver.%.terminate"), select -> {
                                if (select.subservers.length > 0) {
                                    Container<Integer> success = new Container<Integer>(0);
                                    Container<Integer> running = new Container<Integer>(0);
                                    AsyncConsolidator merge = new AsyncConsolidator(() -> {
                                        if (running.value > 0) sender.sendMessage(plugin.api.getLang("SubServers", "Command.Terminate.Not-Running").replace("$int$", running.value.toString()));
                                        if (success.value > 0) sender.sendMessage(plugin.api.getLang("SubServers", "Command.Terminate").replace("$int$", success.value.toString()));
                                    });
                                    Callback<Pair<Integer, SubServer>> stopper = data -> {
                                        switch (data.key()) {
                                            case 3:
                                            case 4:
                                                sender.sendMessage(plugin.api.getLang("SubServers", "Command.Terminate.Disappeared").replace("$str$", data.value().getName()));
                                                break;
                                            case 5:
                                                running.value++;
                                                break;
                                            case 0:
                                                success.value++;
                                                break;
                                        }
                                        merge.release();
                                    };

                                    SubServer self = null;
                                    for (SubServer server : select.subservers) {
                                        if (server.getName().equalsIgnoreCase(plugin.api.getName())) {
                                            self = server;
                                            break;
                                        }
                                    }

                                    for (SubServer server : select.subservers) {
                                        merge.reserve();
                                        if (self != server) server.terminate((sender instanceof Player)?((Player) sender).getUniqueId():null, response -> stopper.run(new ContainedPair<>(response, server)));
                                    }
                                    if (self != null) {
                                        final SubServer fself = self;
                                        fself.terminate((sender instanceof Player) ? ((Player) sender).getUniqueId() : null, response -> stopper.run(new ContainedPair<>(response, fself)));
                                    }
                                }
                            });
                        } else {
                            sender.sendMessage(plugin.api.getLang("SubServers", "Command.Generic.Usage").replace("$str$", label.toLowerCase() + " " + args[0].toLowerCase() + " <Subservers>"));
                        }
                    } else if (args[0].equalsIgnoreCase("cmd") || args[0].equalsIgnoreCase("command")) {
                        if (args.length > 1) {
                            selectServers(sender, args, 1, true, Arrays.asList("subservers.subserver.%.*", "subservers.subserver.%.command"), select -> {
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
                                            if (running.value > 0) sender.sendMessage(plugin.api.getLang("SubServers", "Command.Command.Not-Running").replace("$int$", running.value.toString()));
                                            if (success.value > 0) sender.sendMessage(plugin.api.getLang("SubServers", "Command.Command").replace("$int$", success.value.toString()));
                                        });
                                        for (SubServer server : select.subservers) {
                                            merge.reserve();
                                            server.command((sender instanceof Player)?((Player) sender).getUniqueId():null, builder.toString(), response -> {
                                                switch (response) {
                                                    case 3:
                                                    case 4:
                                                        sender.sendMessage(plugin.api.getLang("SubServers", "Command.Command.Disappeared").replace("$str$", server.getName()));
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
                                        sender.sendMessage(plugin.api.getLang("SubServers", "Command.Command.No-Command"));
                                    }
                                }
                            });
                        } else {
                            sender.sendMessage(plugin.api.getLang("SubServers", "Command.Generic.Usage").replace("$str$", label.toLowerCase() + " " + args[0].toLowerCase() + " <Subservers> <Command> [Args...]"));
                        }
                    } else if (args[0].equalsIgnoreCase("create")) {
                        if (args.length > 3) {
                            if (sender.hasPermission("subservers.host.*.*") || sender.hasPermission("subservers.host.*.create") || sender.hasPermission("subservers.host." + args[2].toLowerCase() + ".*") || sender.hasPermission("subservers.host." + args[2].toLowerCase() + ".create")) {
                                if (args.length > 5 && Util.isException(() -> Integer.parseInt(args[5]))) {
                                    sender.sendMessage(plugin.api.getLang("SubServers", "Command.Creator.Invalid-Port"));
                                } else {
                                    ((SubDataClient) plugin.api.getSubDataNetwork()[0]).sendPacket(new PacketCreateServer((sender instanceof Player)?((Player) sender).getUniqueId():null, args[1], args[2], args[3], (args.length > 4)?new Version(args[4]):null, (args.length > 5)?Integer.parseInt(args[5]):null, data -> {
                                        switch (data.getInt(0x0001)) {
                                            case 3:
                                            case 4:
                                                sender.sendMessage(plugin.api.getLang("SubServers", "Command.Creator.Exists"));
                                                break;
                                            case 5:
                                                sender.sendMessage(plugin.api.getLang("SubServers", "Command.Creator.Unknown-Host"));
                                                break;
                                            case 6:
                                                sender.sendMessage(plugin.api.getLang("SubServers", "Command.Creator.Host-Unavailable"));
                                                break;
                                            case 7:
                                                sender.sendMessage(plugin.api.getLang("SubServers", "Command.Creator.Host-Disabled"));
                                                break;
                                            case 8:
                                                sender.sendMessage(plugin.api.getLang("SubServers", "Command.Creator.Unknown-Template"));
                                                break;
                                            case 9:
                                                sender.sendMessage(plugin.api.getLang("SubServers", "Command.Creator.Template-Disabled"));
                                                break;
                                            case 10:
                                                sender.sendMessage(plugin.api.getLang("SubServers", "Command.Creator.Version-Required"));
                                                break;
                                            case 11:
                                                sender.sendMessage(plugin.api.getLang("SubServers", "Command.Creator.Invalid-Port"));
                                                break;
                                            case 0:
                                                sender.sendMessage(plugin.api.getLang("SubServers", "Command.Creator").replace("$str$", args[1]));
                                                break;
                                        }
                                    }));
                                }
                            } else {
                                sender.sendMessage(plugin.api.getLang("SubServers", "Command.Generic.Invalid-Permission").replace("$str$", "subservers.host." + args[2].toLowerCase() + ".create"));
                            }
                        } else {
                            sender.sendMessage(plugin.api.getLang("SubServers", "Command.Generic.Usage").replace("$str$", label.toLowerCase() + " " + args[0].toLowerCase() + " <Name> <Host> <Template> [Version] [Port]"));
                        }
                    } else if (args[0].equalsIgnoreCase("update") || args[0].equalsIgnoreCase("upgrade")) {
                        if (args.length > 1) {
                            selectServers(sender, args, 1, true, Arrays.asList("subservers.subserver.%.*", "subservers.subserver.%.update"), select -> {
                                if (select.subservers.length > 0) {
                                    String template = (select.args.length > 3)?select.args[2].toLowerCase():null;
                                    Version version = (select.args.length > 2)?new Version(select.args[(template == null)?2:3]):null;
                                    boolean ts = template == null;

                                    Container<Integer> success = new Container<Integer>(0);
                                    AsyncConsolidator merge = new AsyncConsolidator(() -> {
                                        if (success.value > 0) sender.sendMessage(plugin.api.getLang("SubServers", "Command.Update").replace("$int$", success.value.toString()));
                                    });
                                    for (SubServer server : select.subservers) {
                                        merge.reserve();
                                        ((SubDataClient) plugin.api.getSubDataNetwork()[0]).sendPacket(new PacketUpdateServer((sender instanceof Player)?((Player) sender).getUniqueId():null, server.getName(), template, version, data -> {
                                            switch (data.getInt(0x0001)) {
                                                case 3:
                                                case 4:
                                                    sender.sendMessage(plugin.api.getLang("SubServers", "Command.Update.Disappeared").replace("$str$", server.getName()));
                                                    break;
                                                case 5:
                                                    sender.sendMessage(plugin.api.getLang("SubServers", "Command.Update.Host-Unavailable").replace("$str$", server.getName()));
                                                    break;
                                                case 6:
                                                    sender.sendMessage(plugin.api.getLang("SubServers", "Command.Update.Host-Disabled").replace("$str$", server.getName()));
                                                    break;
                                                case 7:
                                                    sender.sendMessage(plugin.api.getLang("SubServers", "Command.Update.Server-Unavailable").replace("$str$", server.getName()));
                                                    break;
                                                case 8:
                                                    sender.sendMessage(plugin.api.getLang("SubServers", "Command.Update.Running").replace("$str$", server.getName()));
                                                    break;
                                                case 9:
                                                    if (ts) sender.sendMessage(plugin.api.getLang("SubServers", "Command.Update.Unknown-Template").replace("$str$", server.getName()));
                                                    else    sender.sendMessage(plugin.api.getLang("SubServers", "Command.Creator.Unknown-Template"));
                                                    break;
                                                case 10:
                                                    if (ts) sender.sendMessage(plugin.api.getLang("SubServers", "Command.Update.Template-Disabled").replace("$str$", server.getName()));
                                                    else    sender.sendMessage(plugin.api.getLang("SubServers", "Command.Creator.Template-Disabled"));
                                                    break;
                                                case 11:
                                                    if (ts) sender.sendMessage(plugin.api.getLang("SubServers", "Command.Update.Template-Invalid").replace("$str$", server.getName()));
                                                    else    sender.sendMessage(plugin.api.getLang("SubServers", "Command.Creator.Template-Invalid"));
                                                    break;
                                                case 12:
                                                    sender.sendMessage(plugin.api.getLang("SubServers", "Command.Update.Version-Required").replace("$str$", server.getName()));
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
                            sender.sendMessage(plugin.api.getLang("SubServers", "Command.Generic.Usage").replace("$str$", label.toLowerCase() + " " + args[0].toLowerCase() + " <Subservers> [[Template] <Version>]"));
                        }
                    } else if (args[0].equalsIgnoreCase("tp") || args[0].equalsIgnoreCase("teleport")) {
                        executeTeleport(sender, label, args);
                    } else if ((args[0].equalsIgnoreCase("view") || args[0].equalsIgnoreCase("open")) && sender instanceof Player) {
                        if (plugin.gui != null) {
                            if (sender.hasPermission("subservers.interface")) {
                                try {
                                    plugin.gui.getRenderer((Player) sender).clearHistory();
                                    switch (args[1].toLowerCase()) {
                                        case "host":
                                            if (args.length > 2) plugin.gui.getRenderer((Player) sender).hostMenu(Integer.parseInt(args[2]));
                                            else plugin.gui.getRenderer((Player) sender).hostMenu(1);
                                            break;
                                        case "host/":
                                            plugin.gui.getRenderer((Player) sender).hostAdmin(args[2]);
                                            break;
                                        case "host/creator":
                                            if (sender.hasPermission("subservers.host.*.*") || sender.hasPermission("subservers.host.*.create") || sender.hasPermission("subservers.host." + args[2].toLowerCase() + ".*") || sender.hasPermission("subservers.host." + args[2].toLowerCase() + ".create"))
                                                plugin.gui.getRenderer((Player) sender).hostCreator(new UIRenderer.CreatorOptions(args[2]));
                                            else throw new IllegalStateException("Player does not meet the requirements to render this page");
                                            break;
                                        case "host/plugin":
                                            if (args.length > 3) plugin.gui.getRenderer((Player) sender).hostPlugin(Integer.parseInt(args[3]), args[2]);
                                            else plugin.gui.getRenderer((Player) sender).hostPlugin(1, args[2]);
                                            break;
                                        case "group":
                                            if (args.length > 2) plugin.gui.getRenderer((Player) sender).groupMenu(Integer.parseInt(args[2]));
                                            else plugin.gui.getRenderer((Player) sender).groupMenu(1);
                                            break;
                                        case "server":
                                        case "subserver":
                                            if (args.length > 4) plugin.gui.getRenderer((Player) sender).serverMenu(Integer.parseInt(args[2]), args[4], null);
                                            else if (args.length > 3) plugin.gui.getRenderer((Player) sender).serverMenu(Integer.parseInt(args[2]), null, args[3]);
                                            else if (args.length > 2) plugin.gui.getRenderer((Player) sender).serverMenu(Integer.parseInt(args[2]), null, null);
                                            else plugin.gui.getRenderer((Player) sender).serverMenu(1, null, null);
                                            break;
                                        case "subserver/":
                                            plugin.gui.getRenderer((Player) sender).subserverAdmin(args[2]);
                                            break;
                                        case "subserver/plugin":
                                            if (args.length > 3) plugin.gui.getRenderer((Player) sender).subserverPlugin(Integer.parseInt(args[3]), args[2]);
                                            else plugin.gui.getRenderer((Player) sender).subserverPlugin(1, args[2]);
                                            break;
                                    }
                                } catch (Throwable e) { /*
                                    List<String> list = new LinkedList<String>();
                                    list.addAll(Arrays.asList(args));
                                    list.remove(0);
                                    new InvocationTargetException(e, "Could not render page with arguments: " + list.toString()).printStackTrace(); */
                                }
                            } else {
                                sender.sendMessage(plugin.api.getLang("SubServers", "Command.Generic.Invalid-Permission").replace("$str$", "subservers.interface"));
                            }
                        }
                    } else {
                        sender.sendMessage(plugin.api.getLang("SubServers", "Command.Generic.Invalid-Subcommand").replace("$str$", args[0]));
                    }
                } else {
                    if (sender.hasPermission("subservers.interface") && sender instanceof Player && plugin.gui != null) {
                        plugin.gui.getRenderer((Player) sender).newUI();
                    } else {
                        sender.sendMessage(printHelp(sender, label));
                    }
                }
            } else if (args.length > 0 && (args[0].equalsIgnoreCase("tp") || args[0].equalsIgnoreCase("teleport"))) {
                executeTeleport(sender, label, args);
            } else if (sender.hasPermission("subservers.interface") && sender instanceof Player) {
                plugin.gui.getRenderer((Player) sender).newUI();
            } else {
                sender.sendMessage(plugin.api.getLang("SubServers", "Command.Generic.Invalid-Permission").replace("$str$", "subservers.command"));
            }
        }
        return true;
    }
    private void executeTeleport(CommandSender sender, String label, String[] args) {
        if (args.length > ((sender instanceof Player)?1:2)) {
            String select = args[(args.length > 2)?2:1];
            plugin.api.getServer(select, server -> {
                if (server != null) {
                    if (permits(server, sender, "subservers.server.%.*", "subservers.server.%.teleport")) {
                        if (!(server instanceof SubServer) || ((SubServer) server).isRunning()) {
                            Player target = (args.length > 2)?Bukkit.getPlayer(args[1]):null;
                            if (target != null || args.length == 2) {
                                if (target == null || target == sender || permits(server, sender, "subservers.server.%.*", "subservers.server.%.teleport-others")) {
                                    if (target == null) target = (Player) sender;

                                    sender.sendMessage(plugin.api.getLang("SubServers", "Command.Teleport").replace("$str$", target.getName()));
                                    plugin.pmc(target, "Connect", server.getName());
                                } else {
                                    sender.sendMessage(plugin.api.getLang("SubServers", "Command.Generic.Invalid-Permission").replace("$str$", "subservers.server." + server.getName() + ".teleport-others"));
                                }
                            } else {
                                sender.sendMessage(plugin.api.getLang("SubServers", "Command.Generic.Unknown-Player").replace("$str$", args[1]));
                            }
                        } else {
                            sender.sendMessage(plugin.api.getLang("SubServers", "Command.Teleport.Not-Running").replace("$str$", server.getName()));
                        }
                    } else {
                        sender.sendMessage(plugin.api.getLang("SubServers", "Command.Generic.Invalid-Select-Permission").replace("$str$", server.getName()));
                    }
                } else {
                    sender.sendMessage(plugin.api.getLang("SubServers", "Command.Generic.Unknown-Server").replace("$str$", select));
                }
            });
        } else {
            sender.sendMessage(plugin.api.getLang("SubServers", "Command.Generic.Usage").replace("$str$", label.toLowerCase() + " " + args[0].toLowerCase() + " " + ((sender instanceof Player)?"[Player]":"<Player>") + " <Server>"));
        }
    }
    private void selectServers(CommandSender sender, String[] rargs, int index, boolean mode, String permissions, Callback<ServerSelection> callback) {
        selectServers(sender, rargs, index, mode, Arrays.asList(permissions), callback);
    }
    private void selectServers(CommandSender sender, String[] rargs, int index, boolean mode, List<String> permissions, Callback<ServerSelection> callback) {
        selectServers(sender, rargs, index, mode, Arrays.asList(permissions), callback);
    }
    @SuppressWarnings("unchecked")
    private void selectServers(CommandSender sender, String[] rargs, int index, boolean mode, Collection<List<String>> permissions, Callback<ServerSelection> callback) {
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

                    boolean permitted = sender == null || permissions == null || permissions.size() <= 0;
                    if (!permitted) {
                        permitted = true;
                        List<String>[] checks = permissions.toArray(new List[0]);
                        for (int p = 0; permitted && p < permissions.size(); p++) {
                            if (checks[p] == null || checks[p].size() <= 0) continue;
                            else permitted = permits(server, sender, checks[p].toArray(new String[0]));
                        }
                    }


                    if (permitted) {
                        servers.add(server);
                        if (server instanceof SubServer)
                            subservers.add((SubServer) server);
                    } else {
                        String msg = plugin.api.getLang("SubServers", "Command.Generic.Invalid-Select-Permission").replace("$str$", server.getName());
                        sender.sendMessage(msg);
                        msgs.add(msg);
                    }
                }
            }

            if ((!mode && servers.size() <= 0) || (mode && subservers.size() <= 0)) {
                String msg = plugin.api.getLang("SubServers", "Command.Generic.No-" + ((mode)?"Sub":"") + "Servers-Selected");
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

                    if (current.equals(".")) {
                        plugin.api.getSubServer(plugin.api.getName(), self -> {
                            if (self != null) {
                                merge.reserve();
                                self.getHost(host -> {
                                    select.addAll(host.getSubServers().values());
                                    merge.release();
                                });
                            } else {
                                String msg = plugin.api.getLang("SubServers", "Command.Generic.Unknown-SubServer").replace("$str$", plugin.api.getName());
                                if (sender != null) sender.sendMessage(msg);
                                msgs.add(msg);
                            }
                            merge.release();
                        });
                    } else if (current.equals("*")) {
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
                                    String msg = plugin.api.getLang("SubServers", "Command.Generic.No-" + ((mode)?"Sub":"") + "Servers-On-Host").replace("$str$", host.getName());
                                    if (sender != null) sender.sendMessage(msg);
                                    msgs.add(msg);
                                }
                            } else {
                                String msg = plugin.api.getLang("SubServers", "Command.Generic.Unknown-Host").replace("$str$", fcurrent);
                                if (sender != null) sender.sendMessage(msg);
                                msgs.add(msg);
                            }
                            merge.release();
                        });
                    }
                } else if (current.startsWith(":") && current.length() > 1) {
                    current = current.substring(1);

                    if (current.equals(".")) {
                        plugin.api.getSubServer(plugin.api.getName(), self -> {
                            AsyncConsolidator merge2 = new AsyncConsolidator(merge::release);
                            for (String name : self.getGroups()) {
                                merge2.reserve();
                                plugin.api.getGroup(name, group -> {
                                    for (Server server : group.value()) {
                                        if (!mode || server instanceof SubServer) select.add(server);
                                    }
                                    merge2.release();
                                });
                            }
                        });
                    } else if (current.equals("*")) {
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
                                    String msg = plugin.api.getLang("SubServers", "Command.Generic.No-" + ((mode)?"Sub":"") + "Servers-In-Group").replace("$str$", group.key());
                                    if (sender != null) sender.sendMessage(msg);
                                    msgs.add(msg);
                                }
                            } else {
                                String msg = plugin.api.getLang("SubServers", "Command.Generic.Unknown-Group").replace("$str$", fcurrent);
                                if (sender != null) sender.sendMessage(msg);
                                msgs.add(msg);
                            }
                            merge.release();
                        });
                    }
                } else {

                    if (current.equals(".")) {
                        plugin.api.getServer(plugin.api.getName(), self -> {
                            if (!mode || self instanceof SubServer) select.add(self);
                            merge.release();
                        });
                    } else if (current.equals("*")) {
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
                                String msg = plugin.api.getLang("SubServers", "Command.Generic.Unknown-" + ((mode)?"Sub":"") + "Server").replace("$str$", fcurrent);
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

    private String[] printHelp(CommandSender sender, String label) {
        return new String[]{
                plugin.api.getLang("SubServers", "Command.Help.Header"),
                plugin.api.getLang("SubServers", "Command.Help.Help").replace("$str$", label.toLowerCase() + " help"),
                plugin.api.getLang("SubServers", "Command.Help.List").replace("$str$", label.toLowerCase() + " list"),
                plugin.api.getLang("SubServers", "Command.Help.Version").replace("$str$", label.toLowerCase() + " version"),
                plugin.api.getLang("SubServers", "Command.Help.Info").replace("$str$", label.toLowerCase() + " info [proxy|host|group|server|player] <Name>"),
                plugin.api.getLang("SubServers", "Command.Help.SubServer.Start").replace("$str$", label.toLowerCase() + " start <Subservers>"),
                plugin.api.getLang("SubServers", "Command.Help.SubServer.Restart").replace("$str$", label.toLowerCase() + " restart <Subservers>"),
                plugin.api.getLang("SubServers", "Command.Help.SubServer.Stop").replace("$str$", label.toLowerCase() + " stop <Subservers>"),
                plugin.api.getLang("SubServers", "Command.Help.SubServer.Terminate").replace("$str$", label.toLowerCase() + " kill <Subservers>"),
                plugin.api.getLang("SubServers", "Command.Help.SubServer.Command").replace("$str$", label.toLowerCase() + " cmd <Subservers> <Command> [Args...]"),
                plugin.api.getLang("SubServers", "Command.Help.Host.Create").replace("$str$", label.toLowerCase() + " create <Name> <Host> <Template> [Version] [Port]"),
                plugin.api.getLang("SubServers", "Command.Help.SubServer.Update").replace("$str$", label.toLowerCase() + " update <Subservers> [[Template] <Version>]"),
        };
    }
}
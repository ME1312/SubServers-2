package net.ME1312.SubServers.Client.Bukkit;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.SubData.Client.SubDataClient;
import net.ME1312.SubServers.Client.Bukkit.Graphic.UIRenderer;
import net.ME1312.Galaxi.Library.Callback.Callback;
import net.ME1312.Galaxi.Library.Config.YAMLSection;
import net.ME1312.Galaxi.Library.Container;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.SubServers.Client.Bukkit.Network.API.*;
import net.ME1312.SubServers.Client.Bukkit.Network.Packet.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
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
                        sender.sendMessage(printHelp(label));
                    } else if (args[0].equalsIgnoreCase("version") || args[0].equalsIgnoreCase("ver")) {
                        String osarch;
                        if (System.getProperty("os.name").toLowerCase().startsWith("windows")) {
                            String arch = System.getenv("PROCESSOR_ARCHITECTURE");
                            String wow64Arch = System.getenv("PROCESSOR_ARCHITEW6432");

                            osarch = arch != null && arch.endsWith("64") || wow64Arch != null && wow64Arch.endsWith("64")?"x64":"x86";
                        } else if (System.getProperty("os.arch").endsWith("86")) {
                            osarch = "x86";
                        } else if (System.getProperty("os.arch").endsWith("64")) {
                            osarch = "x64";
                        } else {
                            osarch = System.getProperty("os.arch");
                        }

                        String javaarch = null;
                        switch (System.getProperty("sun.arch.data.model")) {
                            case "32":
                                javaarch = "x86";
                                break;
                            case "64":
                                javaarch = "x64";
                                break;
                            default:
                                if (!System.getProperty("sun.arch.data.model").equalsIgnoreCase("unknown"))
                                    javaarch = System.getProperty("sun.arch.data.model");
                        }
                        sender.sendMessage(plugin.api.getLang("SubServers", "Command.Version").replace("$str$", "SubServers.Client.Bukkit"));
                        sender.sendMessage(ChatColor.WHITE + "  " + System.getProperty("os.name") + ((!System.getProperty("os.name").toLowerCase().startsWith("windows"))?' ' + System.getProperty("os.version"):"") + ((osarch != null)?" [" + osarch + ']':"") + ChatColor.RESET + ',');
                        sender.sendMessage(ChatColor.WHITE + "  Java " + System.getProperty("java.version") + ((javaarch != null)?" [" + javaarch + ']':"") + ChatColor.RESET + ',');
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
                                            } else if (((SubServer) server).isEnabled() && ((SubServer) server).getCurrentIncompatibilities().size() == 0) {
                                                message += ChatColor.YELLOW;
                                            } else {
                                                message += ChatColor.RED;
                                            }
                                            message += server.getDisplayName() + " (" + ((plugin.config.get().getMap("Settings").getBoolean("Show-Addresses", false))?server.getAddress().getAddress().getHostAddress()+':':"") + server.getAddress().getPort() + ((server.getName().equals(server.getDisplayName()))?"":ChatColor.stripColor(div)+server.getName()) + ")";
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
                                        message += " (" + host.getAddress() + ((host.getName().equals(host.getDisplayName()))?"":ChatColor.stripColor(div)+host.getName()) + ")";
                                    } else if (!host.getName().equals(host.getDisplayName())) {
                                        message += " (" + host.getName() + ")";
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
                                        } else if (subserver.isEnabled() && subserver.getCurrentIncompatibilities().size() == 0) {
                                            message += ChatColor.YELLOW;
                                        } else {
                                            message += ChatColor.RED;
                                        }
                                        message += subserver.getDisplayName() + " (" + subserver.getAddress().getPort() + ((subserver.getName().equals(subserver.getDisplayName()))?"":ChatColor.stripColor(div)+subserver.getName()) + ")";
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
                                    message += ChatColor.WHITE + server.getDisplayName() + " (" + ((plugin.config.get().getMap("Settings").getBoolean("Show-Addresses", false))?server.getAddress().getAddress().getHostAddress()+':':"") + server.getAddress().getPort() + ((server.getName().equals(server.getDisplayName()))?"":ChatColor.stripColor(div)+server.getName()) + ")";
                                    i++;
                                }
                                if (i == 0) sender.sendMessage("  " + plugin.api.getLang("SubServers", "Command.List.Empty"));
                                else sender.sendMessage(message);
                                if (proxies.keySet().size() > 0) {
                                    sender.sendMessage(plugin.api.getLang("SubServers", "Command.List.Proxy-Header"));
                                    message = "  (master)";
                                    for (Proxy proxy : proxies.values()) {
                                        message += div;
                                        if (proxy.getSubData()[0] != null && proxy.isRedis()) {
                                            message += ChatColor.GREEN;
                                        } else if (proxy.getSubData()[0] != null) {
                                            message += ChatColor.AQUA;
                                        } else if (proxy.isRedis()) {
                                            message += ChatColor.WHITE;
                                        } else {
                                            message += ChatColor.RED;
                                        }
                                        message += proxy.getDisplayName() + ((proxy.getName().equals(proxy.getDisplayName()))?"":" ("+proxy.getName()+')');
                                    }
                                    sender.sendMessage(message);
                                }
                            }))));
                        }
                    } else if (args[0].equalsIgnoreCase("info") || args[0].equalsIgnoreCase("status")) {
                        if (args.length > 1) {
                            String type = (args.length > 2)?args[1]:null;
                            String name = args[(type != null)?2:1];

                            Runnable getServer = () -> plugin.api.getServer(name, server -> {
                                if (server != null) {
                                    sender.sendMessage(plugin.api.getLang("SubServers", "Command.Info").replace("$str$", ((server instanceof SubServer)?"Sub":"") + "Server") + ChatColor.WHITE + server.getDisplayName());
                                    if (!server.getName().equals(server.getDisplayName())) sender.sendMessage(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "System Name") + ChatColor.WHITE  + server.getName());
                                    if (server instanceof SubServer) {
                                        sender.sendMessage(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Enabled") + ((((SubServer) server).isEnabled())?ChatColor.GREEN+"yes":ChatColor.RED+"no"));
                                        if (!((SubServer) server).isEditable()) sender.sendMessage(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Editable") + ChatColor.RED + "no");
                                        sender.sendMessage(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Host") + ChatColor.WHITE  + ((SubServer) server).getHost());
                                    }
                                    if (server.getGroups().size() > 0) sender.sendMessage(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Group" + ((server.getGroups().size() > 1)?"s":"")) + ((server.getGroups().size() > 1)?"":ChatColor.WHITE + server.getGroups().get(0)));
                                    if (server.getGroups().size() > 1) for (String group : server.getGroups()) sender.sendMessage("    " + plugin.api.getLang("SubServers", "Command.Info.List") + ChatColor.WHITE + group);
                                    if (plugin.config.get().getMap("Settings").getBoolean("Show-Addresses", false)) sender.sendMessage(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Address") + ChatColor.WHITE + server.getAddress().getAddress().getHostAddress()+':'+server.getAddress().getPort());
                                    else sender.sendMessage(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Port") + ChatColor.AQUA.toString() + server.getAddress().getPort());
                                    if (server instanceof SubServer) sender.sendMessage(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Running") + ((((SubServer) server).isRunning())?ChatColor.GREEN+"yes":ChatColor.RED+"no"));
                                    if (!(server instanceof SubServer) || ((SubServer) server).isRunning()) {
                                        sender.sendMessage(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Connected") + ((server.getSubData()[0] != null)?ChatColor.GREEN+"yes"+((server.getSubData().length > 1)?ChatColor.AQUA+" +"+(server.getSubData().length-1):""):ChatColor.RED+"no"));
                                        sender.sendMessage(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Players") + ChatColor.AQUA + server.getPlayers().size() + " online");
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
                                        sender.sendMessage(plugin.api.getLang("SubServers", "Command.Info.Unknown"));
                                    } else {
                                        sender.sendMessage(plugin.api.getLang("SubServers", "Command.Info.Unknown-Server"));
                                    }
                                }
                            });
                            Runnable getGroup = () -> plugin.api.getGroup(name, group -> {
                                if (group != null) {
                                    sender.sendMessage(plugin.api.getLang("SubServers", "Command.Info").replace("$str$", "Group") + ChatColor.WHITE + name);
                                    sender.sendMessage(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Servers") + ((group.size() <= 0)?ChatColor.GRAY + "(none)":ChatColor.AQUA.toString() + group.size()));
                                    for (Server server : group) sender.sendMessage("    " + plugin.api.getLang("SubServers", "Command.Info.List") + ChatColor.WHITE + server.getDisplayName() + ((server.getName().equals(server.getDisplayName()))?"":" ("+server.getName()+')'));
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
                                    sender.sendMessage(plugin.api.getLang("SubServers", "Command.Info").replace("$str$", "Host") + ChatColor.WHITE + host.getDisplayName());
                                    if (!host.getName().equals(host.getDisplayName())) sender.sendMessage(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "System Name") + ChatColor.WHITE  + host.getName());
                                    sender.sendMessage(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Available") + ((host.isAvailable())?ChatColor.GREEN+"yes":ChatColor.RED+"no"));
                                    sender.sendMessage(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Enabled") + ((host.isEnabled())?ChatColor.GREEN+"yes":ChatColor.RED+"no"));
                                    if (plugin.config.get().getMap("Settings").getBoolean("Show-Addresses", false)) sender.sendMessage(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Address") + ChatColor.WHITE + host.getAddress().getHostAddress());
                                    if (host.getSubData().length > 0) sender.sendMessage(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Connected") + ((host.getSubData()[0] != null)?ChatColor.GREEN+"yes"+((host.getSubData().length > 1)?ChatColor.AQUA+" +"+(host.getSubData().length-1):""):ChatColor.RED+"no"));
                                    sender.sendMessage(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "SubServers") + ((host.getSubServers().keySet().size() <= 0)?ChatColor.GRAY + "(none)":ChatColor.AQUA.toString() + host.getSubServers().keySet().size()));
                                    for (SubServer subserver : host.getSubServers().values()) sender.sendMessage("    " + plugin.api.getLang("SubServers", "Command.Info.List") + ((subserver.isEnabled())?ChatColor.WHITE:ChatColor.GRAY) + subserver.getDisplayName() + ((subserver.getName().equals(subserver.getDisplayName()))?"":" ("+subserver.getName()+')'));
                                    sender.sendMessage(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Templates") + ((host.getCreator().getTemplates().keySet().size() <= 0)?ChatColor.GRAY + "(none)":ChatColor.AQUA.toString() + host.getCreator().getTemplates().keySet().size()));
                                    for (SubCreator.ServerTemplate template : host.getCreator().getTemplates().values()) sender.sendMessage("    " + plugin.api.getLang("SubServers", "Command.Info.List") + ((template.isEnabled())?ChatColor.WHITE:ChatColor.GRAY) + template.getDisplayName() + ((template.getName().equals(template.getDisplayName()))?"":" ("+template.getName()+')'));
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
                                    sender.sendMessage(plugin.api.getLang("SubServers", "Command.Info").replace("$str$", "Proxy") + ChatColor.WHITE + proxy.getDisplayName());
                                    if (!proxy.getName().equals(proxy.getDisplayName())) sender.sendMessage(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "System Name") + ChatColor.WHITE  + proxy.getName());
                                    sender.sendMessage(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Connected") + ((proxy.getSubData()[0] != null)?ChatColor.GREEN+"yes"+((proxy.getSubData().length > 1)?ChatColor.AQUA+" +"+(proxy.getSubData().length-1):""):ChatColor.RED+"no"));
                                    sender.sendMessage(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Redis")  + ((proxy.isRedis())?ChatColor.GREEN:ChatColor.RED+"un") + "available");
                                    if (proxy.isRedis()) sender.sendMessage(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Players") + ChatColor.AQUA + proxy.getPlayers().size() + " online");
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
                                    default:
                                        sender.sendMessage(plugin.api.getLang("SubServers", "Command.Info.Unknown-Type"));
                                }
                            }
                        } else {
                            sender.sendMessage(plugin.api.getLang("SubServers", "Command.Generic.Usage").replace("$str$", label.toLowerCase() + " " + args[0].toLowerCase() + " [proxy|host|group|server] <Name>"));
                        }
                    } else if (args[0].equalsIgnoreCase("start")) {
                        if (args.length > 1) {
                            if (sender.hasPermission("subservers.subserver.start.*") || sender.hasPermission("subservers.subserver.start." + args[1].toLowerCase())) {
                                ((SubDataClient) plugin.api.getSubDataNetwork()[0]).sendPacket(new PacketStartServer((sender instanceof Player)?((Player) sender).getUniqueId():null, args[1], data -> {
                                    switch (data.getInt(0x0001)) {
                                        case 3:
                                            sender.sendMessage(plugin.api.getLang("SubServers", "Command.Start.Unknown"));
                                            break;
                                        case 4:
                                            sender.sendMessage(plugin.api.getLang("SubServers", "Command.Start.Invalid"));
                                            break;
                                        case 5:
                                            sender.sendMessage(plugin.api.getLang("SubServers", "Command.Start.Host-Unavailable"));
                                            break;
                                        case 6:
                                            sender.sendMessage(plugin.api.getLang("SubServers", "Command.Start.Host-Disabled"));
                                            break;
                                        case 7:
                                            sender.sendMessage(plugin.api.getLang("SubServers", "Command.Start.Server-Disabled"));
                                            break;
                                        case 8:
                                            sender.sendMessage(plugin.api.getLang("SubServers", "Command.Start.Running"));
                                            break;
                                        case 9:
                                            sender.sendMessage(plugin.api.getLang("SubServers", "Command.Start.Server-Incompatible").replace("$str$", data.getString(0x0002)));
                                            break;
                                        case 0:
                                        case 1:
                                            sender.sendMessage(plugin.api.getLang("SubServers", "Command.Start"));
                                            break;
                                    }
                                }));
                            } else {
                                sender.sendMessage(plugin.api.getLang("SubServers", "Command.Generic.Invalid-Permission").replace("$str$", "subservers.subserver.start." + args[1].toLowerCase()));
                            }
                        } else {
                            sender.sendMessage(plugin.api.getLang("SubServers", "Command.Generic.Usage").replace("$str$", label.toLowerCase() + " " + args[0].toLowerCase() + " <SubServer>"));
                        }
                    } else if (args[0].equalsIgnoreCase("restart")) {
                        if (args.length > 1) {
                            if ((sender.hasPermission("subservers.subserver.stop.*") || sender.hasPermission("subservers.subserver.stop." + args[1].toLowerCase())) && (sender.hasPermission("subservers.subserver.start.*") || sender.hasPermission("subservers.subserver.start." + args[1].toLowerCase()))) {
                                Runnable starter = () -> ((SubDataClient) plugin.api.getSubDataNetwork()[0]).sendPacket(new PacketStartServer(null, args[1], data -> {
                                    switch (data.getInt(0x0001)) {
                                        case 3:
                                        case 4:
                                            sender.sendMessage(plugin.api.getLang("SubServers", "Command.Restart.Disappeared"));
                                            break;
                                        case 5:
                                            sender.sendMessage(plugin.api.getLang("SubServers", "Command.Restart.Host-Unavailable"));
                                            break;
                                        case 6:
                                            sender.sendMessage(plugin.api.getLang("SubServers", "Command.Restart.Host-Disabled"));
                                            break;
                                        case 7:
                                            sender.sendMessage(plugin.api.getLang("SubServers", "Command.Restart.Server-Disabled"));
                                            break;
                                        case 9:
                                            sender.sendMessage(plugin.api.getLang("SubServers", "Command.Restart.Server-Incompatible"));
                                            break;
                                        case 8:
                                        case 0:
                                        case 1:
                                            sender.sendMessage(plugin.api.getLang("SubServers", "Command.Restart.Finish"));
                                            break;
                                    }
                                }));

                                final Container<Boolean> listening = new Container<Boolean>(true);
                                PacketInExRunEvent.callback("SubStoppedEvent", new Callback<ObjectMap<String>>() {
                                    @Override
                                    public void run(ObjectMap<String> json) {
                                        try {
                                            if (listening.get()) if (!json.getString("server").equalsIgnoreCase(args[1])) {
                                                PacketInExRunEvent.callback("SubStoppedEvent", this);
                                            } else {
                                                Bukkit.getScheduler().runTaskLater(plugin, starter, 5);
                                            }
                                        } catch (Exception e) {}
                                    }
                                });

                                Callback<ObjectMap<Integer>> stopper = data -> {
                                    if (data.getInt(0x0000) != 0) listening.set(false);
                                    switch (data.getInt(0x0000)) {
                                        case 3:
                                            sender.sendMessage(plugin.api.getLang("SubServers", "Command.Restart.Unknown"));
                                            break;
                                        case 4:
                                            sender.sendMessage(plugin.api.getLang("SubServers", "Command.Restart.Invalid"));
                                            break;
                                        case 5:
                                            starter.run();
                                            break;
                                        case 0:
                                        case 1:
                                            sender.sendMessage(plugin.api.getLang("SubServers", "Command.Restart"));
                                            break;
                                    }
                                };

                                if (plugin.api.getName().equalsIgnoreCase(args[1])) {
                                    listening.set(false);
                                    ((SubDataClient) plugin.api.getSubDataNetwork()[0]).sendPacket(new PacketRestartServer((sender instanceof Player)?((Player) sender).getUniqueId():null, args[1], stopper));
                                } else {
                                    ((SubDataClient) plugin.api.getSubDataNetwork()[0]).sendPacket(new PacketStopServer((sender instanceof Player)?((Player) sender).getUniqueId():null, args[1], false, stopper));
                                }
                            } else if (!(sender.hasPermission("subservers.subserver.stop.*") || sender.hasPermission("subservers.subserver.stop." + args[1].toLowerCase()))) {
                                sender.sendMessage(plugin.api.getLang("SubServers", "Command.Generic.Invalid-Permission").replace("$str$", "subservers.subserver.stop." + args[1].toLowerCase()));
                            } else {
                                sender.sendMessage(plugin.api.getLang("SubServers", "Command.Generic.Invalid-Permission").replace("$str$", "subservers.subserver.start." + args[1].toLowerCase()));
                            }
                        } else {
                            sender.sendMessage(plugin.api.getLang("SubServers", "Command.Generic.Usage").replace("$str$", label.toLowerCase() + " " + args[0].toLowerCase() + " <SubServer>"));
                        }
                    } else if (args[0].equalsIgnoreCase("stop")) {
                        if (args.length > 1) {
                            if (sender.hasPermission("subservers.subserver.stop.*") || sender.hasPermission("subservers.subserver.stop." + args[1].toLowerCase())) {
                                ((SubDataClient) plugin.api.getSubDataNetwork()[0]).sendPacket(new PacketStopServer((sender instanceof Player)?((Player) sender).getUniqueId():null, args[1], false, data -> {
                                    switch (data.getInt(0x0001)) {
                                        case 3:
                                            sender.sendMessage(plugin.api.getLang("SubServers", "Command.Stop.Unknown"));
                                            break;
                                        case 4:
                                            sender.sendMessage(plugin.api.getLang("SubServers", "Command.Stop.Invalid"));
                                            break;
                                        case 5:
                                            sender.sendMessage(plugin.api.getLang("SubServers", "Command.Stop.Not-Running"));
                                            break;
                                        case 0:
                                        case 1:
                                            sender.sendMessage(plugin.api.getLang("SubServers", "Command.Stop"));
                                            break;
                                    }
                                }));
                            } else {
                                sender.sendMessage(plugin.api.getLang("SubServers", "Command.Generic.Invalid-Permission").replace("$str$", "subservers.subserver.stop." + args[1].toLowerCase()));
                            }
                        } else {
                            sender.sendMessage(plugin.api.getLang("SubServers", "Command.Generic.Usage").replace("$str$", label.toLowerCase() + " " + args[0].toLowerCase() + " <SubServer>"));
                        }
                    } else if (args[0].equalsIgnoreCase("kill") || args[0].equalsIgnoreCase("terminate")) {
                        if (args.length > 1) {
                            if (sender.hasPermission("subservers.subserver.terminate.*") || sender.hasPermission("subservers.subserver.terminate." + args[1].toLowerCase())) {
                                ((SubDataClient) plugin.api.getSubDataNetwork()[0]).sendPacket(new PacketStopServer((sender instanceof Player)?((Player) sender).getUniqueId():null, args[1], true, data -> {
                                    switch (data.getInt(0x0001)) {
                                        case 3:
                                            sender.sendMessage(plugin.api.getLang("SubServers", "Command.Terminate.Unknown"));
                                            break;
                                        case 4:
                                            sender.sendMessage(plugin.api.getLang("SubServers", "Command.Terminate.Invalid"));
                                            break;
                                        case 5:
                                            sender.sendMessage(plugin.api.getLang("SubServers", "Command.Terminate.Not-Running"));
                                            break;
                                        case 0:
                                        case 1:
                                            sender.sendMessage(plugin.api.getLang("SubServers", "Command.Terminate"));
                                            break;
                                    }
                                }));
                            } else {
                                sender.sendMessage(plugin.api.getLang("SubServers", "Command.Generic.Invalid-Permission").replace("$str$", "subservers.subserver.terminate." + args[1].toLowerCase()));
                            }
                        } else {
                            sender.sendMessage(plugin.api.getLang("SubServers", "Command.Generic.Usage").replace("$str$", label.toLowerCase() + " " + args[0].toLowerCase() + " <SubServer>"));
                        }
                    } else if (args[0].equalsIgnoreCase("cmd") || args[0].equalsIgnoreCase("command")) {
                        if (args.length > 2) {
                            if (sender.hasPermission("subservers.subserver.command.*") || sender.hasPermission("subservers.subserver.command." + args[1].toLowerCase())) {
                                int i = 2;
                                String str = args[2];
                                if (args.length > 3) {
                                    do {
                                        i++;
                                        str = str + " " + args[i];
                                    } while ((i + 1) != args.length);
                                }
                                final String cmd = str;
                                ((SubDataClient) plugin.api.getSubDataNetwork()[0]).sendPacket(new PacketCommandServer((sender instanceof Player)?((Player) sender).getUniqueId():null, args[1], cmd, data -> {
                                    switch (data.getInt(0x0001)) {
                                        case 3:
                                            sender.sendMessage(plugin.api.getLang("SubServers", "Command.Command.Unknown"));
                                            break;
                                        case 4:
                                            sender.sendMessage(plugin.api.getLang("SubServers", "Command.Command.Invalid"));
                                            break;
                                        case 5:
                                            sender.sendMessage(plugin.api.getLang("SubServers", "Command.Command.Not-Running"));
                                            break;
                                        case 0:
                                        case 1:
                                            sender.sendMessage(plugin.api.getLang("SubServers", "Command.Command"));
                                            break;
                                    }
                                }));
                            } else {
                                sender.sendMessage(plugin.api.getLang("SubServers", "Command.Generic.Invalid-Permission").replace("$str$", "subservers.subserver.command." + args[1].toLowerCase()));
                            }
                        } else {
                            sender.sendMessage(plugin.api.getLang("SubServers", "Command.Generic.Usage").replace("$str$", label.toLowerCase() + " " + args[0].toLowerCase() + " <SubServer> <Command> [Args...]"));
                        }
                    } else if (args[0].equalsIgnoreCase("create")) {
                        if (args.length > 3) {
                            if (sender.hasPermission("subservers.host.create.*") || sender.hasPermission("subservers.host.create." + args[2].toLowerCase())) {
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
                                                sender.sendMessage(plugin.api.getLang("SubServers", "Command.Start.Host-Unavailable"));
                                                break;
                                            case 7:
                                                sender.sendMessage(plugin.api.getLang("SubServers", "Command.Start.Host-Disabled"));
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
                                            case 1:
                                                sender.sendMessage(plugin.api.getLang("SubServers", "Command.Creator"));
                                                break;
                                        }
                                    }));
                                }
                            } else {
                                sender.sendMessage(plugin.api.getLang("SubServers", "Command.Generic.Invalid-Permission").replace("$str$", "subservers.host.create." + args[2].toLowerCase()));
                            }
                        } else {
                            sender.sendMessage(plugin.api.getLang("SubServers", "Command.Generic.Usage").replace("$str$", label.toLowerCase() + " " + args[0].toLowerCase() + " <Name> <Host> <Template> [Version] [Port]"));
                        }
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
                                            if (sender.hasPermission("subservers.host.create.*") || sender.hasPermission("subservers.host.create." + args[2].toLowerCase())) plugin.gui.getRenderer((Player) sender).hostCreator(new UIRenderer.CreatorOptions(args[2]));
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
                                } catch (Throwable e) {
                                    List<String> list = new LinkedList<String>();
                                    list.addAll(Arrays.asList(args));
                                    list.remove(0);
                                    new InvocationTargetException(e, "Could not render page with arguments: " + list.toString()).printStackTrace();
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
                        sender.sendMessage(printHelp(label));
                    }
                }
            } else if (sender.hasPermission("subservers.interface") && sender instanceof Player) {
                plugin.gui.getRenderer((Player) sender).newUI();
            } else {
                sender.sendMessage(plugin.api.getLang("SubServers", "Command.Generic.Invalid-Permission").replace("$str$", "subservers.command"));
            }
        }
        return true;
    }

    private String[] printHelp(String label) {
        return new String[]{
                plugin.api.getLang("SubServers", "Command.Help.Header"),
                plugin.api.getLang("SubServers", "Command.Help.Help").replace("$str$", label.toLowerCase() + " help"),
                plugin.api.getLang("SubServers", "Command.Help.List").replace("$str$", label.toLowerCase() + " list"),
                plugin.api.getLang("SubServers", "Command.Help.Version").replace("$str$", label.toLowerCase() + " version"),
                plugin.api.getLang("SubServers", "Command.Help.Info").replace("$str$", label.toLowerCase() + " info [proxy|host|group|server] <Name>"),
                plugin.api.getLang("SubServers", "Command.Help.SubServer.Start").replace("$str$", label.toLowerCase() + " start <SubServer>"),
                plugin.api.getLang("SubServers", "Command.Help.SubServer.Restart").replace("$str$", label.toLowerCase() + " restart <SubServer>"),
                plugin.api.getLang("SubServers", "Command.Help.SubServer.Stop").replace("$str$", label.toLowerCase() + " stop <SubServer>"),
                plugin.api.getLang("SubServers", "Command.Help.SubServer.Terminate").replace("$str$", label.toLowerCase() + " kill <SubServer>"),
                plugin.api.getLang("SubServers", "Command.Help.SubServer.Command").replace("$str$", label.toLowerCase() + " cmd <SubServer> <Command> [Args...]"),
                plugin.api.getLang("SubServers", "Command.Help.Host.Create").replace("$str$", label.toLowerCase() + " create <Name> <Host> <Template> [Version] [Port]"),
        };
    }
}
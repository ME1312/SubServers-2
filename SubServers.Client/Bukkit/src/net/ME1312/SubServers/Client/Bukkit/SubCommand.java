package net.ME1312.SubServers.Client.Bukkit;

import net.ME1312.SubServers.Client.Bukkit.Graphic.UIRenderer;
import net.ME1312.SubServers.Client.Bukkit.Library.Config.YAMLSection;
import net.ME1312.SubServers.Client.Bukkit.Library.Util;
import net.ME1312.SubServers.Client.Bukkit.Library.Version.Version;
import net.ME1312.SubServers.Client.Bukkit.Network.API.Host;
import net.ME1312.SubServers.Client.Bukkit.Network.API.Proxy;
import net.ME1312.SubServers.Client.Bukkit.Network.API.Server;
import net.ME1312.SubServers.Client.Bukkit.Network.API.SubServer;
import net.ME1312.SubServers.Client.Bukkit.Network.Packet.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.*;

public final class SubCommand implements CommandExecutor {
    private SubPlugin plugin;

    public SubCommand(SubPlugin plugin) {
        this.plugin = plugin;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        label = "/" + label;
        if (plugin.subdata == null) {
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
                        sender.sendMessage(plugin.api.getLang("SubServers", "Command.Version").replace("$str$", "SubServers.Client.Bukkit"));
                        sender.sendMessage(ChatColor.WHITE + "  " + System.getProperty("os.name") + ' ' + System.getProperty("os.version") + ChatColor.RESET + ',');
                        sender.sendMessage(ChatColor.WHITE + "  Java " + System.getProperty("java.version") + ChatColor.RESET + ',');
                        sender.sendMessage(ChatColor.WHITE + "  " + Bukkit.getName() + ' ' + Bukkit.getVersion() + ChatColor.RESET + ',');
                        sender.sendMessage(ChatColor.WHITE + "  SubServers.Client.Bukkit v" + plugin.version.toExtendedString() + ((plugin.api.getPluginBuild() != null)?" (" + plugin.api.getPluginBuild() + ')':""));
                        sender.sendMessage("");
                        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                            try {
                                YAMLSection tags = new YAMLSection(plugin.parseJSON("{\"tags\":" + Util.readAll(new BufferedReader(new InputStreamReader(new URL("https://api.github.com/repos/ME1312/SubServers-2/git/refs/tags").openStream(), Charset.forName("UTF-8")))) + '}'));
                                List<Version> versions = new LinkedList<Version>();

                                Version updversion = plugin.version;
                                int updcount = 0;
                                for (YAMLSection tag : tags.getSectionList("tags")) versions.add(Version.fromString(tag.getString("ref").substring(10)));
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
                                            } else if (((SubServer) server).isTemporary()) {
                                                message += ChatColor.AQUA;
                                            } else if (((SubServer) server).isRunning()) {
                                                message += ChatColor.GREEN;
                                            } else if (((SubServer) server).isEnabled() && ((SubServer) server).getCurrentIncompatibilities().size() == 0) {
                                                message += ChatColor.YELLOW;
                                            } else {
                                                message += ChatColor.RED;
                                            }
                                            message += server.getDisplayName() + " (" + ((plugin.config.get().getSection("Settings").getBoolean("Show-Addresses", false))?server.getAddress().getAddress().getHostAddress()+':':"") + server.getAddress().getPort() + ((server.getName().equals(server.getDisplayName()))?"":ChatColor.stripColor(div)+server.getName()) + ")";
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
                                    if (host.isEnabled()) {
                                        message += ChatColor.AQUA;
                                    } else {
                                        message += ChatColor.RED;
                                    }
                                    message += host.getDisplayName();
                                    if (plugin.config.get().getSection("Settings").getBoolean("Show-Addresses", false)) {
                                        message += " (" + host.getAddress() + ((host.getName().equals(host.getDisplayName()))?"":ChatColor.stripColor(div)+host.getName()) + ")";
                                    } else if (!host.getName().equals(host.getDisplayName())) {
                                        message += " (" + host + ")";
                                    }
                                    message += plugin.api.getLang("SubServers", "Command.List.Header");
                                    for (SubServer subserver : host.getSubServers().values()) {
                                        if (i != 0) message += div;
                                        if (subserver.isTemporary()) {
                                            message += ChatColor.AQUA;
                                        } else if (subserver.isRunning()) {
                                            message += ChatColor.GREEN;
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
                                    message += ChatColor.WHITE + server.getDisplayName() + " (" + ((plugin.config.get().getSection("Settings").getBoolean("Show-Addresses", false))?server.getAddress().getAddress().getHostAddress()+':':"") + server.getAddress().getPort() + ((server.getName().equals(server.getDisplayName()))?"":ChatColor.stripColor(div)+server.getName()) + ")";
                                    i++;
                                }
                                if (i == 0) sender.sendMessage("  " + plugin.api.getLang("SubServers", "Command.List.Empty"));
                                else sender.sendMessage(message);
                                if (proxies.keySet().size() > 0) {
                                    sender.sendMessage(plugin.api.getLang("SubServers", "Command.List.Proxy-Header"));
                                    message = "  (master)";
                                    for (Proxy proxy : proxies.values()) {
                                        message += div;
                                        if (proxy.getSubData() != null && proxy.isRedis()) {
                                            message += ChatColor.GREEN;
                                        } else if (proxy.getSubData() != null) {
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
                            plugin.api.getServer(args[1], subserver -> {
                                if (subserver == null) {
                                    sender.sendMessage(plugin.api.getLang("SubServers", "Command.Info.Unknown"));
                                } else if (!(subserver instanceof SubServer)) {
                                    sender.sendMessage(plugin.api.getLang("SubServers", "Command.Info.Invalid"));
                                } else ((SubServer) subserver).getHost(host -> {
                                    if (host == null) {
                                        sender.sendMessage(plugin.api.getLang("SubServers", "Command.Info.Invalid"));
                                    } else {
                                        sender.sendMessage(plugin.api.getLang("SubServers", "Command.Info").replace("$str$", subserver.getDisplayName()));
                                        if (!subserver.getName().equals(subserver.getDisplayName()))
                                            sender.sendMessage("  " + plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Real Name") + ChatColor.AQUA + subserver.getName());
                                        sender.sendMessage("  " + plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Host") + ChatColor.AQUA + host.getName());
                                        sender.sendMessage("  " + plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Enabled") + ((((SubServer) subserver).isEnabled())?ChatColor.GREEN+"yes":ChatColor.DARK_RED+"no"));
                                        sender.sendMessage("  " + plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Editable") + ((((SubServer) subserver).isEditable())?ChatColor.GREEN+"yes":ChatColor.DARK_RED+"no"));
                                        if (subserver.getGroups().size() > 0) {
                                            sender.sendMessage("  " + plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Group"));
                                            for (String group : subserver.getGroups())
                                                sender.sendMessage("  " + plugin.api.getLang("SubServers", "Command.Info.List").replace("$str$", ChatColor.GOLD + group));
                                        }
                                        if (((SubServer) subserver).isTemporary()) sender.sendMessage("  " + plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Temporary") + ChatColor.GREEN+"yes");
                                        sender.sendMessage("  " + plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Running") + ((((SubServer) subserver).isRunning())?ChatColor.GREEN+"yes":ChatColor.DARK_RED+"no"));
                                        sender.sendMessage("  " + plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Logging") + ((((SubServer) subserver).isLogging())?ChatColor.GREEN+"yes":ChatColor.DARK_RED+"no"));
                                        if (plugin.config.get().getSection("Settings").getBoolean("Show-Addresses", false)) {
                                            sender.sendMessage("  " + plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Address") + ChatColor.AQUA + subserver.getAddress().getAddress().getHostAddress());
                                        } else {
                                            sender.sendMessage("  " + plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Port") + ChatColor.AQUA + subserver.getAddress().getPort());
                                        }
                                        sender.sendMessage("  " + plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Auto Restart") + ((((SubServer) subserver).willAutoRestart())?ChatColor.GREEN+"yes":ChatColor.DARK_RED+"no"));
                                        sender.sendMessage("  " + plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Hidden") + ((subserver.isHidden())?ChatColor.GREEN+"yes":ChatColor.DARK_RED+"no"));
                                        if (((SubServer) subserver).getIncompatibilities().size() > 0) {
                                            List<String> current = new ArrayList<String>();
                                            for (String other : ((SubServer) subserver).getCurrentIncompatibilities()) current.add(other.toLowerCase());
                                            sender.sendMessage("  " + plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Incompatibilities"));
                                            for (String other : ((SubServer) subserver).getIncompatibilities())
                                                sender.sendMessage("  " + plugin.api.getLang("SubServers", "Command.Info.List").replace("$str$", ((current.contains(other.toLowerCase()))?ChatColor.DARK_RED:ChatColor.RED) + other));
                                        }
                                        sender.sendMessage("  " + plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Signature") + ChatColor.AQUA + subserver.getSignature());
                                    }
                                });
                            });
                        } else {
                            sender.sendMessage(plugin.api.getLang("SubServers", "Command.Generic.Usage").replace("$str$", label.toLowerCase() + " " + args[0].toLowerCase() + " <SubServer>"));
                        }
                    } else if (args[0].equalsIgnoreCase("start")) {
                        if (args.length > 1) {
                            if (sender.hasPermission("subservers.subserver.start.*") || sender.hasPermission("subservers.subserver.start." + args[1].toLowerCase())) {
                                plugin.subdata.sendPacket(new PacketStartServer((sender instanceof Player)?((Player) sender).getUniqueId():null, args[1], data -> {
                                    switch (data.getInt("r")) {
                                        case 3:
                                            sender.sendMessage(plugin.api.getLang("SubServers", "Command.Start.Unknown"));
                                            break;
                                        case 4:
                                            sender.sendMessage(plugin.api.getLang("SubServers", "Command.Start.Invalid"));
                                            break;
                                        case 5:
                                            sender.sendMessage(plugin.api.getLang("SubServers", "Command.Start.Host-Disabled"));
                                            break;
                                        case 6:
                                            sender.sendMessage(plugin.api.getLang("SubServers", "Command.Start.Server-Disabled"));
                                            break;
                                        case 7:
                                            sender.sendMessage(plugin.api.getLang("SubServers", "Command.Start.Running"));
                                            break;
                                        case 8:
                                            sender.sendMessage(plugin.api.getLang("SubServers", "Command.Start.Server-Incompatible").replace("$str$", data.getString("m").split(":\\s")[1]));
                                            break;
                                        case 0:
                                        case 1:
                                            sender.sendMessage(plugin.api.getLang("SubServers", "Command.Start"));
                                            break;
                                        default:
                                            Bukkit.getLogger().warning("SubData > PacketStartServer(" + ((sender instanceof Player)?((Player) sender).getUniqueId().toString():"null") + ", " + args[1] + ") responded with: " + data.getString("m"));
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
                    } else if (args[0].equalsIgnoreCase("stop")) {
                        if (args.length > 1) {
                            if (sender.hasPermission("subservers.subserver.stop.*") || sender.hasPermission("subservers.subserver.stop." + args[1].toLowerCase())) {
                                plugin.subdata.sendPacket(new PacketStopServer((sender instanceof Player)?((Player) sender).getUniqueId():null, args[1], false, data -> {
                                    switch (data.getInt("r")) {
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
                                        default:
                                            Bukkit.getLogger().warning("SubData > PacketStopServer(" + ((sender instanceof Player)?((Player) sender).getUniqueId().toString():"null") + ", " + args[1] + ", false) responded with: " + data.getString("m"));
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
                                plugin.subdata.sendPacket(new PacketStopServer((sender instanceof Player)?((Player) sender).getUniqueId():null, args[1], true, data -> {
                                    switch (data.getInt("r")) {
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
                                        default:
                                            Bukkit.getLogger().warning("SubData > PacketStopServer(" + ((sender instanceof Player)?((Player) sender).getUniqueId().toString():"null") + ", " + args[1] + ", true) responded with: " + data.getString("m"));
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
                                plugin.subdata.sendPacket(new PacketCommandServer((sender instanceof Player)?((Player) sender).getUniqueId():null, args[1], cmd, data -> {
                                    switch (data.getInt("r")) {
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
                                        default:
                                            Bukkit.getLogger().warning("SubData > PacketCommandServer(" + ((sender instanceof Player)?((Player) sender).getUniqueId().toString():"null") + ", " + args[1] + ", /" + cmd + ") responded with: " + data.getString("m"));
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
                        if (args.length > 5) {
                            if (sender.hasPermission("subservers.host.create.*") || sender.hasPermission("subservers.host.create." + args[2].toLowerCase())) {
                                if (Util.isException(() -> Integer.parseInt(args[5]))) {
                                    sender.sendMessage(plugin.api.getLang("SubServers", "Command.Creator.Invalid-Port"));
                                } else {
                                    plugin.subdata.sendPacket(new PacketCreateServer((sender instanceof Player)?((Player) sender).getUniqueId():null, args[1], args[2], args[3], new Version(args[4]), Integer.parseInt(args[5]), data -> {
                                        switch (data.getInt("r")) {
                                            case 3:
                                            case 4:
                                                sender.sendMessage(plugin.api.getLang("SubServers", "Command.Creator.Exists"));
                                                break;
                                            case 5:
                                                sender.sendMessage(plugin.api.getLang("SubServers", "Command.Creator.Unknown-Host"));
                                                break;
                                            case 6:
                                                sender.sendMessage(plugin.api.getLang("SubServers", "Command.Creator.Invalid-Template"));
                                                break;
                                            case 7:
                                                sender.sendMessage(plugin.api.getLang("SubServers", "Command.Creator.Invalid-Version"));
                                                break;
                                            case 8:
                                                sender.sendMessage(plugin.api.getLang("SubServers", "Command.Creator.Invalid-Port"));
                                                break;
                                            case 0:
                                            case 1:
                                                sender.sendMessage(plugin.api.getLang("SubServers", "Command.Creator"));
                                                break;
                                            default:
                                                Bukkit.getLogger().warning("SubData > PacketCreateServer(" + ((sender instanceof Player)?((Player) sender).getUniqueId().toString():"null") + ", " + args[1] + ", " + args[2] + ", " + args[3] + ", " + args[4] + ", " + args[5] + ") responded with: " + data.getString("m"));
                                                sender.sendMessage(plugin.api.getLang("SubServers", "Command.Creator"));
                                                break;
                                        }
                                    }));
                                }
                            } else {
                                sender.sendMessage(plugin.api.getLang("SubServers", "Command.Generic.Invalid-Permission").replace("$str$", "subservers.host.create." + args[2].toLowerCase()));
                            }
                        } else {
                            sender.sendMessage(plugin.api.getLang("SubServers", "Command.Generic.Usage").replace("$str$", label.toLowerCase() + " " + args[0].toLowerCase() + " <Name> <Host> <Template> <Version> <Port>"));
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
                plugin.api.getLang("SubServers", "Command.Help.Info").replace("$str$", label.toLowerCase() + " info <SubServer>"),
                plugin.api.getLang("SubServers", "Command.Help.SubServer.Start").replace("$str$", label.toLowerCase() + " start <SubServer>"),
                plugin.api.getLang("SubServers", "Command.Help.SubServer.Stop").replace("$str$", label.toLowerCase() + " stop <SubServer>"),
                plugin.api.getLang("SubServers", "Command.Help.SubServer.Terminate").replace("$str$", label.toLowerCase() + " kill <SubServer>"),
                plugin.api.getLang("SubServers", "Command.Help.SubServer.Command").replace("$str$", label.toLowerCase() + " cmd <SubServer> <Command> [Args...]"),
                plugin.api.getLang("SubServers", "Command.Help.Host.Create").replace("$str$", label.toLowerCase() + " create <Name> <Host> <Template> <Version> <Port>"),
        };
    }
}
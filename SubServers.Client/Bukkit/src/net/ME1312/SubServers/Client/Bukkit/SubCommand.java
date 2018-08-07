package net.ME1312.SubServers.Client.Bukkit;

import net.ME1312.SubServers.Client.Bukkit.Graphic.UIRenderer;
import net.ME1312.SubServers.Client.Bukkit.Library.Config.YAMLSection;
import net.ME1312.SubServers.Client.Bukkit.Library.Util;
import net.ME1312.SubServers.Client.Bukkit.Library.Version.Version;
import net.ME1312.SubServers.Client.Bukkit.Network.API.Proxy;
import net.ME1312.SubServers.Client.Bukkit.Network.Packet.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
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
            return false;
        } else if (plugin.lang == null) {
            new IllegalStateException("There are no lang options available at this time").printStackTrace();
            return false;
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
                            plugin.subdata.sendPacket(new PacketDownloadServerList(null, null, data -> {
                                int i = 0;
                                boolean sent = false;
                                sender.sendMessage(plugin.api.getLang("SubServers", "Command.List.Group-Header"));
                                String div = plugin.api.getLang("SubServers", "Command.List.Divider");

                                for (String group : data.getSection("groups").getKeys()) {
                                    String message = "  ";
                                    message += ChatColor.GOLD + group + plugin.api.getLang("SubServers", "Command.List.Header");
                                    for (String server : data.getSection("groups").getSection(group).getKeys()) {
                                        if (i != 0) message += div;
                                        if (!data.getSection("groups").getSection(group).getSection(server).getKeys().contains("host")) {
                                            message += ChatColor.WHITE;
                                        } else if (data.getSection("groups").getSection(group).getSection(server).getBoolean("temp")) {
                                            message += ChatColor.AQUA;
                                        } else if (data.getSection("groups").getSection(group).getSection(server).getBoolean("running")) {
                                            message += ChatColor.GREEN;
                                        } else if (data.getSection("groups").getSection(group).getSection(server).getBoolean("enabled") && data.getSection("groups").getSection(group).getSection(server).getList("incompatible").size() == 0) {
                                            message += ChatColor.YELLOW;
                                        } else {
                                            message += ChatColor.RED;
                                        }
                                        message += data.getSection("groups").getSection(group).getSection(server).getString("display") + " (" + ((plugin.config.get().getSection("Settings").getBoolean("Show-Addresses", false))?data.getSection("groups").getSection(group).getSection(server).getString("address"):data.getSection("groups").getSection(group).getSection(server).getString("address").split(":")[data.getSection("groups").getSection(group).getSection(server).getString("address").split(":").length - 1]) + ((server.equals(data.getSection("groups").getSection(group).getSection(server).getString("display")))?"":ChatColor.stripColor(div)+server) + ")";
                                        i++;
                                    }
                                    if (i == 0) message += plugin.api.getLang("SubServers", "Command.List.Empty");
                                    sender.sendMessage(message);
                                    i = 0;
                                    sent = true;
                                }
                                if (!sent) sender.sendMessage("  " + plugin.api.getLang("SubServers", "Command.List.Empty"));
                                sent = false;
                                sender.sendMessage(plugin.api.getLang("SubServers", "Command.List.Host-Header"));
                                for (String host : data.getSection("hosts").getKeys()) {
                                    String message = "  ";
                                    if (data.getSection("hosts").getSection(host).getBoolean("enabled")) {
                                        message += ChatColor.AQUA;
                                    } else {
                                        message += ChatColor.RED;
                                    }
                                    message += data.getSection("hosts").getSection(host).getString("display");
                                    if (plugin.config.get().getSection("Settings").getBoolean("Show-Addresses", false)) {
                                        message += " (" + data.getSection("hosts").getSection(host).getString("address") + ((host.equals(data.getSection("hosts").getSection(host).getString("display")))?"":ChatColor.stripColor(div)+host) + ")";
                                    } else if (!host.equals(data.getSection("hosts").getSection(host).getString("display"))) {
                                        message += " (" + host + ")";
                                    }
                                    message += plugin.api.getLang("SubServers", "Command.List.Header");
                                    for (String subserver : data.getSection("hosts").getSection(host).getSection("servers").getKeys()) {
                                        if (i != 0) message += div;
                                        if (data.getSection("hosts").getSection(host).getSection("servers").getSection(subserver).getBoolean("temp")) {
                                            message += ChatColor.AQUA;
                                        } else if (data.getSection("hosts").getSection(host).getSection("servers").getSection(subserver).getBoolean("running")) {
                                            message += ChatColor.GREEN;
                                        } else if (data.getSection("hosts").getSection(host).getSection("servers").getSection(subserver).getBoolean("enabled") && data.getSection("hosts").getSection(host).getSection("servers").getSection(subserver).getList("incompatible").size() == 0) {
                                            message += ChatColor.YELLOW;
                                        } else {
                                            message += ChatColor.RED;
                                        }
                                        message += data.getSection("hosts").getSection(host).getSection("servers").getSection(subserver).getString("display") + " (" + data.getSection("hosts").getSection(host).getSection("servers").getSection(subserver).getString("address").split(":")[data.getSection("hosts").getSection(host).getSection("servers").getSection(subserver).getString("address").split(":").length - 1] + ((subserver.equals(data.getSection("hosts").getSection(host).getSection("servers").getSection(subserver).getString("display")))?"":ChatColor.stripColor(div)+subserver) + ")";
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
                                for (String server : data.getSection("servers").getKeys()) {
                                    if (i != 0) message += div;
                                    message += ChatColor.WHITE + data.getSection("servers").getSection(server).getString("display") + " (" + ((plugin.config.get().getSection("Settings").getBoolean("Show-Addresses", false))?data.getSection("servers").getSection(server).getString("address"):data.getSection("servers").getSection(server).getString("address").split(":")[data.getSection("servers").getSection(server).getString("address").split(":").length - 1]) + ((server.equals(data.getSection("servers").getSection(server).getString("display")))?"":ChatColor.stripColor(div)+server) + ")";
                                    i++;
                                }
                                if (i == 0) sender.sendMessage("  " + plugin.api.getLang("SubServers", "Command.List.Empty"));
                                else sender.sendMessage(message);
                                if (data.getSection("proxies").getKeys().size() > 0) {
                                    sender.sendMessage(plugin.api.getLang("SubServers", "Command.List.Proxy-Header"));
                                    message = "  (master)";
                                    for (String proxy : data.getSection("proxies").getKeys()) {
                                        message += div;
                                        if (data.getSection("proxies").getSection(proxy).getKeys().contains("subdata") && data.getSection("proxies").getSection(proxy).getBoolean("redis")) {
                                            message += ChatColor.GREEN;
                                        } else if (data.getSection("proxies").getSection(proxy).getKeys().contains("subdata")) {
                                            message += ChatColor.AQUA;
                                        } else if (data.getSection("proxies").getSection(proxy).getBoolean("redis")) {
                                            message += ChatColor.WHITE;
                                        } else {
                                            message += ChatColor.RED;
                                        }
                                        message += data.getSection("proxies").getSection(proxy).getString("display") + ((proxy.equals(data.getSection("proxies").getSection(proxy).getString("display")))?"":" ("+proxy+')');
                                    }
                                    sender.sendMessage(message);
                                }
                            }));
                        }
                    } else if (args[0].equalsIgnoreCase("info") || args[0].equalsIgnoreCase("status")) {
                        if (args.length > 1) {
                            plugin.subdata.sendPacket(new PacketDownloadServerInfo(args[1].toLowerCase(), data -> {
                                switch (data.getString("type").toLowerCase()) {
                                    case "invalid":
                                        sender.sendMessage(plugin.api.getLang("SubServers", "Command.Info.Unknown"));
                                        break;
                                    case "subserver":
                                        sender.sendMessage(plugin.api.getLang("SubServers", "Command.Info").replace("$str$", data.getSection("server").getString("display")));
                                        if (!data.getSection("server").getString("name").equals(data.getSection("server").getString("display")))
                                            sender.sendMessage("  " + plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Real Name") + ChatColor.AQUA + data.getSection("server").getString("name"));
                                        sender.sendMessage("  " + plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Host") + ChatColor.AQUA + data.getSection("server").getString("host"));
                                        sender.sendMessage("  " + plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Enabled") + ((data.getSection("server").getBoolean("enabled"))?ChatColor.GREEN+"yes":ChatColor.DARK_RED+"no"));
                                        sender.sendMessage("  " + plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Editable") + ((data.getSection("server").getBoolean("editable"))?ChatColor.GREEN+"yes":ChatColor.DARK_RED+"no"));
                                        if (data.getSection("server").getList("group").size() > 0) {
                                            sender.sendMessage("  " + plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Group"));
                                            for (int i = 0; i < data.getSection("server").getList("group").size(); i++)
                                                sender.sendMessage("  " + plugin.api.getLang("SubServers", "Command.Info.List").replace("$str$", ChatColor.GOLD + data.getSection("server").getList("group").get(i).asString()));
                                        }
                                        if (data.getSection("server").getBoolean("temp")) sender.sendMessage("  " + plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Temporary") + ChatColor.GREEN+"yes");
                                        sender.sendMessage("  " + plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Running") + ((data.getSection("server").getBoolean("running"))?ChatColor.GREEN+"yes":ChatColor.DARK_RED+"no"));
                                        sender.sendMessage("  " + plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Logging") + ((data.getSection("server").getBoolean("log"))?ChatColor.GREEN+"yes":ChatColor.DARK_RED+"no"));
                                        if (plugin.config.get().getSection("Settings").getBoolean("Show-Addresses", false)) {
                                            sender.sendMessage("  " + plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Address") + ChatColor.AQUA + data.getSection("server").getString("address"));
                                        } else {
                                            sender.sendMessage("  " + plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Port") + ChatColor.AQUA + data.getSection("server").getString("address").split(":")[data.getSection("server").getString("address").split(":").length - 1]);
                                        }
                                        sender.sendMessage("  " + plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Auto Restart") + ((data.getSection("server").getBoolean("auto-restart"))?ChatColor.GREEN+"yes":ChatColor.DARK_RED+"no"));
                                        sender.sendMessage("  " + plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Hidden") + ((data.getSection("server").getBoolean("hidden"))?ChatColor.GREEN+"yes":ChatColor.DARK_RED+"no"));
                                        if (data.getSection("server").getList("incompatible-list").size() > 0) {
                                            List<String> current = new ArrayList<String>();
                                            for (int i = 0; i < data.getSection("server").getList("incompatible").size(); i++) current.add(data.getSection("server").getList("incompatible").get(i).asString().toLowerCase());
                                            sender.sendMessage("  " + plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Incompatibilities"));
                                            for (int i = 0; i < data.getSection("server").getList("incompatible-list").size(); i++)
                                                sender.sendMessage("  " + plugin.api.getLang("SubServers", "Command.Info.List").replace("$str$", ((current.contains(data.getSection("server").getList("incompatible-list").get(i).asString().toLowerCase()))?ChatColor.DARK_RED:ChatColor.RED) + data.getSection("server").getList("incompatible-list").get(i).asString()));
                                        }
                                        sender.sendMessage("  " + plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Signature") + ChatColor.AQUA + data.getSection("server").getString("signature"));
                                        break;
                                    default:
                                        sender.sendMessage(plugin.api.getLang("SubServers", "Command.Info.Invalid"));
                                }
                            }));
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
                                            if (data.getString("m").contains("Host")) {
                                                sender.sendMessage(plugin.api.getLang("SubServers", "Command.Start.Host-Disabled"));
                                            } else {
                                                sender.sendMessage(plugin.api.getLang("SubServers", "Command.Start.Server-Disabled"));
                                            }
                                            break;
                                        case 6:
                                            sender.sendMessage(plugin.api.getLang("SubServers", "Command.Start.Running"));
                                            break;
                                        case 7:
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
                                                sender.sendMessage(plugin.api.getLang("SubServers", "Command.Creator.Exists"));
                                                break;
                                            case 4:
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
            return true;
        }
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
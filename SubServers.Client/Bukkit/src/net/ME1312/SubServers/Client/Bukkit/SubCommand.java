package net.ME1312.SubServers.Client.Bukkit;

import net.ME1312.SubServers.Client.Bukkit.Graphic.UIRenderer;
import net.ME1312.SubServers.Client.Bukkit.Library.Util;
import net.ME1312.SubServers.Client.Bukkit.Library.Version.Version;
import net.ME1312.SubServers.Client.Bukkit.Network.Packet.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.util.*;

public final class SubCommand implements CommandExecutor {
    private SubPlugin plugin;

    public SubCommand(SubPlugin plugin) {
        this.plugin = plugin;
    }

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
                        sender.sendMessage(plugin.lang.getSection("Lang").getColoredString("Command.Version", '&').replace("$str$", plugin.version.toString() + ((plugin.bversion != null)?" BETA "+plugin.bversion.toString():"")));
                    } else if (args[0].equalsIgnoreCase("list")) {
                        plugin.subdata.sendPacket(new PacketDownloadServerList(null, UUID.randomUUID().toString(), json -> {
                            int i = 0;
                            TreeMap<String, JSONObject> servers = new TreeMap<String, JSONObject>();
                            if (Util.isSpigot() && sender instanceof Player) {
                                net.md_5.bungee.api.chat.TextComponent hostm = new net.md_5.bungee.api.chat.TextComponent(ChatColor.RESET.toString());
                                net.md_5.bungee.api.chat.TextComponent serverm = new net.md_5.bungee.api.chat.TextComponent(ChatColor.RESET.toString());
                                net.md_5.bungee.api.chat.TextComponent div = new net.md_5.bungee.api.chat.TextComponent(plugin.lang.getSection("Lang").getColoredString("Command.List.Divider", '&'));
                                for (String server : json.getJSONObject("servers").keySet()) {
                                    servers.put(server, json.getJSONObject("servers").getJSONObject(server));
                                }
                                for (String host : json.getJSONObject("hosts").keySet()) {
                                    List<net.md_5.bungee.api.chat.TextComponent> hoverm = new LinkedList<net.md_5.bungee.api.chat.TextComponent>();
                                    net.md_5.bungee.api.chat.TextComponent message = new net.md_5.bungee.api.chat.TextComponent(host);
                                    net.md_5.bungee.api.chat.TextComponent hover = new net.md_5.bungee.api.chat.TextComponent(host + '\n');
                                    if (json.getJSONObject("hosts").getJSONObject(host).getBoolean("enabled")) {
                                        message.setColor(net.md_5.bungee.api.ChatColor.AQUA);
                                        hover.setColor(net.md_5.bungee.api.ChatColor.AQUA);
                                        hoverm.add(hover);
                                        hover = new net.md_5.bungee.api.chat.TextComponent(plugin.lang.getSection("Lang").getColoredString("Interface.Host-Menu.Host-Server-Count", '&').replace("$int$", new DecimalFormat("#,###").format(json.getJSONObject("hosts").getJSONObject(host).getJSONObject("servers").keySet().size())));
                                    } else {
                                        message.setColor(net.md_5.bungee.api.ChatColor.RED);
                                        hover.setColor(net.md_5.bungee.api.ChatColor.RED);
                                        hoverm.add(hover);
                                        hover = new net.md_5.bungee.api.chat.TextComponent(plugin.lang.getSection("Lang").getColoredString("Interface.Host-Menu.Host-Disabled", '&'));
                                    }
                                    hoverm.add(hover);
                                    if (i != 0) hostm.addExtra(div);
                                    message.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND, "/subservers open Host/ " + host));
                                    message.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT, hoverm.toArray(new net.md_5.bungee.api.chat.TextComponent[hoverm.size()])));
                                    hostm.addExtra(message);
                                    i++;
                                    for (String subserver : json.getJSONObject("hosts").getJSONObject(host).getJSONObject("servers").keySet()) {
                                        servers.put(subserver, json.getJSONObject("hosts").getJSONObject(host).getJSONObject("servers").getJSONObject(subserver));
                                    }
                                }
                                i = 0;
                                for (String server : servers.keySet()) {
                                    List<net.md_5.bungee.api.chat.TextComponent> hoverm = new LinkedList<net.md_5.bungee.api.chat.TextComponent>();
                                    net.md_5.bungee.api.chat.TextComponent message = new net.md_5.bungee.api.chat.TextComponent(server);
                                    net.md_5.bungee.api.chat.TextComponent hover = new net.md_5.bungee.api.chat.TextComponent(server + '\n');
                                    if (!servers.get(server).keySet().contains("enabled")) {
                                        message.setColor(net.md_5.bungee.api.ChatColor.WHITE);
                                        hover.setColor(net.md_5.bungee.api.ChatColor.WHITE);
                                        hoverm.add(hover);
                                        hover = new net.md_5.bungee.api.chat.TextComponent(plugin.lang.getSection("Lang").getColoredString("Interface.SubServer-Menu.SubServer-External", '&'));
                                    } else {
                                        message.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND, "/subservers open SubServer/ " + server));
                                        if (servers.get(server).getBoolean("temp")) {
                                            message.setColor(net.md_5.bungee.api.ChatColor.AQUA);
                                            hover.setColor(net.md_5.bungee.api.ChatColor.AQUA);
                                            hoverm.add(hover);
                                            hover = new net.md_5.bungee.api.chat.TextComponent(plugin.lang.getSection("Lang").getColoredString("Interface.SubServer-Menu.SubServer-Player-Count", '&').replace("$int$", new DecimalFormat("#,###").format(servers.get(server).getJSONObject("players").keySet().size())) + '\n');
                                            hoverm.add(hover);
                                            hover = new net.md_5.bungee.api.chat.TextComponent(plugin.lang.getSection("Lang").getColoredString("Interface.SubServer-Menu.SubServer-Temporary", '&'));
                                        } else if (servers.get(server).getBoolean("running")) {
                                            message.setColor(net.md_5.bungee.api.ChatColor.GREEN);
                                            hover.setColor(net.md_5.bungee.api.ChatColor.GREEN);
                                            hoverm.add(hover);
                                            hover = new net.md_5.bungee.api.chat.TextComponent(plugin.lang.getSection("Lang").getColoredString("Interface.SubServer-Menu.SubServer-Player-Count", '&').replace("$int$", new DecimalFormat("#,###").format(servers.get(server).getJSONObject("players").keySet().size())));
                                        } else if (servers.get(server).getBoolean("enabled")) {
                                            message.setColor(net.md_5.bungee.api.ChatColor.YELLOW);
                                            hover.setColor(net.md_5.bungee.api.ChatColor.YELLOW);
                                            hoverm.add(hover);
                                            hover = new net.md_5.bungee.api.chat.TextComponent(plugin.lang.getSection("Lang").getColoredString("Interface.SubServer-Menu.SubServer-Offline", '&'));
                                        } else {
                                            message.setColor(net.md_5.bungee.api.ChatColor.RED);
                                            hover.setColor(net.md_5.bungee.api.ChatColor.RED);
                                            hoverm.add(hover);
                                            hover = new net.md_5.bungee.api.chat.TextComponent(plugin.lang.getSection("Lang").getColoredString("Interface.SubServer-Menu.SubServer-Disabled", '&'));
                                        }
                                    }
                                    hoverm.add(hover);
                                    if (i != 0) serverm.addExtra(div);
                                    message.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT, hoverm.toArray(new net.md_5.bungee.api.chat.TextComponent[hoverm.size()])));
                                    serverm.addExtra(message);
                                    i++;
                                }
                                sender.sendMessage(plugin.lang.getSection("Lang").getColoredString("Command.List.Host-Header", '&'));
                                ((Player) sender).spigot().sendMessage(hostm);
                                sender.sendMessage(plugin.lang.getSection("Lang").getColoredString("Command.List.Server-Header", '&'));
                                ((Player) sender).spigot().sendMessage(serverm);
                            } else {
                                String hostm = "";
                                String serverm = "";
                                String div = plugin.lang.getSection("Lang").getColoredString("Command.List.Divider", '&');

                                for (String server : json.getJSONObject("servers").keySet()) {
                                    servers.put(server, json.getJSONObject("servers").getJSONObject(server));
                                }
                                for (String host : json.getJSONObject("hosts").keySet()) {
                                    if (i != 0) hostm += div;
                                    if (json.getJSONObject("hosts").getJSONObject(host).getBoolean("enabled")) {
                                        hostm += ChatColor.AQUA + host;
                                    } else {
                                        hostm += ChatColor.RED + host;
                                    }
                                    i++;
                                    for (String subserver : json.getJSONObject("hosts").getJSONObject(host).getJSONObject("servers").keySet()) {
                                        servers.put(subserver, json.getJSONObject("hosts").getJSONObject(host).getJSONObject("servers").getJSONObject(subserver));
                                    }
                                }
                                i = 0;
                                for (String server : servers.keySet()) {
                                    if (i != 0) serverm += div;
                                    if (!servers.get(server).keySet().contains("enabled")) {
                                        serverm += ChatColor.WHITE + server;
                                    } else if (servers.get(server).getBoolean("temp")) {
                                        serverm += ChatColor.AQUA + server;
                                    } else if (servers.get(server).getBoolean("running")) {
                                        serverm += ChatColor.GREEN + server;
                                    } else if (servers.get(server).getBoolean("enabled")) {
                                        serverm += ChatColor.YELLOW + server;
                                    } else {
                                        serverm += ChatColor.RED + server;
                                    }
                                    i++;
                                }
                                sender.sendMessage(new String[]{plugin.lang.getSection("Lang").getColoredString("Command.List.Host-Header", '&'), hostm, plugin.lang.getSection("Lang").getColoredString("Command.List.Server-Header", '&'), serverm});
                            }
                        }));
                    } else if (args[0].equalsIgnoreCase("start")) {
                        if (sender.hasPermission("subservers.subserver.start.*") || sender.hasPermission("subservers.subserver.start." + args[1].toLowerCase())) {
                            if (args.length > 1) {
                                plugin.subdata.sendPacket(new PacketStartServer((sender instanceof Player)?((Player) sender).getUniqueId():null, args[1], UUID.randomUUID().toString(), json -> {
                                    switch (json.getInt("r")) {
                                        case 3:
                                            sender.sendMessage(plugin.lang.getSection("Lang").getColoredString("Command.Start.Unknown", '&'));
                                            break;
                                        case 4:
                                            sender.sendMessage(plugin.lang.getSection("Lang").getColoredString("Command.Start.Invalid", '&'));
                                            break;
                                        case 5:
                                            if (json.getString("m").contains("Host")) {
                                                sender.sendMessage(plugin.lang.getSection("Lang").getColoredString("Command.Start.Host-Disabled", '&'));
                                            } else {
                                                sender.sendMessage(plugin.lang.getSection("Lang").getColoredString("Command.Start.Server-Disabled", '&'));
                                            }
                                            break;
                                        case 6:
                                            sender.sendMessage(plugin.lang.getSection("Lang").getColoredString("Command.Start.Running", '&'));
                                            break;
                                        case 0:
                                        case 1:
                                            sender.sendMessage(plugin.lang.getSection("Lang").getColoredString("Command.Start", '&'));
                                            break;
                                        default:
                                            Bukkit.getLogger().warning("SubData > PacketStartServer(" + ((sender instanceof Player)?((Player) sender).getUniqueId().toString():"null") + ", " + args[1] + ") responded with: " + json.getString("m"));
                                            sender.sendMessage(plugin.lang.getSection("Lang").getColoredString("Command.Start", '&'));
                                            break;
                                    }
                                }));
                            } else {
                                sender.sendMessage(plugin.lang.getSection("Lang").getColoredString("Command.Generic.Usage", '&').replace("$str$", label.toLowerCase() + " " + args[0].toLowerCase() + " <SubServer>"));
                            }
                        } else {
                            sender.sendMessage(plugin.lang.getSection("Lang").getColoredString("Command.Generic.Invalid-Permission", '&').replace("$str$", "subservers.subserver.start." + args[1].toLowerCase()));
                        }

                    } else if (args[0].equalsIgnoreCase("stop")) {
                        if (sender.hasPermission("subservers.subserver.stop.*") || sender.hasPermission("subservers.subserver.stop." + args[1].toLowerCase())) {
                            if (args.length > 1) {
                                plugin.subdata.sendPacket(new PacketStopServer((sender instanceof Player)?((Player) sender).getUniqueId():null, args[1], false, UUID.randomUUID().toString(), json -> {
                                    switch (json.getInt("r")) {
                                        case 3:
                                            sender.sendMessage(plugin.lang.getSection("Lang").getColoredString("Command.Stop.Unknown", '&'));
                                            break;
                                        case 4:
                                            sender.sendMessage(plugin.lang.getSection("Lang").getColoredString("Command.Stop.Invalid", '&'));
                                            break;
                                        case 5:
                                            sender.sendMessage(plugin.lang.getSection("Lang").getColoredString("Command.Stop.Not-Running", '&'));
                                            break;
                                        case 0:
                                        case 1:
                                            sender.sendMessage(plugin.lang.getSection("Lang").getColoredString("Command.Stop", '&'));
                                            break;
                                        default:
                                            Bukkit.getLogger().warning("SubData > PacketStopServer(" + ((sender instanceof Player)?((Player) sender).getUniqueId().toString():"null") + ", " + args[1] + ", false) responded with: " + json.getString("m"));
                                            sender.sendMessage(plugin.lang.getSection("Lang").getColoredString("Command.Stop", '&'));
                                            break;
                                    }
                                }));
                            } else {
                                sender.sendMessage(plugin.lang.getSection("Lang").getColoredString("Command.Generic.Usage", '&').replace("$str$", label.toLowerCase() + " " + args[0].toLowerCase() + " <SubServer>"));
                            }
                        } else {
                            sender.sendMessage(plugin.lang.getSection("Lang").getColoredString("Command.Generic.Invalid-Permission", '&').replace("$str$", "subservers.subserver.stop." + args[1].toLowerCase()));
                        }
                    } else if (args[0].equalsIgnoreCase("kill") || args[0].equalsIgnoreCase("terminate")) {
                        if (sender.hasPermission("subservers.subserver.terminate.*") || sender.hasPermission("subservers.subserver.terminate." + args[1].toLowerCase())) {
                            if (args.length > 1) {
                                plugin.subdata.sendPacket(new PacketStopServer((sender instanceof Player)?((Player) sender).getUniqueId():null, args[1], true, UUID.randomUUID().toString(), json -> {
                                    switch (json.getInt("r")) {
                                        case 3:
                                            sender.sendMessage(plugin.lang.getSection("Lang").getColoredString("Command.Terminate.Unknown", '&'));
                                            break;
                                        case 4:
                                            sender.sendMessage(plugin.lang.getSection("Lang").getColoredString("Command.Terminate.Invalid", '&'));
                                            break;
                                        case 5:
                                            sender.sendMessage(plugin.lang.getSection("Lang").getColoredString("Command.Terminate.Not-Running", '&'));
                                            break;
                                        case 0:
                                        case 1:
                                            sender.sendMessage(plugin.lang.getSection("Lang").getColoredString("Command.Terminate", '&'));
                                            break;
                                        default:
                                            Bukkit.getLogger().warning("SubData > PacketStopServer(" + ((sender instanceof Player)?((Player) sender).getUniqueId().toString():"null") + ", " + args[1] + ", true) responded with: " + json.getString("m"));
                                            sender.sendMessage(plugin.lang.getSection("Lang").getColoredString("Command.Terminate", '&'));
                                            break;
                                    }
                                }));
                            } else {
                                sender.sendMessage(plugin.lang.getSection("Lang").getColoredString("Command.Generic.Usage", '&').replace("$str$", label.toLowerCase() + " " + args[0].toLowerCase() + " <SubServer>"));
                            }
                        } else {
                            sender.sendMessage(plugin.lang.getSection("Lang").getColoredString("Command.Generic.Invalid-Permission", '&').replace("$str$", "subservers.subserver.terminate." + args[1].toLowerCase()));
                        }
                    } else if (args[0].equalsIgnoreCase("cmd") || args[0].equalsIgnoreCase("command")) {
                        if (sender.hasPermission("subservers.subserver.command.*") || sender.hasPermission("subservers.subserver.command." + args[1].toLowerCase())) {
                            if (args.length > 2) {
                                int i = 2;
                                String str = args[2];
                                if (args.length > 3) {
                                    do {
                                        i++;
                                        str = str + " " + args[i];
                                    } while ((i + 1) != args.length);
                                }
                                final String cmd = str;
                                plugin.subdata.sendPacket(new PacketCommandServer((sender instanceof Player)?((Player) sender).getUniqueId():null, args[1], cmd, UUID.randomUUID().toString(), json -> {
                                    switch (json.getInt("r")) {
                                        case 3:
                                            sender.sendMessage(plugin.lang.getSection("Lang").getColoredString("Command.Command.Unknown", '&'));
                                            break;
                                        case 4:
                                            sender.sendMessage(plugin.lang.getSection("Lang").getColoredString("Command.Command.Invalid", '&'));
                                            break;
                                        case 5:
                                            sender.sendMessage(plugin.lang.getSection("Lang").getColoredString("Command.Command.Not-Running", '&'));
                                            break;
                                        case 0:
                                        case 1:
                                            sender.sendMessage(plugin.lang.getSection("Lang").getColoredString("Command.Command", '&'));
                                            break;
                                        default:
                                            Bukkit.getLogger().warning("SubData > PacketCommandServer(" + ((sender instanceof Player)?((Player) sender).getUniqueId().toString():"null") + ", " + args[1] + ", /" + cmd + ") responded with: " + json.getString("m"));
                                            sender.sendMessage(plugin.lang.getSection("Lang").getColoredString("Command.Command", '&'));
                                            break;
                                    }
                                }));
                            } else {
                                sender.sendMessage(plugin.lang.getSection("Lang").getColoredString("Command.Generic.Usage", '&').replace("$str$", label.toLowerCase() + " " + args[0].toLowerCase() + " <SubServer> <Command> [Args...]"));
                            }
                        } else {
                            sender.sendMessage(plugin.lang.getSection("Lang").getColoredString("Command.Generic.Invalid-Permission", '&').replace("$str$", "subservers.subserver.command." + args[1].toLowerCase()));
                        }
                    } else if (args[0].equalsIgnoreCase("create")) {
                        if (sender.hasPermission("subservers.host.create.*") || sender.hasPermission("subservers.host.create." + args[2].toLowerCase())) {
                            if (args.length > 5) {
                                if (Util.isException(() -> PacketCreateServer.ServerType.valueOf(args[3].toUpperCase()))) {
                                    sender.sendMessage(plugin.lang.getSection("Lang").getColoredString("Command.Creator.Invalid-Type", '&'));
                                } else if (Util.isException(() -> Integer.parseInt(args[5]))) {
                                    sender.sendMessage(plugin.lang.getSection("Lang").getColoredString("Command.Creator.Invalid-Port", '&'));
                                } else if (args.length > 6 && Util.isException(() -> Integer.parseInt(args[6]))) {
                                    sender.sendMessage(plugin.lang.getSection("Lang").getColoredString("Command.Creator.Invalid-Memory", '&'));
                                } else {
                                    plugin.subdata.sendPacket(new PacketCreateServer((sender instanceof Player)?((Player) sender).getUniqueId():null, args[1], args[2], PacketCreateServer.ServerType.valueOf(args[3].toUpperCase()), new Version(args[4]), Integer.parseInt(args[5]), (args.length > 6)?Integer.parseInt(args[6]):1024, UUID.randomUUID().toString(), json -> {
                                        switch (json.getInt("r")) {
                                            case 3:
                                                sender.sendMessage(plugin.lang.getSection("Lang").getColoredString("Command.Creator.Exists", '&'));
                                                break;
                                            case 4:
                                                sender.sendMessage(plugin.lang.getSection("Lang").getColoredString("Command.Creator.Unknown-Host", '&'));
                                                break;
                                            case 5:
                                                sender.sendMessage(plugin.lang.getSection("Lang").getColoredString("Command.Creator.Running", '&'));
                                                break;
                                            case 6:
                                                sender.sendMessage(plugin.lang.getSection("Lang").getColoredString("Command.Creator.Invalid-Type", '&'));
                                                break;
                                            case 7:
                                                sender.sendMessage(plugin.lang.getSection("Lang").getColoredString("Command.Creator.Invalid-Version", '&'));
                                                break;
                                            case 8:
                                                sender.sendMessage(plugin.lang.getSection("Lang").getColoredString("Command.Creator.Invalid-Port", '&'));
                                                break;
                                            case 9:
                                                sender.sendMessage(plugin.lang.getSection("Lang").getColoredString("Command.Creator.Invalid-Memory", '&'));
                                                break;
                                            case 0:
                                            case 1:
                                                sender.sendMessage(plugin.lang.getSection("Lang").getColoredString("Command.Creator", '&'));
                                                break;
                                            default:
                                                Bukkit.getLogger().warning("SubData > PacketCreateServer(" + ((sender instanceof Player)?((Player) sender).getUniqueId().toString():"null") + ", " + args[1] + ", " + args[2] + ", " + args[3].toUpperCase() + ", " + args[4] + ", " + args[5] + ", " + ((args.length > 6)?args[6]:"1024") + ") responded with: " + json.getString("m"));
                                                sender.sendMessage(plugin.lang.getSection("Lang").getColoredString("Command.Creator", '&'));
                                                break;
                                        }
                                    }));
                                }
                            } else {
                                sender.sendMessage(plugin.lang.getSection("Lang").getColoredString("Command.Generic.Usage", '&').replace("$str$", label.toLowerCase() + " " + args[0].toLowerCase() + " <Name> <Host> <Type> <Version> <Port> [RAM]"));
                            }
                        } else {
                            sender.sendMessage(plugin.lang.getSection("Lang").getColoredString("Command.Generic.Invalid-Permission", '&').replace("$str$", "subservers.host.create." + args[2].toLowerCase()));
                        }
                    } else if ((args[0].equalsIgnoreCase("view") || args[0].equalsIgnoreCase("open")) && sender instanceof Player) {
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
                                        break;
                                    case "host/plugins":
                                        plugin.gui.getRenderer((Player) sender).hostPlugin(Integer.parseInt(args[2]), args[3]);
                                        break;
                                    case "subserver":
                                        if (args.length > 3) plugin.gui.getRenderer((Player) sender).subserverMenu(Integer.parseInt(args[2]), args[3]);
                                        else if (args.length > 2) plugin.gui.getRenderer((Player) sender).subserverMenu(Integer.parseInt(args[2]), null);
                                        else plugin.gui.getRenderer((Player) sender).subserverMenu(1, null);
                                        break;
                                    case "subserver/":
                                        plugin.gui.getRenderer((Player) sender).subserverAdmin(args[2]);
                                        break;
                                    case "subserver/plugins":
                                        plugin.gui.getRenderer((Player) sender).subserverPlugin(Integer.parseInt(args[2]), args[3]);
                                        break;
                                }
                            } catch (Throwable e) {}
                        } else {
                            sender.sendMessage(plugin.lang.getSection("Lang").getColoredString("Command.Generic.Invalid-Permission", '&').replace("$str$", "subservers.interface"));
                        }
                    } else if (args[0].equalsIgnoreCase("tp") || args[0].equalsIgnoreCase("teleport")) {
                        if (args.length > 2) {
                            if (sender.hasPermission("subservers.server.teleport.*") || sender.hasPermission("subservers.server.teleport." + args[1].toLowerCase())) {
                                if (sender.hasPermission("subservers.server.teleport-others")) {
                                    plugin.subdata.sendPacket(new PacketDownloadPlayerList(UUID.randomUUID().toString(), players -> {
                                        UUID uuid = null;
                                        for (String id : players.getJSONObject("players").keySet()) {
                                            if (players.getJSONObject("players").getJSONObject(id).getString("name").equalsIgnoreCase(args[2]))
                                                uuid = UUID.fromString(id);
                                        }
                                        if (uuid == null) {
                                            sender.sendMessage(plugin.lang.getSection("Lang").getColoredString("Command.Teleport.Offline", '&'));
                                        } else {
                                            final UUID player = uuid;
                                            plugin.subdata.sendPacket(new PacketTeleportPlayer(player, args[1], UUID.randomUUID().toString(), json -> {
                                                switch (json.getInt("r")) {
                                                    case 2:
                                                        sender.sendMessage(plugin.lang.getSection("Lang").getColoredString("Command.Teleport.Invalid", '&'));
                                                        break;
                                                    case 3:
                                                        sender.sendMessage(plugin.lang.getSection("Lang").getColoredString("Command.Teleport.Offline", '&'));
                                                        break;
                                                    case 0:
                                                    case 1:
                                                        sender.sendMessage(plugin.lang.getSection("Lang").getColoredString("Command.Teleport", '&'));
                                                        break;
                                                    default:
                                                        Bukkit.getLogger().warning("SubData > PacketTeleportPlayer(" + player.toString() + ", " + args[1] + ") responded with: " + json.getString("m"));
                                                        sender.sendMessage(plugin.lang.getSection("Lang").getColoredString("Command.Teleport", '&'));
                                                }
                                            }));
                                        }
                                    }));
                                } else {
                                    sender.sendMessage(plugin.lang.getSection("Lang").getColoredString("Command.Generic.Invalid-Permission", '&').replace("$str$", "subservers.server.teleport-others"));
                                }
                            } else {
                                sender.sendMessage(plugin.lang.getSection("Lang").getColoredString("Command.Generic.Invalid-Permission", '&').replace("$str$", "subservers.server.teleport." + args[1].toLowerCase()));
                            }
                        } else if (args.length > 1) {
                            if (sender.hasPermission("subservers.server.teleport.*") || sender.hasPermission("subservers.server.teleport." + args[1].toLowerCase())) {
                                if (sender instanceof Player) {
                                    plugin.subdata.sendPacket(new PacketTeleportPlayer(((Player) sender).getUniqueId(), args[1], UUID.randomUUID().toString(), json -> {
                                        switch (json.getInt("r")) {
                                            case 2:
                                                sender.sendMessage(plugin.lang.getSection("Lang").getColoredString("Command.Teleport.Invalid", '&'));
                                                break;
                                            case 3:
                                                sender.sendMessage(plugin.lang.getSection("Lang").getColoredString("Command.Teleport.Offline", '&'));
                                                break;
                                            case 0:
                                            case 1:
                                                sender.sendMessage(plugin.lang.getSection("Lang").getColoredString("Command.Teleport", '&'));
                                                break;
                                            default:
                                                Bukkit.getLogger().warning("SubData > PacketTeleportPlayer(" + ((Player) sender).getUniqueId().toString() + ", " + args[1] + ") responded with: " + json.getString("m"));
                                                sender.sendMessage(plugin.lang.getSection("Lang").getColoredString("Command.Teleport", '&'));
                                        }
                                    }));
                                } else {
                                    sender.sendMessage(plugin.lang.getSection("Lang").getColoredString("Command.Generic.Player-Only", '&'));
                                }
                            } else {
                                sender.sendMessage(plugin.lang.getSection("Lang").getColoredString("Command.Generic.Invalid-Permission", '&').replace("$str$", "subservers.server.teleport." + args[1].toLowerCase()));
                            }
                        } else {
                            sender.sendMessage(plugin.lang.getSection("Lang").getColoredString("Command.Generic.Usage", '&').replace("$str$", label.toLowerCase() + " " + args[0].toLowerCase() + " <Server> [Player]"));
                        }
                    } else {
                        sender.sendMessage(plugin.lang.getSection("Lang").getColoredString("Command.Generic.Invalid-Subcommand", '&').replace("$str$", args[0]));
                    }
                } else {
                    if (sender.hasPermission("subservers.interface") && sender instanceof Player) {
                        plugin.gui.getRenderer((Player) sender).newUI();
                    } else {
                        sender.sendMessage(printHelp(label));
                    }
                }
            } else if (sender.hasPermission("subservers.interface") && sender instanceof Player) {
                plugin.gui.getRenderer((Player) sender).newUI();
            } else {
                sender.sendMessage(plugin.lang.getSection("Lang").getColoredString("Command.Generic.Invalid-Permission", '&').replace("$str$", "subservers.command"));
            }
            return true;
        }
    }

    private String[] printHelp(String label) {
        return new String[]{
                plugin.lang.getSection("Lang").getColoredString("Command.Help.Header", '&'),
                plugin.lang.getSection("Lang").getColoredString("Command.Help.Help", '&').replace("$str$", label.toLowerCase() + " help"),
                plugin.lang.getSection("Lang").getColoredString("Command.Help.List", '&').replace("$str$", label.toLowerCase() + " list"),
                plugin.lang.getSection("Lang").getColoredString("Command.Help.Version", '&').replace("$str$", label.toLowerCase() + " version"),
                plugin.lang.getSection("Lang").getColoredString("Command.Help.SubServer.Start", '&').replace("$str$", label.toLowerCase() + " start <SubServer>"),
                plugin.lang.getSection("Lang").getColoredString("Command.Help.SubServer.Stop", '&').replace("$str$", label.toLowerCase() + " stop <SubServer>"),
                plugin.lang.getSection("Lang").getColoredString("Command.Help.SubServer.Terminate", '&').replace("$str$", label.toLowerCase() + " kill <SubServer>"),
                plugin.lang.getSection("Lang").getColoredString("Command.Help.SubServer.Command", '&').replace("$str$", label.toLowerCase() + " cmd <SubServer> <Command> [Args...]"),
                plugin.lang.getSection("Lang").getColoredString("Command.Help.Server.Teleport", '&').replace("$str$", label.toLowerCase() + " tp <Server> [Player]"),
                plugin.lang.getSection("Lang").getColoredString("Command.Help.Host.Create", '&').replace("$str$", label.toLowerCase() + " create <Name> <Host> <Type> <Version> <Port> [RAM]"),
        };
    }
}
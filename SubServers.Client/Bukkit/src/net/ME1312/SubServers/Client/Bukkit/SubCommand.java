package net.ME1312.SubServers.Client.Bukkit;

import net.ME1312.SubServers.Client.Bukkit.Graphic.UIRenderer;
import net.ME1312.SubServers.Client.Bukkit.Library.Container;
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
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.nio.charset.Charset;
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
                        sender.sendMessage(plugin.lang.getSection("Lang").getColoredString("Command.Version", '&').replace("$name$", "SubServers.Client.Bukkit").replace("$str$", plugin.version.toString() + ((plugin.bversion != null)?" BETA "+plugin.bversion.toString():"")));
                        if (plugin.bversion == null) {
                            new Thread(() -> {
                                try {
                                    Document updxml = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new StringReader(Util.readAll(new BufferedReader(new InputStreamReader(new URL("http://src.me1312.net/maven/net/ME1312/SubServers/SubServers.Client.Bukkit/maven-metadata.xml").openStream(), Charset.forName("UTF-8")))))));

                                    NodeList updnodeList = updxml.getElementsByTagName("version");
                                    Version updversion = plugin.version;
                                    int updcount = -1;
                                    for (int i = 0; i < updnodeList.getLength(); i++) {
                                        Node node = updnodeList.item(i);
                                        if (node.getNodeType() == Node.ELEMENT_NODE) {
                                            if (!node.getTextContent().startsWith("-") && new Version(node.getTextContent()).compareTo(updversion) >= 0) {
                                                updversion = new Version(node.getTextContent());
                                                updcount++;
                                            }
                                        }
                                    }
                                    if (updversion.equals(plugin.version)) {
                                        sender.sendMessage(plugin.lang.getSection("Lang").getColoredString("Command.Version.Latest", '&'));
                                    } else {
                                        sender.sendMessage(plugin.lang.getSection("Lang").getColoredString("Command.Version.Outdated", '&').replace("$int$", Integer.toString(updcount)));
                                    }
                                } catch (Exception e) {}
                            }).start();
                        }
                    } else if (args[0].equalsIgnoreCase("list")) {
                        final String fLabel = label;
                        plugin.subdata.sendPacket(new PacketDownloadServerList(null, json -> {
                            int i = 0;
                            Container<Boolean> spigot = new Container<Boolean>(false);
                            if (!Util.isException(() -> {
                                if (Class.forName("org.spigotmc.SpigotConfig") != null) spigot.set(true);
                            }) && spigot.get() && sender instanceof Player) {
                                sender.sendMessage(plugin.lang.getSection("Lang").getColoredString("Command.List.Host-Header", '&'));
                                net.md_5.bungee.api.chat.TextComponent div = new net.md_5.bungee.api.chat.TextComponent(plugin.lang.getSection("Lang").getColoredString("Command.List.Divider", '&'));
                                for (String host : json.getJSONObject("hosts").keySet()) {
                                    List<net.md_5.bungee.api.chat.TextComponent> hoverm = new LinkedList<net.md_5.bungee.api.chat.TextComponent>();
                                    net.md_5.bungee.api.chat.TextComponent msg = new net.md_5.bungee.api.chat.TextComponent(ChatColor.RESET.toString());
                                    net.md_5.bungee.api.chat.TextComponent message = new net.md_5.bungee.api.chat.TextComponent(json.getJSONObject("hosts").getJSONObject(host).getString("display"));
                                    net.md_5.bungee.api.chat.TextComponent hover = new net.md_5.bungee.api.chat.TextComponent(json.getJSONObject("hosts").getJSONObject(host).getString("display") + '\n');
                                    if (json.getJSONObject("hosts").getJSONObject(host).getBoolean("enabled")) {
                                        message.setColor(net.md_5.bungee.api.ChatColor.AQUA);
                                        hover.setColor(net.md_5.bungee.api.ChatColor.AQUA);
                                        hoverm.add(hover);
                                        if (!host.equals(json.getJSONObject("hosts").getJSONObject(host).getString("display"))) {
                                            hover = new net.md_5.bungee.api.chat.TextComponent(host + '\n');
                                            hover.setColor(net.md_5.bungee.api.ChatColor.GRAY);
                                            hoverm.add(hover);
                                        }
                                        hover = new net.md_5.bungee.api.chat.TextComponent(plugin.lang.getSection("Lang").getColoredString("Interface.Host-Menu.Host-Server-Count", '&').replace("$int$", new DecimalFormat("#,###").format(json.getJSONObject("hosts").getJSONObject(host).getJSONObject("servers").keySet().size())));
                                        hoverm.add(hover);
                                        hover = new net.md_5.bungee.api.chat.TextComponent(json.getJSONObject("hosts").getJSONObject(host).getString("address"));
                                        hover.setColor(net.md_5.bungee.api.ChatColor.WHITE);
                                    } else {
                                        message.setColor(net.md_5.bungee.api.ChatColor.RED);
                                        hover.setColor(net.md_5.bungee.api.ChatColor.RED);
                                        hoverm.add(hover);
                                        if (!host.equals(json.getJSONObject("hosts").getJSONObject(host).getString("display"))) {
                                            hover = new net.md_5.bungee.api.chat.TextComponent(host + '\n');
                                            hover.setColor(net.md_5.bungee.api.ChatColor.GRAY);
                                            hoverm.add(hover);
                                        }
                                        hover = new net.md_5.bungee.api.chat.TextComponent(plugin.lang.getSection("Lang").getColoredString("Interface.Host-Menu.Host-Disabled", '&'));
                                        hoverm.add(hover);
                                        hover = new net.md_5.bungee.api.chat.TextComponent(json.getJSONObject("hosts").getJSONObject(host).getString("address"));
                                        hover.setColor(net.md_5.bungee.api.ChatColor.WHITE);
                                    }
                                    hoverm.add(hover);
                                    message.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND, fLabel + " open Host/ " + host));
                                    message.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT, hoverm.toArray(new net.md_5.bungee.api.chat.TextComponent[hoverm.size()])));
                                    msg.addExtra(message);
                                    msg.addExtra(new net.md_5.bungee.api.chat.TextComponent(ChatColor.GRAY + ": "));

                                    for (String subserver : json.getJSONObject("hosts").getJSONObject(host).getJSONObject("servers").keySet()) {
                                        hoverm = new LinkedList<net.md_5.bungee.api.chat.TextComponent>();
                                        message = new net.md_5.bungee.api.chat.TextComponent(json.getJSONObject("hosts").getJSONObject(host).getJSONObject("servers").getJSONObject(subserver).getString("display"));
                                        hover = new net.md_5.bungee.api.chat.TextComponent(json.getJSONObject("hosts").getJSONObject(host).getJSONObject("servers").getJSONObject(subserver).getString("display") + '\n');
                                        message.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND, fLabel + " open SubServer/ " + subserver));
                                        if (json.getJSONObject("hosts").getJSONObject(host).getJSONObject("servers").getJSONObject(subserver).getBoolean("temp")) {
                                            message.setColor(net.md_5.bungee.api.ChatColor.AQUA);
                                            hover.setColor(net.md_5.bungee.api.ChatColor.AQUA);
                                            hoverm.add(hover);
                                            if (!subserver.equals(json.getJSONObject("hosts").getJSONObject(host).getJSONObject("servers").getJSONObject(subserver).getString("display"))) {
                                                hover = new net.md_5.bungee.api.chat.TextComponent(subserver + '\n');
                                                hover.setColor(net.md_5.bungee.api.ChatColor.GRAY);
                                                hoverm.add(hover);
                                            }
                                            hover = new net.md_5.bungee.api.chat.TextComponent(plugin.lang.getSection("Lang").getColoredString("Interface.SubServer-Menu.SubServer-Player-Count", '&').replace("$int$", new DecimalFormat("#,###").format(json.getJSONObject("hosts").getJSONObject(host).getJSONObject("servers").getJSONObject(subserver).getJSONObject("players").keySet().size())) + '\n');
                                            hoverm.add(hover);
                                            hover = new net.md_5.bungee.api.chat.TextComponent(plugin.lang.getSection("Lang").getColoredString("Interface.SubServer-Menu.SubServer-Temporary", '&'));
                                            hoverm.add(hover);
                                            hover = new net.md_5.bungee.api.chat.TextComponent('\n' + json.getJSONObject("hosts").getJSONObject(host).getJSONObject("servers").getJSONObject(subserver).getString("address"));
                                            hover.setColor(net.md_5.bungee.api.ChatColor.WHITE);
                                        } else if (json.getJSONObject("hosts").getJSONObject(host).getJSONObject("servers").getJSONObject(subserver).getBoolean("running")) {
                                            message.setColor(net.md_5.bungee.api.ChatColor.GREEN);
                                            hover.setColor(net.md_5.bungee.api.ChatColor.GREEN);
                                            hoverm.add(hover);
                                            if (!subserver.equals(json.getJSONObject("hosts").getJSONObject(host).getJSONObject("servers").getJSONObject(subserver).getString("display"))) {
                                                hover = new net.md_5.bungee.api.chat.TextComponent(subserver + '\n');
                                                hover.setColor(net.md_5.bungee.api.ChatColor.GRAY);
                                                hoverm.add(hover);
                                            }
                                            hover = new net.md_5.bungee.api.chat.TextComponent(plugin.lang.getSection("Lang").getColoredString("Interface.SubServer-Menu.SubServer-Player-Count", '&').replace("$int$", new DecimalFormat("#,###").format(json.getJSONObject("hosts").getJSONObject(host).getJSONObject("servers").getJSONObject(subserver).getJSONObject("players").keySet().size())));
                                            hoverm.add(hover);
                                            hover = new net.md_5.bungee.api.chat.TextComponent('\n' + json.getJSONObject("hosts").getJSONObject(host).getJSONObject("servers").getJSONObject(subserver).getString("address"));
                                            hover.setColor(net.md_5.bungee.api.ChatColor.WHITE);
                                        } else if (json.getJSONObject("hosts").getJSONObject(host).getJSONObject("servers").getJSONObject(subserver).getBoolean("enabled") && json.getJSONObject("hosts").getJSONObject(host).getJSONObject("servers").getJSONObject(subserver).getJSONArray("incompatible").length() == 0) {
                                            message.setColor(net.md_5.bungee.api.ChatColor.YELLOW);
                                            hover.setColor(net.md_5.bungee.api.ChatColor.YELLOW);
                                            hoverm.add(hover);
                                            if (!subserver.equals(json.getJSONObject("hosts").getJSONObject(host).getJSONObject("servers").getJSONObject(subserver).getString("display"))) {
                                                hover = new net.md_5.bungee.api.chat.TextComponent(subserver + '\n');
                                                hover.setColor(net.md_5.bungee.api.ChatColor.GRAY);
                                                hoverm.add(hover);
                                            }
                                            hover = new net.md_5.bungee.api.chat.TextComponent(plugin.lang.getSection("Lang").getColoredString("Interface.SubServer-Menu.SubServer-Offline", '&'));
                                            hoverm.add(hover);
                                            hover = new net.md_5.bungee.api.chat.TextComponent('\n' + json.getJSONObject("hosts").getJSONObject(host).getJSONObject("servers").getJSONObject(subserver).getString("address"));
                                            hover.setColor(net.md_5.bungee.api.ChatColor.WHITE);
                                        } else {
                                            message.setColor(net.md_5.bungee.api.ChatColor.RED);
                                            hover.setColor(net.md_5.bungee.api.ChatColor.RED);
                                            if (!subserver.equals(json.getJSONObject("hosts").getJSONObject(host).getJSONObject("servers").getJSONObject(subserver).getString("display"))) {
                                                hoverm.add(hover);
                                                hover = new net.md_5.bungee.api.chat.TextComponent(subserver + '\n');
                                                hover.setColor(net.md_5.bungee.api.ChatColor.GRAY);
                                            }
                                            if (json.getJSONObject("hosts").getJSONObject(host).getJSONObject("servers").getJSONObject(subserver).getJSONArray("incompatible").length() != 0) {
                                                hoverm.add(hover);
                                                String list = "";
                                                for (int ii = 0; ii < json.getJSONObject("hosts").getJSONObject(host).getJSONObject("servers").getJSONObject(subserver).getJSONArray("incompatible").length(); ii++) {
                                                    if (list.length() != 0) list += ", ";
                                                    list += json.getJSONObject("hosts").getJSONObject(host).getJSONObject("servers").getJSONObject(subserver).getJSONArray("incompatible").getString(ii);
                                                }
                                                hover = new net.md_5.bungee.api.chat.TextComponent(plugin.lang.getSection("Lang").getColoredString("Interface.SubServer-Menu.SubServer-Incompatible", '&').replace("$str$", list));
                                            }
                                            if (!json.getJSONObject("hosts").getJSONObject(host).getJSONObject("servers").getJSONObject(subserver).getBoolean("enabled")) {
                                                hoverm.add(hover);
                                                hover = new net.md_5.bungee.api.chat.TextComponent(plugin.lang.getSection("Lang").getColoredString("Interface.SubServer-Menu.SubServer-Disabled", '&'));
                                            }
                                            hoverm.add(hover);
                                            hover = new net.md_5.bungee.api.chat.TextComponent('\n' + json.getJSONObject("hosts").getJSONObject(host).getJSONObject("servers").getJSONObject(subserver).getString("address"));
                                            hover.setColor(net.md_5.bungee.api.ChatColor.WHITE);
                                        }
                                        hoverm.add(hover);
                                        message.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND, fLabel + " open SubServer/ " + subserver));
                                        message.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT, hoverm.toArray(new net.md_5.bungee.api.chat.TextComponent[hoverm.size()])));
                                        if (i != 0) msg.addExtra(div);
                                        msg.addExtra(message);
                                        i++;
                                    }
                                    ((Player) sender).spigot().sendMessage(msg);
                                    i = 0;
                                }
                                sender.sendMessage(plugin.lang.getSection("Lang").getColoredString("Command.List.Server-Header", '&'));
                                net.md_5.bungee.api.chat.TextComponent msg = new net.md_5.bungee.api.chat.TextComponent(ChatColor.RESET.toString());
                                for (String server : json.getJSONObject("servers").keySet()) {
                                    List<net.md_5.bungee.api.chat.TextComponent> hoverm = new LinkedList<net.md_5.bungee.api.chat.TextComponent>();
                                    net.md_5.bungee.api.chat.TextComponent message = new net.md_5.bungee.api.chat.TextComponent(json.getJSONObject("servers").getJSONObject(server).getString("display"));
                                    net.md_5.bungee.api.chat.TextComponent hover = new net.md_5.bungee.api.chat.TextComponent(json.getJSONObject("servers").getJSONObject(server).getString("display") + '\n');
                                    message.setColor(net.md_5.bungee.api.ChatColor.WHITE);
                                    hover.setColor(net.md_5.bungee.api.ChatColor.WHITE);
                                    hoverm.add(hover);
                                    hover = new net.md_5.bungee.api.chat.TextComponent(plugin.lang.getSection("Lang").getColoredString("Interface.SubServer-Menu.SubServer-External", '&'));
                                    hoverm.add(hover);
                                    hover = new net.md_5.bungee.api.chat.TextComponent('\n' + json.getJSONObject("servers").getJSONObject(server).getString("address"));
                                    hover.setColor(net.md_5.bungee.api.ChatColor.WHITE);
                                    hoverm.add(hover);
                                    message.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT, hoverm.toArray(new net.md_5.bungee.api.chat.TextComponent[hoverm.size()])));
                                    if (i != 0) msg.addExtra(div);
                                    msg.addExtra(message);
                                    i++;
                                }
                                ((Player) sender).spigot().sendMessage(msg);
                            } else {
                                sender.sendMessage(plugin.lang.getSection("Lang").getColoredString("Command.List.Host-Header", '&'));
                                String div = plugin.lang.getSection("Lang").getColoredString("Command.List.Divider", '&');

                                for (String host : json.getJSONObject("hosts").keySet()) {
                                    String message = "";
                                    if (json.getJSONObject("hosts").getJSONObject(host).getBoolean("enabled")) {
                                        message += ChatColor.AQUA;
                                    } else {
                                        message += ChatColor.RED;
                                    }
                                    message += json.getJSONObject("hosts").getJSONObject(host).getString("display") + " (" + json.getJSONObject("hosts").getJSONObject(host).getString("address") + ((host.equals(json.getJSONObject("hosts").getJSONObject(host).getString("display")))?"":ChatColor.stripColor(div)+host) + ")" + ChatColor.GRAY + ": ";
                                    for (String subserver : json.getJSONObject("hosts").getJSONObject(host).getJSONObject("servers").keySet()) {
                                        if (i != 0) message += div;
                                        if (json.getJSONObject("hosts").getJSONObject(host).getJSONObject("servers").getJSONObject(subserver).getBoolean("temp")) {
                                            message += ChatColor.AQUA;
                                        } else if (json.getJSONObject("hosts").getJSONObject(host).getJSONObject("servers").getJSONObject(subserver).getBoolean("running")) {
                                            message += ChatColor.GREEN;
                                        } else if (json.getJSONObject("hosts").getJSONObject(host).getJSONObject("servers").getJSONObject(subserver).getBoolean("enabled") && json.getJSONObject("hosts").getJSONObject(host).getJSONObject("servers").getJSONObject(subserver).getJSONArray("incompatible").length() == 0) {
                                            message += ChatColor.YELLOW;
                                        } else {
                                            message += ChatColor.RED;
                                        }
                                        message += json.getJSONObject("hosts").getJSONObject(host).getJSONObject("servers").getJSONObject(subserver).getString("display") + " (" + json.getJSONObject("hosts").getJSONObject(host).getJSONObject("servers").getJSONObject(subserver).getString("address").split(":")[json.getJSONObject("hosts").getJSONObject(host).getJSONObject("servers").getJSONObject(subserver).getString("address").split(":").length - 1] + ((subserver.equals(json.getJSONObject("hosts").getJSONObject(host).getJSONObject("servers").getJSONObject(subserver).getString("display")))?"":ChatColor.stripColor(div)+subserver) + ")";
                                        i++;
                                    }
                                    sender.sendMessage(message);
                                    i = 0;
                                }
                                sender.sendMessage(plugin.lang.getSection("Lang").getColoredString("Command.List.Server-Header", '&'));
                                String message = "";
                                for (String server : json.getJSONObject("servers").keySet()) {
                                    if (i != 0) message += div;
                                    message += ChatColor.WHITE + json.getJSONObject("servers").getJSONObject(server).getString("display") + " (" + json.getJSONObject("servers").getJSONObject(server).getString("address") + ((server.equals(json.getJSONObject("servers").getJSONObject(server).getString("display")))?"":ChatColor.stripColor(div)+server) + ")";
                                    i++;
                                }
                                sender.sendMessage(message);
                            }
                        }));
                    } else if (args[0].equalsIgnoreCase("info") || args[0].equalsIgnoreCase("status")) {
                        if (args.length > 1) {
                            plugin.subdata.sendPacket(new PacketDownloadServerInfo(args[1].toLowerCase(), json -> {
                                switch (json.getString("type").toLowerCase()) {
                                    case "invalid":
                                        sender.sendMessage(plugin.lang.getSection("Lang").getColoredString("Command.Info.Unknown", '&'));
                                        break;
                                    case "subserver":
                                        sender.sendMessage(plugin.lang.getSection("Lang").getColoredString("Command.Info", '&').replace("$str$", json.getJSONObject("server").getString("display")));
                                        if (!json.getJSONObject("server").getString("name").equals(json.getJSONObject("server").getString("display")))
                                            sender.sendMessage("  " + plugin.lang.getSection("Lang").getColoredString("Command.Info.Format", '&').replace("$str$", "Real Name") + ChatColor.AQUA + json.getJSONObject("server").getString("name"));
                                        sender.sendMessage("  " + plugin.lang.getSection("Lang").getColoredString("Command.Info.Format", '&').replace("$str$", "Host") + ChatColor.AQUA + json.getJSONObject("server").getString("host"));
                                        sender.sendMessage("  " + plugin.lang.getSection("Lang").getColoredString("Command.Info.Format", '&').replace("$str$", "Enabled") + ((json.getJSONObject("server").getBoolean("enabled"))?ChatColor.GREEN+"yes":ChatColor.DARK_RED+"no"));
                                        if (json.getJSONObject("server").getBoolean("temp")) sender.sendMessage("  " + plugin.lang.getSection("Lang").getColoredString("Command.Info.Format", '&').replace("$str$", "Temporary") + ChatColor.GREEN+"yes");
                                        sender.sendMessage("  " + plugin.lang.getSection("Lang").getColoredString("Command.Info.Format", '&').replace("$str$", "Running") + ((json.getJSONObject("server").getBoolean("running"))?ChatColor.GREEN+"yes":ChatColor.DARK_RED+"no"));
                                        sender.sendMessage("  " + plugin.lang.getSection("Lang").getColoredString("Command.Info.Format", '&').replace("$str$", "Logging") + ((json.getJSONObject("server").getBoolean("log"))?ChatColor.GREEN+"yes":ChatColor.DARK_RED+"no"));
                                        sender.sendMessage("  " + plugin.lang.getSection("Lang").getColoredString("Command.Info.Format", '&').replace("$str$", "Address") + ChatColor.AQUA + json.getJSONObject("server").getString("address"));
                                        sender.sendMessage("  " + plugin.lang.getSection("Lang").getColoredString("Command.Info.Format", '&').replace("$str$", "Auto Restart") + ((json.getJSONObject("server").getBoolean("auto-restart"))?ChatColor.GREEN+"yes":ChatColor.DARK_RED+"no"));
                                        sender.sendMessage("  " + plugin.lang.getSection("Lang").getColoredString("Command.Info.Format", '&').replace("$str$", "Hidden") + ((json.getJSONObject("server").getBoolean("hidden"))?ChatColor.GREEN+"yes":ChatColor.DARK_RED+"no"));
                                        if (json.getJSONObject("server").getJSONArray("incompatible-list").length() > 0) {
                                            List<String> current = new ArrayList<String>();
                                            for (int i = 0; i < json.getJSONObject("server").getJSONArray("incompatible").length(); i++) current.add(json.getJSONObject("server").getJSONArray("incompatible").getString(i).toLowerCase());
                                            sender.sendMessage("  " + plugin.lang.getSection("Lang").getColoredString("Command.Info.Format", '&').replace("$str$", "Incompatibilities"));
                                            for (int i = 0; i < json.getJSONObject("server").getJSONArray("incompatible-list").length(); i++)
                                                sender.sendMessage("  " + plugin.lang.getSection("Lang").getColoredString("Command.Info.List", '&').replace("$str$", ((current.contains(json.getJSONObject("server").getJSONArray("incompatible-list").getString(i).toLowerCase()))?ChatColor.DARK_RED:ChatColor.RED) + json.getJSONObject("server").getJSONArray("incompatible-list").getString(i)));
                                        }
                                        break;
                                    default:
                                        sender.sendMessage(plugin.lang.getSection("Lang").getColoredString("Command.Start.Invalid", '&'));
                                }
                            }));
                        } else {
                            sender.sendMessage(plugin.lang.getSection("Lang").getColoredString("Command.Generic.Usage", '&').replace("$str$", label.toLowerCase() + " " + args[0].toLowerCase() + " <SubServer>"));
                        }
                    } else if (args[0].equalsIgnoreCase("start")) {
                        if (sender.hasPermission("subservers.subserver.start.*") || sender.hasPermission("subservers.subserver.start." + args[1].toLowerCase())) {
                            if (args.length > 1) {
                                plugin.subdata.sendPacket(new PacketStartServer((sender instanceof Player)?((Player) sender).getUniqueId():null, args[1], json -> {
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
                                        case 7:
                                            sender.sendMessage(plugin.lang.getSection("Lang").getColoredString("Command.Start.Server-Incompatible", '&').replace("$str$", json.getString("m").split(":\\s")[1]));
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
                                plugin.subdata.sendPacket(new PacketStopServer((sender instanceof Player)?((Player) sender).getUniqueId():null, args[1], false, json -> {
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
                                plugin.subdata.sendPacket(new PacketStopServer((sender instanceof Player)?((Player) sender).getUniqueId():null, args[1], true, json -> {
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
                                plugin.subdata.sendPacket(new PacketCommandServer((sender instanceof Player)?((Player) sender).getUniqueId():null, args[1], cmd, json -> {
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
                                if (Util.isException(() -> Integer.parseInt(args[5]))) {
                                    sender.sendMessage(plugin.lang.getSection("Lang").getColoredString("Command.Creator.Invalid-Port", '&'));
                                } else {
                                    plugin.subdata.sendPacket(new PacketCreateServer((sender instanceof Player)?((Player) sender).getUniqueId():null, args[1], args[2], args[3], new Version(args[4]), Integer.parseInt(args[5]), json -> {
                                        switch (json.getInt("r")) {
                                            case 3:
                                                sender.sendMessage(plugin.lang.getSection("Lang").getColoredString("Command.Creator.Exists", '&'));
                                                break;
                                            case 4:
                                                sender.sendMessage(plugin.lang.getSection("Lang").getColoredString("Command.Creator.Unknown-Host", '&'));
                                                break;
                                            case 6:
                                                sender.sendMessage(plugin.lang.getSection("Lang").getColoredString("Command.Creator.Invalid-Template", '&'));
                                                break;
                                            case 7:
                                                sender.sendMessage(plugin.lang.getSection("Lang").getColoredString("Command.Creator.Invalid-Version", '&'));
                                                break;
                                            case 8:
                                                sender.sendMessage(plugin.lang.getSection("Lang").getColoredString("Command.Creator.Invalid-Port", '&'));
                                                break;
                                            case 0:
                                            case 1:
                                                sender.sendMessage(plugin.lang.getSection("Lang").getColoredString("Command.Creator", '&'));
                                                break;
                                            default:
                                                Bukkit.getLogger().warning("SubData > PacketCreateServer(" + ((sender instanceof Player)?((Player) sender).getUniqueId().toString():"null") + ", " + args[1] + ", " + args[2] + ", " + args[3] + ", " + args[4] + ", " + args[5] + ") responded with: " + json.getString("m"));
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
                                        if (args.length > 3) plugin.gui.getRenderer((Player) sender).hostPlugin(Integer.parseInt(args[3]), args[2]);
                                        else plugin.gui.getRenderer((Player) sender).hostPlugin(1, args[2]);
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
                                        if (args.length > 3) plugin.gui.getRenderer((Player) sender).subserverPlugin(Integer.parseInt(args[3]), args[2]);
                                        else plugin.gui.getRenderer((Player) sender).subserverPlugin(1, args[2]);
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
                                    plugin.subdata.sendPacket(new PacketDownloadPlayerList(players -> {
                                        UUID uuid = null;
                                        for (String id : players.getJSONObject("players").keySet()) {
                                            if (players.getJSONObject("players").getJSONObject(id).getString("name").equalsIgnoreCase(args[2]))
                                                uuid = UUID.fromString(id);
                                        }
                                        if (uuid == null) {
                                            sender.sendMessage(plugin.lang.getSection("Lang").getColoredString("Command.Teleport.Offline", '&'));
                                        } else {
                                            final UUID player = uuid;
                                            plugin.subdata.sendPacket(new PacketTeleportPlayer(player, args[1], json -> {
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
                                    plugin.subdata.sendPacket(new PacketTeleportPlayer(((Player) sender).getUniqueId(), args[1], json -> {
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
                plugin.lang.getSection("Lang").getColoredString("Command.Help.Info", '&').replace("$str$", label.toLowerCase() + " info <SubServer>"),
                plugin.lang.getSection("Lang").getColoredString("Command.Help.SubServer.Start", '&').replace("$str$", label.toLowerCase() + " start <SubServer>"),
                plugin.lang.getSection("Lang").getColoredString("Command.Help.SubServer.Stop", '&').replace("$str$", label.toLowerCase() + " stop <SubServer>"),
                plugin.lang.getSection("Lang").getColoredString("Command.Help.SubServer.Terminate", '&').replace("$str$", label.toLowerCase() + " kill <SubServer>"),
                plugin.lang.getSection("Lang").getColoredString("Command.Help.SubServer.Command", '&').replace("$str$", label.toLowerCase() + " cmd <SubServer> <Command> [Args...]"),
                plugin.lang.getSection("Lang").getColoredString("Command.Help.Server.Teleport", '&').replace("$str$", label.toLowerCase() + " tp <Server> [Player]"),
                plugin.lang.getSection("Lang").getColoredString("Command.Help.Host.Create", '&').replace("$str$", label.toLowerCase() + " create <Name> <Host> <Template> <Version> <Port>"),
        };
    }
}
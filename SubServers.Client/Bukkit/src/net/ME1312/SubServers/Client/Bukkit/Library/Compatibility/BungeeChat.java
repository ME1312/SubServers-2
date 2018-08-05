package net.ME1312.SubServers.Client.Bukkit.Library.Compatibility;

import net.ME1312.SubServers.Client.Bukkit.Library.Config.YAMLSection;
import net.ME1312.SubServers.Client.Bukkit.Network.API.Proxy;
import net.ME1312.SubServers.Client.Bukkit.Network.Packet.PacketDownloadServerList;
import net.ME1312.SubServers.Client.Bukkit.SubPlugin;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.text.DecimalFormat;
import java.util.LinkedList;
import java.util.List;

public class BungeeChat {
    private SubPlugin plugin;

    public BungeeChat(SubPlugin plugin) {
        this.plugin = plugin;
    }

    public void listCommand(CommandSender sender, String label) {
        plugin.subdata.sendPacket(new PacketDownloadServerList(null, null, data -> {
            int i = 0;
            boolean sent = false;
            TextComponent div = new TextComponent(plugin.api.getLang("SubServers", "Command.List.Divider"));
            if (data.getSection("groups").getKeys().size() > 0) {
                sender.sendMessage(plugin.api.getLang("SubServers", "Command.List.Group-Header"));
                for (String group : data.getSection("groups").getKeys()) {
                    List<TextComponent> hoverm = new LinkedList<TextComponent>();
                    TextComponent msg = new TextComponent("  ");
                    TextComponent message = new TextComponent(group);
                    TextComponent hover = new TextComponent(group + '\n');
                    message.setColor(ChatColor.GOLD);
                    hover.setColor(ChatColor.GOLD);
                    hoverm.add(hover);
                    hover = new TextComponent(plugin.api.getLang("SubServers", "Interface.Group-Menu.Group-Server-Count").replace("$int$", new DecimalFormat("#,###").format(data.getSection("groups").getSection(group).getKeys().size())));
                    hoverm.add(hover);
                    message.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, label + " open Server 1 " + group));
                    message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverm.toArray(new TextComponent[hoverm.size()])));
                    msg.addExtra(message);
                    msg.addExtra(new TextComponent(plugin.api.getLang("SubServers", "Command.List.Header")));

                    for (String server : data.getSection("groups").getSection(group).getKeys()) {
                        hoverm = new LinkedList<TextComponent>();
                        message = new TextComponent(data.getSection("groups").getSection(group).getSection(server).getString("display"));
                        hover = new TextComponent(data.getSection("groups").getSection(group).getSection(server).getString("display") + '\n');
                        if (data.getSection("groups").getSection(group).getSection(server).getKeys().contains("enabled")) {
                            message.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, label + " open SubServer/ " + server));
                            if (data.getSection("groups").getSection(group).getSection(server).getBoolean("temp")) {
                                message.setColor(ChatColor.AQUA);
                                hover.setColor(ChatColor.AQUA);
                                hoverm.add(hover);
                                if (!server.equals(data.getSection("groups").getSection(group).getSection(server).getString("display"))) {
                                    hover = new TextComponent(server + '\n');
                                    hover.setColor(ChatColor.GRAY);
                                    hoverm.add(hover);
                                }
                                hover = new TextComponent(plugin.api.getLang("SubServers", "Interface.Server-Menu.Server-Player-Count").replace("$int$", new DecimalFormat("#,###").format(data.getSection("groups").getSection(group).getSection(server).getSection("players").getKeys().size())) + '\n');
                                hoverm.add(hover);
                                hover = new TextComponent(plugin.api.getLang("SubServers", "Interface.Server-Menu.SubServer-Temporary"));
                            } else if (data.getSection("groups").getSection(group).getSection(server).getBoolean("running")) {
                                message.setColor(ChatColor.GREEN);
                                hover.setColor(ChatColor.GREEN);
                                hoverm.add(hover);
                                if (!server.equals(data.getSection("groups").getSection(group).getSection(server).getString("display"))) {
                                    hover = new TextComponent(server + '\n');
                                    hover.setColor(ChatColor.GRAY);
                                    hoverm.add(hover);
                                }
                                hover = new TextComponent(plugin.api.getLang("SubServers", "Interface.Server-Menu.Server-Player-Count").replace("$int$", new DecimalFormat("#,###").format(data.getSection("groups").getSection(group).getSection(server).getSection("players").getKeys().size())));
                            } else if (data.getSection("groups").getSection(group).getSection(server).getBoolean("enabled") && data.getSection("groups").getSection(group).getSection(server).getList("incompatible").size() == 0) {
                                message.setColor(ChatColor.YELLOW);
                                hover.setColor(ChatColor.YELLOW);
                                hoverm.add(hover);
                                if (!server.equals(data.getSection("groups").getSection(group).getSection(server).getString("display"))) {
                                    hover = new TextComponent(server + '\n');
                                    hover.setColor(ChatColor.GRAY);
                                    hoverm.add(hover);
                                }
                                hover = new TextComponent(plugin.api.getLang("SubServers", "Interface.Server-Menu.SubServer-Offline"));
                            } else {
                                message.setColor(ChatColor.RED);
                                hover.setColor(ChatColor.RED);
                                if (!server.equals(data.getSection("groups").getSection(group).getSection(server).getString("display"))) {
                                    hoverm.add(hover);
                                    hover = new TextComponent(server + '\n');
                                    hover.setColor(ChatColor.GRAY);
                                }
                                if (data.getSection("groups").getSection(group).getSection(server).getList("incompatible").size() != 0) {
                                    hoverm.add(hover);
                                    String list = "";
                                    for (int ii = 0; ii < data.getSection("groups").getSection(group).getSection(server).getList("incompatible").size(); ii++) {
                                        if (list.length() != 0) list += ", ";
                                        list += data.getSection("groups").getSection(group).getSection(server).getList("incompatible").get(ii).asString();
                                    }
                                    hover = new TextComponent(plugin.api.getLang("SubServers", "Interface.Server-Menu.SubServer-Incompatible").replace("$str$", list) + ((data.getSection("groups").getSection(group).getSection(server).getBoolean("enabled")) ? "" : "\n"));
                                }
                                if (!data.getSection("groups").getSection(group).getSection(server).getBoolean("enabled")) {
                                    hoverm.add(hover);
                                    hover = new TextComponent(plugin.api.getLang("SubServers", "Interface.Server-Menu.SubServer-Disabled"));
                                }
                            }
                            hoverm.add(hover);
                            if (plugin.config.get().getSection("Settings").getBoolean("Show-Addresses", false)) {
                                hover = new TextComponent('\n' + data.getSection("groups").getSection(group).getSection(server).getString("address"));
                            } else {
                                hover = new TextComponent('\n' + data.getSection("groups").getSection(group).getSection(server).getString("address").split(":")[data.getSection("groups").getSection(group).getSection(server).getString("address").split(":").length - 1]);
                            }
                            hover.setColor(ChatColor.WHITE);
                            hoverm.add(hover);
                            message.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, label + " open SubServer/ " + server));
                        } else {
                            message.setColor(ChatColor.WHITE);
                            hover.setColor(ChatColor.WHITE);
                            hoverm.add(hover);
                            hover = new TextComponent(plugin.api.getLang("SubServers", "Interface.Server-Menu.Server-External"));
                            hoverm.add(hover);
                            if (plugin.config.get().getSection("Settings").getBoolean("Show-Addresses", false)) {
                                hover = new TextComponent('\n' + data.getSection("servers").getSection(server).getString("address"));
                            } else {
                                hover = new TextComponent('\n' + data.getSection("servers").getSection(server).getString("address").split(":")[data.getSection("servers").getSection(server).getString("address").split(":").length - 1]);
                            }
                            hover.setColor(ChatColor.WHITE);
                            hoverm.add(hover);
                        }
                        message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverm.toArray(new TextComponent[hoverm.size()])));
                        if (i != 0) msg.addExtra(div);
                        msg.addExtra(message);
                        i++;
                    }
                    if (i == 0) msg.addExtra(new TextComponent(plugin.api.getLang("SubServers", "Command.List.Empty")));
                    ((Player) sender).spigot().sendMessage(msg);
                    i = 0;
                    sent = true;
                }
                if (!sent) sender.sendMessage("  " + plugin.api.getLang("SubServers", "Command.List.Empty"));
                sent = false;
            }
            sender.sendMessage(plugin.api.getLang("SubServers", "Command.List.Host-Header"));
            for (String host : data.getSection("hosts").getKeys()) {
                List<TextComponent> hoverm = new LinkedList<TextComponent>();
                TextComponent msg = new TextComponent("  ");
                TextComponent message = new TextComponent(data.getSection("hosts").getSection(host).getString("display"));
                TextComponent hover = new TextComponent(data.getSection("hosts").getSection(host).getString("display") + '\n');
                if (data.getSection("hosts").getSection(host).getBoolean("enabled")) {
                    message.setColor(ChatColor.AQUA);
                    hover.setColor(ChatColor.AQUA);
                    hoverm.add(hover);
                    if (!host.equals(data.getSection("hosts").getSection(host).getString("display"))) {
                        hover = new TextComponent(host + '\n');
                        hover.setColor(ChatColor.GRAY);
                        hoverm.add(hover);
                    }
                    hover = new TextComponent(plugin.api.getLang("SubServers", "Interface.Host-Menu.Host-Server-Count").replace("$int$", new DecimalFormat("#,###").format(data.getSection("hosts").getSection(host).getSection("servers").getKeys().size())));
                } else {
                    message.setColor(ChatColor.RED);
                    hover.setColor(ChatColor.RED);
                    hoverm.add(hover);
                    if (!host.equals(data.getSection("hosts").getSection(host).getString("display"))) {
                        hover = new TextComponent(host + '\n');
                        hover.setColor(ChatColor.GRAY);
                        hoverm.add(hover);
                    }
                    hover = new TextComponent(plugin.api.getLang("SubServers", "Interface.Host-Menu.Host-Disabled"));
                }
                if (plugin.config.get().getSection("Settings").getBoolean("Show-Addresses", false)) {
                    hoverm.add(hover);
                    hover = new TextComponent('\n' + data.getSection("hosts").getSection(host).getString("address"));
                    hover.setColor(ChatColor.WHITE);
                }
                hoverm.add(hover);
                message.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, label + " open Host/ " + host));
                message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverm.toArray(new TextComponent[hoverm.size()])));
                msg.addExtra(message);
                msg.addExtra(new TextComponent(plugin.api.getLang("SubServers", "Command.List.Header")));

                for (String subserver : data.getSection("hosts").getSection(host).getSection("servers").getKeys()) {
                    hoverm = new LinkedList<TextComponent>();
                    message = new TextComponent(data.getSection("hosts").getSection(host).getSection("servers").getSection(subserver).getString("display"));
                    hover = new TextComponent(data.getSection("hosts").getSection(host).getSection("servers").getSection(subserver).getString("display") + '\n');
                    message.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, label + " open SubServer/ " + subserver));
                    if (data.getSection("hosts").getSection(host).getSection("servers").getSection(subserver).getBoolean("temp")) {
                        message.setColor(ChatColor.AQUA);
                        hover.setColor(ChatColor.AQUA);
                        hoverm.add(hover);
                        if (!subserver.equals(data.getSection("hosts").getSection(host).getSection("servers").getSection(subserver).getString("display"))) {
                            hover = new TextComponent(subserver + '\n');
                            hover.setColor(ChatColor.GRAY);
                            hoverm.add(hover);
                        }
                        hover = new TextComponent(plugin.api.getLang("SubServers", "Interface.Server-Menu.Server-Player-Count").replace("$int$", new DecimalFormat("#,###").format(data.getSection("hosts").getSection(host).getSection("servers").getSection(subserver).getSection("players").getKeys().size())) + '\n');
                        hoverm.add(hover);
                        hover = new TextComponent(plugin.api.getLang("SubServers", "Interface.Server-Menu.SubServer-Temporary"));
                    } else if (data.getSection("hosts").getSection(host).getSection("servers").getSection(subserver).getBoolean("running")) {
                        message.setColor(ChatColor.GREEN);
                        hover.setColor(ChatColor.GREEN);
                        hoverm.add(hover);
                        if (!subserver.equals(data.getSection("hosts").getSection(host).getSection("servers").getSection(subserver).getString("display"))) {
                            hover = new TextComponent(subserver + '\n');
                            hover.setColor(ChatColor.GRAY);
                            hoverm.add(hover);
                        }
                        hover = new TextComponent(plugin.api.getLang("SubServers", "Interface.Server-Menu.Server-Player-Count").replace("$int$", new DecimalFormat("#,###").format(data.getSection("hosts").getSection(host).getSection("servers").getSection(subserver).getSection("players").getKeys().size())));
                    } else if (data.getSection("hosts").getSection(host).getSection("servers").getSection(subserver).getBoolean("enabled") && data.getSection("hosts").getSection(host).getSection("servers").getSection(subserver).getList("incompatible").size() == 0) {
                        message.setColor(ChatColor.YELLOW);
                        hover.setColor(ChatColor.YELLOW);
                        hoverm.add(hover);
                        if (!subserver.equals(data.getSection("hosts").getSection(host).getSection("servers").getSection(subserver).getString("display"))) {
                            hover = new TextComponent(subserver + '\n');
                            hover.setColor(ChatColor.GRAY);
                            hoverm.add(hover);
                        }
                        hover = new TextComponent(plugin.api.getLang("SubServers", "Interface.Server-Menu.SubServer-Offline"));
                    } else {
                        message.setColor(ChatColor.RED);
                        hover.setColor(ChatColor.RED);
                        if (!subserver.equals(data.getSection("hosts").getSection(host).getSection("servers").getSection(subserver).getString("display"))) {
                            hoverm.add(hover);
                            hover = new TextComponent(subserver + '\n');
                            hover.setColor(ChatColor.GRAY);
                        }
                        if (data.getSection("hosts").getSection(host).getSection("servers").getSection(subserver).getList("incompatible").size() != 0) {
                            hoverm.add(hover);
                            String list = "";
                            for (int ii = 0; ii < data.getSection("hosts").getSection(host).getSection("servers").getSection(subserver).getList("incompatible").size(); ii++) {
                                if (list.length() != 0) list += ", ";
                                list += data.getSection("hosts").getSection(host).getSection("servers").getSection(subserver).getList("incompatible").get(ii).asString();
                            }
                            hover = new TextComponent(plugin.api.getLang("SubServers", "Interface.Server-Menu.SubServer-Incompatible").replace("$str$", list) + ((data.getSection("hosts").getSection(host).getSection("servers").getSection(subserver).getBoolean("enabled")) ? "" : "\n"));
                        }
                        if (!data.getSection("hosts").getSection(host).getSection("servers").getSection(subserver).getBoolean("enabled")) {
                            hoverm.add(hover);
                            hover = new TextComponent(plugin.api.getLang("SubServers", "Interface.Server-Menu.SubServer-Disabled"));
                        }
                    }
                    hoverm.add(hover);
                    if (plugin.config.get().getSection("Settings").getBoolean("Show-Addresses", false)) {
                        hover = new TextComponent('\n' + data.getSection("hosts").getSection(host).getSection("servers").getSection(subserver).getString("address"));
                    } else {
                        hover = new TextComponent('\n' + data.getSection("hosts").getSection(host).getSection("servers").getSection(subserver).getString("address").split(":")[data.getSection("hosts").getSection(host).getSection("servers").getSection(subserver).getString("address").split(":").length - 1]);
                    }
                    hover.setColor(ChatColor.WHITE);
                    hoverm.add(hover);
                    message.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, label + " open SubServer/ " + subserver));
                    message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverm.toArray(new TextComponent[hoverm.size()])));
                    if (i != 0) msg.addExtra(div);
                    msg.addExtra(message);
                    i++;
                }
                if (i == 0) msg.addExtra(new TextComponent(plugin.api.getLang("SubServers", "Command.List.Empty")));
                ((Player) sender).spigot().sendMessage(msg);
                i = 0;
                sent = true;
            }
            if (!sent) sender.sendMessage("  " + plugin.api.getLang("SubServers", "Command.List.Empty"));
            sender.sendMessage(plugin.api.getLang("SubServers", "Command.List.Server-Header"));
            TextComponent msg = new TextComponent("  ");
            for (String server : data.getSection("servers").getKeys()) {
                List<TextComponent> hoverm = new LinkedList<TextComponent>();
                TextComponent message = new TextComponent(data.getSection("servers").getSection(server).getString("display"));
                TextComponent hover = new TextComponent(data.getSection("servers").getSection(server).getString("display") + '\n');
                message.setColor(ChatColor.WHITE);
                hover.setColor(ChatColor.WHITE);
                hoverm.add(hover);
                if (!server.equals(data.getSection("servers").getSection(server).getString("display"))) {
                    hover = new TextComponent(server + '\n');
                    hover.setColor(ChatColor.GRAY);
                    hoverm.add(hover);
                }
                hover = new TextComponent(plugin.api.getLang("SubServers", "Interface.Server-Menu.Server-External"));
                hoverm.add(hover);
                if (plugin.config.get().getSection("Settings").getBoolean("Show-Addresses", false)) {
                    hover = new TextComponent('\n' + data.getSection("servers").getSection(server).getString("address"));
                } else {
                    hover = new TextComponent('\n' + data.getSection("servers").getSection(server).getString("address").split(":")[data.getSection("servers").getSection(server).getString("address").split(":").length - 1]);
                }
                hover.setColor(ChatColor.WHITE);
                hoverm.add(hover);
                message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverm.toArray(new TextComponent[hoverm.size()])));
                if (i != 0) msg.addExtra(div);
                msg.addExtra(message);
                i++;
            }
            if (i == 0) sender.sendMessage("  " + plugin.api.getLang("SubServers", "Command.List.Empty"));
            else ((Player) sender).spigot().sendMessage(msg);
            if (data.getSection("proxies").getKeys().size() > 0) {
                sender.sendMessage(plugin.api.getLang("SubServers", "Command.List.Proxy-Header"));
                msg = new TextComponent("  (master)");
                msg.setColor(ChatColor.GRAY);
                for (String proxy : data.getSection("proxies").getKeys()) {
                    List<TextComponent> hoverm = new LinkedList<TextComponent>();
                    TextComponent message = new TextComponent(data.getSection("proxies").getSection(proxy).getString("display"));
                    TextComponent hover = new TextComponent(data.getSection("proxies").getSection(proxy).getString("display"));
                    if (data.getSection("proxies").getSection(proxy).getKeys().contains("subdata")) {
                        message.setColor(ChatColor.AQUA);
                        hover.setColor(ChatColor.AQUA);
                    } else {
                        message.setColor(ChatColor.WHITE);
                        hover.setColor(ChatColor.WHITE);
                    }
                    hoverm.add(hover);
                    if (!proxy.equals(data.getSection("proxies").getSection(proxy).getString("display"))) {
                        hover = new TextComponent('\n' + proxy);
                        hover.setColor(ChatColor.GRAY);
                        hoverm.add(hover);
                    }
                    message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverm.toArray(new TextComponent[hoverm.size()])));
                    msg.addExtra(div);
                    msg.addExtra(message);
                }
                ((Player) sender).spigot().sendMessage(msg);
            }
        }));
    }
}

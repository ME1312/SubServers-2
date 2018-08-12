package net.ME1312.SubServers.Client.Bukkit.Library.Compatibility;

import net.ME1312.SubServers.Client.Bukkit.Network.API.Host;
import net.ME1312.SubServers.Client.Bukkit.Network.API.Proxy;
import net.ME1312.SubServers.Client.Bukkit.Network.API.Server;
import net.ME1312.SubServers.Client.Bukkit.Network.API.SubServer;
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
        plugin.api.getGroups(groups -> plugin.api.getHosts(hosts -> plugin.api.getServers(servers -> plugin.api.getMasterProxy(proxymaster -> plugin.api.getProxies(proxies -> {
            int i = 0;
            boolean sent = false;
            TextComponent div = new TextComponent(plugin.api.getLang("SubServers", "Command.List.Divider"));
            if (groups.keySet().size() > 0) {
                sender.sendMessage(plugin.api.getLang("SubServers", "Command.List.Group-Header"));
                for (String group : groups.keySet()) {
                    List<TextComponent> hoverm = new LinkedList<TextComponent>();
                    TextComponent msg = new TextComponent("  ");
                    TextComponent message = new TextComponent(group);
                    TextComponent hover = new TextComponent(group + '\n');
                    message.setColor(ChatColor.GOLD);
                    hover.setColor(ChatColor.GOLD);
                    hoverm.add(hover);
                    hover = new TextComponent(plugin.api.getLang("SubServers", "Interface.Group-Menu.Group-Server-Count").replace("$int$", new DecimalFormat("#,###").format(groups.get(group).size())));
                    hoverm.add(hover);
                    message.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, label + " open Server 1 " + group));
                    message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverm.toArray(new TextComponent[hoverm.size()])));
                    msg.addExtra(message);
                    msg.addExtra(new TextComponent(plugin.api.getLang("SubServers", "Command.List.Header")));

                    for (Server server : groups.get(group)) {
                        hoverm = new LinkedList<TextComponent>();
                        message = new TextComponent(server.getDisplayName());
                        hover = new TextComponent(server.getDisplayName() + '\n');
                        if (server instanceof SubServer) {
                            if (((SubServer) server).isTemporary()) {
                                message.setColor(ChatColor.AQUA);
                                hover.setColor(ChatColor.AQUA);
                                hoverm.add(hover);
                                if (!server.getName().equals(server.getDisplayName())) {
                                    hover = new TextComponent(server.getName() + '\n');
                                    hover.setColor(ChatColor.GRAY);
                                    hoverm.add(hover);
                                }
                                hover = new TextComponent(plugin.api.getLang("SubServers", "Interface.Server-Menu.SubServer-Temporary") + '\n');
                                hoverm.add(hover);
                                hover = new TextComponent(plugin.api.getLang("SubServers", "Interface.Server-Menu.Server-Player-Count").replace("$int$", new DecimalFormat("#,###").format(server.getPlayers().size())));
                            } else if (((SubServer) server).isRunning()) {
                                message.setColor(ChatColor.GREEN);
                                hover.setColor(ChatColor.GREEN);
                                hoverm.add(hover);
                                if (!server.getName().equals(server.getDisplayName())) {
                                    hover = new TextComponent(server.getDisplayName() + '\n');
                                    hover.setColor(ChatColor.GRAY);
                                    hoverm.add(hover);
                                }
                                hover = new TextComponent(plugin.api.getLang("SubServers", "Interface.Server-Menu.Server-Player-Count").replace("$int$", new DecimalFormat("#,###").format(server.getPlayers().size())));
                            } else if (((SubServer) server).isEnabled() && ((SubServer) server).getCurrentIncompatibilities().size() == 0) {
                                message.setColor(ChatColor.YELLOW);
                                hover.setColor(ChatColor.YELLOW);
                                hoverm.add(hover);
                                if (!server.getName().equals(server.getDisplayName())) {
                                    hover = new TextComponent(server.getName() + '\n');
                                    hover.setColor(ChatColor.GRAY);
                                    hoverm.add(hover);
                                }
                                hover = new TextComponent(plugin.api.getLang("SubServers", "Interface.Server-Menu.SubServer-Offline"));
                            } else {
                                message.setColor(ChatColor.RED);
                                hover.setColor(ChatColor.RED);
                                if (!server.getName().equals(server.getDisplayName())) {
                                    hoverm.add(hover);
                                    hover = new TextComponent(server.getName() + '\n');
                                    hover.setColor(ChatColor.GRAY);
                                }
                                if (((SubServer) server).getCurrentIncompatibilities().size() != 0) {
                                    hoverm.add(hover);
                                    String list = "";
                                    for (String other : ((SubServer) server).getCurrentIncompatibilities()) {
                                        if (list.length() != 0) list += ", ";
                                        list += other;
                                    }
                                    hover = new TextComponent(plugin.api.getLang("SubServers", "Interface.Server-Menu.SubServer-Incompatible").replace("$str$", list) + ((((SubServer) server).isEnabled())?"":"\n"));
                                }
                                if (!((SubServer) server).isEnabled()) {
                                    hoverm.add(hover);
                                    hover = new TextComponent(plugin.api.getLang("SubServers", "Interface.Server-Menu.SubServer-Disabled"));
                                }
                            }
                            hoverm.add(hover);
                            if (plugin.config.get().getSection("Settings").getBoolean("Show-Addresses", false)) {
                                hover = new TextComponent('\n' + server.getAddress().getAddress().getHostAddress() + ':' + server.getAddress().getPort());
                            } else {
                                hover = new TextComponent("\n" + server.getAddress().getPort());
                            }
                            hover.setColor(ChatColor.WHITE);
                            hoverm.add(hover);
                            message.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, label + " open SubServer/ " + server.getName()));
                        } else {
                            message.setColor(ChatColor.WHITE);
                            hover.setColor(ChatColor.WHITE);
                            if (!server.getName().equals(server.getDisplayName())) {
                                hoverm.add(hover);
                                hover = new TextComponent(server.getName() + '\n');
                                hover.setColor(ChatColor.GRAY);
                            }
                            hoverm.add(hover);
                            hover = new TextComponent(plugin.api.getLang("SubServers", "Interface.Server-Menu.Server-External") + '\n');
                            hoverm.add(hover);
                            hover = new TextComponent(plugin.api.getLang("SubServers", "Interface.Server-Menu.Server-Player-Count").replace("$int$", new DecimalFormat("#,###").format(server.getPlayers().size())));
                            hoverm.add(hover);
                            if (plugin.config.get().getSection("Settings").getBoolean("Show-Addresses", false)) {
                                hover = new TextComponent('\n' + server.getAddress().getAddress().getHostAddress() + ':' + server.getAddress().getPort());
                            } else {
                                hover = new TextComponent("\n" + server.getAddress().getPort());
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
            for (Host host : hosts.values()) {
                List<TextComponent> hoverm = new LinkedList<TextComponent>();
                TextComponent msg = new TextComponent("  ");
                TextComponent message = new TextComponent(host.getDisplayName());
                TextComponent hover = new TextComponent(host.getDisplayName() + '\n');
                if (host.isEnabled()) {
                    message.setColor(ChatColor.AQUA);
                    hover.setColor(ChatColor.AQUA);
                    hoverm.add(hover);
                    if (!host.getName().equals(host.getDisplayName())) {
                        hover = new TextComponent(host.getName() + '\n');
                        hover.setColor(ChatColor.GRAY);
                        hoverm.add(hover);
                    }
                    hover = new TextComponent(plugin.api.getLang("SubServers", "Interface.Host-Menu.Host-Server-Count").replace("$int$", new DecimalFormat("#,###").format(host.getSubServers().keySet().size())));
                } else {
                    message.setColor(ChatColor.RED);
                    hover.setColor(ChatColor.RED);
                    hoverm.add(hover);
                    if (!host.getName().equals(host.getDisplayName())) {
                        hover = new TextComponent(host.getName() + '\n');
                        hover.setColor(ChatColor.GRAY);
                        hoverm.add(hover);
                    }
                    hover = new TextComponent(plugin.api.getLang("SubServers", "Interface.Host-Menu.Host-Disabled"));
                }
                if (plugin.config.get().getSection("Settings").getBoolean("Show-Addresses", false)) {
                    hoverm.add(hover);
                    hover = new TextComponent('\n' + host.getAddress().getHostAddress());
                    hover.setColor(ChatColor.WHITE);
                }
                hoverm.add(hover);
                message.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, label + " open Host/ " + host.getName()));
                message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverm.toArray(new TextComponent[hoverm.size()])));
                msg.addExtra(message);
                msg.addExtra(new TextComponent(plugin.api.getLang("SubServers", "Command.List.Header")));

                for (SubServer subserver : host.getSubServers().values()) {
                    hoverm = new LinkedList<TextComponent>();
                    message = new TextComponent(subserver.getDisplayName());
                    hover = new TextComponent(subserver.getDisplayName() + '\n');
                    message.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, label + " open SubServer/ " + subserver));
                    if (subserver.isTemporary()) {
                        message.setColor(ChatColor.AQUA);
                        hover.setColor(ChatColor.AQUA);
                        hoverm.add(hover);
                        if (!subserver.getName().equals(subserver.getDisplayName())) {
                            hover = new TextComponent(subserver.getName() + '\n');
                            hover.setColor(ChatColor.GRAY);
                            hoverm.add(hover);
                        }
                        hover = new TextComponent(plugin.api.getLang("SubServers", "Interface.Server-Menu.SubServer-Temporary") + '\n');
                        hoverm.add(hover);
                        hover = new TextComponent(plugin.api.getLang("SubServers", "Interface.Server-Menu.Server-Player-Count").replace("$int$", new DecimalFormat("#,###").format(subserver.getPlayers().size())));
                    } else if (subserver.isEnabled()) {
                        message.setColor(ChatColor.GREEN);
                        hover.setColor(ChatColor.GREEN);
                        hoverm.add(hover);
                        if (!subserver.getName().equals(subserver.getDisplayName())) {
                            hover = new TextComponent(subserver.getName() + '\n');
                            hover.setColor(ChatColor.GRAY);
                            hoverm.add(hover);
                        }
                        hover = new TextComponent(plugin.api.getLang("SubServers", "Interface.Server-Menu.Server-Player-Count").replace("$int$", new DecimalFormat("#,###").format(subserver.getPlayers().size())));
                    } else if (subserver.isEnabled() && subserver.getCurrentIncompatibilities().size() == 0) {
                        message.setColor(ChatColor.YELLOW);
                        hover.setColor(ChatColor.YELLOW);
                        hoverm.add(hover);
                        if (!subserver.getName().equals(subserver.getDisplayName())) {
                            hover = new TextComponent(subserver.getName() + '\n');
                            hover.setColor(ChatColor.GRAY);
                            hoverm.add(hover);
                        }
                        hover = new TextComponent(plugin.api.getLang("SubServers", "Interface.Server-Menu.SubServer-Offline"));
                    } else {
                        message.setColor(ChatColor.RED);
                        hover.setColor(ChatColor.RED);
                        if (!subserver.getName().equals(subserver.getDisplayName())) {
                            hoverm.add(hover);
                            hover = new TextComponent(subserver.getName() + '\n');
                            hover.setColor(ChatColor.GRAY);
                        }
                        if (subserver.getCurrentIncompatibilities().size() != 0) {
                            hoverm.add(hover);
                            String list = "";
                            for (String other : subserver.getCurrentIncompatibilities()) {
                                if (list.length() != 0) list += ", ";
                                list += other;
                            }
                            hover = new TextComponent(plugin.api.getLang("SubServers", "Interface.Server-Menu.SubServer-Incompatible").replace("$str$", list) + ((subserver.isEnabled())?"":"\n"));
                        }
                        if (!subserver.isEnabled()) {
                            hoverm.add(hover);
                            hover = new TextComponent(plugin.api.getLang("SubServers", "Interface.Server-Menu.SubServer-Disabled"));
                        }
                    }
                    hoverm.add(hover);
                    if (plugin.config.get().getSection("Settings").getBoolean("Show-Addresses", false)) {
                        hover = new TextComponent('\n' + subserver.getAddress().getAddress().getHostAddress()+':'+subserver.getAddress().getPort());
                    } else {
                        hover = new TextComponent("\n" + subserver.getAddress().getPort());
                    }
                    hover.setColor(ChatColor.WHITE);
                    hoverm.add(hover);
                    message.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, label + " open SubServer/ " + subserver.getName()));
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
            for (Server server : servers.values()) if (!(server instanceof SubServer)) {
                List<TextComponent> hoverm = new LinkedList<TextComponent>();
                TextComponent message = new TextComponent(server.getDisplayName());
                TextComponent hover = new TextComponent(server.getDisplayName() + '\n');
                message.setColor(ChatColor.WHITE);
                hover.setColor(ChatColor.WHITE);
                hoverm.add(hover);
                if (!server.getName().equals(server.getDisplayName())) {
                    hover = new TextComponent(server.getName() + '\n');
                    hover.setColor(ChatColor.GRAY);
                    hoverm.add(hover);
                }
                hover = new TextComponent(plugin.api.getLang("SubServers", "Interface.Server-Menu.Server-External") + '\n');
                hoverm.add(hover);
                hover = new TextComponent(plugin.api.getLang("SubServers", "Interface.Server-Menu.Server-Player-Count").replace("$int$", new DecimalFormat("#,###").format(server.getPlayers().size())));
                hoverm.add(hover);
                if (plugin.config.get().getSection("Settings").getBoolean("Show-Addresses", false)) {
                    hover = new TextComponent('\n' + server.getAddress().getAddress().getHostAddress()+':'+server.getAddress().getPort());
                } else {
                    hover = new TextComponent("\n" + server.getAddress().getPort());
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
            if (proxies.keySet().size() > 0) {
                sender.sendMessage(plugin.api.getLang("SubServers", "Command.List.Proxy-Header"));
                msg = new TextComponent("  ");
                List<TextComponent> hoverm = new LinkedList<TextComponent>();
                TextComponent message = new TextComponent("(master)");
                TextComponent hover = new TextComponent("(master)");
                message.setColor(ChatColor.GRAY);
                hover.setColor(ChatColor.GRAY);
                hoverm.add(hover);
                if (proxymaster != null) {
                    hover = new TextComponent('\n' + proxymaster.getName());
                    hover.setColor(ChatColor.GRAY);
                    hoverm.add(hover);
                    hover = new TextComponent('\n' + plugin.api.getLang("SubServers", "Interface.Proxy-Menu.Proxy-Master"));
                    hoverm.add(hover);
                    hover = new TextComponent('\n' + plugin.api.getLang("SubServers", "Interface.Proxy-Menu.Proxy-Player-Count").replace("$int$", new DecimalFormat("#,###").format(proxymaster.getPlayers().size())));
                    hoverm.add(hover);
                } else {
                    hover = new TextComponent('\n' + plugin.api.getLang("SubServers", "Interface.Proxy-Menu.Proxy-Master"));
                    hoverm.add(hover);
                }
                message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverm.toArray(new TextComponent[hoverm.size()])));
                msg.addExtra(message);
                for (Proxy proxy : proxies.values()) {
                    hoverm = new LinkedList<TextComponent>();
                    message = new TextComponent(proxy.getDisplayName());
                    hover = new TextComponent(proxy.getDisplayName());
                    if (proxy.getSubData() != null && proxy.isRedis()) {
                        message.setColor(ChatColor.GREEN);
                        hover.setColor(ChatColor.GREEN);
                        if (!proxy.getName().equals(proxy.getDisplayName())) {
                            hoverm.add(hover);
                            hover = new TextComponent('\n' + proxy.getName());
                            hover.setColor(ChatColor.GRAY);
                        }
                        hoverm.add(hover);
                        hover = new TextComponent('\n' + plugin.api.getLang("SubServers", "Interface.Proxy-Menu.Proxy-Player-Count").replace("$int$", new DecimalFormat("#,###").format(proxy.getPlayers().size())));
                    } else if (proxy.getSubData() != null) {
                        message.setColor(ChatColor.AQUA);
                        hover.setColor(ChatColor.AQUA);
                        if (!proxy.getName().equals(proxy.getDisplayName())) {
                            hoverm.add(hover);
                            hover = new TextComponent('\n' + proxy.getName());
                            hover.setColor(ChatColor.GRAY);
                        }
                        if (proxymaster != null) {
                            hoverm.add(hover);
                            hover = new TextComponent('\n' + plugin.api.getLang("SubServers", "Interface.Proxy-Menu.Proxy-SubData"));
                        }
                    } else if (proxy.isRedis()) {
                        message.setColor(ChatColor.WHITE);
                        hover.setColor(ChatColor.WHITE);
                        hoverm.add(hover);
                        if (!proxy.getName().equals(proxy.getDisplayName())) {
                            hover = new TextComponent('\n' + proxy.getName());
                            hover.setColor(ChatColor.GRAY);
                            hoverm.add(hover);
                        }
                        hover = new TextComponent('\n' + plugin.api.getLang("SubServers", "Interface.Proxy-Menu.Proxy-Redis"));
                        hoverm.add(hover);
                        hover = new TextComponent('\n' + plugin.api.getLang("SubServers", "Interface.Proxy-Menu.Proxy-Player-Count").replace("$int$", new DecimalFormat("#,###").format(proxy.getPlayers().size())));
                    } else {
                        message.setColor(ChatColor.RED);
                        hover.setColor(ChatColor.RED);
                        hoverm.add(hover);
                        if (!proxy.getName().equals(proxy.getDisplayName())) {
                            hover = new TextComponent('\n' + proxy.getName());
                            hover.setColor(ChatColor.GRAY);
                            hoverm.add(hover);
                        }
                        hover = new TextComponent('\n' + plugin.api.getLang("SubServers", "Interface.Proxy-Menu.Proxy-Disconnected"));
                    }
                    hoverm.add(hover);
                    message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverm.toArray(new TextComponent[hoverm.size()])));
                    msg.addExtra(div);
                    msg.addExtra(message);
                }
                ((Player) sender).spigot().sendMessage(msg);
            }
        })))));
    }
}

package net.ME1312.SubServers.Console;

import net.ME1312.SubServers.Bungee.Host.Host;
import net.ME1312.SubServers.Bungee.Host.SubServer;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.command.ConsoleCommandSender;

import javax.swing.*;
import java.util.List;
import java.util.Map;

public final class PopoutCommand {
    private PopoutCommand() {}
    public static class SERVER extends Command {
        private ConsolePlugin plugin;
        private String label;

        public SERVER(ConsolePlugin plugin, String command) {
            super(command);
            this.plugin = plugin;
            this.label = command;
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            if (sender instanceof ConsoleCommandSender) {
                if (args.length > 0) {
                    Map<String, SubServer> servers = plugin.getProxy().api.getSubServers();
                    if (servers.keySet().contains(args[0].toLowerCase())) {
                        boolean success = false;
                        if (servers.get(args[0].toLowerCase()).isRunning()) {
                            if (!plugin.sCurrent.keySet().contains(args[0].toLowerCase())) {
                                SwingUtilities.invokeLater(() -> {
                                    ConsoleWindow window = new ConsoleWindow(plugin, servers.get(args[0].toLowerCase()).getLogger());
                                    plugin.sCurrent.put(args[0].toLowerCase(), window);
                                    window.open();
                                });
                            } else {
                                plugin.sCurrent.get(args[0].toLowerCase()).open();
                            }
                            System.out.println("SubConsole > Opening Window...");
                            success = true;
                        }

                        try {
                            if (args.length > 1) {
                                if (args[1].equalsIgnoreCase("true")) {
                                    if (!plugin.config.get().getStringList("Enabled-Servers").contains(args[0].toLowerCase())) {
                                        List<String> list = plugin.config.get().getStringList("Enabled-Servers");
                                        list.add(args[0].toLowerCase());
                                        plugin.config.get().set("Enabled-Servers", list);
                                        plugin.config.save();
                                    }
                                    if (!success) System.out.println("SubConsole > " + servers.get(args[0].toLowerCase()).getName() + " was added to the enabled list");
                                    success = true;
                                } else if (args[1].equalsIgnoreCase("false")) {
                                    List<String> list = plugin.config.get().getStringList("Enabled-Servers");
                                    list.remove(args[0].toLowerCase());
                                    plugin.config.get().set("Enabled-Servers", list);
                                    if (plugin.sCurrent.keySet().contains(args[0].toLowerCase()) && !plugin.sCurrent.get(args[0].toLowerCase()).isOpen()) plugin.onClose(plugin.sCurrent.get(args[0].toLowerCase()));
                                    if (!success) System.out.println("SubConsole > " + servers.get(args[0].toLowerCase()).getName() + " was removed from the enabled list");
                                    success = true;
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        if (!success) System.out.println("SubConsole > That SubServer is not running right now.");
                    } else {
                        System.out.println("SubConsole > There is no SubServer with that name.");
                    }
                } else {
                    System.out.println("SubConsole > Usage: /" + label + " <SubServer> [Remember]");
                }
            } else {
                String str = label;
                for (String arg : args) str += ' ' + arg;
                ((ProxiedPlayer) sender).chat(str);
            }
        }
    }
    public static class CREATOR extends Command {
        private ConsolePlugin plugin;
        private String label;

        public CREATOR(ConsolePlugin plugin, String command) {
            super(command);
            this.plugin = plugin;
            this.label = command;
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            if (sender instanceof ConsoleCommandSender) {
                if (args.length > 0) {
                    Map<String, Host> hosts = plugin.getProxy().api.getHosts();
                    if (hosts.keySet().contains(args[0].toLowerCase())) {
                        boolean success = false;
                        if (hosts.get(args[0].toLowerCase()).getCreator().isBusy()) {
                            if (!plugin.cCurrent.keySet().contains(args[0].toLowerCase())) {
                                SwingUtilities.invokeLater(() -> {
                                    ConsoleWindow window = new ConsoleWindow(plugin, hosts.get(args[0].toLowerCase()).getCreator().getLogger());
                                    plugin.cCurrent.put(args[0].toLowerCase(), window);
                                    window.open();
                                });
                            } else {
                                plugin.cCurrent.get(args[0].toLowerCase()).open();
                            }
                            System.out.println("SubConsole > Opening Window...");
                            success = true;
                        }

                        try {
                            if (args.length > 1) {
                                if (args[1].equalsIgnoreCase("true")) {
                                    if (!plugin.config.get().getStringList("Enabled-Creators").contains(args[0].toLowerCase())) {
                                        List<String> list = plugin.config.get().getStringList("Enabled-Creators");
                                        list.add(args[0].toLowerCase());
                                        plugin.config.get().set("Enabled-Creators", list);
                                        plugin.config.save();
                                    }
                                    if (!success) System.out.println("SubConsole > " + hosts.get(args[0].toLowerCase()).getName() + "/Creator was added to the enabled list");
                                    success = true;
                                } else if (args[1].equalsIgnoreCase("false")) {
                                    List<String> list = plugin.config.get().getStringList("Enabled-Creators");
                                    list.remove(args[0].toLowerCase());
                                    plugin.config.get().set("Enabled-Creators", list);
                                    if (plugin.cCurrent.keySet().contains(args[0].toLowerCase()) && !plugin.cCurrent.get(args[0].toLowerCase()).isOpen()) plugin.onClose(plugin.cCurrent.get(args[0].toLowerCase()));
                                    if (!success) System.out.println("SubConsole > " + hosts.get(args[0].toLowerCase()).getName() + "/Creator was removed from the enabled list");
                                    success = true;
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        if (!success) System.out.println("SubConsole > That Host's Creator is not running right now.");
                    } else {
                        System.out.println("SubConsole > There is no Host with that name.");
                    }
                } else {
                    System.out.println("SubConsole > Usage: /" + label + " <Host> [Remember]");
                }
            } else {
                String str = label;
                for (String arg : args) str += ' ' + arg;
                ((ProxiedPlayer) sender).chat(str);
            }
        }
    }
}

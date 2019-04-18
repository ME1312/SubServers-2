package net.ME1312.SubServers.Console;

import net.ME1312.Galaxi.Library.Callback.Callback;
import net.ME1312.SubServers.Bungee.Host.Host;
import net.ME1312.SubServers.Bungee.Host.SubServer;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.command.ConsoleCommandSender;

import java.util.List;

@SuppressWarnings("unchecked")
public final class ConsoleCommand {
    private ConsoleCommand() {}

    public static class POPOUT extends Command {
        private ConsolePlugin plugin;
        private String label;

        public POPOUT(ConsolePlugin plugin, String command) {
            super(command);
            this.plugin = plugin;
            this.label = command;
        }

        @Override
        public void execute(final CommandSender sender, String[] args) {
            if (sender instanceof ConsoleCommandSender) {
                if (args.length > 0) {
                    final String type = (args.length > 1)?args[0]:null;
                    final String name = args[(type != null)?1:0];

                    final Runnable forServer = new Runnable() {
                        @Override
                        public void run() {
                            SubServer server = plugin.getProxy().api.getSubServer(name);
                            if (server != null) {
                                if (server.isRunning()) {
                                    System.out.println("SubConsole > Opening console window...");
                                    if (!plugin.sCurrent.keySet().contains(name.toLowerCase())) {
                                        ConsoleWindow window = new ConsoleWindow(plugin, server.getLogger());
                                        plugin.sCurrent.put(name.toLowerCase(), window);
                                        window.open();
                                    } else {
                                        plugin.sCurrent.get(name.toLowerCase()).open();
                                    }
                                } else {
                                    sender.sendMessage("SubConsole > That SubServer is not running right now");
                                }
                            } else {
                                if (type == null) {
                                    sender.sendMessage("SubConsole > There is no object with that name");
                                } else {
                                    sender.sendMessage("SubConsole > There is no subserver with that name");
                                }
                            }
                        }
                    };
                    final Runnable forCreator = new Runnable() {
                        @Override
                        public void run() {
                            Host host = plugin.getProxy().api.getHost(name);
                            if (host != null) {
                                if (host.getCreator().getReservedNames().size() > 0) {
                                    sender.sendMessage("SubConsole > Opening console window" + ((host.getCreator().getReservedNames().size() == 1)?"":"s") + "...");
                                    for (String reserved : host.getCreator().getReservedNames()) {
                                        if (!plugin.cCurrent.keySet().contains(reserved.toLowerCase())) {
                                            ConsoleWindow window = new ConsoleWindow(plugin, host.getCreator().getLogger(reserved));
                                            plugin.cCurrent.put(reserved.toLowerCase(), window);
                                            window.open();
                                        } else {
                                            plugin.cCurrent.get(reserved.toLowerCase()).open();
                                        }
                                    }
                                } else {
                                    sender.sendMessage("SubConsole > That Host is not running SubCreator right now");
                                }
                            } else {
                                if (type == null) {
                                    forServer.run();
                                } else {
                                    sender.sendMessage("SubConsole > There is no host with that name");
                                }
                            }
                        }
                    };

                    if (type == null) {
                        forCreator.run();
                    } else {
                        switch (type.toLowerCase()) {
                            case "h":
                            case "host":
                            case "c":
                            case "creator":
                            case "subcreator":
                                forCreator.run();
                                break;
                            case "s":
                            case "server":
                            case "subserver":
                                forServer.run();
                                break;
                            default:
                                sender.sendMessage("SubConsole > There is no object type with that name");
                        }
                    }
                } else {
                    System.out.println("SubConsole > Usage: /" + label + " [host|server] <Name>");
                }
            } else {
                String str = label;
                for (String arg : args) str += ' ' + arg;
                ((ProxiedPlayer) sender).chat(str);
            }
        }
    }

    public static class AUTO_POPOUT extends Command {
        private ConsolePlugin plugin;
        private String label;

        public AUTO_POPOUT(ConsolePlugin plugin, String command) {
            super(command);
            this.plugin = plugin;
            this.label = command;
        }

        @Override
        public void execute(final CommandSender sender, String[] args) {
            if (sender instanceof ConsoleCommandSender) {
                if (args.length > 0) {
                    final String type = (args.length > 1)?args[0]:null;
                    final String name = args[(type != null)?1:0];

                    final Runnable forServer = new Runnable() {
                        @Override
                        public void run() {
                            SubServer server = plugin.getProxy().api.getSubServer(name);
                            List<String> list = plugin.config.get().getStringList("Enabled-Servers");
                            if (!plugin.config.get().getStringList("Enabled-Servers").contains(name.toLowerCase())) {
                                list.add(name.toLowerCase());
                                if (server == null) plugin.getProxy().getLogger().warning("SubConsole > SubServer with name \"" + name + "\" does not exist");
                                sender.sendMessage("SubConsole > " + ((server == null)?name:server.getName()) + " will now popout its console by default");
                            } else {
                                list.remove(name.toLowerCase());
                                sender.sendMessage("SubConsole > " + ((server == null)?name:server.getName()) + " will no longer popout its console by default");
                            }
                            plugin.config.get().set("Enabled-Servers", list);

                            try {
                                plugin.config.save();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    };
                    final Callback<Boolean> forCreator = new Callback<Boolean>() {
                        @Override
                        public void run(Boolean force) {
                            Host host = plugin.getProxy().api.getHost(name);
                            if (force || host != null) {
                                List<String> list = plugin.config.get().getStringList("Enabled-Creators");
                                if (!plugin.config.get().getStringList("Enabled-Creators").contains(name.toLowerCase())) {
                                    list.add(name.toLowerCase());
                                    if (host == null) plugin.getProxy().getLogger().warning("SubConsole > Host with name \"" + name + "\" does not exist");
                                    sender.sendMessage("SubConsole > " + ((host == null)?name:host.getName()) + " will now popout SubCreator's console by default");
                                } else {
                                    list.remove(name.toLowerCase());
                                    sender.sendMessage("SubConsole > " + ((host == null)?name:host.getName()) + " will no longer popout SubCreator's console by default");
                                }
                                plugin.config.get().set("Enabled-Creators", list);

                                try {
                                    plugin.config.save();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            } else {
                                forServer.run();
                            }
                        }
                    };

                    if (type == null) {
                        forCreator.run(false);
                    } else {
                        switch (type.toLowerCase()) {
                            case "h":
                            case "host":
                            case "c":
                            case "creator":
                            case "subcreator":
                                forCreator.run(true);
                                break;
                            case "s":
                            case "server":
                            case "subserver":
                                forServer.run();
                                break;
                            default:
                                sender.sendMessage("SubConsole > There is no object type with that name");
                        }
                    }
                } else {
                    System.out.println("SubConsole > Usage: /" + label + " [host|server] <Name>");
                }
            } else {
                String str = label;
                for (String arg : args) str += ' ' + arg;
                ((ProxiedPlayer) sender).chat(str);
            }
        }
    }
}

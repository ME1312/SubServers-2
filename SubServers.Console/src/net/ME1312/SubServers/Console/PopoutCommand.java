package net.ME1312.SubServers.Console;

import net.ME1312.SubServers.Bungee.Host.SubServer;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.command.ConsoleCommandSender;

import java.util.List;
import java.util.Map;

public class PopoutCommand extends Command {
    private ConsolePlugin plugin;
    private String label;

    public PopoutCommand(ConsolePlugin plugin, String command) {
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
                        ConsoleWindow window = new ConsoleWindow(servers.get(args[0].toLowerCase()));
                        plugin.current.put(args[0].toLowerCase(), window);
                        window.open();
                        System.out.println("SubConsole > Opening Window...");
                        success = true;
                    }

                    try {
                        if (args.length > 1) {
                            if (args[1].equalsIgnoreCase("true")) {
                                List<String> list = plugin.config.get().getStringList("Enabled-Servers");
                                list.add(args[0].toLowerCase());
                                plugin.config.get().set("Enabled-Servers", list);
                                plugin.config.save();
                                if (!success) System.out.println("SubConsole > " + servers.get(args[0].toLowerCase()).getName() + " was added to the enabled list");
                                success = true;
                            } else if (args[1].equalsIgnoreCase("false")) {
                                List<String> list = plugin.config.get().getStringList("Enabled-Servers");
                                list.remove(args[0].toLowerCase());
                                plugin.config.get().set("Enabled-Servers", list);
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

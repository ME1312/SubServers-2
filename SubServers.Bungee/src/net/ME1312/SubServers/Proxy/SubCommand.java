package net.ME1312.SubServers.Proxy;

import net.ME1312.SubServers.Proxy.Host.Server;
import net.ME1312.SubServers.Proxy.Host.SubCreator;
import net.ME1312.SubServers.Proxy.Host.SubServer;
import net.ME1312.SubServers.Proxy.Library.Util;
import net.ME1312.SubServers.Proxy.Library.Version.Version;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.command.ConsoleCommandSender;

import java.util.Map;

/**
 * Plugin Command Class
 *
 * @author ME1312
 */
public final class SubCommand extends Command {
    private SubPlugin plugin;

    public SubCommand(SubPlugin plugin) {
        super("subserver", null, "sub", "subservers");
        this.plugin = plugin;
    }

    /**
     * Load /Sub in console
     *
     * @param sender
     * @param args
     */
    @SuppressWarnings("deprecation")
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (sender instanceof ConsoleCommandSender) {
            if (args.length > 0) {
                if (args[0].equalsIgnoreCase("help") || args[0].equalsIgnoreCase("?")) {
                    sender.sendMessages(printHelp());
                } else if (args[0].equalsIgnoreCase("version") || args[0].equalsIgnoreCase("ver")) {
                    sender.sendMessage("SubServers > SubServers.Bungee is running version " + plugin.version.toString() + ((plugin.bversion != null)?" BETA "+plugin.bversion.toString():""));
                } else if (args[0].equalsIgnoreCase("list")) {
                    sender.sendMessages(
                            "SubServers > Host List:", plugin.hosts.keySet().toString(),
                            "SubServers > Server List:", plugin.api.getServers().keySet().toString());
                } else if (args[0].equalsIgnoreCase("start")) {
                    if (args.length > 1) {
                        Map<String, Server> servers = plugin.api.getServers();
                        if (!servers.keySet().contains(args[1].toLowerCase())) {
                            sender.sendMessage("SubServers > There is no server with that name");
                        } else if (!(servers.get(args[1].toLowerCase()) instanceof SubServer)) {
                            sender.sendMessage("SubServers > That Server is not a SubServer");
                        } else if (!((SubServer) servers.get(args[1].toLowerCase())).getHost().isEnabled()) {
                            sender.sendMessage("SubServers > That SubServer's Host is not enabled");
                        } else if (!((SubServer) servers.get(args[1].toLowerCase())).isEnabled()) {
                            sender.sendMessage("SubServers > That SubServer is not enabled");
                        } else if (((SubServer) servers.get(args[1].toLowerCase())).isRunning()) {
                            sender.sendMessage("SubServers > That SubServer is already running");
                        } else {
                            ((SubServer) servers.get(args[1].toLowerCase())).start();
                        }
                    } else {
                        sender.sendMessage("SubServers > Usage: /sub start <SubServer>");
                    }
                } else if (args[0].equalsIgnoreCase("stop")) {
                    if (args.length > 1) {
                        Map<String, Server> servers = plugin.api.getServers();
                        if (!servers.keySet().contains(args[1].toLowerCase())) {
                            sender.sendMessage("SubServers > There is no server with that name");
                        } else if (!(servers.get(args[1].toLowerCase()) instanceof SubServer)) {
                            sender.sendMessage("SubServers > That Server is not a SubServer");
                        } else if (!((SubServer) servers.get(args[1].toLowerCase())).isRunning()) {
                            sender.sendMessage("SubServers > That SubServer is not running");
                        } else {
                            ((SubServer) servers.get(args[1].toLowerCase())).stop();
                        }
                    } else {
                        sender.sendMessage("SubServers > Usage: /sub stop <SubServer>");
                    }
                } else if (args[0].equalsIgnoreCase("kill") || args[0].equalsIgnoreCase("terminate")) {
                    if (args.length > 1) {
                        Map<String, Server> servers = plugin.api.getServers();
                        if (!servers.keySet().contains(args[1].toLowerCase())) {
                            sender.sendMessage("SubServers > There is no server with that name");
                        } else if (!(servers.get(args[1].toLowerCase()) instanceof SubServer)) {
                            sender.sendMessage("SubServers > That Server is not a SubServer");
                        } else if (!((SubServer) servers.get(args[1].toLowerCase())).isRunning()) {
                            sender.sendMessage("SubServers > That SubServer is not running");
                        } else {
                            ((SubServer) servers.get(args[1].toLowerCase())).terminate();
                        }
                    } else {
                        sender.sendMessage("SubServers > Usage: /sub kill <SubServer>");
                    }
                } else if (args[0].equalsIgnoreCase("cmd") || args[0].equalsIgnoreCase("command")) {
                    if (args.length > 2) {
                        Map<String, Server> servers = plugin.api.getServers();
                        if (!servers.keySet().contains(args[1].toLowerCase())) {
                            sender.sendMessage("SubServers > There is no server with that name");
                        } else if (!(servers.get(args[1].toLowerCase()) instanceof SubServer)) {
                            sender.sendMessage("SubServers > That Server is not a SubServer");
                        } else if (!((SubServer) servers.get(args[1].toLowerCase())).isRunning()) {
                            sender.sendMessage("SubServers > That SubServer is not running");
                        } else {
                            int i = 2;
                            String str = args[2];
                            if (args.length > 3) {
                                do {
                                    i++;
                                    str = str + " " + args[i];
                                } while ((i + 1) != args.length);
                            }
                            ((SubServer) servers.get(args[1].toLowerCase())).command(str);
                        }
                    } else {
                        sender.sendMessage("SubServers > Usage: /sub cmd <SubServer> <Command> [Args...]");
                    }
                } else if (args[0].equalsIgnoreCase("create")) {
                    if (args.length > 5) {
                        if (plugin.api.getSubServers().keySet().contains(args[1].toLowerCase())) {
                            sender.sendMessage("SubServers > There is already a SubServer with that name");
                        } else if (!plugin.hosts.keySet().contains(args[2].toLowerCase())) {
                            sender.sendMessage("SubServers > There is no host with that name");
                        } else if (plugin.hosts.get(args[2].toLowerCase()).getCreator().isBusy()) {
                            sender.sendMessage("SubServers > The SubCreator instance on that host is already running");
                        } else if (Util.isException(() -> SubCreator.ServerType.valueOf(args[3].toUpperCase()))) {
                            sender.sendMessage("SubServers > There is no server type with that name");
                        } else if (new Version("1.8").compareTo(new Version(args[4])) > 0) {
                            sender.sendMessage("SubServers > SubCreator cannot create servers before Minecraft 1.8");
                        } else if (Util.isException(() -> Integer.parseInt(args[5])) || Integer.parseInt(args[5]) <= 0 || Integer.parseInt(args[5]) > 65535) {
                            sender.sendMessage("SubServers > Invalid Port Number");
                        } else if (args.length > 6 && (Util.isException(() -> Integer.parseInt(args[6])) || Integer.parseInt(args[6]) < 256)) {
                            sender.sendMessage("SubServers > Invalid Ram Amount");
                        } else {
                            plugin.hosts.get(args[2].toLowerCase()).getCreator().create(args[1], SubCreator.ServerType.valueOf(args[3].toUpperCase()), new Version(args[4]), (args.length > 6)?Integer.parseInt(args[6]):1024, Integer.parseInt(args[5]));
                        }
                    } else {
                        sender.sendMessage("SubServers > Usage: /sub create <Name> <Host> <Type> <Version> <Port> [RAM]");
                    }
                }
            } else {
                sender.sendMessages(printHelp());
            }
        } else {
            String str = "";
            int i = -1;
            while ((i + 1) != args.length) {
                i++;
                str = str + " " + args[i];
            }
            ((ProxiedPlayer) sender).chat("/subservers" + str);
        }
    }

    public String[] printHelp() {
        return new String[]{
                "SubServers > Console Command Help:",
                "   Help: /sub help",
                "   List: /sub list",
                "   Version: /sub version",
                "   Start Server: /sub start <SubServer>",
                "   Stop Server: /sub stop <SubServer>",
                "   Terminate Server: /sub kill <SubServer>",
                "   Command Server: /sub cmd <SubServer> <Command> [Args...]",
                "   Create Server: /sub create <Name> <Host> <Type> <Version> <Port> [RAM]",
                "",
                "   To see BungeeCord Supplied Commands, please visit:",
                "   https://www.spigotmc.org/wiki/bungeecord-commands/"
        };
    }
}

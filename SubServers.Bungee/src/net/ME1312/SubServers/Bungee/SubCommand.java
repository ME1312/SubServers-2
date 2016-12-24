package net.ME1312.SubServers.Bungee;

import net.ME1312.SubServers.Bungee.Host.Host;
import net.ME1312.SubServers.Bungee.Host.Server;
import net.ME1312.SubServers.Bungee.Host.SubCreator;
import net.ME1312.SubServers.Bungee.Host.SubServer;
import net.ME1312.SubServers.Bungee.Library.Util;
import net.ME1312.SubServers.Bungee.Library.Version.Version;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.command.ConsoleCommandSender;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Plugin Command Class
 *
 * @author ME1312
 */
public final class SubCommand extends Command {
    private SubPlugin plugin;
    private String label;

    protected SubCommand(SubPlugin plugin, String command) {
        super(command);
        this.plugin = plugin;
        this.label = "/" + command;
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
                    List<String> hosts = new ArrayList<String>();
                    for (Host host : plugin.hosts.values())  {
                        hosts.add(host.getName());
                    }
                    List<String> servers = new ArrayList<String>();
                    servers.addAll(plugin.getServers().keySet());
                    sender.sendMessages(
                            "SubServers > Host List:", hosts.toString().substring(1, hosts.toString().length() - 1),
                            "SubServers > Server List:", servers.toString().substring(1, servers.toString().length() - 1));
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
                        sender.sendMessage("SubServers > Usage: " + label + " " + args[0].toLowerCase() + " <SubServer>");
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
                        sender.sendMessage("SubServers > Usage: " + label + " " + args[0].toLowerCase() + " <SubServer>");
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
                        sender.sendMessage("SubServers > Usage: " + label + " " + args[0].toLowerCase() + " <SubServer>");
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
                        sender.sendMessage("SubServers > Usage: " + label + " " + args[0].toLowerCase() + " <SubServer> <Command> [Args...]");
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
                        sender.sendMessage("SubServers > Usage: " + label + " " + args[0].toLowerCase() + " <Name> <Host> <Type> <Version> <Port> [RAM]");
                    }
                } else {
                    sender.sendMessage("SubServers > Unknown sub-command: " + args[0]);
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
            ((ProxiedPlayer) sender).chat(label + str);
        }
    }

    private String[] printHelp() {
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



    public static class BungeeServer extends Command {
        private SubPlugin plugin;
        protected BungeeServer(SubPlugin plugin, String command) {
            super(command, "bungeecord.command.server");
            this.plugin = plugin;
        }

        @SuppressWarnings("deprecation")
        @Override
        public void execute(CommandSender sender, String[] args) {
            if (sender instanceof ProxiedPlayer) {
                if (args.length > 0) {
                    Map<String, Server> servers = plugin.api.getServers();
                    if (servers.keySet().contains(args[0].toLowerCase())) {
                        ((ProxiedPlayer) sender).connect(servers.get(args[0].toLowerCase()));
                    } else {
                        sender.sendMessage(plugin.lang.get().getSection("Lang").getColoredString("Bungee.Server.Invalid", '&'));
                    }
                } else {
                    int i = 0;
                    TextComponent serverm = new TextComponent(ChatColor.RESET.toString());
                    TextComponent div = new TextComponent(plugin.lang.get().getSection("Lang").getColoredString("Bungee.Server.Divider", '&'));
                    for (Server server : plugin.api.getServers().values()) {
                        if (!server.isHidden() && (!(server instanceof SubServer) || ((SubServer) server).isRunning())) {
                            if (i != 0) serverm.addExtra(div);
                            TextComponent message = new TextComponent(plugin.lang.get().getSection("Lang").getColoredString("Bungee.Server.List", '&').replace("$str$", server.getName()));
                            message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponent[]{new TextComponent(plugin.lang.get().getSection("Lang").getColoredString("Bungee.Server.Hover", '&').replace("$int$", Integer.toString(server.getPlayers().size())))}));
                            message.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/server " + server.getName()));
                            serverm.addExtra(message);
                            i++;
                        }
                    }
                    sender.sendMessages(
                            plugin.lang.get().getSection("Lang").getColoredString("Bungee.Server.Current", '&').replace("$str$", ((ProxiedPlayer) sender).getServer().getInfo().getName()),
                            plugin.lang.get().getSection("Lang").getColoredString("Bungee.Server.Available", '&'));
                    sender.sendMessage(serverm);
                }
            } else {
                sender.sendMessage(plugin.lang.get().getSection("Lang").getColoredString("Command.Generic.Player-Only", '&'));
            }
        }
    }

    public static class BungeeList extends Command {
        private SubPlugin plugin;
        protected BungeeList(SubPlugin plugin, String command) {
            super(command, "bungeecord.command.list");
            this.plugin = plugin;
        }

        @SuppressWarnings("deprecation")
        @Override
        public void execute(CommandSender sender, String[] args) {
            List<String> messages = new LinkedList<String>();
            int players = 0;
            for (Server server : plugin.api.getServers().values()) {
                players += server.getPlayers().size();
                if (!server.isHidden() && (!(server instanceof SubServer) || ((SubServer) server).isRunning())) {
                    int i = 0;
                    String message = plugin.lang.get().getSection("Lang").getColoredString("Bungee.List.Format", '&').replace("$str$", server.getName()).replace("$int$", Integer.toString(server.getPlayers().size()));
                    for (ProxiedPlayer player : server.getPlayers()) {
                        if (i != 0) message += plugin.lang.get().getSection("Lang").getColoredString("Bungee.List.Divider", '&');
                        message += plugin.lang.get().getSection("Lang").getColoredString("Bungee.List.List", '&').replace("$str$", player.getName());
                        i++;
                    }
                    messages.add(message);
                }
            }
            sender.sendMessages(messages.toArray(new String[messages.size()]));
            sender.sendMessage(plugin.lang.get().getSection("Lang").getColoredString("Bungee.List.Total", '&').replace("$int$", Integer.toString(players)));
        }
    }
}

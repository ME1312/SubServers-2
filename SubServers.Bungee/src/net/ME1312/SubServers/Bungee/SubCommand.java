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
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;
import net.md_5.bungee.command.ConsoleCommandSender;

import java.util.*;

/**
 * Plugin Command Class
 */
@SuppressWarnings("deprecation")
public final class SubCommand extends Command implements TabExecutor {
    private SubPlugin plugin;
    private String label;

    protected SubCommand(SubPlugin plugin, String command) {
        super(command);
        this.plugin = plugin;
        this.label = "/" + command;
    }

    /**
     * Load /sub in console
     *
     * @param sender Sender
     * @param args Arguments
     */
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
                        hosts.add(host.getDisplayName() + ((host.getName().equals(host.getDisplayName()))?"":" (" + host.getName() + ')'));
                    }
                    List<String> servers = new ArrayList<String>();
                    for (Server server : plugin.api.getServers().values()) {
                        servers.add(server.getDisplayName() + ((server.getName().equals(server.getDisplayName()))?"":" (" + server.getName() + ')'));
                    }
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
                        if (!(servers.keySet().contains(args[1].toLowerCase()) || args[1].equals("*"))) {
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
                            if (args[1].equals("*")) {
                                for (Server server : servers.values()) {
                                    if (((SubServer) server).isRunning()) {
                                        ((SubServer) server).command(str);
                                    }
                                }
                            } else {
                                ((SubServer) servers.get(args[1].toLowerCase())).command(str);
                            }
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
                            plugin.hosts.get(args[2].toLowerCase()).getCreator().create(args[1], SubCreator.ServerType.valueOf(args[3].toUpperCase()), new Version(args[4]), (args.length > 6) ? Integer.parseInt(args[6]) : 1024, Integer.parseInt(args[5]));
                        }
                    } else {
                        sender.sendMessage("SubServers > Usage: " + label + " " + args[0].toLowerCase() + " <Name> <Host> <Type> <Version> <Port> [RAM]");
                    }
                } else if (args[0].equalsIgnoreCase("del") || args[0].equalsIgnoreCase("delete")) {
                    if (args.length > 1) {
                        Map<String, Server> servers = plugin.api.getServers();
                        try {
                            if (!servers.keySet().contains(args[1].toLowerCase())) {
                                sender.sendMessage("SubServers > There is no server with that name");
                            } else if (!(servers.get(args[1].toLowerCase()) instanceof SubServer)) {
                                sender.sendMessage("SubServers > That Server is not a SubServer");
                            } else if (((SubServer) servers.get(args[1].toLowerCase())).isRunning()) {
                                sender.sendMessage("SubServers > That SubServer is still running");
                            } else if (!((SubServer) servers.get(args[1].toLowerCase())).getHost().deleteSubServer(args[1].toLowerCase())){
                                System.out.println("SubServers > Couldn't remove server from memory.");
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        sender.sendMessage("SubServers > Usage: " + label + " " + args[0].toLowerCase() + " <SubServer>");
                    }
                } else {
                    sender.sendMessage("SubServers > Unknown sub-command: " + args[0]);
                }
            } else {
                sender.sendMessages(printHelp());
            }
        } else {
            String str = label;
            for (String arg : args) str += ' ' + arg;
            ((ProxiedPlayer) sender).chat(str);
        }
    }

    /**
     * Tab complete for players
     *
     * @param sender Sender
     * @param args Arguments
     * @return Tab completes
     */
    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        String last = (args.length > 0)?args[args.length - 1].toLowerCase():"";
        if (args.length <= 1) {
            List<String> cmds = Arrays.asList("help", "list", "version", "start", "stop", "kill", "terminate", "cmd", "command", "create");
            if (last.length() == 0) {
                return cmds;
            } else {
                List<String> list = new ArrayList<String>();
                for (String cmd : cmds) {
                    if (cmd.startsWith(last)) list.add(cmd);
                }
                return list;
            }
        } else {
            if (args[0].equals("start") ||
                    args[0].equals("stop") ||
                    args[0].equals("kill") || args[0].equals("terminate")) {
                if (args.length == 2) {
                    List<String> list = new ArrayList<String>();
                    if (last.length() == 0) {
                        for (SubServer server : plugin.api.getSubServers().values()) list.add(server.getName());
                    } else {
                        for (SubServer server : plugin.api.getSubServers().values()) {
                            if (server.getName().toLowerCase().startsWith(last)) list.add(server.getName());
                        }
                    }
                    return list;
                }
                return Collections.emptyList();
            } else if (args[0].equals("cmd") || args[0].equals("command")) {
                if (args.length == 2) {
                    List<String> list = new ArrayList<String>();
                    if (last.length() == 0) {
                        for (SubServer server : plugin.api.getSubServers().values()) list.add(server.getName());
                    } else {
                        for (SubServer server : plugin.api.getSubServers().values()) {
                            if (server.getName().toLowerCase().startsWith(last)) list.add(server.getName());
                        }
                    }
                    return list;
                } else if (args.length == 3) {
                    if (last.length() == 0) {
                        return Collections.singletonList("<Command>");
                    }
                } else {
                    if (last.length() == 0) {
                        return Collections.singletonList("[Args...]");
                    }
                }
                return Collections.emptyList();
            } else if (args[0].equals("create")) {
                if (args.length == 2) {
                    if (last.length() == 0) {
                        return Collections.singletonList("<Name>");
                    }
                } else if (args.length == 3) {
                    List<String> list = new ArrayList<String>();
                    if (last.length() == 0) {
                        for (Host host : plugin.api.getHosts().values()) list.add(host.getName());
                    } else {
                        for (Host host : plugin.api.getHosts().values()) {
                            if (host.getName().toLowerCase().startsWith(last)) list.add(host.getName());
                        }
                    }
                    return list;
                } else if (args.length == 4) {
                    List<String> list = new ArrayList<String>();
                    if (last.length() == 0) {
                        for (SubCreator.ServerType type : SubCreator.ServerType.values()) list.add(type.toString());
                    } else {
                        for (SubCreator.ServerType type : SubCreator.ServerType.values()) {
                            if (type.toString().toLowerCase().startsWith(last)) list.add(type.toString());
                        }
                    }
                    return list;
                } else if (args.length == 5) {
                    if (last.length() == 0) {
                        return Collections.singletonList("<Version>");
                    }
                } else if (args.length == 6) {
                    if (last.length() == 0) {
                        return Collections.singletonList("<Port>");
                    }
                } else if (args.length == 7) {
                    if (last.length() == 0) {
                        return Collections.singletonList("[RAM]");
                    }
                }
                return Collections.emptyList();
            } else {
                return Collections.emptyList();
            }
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
                "   Remove Server: /sub delete <SubServer>",
                "",
                "   To see BungeeCord Supplied Commands, please visit:",
                "   https://www.spigotmc.org/wiki/bungeecord-commands/"
        };
    }

    /**
     * BungeeCord /server
     */
    public static final class BungeeServer extends Command implements TabExecutor {
        private SubPlugin plugin;
        protected BungeeServer(SubPlugin plugin, String command) {
            super(command, "bungeecord.command.server");
            this.plugin = plugin;
        }

        /**
         * Override /server
         *
         * @param sender Sender
         * @param args Arguments
         */
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
                            TextComponent message = new TextComponent(plugin.lang.get().getSection("Lang").getColoredString("Bungee.Server.List", '&').replace("$str$", server.getDisplayName()));
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

        /**
         * Tab completer
         *
         * @param sender Sender
         * @param args Arguments
         * @return Tab completes
         */
        @Override
        public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
            if (args.length <= 1) {
                String last = (args.length > 0)?args[args.length - 1].toLowerCase():"";
                if (last.length() == 0) {
                    return plugin.getServers().keySet();
                } else {
                    List<String> list = new ArrayList<String>();
                    for (String server : plugin.getServers().keySet()) {
                        if (server.toLowerCase().startsWith(last)) list.add(server);
                    }
                    return list;
                }
            } else {
                return Collections.emptyList();
            }
        }
    }

    /**
     * BungeeCord /glist
     */
    public static final class BungeeList extends Command {
        private SubPlugin plugin;
        protected BungeeList(SubPlugin plugin, String command) {
            super(command, "bungeecord.command.list");
            this.plugin = plugin;
        }

        /**
         * Override /glist
         *
         * @param sender
         * @param args
         */
        @SuppressWarnings("deprecation")
        @Override
        public void execute(CommandSender sender, String[] args) {
            List<String> messages = new LinkedList<String>();
            int players = 0;
            for (Server server : plugin.api.getServers().values()) {
                players += server.getPlayers().size();
                if (!server.isHidden() && (!(server instanceof SubServer) || ((SubServer) server).isRunning())) {
                    int i = 0;
                    String message = plugin.lang.get().getSection("Lang").getColoredString("Bungee.List.Format", '&').replace("$str$", server.getDisplayName()).replace("$int$", Integer.toString(server.getPlayers().size()));
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

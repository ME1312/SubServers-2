package net.ME1312.SubServers.Bungee;

import net.ME1312.SubServers.Bungee.Host.Host;
import net.ME1312.SubServers.Bungee.Host.Server;
import net.ME1312.SubServers.Bungee.Host.SubCreator;
import net.ME1312.SubServers.Bungee.Host.SubServer;
import net.ME1312.SubServers.Bungee.Library.Compatibility.CommandX;
import net.ME1312.SubServers.Bungee.Library.NamedContainer;
import net.ME1312.SubServers.Bungee.Library.Util;
import net.ME1312.SubServers.Bungee.Library.Version.Version;
import net.ME1312.SubServers.Bungee.Library.Version.VersionType;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.command.ConsoleCommandSender;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.*;

/**
 * Plugin Command Class
 */
@SuppressWarnings("deprecation")
public final class SubCommand extends CommandX {
    private SubPlugin plugin;
    private String label;

    protected static NamedContainer<SubCommand, CommandX> newInstance(SubPlugin plugin, String command) {
        NamedContainer<SubCommand, CommandX> cmd = new NamedContainer<>(new SubCommand(plugin, command), null);
        CommandX now = cmd.name();
        //if (plugin.api.getGameVersion()[plugin.api.getGameVersion().length - 1].compareTo(new Version("1.13")) >= 0) { // TODO Future Command Validator API?
        //    now = new net.ME1312.SubServers.Bungee.Library.Compatibility.v1_13.CommandX(cmd.name());
        //}
        cmd.set(now);
        return cmd;
    }

    private SubCommand(SubPlugin plugin, String command) {
        super(command);
        this.plugin = plugin;
        this.label = '/' + command;
    }

    /**
     * Load /sub in console
     *
     * @param sender Sender
     * @param args Arguments
     */
    public void execute(CommandSender sender, String[] args) {
        if (sender instanceof ConsoleCommandSender) {
            if (args.length > 0) {
                if (args[0].equalsIgnoreCase("help") || args[0].equalsIgnoreCase("?")) {
                    sender.sendMessages(printHelp());
                } else if (args[0].equalsIgnoreCase("version") || args[0].equalsIgnoreCase("ver")) {
                    boolean build = false;
                    try {
                        Field f = Version.class.getDeclaredField("type");
                        f.setAccessible(true);
                        build = f.get(plugin.version) != VersionType.SNAPSHOT && SubPlugin.class.getPackage().getSpecificationTitle() != null;
                        f.setAccessible(false);
                    } catch (Exception e) {}

                    sender.sendMessage("SubServers > These are the platforms and versions that are running SubServers.Bungee:");
                    sender.sendMessage("  " + System.getProperty("os.name") + ' ' + System.getProperty("os.version") + ',');
                    sender.sendMessage("  Java " + System.getProperty("java.version") + ',');
                    sender.sendMessage("  " + plugin.getBungeeName() + ((plugin.isPatched)?" [Patched] ":" ") + net.md_5.bungee.Bootstrap.class.getPackage().getImplementationVersion() + ',');
                    sender.sendMessage("  SubServers.Bungee v" + SubPlugin.version.toExtendedString() + ((build)?" (" + SubPlugin.class.getPackage().getSpecificationTitle() + ')':""));
                    sender.sendMessage("");
                    new Thread(() -> {
                        try {
                            Document updxml = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new StringReader(Util.readAll(new BufferedReader(new InputStreamReader(new URL("https://src.me1312.net/maven/net/ME1312/SubServers/SubServers.Bungee/maven-metadata.xml").openStream(), Charset.forName("UTF-8")))))));

                            NodeList updnodeList = updxml.getElementsByTagName("version");
                            Version updversion = plugin.version;
                            int updcount = 0;
                            for (int i = 0; i < updnodeList.getLength(); i++) {
                                Node node = updnodeList.item(i);
                                if (node.getNodeType() == Node.ELEMENT_NODE) {
                                    if (!node.getTextContent().startsWith("-") && !node.getTextContent().equals(plugin.version.toString()) && Version.fromString(node.getTextContent()).compareTo(updversion) > 0) {
                                        updversion = Version.fromString(node.getTextContent());
                                        updcount++;
                                    }
                                }
                            }
                            if (updcount == 0) {
                                sender.sendMessage("You are on the latest version.");
                            } else {
                                sender.sendMessage("SubServers.Bungee v" + updversion + " is available. You are " + updcount + " version" + ((updcount == 1)?"":"s") + " behind.");
                            }
                        } catch (Exception e) {}
                    }).start();
                } else if (args[0].equalsIgnoreCase("reload")) {
                    if (args.length > 1) {
                        switch (args[1].toLowerCase()) {
                            case "all":
                            case "system":
                            case "bungee":
                            case "network":
                                plugin.getPluginManager().dispatchCommand(ConsoleCommandSender.getInstance(), "greload");
                                break;
                            case "host":
                            case "hosts":
                            case "server":
                            case "servers":
                            case "subserver":
                            case "subservers":
                            case "subdata":
                            case "config":
                            case "configs":
                                try {
                                    plugin.reload();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                break;
                            case "creator":
                            case "creators":
                            case "template":
                            case "templates":
                                for (Host host : plugin.api.getHosts().values()) {
                                    host.getCreator().reload();
                                }
                                sender.sendMessage("SubServers > SubCreator instances reloaded");
                                break;
                            default:
                                sender.sendMessage("SubServers > Unknown reload type: " + args[1]);
                        }
                    } else {
                        try {
                            plugin.reload();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                } else if (args[0].equalsIgnoreCase("list")) {
                    String div = ChatColor.RESET + ", ";
                    int i = 0;
                    boolean sent = false;
                    sender.sendMessage("SubServers > Group/Server List:");
                    if (plugin.api.getGroups().keySet().size() > 0) {
                        for (String group : plugin.api.getGroups().keySet()) {
                            String message = "";
                            message += ChatColor.GOLD + group + ChatColor.RESET + ": ";
                            List<String> names = new ArrayList<String>();
                            Map<String, Server> servers = plugin.api.getServers();
                            for (Server server : plugin.api.getGroup(group)) names.add(server.getName());
                            Collections.sort(names);
                            for (String name : names) {
                                if (i != 0) message += div;
                                Server server = servers.get(name.toLowerCase());
                                if (!(servers.get(name.toLowerCase()) instanceof SubServer)) {
                                    message += ChatColor.WHITE;
                                } else if (((SubServer) server).isTemporary()) {
                                    message += ChatColor.AQUA;
                                } else if (((SubServer) server).isRunning()) {
                                    message += ChatColor.GREEN;
                                } else if (((SubServer) server).isEnabled() && ((SubServer) server).getCurrentIncompatibilities().size() == 0) {
                                    message += ChatColor.YELLOW;
                                } else {
                                    message += ChatColor.RED;
                                }
                                message += server.getDisplayName() + " (" + server.getAddress().getAddress().getHostAddress() + ':' + server.getAddress().getPort() + ((server.getName().equals(server.getDisplayName())) ? "" : ChatColor.stripColor(div) + server.getName()) + ")";
                                i++;
                            }
                            if (i == 0) message += ChatColor.RESET + "(none)";
                            sender.sendMessage(message);
                            i = 0;
                            sent = true;
                        }
                        if (!sent) sender.sendMessage(ChatColor.RESET + "(none)");
                        sent = false;
                    }
                    sender.sendMessage("SubServers > Host/SubServer List:");
                    for (Host host : plugin.api.getHosts().values()) {
                        String message = "";
                        if (host.isEnabled()) {
                            message += ChatColor.AQUA;
                        } else {
                            message += ChatColor.RED;
                        }
                        message += host.getDisplayName() + " (" + host.getAddress().getHostAddress() + ((host.getName().equals(host.getDisplayName()))?"":ChatColor.stripColor(div)+host.getName()) + ")" + ChatColor.RESET + ": ";
                        for (SubServer subserver : host.getSubServers().values()) {
                            if (i != 0) message += div;
                            if (subserver.isTemporary()) {
                                message += ChatColor.AQUA;
                            } else if (subserver.isRunning()) {
                                message += ChatColor.GREEN;
                            } else if (subserver.isEnabled() && subserver.getCurrentIncompatibilities().size() == 0) {
                                message += ChatColor.YELLOW;
                            } else {
                                message += ChatColor.RED;
                            }
                            message += subserver.getDisplayName() + " (" + subserver.getAddress().getPort() + ((subserver.getName().equals(subserver.getDisplayName()))?"":ChatColor.stripColor(div)+subserver.getName()) + ")";
                            i++;
                        }
                        if (i == 0) message += ChatColor.RESET + "(none)";
                        sender.sendMessage(message);
                        i = 0;
                        sent = true;
                    }
                    if (!sent) sender.sendMessage(ChatColor.RESET + "(none)");
                    sender.sendMessage("SubServers > Server List:");
                    String message = "";
                    for (Server server : plugin.api.getServers().values()) {
                        if (!(server instanceof SubServer)) {
                            if (i != 0) message += div;
                            message += ChatColor.WHITE + server.getDisplayName() + " (" + server.getAddress().getAddress().getHostAddress() + ':' + server.getAddress().getPort() + ((server.getName().equals(server.getDisplayName()))?"":ChatColor.stripColor(div)+server.getName()) + ")";
                            i++;
                        }
                    }
                    if (i == 0) message += ChatColor.RESET + "(none)";
                    sender.sendMessage(message);
                } else if (args[0].equalsIgnoreCase("info") || args[0].equalsIgnoreCase("status")) {
                    if (args.length > 1) {
                        Map<String, Server> servers = plugin.api.getServers();
                        if (!servers.keySet().contains(args[1].toLowerCase())) {
                            sender.sendMessage("SubServers > There is no server with that name");
                        } else if (!(servers.get(args[1].toLowerCase()) instanceof SubServer)) {
                            sender.sendMessage("SubServers > That Server is not a SubServer");
                        } else {
                            SubServer server = (SubServer) servers.get(args[1].toLowerCase());
                            sender.sendMessage("SubServers > Info on " + server.getDisplayName() + ':');
                            if (!server.getName().equals(server.getDisplayName())) sender.sendMessage("  - Real Name: " + server.getName());
                            sender.sendMessage("  - Host: " + server.getHost().getDisplayName() + ((!server.getHost().getName().equals(server.getHost().getDisplayName()))?" ("+server.getHost().getName()+')':""));
                            sender.sendMessage("  - Enabled: " + ((server.isEnabled())?"yes":"no"));
                            sender.sendMessage("  - Editable: " + ((server.isEditable())?"yes":"no"));
                            if (server.getGroups().size() > 0) {
                                sender.sendMessage("  - Groups:");
                                for (String group : server.getGroups()) sender.sendMessage("    - " + group);
                            }
                            if (server.isTemporary()) sender.sendMessage("  - Temporary: yes");
                            sender.sendMessage("  - Running: " + ((server.isRunning())?"yes":"no"));
                            sender.sendMessage("  - Logging: " + ((server.isLogging())?"yes":"no"));
                            sender.sendMessage("  - Address: " + server.getAddress().getAddress().getHostAddress() + ':' + server.getAddress().getPort());
                            sender.sendMessage("  - Auto Restart: " + ((server.willAutoRestart())?"yes":"no"));
                            sender.sendMessage("  - Hidden: " + ((server.isHidden())?"yes":"no"));
                            if (server.getIncompatibilities().size() > 0) {
                                List<SubServer> current = server.getCurrentIncompatibilities();
                                sender.sendMessage("  - Incompatibilities:");
                                for (SubServer other : server.getIncompatibilities()) sender.sendMessage("    - " + other.getDisplayName() + ((current.contains(other))?"*":"") + ((!other.getName().equals(other.getDisplayName()))?" ("+other.getName()+')':""));
                            }
                            sender.sendMessage("  - Signature: " + server.getSignature());

                        }
                    } else {
                        sender.sendMessage("SubServers > Usage: " + label + " " + args[0].toLowerCase() + " <SubServer>");
                    }
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
                        } else if (((SubServer) servers.get(args[1].toLowerCase())).getCurrentIncompatibilities().size() != 0) {
                            String list = "";
                            for (SubServer server : ((SubServer) servers.get(args[1].toLowerCase())).getCurrentIncompatibilities()) {
                                if (list.length() != 0) list += ", ";
                                list += server.getName();
                            }
                            sender.sendMessages("That SubServer cannot start while these server(s) are running:", list);
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
                        if (!args[1].equals("*") && !servers.keySet().contains(args[1].toLowerCase())) {
                            sender.sendMessage("SubServers > There is no server with that name");
                        } else if (!args[1].equals("*") && !(servers.get(args[1].toLowerCase()) instanceof SubServer)) {
                            sender.sendMessage("SubServers > That Server is not a SubServer");
                        } else if (!args[1].equals("*") && !((SubServer) servers.get(args[1].toLowerCase())).isRunning()) {
                            sender.sendMessage("SubServers > That SubServer is not running");
                        } else {
                            String str = args[2];
                            for (int i = 3; i < args.length; i++) {
                                str += " " + args[i];
                            }
                            if (args[1].equals("*")) {
                                for (Server server : servers.values()) {
                                    if (server instanceof SubServer && ((SubServer) server).isRunning()) {
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
                } else if (args[0].equalsIgnoreCase("sudo") || args[0].equalsIgnoreCase("screen")) {
                    if (plugin.canSudo) {
                        if (args.length > 1) {
                            Map<String, Server> servers = plugin.api.getServers();
                            if (!servers.keySet().contains(args[1].toLowerCase())) {
                                sender.sendMessage("SubServers > There is no server with that name");
                            } else if (!(servers.get(args[1].toLowerCase()) instanceof SubServer)) {
                                sender.sendMessage("SubServers > That Server is not a SubServer");
                            } else if (!((SubServer) servers.get(args[1].toLowerCase())).isRunning()) {
                                sender.sendMessage("SubServers > That SubServer is not running");
                            } else {
                                plugin.sudo = (SubServer) servers.get(args[1].toLowerCase());
                                System.out.println("SubServers > Now forwarding commands to " + plugin.sudo.getDisplayName() + ". Type \"exit\" to return.");
                            }
                        } else {
                            sender.sendMessage("SubServers > Usage: " + label + " " + args[0].toLowerCase() + " <SubServer>");
                        }
                    } else {
                        sender.sendMessage("SubServers > The BungeeCord library provided does not support console sudo.");
                    }
                } else if (args[0].equalsIgnoreCase("create")) {
                    if (args.length > 5) {
                        if (plugin.api.getSubServers().keySet().contains(args[1].toLowerCase()) || SubCreator.isReserved(args[1])) {
                            sender.sendMessage("SubServers > There is already a SubServer with that name");
                        } else if (!plugin.hosts.keySet().contains(args[2].toLowerCase())) {
                            sender.sendMessage("SubServers > There is no host with that name");
                        } else if (!plugin.hosts.get(args[2].toLowerCase()).getCreator().getTemplates().keySet().contains(args[3].toLowerCase()) || !plugin.hosts.get(args[2].toLowerCase()).getCreator().getTemplate(args[3]).isEnabled()) {
                            sender.sendMessage("SubServers > There is no template with that name");
                        } else if (new Version("1.8").compareTo(new Version(args[4])) > 0) {
                            sender.sendMessage("SubServers > SubCreator cannot create servers before Minecraft 1.8");
                        } else if (Util.isException(() -> Integer.parseInt(args[5])) || Integer.parseInt(args[5]) <= 0 || Integer.parseInt(args[5]) > 65535) {
                            sender.sendMessage("SubServers > Invalid Port Number");
                        } else {
                            plugin.hosts.get(args[2].toLowerCase()).getCreator().create(args[1], plugin.hosts.get(args[2].toLowerCase()).getCreator().getTemplate(args[3]), new Version(args[4]), Integer.parseInt(args[5]));
                        }
                    } else {
                        sender.sendMessage("SubServers > Usage: " + label + " " + args[0].toLowerCase() + " <Name> <Host> <Template> <Version> <Port>");
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

    private String[] printHelp() {
        return new String[]{
                "SubServers > Console Command Help:",
                "   Help: /sub help",
                "   List: /sub list",
                "   Version: /sub version",
                "   Reload: /sub reload [all|configs|templates]",
                "   Server Info: /sub info <SubServer>",
                "   Start Server: /sub start <SubServer>",
                "   Stop Server: /sub stop <SubServer>",
                "   Terminate Server: /sub kill <SubServer>",
                "   Command Server: /sub cmd <SubServer> <Command> [Args...]",
                "   Sudo Server: /sub sudo <SubServer>",
                "   Create Server: /sub create <Name> <Host> <Template> <Version> <Port>",
                "   Remove Server: /sub delete <SubServer>",
                "",
                "   To see BungeeCord supplied commands, please visit:",
                "   https://www.spigotmc.org/wiki/bungeecord-commands/"
        };
    }

    /**
     * Suggest command arguments
     *
     * @param sender Sender
     * @param args Arguments
     * @return The validator's response and list of possible arguments
     */
    public NamedContainer<String, List<String>> suggestArguments(CommandSender sender, String[] args) {
        String last = (args.length > 0)?args[args.length - 1].toLowerCase():"";
        if (args.length <= 1) {
            List<String> cmds = Arrays.asList("help", "list", "info", "status", "version", "start", "stop", "kill", "terminate", "cmd", "command", "create");
            if (last.length() == 0) {
                return new NamedContainer<>(null, cmds);
            } else {
                List<String> list = new ArrayList<String>();
                for (String cmd : cmds) {
                    if (cmd.startsWith(last)) list.add(last + cmd.substring(last.length()));
                }
                return new NamedContainer<>((list.size() <= 0)?plugin.api.getLang("SubServers", "Command.Generic.Invalid-Subcommand").replace("$str$", args[0]):null, list);
            }
        } else {
            if (args[0].equals("info") || args[0].equals("status") ||
                    args[0].equals("start") ||
                    args[0].equals("stop") ||
                    args[0].equals("kill") || args[0].equals("terminate")) {
                    List<String> list = new ArrayList<String>();
                if (args.length == 2) {
                    if (last.length() == 0) {
                        for (SubServer server : plugin.api.getSubServers().values()) list.add(server.getName());
                    } else {
                        for (SubServer server : plugin.api.getSubServers().values()) {
                            if (server.getName().toLowerCase().startsWith(last))
                                list.add(last + server.getName().substring(last.length()));
                        }
                    }
                    return new NamedContainer<>((list.size() <= 0)?plugin.api.getLang("SubServers", "Command.Generic.Unknown-SubServer").replace("$str$", args[0]):null, list);
                } else {
                    return new NamedContainer<>(null, Collections.emptyList());
                }
            } else if (args[0].equals("cmd") || args[0].equals("command")) {
                if (args.length == 2) {
                    List<String> list = new ArrayList<String>();
                    if (last.length() == 0) {
                        for (SubServer server : plugin.api.getSubServers().values()) list.add(server.getName());
                    } else {
                        for (SubServer server : plugin.api.getSubServers().values()) {
                            if (server.getName().toLowerCase().startsWith(last)) list.add(last + server.getName().substring(last.length()));
                        }
                    }
                    return new NamedContainer<>((list.size() <= 0)?plugin.api.getLang("SubServers", "Command.Generic.Unknown-SubServer").replace("$str$", args[0]):null, list);
                } else if (args.length == 3) {
                    return new NamedContainer<>(null, Collections.singletonList("<Command>"));
                } else {
                    return new NamedContainer<>(null, Collections.singletonList("[Args...]"));
                }
            } else if (args[0].equals("create")) {
                if (args.length == 2) {
                    return new NamedContainer<>(null, Collections.singletonList("<Name>"));
                } else if (args.length == 3) {
                    List<String> list = new ArrayList<String>();
                    if (last.length() == 0) {
                        for (Host host : plugin.api.getHosts().values()) list.add(host.getName());
                    } else {
                        for (Host host : plugin.api.getHosts().values()) {
                            if (host.getName().toLowerCase().startsWith(last)) list.add(last + host.getName().substring(last.length()));
                        }
                    }
                    return new NamedContainer<>((list.size() <= 0)?plugin.api.getLang("SubServers", "Command.Generic.Unknown-Host").replace("$str$", args[0]):null, list);
                } else if (args.length == 4) {
                    List<String> list = new ArrayList<String>();
                    Map<String, Host> hosts = plugin.api.getHosts();
                    if (!hosts.keySet().contains(args[2].toLowerCase())) {
                        list.add("<Template>");
                    } else if (last.length() == 0) {
                        for (SubCreator.ServerTemplate template : hosts.get(args[2].toLowerCase()).getCreator().getTemplates().values()) list.add(template.getName());
                    } else {
                        for (SubCreator.ServerTemplate template : hosts.get(args[2].toLowerCase()).getCreator().getTemplates().values()) {
                            if (template.getName().toLowerCase().startsWith(last)) list.add(last + template.getName().substring(last.length()));
                        }
                    }
                    return new NamedContainer<>((list.size() <= 0)?plugin.api.getLang("SubServers", "Command.Creator.Invalid-Template").replace("$str$", args[0]):null, list);
                } else if (args.length == 5) {
                    if (last.length() > 0) {
                        if (new Version("1.8").compareTo(new Version(last)) > 0) {
                            return new NamedContainer<>(plugin.api.getLang("SubServers", "Command.Creator.Invalid-Version"), Collections.emptyList());
                        }
                    }
                    return new NamedContainer<>(null, Collections.singletonList("<Version>"));
                } else if (args.length == 6) {
                    if (last.length() > 0) {
                        if (Util.isException(() -> Integer.parseInt(last)) || Integer.parseInt(last) <= 0 || Integer.parseInt(last) > 65535) {
                            return new NamedContainer<>(plugin.api.getLang("SubServers", "Command.Creator.Invalid-Port"), Collections.emptyList());
                        }
                    }
                    return new NamedContainer<>(null, Collections.singletonList("<Port>"));
                } else {
                    return new NamedContainer<>(null, Collections.emptyList());
                }
            } else {
                return new NamedContainer<>(plugin.api.getLang("SubServers", "Command.Generic.Invalid-Subcommand").replace("$str$", args[0]), Collections.emptyList());
            }
        }
    }

    /**
     * BungeeCord /server
     */
    public static final class BungeeServer extends CommandX {
        private SubPlugin plugin;
        private BungeeServer(SubPlugin plugin, String command) {
            super(command, "bungeecord.command.server");
            this.plugin = plugin;
        }

        protected static NamedContainer<BungeeServer, CommandX> newInstance(SubPlugin plugin, String command) {
            NamedContainer<BungeeServer, CommandX> cmd = new NamedContainer<>(new BungeeServer(plugin, command), null);
            CommandX now = cmd.name();
            //if (plugin.api.getGameVersion()[plugin.api.getGameVersion().length - 1].compareTo(new Version("1.13")) >= 0) { // TODO Future Command Validator API?
            //    now = new net.ME1312.SubServers.Bungee.Library.Compatibility.v1_13.CommandX(cmd.name());
            //}
            cmd.set(now);
            return cmd;
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
                        sender.sendMessage(plugin.api.getLang("SubServers", "Bungee.Server.Invalid"));
                    }
                } else {
                    int i = 0;
                    TextComponent serverm = new TextComponent(ChatColor.RESET.toString());
                    TextComponent div = new TextComponent(plugin.api.getLang("SubServers", "Bungee.Server.Divider"));
                    for (Server server : plugin.api.getServers().values()) {
                        if (!server.isHidden() && (!(server instanceof SubServer) || ((SubServer) server).isRunning())) {
                            if (i != 0) serverm.addExtra(div);
                            TextComponent message = new TextComponent(plugin.api.getLang("SubServers", "Bungee.Server.List").replace("$str$", server.getDisplayName()));
                            try {
                                message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponent[]{new TextComponent(plugin.api.getLang("SubServers", "Bungee.Server.Hover").replace("$int$", Integer.toString(server.getGlobalPlayers().size())))}));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            message.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/server " + server.getName()));
                            serverm.addExtra(message);
                            i++;
                        }
                    }
                    sender.sendMessages(
                            plugin.api.getLang("SubServers", "Bungee.Server.Current").replace("$str$", ((ProxiedPlayer) sender).getServer().getInfo().getName()),
                            plugin.api.getLang("SubServers", "Bungee.Server.Available"));
                    sender.sendMessage(serverm);
                }
            } else {
                sender.sendMessage(plugin.api.getLang("SubServers", "Command.Generic.Player-Only"));
            }
        }

        /**
         * Suggest command arguments
         *
         * @param sender Sender
         * @param args Arguments
         * @return The validator's response and list of possible arguments
         */
        public NamedContainer<String, List<String>> suggestArguments(CommandSender sender, String[] args) {
            if (args.length <= 1) {
                String last = (args.length > 0)?args[args.length - 1].toLowerCase():"";
                if (last.length() == 0) {
                    return new NamedContainer<>(null, new LinkedList<>(plugin.getServers().keySet()));
                } else {
                    List<String> list = new ArrayList<String>();
                    for (String server : plugin.getServers().keySet()) {
                        if (server.toLowerCase().startsWith(last)) list.add(server);
                    }
                    return new NamedContainer<>((list.size() <= 0)?plugin.api.getLang("SubServers", "Bungee.Server.Invalid").replace("$str$", args[0]):null, list);
                }
            } else {
                return new NamedContainer<>(null, Collections.emptyList());
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
                List<String> playerlist = new ArrayList<String>();
                for (NamedContainer<String, UUID> player : server.getGlobalPlayers()) playerlist.add(player.name());
                Collections.sort(playerlist);

                players += playerlist.size();
                if (!server.isHidden() && (!(server instanceof SubServer) || ((SubServer) server).isRunning())) {
                    int i = 0;
                    String message = plugin.api.getLang("SubServers", "Bungee.List.Format").replace("$str$", server.getDisplayName()).replace("$int$", Integer.toString(playerlist.size()));
                    for (String player : playerlist) {
                        if (i != 0) message += plugin.api.getLang("SubServers", "Bungee.List.Divider");
                        message += plugin.api.getLang("SubServers", "Bungee.List.List").replace("$str$", player);
                        i++;
                    }
                    messages.add(message);
                }
            }
            sender.sendMessages(messages.toArray(new String[messages.size()]));
            sender.sendMessage(plugin.api.getLang("SubServers", "Bungee.List.Total").replace("$int$", Integer.toString(players)));
        }
    }
}

package net.ME1312.SubServers.Bungee;

import com.google.gson.Gson;
import net.ME1312.SubServers.Bungee.Host.*;
import net.ME1312.SubServers.Bungee.Library.Compatibility.CommandX;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.NamedContainer;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.SubData.Server.ClientHandler;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.command.ConsoleCommandSender;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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
        //    now = new net.ME1312.SubServers.Bungee.Library.Compatibility.mc1_13.CommandX(cmd.name());
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
    @SuppressWarnings("unchecked")
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof ProxiedPlayer)) {
            if (args.length > 0) {
                if (args[0].equalsIgnoreCase("help") || args[0].equalsIgnoreCase("?")) {
                    sender.sendMessages(printHelp());
                } else if (args[0].equalsIgnoreCase("version") || args[0].equalsIgnoreCase("ver")) {
                    String osarch;
                    if (System.getProperty("os.name").toLowerCase().startsWith("windows")) {
                        String arch = System.getenv("PROCESSOR_ARCHITECTURE");
                        String wow64Arch = System.getenv("PROCESSOR_ARCHITEW6432");

                        osarch = arch != null && arch.endsWith("64") || wow64Arch != null && wow64Arch.endsWith("64")?"x64":"x86";
                    } else if (System.getProperty("os.arch").endsWith("86")) {
                        osarch = "x86";
                    } else if (System.getProperty("os.arch").endsWith("64")) {
                        osarch = "x64";
                    } else {
                        osarch = System.getProperty("os.arch");
                    }

                    String javaarch = null;
                    switch (System.getProperty("sun.arch.data.model")) {
                        case "32":
                            javaarch = "x86";
                            break;
                        case "64":
                            javaarch = "x64";
                            break;
                        default:
                            if (!System.getProperty("sun.arch.data.model").equalsIgnoreCase("unknown"))
                                javaarch = System.getProperty("sun.arch.data.model");
                    }

                    sender.sendMessage("SubServers > These are the platforms and versions that are running SubServers.Bungee:");
                    sender.sendMessage("  " + System.getProperty("os.name") + ((!System.getProperty("os.name").toLowerCase().startsWith("windows"))?' ' + System.getProperty("os.version"):"") + ((osarch != null)?" [" + osarch + ']':"") + ',');
                    sender.sendMessage("  Java " + System.getProperty("java.version") + ((javaarch != null)?" [" + javaarch + ']':"") + ',');
                    sender.sendMessage("  " + plugin.getBungeeName() + ((plugin.isPatched)?" [Patched] ":" ") + net.md_5.bungee.Bootstrap.class.getPackage().getImplementationVersion() + ',');
                    sender.sendMessage("  SubServers.Bungee v" + SubPlugin.version.toExtendedString() + ((plugin.api.getWrapperBuild() != null)?" (" + plugin.api.getWrapperBuild() + ')':""));
                    sender.sendMessage("");
                    new Thread(() -> {
                        try {
                            ObjectMap<String> tags = new ObjectMap<String>(new Gson().fromJson("{\"tags\":" + Util.readAll(new BufferedReader(new InputStreamReader(new URL("https://api.github.com/repos/ME1312/SubServers-2/git/refs/tags").openStream(), Charset.forName("UTF-8")))) + '}', Map.class));
                            List<Version> versions = new LinkedList<Version>();

                            Version updversion = plugin.version;
                            int updcount = 0;
                            for (ObjectMap<String> tag : tags.getMapList("tags")) versions.add(Version.fromString(tag.getString("ref").substring(10)));
                            Collections.sort(versions);
                            for (Version version : versions) {
                                if (version.compareTo(updversion) > 0) {
                                    updversion = version;
                                    updcount++;
                                }
                            }
                            if (updcount == 0) {
                                sender.sendMessage("You are on the latest version.");
                            } else {
                                sender.sendMessage("SubServers.Bungee v" + updversion + " is available. You are " + updcount + " version" + ((updcount == 1)?"":"s") + " behind.");
                            }
                        } catch (Exception e) {}
                    }, "SubServers.Bungee::Update_Check").start();
                } else if (args[0].equalsIgnoreCase("reload")) {
                    if (args.length > 1) {
                        switch (args[1].toLowerCase()) {
                            case "*":
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
                    if (plugin.api.getGroups().keySet().size() > 0) {
                        sender.sendMessage("SubServers > Group/Server List:");
                        for (String group : plugin.api.getGroups().keySet()) {
                            String message = "  ";
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
                                } else if (((SubServer) server).isRunning()) {
                                    if (((SubServer) server).getStopAction() == SubServer.StopAction.REMOVE_SERVER || ((SubServer) server).getStopAction() == SubServer.StopAction.RECYCLE_SERVER || ((SubServer) server).getStopAction() == SubServer.StopAction.DELETE_SERVER) {
                                        message += ChatColor.AQUA;
                                    } else {
                                        message += ChatColor.GREEN;
                                    }
                                } else if (((SubServer) server).getHost().isAvailable() && ((SubServer) server).getHost().isEnabled() && ((SubServer) server).isEnabled() && ((SubServer) server).getCurrentIncompatibilities().size() == 0) {
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
                        String message = "  ";
                        if (host.isAvailable() && host.isEnabled()) {
                            message += ChatColor.AQUA;
                        } else {
                            message += ChatColor.RED;
                        }
                        message += host.getDisplayName() + " (" + host.getAddress().getHostAddress() + ((host.getName().equals(host.getDisplayName()))?"":ChatColor.stripColor(div)+host.getName()) + ")" + ChatColor.RESET + ": ";
                        for (SubServer subserver : host.getSubServers().values()) {
                            if (i != 0) message += div;
                            if (subserver.isRunning()) {
                                if (subserver.getStopAction() == SubServer.StopAction.REMOVE_SERVER || subserver.getStopAction() == SubServer.StopAction.RECYCLE_SERVER || subserver.getStopAction() == SubServer.StopAction.DELETE_SERVER) {
                                    message += ChatColor.AQUA;
                                } else {
                                    message += ChatColor.GREEN;
                                }
                            } else if (subserver.getHost().isEnabled() && subserver.isEnabled() && subserver.getCurrentIncompatibilities().size() == 0) {
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
                    String message = "  ";
                    for (Server server : plugin.api.getServers().values()) {
                        if (!(server instanceof SubServer)) {
                            if (i != 0) message += div;
                            message += ChatColor.WHITE + server.getDisplayName() + " (" + server.getAddress().getAddress().getHostAddress() + ':' + server.getAddress().getPort() + ((server.getName().equals(server.getDisplayName()))?"":ChatColor.stripColor(div)+server.getName()) + ")";
                            i++;
                        }
                    }
                    if (i == 0) message += ChatColor.RESET + "(none)";
                    sender.sendMessage(message);
                    if (plugin.api.getProxies().keySet().size() > 0) {
                        sender.sendMessage("SubServers > Proxy List:");
                        message = "  (master)";
                        for (Proxy proxy : plugin.api.getProxies().values()) {
                            message += div;
                            if (proxy.getSubData()[0] != null && proxy.isRedis()) {
                                message += ChatColor.GREEN;
                            } else if (proxy.getSubData()[0] != null) {
                                message += ChatColor.AQUA;
                            } else if (proxy.isRedis()) {
                                message += ChatColor.WHITE;
                            } else {
                                message += ChatColor.RED;
                            }
                            message += proxy.getDisplayName() + ((proxy.getName().equals(proxy.getDisplayName()))?"":" ("+proxy.getName()+')');
                        }
                        sender.sendMessage(message);
                    }
                } else if (args[0].equalsIgnoreCase("info") || args[0].equalsIgnoreCase("status")) {
                    if (args.length > 1) {
                        String type = (args.length > 2)?args[1]:null;
                        String name = args[(type != null)?2:1];

                        Runnable getServer = () -> {
                            Server server = plugin.api.getServer(name);
                            if (server != null) {
                                sender.sendMessage("SubServers > Info on " + ((server instanceof SubServer)?"Sub":"") + "Server: " + ChatColor.WHITE + server.getDisplayName());
                                if (!server.getName().equals(server.getDisplayName())) sender.sendMessage(" -> System Name: " + ChatColor.WHITE  + server.getName());
                                if (server instanceof SubServer) {
                                    sender.sendMessage(" -> Enabled: " + ((((SubServer) server).isEnabled())?ChatColor.GREEN+"yes":ChatColor.RED+"no"));
                                    if (!((SubServer) server).isEditable()) sender.sendMessage(" -> Editable: " + ChatColor.RED + "no");
                                    sender.sendMessage(" -> Host: " + ChatColor.WHITE  + ((SubServer) server).getHost().getName());
                                }
                                if (server.getGroups().size() > 0) sender.sendMessage(" -> Group" + ((server.getGroups().size() > 1)?"s:":": " + ChatColor.WHITE + server.getGroups().get(0)));
                                if (server.getGroups().size() > 1) for (String group : server.getGroups()) sender.sendMessage("      - " + ChatColor.WHITE + group);
                                sender.sendMessage(" -> Address: " + ChatColor.WHITE + server.getAddress().getAddress().getHostAddress()+':'+server.getAddress().getPort());
                                if (server instanceof SubServer) sender.sendMessage(" -> Running: " + ((((SubServer) server).isRunning())?ChatColor.GREEN+"yes":ChatColor.RED+"no"));
                                if (!(server instanceof SubServer) || ((SubServer) server).isRunning()) {
                                    sender.sendMessage(" -> Connected: " + ((server.getSubData()[0] != null)?ChatColor.GREEN+"yes"+((server.getSubData().length > 1)?ChatColor.AQUA+" +"+(server.getSubData().length-1):""):ChatColor.RED+"no"));
                                    sender.sendMessage(" -> Players: " + ChatColor.AQUA + server.getPlayers().size() + " online");
                                }
                                sender.sendMessage(" -> MOTD: " + ChatColor.WHITE + ChatColor.stripColor(server.getMotd()));
                                if (server instanceof SubServer && ((SubServer) server).getStopAction() != SubServer.StopAction.NONE) sender.sendMessage(" -> Stop Action: " + ChatColor.WHITE + ((SubServer) server).getStopAction().toString());
                                sender.sendMessage(" -> Signature: " + ChatColor.AQUA + server.getSignature());
                                if (server instanceof SubServer) sender.sendMessage(" -> Logging: " + ((((SubServer) server).isLogging())?ChatColor.GREEN+"yes":ChatColor.RED+"no"));
                                sender.sendMessage(" -> Restricted: " + ((server.isRestricted())?ChatColor.GREEN+"yes":ChatColor.RED+"no"));
                                if (server instanceof SubServer && ((SubServer) server).getIncompatibilities().size() > 0) {
                                    List<String> current = new ArrayList<String>();
                                    for (SubServer other : ((SubServer) server).getCurrentIncompatibilities()) current.add(other.getName().toLowerCase());
                                    sender.sendMessage(" -> Incompatibilities:");
                                    for (SubServer other : ((SubServer) server).getIncompatibilities()) sender.sendMessage("      - " + ((current.contains(other.getName().toLowerCase()))?ChatColor.WHITE:ChatColor.GRAY) + other);
                                }
                                sender.sendMessage(" -> Hidden: " + ((server.isHidden())?ChatColor.GREEN+"yes":ChatColor.RED+"no"));
                            } else {
                                if (type == null) {
                                    sender.sendMessage("SubServers > There is no object with that name");
                                } else {
                                    sender.sendMessage("SubServers > There is no server with that name");
                                }
                            }
                        };
                        Runnable getGroup = () -> {
                            List<Server> group = plugin.api.getGroup(name);
                            if (group != null) {
                                sender.sendMessage("SubServers > Info on Group: " + ChatColor.WHITE + name);
                                sender.sendMessage(" -> Servers: " + ((group.size() <= 0)?ChatColor.GRAY + "(none)":ChatColor.AQUA.toString() + group.size()));
                                for (Server server : group) sender.sendMessage("      - " + ChatColor.WHITE + server.getDisplayName() + ((server.getName().equals(server.getDisplayName()))?"":" ("+server.getName()+')'));
                            } else {
                                if (type == null) {
                                    getServer.run();
                                } else {
                                    sender.sendMessage("SubServers > There is no group with that name");
                                }
                            }
                        };
                        Runnable getHost = () -> {
                            Host host = plugin.api.getHost(name);
                            if (host != null) {
                                sender.sendMessage("SubServers > Info on Host: " + ChatColor.WHITE + host.getDisplayName());
                                if (!host.getName().equals(host.getDisplayName())) sender.sendMessage(" -> System Name: " + ChatColor.WHITE  + host.getName());
                                sender.sendMessage(" -> Available: " + ((host.isAvailable())?ChatColor.GREEN+"yes":ChatColor.RED+"no"));
                                sender.sendMessage(" -> Enabled: " + ((host.isEnabled())?ChatColor.GREEN+"yes":ChatColor.RED+"no"));
                                sender.sendMessage(" -> Address: " + ChatColor.WHITE + host.getAddress().getHostAddress());
                                if (host instanceof ClientHandler) sender.sendMessage(" -> Connected: " + ((((ClientHandler) host).getSubData()[0] != null)?ChatColor.GREEN+"yes"+((((ClientHandler) host).getSubData().length > 1)?ChatColor.AQUA+" +"+(((ClientHandler) host).getSubData().length-1):""):ChatColor.RED+"no"));
                                sender.sendMessage(" -> SubServers: " + ((host.getSubServers().keySet().size() <= 0)?ChatColor.GRAY + "(none)":ChatColor.AQUA.toString() + host.getSubServers().keySet().size()));
                                for (SubServer subserver : host.getSubServers().values()) sender.sendMessage("      - " + ((subserver.isEnabled())?ChatColor.WHITE:ChatColor.GRAY) + subserver.getDisplayName() + ((subserver.getName().equals(subserver.getDisplayName()))?"":" ("+subserver.getName()+')'));
                                sender.sendMessage(" -> Templates: " + ((host.getCreator().getTemplates().keySet().size() <= 0)?ChatColor.GRAY + "(none)":ChatColor.AQUA.toString() + host.getCreator().getTemplates().keySet().size()));
                                for (SubCreator.ServerTemplate template : host.getCreator().getTemplates().values()) sender.sendMessage("      - " + ((template.isEnabled())?ChatColor.WHITE:ChatColor.GRAY) + template.getDisplayName() + ((template.getName().equals(template.getDisplayName()))?"":" ("+template.getName()+')'));
                                sender.sendMessage(" -> Signature: " + ChatColor.AQUA + host.getSignature());
                            } else {
                                if (type == null) {
                                    getGroup.run();
                                } else {
                                    sender.sendMessage("SubServers > There is no host with that name");
                                }
                            }
                        };
                        Runnable getProxy = () -> {
                            Proxy proxy = plugin.api.getProxy(name);
                            if (proxy != null) {
                                sender.sendMessage("SubServers > Info on Proxy: " + ChatColor.WHITE + proxy.getDisplayName());
                                if (!proxy.getName().equals(proxy.getDisplayName())) sender.sendMessage(" -> System Name: " + ChatColor.WHITE  + proxy.getName());
                                sender.sendMessage(" -> Connected: " + ((proxy.getSubData()[0] != null)?ChatColor.GREEN+"yes"+((proxy.getSubData().length > 1)?ChatColor.AQUA+" +"+(proxy.getSubData().length-1):""):ChatColor.RED+"no"));
                                sender.sendMessage(" -> Redis: "  + ((proxy.isRedis())?ChatColor.GREEN:ChatColor.RED+"un") + "available");
                                if (proxy.isRedis()) sender.sendMessage(" -> Players: " + ChatColor.AQUA + proxy.getPlayers().size() + " online");
                                sender.sendMessage(" -> Signature: " + ChatColor.AQUA + proxy.getSignature());
                            } else {
                                if (type == null) {
                                    getHost.run();
                                } else {
                                    sender.sendMessage("SubServers > There is no proxy with that name");
                                }
                            }
                        };

                        if (type == null) {
                            getProxy.run();
                        } else {
                            switch (type.toLowerCase()) {
                                case "p":
                                case "proxy":
                                    getProxy.run();
                                    break;
                                case "h":
                                case "host":
                                    getHost.run();
                                    break;
                                case "g":
                                case "group":
                                    getGroup.run();
                                    break;
                                case "s":
                                case "server":
                                case "subserver":
                                    getServer.run();
                                    break;
                                default:
                                    sender.sendMessage("SubServers > There is no object type with that name");
                            }
                        }
                    } else {
                        sender.sendMessage("SubServers > Usage: " + label + " " + args[0].toLowerCase() + " [proxy|host|group|server] <Name>");
                    }
                } else if (args[0].equalsIgnoreCase("start")) {
                    if (args.length > 1) {
                        Map<String, Server> servers = plugin.api.getServers();
                        if (!servers.keySet().contains(args[1].toLowerCase())) {
                            sender.sendMessage("SubServers > There is no server with that name");
                        } else if (!(servers.get(args[1].toLowerCase()) instanceof SubServer)) {
                            sender.sendMessage("SubServers > That Server is not a SubServer");
                        } else if (!((SubServer) servers.get(args[1].toLowerCase())).getHost().isAvailable()) {
                            sender.sendMessage("SubServers > That SubServer's Host is not available");
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
                } else if (args[0].equalsIgnoreCase("restart")) {
                    if (args.length > 1) {
                        Runnable starter = () -> {
                            Map<String, Server> servers = plugin.api.getServers();
                            if (!servers.keySet().contains(args[1].toLowerCase()) || !(servers.get(args[1].toLowerCase()) instanceof SubServer)) {
                                sender.sendMessage("SubServers > Could not restart server: That SubServer has disappeared");
                            } else if (!((SubServer) servers.get(args[1].toLowerCase())).getHost().isAvailable()) {
                                sender.sendMessage("SubServers > Could not restart server: That SubServer's Host is no longer available");
                            } else if (!((SubServer) servers.get(args[1].toLowerCase())).getHost().isEnabled()) {
                                sender.sendMessage("SubServers > Could not restart server: That SubServer's Host is no longer enabled");
                            } else if (!((SubServer) servers.get(args[1].toLowerCase())).isEnabled()) {
                                sender.sendMessage("SubServers > Could not restart server: That SubServer is no longer enabled");
                            } else if (!((SubServer) servers.get(args[1].toLowerCase())).isRunning()) {
                                if (((SubServer) servers.get(args[1].toLowerCase())).getCurrentIncompatibilities().size() != 0) {
                                    String list = "";
                                    for (SubServer server : ((SubServer) servers.get(args[1].toLowerCase())).getCurrentIncompatibilities()) {
                                        if (list.length() != 0) list += ", ";
                                        list += server.getName();
                                    }
                                    sender.sendMessages("Could not restart server: That SubServer cannot start while these server(s) are running:", list);
                                } else {
                                    ((SubServer) servers.get(args[1].toLowerCase())).start();
                                }
                            }
                        };

                        Map<String, Server> servers = plugin.api.getServers();
                        if (!servers.keySet().contains(args[1].toLowerCase())) {
                            sender.sendMessage("SubServers > There is no server with that name");
                        } else if (!(servers.get(args[1].toLowerCase()) instanceof SubServer)) {
                            sender.sendMessage("SubServers > That Server is not a SubServer");
                        } else if (((SubServer) servers.get(args[1].toLowerCase())).isRunning()) {
                            new Thread(() -> {
                                try {
                                    ((SubServer) servers.get(args[1].toLowerCase())).stop();
                                    ((SubServer) servers.get(args[1].toLowerCase())).waitFor();
                                    Thread.sleep(100);
                                    starter.run();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }, "SubServers.Bungee::Server_Restart_Command_Handler(" + servers.get(args[1].toLowerCase()).getName() + ')').start();
                        } else {
                            starter.run();
                        }
                    } else {
                        sender.sendMessage("SubServers > Usage: " + label + " " + args[0].toLowerCase() + " <SubServer>");
                    }
                } else if (args[0].equalsIgnoreCase("stop")) {
                    if (args.length > 1) {
                        Map<String, Server> servers = plugin.api.getServers();
                        if (!args[1].equals("*") && !servers.keySet().contains(args[1].toLowerCase())) {
                            sender.sendMessage("SubServers > There is no server with that name");
                        } else if (!args[1].equals("*") && !(servers.get(args[1].toLowerCase()) instanceof SubServer)) {
                            sender.sendMessage("SubServers > That Server is not a SubServer");
                        } else if (!args[1].equals("*") && !((SubServer) servers.get(args[1].toLowerCase())).isRunning()) {
                            sender.sendMessage("SubServers > That SubServer is not running");
                        } else if (args[1].equals("*")) {
                            for (Server server : servers.values()) {
                                if (server instanceof SubServer && ((SubServer) server).isRunning()) {
                                    ((SubServer) server).stop();
                                }
                            }
                        } else {
                            ((SubServer) servers.get(args[1].toLowerCase())).stop();
                        }
                    } else {
                        sender.sendMessage("SubServers > Usage: " + label + " " + args[0].toLowerCase() + " <SubServer>");
                    }
                } else if (args[0].equalsIgnoreCase("kill") || args[0].equalsIgnoreCase("terminate")) {
                    if (args.length > 1) {
                        Map<String, Server> servers = plugin.api.getServers();
                        if (!args[1].equals("*") && !servers.keySet().contains(args[1].toLowerCase())) {
                            sender.sendMessage("SubServers > There is no server with that name");
                        } else if (!args[1].equals("*") && !(servers.get(args[1].toLowerCase()) instanceof SubServer)) {
                            sender.sendMessage("SubServers > That Server is not a SubServer");
                        } else if (!args[1].equals("*") && !((SubServer) servers.get(args[1].toLowerCase())).isRunning()) {
                            sender.sendMessage("SubServers > That SubServer is not running");
                        } else if (args[1].equals("*")) {
                            for (Server server : servers.values()) {
                                if (server instanceof SubServer && ((SubServer) server).isRunning()) {
                                    ((SubServer) server).terminate();
                                }
                            }
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
                    if (args.length > 3) {
                        if (plugin.api.getSubServers().keySet().contains(args[1].toLowerCase()) || SubCreator.isReserved(args[1])) {
                            sender.sendMessage("SubServers > There is already a SubServer with that name");
                        } else if (!plugin.hosts.keySet().contains(args[2].toLowerCase())) {
                            sender.sendMessage("SubServers > There is no host with that name");
                        } else if (!plugin.hosts.get(args[2].toLowerCase()).isAvailable()) {
                            sender.sendMessage("SubServers > That Host is not available");
                        } else if (!plugin.hosts.get(args[2].toLowerCase()).isEnabled()) {
                            sender.sendMessage("SubServers > That Host is not enabled");
                        } else if (!plugin.hosts.get(args[2].toLowerCase()).getCreator().getTemplates().keySet().contains(args[3].toLowerCase())) {
                            sender.sendMessage("SubServers > There is no template with that name");
                        } else if (!plugin.hosts.get(args[2].toLowerCase()).getCreator().getTemplate(args[3]).isEnabled()) {
                            sender.sendMessage("SubServers > That Template is not enabled");
                        } else if (args.length <= 4 && plugin.hosts.get(args[2].toLowerCase()).getCreator().getTemplate(args[3]).requiresVersion()) {
                            sender.sendMessage("SubServers > That Template requires a Minecraft Version to be specified");
                        } else if (args.length > 5 && (Util.isException(() -> Integer.parseInt(args[5])) || Integer.parseInt(args[5]) <= 0 || Integer.parseInt(args[5]) > 65535)) {
                            sender.sendMessage("SubServers > Invalid Port Number");
                        } else {
                            plugin.hosts.get(args[2].toLowerCase()).getCreator().create(args[1], plugin.hosts.get(args[2].toLowerCase()).getCreator().getTemplate(args[3]), (args.length > 4)?new Version(args[4]):null, (args.length > 5)?Integer.parseInt(args[5]):null);
                        }
                    } else {
                        sender.sendMessage("SubServers > Usage: " + label + " " + args[0].toLowerCase() + " <Name> <Host> <Template> [Version] [Port]");
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
                            } else if (!((SubServer) servers.get(args[1].toLowerCase())).getHost().recycleSubServer(args[1].toLowerCase())){
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
                "   Reload: /sub reload [all|config|templates]",
                "   Info: /sub info [proxy|host|group|server] <Name>",
                "   Start Server: /sub start <SubServer>",
                "   Restart Server: /sub restart <SubServer>",
                "   Stop Server: /sub stop <SubServer>",
                "   Terminate Server: /sub kill <SubServer>",
                "   Command Server: /sub cmd <SubServer> <Command> [Args...]",
                "   Sudo Server: /sub sudo <SubServer>",
                "   Create Server: /sub create <Name> <Host> <Template> [Version] [Port]",
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
            List<String> cmds = new ArrayList<>();
            cmds.addAll(Arrays.asList("help", "list", "info", "status", "version", "start", "stop", "restart", "kill", "terminate", "cmd", "command", "create"));
            if (!(sender instanceof ProxiedPlayer)) cmds.addAll(Arrays.asList("reload", "sudo", "screen", "delete"));
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
            if (args[0].equals("info") || args[0].equals("status")) {
                if (args.length == 2) {
                    List<String> list = new ArrayList<String>();
                    List<String> subcommands = new ArrayList<String>();
                    subcommands.add("proxy");
                    subcommands.add("host");
                    subcommands.add("group");
                    subcommands.add("server");
                    if (last.length() == 0) {
                        list.addAll(subcommands);
                        for (Proxy proxy : plugin.api.getProxies().values()) if (!list.contains(proxy.getName())) list.add(proxy.getName());
                        for (Host host : plugin.api.getHosts().values()) if (!list.contains(host.getName())) list.add(host.getName());
                        for (String group : plugin.api.getGroups().keySet()) if (!list.contains(group)) list.add(group);
                        for (Server server : plugin.api.getServers().values()) if (!list.contains(server.getName())) list.add(server.getName());
                    } else {
                        for (String command : subcommands) {
                            if (!list.contains(command) && command.toLowerCase().startsWith(last))
                                list.add(last + command.substring(last.length()));
                        }
                        for (Proxy proxy : plugin.api.getProxies().values()) {
                            if (!list.contains(proxy.getName()) && proxy.getName().toLowerCase().startsWith(last))
                                list.add(last + proxy.getName().substring(last.length()));
                        }
                        for (Host host : plugin.api.getHosts().values()) {
                            if (!list.contains(host.getName()) && host.getName().toLowerCase().startsWith(last))
                                list.add(last + host.getName().substring(last.length()));
                        }
                        for (String group : plugin.api.getGroups().keySet()) {
                            if (!list.contains(group) && group.toLowerCase().startsWith(last))
                                list.add(last + group.substring(last.length()));
                        }
                        for (Server server : plugin.api.getServers().values()) {
                            if (!list.contains(server.getName()) && server.getName().toLowerCase().startsWith(last))
                                list.add(last + server.getName().substring(last.length()));
                        }
                    }
                    return new NamedContainer<>((list.size() <= 0)?plugin.api.getLang("SubServers", "Command.Info.Unknown").replace("$str$", args[0]):null, list);
                } else if (args.length == 3) {
                    List<String> list = new ArrayList<String>();
                    if (last.length() == 0) {
                        switch (args[1].toLowerCase()) {
                            case "p":
                            case "proxy":
                                for (Proxy proxy : plugin.api.getProxies().values()) list.add(proxy.getName());
                                break;
                            case "h":
                            case "host":
                                for (Host host : plugin.api.getHosts().values()) list.add(host.getName());
                                break;
                            case "g":
                            case "group":
                                list.addAll(plugin.api.getGroups().keySet());
                                break;
                            case "s":
                            case "server":
                            case "subserver":
                                for (Server server : plugin.api.getServers().values()) list.add(server.getName());
                                break;
                        }
                    } else {
                        switch (args[1].toLowerCase()) {
                            case "p":
                            case "proxy":
                                for (Proxy proxy : plugin.api.getProxies().values()) {
                                    if (!list.contains(proxy.getName()) && proxy.getName().toLowerCase().startsWith(last))
                                        list.add(last + proxy.getName().substring(last.length()));
                                }
                                break;
                            case "h":
                            case "host":
                                for (Host host : plugin.api.getHosts().values()) {
                                    if (host.getName().toLowerCase().startsWith(last))
                                        list.add(last + host.getName().substring(last.length()));
                                }
                                break;
                            case "g":
                            case "group":
                                for (String group : plugin.api.getGroups().keySet()) {
                                    if (group.toLowerCase().startsWith(last))
                                        list.add(last + group.substring(last.length()));
                                }
                                break;
                            case "s":
                            case "server":
                            case "subserver":
                                for (Server server : plugin.api.getServers().values()) {
                                    if (server.getName().toLowerCase().startsWith(last))
                                        list.add(last + server.getName().substring(last.length()));
                                }
                                break;
                        }
                    }
                    return new NamedContainer<>((list.size() <= 0)?plugin.api.getLang("SubServers", "Command.Info.Unknown").replace("$str$", args[0]):null, list);
                } else {
                    return new NamedContainer<>(null, Collections.emptyList());
                }
            } else if (!(sender instanceof ProxiedPlayer) && args[0].equals("reload")) {
                List<String> list = new ArrayList<String>(),
                        completes = Arrays.asList("all", "config", "templates");
                if (args.length == 2) {
                    if (last.length() == 0) {
                        list = completes;
                    } else {
                        for (String complete : completes) {
                            if (complete.toLowerCase().startsWith(last)) list.add(last + complete.substring(last.length()));
                        }
                    }
                    return new NamedContainer<>((list.size() <= 0)?plugin.api.getLang("SubServers", "Command.Generic.Unknown").replace("$str$", args[0]):null, list);
                } else {
                    return new NamedContainer<>(null, Collections.emptyList());
                }
            } else if (args[0].equals("start") ||
                    args[0].equals("restart") ||
                    (!(sender instanceof ProxiedPlayer) && (
                                args[0].equals("sudo") || args[0].equals("screen") ||
                                args[0].equals("del") || args[0].equals("delete")
                            ))) {
                List<String> list = new ArrayList<String>();
                if (args.length == 2) {
                    if (last.length() == 0) {
                        for (SubServer server : plugin.api.getSubServers().values()) list.add(server.getName());
                    } else {
                        for (SubServer server : plugin.api.getSubServers().values()) {
                            if (server.getName().toLowerCase().startsWith(last)) list.add(last + server.getName().substring(last.length()));
                        }
                    }
                    return new NamedContainer<>((list.size() <= 0)?plugin.api.getLang("SubServers", "Command.Generic.Unknown-SubServer").replace("$str$", args[0]):null, list);
                } else {
                    return new NamedContainer<>(null, Collections.emptyList());
                }
            } else if (args[0].equals("stop") ||
                    args[0].equals("kill") || args[0].equals("terminate")) {
                List<String> list = new ArrayList<String>();
                if (args.length == 2) {
                    if (last.length() == 0) {
                        list.add("*");
                        for (SubServer server : plugin.api.getSubServers().values()) list.add(server.getName());
                    } else {
                        if ("*".startsWith(last)) list.add("*");
                        for (SubServer server : plugin.api.getSubServers().values()) {
                            if (server.getName().toLowerCase().startsWith(last)) list.add(last + server.getName().substring(last.length()));
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
                        list.add("*");
                        for (SubServer server : plugin.api.getSubServers().values()) list.add(server.getName());
                    } else {
                        if ("*".startsWith(last)) list.add("*");
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
                    return new NamedContainer<>(null, Collections.singletonList("[Version]"));
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
            //    now = new net.ME1312.SubServers.Bungee.Library.Compatibility.mc1_13.CommandX(cmd.name());
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
                        if (!server.isHidden() && server.canAccess(sender) && (!(server instanceof SubServer) || ((SubServer) server).isRunning())) {
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
                List<String> list = new ArrayList<String>();
                if (last.length() == 0) {
                    for (Server server : plugin.api.getServers().values()) {
                        if (!server.isHidden()) list.add(server.getName());
                    }
                    return new NamedContainer<>(null, new LinkedList<>(list));
                } else {
                    for (Server server : plugin.api.getServers().values()) {
                        if (server.getName().toLowerCase().startsWith(last) && !server.isHidden()) list.add(server.getName());
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

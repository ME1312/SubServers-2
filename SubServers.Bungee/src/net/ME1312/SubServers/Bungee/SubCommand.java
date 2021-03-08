package net.ME1312.SubServers.Bungee;

import net.ME1312.Galaxi.Library.Callback.Callback;
import net.ME1312.Galaxi.Library.Callback.ReturnRunnable;
import net.ME1312.Galaxi.Library.Container.ContainedPair;
import net.ME1312.Galaxi.Library.Container.Pair;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Platform;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.SubData.Server.ClientHandler;
import net.ME1312.SubData.Server.SubDataClient;
import net.ME1312.SubServers.Bungee.Host.*;
import net.ME1312.SubServers.Bungee.Library.Compatibility.Galaxi.GalaxiInfo;
import net.ME1312.SubServers.Bungee.Library.Compatibility.Logger;
import net.ME1312.SubServers.Bungee.Network.Packet.PacketCheckPermission;

import com.google.gson.Gson;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;
import net.md_5.bungee.command.ConsoleCommandSender;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static net.ME1312.SubServers.Bungee.Library.Compatibility.Galaxi.GalaxiCommand.description;
import static net.ME1312.SubServers.Bungee.Library.Compatibility.Galaxi.GalaxiCommand.help;

/**
 * Plugin Command Class
 */
@SuppressWarnings("deprecation")
public final class SubCommand extends Command implements TabExecutor {
    static HashMap<UUID, HashMap<ServerInfo, Pair<Long, Boolean>>> players = new HashMap<UUID, HashMap<ServerInfo, Pair<Long, Boolean>>>();
    private static Thread reload;
    private SubProxy plugin;
    private String label;

    SubCommand(SubProxy plugin, String command) {
        super(command);
        this.plugin = plugin;
        this.label = '/' + command;

        description(this, "The SubServers Command");
        help(this,
                "The command for accessing the SubServers Server Manager.",
                "",
                "Permission: subservers.command",
                "Extended help entries:",
                "  /sub help"
        );
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
                    Version galaxi = GalaxiInfo.getVersion();
                    Version bungee = Util.getDespiteException(() -> (Version) BungeeCord.class.getMethod("getForkVersion").invoke(plugin), null);
                    Version galaxibuild = GalaxiInfo.getBuild();
                    Version bungeebuild = Util.getDespiteException(() -> (Version) BungeeCord.class.getMethod("getForkBuild").invoke(plugin), null);

                    sender.sendMessage("SubServers > These are the platforms and versions that are running SubServers.Bungee:");
                    sender.sendMessage("  " + Platform.getSystemName() + ' ' + Platform.getSystemVersion() + ((Platform.getSystemBuild() != null)?" (" + Platform.getSystemBuild() + ')':"") + ((!Platform.getSystemArchitecture().equals("unknown"))?" [" + Platform.getSystemArchitecture() + ']':"") + ',');
                    sender.sendMessage("  Java " + Platform.getJavaVersion() + ((!Platform.getJavaArchitecture().equals("unknown"))?" [" + Platform.getJavaArchitecture() + ']':"") + ',');
                    if (galaxi != null) Util.isException(() -> sender.sendMessage("  GalaxiEngine v" + galaxi.toExtendedString() + ((galaxibuild != null)?" (" + galaxibuild + ')':"") + ','));
                    sender.sendMessage("  " + plugin.getBungeeName() + ((plugin.isGalaxi)?" v":" ") + ((bungee != null)?bungee:plugin.getVersion()) + ((bungeebuild != null)?" (" + bungeebuild + ')':"") + ((plugin.isPatched)?" [Patched]":"") + ',');
                    sender.sendMessage("  SubServers.Bungee v" + SubProxy.version.toExtendedString() + ((plugin.api.getWrapperBuild() != null)?" (" + plugin.api.getWrapperBuild() + ')':""));
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
                    if (reload == null || !reload.isAlive()) (reload = new Thread(() -> {
                        if (args.length > 1) {
                            switch (args[1].toLowerCase()) {
                                case "*":
                                case "all":
                                case "hard":
                                case "system":
                                case "subdata":
                                case "network":
                                    plugin.stopListeners();
                                    plugin.getLogger().info("Closing player connections");
                                    for (ProxiedPlayer player : plugin.getPlayers()) {
                                        Util.isException(() -> player.disconnect(plugin.getTranslation("restart")));
                                    }
                                    plugin.shutdown();
                                case "soft":
                                case "bungee":
                                case "bungeecord":
                                case "plugin":
                                case "plugins":
                                    plugin.getPluginManager().dispatchCommand(ConsoleCommandSender.getInstance(), "greload");
                                    break;
                                case "host":
                                case "hosts":
                                case "server":
                                case "servers":
                                case "subserver":
                                case "subservers":
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
                                case "subcreator":
                                case "subcreators":
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
                    }, "SubServers.Bungee::Reload_Command_Handler")).start();
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
                            for (Server server : plugin.api.getGroup(group).value()) names.add(server.getName());
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
                                } else if (((SubServer) server).getHost().isAvailable() && ((SubServer) server).getHost().isEnabled() && ((SubServer) server).isAvailable() && ((SubServer) server).isEnabled() && ((SubServer) server).getCurrentIncompatibilities().size() == 0) {
                                    message += ChatColor.YELLOW;
                                } else {
                                    message += ChatColor.RED;
                                }
                                message += server.getDisplayName() + ((server.getName().equals(server.getDisplayName()))?"":" ["+server.getName()+']');
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
                        message += host.getDisplayName() + " [" + ((host.getName().equals(host.getDisplayName()))?"":host.getName()+ChatColor.stripColor(div)) + host.getAddress().getHostAddress() + "]" + ChatColor.RESET + ": ";
                        for (SubServer subserver : host.getSubServers().values()) {
                            if (i != 0) message += div;
                            if (subserver.isRunning()) {
                                if (subserver.getStopAction() == SubServer.StopAction.REMOVE_SERVER || subserver.getStopAction() == SubServer.StopAction.RECYCLE_SERVER || subserver.getStopAction() == SubServer.StopAction.DELETE_SERVER) {
                                    message += ChatColor.AQUA;
                                } else {
                                    message += ChatColor.GREEN;
                                }
                            } else if (subserver.getHost().isAvailable() && subserver.getHost().isEnabled() && subserver.isAvailable() && subserver.isEnabled() && subserver.getCurrentIncompatibilities().size() == 0) {
                                message += ChatColor.YELLOW;
                            } else {
                                message += ChatColor.RED;
                            }
                            message += subserver.getDisplayName() + " [" + ((subserver.getName().equals(subserver.getDisplayName()))?"":subserver.getName()+ChatColor.stripColor(div)) + subserver.getAddress().getPort() + "]";
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
                            message += ChatColor.WHITE + server.getDisplayName() + " [" + ((server.getName().equals(server.getDisplayName()))?"":server.getName()+ChatColor.stripColor(div)) + server.getAddress().getAddress().getHostAddress() + ':' + server.getAddress().getPort() + "]";
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
                            if (proxy.getSubData()[0] != null) {
                                message += ChatColor.AQUA;
                            } else {
                                message += ChatColor.RED;
                            }
                            message += proxy.getDisplayName() + ((proxy.getName().equals(proxy.getDisplayName()))?"":" ["+proxy.getName()+']');
                        }
                        sender.sendMessage(message);
                    }
                } else if (args[0].equalsIgnoreCase("info") || args[0].equalsIgnoreCase("status")) {
                    if (args.length > 1) {
                        String type = (args.length > 2)?args[1]:null;
                        String name = args[(type != null)?2:1];

                        Runnable getPlayer = () -> {
                            RemotePlayer player = plugin.api.getRemotePlayer(name);
                            if (player != null) {
                                sender.sendMessage("SubServers > Info on player: " + ChatColor.WHITE + player.getName());
                                if (player.getProxy() != null) sender.sendMessage(" -> Proxy: " + ChatColor.WHITE + player.getProxy().getName());
                                if (player.getServer() != null) sender.sendMessage(" -> Server: " + ChatColor.WHITE + player.getServer().getName());
                                if (player.getAddress() != null) sender.sendMessage(" -> Address: " + ChatColor.WHITE + player.getAddress().getAddress().getHostAddress() + ':' + player.getAddress().getPort());
                                sender.sendMessage(" -> UUID: " + ChatColor.AQUA + player.getUniqueId());
                            } else {
                                if (type == null) {
                                    sender.sendMessage("SubServers > There is no object with that name");
                                } else {
                                    sender.sendMessage("SubServers > There is no player with that name");
                                }
                            }
                        };
                        Runnable getServer = () -> {
                            Server server = plugin.api.getServer(name);
                            if (server != null) {
                                sender.sendMessage("SubServers > Info on " + ((server instanceof SubServer)?"sub":"") + "server: " + ChatColor.WHITE + server.getDisplayName());
                                if (!server.getName().equals(server.getDisplayName())) sender.sendMessage(" -> System Name: " + ChatColor.WHITE + server.getName());
                                if (server instanceof SubServer) {
                                    sender.sendMessage(" -> Available: " + ((((SubServer) server).isAvailable())?ChatColor.GREEN+"yes":ChatColor.RED+"no"));
                                    sender.sendMessage(" -> Enabled: " + ((((SubServer) server).isEnabled())?ChatColor.GREEN+"yes":ChatColor.RED+"no"));
                                    if (!((SubServer) server).isEditable()) sender.sendMessage(" -> Editable: " + ChatColor.RED + "no");
                                    sender.sendMessage(" -> Host: " + ChatColor.WHITE + ((SubServer) server).getHost().getName());
                                    if (((SubServer) server).getTemplate() != null) sender.sendMessage(" -> Template: " + ChatColor.WHITE + ((SubServer) server).getTemplate().getName());
                                }
                                if (server.getGroups().size() > 0) sender.sendMessage(" -> Group" + ((server.getGroups().size() > 1)?"s:":": " + ChatColor.WHITE + server.getGroups().get(0)));
                                if (server.getGroups().size() > 1) for (String group : server.getGroups()) sender.sendMessage("      - " + ChatColor.WHITE + group);
                                sender.sendMessage(" -> Address: " + ChatColor.WHITE + server.getAddress().getAddress().getHostAddress()+':'+server.getAddress().getPort());
                                if (server instanceof SubServer) sender.sendMessage(" -> " + ((((SubServer) server).isOnline())?"Online":"Running") + ": " + ((((SubServer) server).isRunning())?ChatColor.GREEN+"yes":ChatColor.RED+"no"));
                                if (!(server instanceof SubServer) || ((SubServer) server).isRunning()) {
                                    sender.sendMessage(" -> Connected: " + ((server.getSubData()[0] != null)?ChatColor.GREEN+"yes"+((server.getSubData().length > 1)?ChatColor.AQUA+" +"+(server.getSubData().length-1)+" subchannel"+((server.getSubData().length == 2)?"":"s"):""):ChatColor.RED+"no"));
                                    sender.sendMessage(" -> Players: " + ChatColor.AQUA + server.getRemotePlayers().size() + " online");
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
                                    getPlayer.run();
                                } else {
                                    sender.sendMessage("SubServers > There is no server with that name");
                                }
                            }
                        };
                        Runnable getGroup = () -> {
                            Pair<String, List<Server>> group = plugin.api.getGroup(name);
                            if (group != null) {
                                sender.sendMessage("SubServers > Info on group: " + ChatColor.WHITE + group.key());
                                sender.sendMessage(" -> Servers: " + ((group.value().size() <= 0)?ChatColor.GRAY + "(none)":ChatColor.AQUA.toString() + group.value().size()));
                                for (Server server : group.value()) sender.sendMessage("      - " + ChatColor.WHITE + server.getDisplayName() + ((server.getName().equals(server.getDisplayName()))?"":" ["+server.getName()+']'));
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
                                sender.sendMessage("SubServers > Info on host: " + ChatColor.WHITE + host.getDisplayName());
                                if (!host.getName().equals(host.getDisplayName())) sender.sendMessage(" -> System Name: " + ChatColor.WHITE + host.getName());
                                sender.sendMessage(" -> Available: " + ((host.isAvailable())?ChatColor.GREEN+"yes":ChatColor.RED+"no"));
                                sender.sendMessage(" -> Enabled: " + ((host.isEnabled())?ChatColor.GREEN+"yes":ChatColor.RED+"no"));
                                sender.sendMessage(" -> Address: " + ChatColor.WHITE + host.getAddress().getHostAddress());
                                if (host instanceof ClientHandler) sender.sendMessage(" -> Connected: " + ((((ClientHandler) host).getSubData()[0] != null)?ChatColor.GREEN+"yes"+((((ClientHandler) host).getSubData().length > 1)?ChatColor.AQUA+" +"+(((ClientHandler) host).getSubData().length-1)+" subchannel"+((((ClientHandler) host).getSubData().length == 2)?"":"s"):""):ChatColor.RED+"no"));
                                sender.sendMessage(" -> SubServers: " + ((host.getSubServers().keySet().size() <= 0)?ChatColor.GRAY + "(none)":ChatColor.AQUA.toString() + host.getSubServers().keySet().size()));
                                for (SubServer subserver : host.getSubServers().values()) sender.sendMessage("      - " + ((subserver.isEnabled())?ChatColor.WHITE:ChatColor.GRAY) + subserver.getDisplayName() + ((subserver.getName().equals(subserver.getDisplayName()))?"":" ["+subserver.getName()+']'));
                                sender.sendMessage(" -> Templates: " + ((host.getCreator().getTemplates().keySet().size() <= 0)?ChatColor.GRAY + "(none)":ChatColor.AQUA.toString() + host.getCreator().getTemplates().keySet().size()));
                                for (SubCreator.ServerTemplate template : host.getCreator().getTemplates().values()) sender.sendMessage("      - " + ((template.isEnabled())?ChatColor.WHITE:ChatColor.GRAY) + template.getDisplayName() + ((template.getName().equals(template.getDisplayName()))?"":" ["+template.getName()+']'));
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
                                sender.sendMessage("SubServers > Info on proxy: " + ChatColor.WHITE + proxy.getDisplayName());
                                if (!proxy.getName().equals(proxy.getDisplayName())) sender.sendMessage(" -> System Name: " + ChatColor.WHITE + proxy.getName());
                                if (!proxy.isMaster()) sender.sendMessage(" -> Connected: " + ((proxy.getSubData()[0] != null)?ChatColor.GREEN+"yes"+((proxy.getSubData().length > 1)?ChatColor.AQUA+" +"+(proxy.getSubData().length-1)+" subchannel"+((proxy.getSubData().length == 2)?"":"s"):""):ChatColor.RED+"no"));
                                else if (!proxy.getDisplayName().toLowerCase().contains("master")) sender.sendMessage(" -> Type: " + ChatColor.WHITE + "Master");
                                sender.sendMessage(" -> Players: " + ChatColor.AQUA + proxy.getPlayers().size() + " online");
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
                                case "player":
                                    getPlayer.run();
                                    break;
                                default:
                                    sender.sendMessage("SubServers > There is no object type with that name");
                            }
                        }
                    } else {
                        sender.sendMessage("SubServers > Usage: " + label + " " + args[0].toLowerCase() + " [proxy|host|group|server|player] <Name>");
                    }
                } else if (args[0].equalsIgnoreCase("start")) {
                    if (args.length > 1) {
                        ServerSelection select = selectServers(sender, args, 1, true);
                        if (select.subservers.length > 0) {
                            int success = 0, running = 0;
                            for (SubServer server : select.subservers) {
                                if (!server.getHost().isAvailable()) {
                                    sender.sendMessage("SubServers > The host for " + server.getName() + " is not available");
                                } else if (!server.getHost().isEnabled()) {
                                    sender.sendMessage("SubServers > The host for " + server.getName() + " is not enabled");
                                } else if (!server.isAvailable()) {
                                    sender.sendMessage("SubServers > Subserver " + server.getName() + " is not available");
                                } else if (!server.isEnabled()) {
                                    sender.sendMessage("SubServers > SubServer " + server.getName() + " is not enabled");
                                } else if (server.isRunning()) {
                                    running++;
                                } else if (server.getCurrentIncompatibilities().size() != 0) {
                                    String list = "";
                                    List<SubServer> others = server.getCurrentIncompatibilities();
                                    for (SubServer other : others) {
                                        if (list.length() != 0) list += ", ";
                                        list += other.getName();
                                    }
                                    sender.sendMessages("SubServers > Subserver " + server.getName() + " cannot start while these server"+((others.size() == 1)?"":"s")+" are running:", list);
                                } else if (server.start()) {
                                    success++;
                                }
                            }
                            if (running > 0) sender.sendMessage("SubServers > " + running + " subserver"+((running == 1)?" was":"s were") + " already running");
                            if (success > 0) sender.sendMessage("SubServers > Started " + success + " subserver"+((success == 1)?"":"s"));
                        }
                    } else {
                        sender.sendMessage("SubServers > Usage: " + label + " " + args[0].toLowerCase() + " <Subservers>");
                    }
                } else if (args[0].equalsIgnoreCase("restart")) {
                    if (args.length > 1) {
                        ServerSelection select = selectServers(sender, args, 1, true);
                        if (select.subservers.length > 0) {
                            Callback<SubServer> starter = server -> {
                                Map<String, Server> servers = plugin.api.getServers();
                                if (!servers.keySet().contains(server.getName().toLowerCase()) || !(servers.get(server.getName().toLowerCase()) instanceof SubServer)) {
                                    sender.sendMessage("SubServers > Could not restart server: Subserver " + server.getName() + " has disappeared");
                                } else if (!(server = (SubServer) servers.get(server.getName().toLowerCase())).isRunning()) {
                                    if (!server.getHost().isAvailable()) {
                                        sender.sendMessage("SubServers > Could not restart server: The host for " + server.getName() + " is no longer available");
                                    } else if (!server.getHost().isEnabled()) {
                                        sender.sendMessage("SubServers > Could not restart server: The host for " + server.getName() + " is no longer enabled");
                                    } else if (!server.isAvailable()) {
                                        sender.sendMessage("SubServers > Could not restart server: Subserver " + server.getName() + " is no longer available");
                                    } else if (!server.isEnabled()) {
                                        sender.sendMessage("SubServers > Could not restart server: Subserver " + server.getName() + " is no longer enabled");
                                    } else if (server.getCurrentIncompatibilities().size() != 0) {
                                        String list = "";
                                        List<SubServer> others = server.getCurrentIncompatibilities();
                                        for (SubServer other : others) {
                                            if (list.length() != 0) list += ", ";
                                            list += other.getName();
                                        }
                                        sender.sendMessages("Could not restart server: Subserver " + server.getName() + " cannot start while these server"+((others.size() == 1)?"":"s")+" are running:", list);
                                    } else {
                                        server.start();
                                    }
                                }
                            };

                            int success = 0;
                            for (SubServer server : select.subservers) {
                                if (server.isRunning()) {
                                    if (server.stop()) {
                                        new Thread(() -> {
                                            try {
                                                server.waitFor();
                                                Thread.sleep(100);
                                                starter.run(server);
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                            }
                                        }, "SubServers.Bungee::Server_Restart_Command_Handler(" + server.getName() + ')').start();
                                        success++;
                                    }
                                } else {
                                    starter.run(server);
                                    success++;
                                }
                            }
                            if (success > 0) sender.sendMessage("SubServers > Restarting " + success + " subserver"+((success == 1)?"":"s"));
                        }
                    } else {
                        sender.sendMessage("SubServers > Usage: " + label + " " + args[0].toLowerCase() + " <Subservers>");
                    }
                } else if (args[0].equalsIgnoreCase("stop")) {
                    if (args.length > 1) {
                        ServerSelection select = selectServers(sender, args, 1, true);
                        if (select.subservers.length > 0) {
                            int success = 0, running = 0;
                            for (SubServer server : select.subservers) {
                                if (!server.isRunning()) {
                                    running++;
                                } else if (server.stop()) {
                                    success++;
                                }
                            }
                            if (running > 0) sender.sendMessage("SubServers > " + running + " subserver"+((running == 1)?" was":"s were") + " already offline");
                            if (success > 0) sender.sendMessage("SubServers > Stopping " + success + " subserver"+((success == 1)?"":"s"));
                        }
                    } else {
                        sender.sendMessage("SubServers > Usage: " + label + " " + args[0].toLowerCase() + " <Subservers>");
                    }
                } else if (args[0].equalsIgnoreCase("kill") || args[0].equalsIgnoreCase("terminate")) {
                    if (args.length > 1) {
                        ServerSelection select = selectServers(sender, args, 1, true);
                        if (select.subservers.length > 0) {
                            int success = 0, running = 0;
                            for (SubServer server : select.subservers) {
                                if (!server.isRunning()) {
                                    running++;
                                } else if (server.terminate()) {
                                    success++;
                                }
                            }
                            if (running > 0) sender.sendMessage("SubServers > " + running + " subserver"+((running == 1)?" was":"s were") + " already offline");
                            if (success > 0) sender.sendMessage("SubServers > Terminated " + success + " subserver"+((success == 1)?"":"s"));
                        }
                    } else {
                        sender.sendMessage("SubServers > Usage: " + label + " " + args[0].toLowerCase() + " <Subservers>");
                    }
                } else if (args[0].equalsIgnoreCase("cmd") || args[0].equalsIgnoreCase("command")) {
                    if (args.length > 1) {
                        ServerSelection select = selectServers(sender, args, 1, true);
                        if (select.subservers.length > 0) {
                            if (select.args.length > 2) {
                                StringBuilder builder = new StringBuilder(select.args[2]);
                                for (int i = 3; i < select.args.length; i++) {
                                    builder.append(' ');
                                    builder.append(select.args[i]);
                                }

                                int success = 0, running = 0;
                                String command = builder.toString();
                                for (SubServer server : select.subservers) {
                                    if (!server.isRunning()) {
                                        running++;
                                    } else if (server.command(command)) {
                                        success++;
                                    }
                                }
                                if (running > 0) sender.sendMessage("SubServers > " + running + " subserver"+((running == 1)?" was":"s were") + " offline");
                                if (success > 0) sender.sendMessage("SubServers > Sent command to " + success + " subserver"+((success == 1)?"":"s"));
                            } else {
                                sender.sendMessage("SubServers > No command was entered");
                            }
                        }
                    } else {
                        sender.sendMessage("SubServers > Usage: " + label + " " + args[0].toLowerCase() + " <Subservers> <Command> [Args...]");
                    }
                } else if (args[0].equalsIgnoreCase("sudo") || args[0].equalsIgnoreCase("screen")) {
                    if (plugin.canSudo) {
                        if (args.length > 1) {
                            Map<String, Server> servers = plugin.api.getServers();
                            if (!servers.keySet().contains(args[1].toLowerCase()) || !(servers.get(args[1].toLowerCase()) instanceof SubServer)) {
                                sender.sendMessage("SubServers > There is no subserver with that name");
                            } else if (!((SubServer) servers.get(args[1].toLowerCase())).isRunning()) {
                                sender.sendMessage("SubServers > That subserver is not running");
                            } else {
                                plugin.sudo = (SubServer) servers.get(args[1].toLowerCase());
                                Logger.get("SubServers").info("Now forwarding commands to " + plugin.sudo.getDisplayName() + ". Type \"exit\" to return.");
                            }
                        } else {
                            sender.sendMessage("SubServers > Usage: " + label + " " + args[0].toLowerCase() + " <Subserver>");
                        }
                    } else {
                        sender.sendMessage("SubServers > The BungeeCord library provided does not support console sudo.");
                    }
                } else if (args[0].equalsIgnoreCase("create")) {
                    if (args.length > 3) {
                        if (plugin.api.getSubServers().keySet().contains(args[1].toLowerCase()) || SubCreator.isReserved(args[1])) {
                            sender.sendMessage("SubServers > There is already a subserver with that name");
                        } else if (!plugin.hosts.keySet().contains(args[2].toLowerCase())) {
                            sender.sendMessage("SubServers > There is no host with that name");
                        } else if (!plugin.hosts.get(args[2].toLowerCase()).isAvailable()) {
                            sender.sendMessage("SubServers > That host is not available");
                        } else if (!plugin.hosts.get(args[2].toLowerCase()).isEnabled()) {
                            sender.sendMessage("SubServers > That host is not enabled");
                        } else if (!plugin.hosts.get(args[2].toLowerCase()).getCreator().getTemplates().keySet().contains(args[3].toLowerCase())) {
                            sender.sendMessage("SubServers > There is no template with that name");
                        } else if (!plugin.hosts.get(args[2].toLowerCase()).getCreator().getTemplate(args[3].toLowerCase()).isEnabled()) {
                            sender.sendMessage("SubServers > That template is not enabled");
                        } else if (args.length <= 4 && plugin.hosts.get(args[2].toLowerCase()).getCreator().getTemplate(args[3].toLowerCase()).requiresVersion()) {
                            sender.sendMessage("SubServers > That template requires a Minecraft version to be specified");
                        } else if (args.length > 5 && (Util.isException(() -> Integer.parseInt(args[5])) || Integer.parseInt(args[5]) <= 0 || Integer.parseInt(args[5]) > 65535)) {
                            sender.sendMessage("SubServers > Invalid port number");
                        } else {
                            plugin.hosts.get(args[2].toLowerCase()).getCreator().create(args[1], plugin.hosts.get(args[2].toLowerCase()).getCreator().getTemplate(args[3].toLowerCase()), (args.length > 4)?new Version(args[4]):null, (args.length > 5)?Integer.parseInt(args[5]):null);
                            sender.sendMessage("SubServers > Creating subserver " + args[1]);
                        }
                    } else {
                        sender.sendMessage("SubServers > Usage: " + label + " " + args[0].toLowerCase() + " <Name> <Host> <Template> [Version] [Port]");
                    }
                } else if (args[0].equalsIgnoreCase("update") || args[0].equalsIgnoreCase("upgrade")) {
                    if (args.length > 1) {
                        ServerSelection select = selectServers(sender, args, 1, true);
                        if (select.subservers.length > 0) {
                            String template = (select.args.length > 3)?select.args[2].toLowerCase():null;
                            Version version = (select.args.length > 2)?new Version(select.args[(template == null)?2:3]):null;

                            int success = 0;
                            for (SubServer server : select.subservers) {
                                if (!server.isAvailable()) {
                                    sender.sendMessage("SubServers > The host for " + server.getName() + " is not available");
                                } else if (!server.getHost().isEnabled()) {
                                    sender.sendMessage("SubServers > The host for " + server.getName() + " is not enabled");
                                } else if (!server.isAvailable()) {
                                    sender.sendMessage("SubServers > Subserver " + server.getName() + " is not available");
                                } else if (server.isRunning()) {
                                    sender.sendMessage("SubServers > Cannot update " + server.getName() + " while it is still running");
                                } else {
                                    SubCreator.ServerTemplate ft = (template != null)?server.getHost().getCreator().getTemplate(template):server.getTemplate();
                                    boolean ts = template == null;
                                    if (ft == null) {
                                        if (ts) sender.sendMessage("SubServers > We don't know which template built " + server.getName());
                                        else    sender.sendMessage("SubServers > There is no template with that name");
                                    } else if (!ft.isEnabled()) {
                                        if (ts) sender.sendMessage("SubServers > The template that created " + server.getName() + " is not enabled");
                                        else    sender.sendMessage("SubServers > That template is not enabled");
                                    } else if (!ft.canUpdate()) {
                                        if (ts) sender.sendMessage("SubServers > The template that created " + server.getName() + " does not support subserver updating");
                                        else    sender.sendMessage("SubServers > That template does not support subserver updating");
                                    } else if (version == null && ft.requiresVersion()) {
                                        sender.sendMessage("SubServers > The template that created " + server.getName() + " requires a Minecraft version to be specified");
                                    } else if (server.getHost().getCreator().update(server, ft, version)) {
                                        success++;
                                    }
                                }
                            }
                            if (success > 0) sender.sendMessage("SubServers > Updating " + success + " subserver"+((success == 1)?"":"s"));
                        }
                    } else {
                        sender.sendMessage("SubServers > Usage: " + label + " " + args[0].toLowerCase() + " <Subservers> [[Template] <Version>]");
                    }
                } else if (args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("del") || args[0].equalsIgnoreCase("delete")) {
                    if (args.length > 1) {
                        ServerSelection select = selectServers(sender, args, 1, true);
                        if (select.subservers.length > 0) {
                            int success = 0;
                            for (SubServer server : select.subservers) try {
                                if (server.isRunning()) {
                                    sender.sendMessage("SubServers > Subserver " + server.getName() + " is still running");
                                } else if (server.getHost().recycleSubServer(server.getName())) {
                                    success++;
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            if (success > 0) sender.sendMessage("SubServers > Removing " + success + " subserver"+((success == 1)?"":"s"));
                        }
                    }
                } else if (args[0].equalsIgnoreCase("restore")) {
                    // TODO
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

    private ServerSelection selectServers(CommandSender sender, String[] rargs, int index, boolean mode) {
        LinkedList<String> msgs = new LinkedList<String>();
        LinkedList<String> args = new LinkedList<String>();
        LinkedList<String> selection = new LinkedList<>();
        LinkedList<Server> servers = new LinkedList<Server>();
        String last = null;

        int i = 0;
        while (i < index) {
            args.add(rargs[i]);
            ++i;
        }

        Map<String, Host> hostMap = null;
        Map<String, List<Server>> groupMap = null;
        Map<String, Server> serverMap = null;

        StringBuilder completed = new StringBuilder();
        for (boolean run = true; run && i < rargs.length; i++) {
            String current = last = rargs[i];
            completed.append(current);
            if (current.endsWith(",")) {
                current = current.substring(0, current.length() - 1);
                completed.append(' ');
            } else run = false;
            selection.add(current.toLowerCase());

            if (current.length() > 0) {
                LinkedList<Server> select = new LinkedList<Server>();
                if (current.startsWith("::") && current.length() > 2) {
                    current = current.substring(2);
                    if (hostMap == null) hostMap = plugin.api.getHosts();

                    if (current.equals("*")) {
                        for (Host host : hostMap.values()) {
                            select.addAll(host.getSubServers().values());
                        }
                    } else {
                        Host host = hostMap.getOrDefault(current.toLowerCase(), null);
                        if (host != null) {
                            select.addAll(host.getSubServers().values());
                            if (select.size() <= 0) {
                                String msg = "SubServers > There are no " + ((mode)?"sub":"") + "servers on host: " + host.getName();
                                if (sender != null) sender.sendMessage(msg);
                                msgs.add(msg);
                            }
                        } else {
                            String msg = "SubServers > There is no host with name: " + current;
                            if (sender != null) sender.sendMessage(msg);
                            msgs.add(msg);
                        }
                    }
                } else if (current.startsWith(":") && current.length() > 1) {
                    current = current.substring(1);
                    if (groupMap == null) groupMap = plugin.api.getGroups();

                    if (current.equals("*")) {
                        for (List<Server> group : groupMap.values()) for (Server server : group) {
                            if (!mode || server instanceof SubServer) select.add(server);
                        }
                    } else {
                        Map.Entry<String, List<Server>> group = null;
                        for (Map.Entry<String, List<Server>> entry : groupMap.entrySet()) if (current.equalsIgnoreCase(entry.getKey())) {
                            group = entry;
                            break;
                        }
                        if (group != null) {
                            for (Server server : group.getValue()) {
                                if (!mode || server instanceof SubServer) select.add(server);
                            }
                            if (select.size() <= 0) {
                                String msg = "SubServers > There are no " + ((mode)?"sub":"") + "servers in group: " + group.getKey();
                                if (sender != null) sender.sendMessage(msg);
                                msgs.add(msg);
                            }
                        } else {
                            String msg = "SubServers > There is no group with name: " + current;
                            if (sender != null) sender.sendMessage(msg);
                            msgs.add(msg);
                        }
                    }
                } else {
                    if (serverMap == null) serverMap = plugin.api.getServers();

                    if (current.equals("*")) {
                        for (Server server : serverMap.values()) {
                            if (!mode || server instanceof SubServer) select.add(server);
                        }
                    } else {
                        Server server = serverMap.getOrDefault(current.toLowerCase(), null);
                        if (server != null) {
                            select.add(server);
                        } else {
                            String msg = "SubServers > There is no " + ((mode)?"sub":"") + "server with name: " + current;
                            if (sender != null) sender.sendMessage(msg);
                            msgs.add(msg);
                        }
                    }
                }

                for (Server server : select) {
                    if (!servers.contains(server)) servers.add(server);
                }
            }
        }
        args.add(completed.toString());

        while (i < rargs.length) {
            args.add(rargs[i]);
            last = null;
            i++;
        }

        LinkedList<SubServer> subservers = new LinkedList<SubServer>();
        for (Server server : servers) if (server instanceof SubServer) subservers.add((SubServer) server);

        if ((!mode && servers.size() <= 0) || (mode && subservers.size() <= 0)) {
            String msg = "SubServers > No " + ((mode)?"sub":"") + "servers were selected";
            if (sender != null) sender.sendMessage(msg);
            msgs.add(msg);
        }

        return new ServerSelection(msgs, selection, servers, subservers, args, last);
    }
    private static final class ServerSelection {
        private final String[] msgs;
        private final String[] selection;
        private final Server[] servers;
        private final SubServer[] subservers;
        private final String[] args;
        private final String last;

        private ServerSelection(List<String> msgs, List<String> selection, List<Server> servers, List<SubServer> subservers, List<String> args, String last) {
            this.msgs = msgs.toArray(new String[0]);
            this.selection = selection.toArray(new String[0]);
            this.servers = servers.toArray(new Server[0]);
            this.subservers = subservers.toArray(new SubServer[0]);
            this.args = args.toArray(new String[0]);
            this.last = last;

            Arrays.sort(this.selection);
        }
    }

    private String[] printHelp() {
        return new String[]{
                "SubServers > Console Command Help:",
                "   Help: /sub help",
                "   List: /sub list",
                "   Version: /sub version",
                "   Reload: /sub reload [hard|bungee|servers|templates]",
                "   Info: /sub info [proxy|host|group|server|player] <Name>",
                "   Start Server: /sub start <Subservers>",
                "   Restart Server: /sub restart <Subservers>",
                "   Stop Server: /sub stop <Subservers>",
                "   Terminate Server: /sub kill <Subservers>",
                "   Command Server: /sub cmd <Subservers> <Command> [Args...]",
                "   Sudo Server: /sub sudo <Subserver>",
                "   Create Server: /sub create <Name> <Host> <Template> [Version] [Port]",
                "   Update Server: /sub update <Subservers> [[Template] <Version>]",
                "   Remove Server: /sub delete <Subservers>",
              //"   Restore Server: /sub restore <Subservers>",
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
    @SuppressWarnings("unchecked")
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        String Last = (args.length > 0)?args[args.length - 1]:"";
        String last = Last.toLowerCase();

        if (sender instanceof ProxiedPlayer && (!players.keySet().contains(((ProxiedPlayer) sender).getUniqueId()) || !players.get(((ProxiedPlayer) sender).getUniqueId()).keySet().contains(((ProxiedPlayer) sender).getServer().getInfo())
        || !players.get(((ProxiedPlayer) sender).getUniqueId()).get(((ProxiedPlayer) sender).getServer().getInfo()).value())) {
            if (players.keySet().contains(((ProxiedPlayer) sender).getUniqueId()) && players.get(((ProxiedPlayer) sender).getUniqueId()).keySet().contains(((ProxiedPlayer) sender).getServer().getInfo())
                    && players.get(((ProxiedPlayer) sender).getUniqueId()).get(((ProxiedPlayer) sender).getServer().getInfo()).key() == null) {
                // do nothing
            } else if (!players.keySet().contains(((ProxiedPlayer) sender).getUniqueId()) || !players.get(((ProxiedPlayer) sender).getUniqueId()).keySet().contains(((ProxiedPlayer) sender).getServer().getInfo())
            || Calendar.getInstance().getTime().getTime() - players.get(((ProxiedPlayer) sender).getUniqueId()).get(((ProxiedPlayer) sender).getServer().getInfo()).key() >= TimeUnit.MINUTES.toMillis(1)) {
                if (!(((ProxiedPlayer) sender).getServer().getInfo() instanceof Server) || ((Server) ((ProxiedPlayer) sender).getServer().getInfo()).getSubData()[0] == null) {
                    HashMap<ServerInfo, Pair<Long, Boolean>> map = (players.keySet().contains(((ProxiedPlayer) sender).getUniqueId()))?players.get(((ProxiedPlayer) sender).getUniqueId()):new HashMap<ServerInfo, Pair<Long, Boolean>>();
                    map.put(((ProxiedPlayer) sender).getServer().getInfo(), new ContainedPair<>(Calendar.getInstance().getTime().getTime(), false));
                    players.put(((ProxiedPlayer) sender).getUniqueId(), map);
                } else {
                    HashMap<ServerInfo, Pair<Long, Boolean>> map = (players.keySet().contains(((ProxiedPlayer) sender).getUniqueId()))?players.get(((ProxiedPlayer) sender).getUniqueId()):new HashMap<ServerInfo, Pair<Long, Boolean>>();
                    map.put(((ProxiedPlayer) sender).getServer().getInfo(), new ContainedPair<>(null, false));
                    players.put(((ProxiedPlayer) sender).getUniqueId(), map);
                    ((SubDataClient) ((Server) ((ProxiedPlayer) sender).getServer().getInfo()).getSubData()[0]).sendPacket(new PacketCheckPermission(((ProxiedPlayer) sender).getUniqueId(), "subservers.command", result -> {
                        map.put(((ProxiedPlayer) sender).getServer().getInfo(), new ContainedPair<>(Calendar.getInstance().getTime().getTime(), result));
                    }));
                }
            }
            return Collections.emptyList();
        } else if (args.length <= 1) {
            List<String> cmds = new ArrayList<>();
            cmds.addAll(Arrays.asList("help", "list", "info", "status", "version", "start", "restart", "stop", "kill", "terminate", "cmd", "command", "create", "update", "upgrade"));
            if (!(sender instanceof ProxiedPlayer)) cmds.addAll(Arrays.asList("reload", "sudo", "screen", "remove", "delete", "restore"));
            List<String> list = new ArrayList<String>();
            for (String cmd : cmds) {
                if (cmd.startsWith(last)) list.add(Last + cmd.substring(last.length()));
            }
            return list;
        } else {
            if (args[0].equals("info") || args[0].equals("status")) {
                ReturnRunnable<Collection<String>> getPlayers = () -> {
                    LinkedList<String> names = new LinkedList<String>();
                    for (ProxiedPlayer player : plugin.getPlayers()) names.add(player.getName());
                    for (RemotePlayer player : plugin.api.getRemotePlayers().values()) if (!names.contains(player.getName())) names.add(player.getName());
                    Collections.sort(names);
                    return names;
                };

                if (args.length == 2) {
                    List<String> list = new ArrayList<String>();
                    List<String> subcommands = new ArrayList<String>();
                    subcommands.add("proxy");
                    subcommands.add("host");
                    subcommands.add("group");
                    subcommands.add("server");
                    subcommands.add("subserver");
                    subcommands.add("player");
                    for (String command : subcommands) {
                        if (!list.contains(command) && command.toLowerCase().startsWith(last))
                            list.add(Last + command.substring(last.length()));
                    }
                    Proxy master = plugin.api.getMasterProxy();
                    if (master != null && !list.contains(master.getName()) && master.getName().toLowerCase().startsWith(last))
                        list.add(Last + master.getName().substring(last.length()));
                    for (Proxy proxy : plugin.api.getProxies().values()) {
                        if (!list.contains(proxy.getName()) && proxy.getName().toLowerCase().startsWith(last))
                            list.add(Last + proxy.getName().substring(last.length()));
                    }
                    for (Host host : plugin.api.getHosts().values()) {
                        if (!list.contains(host.getName()) && host.getName().toLowerCase().startsWith(last))
                            list.add(Last + host.getName().substring(last.length()));
                    }
                    for (String group : plugin.api.getGroups().keySet()) {
                        if (!list.contains(group) && group.toLowerCase().startsWith(last))
                            list.add(Last + group.substring(last.length()));
                    }
                    for (Server server : plugin.api.getServers().values()) {
                        if (!list.contains(server.getName()) && server.getName().toLowerCase().startsWith(last))
                            list.add(Last + server.getName().substring(last.length()));
                    }
                    for (String player : getPlayers.run()) {
                        if (!list.contains(player) && player.toLowerCase().startsWith(last))
                            list.add(Last + player.substring(last.length()));
                    }
                    return list;
                } else if (args.length == 3) {
                    List<String> list = new ArrayList<String>();

                    switch (args[1].toLowerCase()) {
                        case "p":
                        case "proxy":
                            Proxy master = plugin.api.getMasterProxy();
                            if (master != null && master.getName().toLowerCase().startsWith(last))
                                list.add(Last + master.getName().substring(last.length()));
                            for (Proxy proxy : plugin.api.getProxies().values()) {
                                if (!list.contains(proxy.getName()) && proxy.getName().toLowerCase().startsWith(last))
                                    list.add(Last + proxy.getName().substring(last.length()));
                            }
                            break;
                        case "h":
                        case "host":
                            for (Host host : plugin.api.getHosts().values()) {
                                if (host.getName().toLowerCase().startsWith(last))
                                    list.add(Last + host.getName().substring(last.length()));
                            }
                            break;
                        case "g":
                        case "group":
                            for (String group : plugin.api.getGroups().keySet()) {
                                if (group.toLowerCase().startsWith(last))
                                    list.add(Last + group.substring(last.length()));
                            }
                            break;
                        case "s":
                        case "server":
                        case "subserver":
                            for (Server server : plugin.api.getServers().values()) {
                                if ((!args[1].equalsIgnoreCase("subserver") || server instanceof SubServer) && server.getName().toLowerCase().startsWith(last))
                                    list.add(Last + server.getName().substring(last.length()));
                            }
                            break;
                        case "player":
                            for (String player : getPlayers.run()) {
                                if (player.toLowerCase().startsWith(last))
                                    list.add(Last + player.substring(last.length()));
                            }
                            break;
                    }
                    return list;
                } else {
                    return Collections.emptyList();
                }
            } else if (!(sender instanceof ProxiedPlayer) && (args[0].equals("reload") || args[0].equals("restore"))) {
                if (args[0].equals("reload")) {
                    List<String> list = new ArrayList<String>(),
                            completes = Arrays.asList("hard", "bungee", "servers", "templates");
                    if (args.length == 2) {
                        for (String complete : completes) {
                            if (complete.toLowerCase().startsWith(last)) list.add(Last + complete.substring(last.length()));
                        }
                        return list;
                    } else {
                        return Collections.emptyList();
                    }
                } else /* if (args[0].equals("restore")) */ {
                    if (args.length == 2) {
                        return Collections.singletonList("<Subserver>");
                    } else {
                        return Collections.emptyList();
                    }
                }
            } else if (args[0].equals("start") ||
                    args[0].equals("restart") ||
                    args[0].equals("stop") ||
                    args[0].equals("kill") || args[0].equals("terminate") ||
                    args[0].equals("cmd") || args[0].equals("command") ||
                    args[0].equals("update") || args[0].equals("upgrade") ||
                    (!(sender instanceof ProxiedPlayer) && (
                                args[0].equals("sudo") || args[0].equals("screen") ||
                                args[0].equals("remove") || args[0].equals("del") || args[0].equals("delete")
                            ))) {
                List<String> list = new ArrayList<String>();
                ServerSelection select = selectServers(null, args, 1, true);
                if (select.last != null) {
                    if (last.startsWith("::")) {
                        Map<String, Host> hosts = plugin.api.getHosts();
                        if (hosts.size() > 0) {
                            if (Arrays.binarySearch(select.selection, "::*") < 0 && "::*".startsWith(last)) list.add("::*");
                            if (sender instanceof ProxiedPlayer && Arrays.binarySearch(select.selection, "::.") < 0 && "::.".startsWith(last)) list.add("::.");
                            for (Host host : hosts.values()) {
                                String name = "::" + host.getName();
                                if (Arrays.binarySearch(select.selection, name.toLowerCase()) < 0 && name.toLowerCase().startsWith(last)) list.add(Last + name.substring(last.length()));
                            }
                        }
                        return list;
                    } else if (last.startsWith(":")) {
                        Map<String, List<Server>> groups = plugin.api.getGroups();
                        if (groups.size() > 0) {
                            if (Arrays.binarySearch(select.selection, ":*") < 0 && ":*".startsWith(last)) list.add(":*");
                            if (sender instanceof ProxiedPlayer && Arrays.binarySearch(select.selection, ":.") < 0 && ":.".startsWith(last)) list.add(":.");
                            for (String group : groups.keySet()) {
                                group = ":" + group;
                                if (Arrays.binarySearch(select.selection, group.toLowerCase()) < 0 && group.toLowerCase().startsWith(last)) list.add(Last + group.substring(last.length()));
                            }
                        }
                        return list;
                    } else {
                        Map<String, SubServer> subservers = plugin.api.getSubServers();
                        if (subservers.size() > 0) {
                            if (Arrays.binarySearch(select.selection, "*") < 0 && "*".startsWith(last)) list.add("*");
                            if (sender instanceof ProxiedPlayer && Arrays.binarySearch(select.selection, ".") < 0 && ".".startsWith(last)) list.add(".");
                            for (SubServer server : subservers.values()) {
                                if (Arrays.binarySearch(select.selection, server.getName().toLowerCase()) < 0 && server.getName().toLowerCase().startsWith(last)) list.add(Last + server.getName().substring(last.length()));
                            }
                        }
                        return list;
                    }
                } else if (args[0].equals("cmd") || args[0].equals("command")) {
                    if (select.args.length == 3) {
                        return Collections.singletonList("<Command>");
                    } else {
                        return Collections.singletonList("[Args...]");
                    }
                } else if (args[0].equals("update") || args[0].equals("upgrade")) {
                    if (select.args.length == 3) {
                        return Arrays.asList("[Template]", "[Version]");
                    } else if (select.args.length == 4) {
                        return Collections.singletonList("<Version>");
                    }
                }
                return Collections.emptyList();
            } else if (args[0].equals("create")) {
                if (args.length == 2) {
                    return Collections.singletonList("<Name>");
                } else if (args.length == 3) {
                    List<String> list = new ArrayList<String>();
                    for (Host host : plugin.api.getHosts().values()) {
                        if (host.getName().toLowerCase().startsWith(last)) list.add(Last + host.getName().substring(last.length()));
                    }
                    return list;
                } else if (args.length == 4) {
                    List<String> list = new ArrayList<String>();
                    Map<String, Host> hosts = plugin.api.getHosts();
                    if (!hosts.keySet().contains(args[2].toLowerCase())) {
                        list.add("<Template>");
                    } else {
                        for (SubCreator.ServerTemplate template : hosts.get(args[2].toLowerCase()).getCreator().getTemplates().values()) {
                            if (template.getName().toLowerCase().startsWith(last)) list.add(Last + template.getName().substring(last.length()));
                        }
                    }
                    return list;
                } else if (args.length == 5) {
                    return Collections.singletonList("[Version]");
                } else if (args.length == 6) {
                    if (last.length() > 0) {
                        if (Util.isException(() -> Integer.parseInt(last)) || Integer.parseInt(last) <= 0 || Integer.parseInt(last) > 65535) {
                            return Collections.emptyList();
                        }
                    }
                    return Collections.singletonList("[Port]");
                } else {
                    return Collections.emptyList();
                }
            } else if (sender instanceof ProxiedPlayer && (args[0].equals("tp") || args[0].equals("teleport"))) {
                if (args.length == 2 || args.length == 3) {
                    List<String> list = new ArrayList<String>();
                    if (args.length == 2) {
                        list.add("@p");
                        list.add("@a");
                        list.add("@r");
                        list.add("@s");

                        List<UUID> used = new ArrayList<UUID>();
                        for (ProxiedPlayer player : ((ProxiedPlayer) sender).getServer().getInfo().getPlayers()) {
                            if (player.getName().toLowerCase().startsWith(last)) list.add(Last + player.getName().substring(last.length()));
                            used.add(player.getUniqueId());
                        }

                        if (((ProxiedPlayer) sender).getServer().getInfo() instanceof Server) {
                            for (RemotePlayer player : ((Server) ((ProxiedPlayer) sender).getServer().getInfo()).getRemotePlayers()) {
                                if (!used.contains(player.getUniqueId())) {
                                    if (player.getName().toLowerCase().startsWith(last)) list.add(Last + player.getName().substring(last.length()));
                                    used.add(player.getUniqueId());
                                }
                            }
                        }
                    }
                    for (Server server : plugin.api.getServers().values()) {
                        if (server.getName().toLowerCase().startsWith(last)) list.add(Last + server.getName().substring(last.length()));
                    }
                    return list;
                } else {
                    return Collections.emptyList();
                }
            } else {
                return Collections.emptyList();
            }
        }
    }

    /**
     * BungeeCord /server
     */
    public static final class BungeeServer extends Command implements TabExecutor {
        private SubProxy plugin;
        BungeeServer(SubProxy plugin, String command) {
            super(command, "bungeecord.command.server");
            this.plugin = plugin;

            description(this, "Displays a list of or connects you to servers");
            help(this,
                    "Displays a list of all players connected to BungeeCord.",
                    "This list is separated into groups by server.",
                    "",
                    "Permission: bungeecord.command.list",
                    "Example:",
                    "  /glist"
            );
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
                                message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponent[]{new TextComponent(plugin.api.getLang("SubServers", "Bungee.Server.Hover").replace("$int$", Integer.toString(server.getRemotePlayers().size())))}));
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
        public List<String> onTabComplete(CommandSender sender, String[] args) {
            if (args.length <= 1) {
                String last = (args.length > 0)?args[args.length - 1].toLowerCase():"";
                List<String> list = new ArrayList<String>();
                for (Server server : plugin.api.getServers().values()) {
                    if (server.getName().toLowerCase().startsWith(last) && !server.isHidden()) list.add(server.getName());
                }
                return list;
            } else {
                return Collections.emptyList();
            }
        }
    }

    /**
     * BungeeCord /glist
     */
    public static final class BungeeList extends Command {
        private SubProxy plugin;
        BungeeList(SubProxy plugin, String command) {
            super(command, "bungeecord.command.list");
            this.plugin = plugin;

            description(this, "Displays a list of all players");
            help(this,
                    "Displays a list of all players connected to BungeeCord.",
                    "This list is separated into groups by server.",
                    "",
                    "Permission: bungeecord.command.list",
                    "Example:",
                    "  /glist"
            );
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
                for (RemotePlayer player : server.getRemotePlayers()) playerlist.add(player.getName());
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

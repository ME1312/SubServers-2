package net.ME1312.SubServers.Host;

import net.ME1312.Galaxi.Engine.GalaxiEngine;
import net.ME1312.Galaxi.Engine.Library.ConsoleReader;
import net.ME1312.Galaxi.Library.AsyncConsolidator;
import net.ME1312.Galaxi.Library.Callback.Callback;
import net.ME1312.Galaxi.Library.Callback.ReturnRunnable;
import net.ME1312.Galaxi.Library.Container.NamedContainer;
import net.ME1312.Galaxi.Library.Container.PrimitiveContainer;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Container.Container;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.Galaxi.Plugin.Command.Command;
import net.ME1312.Galaxi.Plugin.Command.CommandSender;
import net.ME1312.Galaxi.Plugin.Command.CompletionHandler;
import net.ME1312.SubData.Client.SubDataClient;
import net.ME1312.SubServers.Host.Library.TextColor;
import net.ME1312.SubServers.Host.Network.API.*;
import net.ME1312.SubServers.Host.Network.Packet.*;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Command Class
 */
public class SubCommand {
    private static TreeMap<String, Proxy> proxyCache = new TreeMap<String, Proxy>();
    private static TreeMap<String, Host> hostCache = new TreeMap<String, Host>();
    private static TreeMap<String, List<Server>> groupCache = new TreeMap<String, List<Server>>();
    private static TreeMap<String, Server> serverCache = new TreeMap<String, Server>();
    private static Proxy proxyMasterCache = null;
    private static long cacheDate = 0;
    private final ExHost host;

    private static boolean canRun() {
        if (SubAPI.getInstance().getSubDataNetwork()[0] == null) {
            throw new IllegalStateException("SubData is not connected");
        } else {
            return true;
        }
    }

    SubCommand(ExHost host) {
        this.host = host;
    }

    void load() {
        CompletionHandler defaultCompletor;
        new Command(host.info) {
            @Override
            public void command(CommandSender sender, String label, String[] rargs) {
                if (rargs.length > 0) {
                    LinkedList<String> args = new LinkedList<String>(Arrays.asList(rargs));
                    args.removeFirst();

                    ConsoleReader console = GalaxiEngine.getInstance().getConsoleReader();
                    console.runCommand(sender, console.escapeCommand(rargs[0], args.toArray(new String[0])));
                } else {
                    sender.sendMessage("Usage: /" + label + " <Command> [Args...]");
                }
            }
        }.autocomplete((sender, label, rargs) -> {
            LinkedList<String> args = new LinkedList<String>(Arrays.asList(rargs));
            args.removeFirst();

            ConsoleReader console = GalaxiEngine.getInstance().getConsoleReader();
            return console.complete(sender, console.escapeCommand(rargs[0], args.toArray(new String[0]))).toArray(new String[0]);
        }).usage("<Command>", "[Args...]").description("An alias for commands").help(
                "This command is an alias for all registered commands for ease of use.",
                "",
                "Examples:",
                "  /sub help -> /help",
                "  /sub version ExamplePlugin -> /version ExamplePlugin"
        ).register("sub", "subserver", "subservers");
        new Command(host.info) {
            @Override
            public void command(CommandSender sender, String label, String[] args) {
                if (canRun()) host.api.getGroups(groups -> host.api.getHosts(hosts -> host.api.getServers(servers -> host.api.getMasterProxy(proxymaster -> host.api.getProxies(proxies -> {
                    int i = 0;
                    boolean sent = false;
                    String div = TextColor.RESET + ", ";
                    if (groups.keySet().size() > 0) {
                        sender.sendMessage("Group/Server List:");
                        for (String group : groups.keySet()) {
                            String message = "  ";
                            message += TextColor.GOLD + group + TextColor.RESET + ": ";
                            for (Server server : groups.get(group)) {
                                if (i != 0) message += div;
                                if (!(server instanceof SubServer)) {
                                    message += TextColor.WHITE;
                                } else if (((SubServer) server).isRunning()) {
                                    if (((SubServer) server).getStopAction() == SubServer.StopAction.REMOVE_SERVER || ((SubServer) server).getStopAction() == SubServer.StopAction.RECYCLE_SERVER || ((SubServer) server).getStopAction() == SubServer.StopAction.DELETE_SERVER) {
                                        message += TextColor.AQUA;
                                    } else {
                                        message += TextColor.GREEN;
                                    }
                                } else if (((SubServer) server).isAvailable() && ((SubServer) server).isEnabled() && ((SubServer) server).getCurrentIncompatibilities().size() == 0) {
                                    message += TextColor.YELLOW;
                                } else {
                                    message += TextColor.RED;
                                }
                                message += server.getDisplayName() + ((server.getName().equals(server.getDisplayName()))?"":" ["+server.getName()+']');
                                i++;
                            }
                            if (i == 0) message += TextColor.RESET + "(none)";
                            sender.sendMessage(message);
                            i = 0;
                            sent = true;
                        }
                        if (!sent) sender.sendMessage(TextColor.RESET + "(none)");
                        sent = false;
                    }
                    sender.sendMessage("Host/SubServer List:");
                    for (Host host : hosts.values()) {
                        String message = "  ";
                        if (host.isAvailable() && host.isEnabled()) {
                            message += TextColor.AQUA;
                        } else {
                            message += TextColor.RED;
                        }
                        message += host.getDisplayName() + " [" + ((host.getName().equals(host.getDisplayName()))?"":host.getName()+TextColor.stripColor(div)) + host.getAddress().getHostAddress() + "]" + TextColor.RESET + ": ";
                        for (SubServer subserver : host.getSubServers().values()) {
                            if (i != 0) message += div;
                            if (subserver.isRunning()) {
                                if (subserver.getStopAction() == SubServer.StopAction.REMOVE_SERVER || subserver.getStopAction() == SubServer.StopAction.RECYCLE_SERVER || subserver.getStopAction() == SubServer.StopAction.DELETE_SERVER) {
                                    message += TextColor.AQUA;
                                } else {
                                    message += TextColor.GREEN;
                                }
                            } else if (subserver.isAvailable() && subserver.isEnabled() && subserver.getCurrentIncompatibilities().size() == 0) {
                                message += TextColor.YELLOW;
                            } else {
                                message += TextColor.RED;
                            }
                            message += subserver.getDisplayName() + " [" + ((subserver.getName().equals(subserver.getDisplayName()))?"":subserver.getName()+TextColor.stripColor(div)) + subserver.getAddress().getPort() + "]";
                            i++;
                        }
                        if (i == 0) message += TextColor.RESET + "(none)";
                        sender.sendMessage(message);
                        i = 0;
                        sent = true;
                    }
                    if (!sent) sender.sendMessage(TextColor.RESET + "(none)");
                    sender.sendMessage("Server List:");
                    String message = "  ";
                    for (Server server : servers.values()) if (!(server instanceof SubServer)) {
                        if (i != 0) message += div;
                        message += TextColor.WHITE + server.getDisplayName() + " [" + ((server.getName().equals(server.getDisplayName()))?"":server.getName()+TextColor.stripColor(div)) + server.getAddress().getAddress().getHostAddress()+':'+server.getAddress().getPort() + "]";
                        i++;
                    }
                    if (i == 0) message += TextColor.RESET + "(none)";
                    sender.sendMessage(message);
                    if (proxies.keySet().size() > 0) {
                        sender.sendMessage("Proxy List:");
                        message = "  (master)";
                        for (Proxy proxy : proxies.values()) {
                            message += div;
                            if (proxy.getSubData()[0] != null && proxy.isRedis()) {
                                message += TextColor.GREEN;
                            } else if (proxy.getSubData()[0] != null) {
                                message += TextColor.AQUA;
                            } else if (proxy.isRedis()) {
                                message += TextColor.WHITE;
                            } else {
                                message += TextColor.RED;
                            }
                            message += proxy.getDisplayName() + ((proxy.getName().equals(proxy.getDisplayName()))?"":" ["+proxy.getName()+']');
                        }
                        sender.sendMessage(message);
                    }
                })))));
            }
        }.description("List all known SubServers objects").help(
                "Sends you information about all known objects within SubServers",
                "in a detailed, yet concise, list format. This includes objects like:",
                "Proxies, Hosts, Groups, Servers and Subservers; the relationships between them; and their statuses.",
                "",
                "Example:",
                "  /list"
        ).register("list");
        new Command(host.info) {
            @Override
            public void command(CommandSender sender, String label, String[] args) {
                if (canRun()) {
                    if (args.length > 0) {
                        String type = (args.length > 1)?args[0]:null;
                        String name = args[(type != null)?1:0];

                        Runnable getPlayer = () -> host.api.getGlobalPlayer(name, player -> {
                            if (player != null) {
                                sender.sendMessage("Info on player: " + TextColor.WHITE + player.getName());
                                if (player.getProxy() != null) sender.sendMessage(" -> Proxy: " + TextColor.WHITE + player.getProxy());
                                if (player.getServer() != null) sender.sendMessage(" -> Server: " + TextColor.WHITE + player.getServer());
                                if (player.getAddress() != null) sender.sendMessage(" -> Address: " + TextColor.WHITE + player.getAddress().getHostAddress());
                                sender.sendMessage(" -> UUID: " + TextColor.AQUA + player.getUniqueId());
                            } else {
                                if (type == null) {
                                    sender.sendMessage("There is no object with that name");
                                } else {
                                    sender.sendMessage("There is no player with that name");
                                }
                            }
                        });
                        Runnable getServer = () -> host.api.getServer(name, server -> {
                            if (server != null) {
                                sender.sendMessage("Info on " + ((server instanceof SubServer)?"sub":"") + "server: " + TextColor.WHITE + server.getDisplayName());
                                if (!server.getName().equals(server.getDisplayName())) sender.sendMessage(" -> System Name: " + TextColor.WHITE + server.getName());
                                if (server instanceof SubServer) {
                                    sender.sendMessage(" -> Available: " + ((((SubServer) server).isAvailable())?TextColor.GREEN+"yes":TextColor.RED+"no"));
                                    sender.sendMessage(" -> Enabled: " + ((((SubServer) server).isEnabled())?TextColor.GREEN+"yes":TextColor.RED+"no"));
                                    if (!((SubServer) server).isEditable()) sender.sendMessage(" -> Editable: " + TextColor.RED + "no");
                                    sender.sendMessage(" -> Host: " + TextColor.WHITE + ((SubServer) server).getHost());
                                    if (((SubServer) server).getTemplate() != null) sender.sendMessage(" -> Template: " + TextColor.WHITE + ((SubServer) server).getTemplate());
                                }
                                if (server.getGroups().size() > 0) sender.sendMessage(" -> Group" + ((server.getGroups().size() > 1)?"s:":": " + TextColor.WHITE + server.getGroups().get(0)));
                                if (server.getGroups().size() > 1) for (String group : server.getGroups()) sender.sendMessage("      - " + TextColor.WHITE + group);
                                sender.sendMessage(" -> Address: " + TextColor.WHITE + server.getAddress().getAddress().getHostAddress()+':'+server.getAddress().getPort());
                                if (server instanceof SubServer) sender.sendMessage(" -> Running: " + ((((SubServer) server).isRunning())?TextColor.GREEN+"yes":TextColor.RED+"no"));
                                if (!(server instanceof SubServer) || ((SubServer) server).isRunning()) {
                                    sender.sendMessage(" -> Connected: " + ((server.getSubData()[0] != null)?TextColor.GREEN+"yes"+((server.getSubData().length > 1)?TextColor.AQUA+" +"+(server.getSubData().length-1)+" subchannel"+((server.getSubData().length == 2)?"":"s"):""):TextColor.RED+"no"));
                                    sender.sendMessage(" -> Players: " + TextColor.AQUA + server.getGlobalPlayers().size() + " online");
                                }
                                sender.sendMessage(" -> MOTD: " + TextColor.WHITE + TextColor.stripColor(server.getMotd()));
                                if (server instanceof SubServer && ((SubServer) server).getStopAction() != SubServer.StopAction.NONE) sender.sendMessage(" -> Stop Action: " + TextColor.WHITE + ((SubServer) server).getStopAction().toString());
                                sender.sendMessage(" -> Signature: " + TextColor.AQUA + server.getSignature());
                                if (server instanceof SubServer) sender.sendMessage(" -> Logging: " + ((((SubServer) server).isLogging())?TextColor.GREEN+"yes":TextColor.RED+"no"));
                                sender.sendMessage(" -> Restricted: " + ((server.isRestricted())?TextColor.GREEN+"yes":TextColor.RED+"no"));
                                if (server instanceof SubServer && ((SubServer) server).getIncompatibilities().size() > 0) {
                                    List<String> current = new ArrayList<String>();
                                    for (String other : ((SubServer) server).getCurrentIncompatibilities()) current.add(other.toLowerCase());
                                    sender.sendMessage(" -> Incompatibilities:");
                                    for (String other : ((SubServer) server).getIncompatibilities()) sender.sendMessage("      - " + ((current.contains(other.toLowerCase()))?TextColor.WHITE:TextColor.GRAY) + other);
                                }
                                sender.sendMessage(" -> Hidden: " + ((server.isHidden())?TextColor.GREEN+"yes":TextColor.RED+"no"));
                            } else {
                                if (type == null) {
                                    getPlayer.run();
                                } else {
                                    sender.sendMessage("There is no server with that name");
                                }
                            }
                        });
                        Runnable getGroup = () -> host.api.getGroup(name, group -> {
                            if (group != null) {
                                sender.sendMessage("Info on group: " + TextColor.WHITE + group.name());
                                sender.sendMessage(" -> Servers: " + ((group.get().size() <= 0)?TextColor.GRAY + "(none)":TextColor.AQUA.toString() + group.get().size()));
                                for (Server server : group.get()) sender.sendMessage("      - " + TextColor.WHITE + server.getDisplayName() + ((server.getName().equals(server.getDisplayName()))?"":" ["+server.getName()+']'));
                            } else {
                                if (type == null) {
                                    getServer.run();
                                } else {
                                    sender.sendMessage("There is no group with that name");
                                }
                            }
                        });
                        Runnable getHost = () -> host.api.getHost(name, host -> {
                            if (host != null) {
                                sender.sendMessage("Info on host: " + TextColor.WHITE + host.getDisplayName());
                                if (!host.getName().equals(host.getDisplayName())) sender.sendMessage(" -> System Name: " + TextColor.WHITE + host.getName());
                                sender.sendMessage(" -> Available: " + ((host.isAvailable())?TextColor.GREEN+"yes":TextColor.RED+"no"));
                                sender.sendMessage(" -> Enabled: " + ((host.isEnabled())?TextColor.GREEN+"yes":TextColor.RED+"no"));
                                sender.sendMessage(" -> Address: " + TextColor.WHITE + host.getAddress().getHostAddress());
                                if (host.getSubData().length > 0) sender.sendMessage(" -> Connected: " + ((host.getSubData()[0] != null)?TextColor.GREEN+"yes"+((host.getSubData().length > 1)?TextColor.AQUA+" +"+(host.getSubData().length-1)+" subchannel"+((host.getSubData().length == 2)?"":"s"):""):TextColor.RED+"no"));
                                sender.sendMessage(" -> SubServers: " + ((host.getSubServers().keySet().size() <= 0)?TextColor.GRAY + "(none)":TextColor.AQUA.toString() + host.getSubServers().keySet().size()));
                                for (SubServer subserver : host.getSubServers().values()) sender.sendMessage("      - " + ((subserver.isEnabled())?TextColor.WHITE:TextColor.GRAY) + subserver.getDisplayName() + ((subserver.getName().equals(subserver.getDisplayName()))?"":" ["+subserver.getName()+']'));
                                sender.sendMessage(" -> Templates: " + ((host.getCreator().getTemplates().keySet().size() <= 0)?TextColor.GRAY + "(none)":TextColor.AQUA.toString() + host.getCreator().getTemplates().keySet().size()));
                                for (SubCreator.ServerTemplate template : host.getCreator().getTemplates().values()) sender.sendMessage("      - " + ((template.isEnabled())?TextColor.WHITE:TextColor.GRAY) + template.getDisplayName() + ((template.getName().equals(template.getDisplayName()))?"":" ["+template.getName()+']'));
                                sender.sendMessage(" -> Signature: " + TextColor.AQUA + host.getSignature());
                            } else {
                                if (type == null) {
                                    getGroup.run();
                                } else {
                                    sender.sendMessage("There is no host with that name");
                                }
                            }
                        });
                        Runnable getProxy = () -> host.api.getProxy(name, proxy -> {
                            if (proxy != null) {
                                sender.sendMessage("Info on proxy: " + TextColor.WHITE + proxy.getDisplayName());
                                if (!proxy.getName().equals(proxy.getDisplayName())) sender.sendMessage(" -> System Name: " + TextColor.WHITE + proxy.getName());
                                if (!proxy.isMaster()) sender.sendMessage(" -> Connected: " + ((proxy.getSubData()[0] != null)?TextColor.GREEN+"yes"+((proxy.getSubData().length > 1)?TextColor.AQUA+" +"+(proxy.getSubData().length-1)+" subchannel"+((proxy.getSubData().length == 2)?"":"s"):""):TextColor.RED+"no"));
                                else if (!proxy.getDisplayName().toLowerCase().contains("master")) sender.sendMessage(" -> Type: " + TextColor.WHITE + "Master");
                                sender.sendMessage(" -> Redis: " + ((proxy.isRedis())?TextColor.GREEN:TextColor.RED+"un") + "available");
                                if (proxy.isRedis()) sender.sendMessage(" -> Players: " + TextColor.AQUA + proxy.getPlayers().size() + " online");
                                sender.sendMessage(" -> Signature: " + TextColor.AQUA + proxy.getSignature());
                            } else {
                                if (type == null) {
                                    getHost.run();
                                } else {
                                    sender.sendMessage("There is no proxy with that name");
                                }
                            }
                        });
    
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
                                    sender.sendMessage("There is no object type with that name");
                            }
                        }
                    } else {
                        sender.sendMessage("Usage: /" + label + " [proxy|host|group|server|player] <Name>");
                    }
                }
            }
        }.autocomplete((sender, label, args) -> {
            String Last = (args.length > 0)?args[args.length - 1]:"";
            String last = Last.toLowerCase();

            ReturnRunnable<Collection<String>> getPlayers = () -> {
                LinkedList<String> names = new LinkedList<String>();
                if (proxyMasterCache != null)
                    for (NamedContainer<String, UUID> player : proxyMasterCache.getPlayers())
                        names.add(player.name());
                for (Proxy proxy : proxyCache.values())
                    for (NamedContainer<String, UUID> player : proxy.getPlayers())
                        if (!names.contains(player.name())) names.add(player.name());
                Collections.sort(names);
                return names;
            };

            updateCache();

            if (args.length == 1) {
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
                Proxy master = proxyMasterCache;
                if (master != null && !list.contains(master.getName()) && master.getName().toLowerCase().startsWith(last))
                    list.add(Last + master.getName().substring(last.length()));
                for (Proxy proxy : proxyCache.values()) {
                    if (!list.contains(proxy.getName()) && proxy.getName().toLowerCase().startsWith(last))
                        list.add(Last + proxy.getName().substring(last.length()));
                }
                for (Host h : hostCache.values()) {
                    if (!list.contains(h.getName()) && h.getName().toLowerCase().startsWith(last))
                        list.add(Last + h.getName().substring(last.length()));
                }
                for (String group : groupCache.keySet()) {
                    if (!list.contains(group) && group.toLowerCase().startsWith(last))
                        list.add(Last + group.substring(last.length()));
                }
                for (Server server : serverCache.values()) {
                    if (!list.contains(server.getName()) && server.getName().toLowerCase().startsWith(last))
                        list.add(Last + server.getName().substring(last.length()));
                }
                for (String player : getPlayers.run()) {
                    if (!list.contains(player) && player.toLowerCase().startsWith(last))
                        list.add(Last + player.substring(last.length()));
                }
                return list.toArray(new String[0]);
            } else if (args.length == 2) {
                List<String> list = new ArrayList<String>();

                switch (args[0].toLowerCase()) {
                    case "p":
                    case "proxy":
                        Proxy master = proxyMasterCache;
                        if (master != null && master.getName().toLowerCase().startsWith(last))
                            list.add(Last + master.getName().substring(last.length()));
                        for (Proxy proxy : proxyCache.values()) {
                            if (!list.contains(proxy.getName()) && proxy.getName().toLowerCase().startsWith(last))
                                list.add(Last + proxy.getName().substring(last.length()));
                        }
                        break;
                    case "h":
                    case "host":
                        for (Host h : hostCache.values()) {
                            if (h.getName().toLowerCase().startsWith(last))
                                list.add(Last + h.getName().substring(last.length()));
                        }
                        break;
                    case "g":
                    case "group":
                        for (String group : groupCache.keySet()) {
                            if (group.toLowerCase().startsWith(last))
                                list.add(Last + group.substring(last.length()));
                        }
                        break;
                    case "s":
                    case "server":
                    case "subserver":
                        for (Server server : serverCache.values()) {
                            if ((!args[0].equalsIgnoreCase("subserver") || server instanceof SubServer) && server.getName().toLowerCase().startsWith(last))
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
                return list.toArray(new String[0]);
            } else return new String[0];
        }).usage("[proxy|host|group|server|player]", "<Name>").description("Gets information about a SubServers object").help(
                "Sends you all relevant information about a specific object within SubServers.",
                "This command can be really useful in troubleshooting. If you need a more in-depth look",
                "at an object than /sub list can give you, this is how you get it.",
                "",
                "If, for whatever reason, you have objects that share the same name,",
                "you can specify the type of object you are looking for as well.",
                "Specifying a type also makes the command faster since it doesn't have to search all object types.",
                "",
                "Examples:",
                "  /info Server1",
                "  /info server Server1"
        ).register("info", "status");
        new Command(host.info) {
            @Override
            public void command(CommandSender sender, String label, String[] args) {
                if (canRun()) {
                    if (args.length > 0) {
                        selectServers(sender, args, 0, true, select -> {
                            if (select.subservers.length > 0) {
                                PrimitiveContainer<Integer> success = new PrimitiveContainer<Integer>(0);
                                PrimitiveContainer<Integer> running = new PrimitiveContainer<Integer>(0);
                                AsyncConsolidator merge = new AsyncConsolidator(() -> {
                                    if (running.value > 0) sender.sendMessage(running.value + " subserver"+((running.value == 1)?" was":"s were") + " already running");
                                    if (success.value > 0) sender.sendMessage("Started " + success.value + " subserver"+((success.value == 1)?"":"s"));
                                });
                                for (SubServer server : select.subservers) {
                                    merge.reserve();
                                    server.start(null, response -> {
                                        switch (response) {
                                            case 3:
                                            case 4:
                                                sender.sendMessage("Subserver " + server.getName() + " has disappeared");
                                                break;
                                            case 5:
                                                sender.sendMessage("The host for " + server.getName() + " is not available");
                                                break;
                                            case 6:
                                                sender.sendMessage("The host for " + server.getName() + " is not enabled");
                                                break;
                                            case 7:
                                                sender.sendMessage("Subserver " + server.getName() + " is not available");
                                                break;
                                            case 8:
                                                sender.sendMessage("SubServer " + server.getName() + " is not enabled");
                                                break;
                                            case 9:
                                                running.value++;
                                                break;
                                            case 10:
                                                sender.sendMessage("Subserver " + server.getName() + " cannot start while incompatible server(s) are running");
                                                break;
                                            case 0:
                                                success.value++;
                                                break;
                                        }
                                        merge.release();
                                    });
                                }
                            }
                        });
                    } else {
                        sender.sendMessage("Usage: /" + label + " <Subservers>");
                    }
                }
            }
        }.autocomplete(defaultCompletor = new ServerCompletion(0, true, (sender, label, args, select) -> new String[0])
        ).usage("<Subservers>").description("Starts one or more subservers").help(
                "Starts one or more subservers on the network.",
                "",
                "Example:",
                "  /start Server1"
        ).register("start");
        new Command(host.info) {
            @Override
            public void command(CommandSender sender, String label, String[] args) {
                if (canRun()) {
                    if (args.length > 0) {
                        selectServers(sender, args, 0, true, select -> {
                            if (select.subservers.length > 0) {
                                // Step 5: Start the stopped Servers once more
                                Callback<SubServer> starter = server -> server.start(response -> {
                                    switch (response) {
                                        case 3:
                                        case 4:
                                            sender.sendMessage("Could not restart server: Subserver " + server.getName() + " has disappeared");
                                            break;
                                        case 5:
                                            sender.sendMessage("Could not restart server: The host for " + server.getName() + " is no longer available");
                                            break;
                                        case 6:
                                            sender.sendMessage("Could not restart server: The host for " + server.getName() + " is no longer enabled");
                                            break;
                                        case 7:
                                            sender.sendMessage("Could not restart server: Subserver " + server.getName() + " is no longer available");
                                            break;
                                        case 8:
                                            sender.sendMessage("Could not restart server: Subserver " + server.getName() + " is no longer enabled");
                                            break;
                                        case 10:
                                            sender.sendMessage("Could not restart server: Subserver " + server.getName() + " cannot start while incompatible server(s) are running");
                                            break;
                                        case 9:
                                        case 0:
                                            // success!
                                            break;
                                    }
                                });

                                // Step 4: Listen for stopped Servers
                                final HashMap<String, SubServer> listening = new HashMap<String, SubServer>();
                                PacketInExRunEvent.callback("SubStoppedEvent", new Callback<ObjectMap<String>>() {
                                    @Override
                                    public void run(ObjectMap<String> json) {
                                        try {
                                            if (listening.size() > 0) {
                                                PacketInExRunEvent.callback("SubStoppedEvent", this);
                                                String name = json.getString("server").toLowerCase();
                                                if (listening.keySet().contains(name)) {
                                                    new Timer("SubServers.Sync::Server_Restart_Command_Handler(" + name + ")").schedule(new TimerTask() {
                                                        @Override
                                                        public void run() {
                                                            starter.run(listening.get(name));
                                                            listening.remove(name);
                                                        }
                                                    }, 100);
                                                }
                                            }
                                        } catch (Exception e) {}
                                    }
                                });


                                // Step 1-3: Restart Servers / Receive command Responses
                                PrimitiveContainer<Integer> success = new PrimitiveContainer<Integer>(0);
                                AsyncConsolidator merge = new AsyncConsolidator(() -> {
                                    if (success.value > 0) sender.sendMessage("Restarting " + success.value + " subserver"+((success.value == 1)?"":"s"));
                                });
                                for (SubServer server : select.subservers) {
                                    merge.reserve();
                                    listening.put(server.getName().toLowerCase(), server);
                                    server.stop(response -> {
                                        if (response != 0) listening.remove(server.getName().toLowerCase());
                                        switch (response) {
                                            case 3:
                                            case 4:
                                                sender.sendMessage("Could not restart server: Subserver " + server.getName() + " has disappeared");
                                                break;
                                            case 5:
                                                starter.run(server);
                                            case 0:
                                                success.value++;
                                                break;
                                        }
                                        merge.release();
                                    });
                                }
                            }
                        });
                    } else {
                        sender.sendMessage("Usage: /" + label + " <Subservers>");
                    }
                }
            }
        }.autocomplete(defaultCompletor).usage("<Subservers>").description("Starts or restarts one or more subservers").help(
                "Starts or restarts one or more subservers on the network.",
                "",
                "Example:",
                "  /restart Server1"
        ).register("restart");
        new Command(host.info) {
            @Override
            public void command(CommandSender sender, String handle, String[] args) {
                if (canRun()) {
                    if (args.length > 0) {
                        selectServers(sender, args, 0, true, select -> {
                            if (select.subservers.length > 0) {
                                PrimitiveContainer<Integer> success = new PrimitiveContainer<Integer>(0);
                                PrimitiveContainer<Integer> running = new PrimitiveContainer<Integer>(0);
                                AsyncConsolidator merge = new AsyncConsolidator(() -> {
                                    if (running.value > 0) sender.sendMessage(running.value + " subserver"+((running.value == 1)?" was":"s were") + " already offline");
                                    if (success.value > 0) sender.sendMessage("Stopping " + success.value + " subserver"+((success.value == 1)?"":"s"));
                                });
                                for (SubServer server : select.subservers) {
                                    merge.reserve();
                                    server.stop(response -> {
                                        switch (response) {
                                            case 3:
                                            case 4:
                                                sender.sendMessage("Subserver " + server.getName() + " has disappeared");
                                                break;
                                            case 5:
                                                running.value++;
                                                break;
                                            case 0:
                                                success.value++;
                                                break;
                                        }
                                        merge.release();
                                    });
                                }
                            }
                        });
                    } else {
                        sender.sendMessage("Usage: /" + handle + " <Subservers>");
                    }
                }
            }
        }.autocomplete(defaultCompletor).usage("<Subservers>").description("Stops one or more subservers").help(
                "Stops one or more subservers on the network.",
                "",
                "Example:",
                "  /stop Server1"
        ).register("stop");
        new Command(host.info) {
            @Override
            public void command(CommandSender sender, String handle, String[] args) {
                if (canRun()) {
                    if (args.length > 0) {
                        selectServers(sender, args, 0, true, select -> {
                            if (select.subservers.length > 0) {
                                PrimitiveContainer<Integer> success = new PrimitiveContainer<Integer>(0);
                                PrimitiveContainer<Integer> running = new PrimitiveContainer<Integer>(0);
                                AsyncConsolidator merge = new AsyncConsolidator(() -> {
                                    if (running.value > 0) sender.sendMessage(running.value + " subserver"+((running.value == 1)?" was":"s were") + " already offline");
                                    if (success.value > 0) sender.sendMessage("Terminated " + success.value + " subserver"+((success.value == 1)?"":"s"));
                                });
                                for (SubServer server : select.subservers) {
                                    merge.reserve();
                                    server.terminate(response -> {
                                        switch (response) {
                                            case 3:
                                            case 4:
                                                sender.sendMessage("Subserver " + server.getName() + " has disappeared");
                                                break;
                                            case 5:
                                                running.value++;
                                                break;
                                            case 0:
                                                success.value++;
                                                break;
                                        }
                                        merge.release();
                                    });
                                }
                            }
                        });
                    } else {
                        sender.sendMessage("Usage: /" + handle + " <Subservers>");
                    }
                }
            }
        }.autocomplete(defaultCompletor).usage("<Subservers>").description("Forcefully stops one or more subservers").help(
                "Forcefully stops one or more subservers on the network.",
                "",
                "Stopping subservers in this way can make you lose unsaved data though, so it",
                "is generally not recommended to use this command unless a server stops responding.",
                "",
                "Example:",
                "  /terminate Server1"
        ).register("kill", "terminate");
        new Command(host.info) {
            @Override
            public void command(CommandSender sender, String handle, String[] args) {
                if (canRun()) {
                    if (args.length > 0) {
                        selectServers(sender, args, 0, true, select -> {
                            if (select.subservers.length > 0) {
                                if (select.args.length > 1) {
                                    StringBuilder builder = new StringBuilder(select.args[1]);
                                    for (int i = 2; i < select.args.length; i++) {
                                        builder.append(' ');
                                        builder.append(select.args[i]);
                                    }

                                    PrimitiveContainer<Integer> success = new PrimitiveContainer<Integer>(0);
                                    PrimitiveContainer<Integer> running = new PrimitiveContainer<Integer>(0);
                                    AsyncConsolidator merge = new AsyncConsolidator(() -> {
                                        if (running.value > 0) sender.sendMessage(running.value + " subserver"+((running.value == 1)?" was":"s were") + " offline");
                                        if (success.value > 0) sender.sendMessage("Sent command to " + success.value + " subserver"+((success.value == 1)?"":"s"));
                                    });
                                    for (SubServer server : select.subservers) {
                                        merge.reserve();
                                        server.command(builder.toString(), response -> {
                                            switch (response) {
                                                case 3:
                                                case 4:
                                                    sender.sendMessage("Subserver " + server.getName() + " has disappeared");
                                                    break;
                                                case 5:
                                                    running.value++;
                                                    break;
                                                case 0:
                                                    success.value++;
                                                    break;
                                            }
                                            merge.release();
                                        });
                                    }
                                } else {
                                    sender.sendMessage("No command was entered");
                                }
                            }
                        });
                    } else {
                        sender.sendMessage("Usage: /" + handle + " <Subservers> <Command> [Args...]");
                    }
                }
            }
        }.autocomplete(defaultCompletor).usage("<Subservers>", "<Command>", "[Args...]").description("Sends a command to the console of one or more subservers").help(
                "Sends a command to the console of one or more subservers on the network.",
                "",
                "Examples:",
                "  /command Server1 version",
                "  /command Server1 op Notch",
                "  /command Server1 say Hello World!"
        ).register("cmd", "command");
        new Command(host.info) {
            @Override
            public void command(CommandSender sender, String handle, String[] args) {
                if (canRun()) {
                    if (args.length > 2) {
                        if (args.length > 4 && Util.isException(() -> Integer.parseInt(args[4]))) {
                            sender.sendMessage("Invalid port number");
                        } else {
                            ((SubDataClient) SubAPI.getInstance().getSubDataNetwork()[0]).sendPacket(new PacketCreateServer(null, args[0], args[1], args[2], (args.length > 3)?new Version(args[3]):null, (args.length > 4)?Integer.parseInt(args[4]):null, data -> {
                                switch (data.getInt(0x0001)) {
                                    case 3:
                                        sender.sendMessage("Server names cannot include spaces");
                                        break;
                                    case 4:
                                        sender.sendMessage("There is already a subserver with that name");
                                        break;
                                    case 5:
                                        sender.sendMessage("There is no host with that name");
                                        break;
                                    case 6:
                                        sender.sendMessage("That host is not available");
                                        break;
                                    case 7:
                                        sender.sendMessage("That host is not enabled");
                                        break;
                                    case 8:
                                        sender.sendMessage("There is no template with that name");
                                        break;
                                    case 9:
                                        sender.sendMessage("That template is not enabled");
                                        break;
                                    case 10:
                                        sender.sendMessage("That template requires a Minecraft version to be specified");
                                        break;
                                    case 11:
                                        sender.sendMessage("Invalid port number");
                                        break;
                                    case 0:
                                        sender.sendMessage("Creating subserver " + args[1]);
                                        break;
                                }
                            }));
                        }
                    } else {
                        sender.sendMessage("Usage: /" + handle + " <Name> <Host> <Template> [Version] [Port]");
                    }
                }
            }
        }.autocomplete((sender, handle, args) -> {
            String Last = (args.length > 0)?args[args.length - 1]:"";
            String last = Last.toLowerCase();

            updateCache();
            if (args.length == 2) {
                List<String> list = new ArrayList<String>();
                for (Host host : hostCache.values()) {
                    if (host.getName().toLowerCase().startsWith(last)) list.add(Last + host.getName().substring(last.length()));
                }
                return list.toArray(new String[0]);
            } else if (args.length == 3) {
                List<String> list = new ArrayList<String>();
                Map<String, Host> hosts = hostCache;
                if (hosts.keySet().contains(args[1].toLowerCase())) {
                    for (SubCreator.ServerTemplate template : hosts.get(args[1].toLowerCase()).getCreator().getTemplates().values()) {
                        if (template.getName().toLowerCase().startsWith(last)) list.add(Last + template.getName().substring(last.length()));
                    }
                }
                return list.toArray(new String[0]);
            } else {
                return new String[0];
            }
        }).usage("<Name>", "<Host>", "<Template>", "[Version]", "[Port]").description("Creates a subserver").help(
                "Creates a subserver using the specified template on the specified host.",
                "",
                "The version argument is template-dependent in this command,",
                "meaning that it is only required if your template requires it.",
                "Similarly, it will only be used if your template uses it.",
                "",
                "Examples:",
                "  /create Server2 Host1 MyTemplate",
                "  /create Server2 Host1 Vanilla 1.12.2",
                "  /create Server2 Host1 Vanilla 1.12.2 25567"
        ).register("create");
        new Command(host.info) {
            @Override
            public void command(CommandSender sender, String handle, String[] args) {
                if (canRun()) {
                    if (args.length > 0) {
                        selectServers(sender, args, 0, true, select -> {
                            if (select.subservers.length > 0) {
                                PrimitiveContainer<Integer> success = new PrimitiveContainer<Integer>(0);
                                AsyncConsolidator merge = new AsyncConsolidator(() -> {
                                    if (success.value > 0) sender.sendMessage("Updating " + success.value + " subserver"+((success.value == 1)?"":"s"));
                                });
                                for (SubServer server : select.subservers) {
                                    merge.reserve();
                                    ((SubDataClient) host.api.getSubDataNetwork()[0]).sendPacket(new PacketUpdateServer(null, server.getName(), (select.args.length > 1)?new Version(select.args[1]):null, data -> {
                                        switch (data.getInt(0x0001)) {
                                            case 3:
                                            case 4:
                                                sender.sendMessage("Subserver " + server.getName() + " has disappeared");
                                                break;
                                            case 5:
                                                sender.sendMessage("The host for " + server.getName() + " is not available");
                                                break;
                                            case 6:
                                                sender.sendMessage("The host for " + server.getName() + " is not enabled");
                                                break;
                                            case 7:
                                                sender.sendMessage("Subserver " + server.getName() + " is not available");
                                                break;
                                            case 8:
                                                sender.sendMessage("Cannot update " + server.getName() + " while it is still running");
                                                break;
                                            case 9:
                                                sender.sendMessage("We don't know which template built " + server.getName());
                                                break;
                                            case 10:
                                                sender.sendMessage("The template that created " + server.getName() + " is not enabled");
                                                break;
                                            case 11:
                                                sender.sendMessage("The template that created " + server.getName() + " does not support subserver updating");
                                                break;
                                            case 12:
                                                sender.sendMessage("The template that created " + server.getName() + " requires a Minecraft version to be specified");
                                                break;
                                            case 0:
                                                success.value++;
                                                break;
                                        }
                                        merge.release();
                                    }));
                                }
                            }
                        });
                    } else {
                        sender.sendMessage("Usage: /" + handle + " <Subservers> [Version]");
                    }
                }
            }
        }.autocomplete(defaultCompletor).usage("<Subservers>", "[Version]").description("Updates one or more subservers").help(
                "Updates one or more subservers on the network.",
                "",
                "The version argument is template-dependent in this command,",
                "meaning that it is only required if your template requires it.",
                "Similarly, it will only be used if your template uses it.",
                "Additionally, your template might not support updating at all.",
                "If this is the case, the command will fail.",
                "",
                "When selecting multiple servers, keep in mind that some templates use this command",
                "to rebuild their cache, like the default templates Spigot and Vanilla.",
                "In such cases, you would want to update a single server first",
                "to rebuild the cache and then update the rest in bulk.",
                "",
                "Examples:",
                "  /update Server2",
                "  /update Server2 1.14.4"
        ).register("update", "upgrade");
        new Command(host.info) {
            @Override
            public void command(CommandSender sender, String handle, String[] args) {
                if (canRun()) {
                    if (args.length > 0) {
                        selectServers(sender, args, 0, true, select -> {
                            if (select.subservers.length > 0) {
                                PrimitiveContainer<Integer> success = new PrimitiveContainer<Integer>(0);
                                PrimitiveContainer<Integer> running = new PrimitiveContainer<Integer>(0);
                                AsyncConsolidator merge = new AsyncConsolidator(() -> {
                                    if (success.value > 0) sender.sendMessage("Removing " + success.value + " subserver"+((success.value == 1)?"":"s"));
                                });
                                for (SubServer server : select.subservers) {
                                    if (server.isRunning()) {
                                        sender.sendMessage("Subserver " + server.getName() + " is still running");
                                    } else {
                                        server.getHost(host -> {
                                            if (host == null) {
                                                sender.sendMessage("Subserver " + server.getName() + " has disappeared");
                                            } else {
                                                merge.reserve();
                                                host.recycleSubServer(server.getName(), response -> {
                                                    switch (response) {
                                                        case 3:
                                                        case 4:
                                                            sender.sendMessage("Subserver " + server.getName() + " has disappeared");
                                                            break;
                                                        case 0:
                                                            success.value++;
                                                            break;
                                                    }
                                                    merge.release();
                                                });
                                            }
                                        });
                                    }
                                }
                            }
                        });
                    } else {
                        sender.sendMessage("Usage: /" + handle + " <Subservers>");
                    }
                }
            }
        }.autocomplete(defaultCompletor).usage("<Subservers>").description("Removes one or more subservers").help(
                "Removes one or more subservers from the network",
                "and moves its files to the Recently Deleted folder.",
                "",
                "It should be noted that this is functionally the same internally as",
                "the API methods starting with .recycle, which are different from",
                "the API methods starting with .delete that actually permanently delete",
                "a server's files on disk. Even so, due to the destructive nature of",
                "this command, it is not available for in-game users.",
                "",
                "Example:",
                "  /delete Server2"
        ).register("del", "delete", "remove");
    }

    private void selectServers(CommandSender sender, String[] rargs, int index, boolean mode, Callback<ServerSelection> callback) {
        StackTraceElement[] origin = new Exception().getStackTrace();
        LinkedList<String> msgs = new LinkedList<String>();
        LinkedList<String> args = new LinkedList<String>();
        LinkedList<String> selection = new LinkedList<String>();
        LinkedList<Server> select = new LinkedList<Server>();
        Container<String> last = new Container<String>(null);

        // Step 1
        Container<Integer> ic = new Container<Integer>(0);
        while (ic.get() < index) {
            args.add(rargs[ic.get()]);
            ic.set(ic.get() + 1);
        }

        // Step 3
        StringBuilder completed = new StringBuilder();
        Runnable finished = () -> {
            args.add(completed.toString());

            int i = ic.get();
            while (i < rargs.length) {
                args.add(rargs[i]);
                last.set(null);
                i++;
            }

            LinkedList<Server> history = new LinkedList<Server>();
            LinkedList<Server> servers = new LinkedList<Server>();
            LinkedList<SubServer> subservers = new LinkedList<SubServer>();
            for (Server server : select) {
                if (!history.contains(server)) {
                    history.add(server);
                    servers.add(server);
                    if (server instanceof SubServer)
                        subservers.add((SubServer) server);
                }
            }

            if ((!mode && servers.size() <= 0) || (mode && subservers.size() <= 0)) {
                String msg = "No " + ((mode)?"sub":"") + "servers were selected";
                if (sender != null) sender.sendMessage(msg);
                msgs.add(msg);
            }

            try {
                callback.run(new ServerSelection(msgs, selection, servers, subservers, args, last.get()));
            } catch (Throwable e) {
                Throwable ew = new InvocationTargetException(e);
                ew.setStackTrace(origin);
                ew.printStackTrace();
            }
        };

        // Step 2
        AsyncConsolidator merge = new AsyncConsolidator(finished);
        for (boolean run = true; run && ic.get() < rargs.length; ic.set(ic.get() + 1)) {
            String current = rargs[ic.get()];
            last.set(current);
            completed.append(current);
            if (current.endsWith(",")) {
                current = current.substring(0, current.length() - 1);
                completed.append(' ');
            } else run = false;
            selection.add(current.toLowerCase());

            if (current.length() > 0) {
                merge.reserve();

                if (current.startsWith("::") && current.length() > 2) {
                    current = current.substring(2);

                    if (current.equals("*")) {
                        host.api.getHosts(hostMap -> {
                            for (Host host : hostMap.values()) {
                                select.addAll(host.getSubServers().values());
                            }
                            merge.release();
                        });
                    } else {
                        final String fcurrent = (current.equals("."))?host.api.getName():current;
                        host.api.getHost(fcurrent, host -> {
                            if (host != null) {
                                if (!select.addAll(host.getSubServers().values())) {
                                    String msg = "There are no " + ((mode)?"sub":"") + "servers on host: " + host.getName();
                                    if (sender != null) sender.sendMessage(msg);
                                    msgs.add(msg);
                                }
                            } else {
                                String msg = "There is no host with name: " + fcurrent;
                                if (sender != null) sender.sendMessage(msg);
                                msgs.add(msg);
                            }
                            merge.release();
                        });
                    }
                } else if (current.startsWith(":") && current.length() > 1) {
                    current = current.substring(1);

                    if (current.equals("*")) {
                        host.api.getGroups(groupMap -> {
                            for (List<Server> group : groupMap.values()) for (Server server : group) {
                                if (!mode || server instanceof SubServer) select.add(server);
                            }
                            merge.release();
                        });
                    } else {
                        final String fcurrent = current;
                        host.api.getGroup(current, group -> {
                            if (group != null) {
                                int i = 0;
                                for (Server server : group.get()) {
                                    if (!mode || server instanceof SubServer) {
                                        select.add(server);
                                        i++;
                                    }
                                }
                                if (i <= 0) {
                                    String msg = "There are no " + ((mode)?"sub":"") + "servers in group: " + group.name();
                                    if (sender != null) sender.sendMessage(msg);
                                    msgs.add(msg);
                                }
                            } else {
                                String msg = "There is no group with name: " + fcurrent;
                                if (sender != null) sender.sendMessage(msg);
                                msgs.add(msg);
                            }
                            merge.release();
                        });
                    }
                } else {

                    if (current.equals("*")) {
                        host.api.getServers(serverMap -> {
                            for (Server server : serverMap.values()) {
                                if (!mode || server instanceof SubServer) select.add(server);
                            }
                            merge.release();
                        });
                    } else {
                        final String fcurrent = current;
                        host.api.getServer(current, server -> {
                            if (server != null) {
                                select.add(server);
                            } else {
                                String msg = "There is no " + ((mode)?"sub":"") + "server with name: " + fcurrent;
                                if (sender != null) sender.sendMessage(msg);
                                msgs.add(msg);
                            }
                            merge.release();
                        });
                    }
                }
            }
        }
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

    private class ServerCompletion implements CompletionHandler {
        private final ServerCompletionHandler handler;
        private final boolean mode;
        private final int index;

        private ServerCompletion(int index, boolean mode, ServerCompletionHandler handler) {
            this.index = index;
            this.mode = mode;
            this.handler = handler;
        }

        @Override
        public String[] complete(CommandSender sender, String label, String[] args) {
            if (args.length >= index) {
                List<String> list = new ArrayList<String>();
                ServerSelection select = select(null, args, index, mode);
                if (select.last != null) {
                    String Last = (args.length > 0)?args[args.length - 1]:"";
                    String last = Last.toLowerCase();

                    if (last.startsWith("::")) {
                        Map<String, Host> hosts = hostCache;
                        if (hosts.size() > 0) {
                            if (Arrays.binarySearch(select.selection, "::*") < 0 && "::*".startsWith(last)) list.add("::*");
                            if (Arrays.binarySearch(select.selection, "::.") < 0 && "::.".startsWith(last)) list.add("::.");
                            for (Host host : hosts.values()) {
                                String name = "::" + host.getName();
                                if (Arrays.binarySearch(select.selection, name.toLowerCase()) < 0 && name.toLowerCase().startsWith(last)) list.add(Last + name.substring(last.length()));
                            }
                        }
                        return list.toArray(new String[0]);
                    } else if (last.startsWith(":")) {
                        Map<String, List<Server>> groups = groupCache;
                        if (groups.size() > 0) {
                            if (Arrays.binarySearch(select.selection, ":*") < 0 && ":*".startsWith(last)) list.add(":*");
                            for (String group : groups.keySet()) {
                                group = ":" + group;
                                if (Arrays.binarySearch(select.selection, group.toLowerCase()) < 0 && group.toLowerCase().startsWith(last)) list.add(Last + group.substring(last.length()));
                            }
                        }
                        return list.toArray(new String[0]);
                    } else {
                        Map<String, Server> subservers = serverCache;
                        if (subservers.size() > 0) {
                            if (Arrays.binarySearch(select.selection, "*") < 0 && "*".startsWith(last)) list.add("*");
                            for (Server server : subservers.values()) {
                                if ((!mode || server instanceof SubServer) && Arrays.binarySearch(select.selection, server.getName().toLowerCase()) < 0 && server.getName().toLowerCase().startsWith(last)) list.add(Last + server.getName().substring(last.length()));
                            }
                        }
                        return list.toArray(new String[0]);
                    }
                } else {
                    return handler.complete(sender, label, args, select);
                }
            } else {
                return handler.complete(sender, label, args, null);
            }
        }

        private ServerSelection select(CommandSender sender, String[] rargs, int index, boolean mode) {
            LinkedList<String> msgs = new LinkedList<String>();
            LinkedList<String> args = new LinkedList<String>();
            LinkedList<String> selection = new LinkedList<>();
            LinkedList<Server> servers = new LinkedList<Server>();
            String last = null;

            updateCache();

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
                    if (serverMap == null) serverMap = serverCache;

                    if (current.startsWith("::") && current.length() > 2) {
                        current = current.substring(2);
                        if (hostMap == null) hostMap = hostCache;

                        if (current.equals("*")) {
                            for (Host host : hostMap.values()) {
                                select.addAll(host.getSubServers().values());
                            }
                        } else {
                            if (current.equals(".")) current = host.api.getName();

                            Host host = hostMap.getOrDefault(current.toLowerCase(), null);
                            if (host != null) {
                                select.addAll(host.getSubServers().values());
                                if (select.size() <= 0) {
                                    String msg = "There are no " + ((mode)?"sub":"") + "servers on host: " + host.getName();
                                    if (sender != null) sender.sendMessage(msg);
                                    msgs.add(msg);
                                }
                            } else {
                                String msg = "There is no host with name: " + current;
                                if (sender != null) sender.sendMessage(msg);
                                msgs.add(msg);
                            }
                        }
                    } else if (current.startsWith(":") && current.length() > 1) {
                        current = current.substring(1);
                        if (groupMap == null) groupMap = groupCache;

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
                                    String msg = "There are no " + ((mode)?"sub":"") + "servers in group: " + group.getKey();
                                    if (sender != null) sender.sendMessage(msg);
                                    msgs.add(msg);
                                }
                            } else {
                                String msg = "There is no group with name: " + current;
                                if (sender != null) sender.sendMessage(msg);
                                msgs.add(msg);
                            }
                        }
                    } else {

                        if (current.equals("*")) {
                            for (Server server : serverMap.values()) {
                                if (!mode || server instanceof SubServer) select.add(server);
                            }
                        } else {
                            Server server = serverMap.getOrDefault(current.toLowerCase(), null);
                            if (server != null) {
                                select.add(server);
                            } else {
                                String msg = "There is no " + ((mode)?"sub":"") + "server with name: " + current;
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
                String msg = "No " + ((mode)?"sub":"") + "servers were selected";
                if (sender != null) sender.sendMessage(msg);
                msgs.add(msg);
            }

            return new ServerSelection(msgs, selection, servers, subservers, args, last);
        }
    }
    private interface ServerCompletionHandler {
        String[] complete(CommandSender sender, String label, String[] args, ServerSelection select);
    }

    private static void updateCache() {
        if (Calendar.getInstance().getTime().getTime() - cacheDate >= TimeUnit.MINUTES.toMillis(1)) {
            cacheDate = Calendar.getInstance().getTime().getTime();
            SubAPI.getInstance().getProxies(proxies -> {
                proxyCache = new TreeMap<>(proxies);
                cacheDate = Calendar.getInstance().getTime().getTime();
            });
            SubAPI.getInstance().getMasterProxy(master -> {
                proxyMasterCache = master;
                cacheDate = Calendar.getInstance().getTime().getTime();
            });
            SubAPI.getInstance().getHosts(hosts -> {
                hostCache = new TreeMap<>(hosts);
                cacheDate = Calendar.getInstance().getTime().getTime();
            });
            SubAPI.getInstance().getGroups(groups -> {
                groupCache = new TreeMap<>(groups);
                cacheDate = Calendar.getInstance().getTime().getTime();
            });
            SubAPI.getInstance().getServers(servers -> {
                serverCache = new TreeMap<>(servers);
                cacheDate = Calendar.getInstance().getTime().getTime();
            });
        }

    }
}

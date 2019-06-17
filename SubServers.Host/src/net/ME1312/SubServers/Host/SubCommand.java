package net.ME1312.SubServers.Host;

import net.ME1312.Galaxi.Engine.GalaxiEngine;
import net.ME1312.Galaxi.Library.Callback.Callback;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Map.ObjectMapValue;
import net.ME1312.Galaxi.Library.Container;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.Galaxi.Plugin.Command.Command;
import net.ME1312.Galaxi.Plugin.Command.CommandSender;
import net.ME1312.Galaxi.Plugin.Command.CompletionHandler;
import net.ME1312.Galaxi.Plugin.PluginManager;
import net.ME1312.SubData.Client.SubDataClient;
import net.ME1312.SubServers.Host.Library.TextColor;
import net.ME1312.SubServers.Host.Network.API.*;
import net.ME1312.SubServers.Host.Network.Packet.*;

import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Command Class
 */
public class SubCommand {
    private static LinkedList<String> proxyCache = new LinkedList<String>();
    private static TreeMap<String, List<String>> hostCache = new TreeMap<String, List<String>>();
    private static LinkedList<String> groupCache = new LinkedList<String>();
    private static TreeMap<String, Boolean> serverCache = new TreeMap<String, Boolean>();
    private static long cacheDate = 0;

    private static boolean canRun() {
        if (SubAPI.getInstance().getSubDataNetwork()[0] == null) {
            throw new IllegalStateException("SubData is not connected");
        } else {
            return true;
        }
    }

    private SubCommand() {}
    @SuppressWarnings("unchecked")
    protected static void load(ExHost host) {
        CompletionHandler defaultCompletor;
        new Command(host.info) {
            @Override
            public void command(CommandSender sender, String handle, String[] args) {
                if (canRun()) {
                    if (args.length > 0) {
                        int i = 0;
                        String str = args[0];
                        if (args.length > 1) {
                            do {
                                i++;
                                str = str + " " + args[i].replace(" ", "\\ ");
                            } while ((i + 1) != args.length);
                        }
                        GalaxiEngine.getInstance().getConsoleReader().runCommand(sender, str);
                    } else {
                        sender.sendMessage("Usage: /" + handle + " <Command> [Args...]");
                    }
                }
            }
        }.autocomplete((sender, handle, args) -> {
            String last = (args.length > 0)?args[args.length - 1].toLowerCase():"";
            TreeMap<String, Command> commands;
            try {
                commands = Util.reflect(PluginManager.class.getDeclaredField("commands"), GalaxiEngine.getInstance().getPluginManager());
            } catch (Exception e) {
                SubAPI.getInstance().getAppInfo().getLogger().error.println(e);
                commands = new TreeMap<String, Command>();
            }
            if (args.length <= 1) {
                if (last.length() == 0) {
                    return commands.keySet().toArray(new String[0]);
                } else {
                    List<String> list = new ArrayList<String>();
                    for (String command : commands.keySet()) {
                        if (command.toLowerCase().startsWith(last)) list.add(command);
                    }
                    return list.toArray(new String[0]);
                }
            } else if (commands.keySet().contains(args[0].toLowerCase())) {
                CompletionHandler autocompletor = commands.get(args[0].toLowerCase()).autocomplete();
                if (autocompletor != null) {
                    LinkedList<String> arguments = new LinkedList<String>();
                    arguments.addAll(Arrays.asList(args));
                    arguments.removeFirst();
                    return autocompletor.complete(sender, args[0], arguments.toArray(new String[0]));
                } else return new String[0];
            } else {
                return new String[0];
            }
        }).usage("<Command>", "[Args...]").description("An alias for commands").help(
                "This command is an alias for all registered commands for ease of use.",
                "",
                "Examples:",
                "  /sub help -> /help",
                "  /sub version ExamplePlugin -> /version ExamplePlugin"
        ).register("sub", "subserver", "subservers");
        new Command(host.info) {
            @Override
            public void command(CommandSender sender, String handle, String[] args) {
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
                                message += server.getDisplayName() + " (" + server.getAddress().getAddress().getHostAddress()+':'+server.getAddress().getPort() + ((server.getName().equals(server.getDisplayName())) ? "" : TextColor.stripColor(div) + server.getName()) + ")";
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
                        message += host.getDisplayName() + " (" + host.getAddress().getHostAddress() + ((host.getName().equals(host.getDisplayName()))?"":TextColor.stripColor(div)+host.getName()) + ")" + TextColor.RESET + ": ";
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
                            message += subserver.getDisplayName() + " (" + subserver.getAddress().getPort() + ((subserver.getName().equals(subserver.getDisplayName()))?"":TextColor.stripColor(div)+subserver.getName()) + ")";
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
                        message += TextColor.WHITE + server.getDisplayName() + " (" + server.getAddress().getAddress().getHostAddress()+':'+server.getAddress().getPort() + ((server.getName().equals(server.getDisplayName()))?"":TextColor.stripColor(div)+server.getName()) + ")";
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
                            message += proxy.getDisplayName() + ((proxy.getName().equals(proxy.getDisplayName()))?"":" ("+proxy.getName()+')');
                        }
                        sender.sendMessage(message);
                    }
                })))));
            }
        }.description("Lists the available Hosts and Servers").help(
                "This command will print a list of the available Hosts and Servers.",
                "You can then use these names in commands where applicable.",
                "",
                "Example:",
                "  /list"
        ).register("list");
        new Command(host.info) {
            @Override
            public void command(CommandSender sender, String handle, String[] args) {
                if (canRun()) {
                    if (args.length > 0) {
                        String type = (args.length > 1)?args[0]:null;
                        String name = args[(type != null)?1:0];
    
                        Runnable getServer = () -> host.api.getServer(name, server -> {
                            if (server != null) {
                                sender.sendMessage("SubServers > Info on " + ((server instanceof SubServer)?"Sub":"") + "Server: " + TextColor.WHITE + server.getDisplayName());
                                if (!server.getName().equals(server.getDisplayName())) sender.sendMessage(" -> System Name: " + TextColor.WHITE  + server.getName());
                                if (server instanceof SubServer) {
                                    sender.sendMessage(" -> Available: " + ((((SubServer) server).isAvailable())?TextColor.GREEN+"yes":TextColor.RED+"no"));
                                    sender.sendMessage(" -> Enabled: " + ((((SubServer) server).isEnabled())?TextColor.GREEN+"yes":TextColor.RED+"no"));
                                    if (!((SubServer) server).isEditable()) sender.sendMessage(" -> Editable: " + TextColor.RED + "no");
                                    sender.sendMessage(" -> Host: " + TextColor.WHITE  + ((SubServer) server).getHost());
                                    if (((SubServer) server).getTemplate() != null) sender.sendMessage(" -> Template: " + TextColor.WHITE  + ((SubServer) server).getTemplate());
                                }
                                if (server.getGroups().size() > 0) sender.sendMessage(" -> Group" + ((server.getGroups().size() > 1)?"s:":": " + TextColor.WHITE + server.getGroups().get(0)));
                                if (server.getGroups().size() > 1) for (String group : server.getGroups()) sender.sendMessage("      - " + TextColor.WHITE + group);
                                sender.sendMessage(" -> Address: " + TextColor.WHITE + server.getAddress().getAddress().getHostAddress()+':'+server.getAddress().getPort());
                                if (server instanceof SubServer) sender.sendMessage(" -> Running: " + ((((SubServer) server).isRunning())?TextColor.GREEN+"yes":TextColor.RED+"no"));
                                if (!(server instanceof SubServer) || ((SubServer) server).isRunning()) {
                                    sender.sendMessage(" -> Connected: " + ((server.getSubData()[0] != null)?TextColor.GREEN+"yes"+((server.getSubData().length > 1)?TextColor.AQUA+" +"+(server.getSubData().length-1)+" subchannel"+((server.getSubData().length == 2)?"":"s"):""):TextColor.RED+"no"));
                                    sender.sendMessage(" -> Players: " + TextColor.AQUA + server.getPlayers().size() + " online");
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
                                    sender.sendMessage("SubServers > There is no object with that name");
                                } else {
                                    sender.sendMessage("SubServers > There is no server with that name");
                                }
                            }
                        });
                        Runnable getGroup = () -> host.api.getGroup(name, group -> {
                            if (group != null) {
                                sender.sendMessage("SubServers > Info on Group: " + TextColor.WHITE + name);
                                sender.sendMessage(" -> Servers: " + ((group.size() <= 0)?TextColor.GRAY + "(none)":TextColor.AQUA.toString() + group.size()));
                                for (Server server : group) sender.sendMessage("      - " + TextColor.WHITE + server.getDisplayName() + ((server.getName().equals(server.getDisplayName()))?"":" ("+server.getName()+')'));
                            } else {
                                if (type == null) {
                                    getServer.run();
                                } else {
                                    sender.sendMessage("SubServers > There is no group with that name");
                                }
                            }
                        });
                        Runnable getHost = () -> host.api.getHost(name, host -> {
                            if (host != null) {
                                sender.sendMessage("SubServers > Info on Host: " + TextColor.WHITE + host.getDisplayName());
                                if (!host.getName().equals(host.getDisplayName())) sender.sendMessage(" -> System Name: " + TextColor.WHITE  + host.getName());
                                sender.sendMessage(" -> Available: " + ((host.isAvailable())?TextColor.GREEN+"yes":TextColor.RED+"no"));
                                sender.sendMessage(" -> Enabled: " + ((host.isEnabled())?TextColor.GREEN+"yes":TextColor.RED+"no"));
                                sender.sendMessage(" -> Address: " + TextColor.WHITE + host.getAddress().getHostAddress());
                                if (host.getSubData().length > 0) sender.sendMessage(" -> Connected: " + ((host.getSubData()[0] != null)?TextColor.GREEN+"yes"+((host.getSubData().length > 1)?TextColor.AQUA+" +"+(host.getSubData().length-1)+" subchannel"+((host.getSubData().length == 2)?"":"s"):""):TextColor.RED+"no"));
                                sender.sendMessage(" -> SubServers: " + ((host.getSubServers().keySet().size() <= 0)?TextColor.GRAY + "(none)":TextColor.AQUA.toString() + host.getSubServers().keySet().size()));
                                for (SubServer subserver : host.getSubServers().values()) sender.sendMessage("      - " + ((subserver.isEnabled())?TextColor.WHITE:TextColor.GRAY) + subserver.getDisplayName() + ((subserver.getName().equals(subserver.getDisplayName()))?"":" ("+subserver.getName()+')'));
                                sender.sendMessage(" -> Templates: " + ((host.getCreator().getTemplates().keySet().size() <= 0)?TextColor.GRAY + "(none)":TextColor.AQUA.toString() + host.getCreator().getTemplates().keySet().size()));
                                for (SubCreator.ServerTemplate template : host.getCreator().getTemplates().values()) sender.sendMessage("      - " + ((template.isEnabled())?TextColor.WHITE:TextColor.GRAY) + template.getDisplayName() + ((template.getName().equals(template.getDisplayName()))?"":" ("+template.getName()+')'));
                                sender.sendMessage(" -> Signature: " + TextColor.AQUA + host.getSignature());
                            } else {
                                if (type == null) {
                                    getGroup.run();
                                } else {
                                    sender.sendMessage("SubServers > There is no host with that name");
                                }
                            }
                        });
                        Runnable getProxy = () -> host.api.getProxy(name, proxy -> {
                            if (proxy != null) {
                                sender.sendMessage("SubServers > Info on Proxy: " + TextColor.WHITE + proxy.getDisplayName());
                                if (!proxy.getName().equals(proxy.getDisplayName())) sender.sendMessage(" -> System Name: " + TextColor.WHITE  + proxy.getName());
                                sender.sendMessage(" -> Connected: " + ((proxy.getSubData()[0] != null)?TextColor.GREEN+"yes"+((proxy.getSubData().length > 1)?TextColor.AQUA+" +"+(proxy.getSubData().length-1)+" subchannel"+((proxy.getSubData().length == 2)?"":"s"):""):TextColor.RED+"no"));
                                sender.sendMessage(" -> Redis: "  + ((proxy.isRedis())?TextColor.GREEN:TextColor.RED+"un") + "available");
                                if (proxy.isRedis()) sender.sendMessage(" -> Players: " + TextColor.AQUA + proxy.getPlayers().size() + " online");
                                sender.sendMessage(" -> Signature: " + TextColor.AQUA + proxy.getSignature());
                            } else {
                                if (type == null) {
                                    getHost.run();
                                } else {
                                    sender.sendMessage("SubServers > There is no proxy with that name");
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
                                default:
                                    sender.sendMessage("SubServers > There is no object type with that name");
                            }
                        }
                    } else {
                        sender.sendMessage("SubServers > Usage: /" + handle + " [proxy|host|group|server] <Name>");
                    }
                }
            }
        }.autocomplete((sender, handle, args) -> {
            String last = (args.length > 0)?args[args.length - 1].toLowerCase():"";
            TreeMap<String, Command> commands;
            if (args.length == 1) {
                updateCache();
                List<String> list = new ArrayList<String>();
                List<String> subcommands = new ArrayList<String>();
                subcommands.add("proxy");
                subcommands.add("host");
                subcommands.add("group");
                subcommands.add("server");
                if (last.length() == 0) {
                    list.addAll(subcommands);
                    for (String proxy : proxyCache) if (!list.contains(proxy)) list.add(proxy);
                    for (String h : hostCache.keySet()) if (!list.contains(h)) list.add(h);
                    for (String group : groupCache) if (!list.contains(group)) list.add(group);
                    for (String server : serverCache.keySet()) if (!list.contains(server)) list.add(server);
                } else {
                    for (String command : subcommands) {
                        if (!list.contains(command) && command.toLowerCase().startsWith(last))
                            list.add(last + command.substring(last.length()));
                    }
                    for (String proxy : proxyCache) {
                        if (!list.contains(proxy) && proxy.toLowerCase().startsWith(last))
                            list.add(last + proxy.substring(last.length()));
                    }
                    for (String h : hostCache.keySet()) {
                        if (!list.contains(h) && h.toLowerCase().startsWith(last))
                            list.add(last + h.substring(last.length()));
                    }
                    for (String group : groupCache) {
                        if (!list.contains(group) && group.toLowerCase().startsWith(last))
                            list.add(last + group.substring(last.length()));
                    }
                    for (String server : serverCache.keySet()) {
                        if (!list.contains(server) && server.toLowerCase().startsWith(last))
                            list.add(last + server.substring(last.length()));
                    }
                }
                return list.toArray(new String[0]);
            } else if (args.length == 2) {
                updateCache();
                List<String> list = new ArrayList<String>();
                if (last.length() == 0) {
                    switch (args[0].toLowerCase()) {
                        case "p":
                        case "proxy":
                            list.addAll(proxyCache);
                            break;
                        case "h":
                        case "host":
                            list.addAll(hostCache.keySet());
                            break;
                        case "g":
                        case "group":
                            list.addAll(groupCache);
                            break;
                        case "s":
                        case "server":
                        case "subserver":
                            list.addAll(serverCache.keySet());
                            break;
                    }
                } else {
                    switch (args[0].toLowerCase()) {
                        case "p":
                        case "proxy":
                            for (String proxy : proxyCache) {
                                if (proxy.toLowerCase().startsWith(last))
                                    list.add(last + proxy.substring(last.length()));
                            }
                            break;
                        case "h":
                        case "host":
                            for (String h : hostCache.keySet()) {
                                if (h.toLowerCase().startsWith(last))
                                    list.add(last + h.substring(last.length()));
                            }
                            break;
                        case "g":
                        case "group":
                            for (String group : groupCache) {
                                if (group.toLowerCase().startsWith(last))
                                    list.add(last + group.substring(last.length()));
                            }
                            break;
                        case "s":
                        case "server":
                        case "subserver":
                            for (String server : serverCache.keySet()) {
                                if (!list.contains(server) && server.toLowerCase().startsWith(last))
                                    list.add(last + server.substring(last.length()));
                            }
                            break;
                    }
                }
                return list.toArray(new String[0]);
            } else {
                return new String[0];
            }
        }).usage("[proxy|host|group|server]", "<Name>").description("Gets information about an Object").help(
                "This command will print a list of information about",
                "the specified Object.",
                "",
                "If the [proxy|host|group|server] option is provided,",
                "it will only include objects of the type specified in the search.",
                "",
                "The <Name> argument is required, and should be the name of",
                "the Object you want to obtain information about.",
                "",
                "Examples:",
                "  /info ExampleServer",
                "  /info server ExampleServer"
        ).register("info", "status");
        new Command(host.info) {
            @Override
            public void command(CommandSender sender, String handle, String[] args) {
                if (canRun()) {
                    if (args.length > 0) {
                        ((SubDataClient) SubAPI.getInstance().getSubDataNetwork()[0]).sendPacket(new PacketStartServer(null, args[0], data -> {
                            switch (data.getInt(0x0001)) {
                                case 3:
                                    sender.sendMessage("There is no server with that name");
                                    break;
                                case 4:
                                    sender.sendMessage("That Server is not a SubServer");
                                    break;
                                case 5:
                                    sender.sendMessage("That SubServer's Host is not available");
                                    break;
                                case 6:
                                    sender.sendMessage("That SubServer's Host is not enabled");
                                    break;
                                case 7:
                                    sender.sendMessage("That SubServer is not available");
                                    break;
                                case 8:
                                    sender.sendMessage("That SubServer is not enabled");
                                    break;
                                case 9:
                                    sender.sendMessage("That SubServer is already running");
                                    break;
                                case 10:
                                    sender.sendMessage("That SubServer cannot start while these server(s) are running:", data.getRawString(0x0002));
                                    break;
                                case 0:
                                case 1:
                                    sender.sendMessage("Server was started successfully");
                                    break;
                            }
                        }));
                    } else {
                        sender.sendMessage("Usage: /" + handle + " <SubServer>");
                    }
                }
            }
        }.autocomplete(defaultCompletor = (sender, handle, args) -> {
            String last = (args.length > 0)?args[args.length - 1].toLowerCase():"";
            List<String> list = new ArrayList<String>();
            if (args.length == 1) {
                updateCache();
                if (last.length() == 0) {
                    for (String server : serverCache.keySet()) if (serverCache.get(server) == Boolean.TRUE) list.add(server);
                } else {
                    for (String server : serverCache.keySet()) {
                        if (serverCache.get(server) == Boolean.TRUE && server.toLowerCase().startsWith(last));
                            list.add(last + server.substring(last.length()));
                    }
                }
                return list.toArray(new String[0]);
            } else {
                return new String[0];
            }
        }).usage("<SubServer>").description("Starts a SubServer").help(
                "This command is used to start a SubServer on the network.",
                "Once it has been started, you can control it via the other commands",
                "",
                "The <SubServer> argument is required, and should be the name of",
                "the SubServer you want to start.",
                "",
                "Example:",
                "  /start ExampleServer"
        ).register("start");
        new Command(host.info) {
            @Override
            public void command(CommandSender sender, String handle, String[] args) {
                if (canRun()) {
                    if (args.length > 0) {
                        TimerTask starter = new TimerTask() {
                            @Override
                            public void run() {
                                ((SubDataClient) SubAPI.getInstance().getSubDataNetwork()[0]).sendPacket(new PacketStartServer(null, args[0], data -> {
                                    switch (data.getInt(0x0001)) {
                                        case 3:
                                        case 4:
                                            sender.sendMessage("Could not restart server: That SubServer has disappeared");
                                            break;
                                        case 5:
                                            sender.sendMessage("Could not restart server: That SubServer's Host is no longer available");
                                            break;
                                        case 6:
                                            sender.sendMessage("Could not restart server: That SubServer's Host is no longer enabled");
                                            break;
                                        case 7:
                                            sender.sendMessage("Could not restart server: That SubServer is no longer available");
                                            break;
                                        case 8:
                                            sender.sendMessage("Could not restart server: That SubServer is no longer enabled");
                                            break;
                                        case 10:
                                            sender.sendMessage("Could not restart server: That SubServer cannot start while these server(s) are running:", data.getRawString(0x0002));
                                            break;
                                        case 9:
                                        case 0:
                                        case 1:
                                            sender.sendMessage("Server was started successfully");
                                            break;
                                    }
                                }));
                            }
                        };
    
                        final Container<Boolean> listening = new Container<Boolean>(true);
                        PacketInExRunEvent.callback("SubStoppedEvent", new Callback<ObjectMap<String>>() {
                            @Override
                            public void run(ObjectMap<String> json) {
                                try {
                                    if (listening.get()) if (!json.getString("server").equalsIgnoreCase(args[0])) {
                                        PacketInExRunEvent.callback("SubStoppedEvent", this);
                                    } else {
                                        new Timer(SubAPI.getInstance().getAppInfo().getName() + "::Server_Restart_Command_Handler(" + args[0] + ')').schedule(starter, 100);
                                    }
                                } catch (Exception e) {}
                            }
                        });
    
                        ((SubDataClient) SubAPI.getInstance().getSubDataNetwork()[0]).sendPacket(new PacketStopServer(null, args[0], false, data -> {
                            if (data.getInt(0x0001) != 0) listening.set(false);
                            switch (data.getInt(0x0001)) {
                                case 3:
                                    sender.sendMessage("There is no server with that name");
                                    break;
                                case 4:
                                    sender.sendMessage("That Server is not a SubServer");
                                    break;
                                case 5:
                                    starter.run();
                                    break;
                                case 0:
                                case 1:
                                    sender.sendMessage("Server was stopped successfully");
                                    break;
                            }
                        }));
                    } else {
                        sender.sendMessage("Usage: /" + handle + " <SubServer>");
                    }
                }
            }
        }.autocomplete(defaultCompletor).usage("<SubServer>").description("Restarts a SubServer").help(
                "This command is used to request a SubServer to restart via the network.",
                "Restarting a SubServer in this way will run the stop command",
                "specified in the server's configuration before re-launching the start command.",
                "",
                "The <SubServer> argument is required, and should be the name of",
                "the SubServer you want to restart.",
                "",
                "Example:",
                "  /restart ExampleServer"
        ).register("restart");
        new Command(host.info) {
            @Override
            public void command(CommandSender sender, String handle, String[] args) {
                if (canRun()) {
                    if (args.length > 0) {
                        ((SubDataClient) SubAPI.getInstance().getSubDataNetwork()[0]).sendPacket(new PacketStopServer(null, args[0], false, data -> {
                            switch (data.getInt(0x0001)) {
                                case 3:
                                    sender.sendMessage("There is no server with that name");
                                    break;
                                case 4:
                                    sender.sendMessage("That Server is not a SubServer");
                                    break;
                                case 5:
                                    sender.sendMessage("That SubServer is not running");
                                    break;
                                case 0:
                                case 1:
                                    sender.sendMessage("Server was stopped successfully");
                                    break;
                            }
                        }));
                    } else {
                        sender.sendMessage("Usage: /" + handle + " <SubServer>");
                    }
                }
            }
        }.autocomplete(defaultCompletor).usage("<SubServer>").description("Stops a SubServer").help(
                "This command is used to request a SubServer to stop via the network.",
                "Stopping a SubServer in this way will run the stop command",
                "specified in the server's configuration",
                "",
                "The <SubServer> argument is required, and should be the name of",
                "the SubServer you want to stop.",
                "",
                "Example:",
                "  /stop ExampleServer"
        ).register("stop");
        new Command(host.info) {
            @Override
            public void command(CommandSender sender, String handle, String[] args) {
                if (canRun()) {
                    if (args.length > 0) {
                        ((SubDataClient) SubAPI.getInstance().getSubDataNetwork()[0]).sendPacket(new PacketStopServer(null, args[0], true, data -> {
                            switch (data.getInt(0x0001)) {
                                case 3:
                                    sender.sendMessage("There is no server with that name");
                                    break;
                                case 4:
                                    sender.sendMessage("That Server is not a SubServer");
                                    break;
                                case 5:
                                    sender.sendMessage("That SubServer is not running");
                                    break;
                                case 0:
                                case 1:
                                    sender.sendMessage("Server was terminated successfully");
                                    break;
                            }
                        }));
                    } else {
                        sender.sendMessage("Usage: /" + handle + " <SubServer>");
                    }
                }
            }
        }.autocomplete(defaultCompletor).usage("<SubServer>").description("Terminates a SubServer").help(
                "This command is used to forcefully stop a SubServer on the network.",
                "Stopping a SubServer in this way can make you lose unsaved data though,",
                "so it is generally recommended to use this command only when it stops responding.",
                "",
                "The <SubServer> argument is required, and should be the name of",
                "the SubServer you want to terminate.",
                "",
                "Example:",
                "  /kill ExampleServer"
        ).register("kill", "terminate");
        new Command(host.info) {
            @Override
            public void command(CommandSender sender, String handle, String[] args) {
                if (canRun()) {
                    if (args.length > 1) {
                        int i = 1;
                        String str = args[1];
                        if (args.length > 2) {
                            do {
                                i++;
                                str = str + " " + args[i];
                            } while ((i + 1) != args.length);
                        }
                        final String cmd = str;
                        ((SubDataClient) SubAPI.getInstance().getSubDataNetwork()[0]).sendPacket(new PacketCommandServer(null, args[0], cmd, data -> {
                            switch (data.getInt(0x0001)) {
                                case 3:
                                    sender.sendMessage("There is no server with that name");
                                    break;
                                case 4:
                                    sender.sendMessage("That Server is not a SubServer");
                                    break;
                                case 5:
                                    sender.sendMessage("That SubServer is not running");
                                    break;
                                case 0:
                                case 1:
                                    sender.sendMessage("Command was sent successfully");
                                    break;
                            }
                        }));
                    } else {
                        sender.sendMessage("Usage: /" + handle + " <SubServer> <Command> [Args...]");
                    }
                }
            }
        }.autocomplete(defaultCompletor).usage("<SubServer>", "<Command>", "[Args...]").description("Sends a Command to a SubServer").help(
                "This command is used to send a command to a SubServer's Console via the network.",
                "",
                "The <SubServer> argument is required, and should be the name of",
                "the SubServer you want to send a command to.",
                "",
                "The <Command> argument is required, and should be the command you",
                "want to send, the following [Args...] will be passed to that command.",
                "",
                "Examples:",
                "  /cmd ExampleServer help",
                "  /cmd ExampleServer say Hello World!"
        ).register("cmd", "command");
        new Command(host.info) {
            @Override
            public void command(CommandSender sender, String handle, String[] args) {
                if (canRun()) {
                    if (args.length > 2) {
                        if (args.length > 4 && Util.isException(() -> Integer.parseInt(args[4]))) {
                            sender.sendMessage("Invalid Port Number");
                        } else {
                            ((SubDataClient) SubAPI.getInstance().getSubDataNetwork()[0]).sendPacket(new PacketCreateServer(null, args[0], args[1], args[2], (args.length > 3)?new Version(args[3]):null, (args.length > 4)?Integer.parseInt(args[4]):null, data -> {
                                switch (data.getInt(0x0001)) {
                                    case 3:
                                        sender.sendMessage("Server names cannot use spaces");
                                    case 4:
                                        sender.sendMessage("There is already a SubServer with that name");
                                        break;
                                    case 5:
                                        sender.sendMessage("There is no host with that name");
                                        break;
                                    case 6:
                                        sender.sendMessage("That Host is not available");
                                        break;
                                    case 7:
                                        sender.sendMessage("That Host is not enabled");
                                        break;
                                    case 8:
                                        sender.sendMessage("There is no template with that name");
                                        break;
                                    case 9:
                                        sender.sendMessage("That Template is not enabled");
                                        break;
                                    case 10:
                                        sender.sendMessage("That Template requires a Minecraft Version to be specified");
                                        break;
                                    case 11:
                                        sender.sendMessage("Invalid Port Number");
                                        break;
                                    case 0:
                                    case 1:
                                        sender.sendMessage("Launching SubCreator...");
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
            String last = (args.length > 0)?args[args.length - 1].toLowerCase():"";
            if (args.length == 2) {
                updateCache();
                List<String> list = new ArrayList<String>();
                if (cacheDate <= 0) {
                } else if (last.length() == 0) {
                    list.addAll(hostCache.keySet());
                } else {
                    for (String h : hostCache.keySet()) {
                        if (h.toLowerCase().startsWith(last)) list.add(last + h.substring(last.length()));
                    }
                }
                return list.toArray(new String[0]);
            } else if (args.length == 3) {
                updateCache();
                List<String> list = new ArrayList<String>();
                if (cacheDate <= 0 || !hostCache.keySet().contains(args[1].toLowerCase())) {
                } else if (last.length() == 0) {
                    list.addAll(hostCache.get(args[1].toLowerCase()));
                } else {
                    for (String template : hostCache.get(args[1].toLowerCase())) {
                        if (template.toLowerCase().startsWith(last)) list.add(last + template.substring(last.length()));
                    }
                }
                return list.toArray(new String[0]);
            } else {
                return new String[0];
            }
        }).usage("<Name>", "<Host>", "<Template>", "[Version]", "[Port]").description("Creates a SubServer").help(
                "This command is used to create and launch a SubServer on the specified host via the network.",
                "Templates are downloaded from SubServers.Bungee to ./Templates.",
                "",
                "The <Name> argument is required, and should be the name of",
                "the SubServer you want to create.",
                "",
                "The <Host> argument is required, and should be the name of",
                "the host you want to the server to run on.",
                "",
                "The <Template> argument is required, and should be the name of",
                "the template you want to create your server with.",
                "",
                "When the [Version] argument is provided, it will set the",
                "Minecraft version of the type of server that you want to create",
                "",
                "When the [Port] argument is provided, it will set the port number",
                "the server will listen on after it has been created.",
                "",
                "Examples:",
                "  /create ExampleServer ExampleHost Spigot",
                "  /create ExampleServer ExampleHost Spigot 1.12.2",
                "  /create ExampleServer ExampleHost Spigot 1.12.2 25565"
        ).register("create");
        new Command(host.info) {
            @Override
            public void command(CommandSender sender, String handle, String[] args) {
                if (canRun()) {
                    if (args.length > 0) {
                        ((SubDataClient) SubAPI.getInstance().getSubDataNetwork()[0]).sendPacket(new PacketUpdateServer(null, args[0], (args.length > 1)?new Version(args[1]):null, data -> {
                            switch (data.getInt(0x0001)) {
                                case 3:
                                    sender.sendMessage("There is no server with that name");
                                    break;
                                case 4:
                                    sender.sendMessage("That Server is not a SubServer");
                                    break;
                                case 5:
                                    sender.sendMessage("That SubServer's Host is not available");
                                    break;
                                case 6:
                                    sender.sendMessage("That SubServer's Host is not enabled");
                                    break;
                                case 7:
                                    sender.sendMessage("That SubServer is not available");
                                    break;
                                case 8:
                                    sender.sendMessage("Cannot update servers while they are still running");
                                    break;
                                case 9:
                                    sender.sendMessage("We don't know which template created that SubServer");
                                    break;
                                case 10:
                                    sender.sendMessage("That SubServer's Template is not enabled");
                                    break;
                                case 11:
                                    sender.sendMessage("That SubServer's Template does not support server updating");
                                    break;
                                case 12:
                                    sender.sendMessage("That SubServer's Template requires a Minecraft Version to be specified");
                                    break;
                                case 0:
                                case 1:
                                    sender.sendMessage("Launching SubCreator...");
                                    break;
                            }
                        }));
                    } else {
                        sender.sendMessage("Usage: /" + handle + " <SubServer> [Version]");
                    }
                }
            }
        }.autocomplete((sender, handle, args) -> {
            String last = (args.length > 0)?args[args.length - 1].toLowerCase():"";
            if (args.length == 1) {
                updateCache();
                List<String> list = new ArrayList<String>();
                if (last.length() == 0) {
                    for (String server : serverCache.keySet()) if (serverCache.get(server) == Boolean.TRUE) list.add(server);
                } else {
                    for (String server : serverCache.keySet()) {
                        if (serverCache.get(server) == Boolean.TRUE && server.toLowerCase().startsWith(last));
                        list.add(last + server.substring(last.length()));
                    }
                }
                return list.toArray(new String[0]);
            } else {
                return new String[0];
            }
        }).usage("<SubServer>", "[Version]").description("Updates a SubServer").help(
                "This command is used to update a SubServer via the network.",
                "Templates are downloaded from SubServers.Bungee to ./Templates.",
                "",
                "The <SubServer> argument is required, and should be the name of",
                "the SubServer you want to update.",
                "",
                "When the [Version] argument is provided, it will set the",
                "Minecraft version of the type of server that you want to update to",
                "",
                "Examples:",
                "  /update ExampleServer",
                "  /update ExampleServer 1.12.2"
        ).register("update", "upgrade");
    }

    private static void updateCache() {
        if (canRun()) {
            if (Calendar.getInstance().getTime().getTime() - cacheDate >= TimeUnit.MINUTES.toMillis(1)) {
                cacheDate = Calendar.getInstance().getTime().getTime();
                SubAPI.getInstance().getProxies(proxies -> {
                    proxyCache = new LinkedList<String>(proxies.keySet());
                    cacheDate = Calendar.getInstance().getTime().getTime();
                });
                SubAPI.getInstance().getHosts(hosts -> {
                    TreeMap<String, List<String>> cache = new TreeMap<String, List<String>>();
                    for (Host host : hosts.values()) {
                        List<String> templates = new ArrayList<String>();
                        templates.addAll(host.getCreator().getTemplates().keySet());
                        cache.put(host.getName().toLowerCase(), templates);
                    }
                    hostCache = cache;
                    cacheDate = Calendar.getInstance().getTime().getTime();
                });
                SubAPI.getInstance().getGroups(groups -> {
                    groupCache = new LinkedList<String>(groups.keySet());
                    cacheDate = Calendar.getInstance().getTime().getTime();
                });
                SubAPI.getInstance().getServers(servers -> {
                    TreeMap<String, Boolean> cache = new TreeMap<String, Boolean>();
                    for (Server server : servers.values()) {
                        cache.put(server.getName(), server instanceof SubServer);
                    }
                    serverCache = cache;
                    cacheDate = Calendar.getInstance().getTime().getTime();
                });
            }
        }

    }
}

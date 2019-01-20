package net.ME1312.SubServers.Sync;

import com.google.gson.Gson;
import net.ME1312.SubServers.Sync.Library.Callback;
import net.ME1312.SubServers.Sync.Library.Compatibility.CommandX;
import net.ME1312.SubServers.Sync.Library.Config.YAMLSection;
import net.ME1312.SubServers.Sync.Library.Container;
import net.ME1312.SubServers.Sync.Library.NamedContainer;
import net.ME1312.SubServers.Sync.Library.Util;
import net.ME1312.SubServers.Sync.Library.Version.Version;
import net.ME1312.SubServers.Sync.Network.API.*;
import net.ME1312.SubServers.Sync.Network.Packet.*;
import net.ME1312.SubServers.Sync.Server.ServerContainer;
import net.ME1312.SubServers.Sync.Server.SubServerContainer;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.command.ConsoleCommandSender;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("deprecation")
public final class SubCommand extends CommandX {
    private LinkedList<String> proxyCache = new LinkedList<String>();
    private TreeMap<String, List<String>> hostCache = new TreeMap<String, List<String>>();
    private LinkedList<String> groupCache = new LinkedList<String>();
    private long cacheDate = 0;
    private SubPlugin plugin;
    private String label;

    protected static NamedContainer<SubCommand, CommandX> newInstance(SubPlugin plugin, String command) {
        NamedContainer<SubCommand, CommandX> cmd = new NamedContainer<>(new SubCommand(plugin, command), null);
        CommandX now = cmd.name();
        //if (plugin.api.getGameVersion()[plugin.api.getGameVersion().length - 1].compareTo(new Version("1.13")) >= 0) { // TODO Future Command Validator API?
        //    now = new net.ME1312.SubServers.Sync.Library.Compatibility.mc1_13.CommandX(cmd.name());
        //}
        cmd.set(now);
        return cmd;
    }

    private SubCommand(SubPlugin plugin, String command) {
        super(command);
        this.plugin = plugin;
        this.label = '/' + command;
    }

    @SuppressWarnings("unchecked")
    @Override
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

                    sender.sendMessage("SubServers > These are the platforms and versions that are running SubServers.Sync:");
                    sender.sendMessage("  " + System.getProperty("os.name") + ((!System.getProperty("os.name").toLowerCase().startsWith("windows"))?' ' + System.getProperty("os.version"):"") + ((osarch != null)?" [" + osarch + ']':"") + ',');
                    sender.sendMessage("  Java " + System.getProperty("java.version") + ((javaarch != null)?" [" + javaarch + ']':"") + ',');
                    sender.sendMessage("  " + plugin.getBungeeName() + ((plugin.isPatched)?" [Patched] ":" ") + net.md_5.bungee.Bootstrap.class.getPackage().getImplementationVersion() + ',');
                    sender.sendMessage("  SubServers.Sync v" + SubPlugin.version.toExtendedString() + ((plugin.api.getWrapperBuild() != null)?" (" + plugin.api.getWrapperBuild() + ')':""));
                    sender.sendMessage("");
                    new Thread(() -> {
                        try {
                            YAMLSection tags = new YAMLSection(new Gson().fromJson("{\"tags\":" + Util.readAll(new BufferedReader(new InputStreamReader(new URL("https://api.github.com/repos/ME1312/SubServers-2/git/refs/tags").openStream(), Charset.forName("UTF-8")))) + '}', Map.class));
                            List<Version> versions = new LinkedList<Version>();

                            Version updversion = plugin.version;
                            int updcount = 0;
                            for (YAMLSection tag : tags.getSectionList("tags")) versions.add(Version.fromString(tag.getString("ref").substring(10)));
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
                                sender.sendMessage("SubServers.Sync v" + updversion + " is available. You are " + updcount + " version" + ((updcount == 1)?"":"s") + " behind.");
                            }
                        } catch (Exception e) {
                        }
                    }, "SubServers.Sync::Update_Check").start();
                } else if (args[0].equalsIgnoreCase("list")) {
                    plugin.api.getGroups(groups -> plugin.api.getHosts(hosts -> plugin.api.getServers(servers -> plugin.api.getMasterProxy(proxymaster -> plugin.api.getProxies(proxies -> {
                        int i = 0;
                        boolean sent = false;
                        String div = ChatColor.RESET + ", ";
                        if (groups.keySet().size() > 0) {
                            sender.sendMessage("Group/Server List:");
                            for (String group : groups.keySet()) {
                                String message = "  ";
                                message += ChatColor.GOLD + group + ChatColor.RESET + ": ";
                                for (Server server : groups.get(group)) {
                                    if (i != 0) message += div;
                                    if (!(server instanceof SubServer)) {
                                        message += ChatColor.WHITE;
                                    } else if (((SubServer) server).isRunning()) {
                                        if (((SubServer) server).getStopAction() == SubServer.StopAction.REMOVE_SERVER || ((SubServer) server).getStopAction() == SubServer.StopAction.DELETE_SERVER) {
                                            message += ChatColor.AQUA;
                                        } else {
                                            message += ChatColor.GREEN;
                                        }
                                    } else if (((SubServer) server).isEnabled() && ((SubServer) server).getCurrentIncompatibilities().size() == 0) {
                                        message += ChatColor.YELLOW;
                                    } else {
                                        message += ChatColor.RED;
                                    }
                                    message += server.getDisplayName() + " (" + server.getAddress().getAddress().getHostAddress()+':'+server.getAddress().getPort() + ((server.getName().equals(server.getDisplayName())) ? "" : ChatColor.stripColor(div) + server.getName()) + ")";
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
                        sender.sendMessage("Host/SubServer List:");
                        for (Host host : hosts.values()) {
                            String message = "  ";
                            if (host.isEnabled()) {
                                message += ChatColor.AQUA;
                            } else {
                                message += ChatColor.RED;
                            }
                            message += host.getDisplayName() + " (" + host.getAddress().getHostAddress() + ((host.getName().equals(host.getDisplayName()))?"":ChatColor.stripColor(div)+host.getName()) + ")" + ChatColor.RESET + ": ";
                            for (SubServer subserver : host.getSubServers().values()) {
                                if (i != 0) message += div;
                                if (subserver.isRunning()) {
                                    if (subserver.getStopAction() == SubServer.StopAction.REMOVE_SERVER || subserver.getStopAction() == SubServer.StopAction.DELETE_SERVER) {
                                        message += ChatColor.AQUA;
                                    } else {
                                        message += ChatColor.GREEN;
                                    }
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
                        sender.sendMessage("Server List:");
                        String message = "  ";
                        for (Server server : servers.values()) if (!(server instanceof SubServer)) {
                            if (i != 0) message += div;
                            message += ChatColor.WHITE + server.getDisplayName() + " (" + server.getAddress().getAddress().getHostAddress()+':'+server.getAddress().getPort() + ((server.getName().equals(server.getDisplayName()))?"":ChatColor.stripColor(div)+server.getName()) + ")";
                            i++;
                        }
                        if (i == 0) message += ChatColor.RESET + "(none)";
                        sender.sendMessage(message);
                        if (proxies.keySet().size() > 0) {
                            sender.sendMessage("Proxy List:");
                            message = "  (master)";
                            for (Proxy proxy : proxies.values()) {
                                message += div;
                                if (proxy.getSubData() != null && proxy.isRedis()) {
                                    message += ChatColor.GREEN;
                                } else if (proxy.getSubData() != null) {
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
                    })))));
                } else if (args[0].equalsIgnoreCase("info") || args[0].equalsIgnoreCase("status")) {
                    if (args.length > 1) {
                        String type = (args.length > 2)?args[1]:null;
                        String name = args[(type != null)?2:1];

                        Runnable getServer = () -> plugin.api.getServer(name, server -> {
                            if (server != null) {
                                sender.sendMessage("SubServers > Info on " + ((server instanceof SubServer)?"Sub":"") + "Server: " + ChatColor.WHITE + server.getDisplayName());
                                if (!server.getName().equals(server.getDisplayName())) sender.sendMessage(" -> System Name: " + ChatColor.WHITE  + server.getName());
                                if (server instanceof SubServer) {
                                    sender.sendMessage(" -> Enabled: " + ((((SubServer) server).isEnabled())?ChatColor.GREEN+"yes":ChatColor.RED+"no"));
                                    if (!((SubServer) server).isEditable()) sender.sendMessage(" -> Editable: " + ChatColor.RED + "no");
                                    sender.sendMessage(" -> Host: " + ChatColor.WHITE  + ((SubServer) server).getHost());
                                }
                                if (server.getGroups().size() > 0) sender.sendMessage(" -> Group" + ((server.getGroups().size() > 1)?"s:":": " + ChatColor.WHITE + server.getGroups().get(0)));
                                if (server.getGroups().size() > 1) for (String group : server.getGroups()) sender.sendMessage("      - " + ChatColor.WHITE + group);
                                sender.sendMessage(" -> Address: " + ChatColor.WHITE + server.getAddress().getAddress().getHostAddress()+':'+server.getAddress().getPort());
                                if (server instanceof SubServer) sender.sendMessage(" -> Running: " + ((((SubServer) server).isRunning())?ChatColor.GREEN+"yes":ChatColor.RED+"no"));
                                if (!(server instanceof SubServer) || ((SubServer) server).isRunning()) {
                                    sender.sendMessage(" -> Connected: " + ((server.getSubData() != null)?ChatColor.GREEN+"yes":ChatColor.RED+"no"));
                                    sender.sendMessage(" -> Players: " + ChatColor.AQUA + server.getPlayers().size() + " online");
                                }
                                sender.sendMessage(" -> MOTD: " + ChatColor.WHITE + ChatColor.stripColor(server.getMotd()));
                                if (server instanceof SubServer && ((SubServer) server).getStopAction() != SubServer.StopAction.NONE) sender.sendMessage(" -> Stop Action: " + ChatColor.WHITE + ((SubServer) server).getStopAction().toString());
                                sender.sendMessage(" -> Signature: " + ChatColor.AQUA + server.getSignature());
                                if (server instanceof SubServer) sender.sendMessage(" -> Logging: " + ((((SubServer) server).isLogging())?ChatColor.GREEN+"yes":ChatColor.RED+"no"));
                                sender.sendMessage(" -> Restricted: " + ((server.isRestricted())?ChatColor.GREEN+"yes":ChatColor.RED+"no"));
                                if (server instanceof SubServer && ((SubServer) server).getIncompatibilities().size() > 0) {
                                    List<String> current = new ArrayList<String>();
                                    for (String other : ((SubServer) server).getCurrentIncompatibilities()) current.add(other.toLowerCase());
                                    sender.sendMessage(" -> Incompatibilities:");
                                    for (String other : ((SubServer) server).getIncompatibilities()) sender.sendMessage("      - " + ((current.contains(other.toLowerCase()))?ChatColor.WHITE:ChatColor.GRAY) + other);
                                }
                                sender.sendMessage(" -> Hidden: " + ((server.isHidden())?ChatColor.GREEN+"yes":ChatColor.RED+"no"));
                            } else {
                                if (type == null) {
                                    sender.sendMessage("SubServers > There is no object with that name");
                                } else {
                                    sender.sendMessage("SubServers > There is no server with that name");
                                }
                            }
                        });
                        Runnable getGroup = () -> plugin.api.getGroup(name, group -> {
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
                        });
                        Runnable getHost = () -> plugin.api.getHost(name, host -> {
                            if (host != null) {
                                sender.sendMessage("SubServers > Info on Host: " + ChatColor.WHITE + host.getDisplayName());
                                if (!host.getName().equals(host.getDisplayName())) sender.sendMessage(" -> System Name: " + ChatColor.WHITE  + host.getName());
                                sender.sendMessage(" -> Available: " + ((host.isAvailable())?ChatColor.GREEN+"yes":ChatColor.RED+"no"));
                                sender.sendMessage(" -> Enabled: " + ((host.isEnabled())?ChatColor.GREEN+"yes":ChatColor.RED+"no"));
                                sender.sendMessage(" -> Address: " + ChatColor.WHITE + host.getAddress().getHostAddress());
                                if (host.getSubData() != null) sender.sendMessage(" -> Connected: " + ChatColor.GREEN + "yes");
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
                        });
                        Runnable getProxy = () -> plugin.api.getProxy(name, proxy -> {
                            if (proxy != null) {
                                sender.sendMessage("SubServers > Info on Proxy: " + ChatColor.WHITE + proxy.getDisplayName());
                                if (!proxy.getName().equals(proxy.getDisplayName())) sender.sendMessage(" -> System Name: " + ChatColor.WHITE  + proxy.getName());
                                sender.sendMessage(" -> Connected: " + ((proxy.getSubData() != null)?ChatColor.GREEN+"yes":ChatColor.RED+"no"));
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
                        sender.sendMessage("SubServers > Usage: " + label + " " + args[0].toLowerCase() + " [proxy|host|group|server] <Name>");
                    }
                } else if (args[0].equalsIgnoreCase("start")) {
                    if (args.length > 1) {
                        plugin.subdata.sendPacket(new PacketStartServer(null, args[1], data -> {
                            switch (data.getInt("r")) {
                                case 3:
                                    sender.sendMessage("SubServers > There is no server with that name");
                                    break;
                                case 4:
                                    sender.sendMessage("SubServers > That Server is not a SubServer");
                                    break;
                                case 5:
                                    sender.sendMessage("SubServers > That SubServer's Host is not available");
                                    break;
                                case 6:
                                    sender.sendMessage("SubServers > That SubServer's Host is not enabled");
                                    break;
                                case 7:
                                    sender.sendMessage("SubServers > That SubServer is not enabled");
                                    break;
                                case 8:
                                    sender.sendMessage("SubServers > That SubServer is already running");
                                    break;
                                case 9:
                                    sender.sendMessage("SubServers > That SubServer cannot start while these server(s) are running: " + data.getRawString("m").split(":\\s")[1]);
                                    break;
                                default:
                                    System.out.println("PacketStartServer(null, " + args[1] + ") responded with: " + data.getRawString("m"));
                                case 0:
                                case 1:
                                    sender.sendMessage("SubServers > Server was started successfully");
                                    break;
                            }
                        }));
                    } else {
                        sender.sendMessage("Usage: " + label + " " + args[0].toLowerCase() + " <SubServer>");
                    }
                } else if (args[0].equalsIgnoreCase("restart")) {
                    if (args.length > 1) {
                        TimerTask starter = new TimerTask() {
                            @Override
                            public void run() {
                                plugin.subdata.sendPacket(new PacketStartServer(null, args[1], data -> {
                                    switch (data.getInt("r")) {
                                        case 3:
                                        case 4:
                                            sender.sendMessage("SubServers > Could not restart server: That SubServer has disappeared");
                                            break;
                                        case 5:
                                            sender.sendMessage("SubServers > Could not restart server: That SubServer's Host is no longer available");
                                            break;
                                        case 6:
                                            sender.sendMessage("SubServers > Could not restart server: That SubServer's Host is no longer enabled");
                                            break;
                                        case 7:
                                            sender.sendMessage("SubServers > Could not restart server: That SubServer is no longer enabled");
                                            break;
                                        case 9:
                                            sender.sendMessage("SubServers > Could not restart server: That SubServer cannot start while these server(s) are running: " + data.getRawString("m").split(":\\s")[1]);
                                            break;
                                        default:
                                            System.out.println("PacketStartServer(null, " + args[1] + ") responded with: " + data.getRawString("m"));
                                        case 8:
                                        case 0:
                                        case 1:
                                            sender.sendMessage("SubServers > Server was started successfully");
                                            break;
                                    }
                                }));
                            }
                        };

                        final Container<Boolean> listening = new Container<Boolean>(true);
                        PacketInRunEvent.callback("SubStoppedEvent", new Callback<YAMLSection>() {
                            @Override
                            public void run(YAMLSection json) {
                                try {
                                    if (listening.get()) if (!json.getString("server").equalsIgnoreCase(args[1])) {
                                        PacketInRunEvent.callback("SubStoppedEvent", this);
                                    } else {
                                        new Timer("SubServers.Sync::Server_Restart_Command_Handler(" + args[1] + ')').schedule(starter, 100);
                                    }
                                } catch (Exception e) {}
                            }
                        });

                        plugin.subdata.sendPacket(new PacketStopServer(null, args[1], false, data -> {
                            if (data.getInt("r") != 0) listening.set(false);
                            switch (data.getInt("r")) {
                                case 3:
                                    sender.sendMessage("SubServers > There is no server with that name");
                                    break;
                                case 4:
                                    sender.sendMessage("SubServers > That Server is not a SubServer");
                                    break;
                                case 5:
                                    starter.run();
                                    break;
                                default:
                                    System.out.println("PacketStopServer(null, " + args[1] + ", false) responded with: " + data.getRawString("m"));
                                case 0:
                                case 1:
                                    sender.sendMessage("SubServers > Server was stopped successfully");
                                    break;
                            }
                        }));
                    } else {
                        sender.sendMessage("Usage: " + label + " " + args[0].toLowerCase() + " <SubServer>");
                    }
                } else if (args[0].equalsIgnoreCase("stop")) {
                    if (args.length > 1) {
                        plugin.subdata.sendPacket(new PacketStopServer(null, args[1], false, data -> {
                            switch (data.getInt("r")) {
                                case 3:
                                    sender.sendMessage("SubServers > There is no server with that name");
                                    break;
                                case 4:
                                    sender.sendMessage("SubServers > That Server is not a SubServer");
                                    break;
                                case 5:
                                    sender.sendMessage("SubServers > That SubServer is not running");
                                    break;
                                default:
                                    System.out.println("PacketStopServer(null, " + args[1] + ", false) responded with: " + data.getRawString("m"));
                                case 0:
                                case 1:
                                    sender.sendMessage("SubServers > Server was stopped successfully");
                                    break;
                            }
                        }));
                    } else {
                        sender.sendMessage("Usage: " + label + " " + args[0].toLowerCase() + " <SubServer>");
                    }
                } else if (args[0].equalsIgnoreCase("kill") || args[0].equalsIgnoreCase("terminate")) {
                    if (args.length > 1) {
                        plugin.subdata.sendPacket(new PacketStopServer(null, args[1], true, data -> {
                            switch (data.getInt("r")) {
                                case 3:
                                    sender.sendMessage("SubServers > There is no server with that name");
                                    break;
                                case 4:
                                    sender.sendMessage("SubServers > That Server is not a SubServer");
                                    break;
                                case 5:
                                    sender.sendMessage("SubServers > That SubServer is not running");
                                    break;
                                default:
                                    System.out.println("PacketStopServer(null, " + args[1] + ", true) responded with: " + data.getRawString("m"));
                                case 0:
                                case 1:
                                    sender.sendMessage("SubServers > Server was terminated successfully");
                                    break;
                            }
                        }));
                    } else {
                        sender.sendMessage("Usage: " + label + " " + args[0].toLowerCase() + " <SubServer>");
                    }
                } else if (args[0].equalsIgnoreCase("cmd") || args[0].equalsIgnoreCase("command")) {
                    if (args.length > 2) {
                        int i = 2;
                        String str = args[2];
                        if (args.length > 3) {
                            do {
                                i++;
                                str = str + " " + args[i];
                            } while ((i + 1) != args.length);
                        }
                        final String cmd = str;
                        plugin.subdata.sendPacket(new PacketCommandServer(null, args[1], cmd, data -> {
                            switch (data.getInt("r")) {
                                case 3:
                                    sender.sendMessage("SubServers > There is no server with that name");
                                    break;
                                case 4:
                                    sender.sendMessage("SubServers > That Server is not a SubServer");
                                    break;
                                case 5:
                                    sender.sendMessage("SubServers > That SubServer is not running");
                                    break;
                                default:
                                    System.out.println("PacketCommandServer(null, " + args[1] + ", /" + cmd + ") responded with: " + data.getRawString("m"));
                                case 0:
                                case 1:
                                    sender.sendMessage("SubServers > Command was sent successfully");
                                    break;
                            }
                        }));
                    } else {
                        sender.sendMessage("Usage: " + label + " " + args[0].toLowerCase() + " <SubServer> <Command> [Args...]");
                    }
                } else if (args[0].equalsIgnoreCase("create")) {
                    if (args.length > 4) {
                        if (args.length > 5 && Util.isException(() -> Integer.parseInt(args[5]))) {
                            sender.sendMessage("Invalid Port Number");
                        } else {
                            plugin.subdata.sendPacket(new PacketCreateServer(null, args[1], args[2],args[3], new Version(args[4]), (args.length > 5)?Integer.parseInt(args[5]):null, data -> {
                                switch (data.getInt("r")) {
                                    case 3:
                                    case 4:
                                        sender.sendMessage("SubServers > There is already a SubServer with that name");
                                        break;
                                    case 5:
                                        sender.sendMessage("SubServers > There is no host with that name");
                                        break;
                                    case 6:
                                        sender.sendMessage("SubServers > That Host is not available");
                                        break;
                                    case 7:
                                        sender.sendMessage("SubServers > That Host is not enabled");
                                        break;
                                    case 8:
                                        sender.sendMessage("SubServers > There is no template with that name");
                                        break;
                                    case 9:
                                        sender.sendMessage("SubServers > That Template is not available");
                                        break;
                                    case 10:
                                        sender.sendMessage("SubServers > SubCreator cannot create servers before Minecraft 1.8");
                                        break;
                                    case 11:
                                        sender.sendMessage("SubServers > Invalid Port Number");
                                        break;
                                    default:
                                        System.out.println("PacketCreateServer(null, " + args[1] + ", " + args[2] + ", " + args[3] + ", " + args[4] + ", " + ((args.length > 5)?args[5]:"null") + ") responded with: " + data.getRawString("m"));
                                    case 0:
                                    case 1:
                                        sender.sendMessage("SubServers > Launching SubCreator...");
                                        break;
                                }
                            }));
                        }
                    } else {
                        sender.sendMessage("Usage: " + label + " " + args[0].toLowerCase() + " <Name> <Host> <Template> <Version> <Port>");
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
                "   Info: /sub info [proxy|host|group|server] <Name>",
                "   Start Server: /sub start <SubServer>",
                "   Restart Server: /sub restart <SubServer>",
                "   Stop Server: /sub stop <SubServer>",
                "   Terminate Server: /sub kill <SubServer>",
                "   Command Server: /sub cmd <SubServer> <Command> [Args...]",
                "   Create Server: /sub create <Name> <Host> <Template> <Version> <Port>",
                "",
                "   To see BungeeCord Supplied Commands, please visit:",
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
            List<String> cmds = Arrays.asList("help", "list", "info", "status", "version", "start", "restart", "stop", "kill", "terminate", "cmd", "command", "create");
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
                        for (String host : hostCache.keySet()) if (!list.contains(host)) list.add(host);
                        for (String group : groupCache) if (!list.contains(group)) list.add(group);
                        for (ServerContainer server : plugin.servers.values()) if (!list.contains(server.getName())) list.add(server.getName());
                    } else {
                        for (String command : subcommands) {
                            if (!list.contains(command) && command.toLowerCase().startsWith(last))
                                list.add(last + command.substring(last.length()));
                        }
                        for (String proxy : proxyCache) {
                            if (!list.contains(proxy) && proxy.toLowerCase().startsWith(last))
                                list.add(last + proxy.substring(last.length()));
                        }
                        for (String host : hostCache.keySet()) {
                            if (!list.contains(host) && host.toLowerCase().startsWith(last))
                                list.add(last + host.substring(last.length()));
                        }
                        for (String group : groupCache) {
                            if (!list.contains(group) && group.toLowerCase().startsWith(last))
                                list.add(last + group.substring(last.length()));
                        }
                        for (ServerContainer server : plugin.servers.values()) {
                            if (!list.contains(server.getName()) && server.getName().toLowerCase().startsWith(last))
                                list.add(last + server.getName().substring(last.length()));
                        }
                    }
                    return new NamedContainer<>((list.size() <= 0)?plugin.api.getLang("SubServers", "Command.Info.Unknown").replace("$str$", args[0]):null, list);
                } else if (args.length == 3) {
                    updateCache();
                    List<String> list = new ArrayList<String>();
                    if (last.length() == 0) {
                        switch (args[1].toLowerCase()) {
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
                                for (ServerContainer server : plugin.servers.values()) list.add(server.getName());
                                break;
                        }
                    } else {
                        switch (args[1].toLowerCase()) {
                            case "p":
                            case "proxy":
                                for (String proxy : proxyCache) {
                                    if (proxy.toLowerCase().startsWith(last))
                                        list.add(last + proxy.substring(last.length()));
                                }
                                break;
                            case "h":
                            case "host":
                                for (String host : hostCache.keySet()) {
                                    if (host.toLowerCase().startsWith(last))
                                        list.add(last + host.substring(last.length()));
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
                                for (ServerContainer server : plugin.servers.values()) {
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
            } else if (args[0].equals("start") ||
                    args[0].equals("restart")) {
                List<String> list = new ArrayList<String>();
                if (args.length == 2) {
                    if (last.length() == 0) {
                        for (ServerContainer server : plugin.servers.values()) if (server instanceof SubServerContainer) list.add(server.getName());
                    } else {
                        for (ServerContainer server : plugin.servers.values()) {
                            if (server instanceof SubServerContainer && server.getName().toLowerCase().startsWith(last)) list.add(last + server.getName().substring(last.length()));
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
                        for (ServerContainer server : plugin.servers.values()) if (server instanceof SubServerContainer) list.add(server.getName());
                    } else {
                        if ("*".startsWith(last)) list.add("*");
                        for (ServerContainer server : plugin.servers.values()) {
                            if (server instanceof SubServerContainer && server.getName().toLowerCase().startsWith(last)) list.add(last + server.getName().substring(last.length()));
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
                        for (ServerContainer server : plugin.servers.values()) if (server instanceof SubServerContainer) list.add(server.getName());
                    } else {
                        for (ServerContainer server : plugin.servers.values()) {
                            if (server instanceof SubServerContainer && server.getName().toLowerCase().startsWith(last)) list.add(last + server.getName().substring(last.length()));
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
                    updateCache();
                    List<String> list = new ArrayList<String>();
                    if (cacheDate <= 0) {
                        list.add("<Host>");
                    } else if (last.length() == 0) {
                        list.addAll(hostCache.keySet());
                    } else {
                        for (String host : hostCache.keySet()) {
                            if (host.toLowerCase().startsWith(last)) list.add(last + host.substring(last.length()));
                        }
                    }
                    return new NamedContainer<>((list.size() <= 0)?plugin.api.getLang("SubServers", "Command.Generic.Unknown-Host").replace("$str$", args[0]):null, list);
                } else if (args.length == 4) {
                    updateCache();
                    List<String> list = new ArrayList<String>();
                    if (cacheDate <= 0 || !hostCache.keySet().contains(args[2].toLowerCase())) {
                        list.add("<Template>");
                    } else if (last.length() == 0) {
                        list.addAll(hostCache.get(args[2].toLowerCase()));
                    } else {
                        for (String template : hostCache.get(args[2].toLowerCase())) {
                            if (template.toLowerCase().startsWith(last)) list.add(last + template.substring(last.length()));
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

    private void updateCache() {
        if (Calendar.getInstance().getTime().getTime() - cacheDate >= TimeUnit.MINUTES.toMillis(1)) {
            cacheDate = Calendar.getInstance().getTime().getTime();
            plugin.api.getProxies(proxies -> {
                proxyCache = new LinkedList<String>(proxies.keySet());
                cacheDate = Calendar.getInstance().getTime().getTime();
            });
            plugin.api.getHosts(hosts -> {
                TreeMap<String, List<String>> cache = new TreeMap<String, List<String>>();
                for (Host host : hosts.values()) {
                    List<String> templates = new ArrayList<String>();
                    templates.addAll(host.getCreator().getTemplates().keySet());
                    cache.put(host.getName().toLowerCase(), templates);
                }
                hostCache = cache;
                cacheDate = Calendar.getInstance().getTime().getTime();
            });
            plugin.api.getGroups(groups -> {
                groupCache = new LinkedList<String>(groups.keySet());
                cacheDate = Calendar.getInstance().getTime().getTime();
            });
        }

    }

    /**
     * BungeeCord /server
     */
    @SuppressWarnings("unchecked")
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
            //    now = new net.ME1312.SubServers.Sync.Library.Compatibility.mc1_13.CommandX(cmd.name());
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
                    Map<String, ServerContainer> servers = plugin.servers;
                    if (servers.keySet().contains(args[0].toLowerCase())) {
                        ((ProxiedPlayer) sender).connect(servers.get(args[0].toLowerCase()));
                    } else {
                        sender.sendMessage(plugin.api.getLang("SubServers", "Bungee.Server.Invalid"));
                    }
                } else {
                    int i = 0;
                    TextComponent serverm = new TextComponent(ChatColor.RESET.toString());
                    TextComponent div = new TextComponent(plugin.api.getLang("SubServers", "Bungee.Server.Divider"));
                    for (ServerContainer server : plugin.servers.values()) {
                        if (!server.isHidden() && server.canAccess(sender) && (!(server instanceof SubServerContainer) || ((SubServerContainer) server).isRunning())) {
                            if (i != 0) serverm.addExtra(div);
                            TextComponent message = new TextComponent(plugin.api.getLang("SubServers", "Bungee.Server.List").replace("$str$", server.getDisplayName()));
                            try {
                                message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponent[]{new TextComponent(plugin.api.getLang("SubServers", "Bungee.Server.Hover").replace("$int$", Integer.toString((plugin.redis)?((Set<UUID>)plugin.redis("getPlayersOnServer", new NamedContainer<>(String.class, server.getName()))).size():server.getPlayers().size())))}));
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
                    for (ServerContainer server : plugin.servers.values()) {
                        if (!server.isHidden()) list.add(server.getName());
                    }
                    return new NamedContainer<>(null, new LinkedList<>(list));
                } else {
                    for (ServerContainer server : plugin.servers.values()) {
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
    @SuppressWarnings("unchecked")
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
            for (ServerContainer server : plugin.servers.values()) {
                List<String> playerlist = new ArrayList<String>();
                if (plugin.redis) {
                    try {
                        for (UUID player : (Set<UUID>) plugin.redis("getPlayersOnServer", new NamedContainer<>(String.class, server.getName()))) playerlist.add((String) plugin.redis("getNameFromUuid", new NamedContainer<>(UUID.class, player)));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    for (ProxiedPlayer player : server.getPlayers()) playerlist.add(player.getName());
                }
                Collections.sort(playerlist);

                players += playerlist.size();
                if (!server.isHidden() && (!(server instanceof SubServerContainer) || ((SubServerContainer) server).isRunning())) {
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
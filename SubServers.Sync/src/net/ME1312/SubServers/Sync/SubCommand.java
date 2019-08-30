package net.ME1312.SubServers.Sync;

import com.google.gson.Gson;
import net.ME1312.Galaxi.Library.Callback.Callback;
import net.ME1312.SubData.Client.SubDataClient;
import net.ME1312.SubServers.Sync.Library.Compatibility.CommandX;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Container;
import net.ME1312.Galaxi.Library.NamedContainer;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.SubServers.Sync.Library.Compatibility.GalaxiInfo;
import net.ME1312.SubServers.Sync.Network.API.*;
import net.ME1312.SubServers.Sync.Network.Packet.*;
import net.ME1312.SubServers.Sync.Server.ServerImpl;
import net.ME1312.SubServers.Sync.Server.SubServerImpl;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.command.ConsoleCommandSender;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static net.ME1312.SubServers.Sync.Library.Compatibility.GalaxiCommand.*;

@SuppressWarnings("deprecation")
public final class SubCommand extends CommandX {
    static HashMap<UUID, HashMap<ServerInfo, NamedContainer<Long, Boolean>>> players = new HashMap<UUID, HashMap<ServerInfo, NamedContainer<Long, Boolean>>>();
    private LinkedList<String> proxyCache = new LinkedList<String>();
    private TreeMap<String, List<String>> hostCache = new TreeMap<String, List<String>>();
    private LinkedList<String> groupCache = new LinkedList<String>();
    private long cacheDate = 0;
    private ExProxy plugin;
    private String label;

    protected static NamedContainer<SubCommand, CommandX> newInstance(ExProxy plugin, String command) {
        NamedContainer<SubCommand, CommandX> cmd = new NamedContainer<>(new SubCommand(plugin, command), null);
        CommandX now = cmd.name();
        //if (plugin.api.getGameVersion()[plugin.api.getGameVersion().length - 1].compareTo(new Version("1.13")) >= 0) { // TODO Future Command Validator API?
        //    now = new net.ME1312.SubServers.Sync.Library.Compatibility.mc1_13.CommandX(cmd.name());
        //}
        cmd.set(now);
        return cmd;
    }

    private SubCommand(ExProxy plugin, String command) {
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

    @SuppressWarnings("unchecked")
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof ProxiedPlayer)) {
            if (plugin.api.getSubDataNetwork()[0] == null) {
                new IllegalStateException("SubData is not connected").printStackTrace();
                if (!(sender instanceof ConsoleCommandSender)) sender.sendMessage(ChatColor.RED + "An exception has occurred while running this command");
            } else {
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

                        Version galaxi = GalaxiInfo.getVersion();
                        Version bungee = Util.getDespiteException(() -> (Version) BungeeCord.class.getMethod("getBuildVersion").invoke(plugin), null);
                        Version galaxibuild = GalaxiInfo.getSignature();
                        Version bungeebuild = Util.getDespiteException(() -> (Version) BungeeCord.class.getMethod("getBuildSignature").invoke(plugin), null);

                        sender.sendMessage("SubServers > These are the platforms and versions that are running SubServers.Sync:");
                        sender.sendMessage("  " + System.getProperty("os.name") + ((!System.getProperty("os.name").toLowerCase().startsWith("windows"))?' ' + System.getProperty("os.version"):"") + ((osarch != null)?" [" + osarch + ']':"") + ',');
                        sender.sendMessage("  Java " + System.getProperty("java.version") + ((javaarch != null)?" [" + javaarch + ']':"") + ',');
                        if (galaxi != null)
                            Util.isException(() -> sender.sendMessage("GalaxiEngine v" + galaxi.toExtendedString() + ((galaxibuild != null)?" (" + galaxibuild + ')':"") + ','));
                        sender.sendMessage("  " + plugin.getBungeeName() + ((plugin.isGalaxi)?" v":" ") + ((bungee != null)?bungee:plugin.getVersion()) + ((bungeebuild != null)?" (" + bungeebuild + ')':"") + ((plugin.isPatched)?" [Patched]":"") + ',');
                        sender.sendMessage("  SubServers.Sync v" + ExProxy.version.toExtendedString() + ((plugin.api.getWrapperBuild() != null)?" (" + plugin.api.getWrapperBuild() + ')':""));
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
                                            if (((SubServer) server).getStopAction() == SubServer.StopAction.REMOVE_SERVER || ((SubServer) server).getStopAction() == SubServer.StopAction.RECYCLE_SERVER || ((SubServer) server).getStopAction() == SubServer.StopAction.DELETE_SERVER) {
                                                message += ChatColor.AQUA;
                                            } else {
                                                message += ChatColor.GREEN;
                                            }
                                        } else if (((SubServer) server).isAvailable() && ((SubServer) server).isEnabled() && ((SubServer) server).getCurrentIncompatibilities().size() == 0) {
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
                                    } else if (subserver.isAvailable() && subserver.isEnabled() && subserver.getCurrentIncompatibilities().size() == 0) {
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
                                        sender.sendMessage(" -> Available: " + ((((SubServer) server).isAvailable())?ChatColor.GREEN+"yes":ChatColor.RED+"no"));
                                        sender.sendMessage(" -> Enabled: " + ((((SubServer) server).isEnabled())?ChatColor.GREEN+"yes":ChatColor.RED+"no"));
                                        if (!((SubServer) server).isEditable()) sender.sendMessage(" -> Editable: " + ChatColor.RED + "no");
                                        sender.sendMessage(" -> Host: " + ChatColor.WHITE  + ((SubServer) server).getHost());
                                        if (((SubServer) server).getTemplate() != null) sender.sendMessage(" -> Template: " + ChatColor.WHITE  + ((SubServer) server).getHost());
                                    }
                                    if (server.getGroups().size() > 0) sender.sendMessage(" -> Group" + ((server.getGroups().size() > 1)?"s:":": " + ChatColor.WHITE + server.getGroups().get(0)));
                                    if (server.getGroups().size() > 1) for (String group : server.getGroups()) sender.sendMessage("      - " + ChatColor.WHITE + group);
                                    sender.sendMessage(" -> Address: " + ChatColor.WHITE + server.getAddress().getAddress().getHostAddress()+':'+server.getAddress().getPort());
                                    if (server instanceof SubServer) sender.sendMessage(" -> Running: " + ((((SubServer) server).isRunning())?ChatColor.GREEN+"yes":ChatColor.RED+"no"));
                                    if (!(server instanceof SubServer) || ((SubServer) server).isRunning()) {
                                        sender.sendMessage(" -> Connected: " + ((server.getSubData()[0] != null)?ChatColor.GREEN+"yes"+((server.getSubData().length > 1)?ChatColor.AQUA+" +"+(server.getSubData().length-1)+" subchannel"+((server.getSubData().length == 2)?"":"s"):""):ChatColor.RED+"no"));
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
                                    if (host.getSubData().length > 0) sender.sendMessage(" -> Connected: " + ((host.getSubData()[0] != null)?ChatColor.GREEN+"yes"+((host.getSubData().length > 1)?ChatColor.AQUA+" +"+(host.getSubData().length-1)+" subchannel"+((host.getSubData().length == 2)?"":"s"):""):ChatColor.RED+"no"));
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
                                    sender.sendMessage(" -> Connected: " + ((proxy.getSubData()[0] != null)?ChatColor.GREEN+"yes"+((proxy.getSubData().length > 1)?ChatColor.AQUA+" +"+(proxy.getSubData().length-1)+" subchannel"+((proxy.getSubData().length == 2)?"":"s"):""):ChatColor.RED+"no"));
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
                            ((SubDataClient) SubAPI.getInstance().getSubDataNetwork()[0]).sendPacket(new PacketStartServer(null, args[1], data -> {
                                switch (data.getInt(0x0001)) {
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
                                        sender.sendMessage("SubServers > That SubServer is not available");
                                        break;
                                    case 8:
                                        sender.sendMessage("SubServers > That SubServer is not enabled");
                                        break;
                                    case 9:
                                        sender.sendMessage("SubServers > That SubServer is already running");
                                        break;
                                    case 10:
                                        sender.sendMessage("SubServers > That SubServer cannot start while these server(s) are running: " + data.getRawString(0x0002));
                                        break;
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
                                    ((SubDataClient) SubAPI.getInstance().getSubDataNetwork()[0]).sendPacket(new PacketStartServer(null, args[1], data -> {
                                        switch (data.getInt(0x0001)) {
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
                                                sender.sendMessage("SubServers > Could not restart server: That SubServer is no longer available");
                                                break;
                                            case 8:
                                                sender.sendMessage("SubServers > Could not restart server: That SubServer is no longer enabled");
                                                break;
                                            case 10:
                                                sender.sendMessage("SubServers > Could not restart server: That SubServer cannot start while these server(s) are running: " + data.getRawString(0x0002));
                                                break;
                                            case 9:
                                            case 0:
                                            case 1:
                                                sender.sendMessage("SubServers > Server was started successfully");
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
                                        if (listening.get()) if (!json.getString("server").equalsIgnoreCase(args[1])) {
                                            PacketInExRunEvent.callback("SubStoppedEvent", this);
                                        } else {
                                            new Timer("SubServers.Sync::Server_Restart_Command_Handler(" + args[1] + ')').schedule(starter, 100);
                                        }
                                    } catch (Exception e) {}
                                }
                            });

                            ((SubDataClient) SubAPI.getInstance().getSubDataNetwork()[0]).sendPacket(new PacketStopServer(null, args[1], false, data -> {
                                if (data.getInt(0x0001) != 0) listening.set(false);
                                switch (data.getInt(0x0001)) {
                                    case 3:
                                        sender.sendMessage("SubServers > There is no server with that name");
                                        break;
                                    case 4:
                                        sender.sendMessage("SubServers > That Server is not a SubServer");
                                        break;
                                    case 5:
                                        starter.run();
                                        break;
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
                            ((SubDataClient) SubAPI.getInstance().getSubDataNetwork()[0]).sendPacket(new PacketStopServer(null, args[1], false, data -> {
                                switch (data.getInt(0x0001)) {
                                    case 3:
                                        sender.sendMessage("SubServers > There is no server with that name");
                                        break;
                                    case 4:
                                        sender.sendMessage("SubServers > That Server is not a SubServer");
                                        break;
                                    case 5:
                                        sender.sendMessage("SubServers > That SubServer is not running");
                                        break;
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
                            ((SubDataClient) SubAPI.getInstance().getSubDataNetwork()[0]).sendPacket(new PacketStopServer(null, args[1], true, data -> {
                                switch (data.getInt(0x0001)) {
                                    case 3:
                                        sender.sendMessage("SubServers > There is no server with that name");
                                        break;
                                    case 4:
                                        sender.sendMessage("SubServers > That Server is not a SubServer");
                                        break;
                                    case 5:
                                        sender.sendMessage("SubServers > That SubServer is not running");
                                        break;
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
                            ((SubDataClient) SubAPI.getInstance().getSubDataNetwork()[0]).sendPacket(new PacketCommandServer(null, args[1], cmd, data -> {
                                switch (data.getInt(0x0001)) {
                                    case 3:
                                        sender.sendMessage("SubServers > There is no server with that name");
                                        break;
                                    case 4:
                                        sender.sendMessage("SubServers > That Server is not a SubServer");
                                        break;
                                    case 5:
                                        sender.sendMessage("SubServers > That SubServer is not running");
                                        break;
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
                        if (args.length > 3) {
                            if (args.length > 5 && Util.isException(() -> Integer.parseInt(args[5]))) {
                                sender.sendMessage("Invalid Port Number");
                            } else {
                                ((SubDataClient) SubAPI.getInstance().getSubDataNetwork()[0]).sendPacket(new PacketCreateServer(null, args[1], args[2],args[3], (args.length > 4)?new Version(args[4]):null, (args.length > 5)?Integer.parseInt(args[5]):null, data -> {
                                    switch (data.getInt(0x0001)) {
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
                                            sender.sendMessage("SubServers > That Template requires a Minecraft Version to be specified");
                                            break;
                                        case 11:
                                            sender.sendMessage("SubServers > Invalid Port Number");
                                            break;
                                        case 0:
                                        case 1:
                                            sender.sendMessage("SubServers > Launching SubCreator...");
                                            break;
                                    }
                                }));
                            }
                        }
                    } else if (args[0].equalsIgnoreCase("update") || args[0].equalsIgnoreCase("upgrade")) {
                        if (args.length > 1) {
                            ((SubDataClient) SubAPI.getInstance().getSubDataNetwork()[0]).sendPacket(new PacketUpdateServer(null, args[1], (args.length > 2)?new Version(args[2]):null, data -> {
                                switch (data.getInt(0x0001)) {
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
                                        sender.sendMessage("SubServers > That SubServer is not available");
                                        break;
                                    case 8:
                                        sender.sendMessage("SubServers > Cannot update servers while they are still running");
                                        break;
                                    case 9:
                                        sender.sendMessage("SubServers > We don't know which template created that SubServer");
                                        break;
                                    case 10:
                                        sender.sendMessage("SubServers > That SubServer's Template is not enabled");
                                        break;
                                    case 11:
                                        sender.sendMessage("SubServers > That SubServer's Template does not support server updating");
                                        break;
                                    case 12:
                                        sender.sendMessage("SubServers > That SubServer's Template requires a Minecraft Version to be specified");
                                        break;
                                    case 0:
                                    case 1:
                                        sender.sendMessage("SubServers > Launching SubCreator...");
                                        break;
                                }
                            }));
                        } else {
                            sender.sendMessage("Usage: " + label + " " + args[0].toLowerCase() + " <SubServer> [Version]");
                        }
                    } else {
                        sender.sendMessage("SubServers > Unknown sub-command: " + args[0]);
                    }
                } else {
                    sender.sendMessages(printHelp());
                }
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
                "   Create Server: /sub create <Name> <Host> <Template> [Version] [Port]",
                "   Update Server: /sub update <SubServer> [Version]",
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

        if (plugin.api.getSubDataNetwork()[0] == null) {
            if (sender instanceof ConsoleCommandSender)
                new IllegalStateException("SubData is not connected").printStackTrace();
            return new NamedContainer<>(null, Collections.emptyList());
        } else if (sender instanceof ProxiedPlayer && (!players.keySet().contains(((ProxiedPlayer) sender).getUniqueId()) || !players.get(((ProxiedPlayer) sender).getUniqueId()).keySet().contains(((ProxiedPlayer) sender).getServer().getInfo())
                || !players.get(((ProxiedPlayer) sender).getUniqueId()).get(((ProxiedPlayer) sender).getServer().getInfo()).get())) {
            if (players.keySet().contains(((ProxiedPlayer) sender).getUniqueId()) && players.get(((ProxiedPlayer) sender).getUniqueId()).keySet().contains(((ProxiedPlayer) sender).getServer().getInfo())
                    && players.get(((ProxiedPlayer) sender).getUniqueId()).get(((ProxiedPlayer) sender).getServer().getInfo()).name() == null) {
                // do nothing
            } else if (!players.keySet().contains(((ProxiedPlayer) sender).getUniqueId()) || !players.get(((ProxiedPlayer) sender).getUniqueId()).keySet().contains(((ProxiedPlayer) sender).getServer().getInfo())
                    || Calendar.getInstance().getTime().getTime() - players.get(((ProxiedPlayer) sender).getUniqueId()).get(((ProxiedPlayer) sender).getServer().getInfo()).name() >= TimeUnit.MINUTES.toMillis(1)) {
                if (!(((ProxiedPlayer) sender).getServer().getInfo() instanceof ServerImpl) || ((ServerImpl) ((ProxiedPlayer) sender).getServer().getInfo()).getSubData()[0] == null) {
                    HashMap<ServerInfo, NamedContainer<Long, Boolean>> map = (players.keySet().contains(((ProxiedPlayer) sender).getUniqueId()))?players.get(((ProxiedPlayer) sender).getUniqueId()):new HashMap<ServerInfo, NamedContainer<Long, Boolean>>();
                    map.put(((ProxiedPlayer) sender).getServer().getInfo(), new NamedContainer<>(Calendar.getInstance().getTime().getTime(), false));
                    players.put(((ProxiedPlayer) sender).getUniqueId(), map);
                } else {
                    HashMap<ServerInfo, NamedContainer<Long, Boolean>> map = (players.keySet().contains(((ProxiedPlayer) sender).getUniqueId()))?players.get(((ProxiedPlayer) sender).getUniqueId()):new HashMap<ServerInfo, NamedContainer<Long, Boolean>>();
                    map.put(((ProxiedPlayer) sender).getServer().getInfo(), new NamedContainer<>(null, false));
                    players.put(((ProxiedPlayer) sender).getUniqueId(), map);
                    ((SubDataClient) SubAPI.getInstance().getSubDataNetwork()[0]).sendPacket(new PacketCheckPermission(((ProxiedPlayer) sender).getServer().getInfo().getName(), ((ProxiedPlayer) sender).getUniqueId(), "subservers.command", result -> {
                        map.put(((ProxiedPlayer) sender).getServer().getInfo(), new NamedContainer<>(Calendar.getInstance().getTime().getTime(), result));
                    }));
                }
            }
            return new NamedContainer<>(null, Collections.emptyList());
        } else if (args.length <= 1) {
            List<String> cmds = Arrays.asList("help", "list", "info", "status", "version", "start", "restart", "stop", "kill", "terminate", "cmd", "command", "create", "update", "upgrade");
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
                        for (ServerImpl server : plugin.servers.values()) if (!list.contains(server.getName())) list.add(server.getName());
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
                        for (ServerImpl server : plugin.servers.values()) {
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
                                for (ServerImpl server : plugin.servers.values()) list.add(server.getName());
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
                                for (ServerImpl server : plugin.servers.values()) {
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
                        for (ServerImpl server : plugin.servers.values()) if (server instanceof SubServerImpl) list.add(server.getName());
                    } else {
                        for (ServerImpl server : plugin.servers.values()) {
                            if (server instanceof SubServerImpl && server.getName().toLowerCase().startsWith(last)) list.add(last + server.getName().substring(last.length()));
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
                        for (ServerImpl server : plugin.servers.values()) if (server instanceof SubServerImpl) list.add(server.getName());
                    } else {
                        if ("*".startsWith(last)) list.add("*");
                        for (ServerImpl server : plugin.servers.values()) {
                            if (server instanceof SubServerImpl && server.getName().toLowerCase().startsWith(last)) list.add(last + server.getName().substring(last.length()));
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
                        for (ServerImpl server : plugin.servers.values()) if (server instanceof SubServerImpl) list.add(server.getName());
                    } else {
                        for (ServerImpl server : plugin.servers.values()) {
                            if (server instanceof SubServerImpl && server.getName().toLowerCase().startsWith(last)) list.add(last + server.getName().substring(last.length()));
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
                    return new NamedContainer<>(null, Collections.singletonList("[Version]"));
                } else if (args.length == 6) {
                    if (last.length() > 0) {
                        if (Util.isException(() -> Integer.parseInt(last)) || Integer.parseInt(last) <= 0 || Integer.parseInt(last) > 65535) {
                            return new NamedContainer<>(plugin.api.getLang("SubServers", "Command.Creator.Invalid-Port"), Collections.emptyList());
                        }
                    }
                    return new NamedContainer<>(null, Collections.singletonList("[Port]"));
                } else {
                    return new NamedContainer<>(null, Collections.emptyList());
                }
            } else if (args[0].equals("update") || args[0].equals("upgrade")) {
                if (args.length == 2) {
                    List<String> list = new ArrayList<String>();
                    if (last.length() == 0) {
                        for (ServerImpl server : plugin.servers.values()) list.add(server.getName());
                    } else {
                        for (ServerImpl server : plugin.servers.values()) {
                            if (server.getName().toLowerCase().startsWith(last)) list.add(last + server.getName().substring(last.length()));
                        }
                    }
                    return new NamedContainer<>((list.size() <= 0)?plugin.api.getLang("SubServers", "Command.Generic.Unknown-SubServer").replace("$str$", args[0]):null, list);
                } else if (args.length == 3) {
                    return new NamedContainer<>(null, Collections.singletonList("[Version]"));
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
        private ExProxy plugin;
        private BungeeServer(ExProxy plugin, String command) {
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

        protected static NamedContainer<BungeeServer, CommandX> newInstance(ExProxy plugin, String command) {
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
                    Map<String, ServerImpl> servers = plugin.servers;
                    if (servers.keySet().contains(args[0].toLowerCase())) {
                        ((ProxiedPlayer) sender).connect(servers.get(args[0].toLowerCase()));
                    } else {
                        sender.sendMessage(plugin.api.getLang("SubServers", "Bungee.Server.Invalid"));
                    }
                } else {
                    int i = 0;
                    TextComponent serverm = new TextComponent(ChatColor.RESET.toString());
                    TextComponent div = new TextComponent(plugin.api.getLang("SubServers", "Bungee.Server.Divider"));
                    for (ServerImpl server : plugin.servers.values()) {
                        if (!server.isHidden() && server.canAccess(sender) && (!(server instanceof SubServerImpl) || ((SubServerImpl) server).isRunning())) {
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
                    for (ServerImpl server : plugin.servers.values()) {
                        if (!server.isHidden()) list.add(server.getName());
                    }
                    return new NamedContainer<>(null, new LinkedList<>(list));
                } else {
                    for (ServerImpl server : plugin.servers.values()) {
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
        private ExProxy plugin;
        protected BungeeList(ExProxy plugin, String command) {
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
            for (ServerImpl server : plugin.servers.values()) {
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
                if (!server.isHidden() && (!(server instanceof SubServerImpl) || ((SubServerImpl) server).isRunning())) {
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
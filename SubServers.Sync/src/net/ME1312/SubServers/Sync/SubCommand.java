package net.ME1312.SubServers.Sync;

import com.google.gson.Gson;
import net.ME1312.SubServers.Sync.Library.Compatibility.CommandX;
import net.ME1312.SubServers.Sync.Library.Config.YAMLSection;
import net.ME1312.SubServers.Sync.Library.NamedContainer;
import net.ME1312.SubServers.Sync.Library.Util;
import net.ME1312.SubServers.Sync.Library.Version.Version;
import net.ME1312.SubServers.Sync.Network.API.Host;
import net.ME1312.SubServers.Sync.Network.API.Proxy;
import net.ME1312.SubServers.Sync.Network.API.Server;
import net.ME1312.SubServers.Sync.Network.API.SubServer;
import net.ME1312.SubServers.Sync.Network.Packet.*;
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
    private NamedContainer<Long, TreeMap<String, List<String>>> templateCache = new NamedContainer<Long, TreeMap<String, List<String>>>(0L, new TreeMap<String, List<String>>());
    private SubPlugin plugin;
    private String label;

    protected static NamedContainer<SubCommand, CommandX> newInstance(SubPlugin plugin, String command) {
        NamedContainer<SubCommand, CommandX> cmd = new NamedContainer<>(new SubCommand(plugin, command), null);
        CommandX now = cmd.name();
        //if (plugin.api.getGameVersion()[plugin.api.getGameVersion().length - 1].compareTo(new Version("1.13")) >= 0) { // TODO Future Command Validator API?
        //    now = new net.ME1312.SubServers.Sync.Library.Compatibility.v1_13.CommandX(cmd.name());
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
        if (sender instanceof ConsoleCommandSender) {
            if (plugin.subdata == null) {
                throw new IllegalStateException("SubData is not connected");
            } else if (plugin.lang == null) {
                throw new IllegalStateException("There are no lang options available at this time");
            } else {
                if (args.length > 0) {
                    if (args[0].equalsIgnoreCase("help") || args[0].equalsIgnoreCase("?")) {
                        sender.sendMessages(printHelp());
                    } else if (args[0].equalsIgnoreCase("version") || args[0].equalsIgnoreCase("ver")) {
                        sender.sendMessage("SubServers > These are the platforms and versions that are running SubServers.Sync:");
                        sender.sendMessage("  " + System.getProperty("os.name") + ' ' + System.getProperty("os.version") + ',');
                        sender.sendMessage("  Java " + System.getProperty("java.version") + ',');
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
                        }).start();
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
                                        } else if (((SubServer) server).isTemporary()) {
                                            message += ChatColor.AQUA;
                                        } else if (((SubServer) server).isRunning()) {
                                            message += ChatColor.GREEN;
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
                            plugin.subdata.sendPacket(new PacketDownloadServerInfo(args[1].toLowerCase(), data -> {
                                switch (data.getRawString("type").toLowerCase()) {
                                    case "invalid":
                                        sender.sendMessage("SubServers > There is no server with that name");
                                        break;
                                    case "subserver":
                                        sender.sendMessage("SubServers > Info on " + data.getSection("server").getRawString("display") + ':');
                                        if (!data.getSection("server").getRawString("name").equals(data.getSection("server").getRawString("display"))) sender.sendMessage("  - Real Name: " + data.getSection("server").getRawString("name"));
                                        sender.sendMessage("  - Host: " + data.getSection("server").getRawString("host"));
                                        sender.sendMessage("  - Enabled: " + ((data.getSection("server").getBoolean("enabled"))?"yes":"no"));
                                        sender.sendMessage("  - Editable: " + ((data.getSection("server").getBoolean("editable"))?"yes":"no"));
                                        if (data.getSection("server").getList("group").size() > 0) {
                                            sender.sendMessage("  - Group:");
                                            for (int i = 0; i < data.getSection("server").getList("group").size(); i++)
                                                sender.sendMessage("    - " + data.getSection("server").getList("group").get(i).asRawString());
                                        }
                                        if (data.getSection("server").getBoolean("temp")) sender.sendMessage("  - Temporary: yes");
                                        sender.sendMessage("  - Running: " + ((data.getSection("server").getBoolean("running"))?"yes":"no"));
                                        sender.sendMessage("  - Logging: " + ((data.getSection("server").getBoolean("log"))?"yes":"no"));
                                        sender.sendMessage("  - Address: " + data.getSection("server").getRawString("address"));
                                        sender.sendMessage("  - Auto Restart: " + ((data.getSection("server").getBoolean("auto-restart"))?"yes":"no"));
                                        sender.sendMessage("  - Hidden: " + ((data.getSection("server").getBoolean("hidden"))?"yes":"no"));
                                        if (data.getSection("server").getList("incompatible-list").size() > 0) {
                                            List<String> current = new ArrayList<String>();
                                            for (int i = 0; i < data.getSection("server").getList("incompatible").size(); i++) current.add(data.getSection("server").getList("incompatible").get(i).asRawString().toLowerCase());
                                            sender.sendMessage("  - Incompatibilities:");
                                            for (int i = 0; i < data.getSection("server").getList("incompatible-list").size(); i++)
                                                sender.sendMessage("    - " + data.getSection("server").getList("incompatible-list").get(i).asRawString() + ((current.contains(data.getSection("server").getList("incompatible-list").get(i).asRawString().toLowerCase()))?"*":""));
                                        }
                                        sender.sendMessage("  - Signature: " + data.getSection("server").getRawString("signature"));
                                        break;
                                    default:
                                        sender.sendMessage("SubSevrers > That Server is not a SubServer");
                                }
                            }));
                        } else {
                            sender.sendMessage("SubServers > Usage: " + label + " " + args[1].toLowerCase() + " <SubServer>");
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
                                        sender.sendMessage("SubServers > That SubServer's Host is not enabled");
                                        break;
                                    case 6:
                                        sender.sendMessage("SubServers > That SubServer is not enabled");
                                        break;
                                    case 7:
                                        sender.sendMessage("SubServers > That SubServer is already running");
                                        break;
                                    case 8:
                                        sender.sendMessages("That SubServer cannot start while these server(s) are running:", data.getRawString("m").split(":\\s")[1]);
                                        break;
                                    case 0:
                                    case 1:
                                        sender.sendMessage("SubServers > Server was started successfully");
                                        break;
                                    default:
                                        System.out.println("PacketStartServer(null, " + args[1] + ") responded with: " + data.getRawString("m"));
                                        sender.sendMessage("SubServers > Server was started successfully");
                                        break;
                                }
                            }));
                        } else {
                            sender.sendMessage("Usage: " + label + " <SubServer>");
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
                                    case 0:
                                    case 1:
                                        sender.sendMessage("SubServers > Server was stopped successfully");
                                        break;
                                    default:
                                        System.out.println("PacketStopServer(null, " + args[1] + ", false) responded with: " + data.getRawString("m"));
                                        sender.sendMessage("SubServers > Server was stopped successfully");
                                        break;
                                }
                            }));
                        } else {
                            sender.sendMessage("Usage: " + label + " <SubServer>");
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
                                    case 0:
                                    case 1:
                                        sender.sendMessage("SubServers > Server was terminated successfully");
                                        break;
                                    default:
                                        System.out.println("PacketStopServer(null, " + args[1] + ", true) responded with: " + data.getRawString("m"));
                                        sender.sendMessage("SubServers > Server was terminated successfully");
                                        break;
                                }
                            }));
                        } else {
                            sender.sendMessage("Usage: " + label + " <SubServer>");
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
                                    case 0:
                                    case 1:
                                        sender.sendMessage("SubServers > Command was sent successfully");
                                        break;
                                    default:
                                        System.out.println("PacketCommandServer(null, " + args[1] + ", /" + cmd + ") responded with: " + data.getRawString("m"));
                                        sender.sendMessage("SubServers > Command was sent successfully");
                                        break;
                                }
                            }));
                        } else {
                            sender.sendMessage("Usage: " + label + " <SubServer> <Command> [Args...]");
                        }
                    } else if (args[0].equalsIgnoreCase("create")) {
                        if (args.length > 5) {
                            if (Util.isException(() -> Integer.parseInt(args[5]))) {
                                sender.sendMessage("Invalid Port Number");
                            } else {
                                plugin.subdata.sendPacket(new PacketCreateServer(null, args[1], args[2],args[3], new Version(args[4]), Integer.parseInt(args[5]), data -> {
                                    switch (data.getInt("r")) {
                                        case 3:
                                        case 4:
                                            sender.sendMessage("SubServers > There is already a SubServer with that name");
                                            break;
                                        case 5:
                                            sender.sendMessage("SubServers > There is no host with that name");
                                            break;
                                        case 6:
                                            sender.sendMessage("SubServers > There is no template with that name");
                                            break;
                                        case 7:
                                            sender.sendMessage("SubServers > SubCreator cannot create servers before Minecraft 1.8");
                                            break;
                                        case 8:
                                            sender.sendMessage("SubServers > Invalid Port Number");
                                            break;
                                        case 0:
                                        case 1:
                                            sender.sendMessage("SubServers > Launching SubCreator...");
                                            break;
                                        default:
                                            System.out.println("PacketCreateServer(null, " + args[1] + ", " + args[2] + ", " + args[3] + ", " + args[4] + ", " + args[5] + ") responded with: " + data.getRawString("m"));
                                            sender.sendMessage("SubServers > Launching SubCreator...");
                                            break;
                                    }
                                }));
                            }
                        } else {
                            sender.sendMessage("Usage: " + label + " <Name> <Host> <Template> <Version> <Port>");
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
                "   Server Info: /sub info <SubServer>",
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
                        for (net.ME1312.SubServers.Sync.Server.Server server : plugin.servers.values()) if (server instanceof net.ME1312.SubServers.Sync.Server.SubServer) list.add(server.getName());
                    } else {
                        for (net.ME1312.SubServers.Sync.Server.Server server : plugin.servers.values()) {
                            if (server instanceof net.ME1312.SubServers.Sync.Server.SubServer && server.getName().toLowerCase().startsWith(last))
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
                        for (net.ME1312.SubServers.Sync.Server.Server server : plugin.servers.values()) if (server instanceof net.ME1312.SubServers.Sync.Server.SubServer) list.add(server.getName());
                    } else {
                        for (net.ME1312.SubServers.Sync.Server.Server server : plugin.servers.values()) {
                            if (server instanceof net.ME1312.SubServers.Sync.Server.SubServer && server.getName().toLowerCase().startsWith(last)) list.add(last + server.getName().substring(last.length()));
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
                    updateTemplateCache();
                    List<String> list = new ArrayList<String>();
                    if (templateCache.name() <= 0) {
                        list.add("<Host>");
                    } else if (last.length() == 0) {
                        list.addAll(templateCache.get().keySet());
                    } else {
                        for (String host : templateCache.get().keySet()) {
                            if (host.toLowerCase().startsWith(last)) list.add(last + host.substring(last.length()));
                        }
                    }
                    return new NamedContainer<>((list.size() <= 0)?plugin.api.getLang("SubServers", "Command.Generic.Unknown-Host").replace("$str$", args[0]):null, list);
                } else if (args.length == 4) {
                    updateTemplateCache();
                    List<String> list = new ArrayList<String>();
                    if (templateCache.name() <= 0 || !templateCache.get().keySet().contains(args[2].toLowerCase())) {
                        list.add("<Template>");
                    } else if (last.length() == 0) {
                        list.addAll(templateCache.get().get(args[2].toLowerCase()));
                    } else {
                        for (String template : templateCache.get().get(args[2].toLowerCase())) {
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

    private void updateTemplateCache() {
        if (Calendar.getInstance().getTime().getTime() - templateCache.name() >= TimeUnit.MINUTES.toMillis(5)) {
            templateCache.rename(Calendar.getInstance().getTime().getTime());
            plugin.api.getHosts(hosts -> {
                TreeMap<String, List<String>> cache = new TreeMap<String, List<String>>();
                for (Host host : hosts.values()) {
                    List<String> templates = new ArrayList<String>();
                    templates.addAll(host.getCreator().getTemplates().keySet());
                    cache.put(host.getName().toLowerCase(), templates);
                }
                templateCache.set(cache);
                templateCache.rename(Calendar.getInstance().getTime().getTime());
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
            //    now = new net.ME1312.SubServers.Sync.Library.Compatibility.v1_13.CommandX(cmd.name());
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
                    Map<String, net.ME1312.SubServers.Sync.Server.Server> servers = plugin.servers;
                    if (servers.keySet().contains(args[0].toLowerCase())) {
                        ((ProxiedPlayer) sender).connect(servers.get(args[0].toLowerCase()));
                    } else {
                        sender.sendMessage(plugin.api.getLang("SubServers", "Bungee.Server.Invalid"));
                    }
                } else {
                    int i = 0;
                    TextComponent serverm = new TextComponent(ChatColor.RESET.toString());
                    TextComponent div = new TextComponent(plugin.api.getLang("SubServers", "Bungee.Server.Divider"));
                    for (net.ME1312.SubServers.Sync.Server.Server server : plugin.servers.values()) {
                        if (!server.isHidden() && (!(server instanceof net.ME1312.SubServers.Sync.Server.SubServer) || ((net.ME1312.SubServers.Sync.Server.SubServer) server).isRunning())) {
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
            for (net.ME1312.SubServers.Sync.Server.Server server : plugin.servers.values()) {
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
                if (!server.isHidden() && (!(server instanceof net.ME1312.SubServers.Sync.Server.SubServer) || ((net.ME1312.SubServers.Sync.Server.SubServer) server).isRunning())) {
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
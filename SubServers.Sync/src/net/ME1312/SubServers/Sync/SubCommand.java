package net.ME1312.SubServers.Sync;

import net.ME1312.SubServers.Sync.Library.Compatibility.CommandX;
import net.ME1312.SubServers.Sync.Library.NamedContainer;
import net.ME1312.SubServers.Sync.Library.Util;
import net.ME1312.SubServers.Sync.Library.Version.Version;
import net.ME1312.SubServers.Sync.Network.Packet.*;
import net.ME1312.SubServers.Sync.Server.Server;
import net.ME1312.SubServers.Sync.Server.SubServer;
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
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
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
        if (plugin.api.getGameVersion().compareTo(new Version("1.13")) >= 0) {
            now = new net.ME1312.SubServers.Sync.Library.Compatibility.v1_13.CommandX(cmd.name());
        }
        cmd.set(now);
        return cmd;
    }

    private SubCommand(SubPlugin plugin, String command) {
        super(command);
        this.plugin = plugin;
        this.label = '/' + command;
    }

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
                        sender.sendMessage("SubServers > SubServers.Sync is running version " + plugin.version.toExtendedString());
                        new Thread(() -> {
                            try {
                                Document updxml = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new StringReader(Util.readAll(new BufferedReader(new InputStreamReader(new URL("https://src.me1312.net/maven/net/ME1312/SubServers/SubServers.Sync/maven-metadata.xml").openStream(), Charset.forName("UTF-8")))))));

                                NodeList updnodeList = updxml.getElementsByTagName("version");
                                Version updversion = plugin.version;
                                int updcount = -1;
                                for (int i = 0; i < updnodeList.getLength(); i++) {
                                    Node node = updnodeList.item(i);
                                    if (node.getNodeType() == Node.ELEMENT_NODE) {
                                        if (!node.getTextContent().startsWith("-") && new Version(node.getTextContent()).compareTo(updversion) >= 0) {
                                            updversion = new Version(node.getTextContent());
                                            updcount++;
                                        }
                                    }
                                }
                                if (updversion.equals(plugin.version)) {
                                    sender.sendMessage("You are on the latest version.");
                                } else {
                                    sender.sendMessage("You are " + updcount + " version" + ((updcount == 1) ? "" : "s") + " behind.");
                                }
                            } catch (Exception e) {
                            }
                        }).start();
                    } else if (args[0].equalsIgnoreCase("list")) {
                        plugin.subdata.sendPacket(new PacketDownloadServerList(null, null, json -> {
                            int i = 0;
                            boolean sent = false;
                            String div = ChatColor.RESET + ", ";
                            if (json.getJSONObject("groups").length() > 0) {
                                sender.sendMessage("SubServers > Group/Server List:");
                                for (String group : json.getJSONObject("groups").keySet()) {
                                    String message = "";
                                    message += ChatColor.GOLD + group + ChatColor.RESET + ": ";
                                    for (String server : json.getJSONObject("groups").getJSONObject(group).keySet()) {
                                        if (i != 0) message += div;
                                        if (!json.getJSONObject("groups").getJSONObject(group).getJSONObject(server).keySet().contains("enabled")) {
                                            message += ChatColor.WHITE;
                                        } else if (json.getJSONObject("groups").getJSONObject(group).getJSONObject(server).getBoolean("temp")) {
                                            message += ChatColor.AQUA;
                                        } else if (json.getJSONObject("groups").getJSONObject(group).getJSONObject(server).getBoolean("running")) {
                                            message += ChatColor.GREEN;
                                        } else if (json.getJSONObject("groups").getJSONObject(group).getJSONObject(server).getBoolean("enabled") && json.getJSONObject("groups").getJSONObject(group).getJSONObject(server).getJSONArray("incompatible").length() == 0) {
                                            message += ChatColor.YELLOW;
                                        } else {
                                            message += ChatColor.RED;
                                        }
                                        message += json.getJSONObject("groups").getJSONObject(group).getJSONObject(server).getString("display") + " (" + json.getJSONObject("groups").getJSONObject(group).getJSONObject(server).getString("address") + ((server.equals(json.getJSONObject("groups").getJSONObject(group).getJSONObject(server).getString("display"))) ? "" : ChatColor.stripColor(div) + server) + ")";
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
                            for (String host : json.getJSONObject("hosts").keySet()) {
                                String message = "";
                                if (json.getJSONObject("hosts").getJSONObject(host).getBoolean("enabled")) {
                                    message += ChatColor.AQUA;
                                } else {
                                    message += ChatColor.RED;
                                }
                                message += json.getJSONObject("hosts").getJSONObject(host).getString("display") + " (" + json.getJSONObject("hosts").getJSONObject(host).getString("address") + ((host.equals(json.getJSONObject("hosts").getJSONObject(host).getString("display"))) ? "" : ChatColor.stripColor(div) + host) + ")" + ChatColor.RESET + ": ";
                                for (String subserver : json.getJSONObject("hosts").getJSONObject(host).getJSONObject("servers").keySet()) {
                                    if (i != 0) message += div;
                                    if (json.getJSONObject("hosts").getJSONObject(host).getJSONObject("servers").getJSONObject(subserver).getBoolean("temp")) {
                                        message += ChatColor.AQUA;
                                    } else if (json.getJSONObject("hosts").getJSONObject(host).getJSONObject("servers").getJSONObject(subserver).getBoolean("running")) {
                                        message += ChatColor.GREEN;
                                    } else if (json.getJSONObject("hosts").getJSONObject(host).getJSONObject("servers").getJSONObject(subserver).getBoolean("enabled") && json.getJSONObject("hosts").getJSONObject(host).getJSONObject("servers").getJSONObject(subserver).getJSONArray("incompatible").length() == 0) {
                                        message += ChatColor.YELLOW;
                                    } else {
                                        message += ChatColor.RED;
                                    }
                                    message += json.getJSONObject("hosts").getJSONObject(host).getJSONObject("servers").getJSONObject(subserver).getString("display") + " (" + json.getJSONObject("hosts").getJSONObject(host).getJSONObject("servers").getJSONObject(subserver).getString("address").split(":")[json.getJSONObject("hosts").getJSONObject(host).getJSONObject("servers").getJSONObject(subserver).getString("address").split(":").length - 1] + ((subserver.equals(json.getJSONObject("hosts").getJSONObject(host).getJSONObject("servers").getJSONObject(subserver).getString("display"))) ? "" : ChatColor.stripColor(div) + subserver) + ")";
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
                            for (String server : json.getJSONObject("servers").keySet()) {
                                if (i != 0) message += div;
                                message += ChatColor.WHITE + json.getJSONObject("servers").getJSONObject(server).getString("display") + " (" + json.getJSONObject("servers").getJSONObject(server).getString("address") + ((server.equals(json.getJSONObject("servers").getJSONObject(server).getString("display"))) ? "" : ChatColor.stripColor(div) + server) + ")";
                                i++;
                            }
                            if (i == 0) message += ChatColor.RESET + "(none)";
                            sender.sendMessage(message);
                        }));
                    } else if (args[0].equalsIgnoreCase("info") || args[0].equalsIgnoreCase("status")) {
                        if (args.length > 1) {
                            plugin.subdata.sendPacket(new PacketDownloadServerInfo(args[1].toLowerCase(), json -> {
                                switch (json.getString("type").toLowerCase()) {
                                    case "invalid":
                                        sender.sendMessage("SubServers > There is no server with that name");
                                        break;
                                    case "subserver":
                                        sender.sendMessage("SubServers > Info on " + json.getJSONObject("server").getString("display") + ':');
                                        if (!json.getJSONObject("server").getString("name").equals(json.getJSONObject("server").getString("display"))) sender.sendMessage("  - Real Name: " + json.getJSONObject("server").getString("name"));
                                        sender.sendMessage("  - Host: " + json.getJSONObject("server").getString("host"));
                                        sender.sendMessage("  - Enabled: " + ((json.getJSONObject("server").getBoolean("enabled"))?"yes":"no"));
                                        sender.sendMessage("  - Editable: " + ((json.getJSONObject("server").getBoolean("editable"))?"yes":"no"));
                                        if (json.getJSONObject("server").getJSONArray("group").length() > 0) {
                                            sender.sendMessage("  - Group:");
                                            for (int i = 0; i < json.getJSONObject("server").getJSONArray("group").length(); i++)
                                                sender.sendMessage("    - " + json.getJSONObject("server").getJSONArray("group").getString(i));
                                        }
                                        if (json.getJSONObject("server").getBoolean("temp")) sender.sendMessage("  - Temporary: yes");
                                        sender.sendMessage("  - Running: " + ((json.getJSONObject("server").getBoolean("running"))?"yes":"no"));
                                        sender.sendMessage("  - Logging: " + ((json.getJSONObject("server").getBoolean("log"))?"yes":"no"));
                                        sender.sendMessage("  - Address: " + json.getJSONObject("server").getString("address"));
                                        sender.sendMessage("  - Auto Restart: " + ((json.getJSONObject("server").getBoolean("auto-restart"))?"yes":"no"));
                                        sender.sendMessage("  - Hidden: " + ((json.getJSONObject("server").getBoolean("hidden"))?"yes":"no"));
                                        if (json.getJSONObject("server").getJSONArray("incompatible-list").length() > 0) {
                                            List<String> current = new ArrayList<String>();
                                            for (int i = 0; i < json.getJSONObject("server").getJSONArray("incompatible").length(); i++) current.add(json.getJSONObject("server").getJSONArray("incompatible").getString(i).toLowerCase());
                                            sender.sendMessage("  - Incompatibilities:");
                                            for (int i = 0; i < json.getJSONObject("server").getJSONArray("incompatible-list").length(); i++)
                                                sender.sendMessage("    - " + json.getJSONObject("server").getJSONArray("incompatible-list").getString(i) + ((current.contains(json.getJSONObject("server").getJSONArray("incompatible-list").getString(i).toLowerCase()))?"*":""));
                                        }
                                        sender.sendMessage("  - Signature: " + json.getJSONObject("server").getString("signature"));
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
                            plugin.subdata.sendPacket(new PacketStartServer(null, args[1], json -> {
                                switch (json.getInt("r")) {
                                    case 3:
                                        sender.sendMessage("SubServers > There is no server with that name");
                                        break;
                                    case 4:
                                        sender.sendMessage("SubServers > That Server is not a SubServer");
                                        break;
                                    case 5:
                                        if (json.getString("m").contains("Host")) {
                                            sender.sendMessage("SubServers > That SubServer's Host is not enabled");
                                        } else {
                                            sender.sendMessage("SubServers > That SubServer is not enabled");
                                        }
                                        break;
                                    case 6:
                                        sender.sendMessage("SubServers > That SubServer is already running");
                                        break;
                                    case 0:
                                    case 1:
                                        sender.sendMessage("SubServers > Server was started successfully");
                                        break;
                                    default:
                                        System.out.println("PacketStartServer(null, " + args[1] + ") responded with: " + json.getString("m"));
                                        sender.sendMessage("SubServers > Server was started successfully");
                                        break;
                                }
                            }));
                        } else {
                            sender.sendMessage("Usage: " + label + " <SubServer>");
                        }
                    } else if (args[0].equalsIgnoreCase("stop")) {
                        if (args.length > 1) {
                            plugin.subdata.sendPacket(new PacketStopServer(null, args[1], false, json -> {
                                switch (json.getInt("r")) {
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
                                        System.out.println("PacketStopServer(null, " + args[1] + ", false) responded with: " + json.getString("m"));
                                        sender.sendMessage("SubServers > Server was stopped successfully");
                                        break;
                                }
                            }));
                        } else {
                            sender.sendMessage("Usage: " + label + " <SubServer>");
                        }
                    } else if (args[0].equalsIgnoreCase("kill") || args[0].equalsIgnoreCase("terminate")) {
                        if (args.length > 1) {
                            plugin.subdata.sendPacket(new PacketStopServer(null, args[1], true, json -> {
                                switch (json.getInt("r")) {
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
                                        System.out.println("PacketStopServer(null, " + args[1] + ", true) responded with: " + json.getString("m"));
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
                            plugin.subdata.sendPacket(new PacketCommandServer(null, args[1], cmd, json -> {
                                switch (json.getInt("r")) {
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
                                        System.out.println("PacketCommandServer(null, " + args[1] + ", /" + cmd + ") responded with: " + json.getString("m"));
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
                                plugin.subdata.sendPacket(new PacketCreateServer(null, args[1], args[2],args[3], new Version(args[4]), Integer.parseInt(args[5]), json -> {
                                    switch (json.getInt("r")) {
                                        case 3:
                                            sender.sendMessage("SubServers > There is already a SubServer with that name");
                                            break;
                                        case 4:
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
                                            System.out.println("PacketCreateServer(null, " + args[1] + ", " + args[2] + ", " + args[3] + ", " + args[4] + ", " + args[5] + ") responded with: " + json.getString("m"));
                                            sender.sendMessage("SubServers > Launching SubCreator...");
                                            break;
                                    }
                                }));
                            }
                        } else {
                            sender.sendMessage("Usage: " + label + " <Name> <Host> <Type> <Version> <Port> [RAM]");
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
                        for (Server server : plugin.api.getServers().values()) if (server instanceof SubServer) list.add(server.getName());
                    } else {
                        for (Server server : plugin.api.getServers().values()) {
                            if (server instanceof SubServer && server.getName().toLowerCase().startsWith(last))
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
                        for (Server server : plugin.api.getServers().values()) if (server instanceof SubServer) list.add(server.getName());
                    } else {
                        for (Server server : plugin.api.getServers().values()) {
                            if (server instanceof SubServer && server.getName().toLowerCase().startsWith(last)) list.add(last + server.getName().substring(last.length()));
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
            plugin.subdata.sendPacket(new PacketDownloadServerList(null, null, (json) -> {
                TreeMap<String, List<String>> hosts = new TreeMap<String, List<String>>();
                for (String host : json.getJSONObject("hosts").keySet()) {
                    List<String> templates = new ArrayList<String>();
                    templates.addAll(json.getJSONObject("hosts").getJSONObject(host).getJSONObject("creator").getJSONObject("templates").keySet());
                    hosts.put(host, templates);
                }
                templateCache.set(hosts);
                templateCache.rename(Calendar.getInstance().getTime().getTime());
            }));
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
            if (plugin.api.getGameVersion().compareTo(new Version("1.13")) >= 0) {
                now = new net.ME1312.SubServers.Sync.Library.Compatibility.v1_13.CommandX(cmd.name());
            }
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
            for (Server server : plugin.api.getServers().values()) {
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
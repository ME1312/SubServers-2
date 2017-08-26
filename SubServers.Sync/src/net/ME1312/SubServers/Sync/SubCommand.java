package net.ME1312.SubServers.Sync;

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
import java.net.URL;
import java.nio.charset.Charset;
import java.util.*;

@SuppressWarnings("deprecation")
public final class SubCommand extends Command {
    private SubPlugin plugin;
    private String label;

    public SubCommand(SubPlugin plugin, String label) {
        super(label);
        this.plugin = plugin;
        this.label = '/' + label;
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
                        sender.sendMessage("SubServers > SubServers.Sync is running version " + plugin.version.toString() + ((plugin.bversion != null)?" BETA "+plugin.bversion.toString():""));
                        if (plugin.bversion == null) {
                            new Thread(() -> {
                                try {
                                    Document updxml = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new StringReader(Util.readAll(new BufferedReader(new InputStreamReader(new URL("http://src.me1312.net/maven/net/ME1312/SubServers/SubServers.Sync/maven-metadata.xml").openStream(), Charset.forName("UTF-8")))))));

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
                        }
                    } else if (args[0].equalsIgnoreCase("list")) {
                        plugin.subdata.sendPacket(new PacketDownloadServerList(null, json -> {
                            int i = 0;
                            boolean sent = false;
                            String div = ChatColor.RESET + ", ";
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
                                    message += json.getJSONObject("groups").getJSONObject(group).getJSONObject(server).getString("display") + " (" + json.getJSONObject("groups").getJSONObject(group).getJSONObject(server).getString("address") + ((server.equals(json.getJSONObject("groups").getJSONObject(group).getJSONObject(server).getString("display")))?"":ChatColor.stripColor(div)+server) + ")";
                                    i++;
                                }
                                if (i == 0) message += ChatColor.RESET + "(none)";
                                sender.sendMessage(message);
                                i = 0;
                                sent = true;
                            }
                            if (!sent) sender.sendMessage(ChatColor.RESET + "(none)");
                            sent = false;
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
                        sender.sendMessage(plugin.lang.getSection("Lang").getColoredString("Bungee.Server.Invalid", '&'));
                    }
                } else {
                    int i = 0;
                    TextComponent serverm = new TextComponent(ChatColor.RESET.toString());
                    TextComponent div = new TextComponent(plugin.lang.getSection("Lang").getColoredString("Bungee.Server.Divider", '&'));
                    for (Server server : plugin.api.getServers().values()) {
                        if (!server.isHidden() && (!(server instanceof SubServer) || ((SubServer) server).isRunning())) {
                            if (i != 0) serverm.addExtra(div);
                            TextComponent message = new TextComponent(plugin.lang.getSection("Lang").getColoredString("Bungee.Server.List", '&').replace("$str$", server.getDisplayName()));
                            message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponent[]{new TextComponent(plugin.lang.getSection("Lang").getColoredString("Bungee.Server.Hover", '&').replace("$int$", Integer.toString(server.getPlayers().size())))}));
                            message.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/server " + server.getName()));
                            serverm.addExtra(message);
                            i++;
                        }
                    }
                    sender.sendMessages(
                            plugin.lang.getSection("Lang").getColoredString("Bungee.Server.Current", '&').replace("$str$", ((ProxiedPlayer) sender).getServer().getInfo().getName()),
                            plugin.lang.getSection("Lang").getColoredString("Bungee.Server.Available", '&'));
                    sender.sendMessage(serverm);
                }
            } else {
                sender.sendMessage(plugin.lang.getSection("Lang").getColoredString("Command.Generic.Player-Only", '&'));
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
                    String message = plugin.lang.getSection("Lang").getColoredString("Bungee.List.Format", '&').replace("$str$", server.getDisplayName()).replace("$int$", Integer.toString(server.getPlayers().size()));
                    for (ProxiedPlayer player : server.getPlayers()) {
                        if (i != 0) message += plugin.lang.getSection("Lang").getColoredString("Bungee.List.Divider", '&');
                        message += plugin.lang.getSection("Lang").getColoredString("Bungee.List.List", '&').replace("$str$", player.getName());
                        i++;
                    }
                    messages.add(message);
                }
            }
            sender.sendMessages(messages.toArray(new String[messages.size()]));
            sender.sendMessage(plugin.lang.getSection("Lang").getColoredString("Bungee.List.Total", '&').replace("$int$", Integer.toString(players)));
        }
    }
}
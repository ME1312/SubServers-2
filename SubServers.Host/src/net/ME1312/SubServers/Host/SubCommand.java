package net.ME1312.SubServers.Host;

import net.ME1312.SubServers.Host.API.Command;
import net.ME1312.SubServers.Host.API.SubPluginInfo;
import net.ME1312.SubServers.Host.Library.Config.YAMLSection;
import net.ME1312.SubServers.Host.Library.TextColor;
import net.ME1312.SubServers.Host.Library.Util;
import net.ME1312.SubServers.Host.Library.Version.Version;
import net.ME1312.SubServers.Host.Library.Version.VersionType;
import net.ME1312.SubServers.Host.Network.Packet.*;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.*;

/**
 * Command Class
 */
public class SubCommand {
    private SubCommand() {}
    protected static void load(ExHost host) {
        new Command(null) {
            @Override
            public void command(String handle, String[] args) {
                if (args.length == 0 || host.api.plugins.get(args[0].toLowerCase()) != null) {
                    host.log.message.println(
                            "These are the platforms and versions that are running " + ((args.length == 0)?"SubServers.Host":host.api.plugins.get(args[0].toLowerCase()).getName()) +":",
                            "  " + System.getProperty("os.name") + ' ' + System.getProperty("os.version") + ',',
                            "  Java " + System.getProperty("java.version") + ',',
                            "  SubServers.Host v" + host.version.toExtendedString() + ((host.api.getAppBuild() != null)?" (" + host.api.getAppBuild() + ')':""));
                    if (args.length == 0) {
                        host.log.message.println("");
                        new Thread(() -> {
                            try {
                                YAMLSection tags = new YAMLSection(new JSONObject("{\"tags\":" + Util.readAll(new BufferedReader(new InputStreamReader(new URL("https://api.github.com/repos/ME1312/SubServers-2/git/refs/tags").openStream(), Charset.forName("UTF-8")))) + '}'));
                                List<Version> versions = new LinkedList<Version>();

                                Version updversion = host.version;
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
                                    host.log.message.println("You are on the latest version.");
                                } else {
                                    host.log.message.println("SubServers.Host v" + updversion + " is available. You are " + updcount + " version" + ((updcount == 1)?"":"s") + " behind.");
                                }
                            } catch (Exception e) {}
                        }).start();
                    } else {
                        SubPluginInfo plugin = host.api.plugins.get(args[0].toLowerCase());
                        String title = "  " + plugin.getName() + " v" + plugin.getVersion().toExtendedString();
                        String subtitle = "    by ";
                        int i = 0;
                        for (String author : plugin.getAuthors()) {
                            i++;
                            if (i > 1) {
                                if (plugin.getAuthors().size() > 2) subtitle += ", ";
                                else if (plugin.getAuthors().size() == 2) subtitle += ' ';
                                if (i == plugin.getAuthors().size()) subtitle += "and ";
                            }
                            subtitle += author;
                        }
                        if (plugin.getWebsite() != null) {
                            if (title.length() > subtitle.length() + 5 + plugin.getWebsite().toString().length()) {
                                i = subtitle.length();
                                while (i < title.length() - plugin.getWebsite().toString().length() - 2) {
                                    i++;
                                    subtitle += ' ';
                                }
                            } else {
                                subtitle += " - ";
                            }
                            subtitle += plugin.getWebsite().toString();
                        }
                        host.log.message.println(title, subtitle);
                        if (plugin.getDescription() != null) host.log.message.println("", plugin.getDescription());
                    }
                } else {
                    host.log.message.println("There is no plugin with that name");
                }
            }
        }.usage("[plugin]").description("Gets the version of the System and SubServers or the specified Plugin").help(
                "This command will print what OS you're running, your OS version,",
                "your Java version, and the SubServers.Host version.",
                "",
                "If the [plugin] option is provided, it will print information about the specified plugin instead.",
                "",
                "Examples:",
                "  /version",
                "  /version ExamplePlugin"
        ).register("ver", "version");
        new Command(null) {
            @Override
            public void command(String handle, String[] args) {
                host.subdata.sendPacket(new PacketDownloadServerList(null, null, data -> {
                    int i = 0;
                    boolean sent = false;
                    String div = TextColor.RESET + ", ";
                    if (data.getSection("groups").getKeys().size() > 0) {
                        host.log.message.println("Group/Server List:");
                        for (String group : data.getSection("groups").getKeys()) {
                            String message = "  ";
                            message += TextColor.GOLD + group + TextColor.RESET + ": ";
                            for (String server : data.getSection("groups").getSection(group).getKeys()) {
                                if (i != 0) message += div;
                                if (!data.getSection("groups").getSection(group).getSection(server).contains("enabled")) {
                                    message += TextColor.WHITE;
                                } else if (data.getSection("groups").getSection(group).getSection(server).getBoolean("temp")) {
                                    message += TextColor.AQUA;
                                } else if (data.getSection("groups").getSection(group).getSection(server).getBoolean("running")) {
                                    message += TextColor.GREEN;
                                } else if (data.getSection("groups").getSection(group).getSection(server).getBoolean("enabled") && data.getSection("groups").getSection(group).getSection(server).getList("incompatible").size() == 0) {
                                    message += TextColor.YELLOW;
                                } else {
                                    message += TextColor.RED;
                                }
                                message += data.getSection("groups").getSection(group).getSection(server).getRawString("display") + " (" + data.getSection("groups").getSection(group).getSection(server).getRawString("address") + ((server.equals(data.getSection("groups").getSection(group).getSection(server).getRawString("display"))) ? "" : TextColor.stripColor(div) + server) + ")";
                                i++;
                            }
                            if (i == 0) message += TextColor.RESET + "(none)";
                            host.log.message.println(message);
                            i = 0;
                            sent = true;
                        }
                        if (!sent) host.log.message.println(TextColor.RESET + "(none)");
                        sent = false;
                    }
                    ExHost h = host;
                    host.log.message.println("Host/SubServer List:");
                    for (String host : data.getSection("hosts").getKeys()) {
                        String message = "  ";
                        if (data.getSection("hosts").getSection(host).getBoolean("enabled")) {
                            message += TextColor.AQUA;
                        } else {
                            message += TextColor.RED;
                        }
                        message += data.getSection("hosts").getSection(host).getRawString("display") + " (" + data.getSection("hosts").getSection(host).getRawString("address") + ((host.equals(data.getSection("hosts").getSection(host).getRawString("display")))?"":TextColor.stripColor(div)+host) + ")" + TextColor.RESET + ": ";
                        for (String subserver : data.getSection("hosts").getSection(host).getSection("servers").getKeys()) {
                            if (i != 0) message += div;
                            if (data.getSection("hosts").getSection(host).getSection("servers").getSection(subserver).getBoolean("temp")) {
                                message += TextColor.AQUA;
                            } else if (data.getSection("hosts").getSection(host).getSection("servers").getSection(subserver).getBoolean("running")) {
                                message += TextColor.GREEN;
                            } else if (data.getSection("hosts").getSection(host).getSection("servers").getSection(subserver).getBoolean("enabled") && data.getSection("hosts").getSection(host).getSection("servers").getSection(subserver).getList("incompatible").size() == 0) {
                                message += TextColor.YELLOW;
                            } else {
                                message += TextColor.RED;
                            }
                            message += data.getSection("hosts").getSection(host).getSection("servers").getSection(subserver).getRawString("display") + " (" + data.getSection("hosts").getSection(host).getSection("servers").getSection(subserver).getRawString("address").split(":")[data.getSection("hosts").getSection(host).getSection("servers").getSection(subserver).getRawString("address").split(":").length - 1] + ((subserver.equals(data.getSection("hosts").getSection(host).getSection("servers").getSection(subserver).getRawString("display")))?"":TextColor.stripColor(div)+subserver) + ")";
                            i++;
                        }
                        if (i == 0) message += TextColor.RESET + "(none)";
                        h.log.message.println(message);
                        i = 0;
                        sent = true;
                    }
                    if (!sent) host.log.message.println(TextColor.RESET + "(none)");
                    host.log.message.println("Server List:");
                    String message = "  ";
                    for (String server : data.getSection("servers").getKeys()) {
                        if (i != 0) message += div;
                        message += TextColor.WHITE + data.getSection("servers").getSection(server).getRawString("display") + " (" + data.getSection("servers").getSection(server).getRawString("address") + ((server.equals(data.getSection("servers").getSection(server).getRawString("display")))?"":TextColor.stripColor(div)+server) + ")";
                        i++;
                    }
                    if (i == 0) message += TextColor.RESET + "(none)";
                    host.log.message.println(message);
                }));
            }
        }.description("Lists the available Hosts and Servers").help(
                "This command will print a list of the available Hosts and Servers.",
                "You can then use these names in commands where applicable.",
                "",
                "Example:",
                "  /list"
        ).register("list");
        new Command(null) {
            @Override
            public void command(String handle, String[] args) {
                if (args.length > 0) {
                    host.subdata.sendPacket(new PacketDownloadServerInfo(args[0].toLowerCase(), data -> {
                        switch (data.getRawString("type").toLowerCase()) {
                            case "invalid":
                                host.log.message.println("There is no server with that name");
                                break;
                            case "subserver":
                                host.log.message.println("Info on " + data.getSection("server").getRawString("display") + ':');
                                if (!data.getSection("server").getRawString("name").equals(data.getSection("server").getRawString("display"))) host.log.message.println("  - Real Name: " + data.getSection("server").getRawString("name"));
                                host.log.message.println("  - Host: " + data.getSection("server").getRawString("host"));
                                host.log.message.println("  - Enabled: " + ((data.getSection("server").getBoolean("enabled"))?"yes":"no"));
                                host.log.message.println("  - Editable: " + ((data.getSection("server").getBoolean("editable"))?"yes":"no"));
                                if (data.getSection("server").getList("group").size() > 0) {
                                    host.log.message.println("  - Group:");
                                    for (int i = 0; i < data.getSection("server").getList("group").size(); i++)
                                        host.log.message.println("    - " + data.getSection("server").getList("group").get(i).asRawString());
                                }
                                if (data.getSection("server").getBoolean("temp")) host.log.message.println("  - Temporary: yes");
                                host.log.message.println("  - Running: " + ((data.getSection("server").getBoolean("running"))?"yes":"no"));
                                host.log.message.println("  - Logging: " + ((data.getSection("server").getBoolean("log"))?"yes":"no"));
                                host.log.message.println("  - Address: " + data.getSection("server").getRawString("address"));
                                host.log.message.println("  - Auto Restart: " + ((data.getSection("server").getBoolean("auto-restart"))?"yes":"no"));
                                host.log.message.println("  - Hidden: " + ((data.getSection("server").getBoolean("hidden"))?"yes":"no"));
                                if (data.getSection("server").getList("incompatible-list").size() > 0) {
                                    List<String> current = new ArrayList<String>();
                                    for (int i = 0; i < data.getSection("server").getList("incompatible").size(); i++) current.add(data.getSection("server").getList("incompatible").get(i).asRawString().toLowerCase());
                                    host.log.message.println("  - Incompatibilities:");
                                    for (int i = 0; i < data.getSection("server").getList("incompatible-list").size(); i++)
                                        host.log.message.println("    - " + data.getSection("server").getList("incompatible-list").get(i).asRawString() + ((current.contains(data.getSection("server").getList("incompatible-list").get(i).asRawString().toLowerCase()))?"*":""));
                                }
                                host.log.message.println("  - Signature: " + data.getSection("server").getRawString("signature"));
                                break;
                            default:
                                host.log.message.println("That Server is not a SubServer");
                        }
                    }));
                } else {
                    host.log.message.println("Usage: /" + handle + " <SubServer>");
                }
            }
        }.usage("<SubServer>").description("Gets information about a SubServer").help(
                "This command will print a list of information about",
                "the specified SubServer.",
                "",
                "The <SubServer> argument is required, and should be the name of",
                "the SubServer you want to obtain information about.",
                "",
                "Example:",
                "  /info ExampleServer"
        ).register("info", "status");
        new Command(null) {
            @Override
            public void command(String handle, String[] args) {
                if (args.length > 0) {
                    host.subdata.sendPacket(new PacketStartServer(null, args[0], data -> {
                        switch (data.getInt("r")) {
                            case 3:
                                host.log.message.println("There is no server with that name");
                                break;
                            case 4:
                                host.log.message.println("That Server is not a SubServer");
                                break;
                            case 5:
                                if (data.getRawString("m").contains("Host")) {
                                    host.log.message.println("That SubServer's Host is not enabled");
                                } else {
                                    host.log.message.println("That SubServer is not enabled");
                                }
                                break;
                            case 6:
                                host.log.message.println("That SubServer is already running");
                                break;
                            case 7:
                                host.log.message.println("That SubServer cannot start while these server(s) are running:", data.getRawString("m").split(":\\s")[1]);
                                break;
                            case 0:
                            case 1:
                                host.log.message.println("Server was started successfully");
                                break;
                            default:
                                host.log.warn.println("PacketStartServer(null, " + args[0] + ") responded with: " + data.getRawString("m"));
                                host.log.message.println("Server was started successfully");
                                break;
                        }
                    }));
                } else {
                    host.log.message.println("Usage: /" + handle + " <SubServer>");
                }
            }
        }.usage("<SubServer>").description("Starts a SubServer").help(
                "This command is used to start a SubServer on the network.",
                "Once it has been started, you can control it via the other commands",
                "",
                "The <SubServer> argument is required, and should be the name of",
                "the SubServer you want to start.",
                "",
                "Example:",
                "  /start ExampleServer"
        ).register("start");
        new Command(null) {
            @Override
            public void command(String handle, String[] args) {
                if (args.length > 0) {
                    host.subdata.sendPacket(new PacketStopServer(null, args[0], false, data -> {
                        switch (data.getInt("r")) {
                            case 3:
                                host.log.message.println("There is no server with that name");
                                break;
                            case 4:
                                host.log.message.println("That Server is not a SubServer");
                                break;
                            case 5:
                                host.log.message.println("That SubServer is not running");
                                break;
                            case 0:
                            case 1:
                                host.log.message.println("Server was stopped successfully");
                                break;
                            default:
                                host.log.warn.println("PacketStopServer(null, " + args[0] + ", false) responded with: " + data.getRawString("m"));
                                host.log.message.println("Server was stopped successfully");
                                break;
                        }
                    }));
                } else {
                    host.log.message.println("Usage: /" + handle + " <SubServer>");
                }
            }
        }.usage("<SubServer>").description("Stops a SubServer").help(
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
        new Command(null) {
            @Override
            public void command(String handle, String[] args) {
                if (args.length > 0) {
                    host.subdata.sendPacket(new PacketStopServer(null, args[0], true, data -> {
                        switch (data.getInt("r")) {
                            case 3:
                                host.log.message.println("There is no server with that name");
                                break;
                            case 4:
                                host.log.message.println("That Server is not a SubServer");
                                break;
                            case 5:
                                host.log.message.println("That SubServer is not running");
                                break;
                            case 0:
                            case 1:
                                host.log.message.println("Server was terminated successfully");
                                break;
                            default:
                                host.log.warn.println("PacketStopServer(null, " + args[0] + ", true) responded with: " + data.getRawString("m"));
                                host.log.message.println("Server was terminated successfully");
                                break;
                        }
                    }));
                } else {
                    host.log.message.println("Usage: /" + handle + " <SubServer>");
                }
            }
        }.usage("<SubServer>").description("Terminates a SubServer").help(
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
        new Command(null) {
            @Override
            public void command(String handle, String[] args) {
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
                    host.subdata.sendPacket(new PacketCommandServer(null, args[0], cmd, data -> {
                        switch (data.getInt("r")) {
                            case 3:
                                host.log.message.println("There is no server with that name");
                                break;
                            case 4:
                                host.log.message.println("That Server is not a SubServer");
                                break;
                            case 5:
                                host.log.message.println("That SubServer is not running");
                                break;
                            case 0:
                            case 1:
                                host.log.message.println("Command was sent successfully");
                                break;
                            default:
                                host.log.warn.println("PacketCommandServer(null, " + args[0] + ", /" + cmd + ") responded with: " + data.getRawString("m"));
                                host.log.message.println("Command was sent successfully");
                                break;
                        }
                    }));
                } else {
                    host.log.message.println("Usage: /" + handle + " <SubServer> <Command> [Args...]");
                }
            }
        }.usage("<SubServer>", "<Command>", "[Args...]").description("Sends a Command to a SubServer").help(
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
        new Command(null) {
            @Override
            public void command(String handle, String[] args) {
                if (args.length > 4) {
                    if (Util.isException(() -> Integer.parseInt(args[4]))) {
                        host.log.message.println("Invalid Port Number");
                    } else {
                        host.subdata.sendPacket(new PacketCreateServer(null, args[0], args[1],args[2], new Version(args[3]), Integer.parseInt(args[4]), data -> {
                            switch (data.getInt("r")) {
                                case 3:
                                    host.log.message.println("There is already a SubServer with that name");
                                    break;
                                case 4:
                                    host.log.message.println("There is no host with that name");
                                    break;
                                case 6:
                                    host.log.message.println("There is no template with that name");
                                    break;
                                case 7:
                                    host.log.message.println("SubCreator cannot create servers before Minecraft 1.8");
                                    break;
                                case 8:
                                    host.log.message.println("Invalid Port Number");
                                    break;
                                case 0:
                                case 1:
                                    host.log.message.println("Launching SubCreator...");
                                    break;
                                default:
                                    host.log.warn.println("PacketCreateServer(null, " + args[0] + ", " + args[1] + ", " + args[2] + ", " + args[3] + ", " + args[4] + ") responded with: " + data.getRawString("m"));
                                    host.log.message.println("Launching SubCreator...");
                                    break;
                            }
                        }));
                    }
                } else {
                    host.log.message.println("Usage: /" + handle + " <Name> <Host> <Template> <Version> <Port>");
                }
            }
        }.usage("<Name>", "<Host>", "<Template>", "<Version>", "<Port>").description("Creates a SubServer").help(
                "This command is used to create and launch a SubServer on the specified host via the network.",
                "Templates are downloaded from SubServers.Bungee to ~/Templates.",
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
                "The <Version> argument is required, and should be a version",
                "string of the type of server that you want to create",
                "",
                "The <Port> argument is required, and should be the port number",
                "that you want the server to listen on after it has been created.",
                "",
                "Examples:",
                "  /create ExampleServer ExampleHost Spigot 1.11 25565"
        ).register("create");
        new Command(null) {
            public void command(String handle, String[] args) {
                HashMap<String, String> commands = new LinkedHashMap<String, String>();
                HashMap<Command, String> handles = new LinkedHashMap<Command, String>();

                int length = 0;
                for(String command : host.api.commands.keySet()) {
                    String formatted = "/ ";
                    Command cmd = host.api.commands.get(command);
                    String alias = (handles.keySet().contains(cmd))?handles.get(cmd):null;

                    if (alias != null) formatted = commands.get(alias);
                    if (cmd.usage().length == 0 || alias != null) {
                        formatted = formatted.replaceFirst("\\s", ((alias != null)?"|":"") + command + ' ');
                    } else {
                        String usage = "";
                        for (String str : cmd.usage()) usage += ((usage.length() == 0)?"":" ") + str;
                        formatted = formatted.replaceFirst("\\s", command + ' ' + usage + ' ');
                    }
                    if(formatted.length() > length) {
                        length = formatted.length();
                    }

                    if (alias == null) {
                        commands.put(command, formatted);
                        handles.put(cmd, command);
                    } else {
                        commands.put(alias, formatted);
                    }
                }

                if (args.length == 0) {
                    host.log.message.println("SubServers.Host Command List:");
                    for (String command : commands.keySet()) {
                        String formatted = commands.get(command);
                        Command cmd = host.api.commands.get(command);

                        while (formatted.length() < length) {
                            formatted += ' ';
                        }
                        formatted += ((cmd.description() == null || cmd.description().length() == 0)?"  ":"- "+cmd.description());

                        host.log.message.println(formatted);
                    }
                } else if (host.api.commands.keySet().contains((args[0].startsWith("/"))?args[0].toLowerCase().substring(1):args[0].toLowerCase())) {
                    Command cmd = host.api.commands.get((args[0].startsWith("/"))?args[0].toLowerCase().substring(1):args[0].toLowerCase());
                    String formatted = commands.get(Util.getBackwards(host.api.commands, cmd).get(0));
                    host.log.message.println(formatted.substring(0, formatted.length() - 1));
                    for (String line : cmd.help()) {
                        host.log.message.println("  " + line);
                    }
                } else {
                    host.log.message.println("There is no command with that name");
                }
            }
        }.usage("[command]").description("Prints a list of the commands and/or their descriptions").help(
                "This command will print a list of all currently registered commands and aliases,",
                "along with their usage and a short description.",
                "",
                "If the [command] option is provided, it will print that command, it's aliases,",
                "it's usage, and an extended description like the one you see here instead.",
                "",
                "Examples:",
                "  /help",
                "  /help end"
        ).register("help", "?");
        new Command(null) {
            @Override
            public void command(String handle, String[] args) {
                host.stop(0);
            }
        }.description("Stops this SubServers instance").help(
                "This command will shutdown this instance of SubServers.Host,",
                "SubServers running on this host, and any plugins currently running via SubAPI.",
                "",
                "Example:",
                "  /exit"
        ).register("exit", "end");
    }
}

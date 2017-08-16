package net.ME1312.SubServers.Host;

import net.ME1312.SubServers.Host.API.Command;
import net.ME1312.SubServers.Host.API.SubPluginInfo;
import net.ME1312.SubServers.Host.Executable.SubCreator;
import net.ME1312.SubServers.Host.Library.Util;
import net.ME1312.SubServers.Host.Library.Version.Version;
import net.ME1312.SubServers.Host.Network.Packet.*;

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
                if (args.length == 0) {
                    host.log.message.println(
                            System.getProperty("os.name") + ' ' + System.getProperty("os.version") + ',',
                            "Java " + System.getProperty("java.version") + ',',
                            "SubServers.Host v" + host.version.toString() + ((host.bversion == null) ? "" : " BETA " + host.bversion.toString()));
                } else if (host.api.plugins.get(args[0].toLowerCase()) != null) {
                    SubPluginInfo plugin = host.api.plugins.get(args[0].toLowerCase());
                    host.log.message.println(plugin.getName() + " v" + plugin.getVersion() + " by " + plugin.getAuthors().toString().substring(1, plugin.getAuthors().toString().length() - 1));
                    if (plugin.getWebsite() != null) host.log.message.println(plugin.getWebsite().toString());
                    if (plugin.getDescription() != null) host.log.message.println("", plugin.getDescription());
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
                host.subdata.sendPacket(new PacketDownloadServerList(null, json -> {
                    TreeSet<String> servers = new TreeSet<String>();
                    TreeSet<String> hosts = new TreeSet<String>();
                    for (String host : json.getJSONObject("hosts").keySet())  {
                        hosts.add(host);
                        for (String subserver : json.getJSONObject("hosts").getJSONObject(host).getJSONObject("servers").keySet()) {
                            servers.add(subserver);
                        }
                    }
                    for (String server : json.getJSONObject("servers").keySet()) {
                        servers.add(server);
                    }
                    host.log.message.println("Host List:", hosts.toString().substring(1, hosts.toString().length() - 1), "Server List:", servers.toString().substring(1, servers.toString().length() - 1));
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
                    host.subdata.sendPacket(new PacketDownloadServerInfo(args[0].toLowerCase(), json -> {
                        switch (json.getString("type").toLowerCase()) {
                            case "invalid":
                                host.log.message.println("There is no server with that name");
                                break;
                            case "subserver":
                                host.log.message.println("Info on " + json.getJSONObject("server").getString("display") + ':');
                                if (!json.getJSONObject("server").getString("name").equals(json.getJSONObject("server").getString("display"))) host.log.message.println("  - Real Name: " + json.getJSONObject("server").getString("name"));
                                host.log.message.println("  - Host: " + json.getJSONObject("server").getString("host"));
                                host.log.message.println("  - Enabled: " + ((json.getJSONObject("server").getBoolean("enabled"))?"yes":"no"));
                                if (json.getJSONObject("server").getBoolean("temp")) host.log.message.println("  - Temporary: yes");
                                host.log.message.println("  - Running: " + ((json.getJSONObject("server").getBoolean("running"))?"yes":"no"));
                                host.log.message.println("  - Logging: " + ((json.getJSONObject("server").getBoolean("log"))?"yes":"no"));
                                host.log.message.println("  - Auto Restart: " + ((json.getJSONObject("server").getBoolean("auto-restart"))?"yes":"no"));
                                host.log.message.println("  - Hidden: " + ((json.getJSONObject("server").getBoolean("hidden"))?"yes":"no"));
                                if (json.getJSONObject("server").getJSONArray("incompatible-list").length() > 0) {
                                    List<String> current = new ArrayList<String>();
                                    for (int i = 0; i < json.getJSONObject("server").getJSONArray("incompatible").length(); i++) current.add(json.getJSONObject("server").getJSONArray("incompatible").getString(i).toLowerCase());
                                    host.log.message.println("  - Incompatibilities:");
                                    for (int i = 0; i < json.getJSONObject("server").getJSONArray("incompatible-list").length(); i++)
                                        host.log.message.println("    - " + json.getJSONObject("server").getJSONArray("incompatible-list").getString(i) + ((current.contains(json.getJSONObject("server").getJSONArray("incompatible-list").getString(i).toLowerCase()))?"*":""));
                                }
                                break;
                            default:
                                host.log.message.println("That Server is not a SubServer");
                        }
                    }));
                } else {
                    host.log.message.println("Usage: " + handle + " <SubServer>");
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
                    host.subdata.sendPacket(new PacketStartServer(null, args[0], json -> {
                        switch (json.getInt("r")) {
                            case 3:
                                host.log.message.println("There is no server with that name");
                                break;
                            case 4:
                                host.log.message.println("That Server is not a SubServer");
                                break;
                            case 5:
                                if (json.getString("m").contains("Host")) {
                                    host.log.message.println("That SubServer's Host is not enabled");
                                } else {
                                    host.log.message.println("That SubServer is not enabled");
                                }
                                break;
                            case 6:
                                host.log.message.println("That SubServer is already running");
                                break;
                            case 7:
                                host.log.message.println("That SubServer cannot start while these server(s) are running:", json.getString("m").split(":\\s")[1]);
                                break;
                            case 0:
                            case 1:
                                host.log.message.println("Server was started successfully");
                                break;
                            default:
                                host.log.warn.println("PacketStartServer(null, " + args[0] + ") responded with: " + json.getString("m"));
                                host.log.message.println("Server was started successfully");
                                break;
                        }
                    }));
                } else {
                    host.log.message.println("Usage: " + handle + " <SubServer>");
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
                    host.subdata.sendPacket(new PacketStopServer(null, args[0], false, json -> {
                        switch (json.getInt("r")) {
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
                                host.log.warn.println("PacketStopServer(null, " + args[0] + ", false) responded with: " + json.getString("m"));
                                host.log.message.println("Server was stopped successfully");
                                break;
                        }
                    }));
                } else {
                    host.log.message.println("Usage: " + handle + " <SubServer>");
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
                    host.subdata.sendPacket(new PacketStopServer(null, args[0], true, json -> {
                        switch (json.getInt("r")) {
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
                                host.log.warn.println("PacketStopServer(null, " + args[0] + ", true) responded with: " + json.getString("m"));
                                host.log.message.println("Server was terminated successfully");
                                break;
                        }
                    }));
                } else {
                    host.log.message.println("Usage: " + handle + " <SubServer>");
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
                    host.subdata.sendPacket(new PacketCommandServer(null, args[0], cmd, json -> {
                        switch (json.getInt("r")) {
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
                                host.log.warn.println("PacketCommandServer(null, " + args[0] + ", /" + cmd + ") responded with: " + json.getString("m"));
                                host.log.message.println("Command was sent successfully");
                                break;
                        }
                    }));
                } else {
                    host.log.message.println("Usage: " + handle + " <SubServer> <Command> [Args...]");
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
                        host.subdata.sendPacket(new PacketCreateServer(null, args[0], args[1],args[2], new Version(args[3]), Integer.parseInt(args[4]), json -> {
                            switch (json.getInt("r")) {
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
                                    host.log.warn.println("PacketCreateServer(null, " + args[0] + ", " + args[1] + ", " + args[2] + ", " + args[3] + ", " + args[4] + ") responded with: " + json.getString("m"));
                                    host.log.message.println("Launching SubCreator...");
                                    break;
                            }
                        }));
                    }
                } else {
                    host.log.message.println("Usage: " + handle + " <Name> <Host> <Template> <Version> <Port>");
                }
            }
        }.usage("<Name>", "<Host>", "<Template>", "<Version>", "<Port>").description("Creates a SubServer").help(
                "This command is used to create and launch a SubServer on the specified host via the network.",
                "You may also create custom templates in ~/Templates.",
                "",
                "The <Name> argument is required, and should be the name of",
                "the SubServer you want to create.",
                "",
                "The <Host> argument is required, and should be the name of",
                "the host you want to the server to run on.",
                "",
                "The <Type> argument is required, and should be the name of",
                "the type of server you want to create.",
                "",
                "The <Version> argument is required, and should be a version",
                "string of the type of server that you want to create",
                "",
                "The <Port> argument is required, and should be the port number",
                "that you want the server to listen on after it has been created.",
                "",
                "If the [RAM] argument is provided, it will allocate in megabytes",
                "the amount of RAM that the server will use after it has been created",
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

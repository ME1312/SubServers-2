package net.ME1312.SubServers.Client.Sponge;

import com.google.gson.Gson;
import net.ME1312.SubServers.Client.Sponge.Graphic.UIRenderer;
import net.ME1312.SubServers.Client.Sponge.Library.Callback;
import net.ME1312.SubServers.Client.Sponge.Library.ChatColor;
import net.ME1312.SubServers.Client.Sponge.Library.Config.YAMLSection;
import net.ME1312.SubServers.Client.Sponge.Library.Container;
import net.ME1312.SubServers.Client.Sponge.Library.Util;
import net.ME1312.SubServers.Client.Sponge.Library.Version.Version;
import net.ME1312.SubServers.Client.Sponge.Network.API.*;
import net.ME1312.SubServers.Client.Sponge.Network.Packet.*;
import org.spongepowered.api.Platform;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

public final class SubCommand implements CommandExecutor {
    private SubPlugin plugin;

    public SubCommand(SubPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Generate CommandSpec for this command
     *
     * @return CommandSpec
     */
    public CommandSpec spec() {
        SubCommand root = new SubCommand(plugin);
        return CommandSpec.builder()
                .description(Text.of("The SubServers Command"))
                .executor(root)
                .arguments(GenericArguments.optional(GenericArguments.string(Text.of("subcommand"))), GenericArguments.optional(GenericArguments.remainingJoinedStrings(Text.of("extra"))))
                .child(CommandSpec.builder()
                        .description(Text.of("The SubServers Command - Help"))
                        .executor(new HELP())
                        .arguments(GenericArguments.optional(GenericArguments.remainingJoinedStrings(Text.of("extra"))))
                        .build(), "help", "?")
                .child(CommandSpec.builder()
                        .description(Text.of("The SubServers Command - Version"))
                        .executor(new VERSION())
                        .arguments(GenericArguments.optional(GenericArguments.remainingJoinedStrings(Text.of("extra"))))
                        .build(), "version", "ver")
                .child(CommandSpec.builder()
                        .description(Text.of("The SubServers Command - List"))
                        .executor(new LIST())
                        .arguments(GenericArguments.optional(GenericArguments.remainingJoinedStrings(Text.of("extra"))))
                        .build(), "list")
                .child(CommandSpec.builder()
                        .description(Text.of("The SubServers Command - Info"))
                        .executor(new INFO())
                        .arguments(GenericArguments.optional(GenericArguments.string(Text.of("Type"))), GenericArguments.optional(GenericArguments.string(Text.of("Name"))), GenericArguments.optional(GenericArguments.remainingJoinedStrings(Text.of("extra"))))
                        .build(), "info", "status")
                .child(CommandSpec.builder()
                        .description(Text.of("The SubServers Command - Start"))
                        .executor(new START())
                        .arguments(GenericArguments.optional(GenericArguments.string(Text.of("SubServer"))), GenericArguments.optional(GenericArguments.remainingJoinedStrings(Text.of("extra"))))
                        .build(), "start")
                .child(CommandSpec.builder()
                        .description(Text.of("The SubServers Command - Restart"))
                        .executor(new RESTART())
                        .arguments(GenericArguments.optional(GenericArguments.string(Text.of("SubServer"))), GenericArguments.optional(GenericArguments.remainingJoinedStrings(Text.of("extra"))))
                        .build(), "restart")
                .child(CommandSpec.builder()
                        .description(Text.of("The SubServers Command - Stop"))
                        .executor(new STOP())
                        .arguments(GenericArguments.optional(GenericArguments.string(Text.of("SubServer"))), GenericArguments.optional(GenericArguments.remainingJoinedStrings(Text.of("extra"))))
                        .build(), "stop")
                .child(CommandSpec.builder()
                        .description(Text.of("The SubServers Command - Terminate"))
                        .executor(new TERMINATE())
                        .arguments(GenericArguments.optional(GenericArguments.string(Text.of("SubServer"))), GenericArguments.optional(GenericArguments.remainingJoinedStrings(Text.of("extra"))))
                        .build(), "kill", "terminate")
                .child(CommandSpec.builder()
                        .description(Text.of("The SubServers Command - Command"))
                        .executor(new COMMAND())
                        .arguments(GenericArguments.optional(GenericArguments.string(Text.of("SubServer"))), GenericArguments.optional(GenericArguments.remainingJoinedStrings(Text.of("Command"))))
                        .build(), "command", "cmd")
                .child(CommandSpec.builder()
                        .description(Text.of("The SubServers Command - Create"))
                        .executor(new CREATE())
                        .arguments(GenericArguments.optional(GenericArguments.string(Text.of("Name"))), GenericArguments.optional(GenericArguments.string(Text.of("Host"))), GenericArguments.optional(GenericArguments.string(Text.of("Template"))), GenericArguments.optional(GenericArguments.string(Text.of("Version"))), GenericArguments.optional(GenericArguments.string(Text.of("Port"))), GenericArguments.optional(GenericArguments.remainingJoinedStrings(Text.of("extra"))))
                        .build(), "create")
                .child(CommandSpec.builder()
                        .description(Text.of("The SubServers Command - Open Menu"))
                        .executor(new OPEN())
                        .arguments(GenericArguments.optional(GenericArguments.string(Text.of("MenuID"))), GenericArguments.optional(GenericArguments.allOf(GenericArguments.string(Text.of("Args")))))
                        .build(), "open", "view")
                .build();
    }

    private boolean canRun(CommandSource sender) throws CommandException {
        if (plugin.subdata == null) {
            throw new CommandException(Text.builder("An exception has occurred while running this command").color(TextColors.RED).build(), new IllegalStateException("SubData is not connected"), false);
        } else if (plugin.lang == null) {
            throw new CommandException(Text.builder("An exception has occurred while running this command").color(TextColors.RED).build(), new IllegalStateException("There are no lang options available at this time"), false);
        } else {
            return sender.hasPermission("subservers.command");
        }
    }

    public CommandResult execute(CommandSource sender, CommandContext args) throws CommandException {
        if (canRun(sender)) {
            Optional<String> subcommand = args.getOne(Text.of("subcommand"));
            if (subcommand.isPresent()) {
                sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Generic.Invalid-Subcommand").replace("$str$", subcommand.get())));
                return CommandResult.builder().successCount(0).build();
            } else {
                if (sender.hasPermission("subservers.interface") && sender instanceof Player && plugin.gui != null) {
                    plugin.gui.getRenderer((Player) sender).newUI();
                } else {
                    sender.sendMessages(printHelp());
                }
                return CommandResult.builder().successCount(1).build();
            }
        } else {
            sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Generic.Invalid-Permission").replace("$str$", "subservers.command")));
            return CommandResult.builder().successCount(0).build();
        }
    }

    public final class HELP implements CommandExecutor {
        public CommandResult execute(CommandSource sender, CommandContext args) throws CommandException {
            if (canRun(sender)) {
                sender.sendMessages(printHelp());
                return CommandResult.builder().successCount(1).build();
            } else {
                sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Generic.Invalid-Permission").replace("$str$", "subservers.command")));
                return CommandResult.builder().successCount(0).build();
            }
        }
    }

    public final class VERSION implements CommandExecutor {
        @SuppressWarnings("unchecked")
        public CommandResult execute(CommandSource sender, CommandContext args) throws CommandException {
            if (canRun(sender)) {
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

                PluginContainer container = null;
                if (container == null) container = Util.getDespiteException(() -> (PluginContainer) Platform.class.getMethod("getContainer", Class.forName("org.spongepowered.api.Platform$Component")).invoke(Sponge.getPlatform(), Enum.valueOf((Class<Enum>) Class.forName("org.spongepowered.api.Platform$Component"), "IMPLEMENTATION")), null);
                if (container == null) container = Util.getDespiteException(() -> (PluginContainer) Platform.class.getMethod("getImplementation").invoke(Sponge.getPlatform()), null);

                sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Version").replace("$str$", "SubServers.Client.Sponge")));
                sender.sendMessage(Text.builder("  " + System.getProperty("os.name") + ((!System.getProperty("os.name").toLowerCase().startsWith("windows"))?' ' + System.getProperty("os.version"):"") + ((osarch != null)?" [" + osarch + ']':"")).color(TextColors.WHITE).append(Text.of(",")).build());
                sender.sendMessage(Text.builder("  Java " + System.getProperty("java.version") + ((javaarch != null)?" [" + javaarch + ']':"")).color(TextColors.WHITE).append(Text.of(",")).build());
                sender.sendMessage(Text.builder("  " + container.getName() + ' ' + container.getVersion().get()).color(TextColors.WHITE).append(Text.of(",")).build());
                sender.sendMessage(Text.builder("  SubServers.Client.Sponge v" + plugin.version.toExtendedString() + ((plugin.api.getPluginBuild() != null)?" (" + plugin.api.getPluginBuild() + ')':"")).color(TextColors.WHITE).build());
                sender.sendMessage(Text.EMPTY);
                plugin.game.getScheduler().createTaskBuilder().async().execute(() -> {
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
                            sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Version.Latest")));
                        } else {
                            sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Version.Outdated").replace("$name$", "SubServers.Client.Sponge").replace("$str$", updversion.toString()).replace("$int$", Integer.toString(updcount))));
                        }
                    } catch (Exception e) {}
                }).submit(plugin);
                return CommandResult.builder().successCount(1).build();
            } else {
                sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Generic.Invalid-Permission").replace("$str$", "subservers.command")));
                return CommandResult.builder().successCount(0).build();
            }
        }
    }

    public final class LIST implements CommandExecutor {
        public CommandResult execute(CommandSource sender, CommandContext args) throws CommandException {
            if (canRun(sender)) {
                plugin.api.getGroups(groups -> plugin.api.getHosts(hosts -> plugin.api.getServers(servers -> plugin.api.getMasterProxy(proxymaster -> plugin.api.getProxies(proxies -> {
                    int i = 0;
                    boolean sent = false;
                    Text div = ChatColor.convertColor(plugin.api.getLang("SubServers","Command.List.Divider"));
                    if (groups.keySet().size() > 0) {
                        sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.List.Group-Header")));
                        for (String group : groups.keySet()) {
                            Text.Builder msg = Text.builder(group).color(TextColors.GOLD).onHover(TextActions.showText(
                                    Text.builder(group + '\n').color(TextColors.GOLD).append(
                                            ChatColor.convertColor(plugin.api.getLang("SubServers","Interface.Group-Menu.Group-Server-Count").replace("$int$", new DecimalFormat("#,###").format(groups.get(group).size())))
                                    ).build())
                            ).onClick(TextActions.runCommand("/subservers open Server 1 " + group)).append(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.List.Header")));

                            for (Server server : groups.get(group)) {
                                Text.Builder message = Text.builder(server.getDisplayName());
                                Text.Builder hover = Text.builder(server.getDisplayName() + '\n');
                                if (server instanceof SubServer) {
                                    message.onClick(TextActions.runCommand("/subservers open SubServer/ " + server.getName()));
                                    if (((SubServer) server).isRunning()) {
                                        message.color(TextColors.GREEN);
                                        hover.color(TextColors.GREEN);
                                        if (!server.getName().equals(server.getDisplayName())) {
                                            hover.append(Text.builder(server.getName() + '\n').color(TextColors.GRAY).build());
                                        }
                                        if (((SubServer) server).getStopAction() == SubServer.StopAction.REMOVE_SERVER || ((SubServer) server).getStopAction() == SubServer.StopAction.DELETE_SERVER) {
                                            message.color(TextColors.AQUA);
                                            hover.color(TextColors.AQUA);
                                            hover.append(ChatColor.convertColor(plugin.api.getLang("SubServers","Interface.Server-Menu.SubServer-Temporary") + '\n'));
                                        }
                                        hover.append(
                                                ChatColor.convertColor(plugin.api.getLang("SubServers","Interface.Server-Menu.Server-Player-Count").replace("$int$", new DecimalFormat("#,###").format(server.getPlayers().size())))
                                        );
                                    } else if (((SubServer) server).isEnabled() && ((SubServer) server).getCurrentIncompatibilities().size() == 0) {
                                        message.color(TextColors.YELLOW);
                                        hover.color(TextColors.YELLOW);
                                        if (!server.getName().equals(server.getDisplayName())) {
                                            hover.append(Text.builder(server.getName() + '\n').color(TextColors.GRAY).build());
                                        }
                                        hover.append(
                                            ChatColor.convertColor(plugin.api.getLang("SubServers","Interface.Server-Menu.SubServer-Offline"))
                                        );
                                    } else {
                                        message.color(TextColors.RED);
                                        hover.color(TextColors.RED);
                                        if (!server.getName().equals(server.getDisplayName())) {
                                            hover.append(Text.builder(server.getName() + '\n').color(TextColors.GRAY).build());
                                        }
                                        if (((SubServer) server).getCurrentIncompatibilities().size() != 0) {
                                            String list = "";
                                            for (String other : ((SubServer) server).getCurrentIncompatibilities()) {
                                                if (list.length() != 0) list += ", ";
                                                list += other;
                                            }
                                            hover.append(ChatColor.convertColor(plugin.api.getLang("SubServers","Interface.Server-Menu.SubServer-Incompatible").replace("$str$", list) + ((((SubServer) server).isEnabled())?"":"\n")));
                                        }
                                        if (!((SubServer) server).isEnabled()) {
                                            hover.append(ChatColor.convertColor(plugin.api.getLang("SubServers","Interface.Server-Menu.SubServer-Disabled")));
                                        }
                                    }
                                    if (plugin.config.get().getSection("Settings").getBoolean("Show-Addresses", false)) {
                                        hover.append(Text.builder('\n' + server.getAddress().getAddress().getHostAddress()+':'+server.getAddress().getPort()).color(TextColors.WHITE).build());
                                    } else {
                                        hover.append(Text.builder("\n" + server.getAddress().getPort()).color(TextColors.WHITE).build());
                                    }
                                    message.onClick(TextActions.runCommand("/subservers open SubServer/ " + server.getName()));
                                } else {
                                    message.color(TextColors.WHITE);
                                    hover.color(TextColors.WHITE);
                                    if (!server.getName().equals(server.getDisplayName())) {
                                        hover.append(Text.builder(server.getName() + '\n').color(TextColors.GRAY).build());
                                    }
                                    hover.append(
                                            ChatColor.convertColor(plugin.api.getLang("SubServers","Interface.Server-Menu.Server-External") + '\n'),
                                            ChatColor.convertColor(plugin.api.getLang("SubServers","Interface.Server-Menu.Server-Player-Count").replace("$int$", new DecimalFormat("#,###").format(server.getPlayers().size())))
                                    );
                                    if (plugin.config.get().getSection("Settings").getBoolean("Show-Addresses", false)) {
                                        hover.append(Text.builder('\n' + server.getAddress().getAddress().getHostAddress()+':'+server.getAddress().getPort()).color(TextColors.WHITE).build());
                                    } else {
                                        hover.append(Text.builder("\n" + server.getAddress().getPort()).color(TextColors.WHITE).build());
                                    }
                                }
                                message.onHover(TextActions.showText(hover.build()));
                                if (i != 0) msg.append(div);
                                msg.append(message.build());
                                i++;
                            }
                            if (i == 0) msg.append(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.List.Empty")));
                            sender.sendMessages(Text.builder("  ").append(msg.build()).build());
                            i = 0;
                            sent = true;
                        }
                        if (!sent) sender.sendMessage(ChatColor.convertColor("  " + plugin.api.getLang("SubServers", "Command.List.Empty")));
                        sent = false;
                    }
                    sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.List.Host-Header")));
                    for (Host host : hosts.values()) {
                        Text.Builder msg = Text.builder(host.getDisplayName());
                        Text.Builder hover = Text.builder(host.getDisplayName() + '\n');
                        if (host.isAvailable() && host.isEnabled()) {
                            msg.color(TextColors.AQUA);
                            hover.color(TextColors.AQUA);
                            if (!host.getName().equals(host.getDisplayName())) {
                                hover.append(Text.builder(host.getName() + '\n').color(TextColors.GRAY).build());
                            }
                            hover.append(ChatColor.convertColor(plugin.api.getLang("SubServers","Interface.Host-Menu.Host-Server-Count").replace("$int$", new DecimalFormat("#,###").format(host.getSubServers().keySet().size()))));
                        } else {
                            msg.color(TextColors.RED);
                            hover.color(TextColors.RED);
                            if (!host.getName().equals(host.getDisplayName())) {
                                hover.append(Text.builder(host.getName() + '\n').color(TextColors.GRAY).build());
                            }
                            if (!host.isAvailable()) hover.append(ChatColor.convertColor(plugin.api.getLang("SubServers","Interface.Host-Menu.Host-Unavailable")));
                            else hover.append(ChatColor.convertColor(plugin.api.getLang("SubServers","Interface.Host-Menu.Host-Disabled")));
                        }
                        if (plugin.config.get().getSection("Settings").getBoolean("Show-Addresses", false)) {
                            hover.append(Text.builder('\n' + host.getAddress().getHostAddress()).color(TextColors.WHITE).build());
                        }
                        msg.onClick(TextActions.runCommand("/subservers open Host/ " + host.getName()));
                        msg.onHover(TextActions.showText(hover.build()));
                        msg.append(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.List.Header")));

                        for (SubServer subserver : host.getSubServers().values()) {
                            Text.Builder message = Text.builder(subserver.getDisplayName());
                            hover = Text.builder(subserver.getDisplayName() + '\n');
                            if (subserver.isRunning()) {
                                message.color(TextColors.GREEN);
                                hover.color(TextColors.GREEN);
                                if (!subserver.getName().equals(subserver.getDisplayName())) {
                                    hover.append(Text.builder(subserver.getName() + '\n').color(TextColors.GRAY).build());
                                }
                                if (subserver.getStopAction() == SubServer.StopAction.REMOVE_SERVER || subserver.getStopAction() == SubServer.StopAction.DELETE_SERVER) {
                                    message.color(TextColors.AQUA);
                                    hover.color(TextColors.AQUA);
                                    hover.append(ChatColor.convertColor(plugin.api.getLang("SubServers","Interface.Server-Menu.SubServer-Temporary") + '\n'));
                                }
                                hover.append(ChatColor.convertColor(plugin.api.getLang("SubServers","Interface.Server-Menu.Server-Player-Count").replace("$int$", new DecimalFormat("#,###").format(subserver.getPlayers().size()))));
                            } else if (subserver.isEnabled() && subserver.getCurrentIncompatibilities().size() == 0) {
                                message.color(TextColors.YELLOW);
                                hover.color(TextColors.YELLOW);
                                if (!subserver.getName().equals(subserver.getDisplayName())) {
                                    hover.append(Text.builder(subserver.getName() + '\n').color(TextColors.GRAY).build());
                                }
                                hover.append(ChatColor.convertColor(plugin.api.getLang("SubServers","Interface.Server-Menu.SubServer-Offline")));
                            } else {
                                message.color(TextColors.RED);
                                hover.color(TextColors.RED);
                                if (!subserver.getName().equals(subserver.getDisplayName())) {
                                    hover.append(Text.builder(subserver.getName() + '\n').color(TextColors.GRAY).build());
                                }
                                if (subserver.getCurrentIncompatibilities().size() != 0) {
                                    String list = "";
                                    for (String other : subserver.getCurrentIncompatibilities()) {
                                        if (list.length() != 0) list += ", ";
                                        list += other;
                                    }
                                    hover.append(ChatColor.convertColor(plugin.api.getLang("SubServers","Interface.Server-Menu.SubServer-Incompatible").replace("$str$", list) + ((subserver.isEnabled())?"":"\n")));
                                }
                                if (!subserver.isEnabled()) {
                                    hover.append(ChatColor.convertColor(plugin.api.getLang("SubServers","Interface.Server-Menu.SubServer-Disabled")));
                                }
                            }
                            if (plugin.config.get().getSection("Settings").getBoolean("Show-Addresses", false)) {
                                hover.append(Text.builder('\n' + subserver.getAddress().getAddress().getHostAddress()+':'+subserver.getAddress().getPort()).color(TextColors.WHITE).build());
                            } else {
                                hover.append(Text.builder("\n" + subserver.getAddress().getPort()).color(TextColors.WHITE).build());
                            }
                            message.onClick(TextActions.runCommand("/subservers open SubServer/ " + subserver.getName()));
                            message.onHover(TextActions.showText(hover.build()));
                            if (i != 0) msg.append(div);
                            msg.append(message.build());
                            i++;
                        }
                        if (i == 0) msg.append(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.List.Empty")));
                        sender.sendMessage(Text.builder("  ").append(msg.build()).build());
                        i = 0;
                        sent = true;
                    }
                    if (!sent) sender.sendMessage(ChatColor.convertColor("  " + plugin.api.getLang("SubServers", "Command.List.Empty")));
                    sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.List.Server-Header")));
                    Text.Builder msg = Text.builder();
                    for (Server server : servers.values()) if (!(server instanceof SubServer)) {
                        Text.Builder message = Text.builder(server.getDisplayName());
                        Text.Builder hover = Text.builder(server.getDisplayName() + '\n');
                        message.color(TextColors.WHITE);
                        hover.color(TextColors.WHITE);
                        if (!server.getName().equals(server.getDisplayName())) {
                            hover.append(Text.builder(server.getName() + '\n').color(TextColors.GRAY).build());
                        }
                        hover.append(
                                ChatColor.convertColor(plugin.api.getLang("SubServers","Interface.Server-Menu.Server-External") + '\n'),
                                ChatColor.convertColor(plugin.api.getLang("SubServers","Interface.Server-Menu.Server-Player-Count").replace("$int$", new DecimalFormat("#,###").format(server.getPlayers().size()))));
                        if (plugin.config.get().getSection("Settings").getBoolean("Show-Addresses", false)) {
                            hover.append(Text.builder('\n' + server.getAddress().getAddress().getHostAddress()+':'+server.getAddress().getPort()).color(TextColors.WHITE).build());
                        } else {
                            hover.append(Text.builder("\n" + server.getAddress().getPort()).color(TextColors.WHITE).build());
                        }
                        message.onHover(TextActions.showText(hover.build()));
                        if (i != 0) msg.append(div);
                        msg.append(message.build());
                        i++;
                    }
                    if (i == 0) sender.sendMessage(ChatColor.convertColor("  " + plugin.api.getLang("SubServers", "Command.List.Empty")));
                    else sender.sendMessage(Text.builder("  ").append(msg.build()).build());
                    if (proxies.keySet().size() > 0) {
                        sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.List.Proxy-Header")));
                        msg = Text.builder();
                        Text.Builder message = Text.builder("(master)");
                        Text.Builder hover = Text.builder("(master)");
                        message.color(TextColors.GRAY);
                        hover.color(TextColors.GRAY);
                        if (proxymaster != null) {
                            hover.append(
                                    Text.builder('\n' + proxymaster.getName()).color(TextColors.GRAY).build(),
                                    ChatColor.convertColor('\n' + plugin.api.getLang("SubServers", "Interface.Proxy-Menu.Proxy-Master")),
                                    ChatColor.convertColor('\n' + plugin.api.getLang("SubServers", "Interface.Proxy-Menu.Proxy-Player-Count").replace("$int$", new DecimalFormat("#,###").format(proxymaster.getPlayers().size())))
                            );
                        } else hover.append(ChatColor.convertColor('\n' + plugin.api.getLang("SubServers", "Interface.Proxy-Menu.Proxy-Master")));
                        message.onHover(TextActions.showText(hover.build()));
                        msg.append(message.build());
                        for (Proxy proxy : proxies.values()) {
                            message = Text.builder(proxy.getDisplayName());
                            hover = Text.builder(proxy.getDisplayName());
                            if (proxy.getSubData() != null && proxy.isRedis()) {
                                message.color(TextColors.GREEN);
                                hover.color(TextColors.GREEN);
                                if (!proxy.getName().equals(proxy.getDisplayName())) {
                                    hover.append(Text.builder('\n' + proxy.getName()).color(TextColors.GRAY).build());
                                }
                                hover.append(ChatColor.convertColor('\n' + plugin.api.getLang("SubServers", "Interface.Proxy-Menu.Proxy-Player-Count").replace("$int$", new DecimalFormat("#,###").format(proxy.getPlayers().size()))));
                            } else if (proxy.getSubData() != null) {
                                message.color(TextColors.AQUA);
                                hover.color(TextColors.AQUA);
                                if (!proxy.getName().equals(proxy.getDisplayName())) {
                                    hover.append(Text.builder('\n' + proxy.getName()).color(TextColors.GRAY).build());
                                }
                                if (proxymaster != null) {
                                    hover.append(ChatColor.convertColor('\n' + plugin.api.getLang("SubServers", "Interface.Proxy-Menu.Proxy-SubData")));
                                }
                            } else if (proxy.isRedis()) {
                                message.color(TextColors.WHITE);
                                hover.color(TextColors.WHITE);
                                if (!proxy.getName().equals(proxy.getDisplayName())) {
                                    hover.append(Text.builder('\n' + proxy.getName()).color(TextColors.GRAY).build());
                                }
                                hover.append(
                                        ChatColor.convertColor('\n' + plugin.api.getLang("SubServers", "Interface.Proxy-Menu.Proxy-Redis")),
                                        ChatColor.convertColor('\n' + plugin.api.getLang("SubServers", "Interface.Proxy-Menu.Proxy-Player-Count").replace("$int$", new DecimalFormat("#,###").format(proxy.getPlayers().size())))
                                );
                            } else {
                                message.color(TextColors.RED);
                                hover.color(TextColors.RED);
                                if (!proxy.getName().equals(proxy.getDisplayName())) {
                                    hover.append(Text.builder('\n' + proxy.getName()).color(TextColors.GRAY).build());
                                }
                                hover.append(ChatColor.convertColor('\n' + plugin.api.getLang("SubServers", "Interface.Proxy-Menu.Proxy-Disconnected")));
                            }
                            message.onHover(TextActions.showText(hover.build()));
                            msg.append(div, message.build());
                        }
                        sender.sendMessage(Text.builder("  ").append(msg.build()).build());
                    }
                })))));
                return CommandResult.builder().successCount(1).build();
            } else {
                sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Generic.Invalid-Permission").replace("$str$", "subservers.command")));
                return CommandResult.builder().successCount(0).build();
            }
        }
    }

    public final class INFO implements CommandExecutor {
        public CommandResult execute(CommandSource sender, CommandContext args) throws CommandException {
            if (canRun(sender)) {
                Optional<String> t = args.getOne(Text.of("Type"));
                Optional<String> n = args.getOne(Text.of("Name"));
                if (!n.isPresent()) {
                    Optional<String> tmp = n;
                    n = t;
                    t = tmp;
                }

                if (n.isPresent()) {
                    String type = (t.isPresent())?t.get():null;
                    String name = n.get();

                    Runnable getServer = () -> plugin.api.getServer(name, server -> {
                        if (server != null) {
                            sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Info").replace("$str$", ((server instanceof SubServer)?"Sub":"") + "Server")).toBuilder().append(Text.builder(server.getDisplayName()).color(TextColors.WHITE).build()).build());
                            if (!server.getName().equals(server.getDisplayName())) sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "System Name")).toBuilder().append(Text.builder(server.getName()).color(TextColors.WHITE).build()).build());
                            if (server instanceof SubServer) {
                                sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Enabled")).toBuilder().append(Text.builder((((SubServer) server).isEnabled())?"yes":"no").color((((SubServer) server).isEnabled())?TextColors.GREEN:TextColors.RED).build()).build());
                                if (!((SubServer) server).isEditable()) sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Editable")).toBuilder().append(Text.builder("no").color(TextColors.RED).build()).build());
                                sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Host")).toBuilder().append(Text.builder(((SubServer) server).getHost()).color(TextColors.WHITE ).build()).build());
                            }
                            if (server.getGroups().size() > 0) sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Group" + ((server.getGroups().size() > 1)?"s":""))).toBuilder().append(Text.builder((server.getGroups().size() > 1)?"":server.getGroups().get(0)).color(TextColors.WHITE).build()).build());
                            if (server.getGroups().size() > 1) for (String group : server.getGroups()) sender.sendMessage(ChatColor.convertColor("    " + plugin.api.getLang("SubServers", "Command.Info.List")).toBuilder().append(Text.builder(group).color(TextColors.WHITE).build()).build());
                            if (plugin.config.get().getSection("Settings").getBoolean("Show-Addresses", false)) sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Address")).toBuilder().append(Text.builder(server.getAddress().getAddress().getHostAddress()+':'+server.getAddress().getPort()).color(TextColors.WHITE).build()).build());
                            else sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Port")).toBuilder().append(Text.builder(Integer.toString(server.getAddress().getPort())).color(TextColors.AQUA).build()).build());
                            if (server instanceof SubServer) sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Running")).toBuilder().append(Text.builder((((SubServer) server).isRunning())?"yes":"no").color((((SubServer) server).isRunning())?TextColors.GREEN:TextColors.RED).build()).build());
                            if (!(server instanceof SubServer) || ((SubServer) server).isRunning()) {
                                sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Connected")).toBuilder().append(Text.builder((server.getSubData() != null)?"yes":"no").color((server.getSubData() != null)?TextColors.GREEN:TextColors.RED).build()).build());
                                sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Players")).toBuilder().append(Text.builder(server.getPlayers().size() + " online").color(TextColors.AQUA).build()).build());
                            }
                            sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "MOTD")).toBuilder().append(Text.builder(server.getMotd().replaceAll("\\u00A7[0-9a-fA-Fk-oK-ORr]", "")).color(TextColors.WHITE).build()).build());
                            if (server instanceof SubServer && ((SubServer) server).getStopAction() != SubServer.StopAction.NONE) sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Stop Action")).toBuilder().append(Text.builder(((SubServer) server).getStopAction().toString()).color(TextColors.WHITE).build()).build());
                            sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Signature")).toBuilder().append(Text.builder(server.getSignature()).color(TextColors.AQUA).build()).build());
                            if (server instanceof SubServer) sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Logging")).toBuilder().append(Text.builder((((SubServer) server).isLogging())?"yes":"no").color((((SubServer) server).isLogging())?TextColors.GREEN:TextColors.RED).build()).build());
                            sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Restricted")).toBuilder().append(Text.builder((server.isRestricted())?"yes":"no").color((server.isRestricted())?TextColors.GREEN:TextColors.RED).build()).build());
                            if (server instanceof SubServer && ((SubServer) server).getIncompatibilities().size() > 0) {
                                List<String> current = new ArrayList<String>();
                                for (String other : ((SubServer) server).getCurrentIncompatibilities()) current.add(other.toLowerCase());
                                sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Info.Format").replace("$str$", "Incompatibilities")));
                                for (String other : ((SubServer) server).getIncompatibilities()) sender.sendMessage(ChatColor.convertColor("    " + plugin.api.getLang("SubServers", "Command.Info.List")).toBuilder().append(Text.builder(other).color((current.contains(other.toLowerCase()))?TextColors.WHITE:TextColors.GRAY).build()).build());
                            }
                            sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Hidden")).toBuilder().append(Text.builder((server.isHidden())?"yes":"no").color((server.isHidden())?TextColors.GREEN:TextColors.RED).build()).build());
                        } else {
                            if (type == null) {
                                sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Info.Unknown")));
                            } else {
                                sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Info.Unknown-Server")));
                            }
                        }
                    });
                    Runnable getGroup = () -> plugin.api.getGroup(name, group -> {
                        if (group != null) {
                            sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Info").replace("$str$", "Group")).toBuilder().append(Text.builder(name).color(TextColors.WHITE).build()).build());
                            sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Servers")).toBuilder().append(Text.builder((group.size() <= 0)?"(none)":Integer.toString(group.size())).color((group.size() <= 0)?TextColors.GRAY:TextColors.AQUA).build()).build());
                            for (Server server : group) sender.sendMessage(ChatColor.convertColor("    " + plugin.api.getLang("SubServers", "Command.Info.List")).toBuilder().append(Text.builder(server.getDisplayName() + ((server.getName().equals(server.getDisplayName()))?"":" ("+server.getName()+')')).color(TextColors.WHITE).build()).build());
                        } else {
                            if (type == null) {
                                getServer.run();
                            } else {
                                sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Info.Unknown-Group")));
                            }
                        }
                    });
                    Runnable getHost = () -> plugin.api.getHost(name, host -> {
                        if (host != null) {
                            sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Info").replace("$str$", "Host")).toBuilder().append(Text.builder(host.getDisplayName()).color(TextColors.WHITE).build()).build());
                            if (!host.getName().equals(host.getDisplayName())) sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "System Name")).toBuilder().append(Text.builder(host.getName()).color(TextColors.WHITE).build()).build());
                            sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Available")).toBuilder().append(Text.builder((host.isAvailable())?"yes":"no").color((host.isAvailable())?TextColors.GREEN:TextColors.RED).build()).build());
                            sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Enabled")).toBuilder().append(Text.builder((host.isEnabled())?"yes":"no").color((host.isEnabled())?TextColors.GREEN:TextColors.RED).build()).build());
                            if (plugin.config.get().getSection("Settings").getBoolean("Show-Addresses", false)) sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Address")).toBuilder().append(Text.builder(host.getAddress().getHostAddress()).color(TextColors.WHITE).build()).build());
                            if (host.getSubData() != null) sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Connected")).toBuilder().append(Text.builder("yes").color(TextColors.GREEN).build()).build());
                            sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "SubServers")).toBuilder().append(Text.builder((host.getSubServers().keySet().size() <= 0)?"(none)":Integer.toString(host.getSubServers().keySet().size())).color((host.getSubServers().keySet().size() <= 0)?TextColors.GRAY:TextColors.AQUA).build()).build());
                            for (SubServer subserver : host.getSubServers().values()) sender.sendMessage(ChatColor.convertColor("    " + plugin.api.getLang("SubServers", "Command.Info.List")).toBuilder().append(Text.builder(subserver.getDisplayName() + ((subserver.getName().equals(subserver.getDisplayName()))?"":" ("+subserver.getName()+')')).color((subserver.isEnabled())?TextColors.WHITE:TextColors.GRAY).build()).build());
                            sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Templates")).toBuilder().append(Text.builder((host.getCreator().getTemplates().keySet().size() <= 0)?"(none)":Integer.toString(host.getCreator().getTemplates().keySet().size())).color((host.getCreator().getTemplates().keySet().size() <= 0)?TextColors.GRAY:TextColors.AQUA).build()).build());
                            for (SubCreator.ServerTemplate template : host.getCreator().getTemplates().values()) sender.sendMessage(ChatColor.convertColor("    " + plugin.api.getLang("SubServers", "Command.Info.List")).toBuilder().append(Text.builder(template.getDisplayName() + ((template.getName().equals(template.getDisplayName()))?"":" ("+template.getName()+')')).color((template.isEnabled())?TextColors.WHITE:TextColors.GRAY).build()).build());
                            sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Signature")).toBuilder().append(Text.builder(host.getSignature()).color(TextColors.AQUA).build()).build());
                        } else {
                            if (type == null) {
                                getGroup.run();
                            } else {
                                sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Info.Unknown-Host")));
                            }
                        }
                    });
                    Runnable getProxy = () -> plugin.api.getProxy(name, proxy -> {
                        if (proxy != null) {
                            sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Info").replace("$str$", "Proxy")).toBuilder().append(Text.builder(proxy.getDisplayName()).color(TextColors.WHITE).build()).build());
                            if (!proxy.getName().equals(proxy.getDisplayName())) sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "System Name")).toBuilder().append(Text.builder(proxy.getName()).color(TextColors.WHITE ).build()).build());
                            sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Connected")).toBuilder().append(Text.builder((proxy.getSubData() != null)?"yes":"no").color((proxy.getSubData() != null)?TextColors.GREEN:TextColors.RED).build()).build());
                            sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Redis") ).toBuilder().append(Text.builder(((proxy.isRedis())?"":"un") + "available").color((proxy.isRedis())?TextColors.GREEN:TextColors.RED).build()).build());
                            if (proxy.isRedis()) sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Players")).toBuilder().append(Text.builder(proxy.getPlayers().size() + " online").color(TextColors.AQUA).build()).build());
                            sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Signature")).toBuilder().append(Text.builder(proxy.getSignature()).color(TextColors.AQUA).build()).build());
                        } else {
                            if (type == null) {
                                getHost.run();
                            } else {
                                sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Info.Unknown-Proxy")));
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
                                sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Info.Unknown-Type")));
                        }
                    }
                    return CommandResult.builder().successCount(1).build();
                } else {
                    sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Generic.Usage").replace("$str$", "/sub info [proxy|host|group|server] <Name>")));
                    return CommandResult.builder().successCount(0).build();
                }
            } else {
                sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Generic.Invalid-Permission").replace("$str$", "subservers.command")));
                return CommandResult.builder().successCount(0).build();
            }
        }
    }

    public final class START implements CommandExecutor {
        public CommandResult execute(CommandSource sender, CommandContext args) throws CommandException {
            if (canRun(sender)) {
                Optional<String> subserver = args.getOne(Text.of("SubServer"));
                if (subserver.isPresent()) {
                    if (sender.hasPermission("subservers.subserver.start." + subserver.get().toLowerCase())) {
                        plugin.subdata.sendPacket(new PacketStartServer((sender instanceof Player)?((Player) sender).getUniqueId():null, subserver.get(), data -> {
                            switch (data.getInt("r")) {
                                case 3:
                                    sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Start.Unknown")));
                                    break;
                                case 4:
                                    sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Start.Invalid")));
                                    break;
                                case 5:
                                    sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Start.Host-Unavailable")));
                                    break;
                                case 6:
                                    sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Start.Host-Disabled")));
                                    break;
                                case 7:
                                    sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Start.Server-Disabled")));
                                    break;
                                case 8:
                                    sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Start.Running")));
                                    break;
                                case 9:
                                    sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Start.Server-Incompatible").replace("$str$", data.getString("m").split(":\\s")[1])));
                                    break;
                                default:
                                    plugin.logger.warn("PacketStartServer(" + ((sender instanceof Player)?((Player) sender).getUniqueId().toString():"null") + ", " + subserver.get() + ") responded with: " + data.getString("m"));
                                case 0:
                                case 1:
                                    sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Start")));
                                    break;
                            }
                        }));
                        return CommandResult.builder().successCount(1).build();
                    } else {
                        sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Generic.Invalid-Permission").replace("$str$", "subservers.subserver.start." + subserver.get().toLowerCase())));
                        return CommandResult.builder().successCount(0).build();
                    }
                } else {
                    sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Generic.Usage").replace("$str$", "/sub start <SubServer>")));
                    return CommandResult.builder().successCount(0).build();
                }
            } else {
                sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Generic.Invalid-Permission").replace("$str$", "subservers.command")));
                return CommandResult.builder().successCount(0).build();
            }
        }
    }

    public final class RESTART implements CommandExecutor {
        public CommandResult execute(CommandSource sender, CommandContext args) throws CommandException {
            if (canRun(sender)) {
                Optional<String> subserver = args.getOne(Text.of("SubServer"));
                if (subserver.isPresent()) {
                    if (sender.hasPermission("subservers.subserver.stop." + subserver.get().toLowerCase()) && sender.hasPermission("subservers.subserver.start." + subserver.get().toLowerCase())) {
                        Runnable starter = () -> plugin.subdata.sendPacket(new PacketStartServer(null, subserver.get(), data -> {
                            switch (data.getInt("r")) {
                                case 3:
                                case 4:
                                    sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Restart.Disappeared")));
                                    break;
                                case 5:
                                    sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Restart.Host-Unavailable")));
                                    break;
                                case 6:
                                    sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Restart.Host-Disabled")));
                                    break;
                                case 7:
                                    sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Restart.Server-Disabled")));
                                    break;
                                case 9:
                                    sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Start.Server-Incompatible").replace("$str$", data.getString("m").split(":\\s")[1])));
                                    break;
                                default:
                                    plugin.logger.warn("PacketStartServer(" + ((sender instanceof Player)?((Player) sender).getUniqueId().toString():"null") + ", " + subserver.get() + ") responded with: " + data.getString("m"));
                                case 8:
                                case 0:
                                case 1:
                                    sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Restart.Finish")));
                                    break;
                            }
                        }));

                        final Container<Boolean> listening = new Container<Boolean>(true);
                        PacketInRunEvent.callback("SubStoppedEvent", new Callback<YAMLSection>() {
                            @Override
                            public void run(YAMLSection json) {
                                try {
                                    if (listening.get()) if (!json.getString("server").equalsIgnoreCase(subserver.get())) {
                                        PacketInRunEvent.callback("SubStoppedEvent", this);
                                    } else {
                                        plugin.game.getScheduler().createTaskBuilder().execute(starter).delay(100, TimeUnit.MILLISECONDS).submit(plugin);
                                    }
                                } catch (Exception e) {}
                            }
                        });

                        Callback<YAMLSection> stopper = data -> {
                            if (data.getInt("r") != 0) listening.set(false);
                            switch (data.getInt("r")) {
                                case 3:
                                    sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Restart.Unknown")));
                                    break;
                                case 4:
                                    sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Restart.Invalid")));
                                    break;
                                case 5:
                                    starter.run();
                                    break;
                                default:
                                    plugin.logger.warn("PacketStopServer(" + ((sender instanceof Player)?((Player) sender).getUniqueId().toString():"null") + ", " + subserver.get() + ", false) responded with: " + data.getString("m"));
                                case 0:
                                case 1:
                                    sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Restart")));
                                    break;
                            }
                        };

                        if (plugin.subdata.getName().equalsIgnoreCase(subserver.get())) {
                            listening.set(false);
                            plugin.subdata.sendPacket(new PacketRestartServer((sender instanceof Player)?((Player) sender).getUniqueId():null, subserver.get(), stopper));
                        } else {
                            plugin.subdata.sendPacket(new PacketStopServer((sender instanceof Player)?((Player) sender).getUniqueId():null, subserver.get(), false, stopper));
                        }
                        return CommandResult.builder().successCount(1).build();
                    } else if (!sender.hasPermission("subservers.subserver.stop." + subserver.get().toLowerCase())) {
                        sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Generic.Invalid-Permission").replace("$str$", "subservers.subserver.stop." + subserver.get().toLowerCase())));
                        return CommandResult.builder().successCount(0).build();
                    } else {
                        sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Generic.Invalid-Permission").replace("$str$", "subservers.subserver.start." + subserver.get().toLowerCase())));
                        return CommandResult.builder().successCount(0).build();
                    }
                } else {
                    sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Generic.Usage").replace("$str$", "/sub restart <SubServer>")));
                    return CommandResult.builder().successCount(0).build();
                }
            } else {
                sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Generic.Invalid-Permission").replace("$str$", "subservers.command")));
                return CommandResult.builder().successCount(0).build();
            }
        }
    }

    public final class STOP implements CommandExecutor {
        public CommandResult execute(CommandSource sender, CommandContext args) throws CommandException {
            if (canRun(sender)) {
                Optional<String> subserver = args.getOne(Text.of("SubServer"));
                if (subserver.isPresent()) {
                    if (sender.hasPermission("subservers.subserver.stop." + subserver.get().toLowerCase())) {
                        plugin.subdata.sendPacket(new PacketStopServer((sender instanceof Player) ? ((Player) sender).getUniqueId():null, subserver.get(), false, data -> {
                            switch (data.getInt("r")) {
                                case 3:
                                    sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Stop.Unknown")));
                                    break;
                                case 4:
                                    sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Stop.Invalid")));
                                    break;
                                case 5:
                                    sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Stop.Not-Running")));
                                    break;
                                default:
                                    plugin.logger.warn("PacketStopServer(" + ((sender instanceof Player)?((Player) sender).getUniqueId().toString():"null") + ", " + subserver.get() + ", false) responded with: " + data.getString("m"));
                                case 0:
                                case 1:
                                    sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Stop")));
                                    break;
                            }
                        }));
                        return CommandResult.builder().successCount(1).build();
                    } else {
                        sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Generic.Invalid-Permission").replace("$str$", "subservers.subserver.stop." + subserver.get().toLowerCase())));
                        return CommandResult.builder().successCount(0).build();
                    }
                } else {
                    sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Generic.Usage").replace("$str$", "/sub stop <SubServer>")));
                    return CommandResult.builder().successCount(0).build();
                }
            } else {
                sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Generic.Invalid-Permission").replace("$str$", "subservers.command")));
                return CommandResult.builder().successCount(0).build();
            }
        }
    }

    public final class TERMINATE implements CommandExecutor {
        public CommandResult execute(CommandSource sender, CommandContext args) throws CommandException {
            if (canRun(sender)) {
                Optional<String> subserver = args.getOne(Text.of("SubServer"));
                if (subserver.isPresent()) {
                    if (sender.hasPermission("subservers.subserver.terminate." + subserver.get().toLowerCase())) {
                        plugin.subdata.sendPacket(new PacketStopServer((sender instanceof Player)?((Player) sender).getUniqueId():null, subserver.get(), true, data -> {
                            switch (data.getInt("r")) {
                                case 3:
                                    sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Terminate.Unknown")));
                                    break;
                                case 4:
                                    sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Terminate.Invalid")));
                                    break;
                                case 5:
                                    sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Terminate.Not-Running")));
                                    break;
                                default:
                                    plugin.logger.warn("PacketStopServer(" + ((sender instanceof Player)?((Player) sender).getUniqueId().toString():"null") + ", " + subserver.get() + ", true) responded with: " + data.getString("m"));
                                case 0:
                                case 1:
                                    sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Terminate")));
                                    break;
                            }
                        }));
                        return CommandResult.builder().successCount(1).build();
                    } else {
                        sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Generic.Invalid-Permission").replace("$str$", "subservers.subserver.terminate." + subserver.get().toLowerCase())));
                        return CommandResult.builder().successCount(0).build();
                    }
                } else {
                    sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Generic.Usage").replace("$str$", "/sub terminate <SubServer>")));
                    return CommandResult.builder().successCount(0).build();
                }
            } else {
                sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Generic.Invalid-Permission").replace("$str$", "subservers.command")));
                return CommandResult.builder().successCount(0).build();
            }
        }
    }

    public final class COMMAND implements CommandExecutor {
        public CommandResult execute(CommandSource sender, CommandContext args) throws CommandException {
            if (canRun(sender)) {
                Optional<String> subserver = args.getOne(Text.of("SubServer"));
                Optional<String> command = args.getOne(Text.of("Command"));
                if (subserver.isPresent() && command.isPresent()) {
                    if (sender.hasPermission("subservers.subserver.command." + subserver.get().toLowerCase())) {
                        plugin.subdata.sendPacket(new PacketCommandServer((sender instanceof Player)?((Player) sender).getUniqueId():null, subserver.get(), command.get(), data -> {
                            switch (data.getInt("r")) {
                                case 3:
                                    sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Command.Unknown")));
                                    break;
                                case 4:
                                    sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Command.Invalid")));
                                    break;
                                case 5:
                                    sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Command.Not-Running")));
                                    break;
                                default:
                                    plugin.logger.warn("PacketCommandServer(" + ((sender instanceof Player)?((Player) sender).getUniqueId().toString():"null") + ", " + subserver.get() + ", /" + command.get() + ") responded with: " + data.getString("m"));
                                case 0:
                                case 1:
                                    sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Command")));
                                    break;
                            }
                        }));
                        return CommandResult.builder().successCount(1).build();
                    } else {
                        sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Generic.Invalid-Permission").replace("$str$", "subservers.subserver.command." + subserver.get().toLowerCase())));
                        return CommandResult.builder().successCount(0).build();
                    }
                } else {
                    sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Generic.Usage").replace("$str$", "/sub command <SubServer> <Command> [Args...]")));
                    return CommandResult.builder().successCount(0).build();
                }
            } else {
                sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Generic.Invalid-Permission").replace("$str$", "subservers.command")));
                return CommandResult.builder().successCount(0).build();
            }
        }
    }

    public final class CREATE implements CommandExecutor {
        public CommandResult execute(CommandSource sender, CommandContext args) throws CommandException {
            if (canRun(sender)) {
                Optional<String> name = args.getOne(Text.of("Name"));
                Optional<String> host = args.getOne(Text.of("Host"));
                Optional<String> template = args.getOne(Text.of("Template"));
                Optional<String> version = args.getOne(Text.of("Version"));
                Optional<String> port = args.getOne(Text.of("Port"));
                if (name.isPresent() && host.isPresent() && template.isPresent() && version.isPresent()) {
                    if (sender.hasPermission("subservers.host.create." + host.get().toLowerCase())) {
                        if (port.isPresent() && Util.isException(() -> Integer.parseInt(port.get()))) {
                            sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Creator.Invalid-Port")));
                            return CommandResult.builder().successCount(0).build();
                        } else {
                            plugin.subdata.sendPacket(new PacketCreateServer((sender instanceof Player)?((Player) sender).getUniqueId():null, name.get(), host.get(), template.get(), new Version(version.get()), (port.isPresent())?Integer.parseInt(port.get()):null, data -> {
                                switch (data.getInt("r")) {
                                    case 3:
                                    case 4:
                                        sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Creator.Exists")));
                                        break;
                                    case 5:
                                        sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Creator.Unknown-Host")));
                                        break;
                                    case 6:
                                        sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Start.Host-Unavailable")));
                                        break;
                                    case 7:
                                        sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Start.Host-Disabled")));
                                        break;
                                    case 8:
                                        sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Creator.Unknown-Template")));
                                        break;
                                    case 9:
                                        sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Creator.Template-Disabled")));
                                        break;
                                    case 10:
                                        sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Creator.Invalid-Version")));
                                        break;
                                    case 11:
                                        sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Creator.Invalid-Port")));
                                        break;
                                    default:
                                        plugin.logger.warn("PacketCreateServer(" + ((sender instanceof Player)?((Player) sender).getUniqueId().toString():"null") + ", " + name.get() + ", " + host.get() + ", " + template.get() + ", " + version.get() + ", " + (port.orElse("null")) + ") responded with: " + data.getString("m"));
                                    case 0:
                                    case 1:
                                        sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Creator")));
                                        break;
                                }
                            }));
                            return CommandResult.builder().successCount(1).build();
                        }
                    } else {
                        sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Generic.Invalid-Permission").replace("$str$", "subservers.host.create." + host.get().toLowerCase())));
                        return CommandResult.builder().successCount(0).build();
                    }
                } else {
                    sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Generic.Usage").replace("$str$", "/sub create <Name> <Host> <Template> <Version> <Port>")));
                    return CommandResult.builder().successCount(0).build();
                }
            } else {
                sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Generic.Invalid-Permission").replace("$str$", "subservers.command")));
                return CommandResult.builder().successCount(0).build();
            }
        }
    }

    public final class OPEN implements CommandExecutor {
        public CommandResult execute(CommandSource sender, CommandContext args) throws CommandException {
            if (canRun(sender)) {
                if (plugin.gui != null) {
                    Optional<String> menu = args.getOne(Text.of("MenuID"));
                    String[] menuopts = args.<String>getAll(Text.of("Args")).toArray(new String[0]);
                    if (sender.hasPermission("subservers.interface")) {
                        try {
                            plugin.gui.getRenderer((Player) sender).clearHistory();
                            switch (menu.get().toLowerCase()) {
                                case "host":
                                    if (menuopts.length > 0) plugin.gui.getRenderer((Player) sender).hostMenu(Integer.parseInt(menuopts[0]));
                                    else plugin.gui.getRenderer((Player) sender).hostMenu(1);
                                    break;
                                case "host/":
                                    plugin.gui.getRenderer((Player) sender).hostAdmin(menuopts[0]);
                                    break;
                                case "host/creator":
                                    if (sender.hasPermission("subservers.host.create." + menuopts[0].toLowerCase())) plugin.gui.getRenderer((Player) sender).hostCreator(new UIRenderer.CreatorOptions(menuopts[0]));
                                    else throw new IllegalStateException("Player does not meet the requirements to render this page");
                                    break;
                                case "host/plugin":
                                    if (menuopts.length > 1) plugin.gui.getRenderer((Player) sender).hostPlugin(Integer.parseInt(menuopts[1]), menuopts[0]);
                                    else plugin.gui.getRenderer((Player) sender).hostPlugin(1, menuopts[0]);
                                    break;
                                case "group":
                                    if (menuopts.length > 0) plugin.gui.getRenderer((Player) sender).groupMenu(Integer.parseInt(menuopts[0]));
                                    else plugin.gui.getRenderer((Player) sender).groupMenu(1);
                                    break;
                                case "server":
                                    if (menuopts.length > 2) plugin.gui.getRenderer((Player) sender).serverMenu(Integer.parseInt(menuopts[0]), menuopts[2], null);
                                    else if (menuopts.length > 1) plugin.gui.getRenderer((Player) sender).serverMenu(Integer.parseInt(menuopts[0]), null, menuopts[1]);
                                    else if (menuopts.length > 0) plugin.gui.getRenderer((Player) sender).serverMenu(Integer.parseInt(menuopts[0]), null, null);
                                    else plugin.gui.getRenderer((Player) sender).serverMenu(1, null, null);
                                    break;
                                case "subserver/":
                                    plugin.gui.getRenderer((Player) sender).subserverAdmin(menuopts[0]);
                                    break;
                                case "subserver/plugin":
                                    if (menuopts.length > 1) plugin.gui.getRenderer((Player) sender).subserverPlugin(Integer.parseInt(menuopts[1]), menuopts[0]);
                                    else plugin.gui.getRenderer((Player) sender).subserverPlugin(1, menuopts[0]);
                                    break;
                            }
                            return CommandResult.builder().successCount(1).build();
                        } catch (Throwable e) {
                            List<String> list = new LinkedList<String>();
                            list.addAll(Arrays.asList(menuopts));
                            list.remove(0);
                            new InvocationTargetException(e, "Could not render page with arguments: " + list.toString()).printStackTrace();
                            return CommandResult.builder().successCount(0).build();
                        }
                    } else {
                        sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Generic.Invalid-Permission").replace("$str$", "subservers.interface")));
                        return CommandResult.builder().successCount(0).build();
                    }
                } else return CommandResult.builder().successCount(0).build();
            } else {
                sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Generic.Invalid-Permission").replace("$str$", "subservers.command")));
                return CommandResult.builder().successCount(0).build();
            }
        }
    }

    private Text[] printHelp() {
        return new Text[]{
                ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Help.Header")),
                ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Help.Help").replace("$str$", "/sub help")),
                ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Help.List").replace("$str$", "/sub list")),
                ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Help.Version").replace("$str$", "/sub version")),
                ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Help.Info").replace("$str$", "/sub info [proxy|host|group|server] <Name>")),
                ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Help.SubServer.Start").replace("$str$", "/sub start <SubServer>")),
                ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Help.SubServer.Restart").replace("$str$", "/sub restart <SubServer>")),
                ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Help.SubServer.Stop").replace("$str$", "/sub stop <SubServer>")),
                ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Help.SubServer.Terminate").replace("$str$", "/sub kill <SubServer>")),
                ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Help.SubServer.Command").replace("$str$", "/sub cmd <SubServer> <Command> [Args...]")),
                ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Help.Host.Create").replace("$str$", "/sub create <Name> <Host> <Template> <Version> <Port>")),
        };
    }
}
package net.ME1312.SubServers.Client.Sponge;

import net.ME1312.Galaxi.Library.Container.ContainedPair;
import net.ME1312.Galaxi.Library.Container.Container;
import net.ME1312.Galaxi.Library.Container.Pair;
import net.ME1312.Galaxi.Library.Container.Value;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Merger;
import net.ME1312.Galaxi.Library.Platform;
import net.ME1312.Galaxi.Library.Try;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.SubData.Client.SubDataClient;
import net.ME1312.SubServers.Client.Common.Network.API.*;
import net.ME1312.SubServers.Client.Common.Network.Packet.PacketCreateServer;
import net.ME1312.SubServers.Client.Common.Network.Packet.PacketRestartServer;
import net.ME1312.SubServers.Client.Common.Network.Packet.PacketUpdateServer;
import net.ME1312.SubServers.Client.Sponge.Graphic.UIRenderer;
import net.ME1312.SubServers.Client.Sponge.Library.Compatibility.ChatColor;
import net.ME1312.SubServers.Client.Sponge.Library.Compatibility.ListArgument;
import net.ME1312.SubServers.Client.Sponge.Network.Packet.PacketInExRunEvent;

import com.google.gson.Gson;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.selector.Selector;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static net.ME1312.SubServers.Client.Sponge.Library.ObjectPermission.permits;

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
        CommandSpec.Builder spec = CommandSpec.builder()
                .description(Text.of("The SubServers Command"))
                .executor(root)
                .arguments(GenericArguments.optional(GenericArguments.string(Text.of("Command"))), GenericArguments.optional(GenericArguments.remainingJoinedStrings(Text.of("..."))))
                .child(CommandSpec.builder()
                        .description(Text.of("The SubServers Command - Help"))
                        .executor(new HELP())
                        .arguments(GenericArguments.optional(GenericArguments.remainingJoinedStrings(Text.of("..."))))
                        .build(), "help", "?")
                .child(CommandSpec.builder()
                        .description(Text.of("The SubServers Command - Version"))
                        .executor(new VERSION())
                        .arguments(GenericArguments.optional(GenericArguments.remainingJoinedStrings(Text.of("..."))))
                        .build(), "version", "ver")
                .child(CommandSpec.builder()
                        .description(Text.of("The SubServers Command - List"))
                        .executor(new LIST())
                        .arguments(GenericArguments.optional(GenericArguments.remainingJoinedStrings(Text.of("..."))))
                        .build(), "list")
                .child(CommandSpec.builder()
                        .description(Text.of("The SubServers Command - Info"))
                        .executor(new INFO())
                        .arguments(GenericArguments.optional(GenericArguments.string(Text.of("Type"))), GenericArguments.optional(GenericArguments.string(Text.of("Name"))), GenericArguments.optional(GenericArguments.remainingJoinedStrings(Text.of("..."))))
                        .build(), "info", "status")
                .child(CommandSpec.builder()
                        .description(Text.of("The SubServers Command - Start"))
                        .executor(new START())
                        .arguments(GenericArguments.optional(new ListArgument(Text.of("Subservers"))), GenericArguments.optional(GenericArguments.remainingJoinedStrings(Text.of("..."))))
                        .build(), "start")
                .child(CommandSpec.builder()
                        .description(Text.of("The SubServers Command - Restart"))
                        .executor(new RESTART())
                        .arguments(GenericArguments.optional(new ListArgument(Text.of("Subservers"))), GenericArguments.optional(GenericArguments.remainingJoinedStrings(Text.of("..."))))
                        .build(), "restart")
                .child(CommandSpec.builder()
                        .description(Text.of("The SubServers Command - Stop"))
                        .executor(new STOP())
                        .arguments(GenericArguments.optional(new ListArgument(Text.of("Subservers"))), GenericArguments.optional(GenericArguments.remainingJoinedStrings(Text.of("..."))))
                        .build(), "stop")
                .child(CommandSpec.builder()
                        .description(Text.of("The SubServers Command - Terminate"))
                        .executor(new TERMINATE())
                        .arguments(GenericArguments.optional(new ListArgument(Text.of("Subservers"))), GenericArguments.optional(GenericArguments.remainingJoinedStrings(Text.of("..."))))
                        .build(), "kill", "terminate")
                .child(CommandSpec.builder()
                        .description(Text.of("The SubServers Command - Command"))
                        .executor(new COMMAND())
                        .arguments(GenericArguments.optional(new ListArgument(Text.of("Subservers"))), GenericArguments.optional(GenericArguments.remainingJoinedStrings(Text.of("Command"))))
                        .build(), "command", "cmd")
                .child(CommandSpec.builder()
                        .description(Text.of("The SubServers Command - Create"))
                        .executor(new CREATE())
                        .arguments(GenericArguments.optional(GenericArguments.string(Text.of("Name"))), GenericArguments.optional(GenericArguments.string(Text.of("Host"))), GenericArguments.optional(GenericArguments.string(Text.of("Template"))), GenericArguments.optional(GenericArguments.string(Text.of("Version"))), GenericArguments.optional(GenericArguments.string(Text.of("Port"))), GenericArguments.optional(GenericArguments.remainingJoinedStrings(Text.of("..."))))
                        .build(), "create")
                .child(CommandSpec.builder()
                        .description(Text.of("The SubServers Command - Update"))
                        .executor(new UPDATE())
                        .arguments(GenericArguments.optional(new ListArgument(Text.of("Subservers"))), GenericArguments.optional(GenericArguments.string(Text.of("Template"))), GenericArguments.optional(GenericArguments.string(Text.of("Version"))), GenericArguments.optional(GenericArguments.remainingJoinedStrings(Text.of("..."))))
                        .build(), "update", "upgrade")
                .child(CommandSpec.builder()
                        .description(Text.of("The SubServers Command - Teleport"))
                        .executor(new TELEPORT())
                        .arguments(GenericArguments.optional(GenericArguments.string(Text.of("Player"))), GenericArguments.optional(GenericArguments.string(Text.of("Server"))), GenericArguments.optional(GenericArguments.remainingJoinedStrings(Text.of("..."))))
                        .build(), "tp", "teleport")
                .child(CommandSpec.builder()
                        .description(Text.of("The SubServers Command - Open Menu"))
                        .executor(new OPEN())
                        .arguments(GenericArguments.optional(GenericArguments.string(Text.of("Menu"))), GenericArguments.optional(GenericArguments.allOf(GenericArguments.string(Text.of("Args")))))
                        .build(), "open", "view");

        if (plugin.config.get().getMap("Settings").getBoolean("Allow-Deletion", false)) spec
                .child(CommandSpec.builder()
                        .description(Text.of("The SubServers Command - Delete"))
                        .executor(new DELETE())
                        .arguments(GenericArguments.optional(new ListArgument(Text.of("Subservers"))), GenericArguments.optional(GenericArguments.remainingJoinedStrings(Text.of("..."))))
                        .build(), "remove", "del", "delete");

        return spec.build();
    }

    private boolean canRun(CommandSource sender) throws CommandException {
        return canRun(sender, false);
    }

    private boolean canRun(CommandSource sender, boolean permitted) throws CommandException {
        if (SubAPI.getInstance().getSubDataNetwork()[0] == null || plugin.api.getSubDataNetwork()[0].isClosed()) {
            throw new CommandException(Text.builder("An exception has occurred while running this command").color(TextColors.RED).build(), new IllegalStateException("SubData is not connected"), false);
        } else if (plugin.lang == null) {
            throw new CommandException(Text.builder("An exception has occurred while running this command").color(TextColors.RED).build(), new IllegalStateException("There are no lang options available at this time"), false);
        } else {
            return permitted || sender.hasPermission("subservers.command");
        }
    }

    public CommandResult execute(CommandSource sender, CommandContext args) throws CommandException {
        if (canRun(sender)) {
            Optional<String> subcommand = args.getOne(Text.of("Command"));
            if (subcommand.isPresent()) {
                sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Generic.Invalid-Subcommand").replace("$str$", subcommand.get())));
                return CommandResult.builder().successCount(0).build();
            } else {
                if (plugin.gui != null && sender instanceof Player && sender.hasPermission("subservers.interface")) {
                    plugin.gui.getRenderer((Player) sender).newUI();
                } else {
                    sender.sendMessages(printHelp());
                }
                return CommandResult.builder().successCount(1).build();
            }
        } else if (plugin.gui != null && sender instanceof Player && sender.hasPermission("subservers.interface")) {
            plugin.gui.getRenderer((Player) sender).newUI();
            return CommandResult.builder().successCount(1).build();
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
                PluginContainer container        = Try.all.get(() -> (PluginContainer) org.spongepowered.api.Platform.class.getMethod("getValue", Class.forName("org.spongepowered.api.Platform$Component")).invoke(Sponge.getPlatform(), Enum.valueOf((Class<Enum>) Class.forName("org.spongepowered.api.Platform$Component"), "IMPLEMENTATION")));
                if (container == null) container = Try.all.get(() -> (PluginContainer) org.spongepowered.api.Platform.class.getMethod("getImplementation").invoke(Sponge.getPlatform()));

                sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Version").replace("$str$", "SubServers.Client.Sponge")));
                sender.sendMessage(Text.builder("  " + Platform.getSystemName() + ' ' + Platform.getSystemVersion() + ((Platform.getSystemBuild() != null)?" (" + Platform.getSystemBuild() + ')':"") + ((!Platform.getSystemArchitecture().equals("unknown"))?" [" + Platform.getSystemArchitecture() + ']':"")).color(TextColors.WHITE).append(Text.of(",")).build());
                sender.sendMessage(Text.builder("  Java " + Platform.getJavaVersion() + ((!Platform.getJavaArchitecture().equals("unknown"))?" [" + Platform.getJavaArchitecture() + ']':"")).color(TextColors.WHITE).append(Text.of(",")).build());
                sender.sendMessage(Text.builder("  " + container.getName() + ' ' + container.getVersion().get()).color(TextColors.WHITE).append(Text.of(",")).build());
                sender.sendMessage(Text.builder("  SubServers.Client.Sponge v" + plugin.version.toExtendedString() + ((plugin.api.getPluginBuild() != null)?" (" + plugin.api.getPluginBuild() + ')':"")).color(TextColors.WHITE).build());
                sender.sendMessage(Text.EMPTY);
                plugin.game.getScheduler().createTaskBuilder().async().execute(() -> {
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
                                    message.onClick(TextActions.runCommand("/subservers open Server/ " + server.getName()));
                                    if (((SubServer) server).isRunning()) {
                                        message.color(TextColors.GREEN);
                                        hover.color(TextColors.GREEN);
                                        if (!server.getName().equals(server.getDisplayName())) {
                                            hover.append(Text.builder(server.getName() + '\n').color(TextColors.GRAY).build());
                                        }
                                        if (((SubServer) server).getStopAction() == SubServer.StopAction.REMOVE_SERVER || ((SubServer) server).getStopAction() == SubServer.StopAction.RECYCLE_SERVER || ((SubServer) server).getStopAction() == SubServer.StopAction.DELETE_SERVER) {
                                            message.color(TextColors.AQUA);
                                            hover.color(TextColors.AQUA);
                                            hover.append(ChatColor.convertColor(plugin.api.getLang("SubServers","Interface.Server-Menu.SubServer-Temporary") + '\n'));
                                        }
                                        hover.append(
                                                ChatColor.convertColor(plugin.api.getLang("SubServers","Interface.Server-Menu.Server-Player-Count").replace("$int$", new DecimalFormat("#,###").format(server.getRemotePlayers().size())))
                                        );
                                    } else if (((SubServer) server).isAvailable() && ((SubServer) server).isEnabled() && ((SubServer) server).getCurrentIncompatibilities().size() == 0) {
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
                                            hover.append(ChatColor.convertColor(plugin.api.getLang("SubServers","Interface.Server-Menu.SubServer-Incompatible").replace("$str$", list) + (((SubServer) server).isAvailable() && (((SubServer) server).isEnabled())?"":"\n")));
                                        }
                                        if (!((SubServer) server).isAvailable() || !((SubServer) server).isEnabled()) {
                                            hover.append(ChatColor.convertColor(plugin.api.getLang("SubServers",(!((SubServer) server).isAvailable())?"Interface.Server-Menu.SubServer-Unavailable":"Interface.Server-Menu.SubServer-Disabled")));
                                        }
                                    }
                                    if (plugin.config.get().getMap("Settings").getBoolean("Show-Addresses", false)) {
                                        hover.append(Text.builder('\n' + server.getAddress().getAddress().getHostAddress()+':'+server.getAddress().getPort()).color(TextColors.WHITE).build());
                                    } else {
                                        hover.append(Text.builder("\n" + server.getAddress().getPort()).color(TextColors.WHITE).build());
                                    }
                                    message.onClick(TextActions.runCommand("/subservers open Server/ " + server.getName()));
                                } else {
                                    message.color(TextColors.WHITE);
                                    hover.color(TextColors.WHITE);
                                    if (!server.getName().equals(server.getDisplayName())) {
                                        hover.append(Text.builder(server.getName() + '\n').color(TextColors.GRAY).build());
                                    }
                                    hover.append(
                                            ChatColor.convertColor(plugin.api.getLang("SubServers","Interface.Server-Menu.Server-External") + '\n'),
                                            ChatColor.convertColor(plugin.api.getLang("SubServers","Interface.Server-Menu.Server-Player-Count").replace("$int$", new DecimalFormat("#,###").format(server.getRemotePlayers().size())))
                                    );
                                    if (plugin.config.get().getMap("Settings").getBoolean("Show-Addresses", false)) {
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
                        if (plugin.config.get().getMap("Settings").getBoolean("Show-Addresses", false)) {
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
                                if (subserver.getStopAction() == SubServer.StopAction.REMOVE_SERVER || subserver.getStopAction() == SubServer.StopAction.RECYCLE_SERVER || subserver.getStopAction() == SubServer.StopAction.DELETE_SERVER) {
                                    message.color(TextColors.AQUA);
                                    hover.color(TextColors.AQUA);
                                    hover.append(ChatColor.convertColor(plugin.api.getLang("SubServers","Interface.Server-Menu.SubServer-Temporary") + '\n'));
                                }
                                hover.append(ChatColor.convertColor(plugin.api.getLang("SubServers","Interface.Server-Menu.Server-Player-Count").replace("$int$", new DecimalFormat("#,###").format(subserver.getRemotePlayers().size()))));
                            } else if (subserver.isAvailable() && subserver.isEnabled() && subserver.getCurrentIncompatibilities().size() == 0) {
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
                                    hover.append(ChatColor.convertColor(plugin.api.getLang("SubServers","Interface.Server-Menu.SubServer-Incompatible").replace("$str$", list) + ((subserver.isAvailable() && subserver.isEnabled())?"":"\n")));
                                }
                                if (!subserver.isAvailable() || !subserver.isEnabled()) {
                                    hover.append(ChatColor.convertColor(plugin.api.getLang("SubServers",(!subserver.isAvailable())?"Interface.Server-Menu.SubServer-Unavailable":"Interface.Server-Menu.SubServer-Disabled")));
                                }
                            }
                            if (plugin.config.get().getMap("Settings").getBoolean("Show-Addresses", false)) {
                                hover.append(Text.builder('\n' + subserver.getAddress().getAddress().getHostAddress()+':'+subserver.getAddress().getPort()).color(TextColors.WHITE).build());
                            } else {
                                hover.append(Text.builder("\n" + subserver.getAddress().getPort()).color(TextColors.WHITE).build());
                            }
                            message.onClick(TextActions.runCommand("/subservers open Server/ " + subserver.getName()));
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
                                ChatColor.convertColor(plugin.api.getLang("SubServers","Interface.Server-Menu.Server-Player-Count").replace("$int$", new DecimalFormat("#,###").format(server.getRemotePlayers().size()))));
                        if (plugin.config.get().getMap("Settings").getBoolean("Show-Addresses", false)) {
                            hover.append(Text.builder('\n' + server.getAddress().getAddress().getHostAddress()+':'+server.getAddress().getPort()).color(TextColors.WHITE).build());
                        } else {
                            hover.append(Text.builder("\n" + server.getAddress().getPort()).color(TextColors.WHITE).build());
                        }
                        message.onClick(TextActions.runCommand("/subservers open Server/ " + server.getName()));
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
                            if (!proxymaster.getName().equals(proxymaster.getDisplayName())) {
                                hover.append(Text.builder('\n' + proxymaster.getDisplayName()).color(TextColors.GRAY).build());
                            }
                            hover.append(
                                    ChatColor.convertColor('\n' + plugin.api.getLang("SubServers", "Interface.Proxy-Menu.Proxy-Master")),
                                    ChatColor.convertColor('\n' + plugin.api.getLang("SubServers", "Interface.Proxy-Menu.Proxy-Player-Count").replace("$int$", new DecimalFormat("#,###").format(proxymaster.getPlayers().size())))
                            );
                        } else hover.append(ChatColor.convertColor('\n' + plugin.api.getLang("SubServers", "Interface.Proxy-Menu.Proxy-Master")));
                        message.onHover(TextActions.showText(hover.build()));
                        msg.append(message.build());
                        for (Proxy proxy : proxies.values()) {
                            message = Text.builder(proxy.getDisplayName());
                            hover = Text.builder(proxy.getDisplayName());
                            if (proxy.getSubData()[0] != null) {
                                message.color(TextColors.AQUA);
                                hover.color(TextColors.AQUA);
                                if (!proxy.getName().equals(proxy.getDisplayName())) {
                                    hover.append(Text.builder('\n' + proxy.getName()).color(TextColors.GRAY).build());
                                }
                                hover.append(ChatColor.convertColor('\n' + plugin.api.getLang("SubServers", "Interface.Proxy-Menu.Proxy-Player-Count").replace("$int$", new DecimalFormat("#,###").format(proxy.getPlayers().size()))));
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


                    Runnable getPlayer = () -> plugin.api.getRemotePlayer(name, player -> {
                        if (player != null) {
                            sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Info").replace("$str$", "player")).toBuilder().append(Text.builder(player.getName()).color(TextColors.WHITE).build()).build());
                            if (player.getProxyName() != null) sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Proxy")).toBuilder().append(Text.builder(player.getProxyName()).color(TextColors.WHITE).build()).build());
                            if (player.getServerName() != null) sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Server")).toBuilder().append(Text.builder(player.getServerName()).color(TextColors.WHITE).build()).build());
                            if (player.getAddress() != null && plugin.config.get().getMap("Settings").getBoolean("Show-Addresses", false))
                                sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Address")).toBuilder().append(Text.builder(player.getAddress().getAddress().getHostAddress() + ':' + player.getAddress().getPort()).color(TextColors.WHITE).build()).build());
                            sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "UUID")).toBuilder().append(Text.builder(player.getUniqueId().toString()).color(TextColors.AQUA).build()).build());
                        } else {
                            if (type == null) {
                                sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Info.Unknown")));
                            } else {
                                sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Info.Unknown-Player")));
                            }
                        }
                    });
                    Runnable getServer = () -> plugin.api.getServer(name, server -> {
                        if (server != null) {
                            sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Info").replace("$str$", ((server instanceof SubServer)?"sub":"") + "server")).toBuilder().append(Text.builder(server.getDisplayName()).color(TextColors.WHITE).build()).build());
                            if (!server.getName().equals(server.getDisplayName())) sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "System Name")).toBuilder().append(Text.builder(server.getName()).color(TextColors.WHITE).build()).build());
                            if (server instanceof SubServer) {
                                sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Available")).toBuilder().append(Text.builder((((SubServer) server).isAvailable())?"yes":"no").color((((SubServer) server).isAvailable())?TextColors.GREEN:TextColors.RED).build()).build());
                                sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Enabled")).toBuilder().append(Text.builder((((SubServer) server).isEnabled())?"yes":"no").color((((SubServer) server).isEnabled())?TextColors.GREEN:TextColors.RED).build()).build());
                                if (!((SubServer) server).isEditable()) sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Editable")).toBuilder().append(Text.builder("no").color(TextColors.RED).build()).build());
                                sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Host")).toBuilder().append(Text.builder(((SubServer) server).getHost()).color(TextColors.WHITE).build()).build());
                                if (((SubServer) server).getTemplate() != null) sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Template")).toBuilder().append(Text.builder(((SubServer) server).getTemplate()).color(TextColors.WHITE).build()).build());
                            }
                            if (server.getGroups().size() > 0) sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Group" + ((server.getGroups().size() > 1)?"s":""))).toBuilder().append(Text.builder((server.getGroups().size() > 1)?"":server.getGroups().get(0)).color(TextColors.WHITE).build()).build());
                            if (server.getGroups().size() > 1) for (String group : server.getGroups()) sender.sendMessage(ChatColor.convertColor("    " + plugin.api.getLang("SubServers", "Command.Info.List")).toBuilder().append(Text.builder(group).color(TextColors.WHITE).build()).build());
                            if (plugin.config.get().getMap("Settings").getBoolean("Show-Addresses", false)) sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Address")).toBuilder().append(Text.builder(server.getAddress().getAddress().getHostAddress()+':'+server.getAddress().getPort()).color(TextColors.WHITE).build()).build());
                            else sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Port")).toBuilder().append(Text.builder(Integer.toString(server.getAddress().getPort())).color(TextColors.AQUA).build()).build());
                            if (server instanceof SubServer) sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", ((((SubServer) server).isOnline())?"Online":"Running"))).toBuilder().append(Text.builder((((SubServer) server).isRunning())?"yes":"no").color((((SubServer) server).isRunning())?TextColors.GREEN:TextColors.RED).build()).build());
                            if (!(server instanceof SubServer) || ((SubServer) server).isRunning()) {
                                sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Connected")).toBuilder().append(Text.builder((server.getSubData()[0] != null)?"yes":"no").color((server.getSubData()[0] != null)?TextColors.GREEN:TextColors.RED).build(), Text.builder((server.getSubData().length > 1)?" +"+(server.getSubData().length-1)+" subchannel"+((server.getSubData().length == 2)?"":"s"):"").color(TextColors.AQUA).build()).build());
                                sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Players")).toBuilder().append(Text.builder(server.getRemotePlayers().size() + " online").color(TextColors.AQUA).build()).build());
                            }
                            sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "MOTD")).toBuilder().append(Text.builder(server.getMotd().replaceAll("\\u00A7[0-9a-fA-Fk-oK-ORr]", "")).color(TextColors.WHITE).build()).build());
                            if (server instanceof SubServer) {
                                if (((SubServer) server).getStopAction() != SubServer.StopAction.NONE) sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Stop Action")).toBuilder().append(Text.builder(((SubServer) server).getStopAction().toString()).color(TextColors.WHITE).build()).build());
                                if (((SubServer) server).isStopping()) sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Stopping")).toBuilder().append(Text.builder("yes").color(TextColors.GREEN).build()).build());
                            }
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
                                getPlayer.run();
                            } else {
                                sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Info.Unknown-Server")));
                            }
                        }
                    });
                    Runnable getGroup = () -> plugin.api.getGroup(name, group -> {
                        if (group != null) {
                            sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Info").replace("$str$", "group")).toBuilder().append(Text.builder(group.key()).color(TextColors.WHITE).build()).build());
                            sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Servers")).toBuilder().append(Text.builder((group.value().size() <= 0)?"(none)":Integer.toString(group.value().size())).color((group.value().size() <= 0)?TextColors.GRAY:TextColors.AQUA).build()).build());
                            for (Server server : group.value()) sender.sendMessage(ChatColor.convertColor("    " + plugin.api.getLang("SubServers", "Command.Info.List")).toBuilder().append(Text.builder(server.getDisplayName() + ((server.getName().equals(server.getDisplayName()))?"":" ["+server.getName()+']')).color(TextColors.WHITE).build()).build());
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
                            sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Info").replace("$str$", "host")).toBuilder().append(Text.builder(host.getDisplayName()).color(TextColors.WHITE).build()).build());
                            if (!host.getName().equals(host.getDisplayName())) sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "System Name")).toBuilder().append(Text.builder(host.getName()).color(TextColors.WHITE).build()).build());
                            sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Available")).toBuilder().append(Text.builder((host.isAvailable())?"yes":"no").color((host.isAvailable())?TextColors.GREEN:TextColors.RED).build()).build());
                            sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Enabled")).toBuilder().append(Text.builder((host.isEnabled())?"yes":"no").color((host.isEnabled())?TextColors.GREEN:TextColors.RED).build()).build());
                            if (plugin.config.get().getMap("Settings").getBoolean("Show-Addresses", false)) sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Address")).toBuilder().append(Text.builder(host.getAddress().getHostAddress()).color(TextColors.WHITE).build()).build());
                            if (host.getSubData().length > 0) sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Connected")).toBuilder().append(Text.builder((host.getSubData()[0] != null)?"yes":"no").color((host.getSubData()[0] != null)?TextColors.GREEN:TextColors.RED).build(), Text.builder((host.getSubData().length > 1)?" +"+(host.getSubData().length-1)+" subchannel"+((host.getSubData().length == 2)?"":"s"):"").color(TextColors.AQUA).build()).build());
                            sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "SubServers")).toBuilder().append(Text.builder((host.getSubServers().keySet().size() <= 0)?"(none)":Integer.toString(host.getSubServers().keySet().size())).color((host.getSubServers().keySet().size() <= 0)?TextColors.GRAY:TextColors.AQUA).build()).build());
                            for (SubServer subserver : host.getSubServers().values()) sender.sendMessage(ChatColor.convertColor("    " + plugin.api.getLang("SubServers", "Command.Info.List")).toBuilder().append(Text.builder(subserver.getDisplayName() + ((subserver.getName().equals(subserver.getDisplayName()))?"":" ["+subserver.getName()+']')).color((subserver.isEnabled())?TextColors.WHITE:TextColors.GRAY).build()).build());
                            sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Templates")).toBuilder().append(Text.builder((host.getCreator().getTemplates().keySet().size() <= 0)?"(none)":Integer.toString(host.getCreator().getTemplates().keySet().size())).color((host.getCreator().getTemplates().keySet().size() <= 0)?TextColors.GRAY:TextColors.AQUA).build()).build());
                            for (SubCreator.ServerTemplate template : host.getCreator().getTemplates().values()) sender.sendMessage(ChatColor.convertColor("    " + plugin.api.getLang("SubServers", "Command.Info.List")).toBuilder().append(Text.builder(template.getDisplayName() + ((template.getName().equals(template.getDisplayName()))?"":" ["+template.getName()+']')).color((template.isEnabled())?TextColors.WHITE:TextColors.GRAY).build()).build());
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
                            sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Info").replace("$str$", "proxy")).toBuilder().append(Text.builder(proxy.getDisplayName()).color(TextColors.WHITE).build()).build());
                            if (!proxy.getName().equals(proxy.getDisplayName())) sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "System Name")).toBuilder().append(Text.builder(proxy.getName()).color(TextColors.WHITE).build()).build());
                            if (!proxy.isMaster()) sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Connected")).toBuilder().append(Text.builder((proxy.getSubData()[0] != null)?"yes":"no").color((proxy.getSubData()[0] != null)?TextColors.GREEN:TextColors.RED).build(), Text.builder((proxy.getSubData().length > 1)?" +"+(proxy.getSubData().length-1)+" subchannel"+((proxy.getSubData().length == 2)?"":"s"):"").color(TextColors.AQUA).build()).build());
                            else if (!proxy.getDisplayName().toLowerCase().contains("master")) sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Type")).toBuilder().append(Text.builder("Master").color(TextColors.WHITE).build()).build());
                            sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Info.Format").replace("$str$", "Players")).toBuilder().append(Text.builder(proxy.getPlayers().size() + " online").color(TextColors.AQUA).build()).build());
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
                            case "u":
                            case "user":
                            case "player":
                                getPlayer.run();
                                break;
                            default:
                                sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Info.Unknown-Type")));
                        }
                    }
                    return CommandResult.builder().successCount(1).build();
                } else {
                    sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Generic.Usage").replace("$str$", "/sub info [proxy|host|group|server|player] <Name>")));
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
                Optional<String[]> s = args.getOne(Text.of("Subservers"));
                if (s.isPresent()) {
                    selectServers(sender, s.get(), true, new String[]{"subservers.subserver.%.*", "subservers.subserver.%.start"}, select -> {
                        if (select.subservers.length > 0) {
                            Container<Integer> success = new Container<Integer>(0);
                            Container<Integer> running = new Container<Integer>(0);
                            Merger merge = new Merger(() -> {
                                if (running.value > 0) sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Start.Running").replace("$int$", running.value.toString())));
                                if (success.value > 0) sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Start").replace("$int$", success.value.toString())));
                            });
                            for (SubServer server : select.subservers) {
                                merge.reserve();
                                server.start((sender instanceof Player)?((Player) sender).getUniqueId():null, response -> {
                                    switch (response) {
                                        case 3:
                                        case 4:
                                            sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Start.Disappeared").replace("$str$", server.getName())));
                                            break;
                                        case 5:
                                            sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Start.Host-Unavailable").replace("$str$", server.getName())));
                                            break;
                                        case 6:
                                            sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Start.Host-Disabled").replace("$str$", server.getName())));
                                            break;
                                        case 7:
                                            sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Start.Server-Unavailable").replace("$str$", server.getName())));
                                            break;
                                        case 8:
                                            sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Start.Server-Disabled").replace("$str$", server.getName())));
                                            break;
                                        case 9:
                                            running.value++;
                                            break;
                                        case 10:
                                            sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Start.Server-Incompatible").replace("$str$", server.getName())));
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
                    return CommandResult.builder().successCount(1).build();
                } else {
                    sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Generic.Usage").replace("$str$", "/sub start <Subservers>")));
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
                Optional<String[]> s = args.getOne(Text.of("Subservers"));
                if (s.isPresent()) {
                    selectServers(sender, s.get(), true, new String[][]{{"subservers.subserver.%.*", "subservers.subserver.%.start"}, {"subservers.subserver.%.*", "subservers.subserver.%.stop"}}, select -> {
                        if (select.subservers.length > 0) {
                            // Step 5: Start the stopped Servers once more
                            final UUID player = (sender instanceof Player)?((Player) sender).getUniqueId():null;
                            Consumer<SubServer> starter = server -> server.start(player, response -> {
                                switch (response) {
                                    case 3:
                                    case 4:
                                        sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Restart.Disappeared").replace("$str$", server.getName())));
                                        break;
                                    case 5:
                                        sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Restart.Host-Unavailable").replace("$str$", server.getName())));
                                        break;
                                    case 6:
                                        sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Restart.Host-Disabled").replace("$str$", server.getName())));
                                        break;
                                    case 7:
                                        sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Restart.Server-Unavailable").replace("$str$", server.getName())));
                                        break;
                                    case 8:
                                        sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Restart.Server-Disabled").replace("$str$", server.getName())));
                                        break;
                                    case 10:
                                        sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Start.Server-Incompatible").replace("$str$", server.getName())));
                                        break;
                                    case 9:
                                    case 0:
                                        // success!
                                        break;
                                }
                            });

                            // Step 4: Listen for stopped Servers
                            final HashMap<String, SubServer> listening = new HashMap<String, SubServer>();
                            PacketInExRunEvent.callback("SubStoppedEvent", new Consumer<ObjectMap<String>>() {
                                @Override
                                public void accept(ObjectMap<String> json) {
                                    try {
                                        if (listening.size() > 0) {
                                            PacketInExRunEvent.callback("SubStoppedEvent", this);
                                            String name = json.getString("server").toLowerCase();
                                            if (listening.containsKey(name)) {
                                                plugin.game.getScheduler().createTaskBuilder().execute(() -> {
                                                    starter.accept(listening.get(name));
                                                    listening.remove(name);
                                                }).delay(100, TimeUnit.MILLISECONDS).submit(plugin);
                                            }
                                        }
                                    } catch (Exception e) {}
                                }
                            });

                            // Step 3: Receive command Responses
                            Container<Integer> success = new Container<Integer>(0);
                            Merger merge = new Merger(() -> {
                                if (success.value > 0) sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Restart").replace("$int$", success.value.toString())));
                            });
                            Consumer<Pair<Integer, SubServer>> stopper = data -> {
                                if (data.key() != 0) listening.remove(data.value().getName().toLowerCase());
                                switch (data.key()) {
                                    case 3:
                                    case 4:
                                        sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Restart.Disappeared").replace("$str$", data.value().getName())));
                                        break;
                                    case 5:
                                        starter.accept(data.value());
                                    case 0:
                                        success.value++;
                                        break;
                                }
                                merge.release();
                            };

                            // Step 1: Detect Self
                            SubServer self = null;
                            for (SubServer server : select.subservers) {
                                if (server.getName().equalsIgnoreCase(plugin.api.getName())) {
                                    self = server;
                                    break;
                                }
                            }

                            // Step 2: Restart Servers
                            for (SubServer server : select.subservers) {
                                merge.reserve();
                                if (self == null) {
                                    listening.put(server.getName().toLowerCase(), server);
                                    server.stop(player, response -> stopper.accept(new ContainedPair<>(response, server)));
                                } else if (self != server) {
                                    ((SubDataClient) plugin.api.getSubDataNetwork()[0]).sendPacket(new PacketRestartServer(player, server.getName(), data -> stopper.accept(new ContainedPair<>(data.getInt(0x0001), server))));
                                }
                            }
                            if (self != null) {
                                final SubServer fself = self;
                                ((SubDataClient) plugin.api.getSubDataNetwork()[0]).sendPacket(new PacketRestartServer(player, self.getName(), data -> stopper.accept(new ContainedPair<>(data.getInt(0x0001), fself))));
                            }
                        }
                    });
                    return CommandResult.builder().successCount(1).build();
                } else {
                    sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Generic.Usage").replace("$str$", "/sub restart <Subservers>")));
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
                Optional<String[]> s = args.getOne(Text.of("Subservers"));;
                if (s.isPresent()) {
                    selectServers(sender, s.get(), true, new String[]{"subservers.subserver.%.*", "subservers.subserver.%.stop"}, select -> {
                        if (select.subservers.length > 0) {
                            Container<Integer> success = new Container<Integer>(0);
                            Container<Integer> running = new Container<Integer>(0);
                            Merger merge = new Merger(() -> {
                                if (running.value > 0) sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Stop.Not-Running").replace("$int$", running.value.toString())));
                                if (success.value > 0) sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Stop").replace("$int$", success.value.toString())));
                            });
                            Consumer<Pair<Integer, SubServer>> stopper = data -> {
                                switch (data.key()) {
                                    case 3:
                                    case 4:
                                        sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Stop.Disappeared").replace("$str$", data.value().getName())));
                                        break;
                                    case 5:
                                        running.value++;
                                        break;
                                    case 0:
                                        success.value++;
                                        break;
                                }
                                merge.release();
                            };

                            SubServer self = null;
                            for (SubServer server : select.subservers) {
                                if (server.getName().equalsIgnoreCase(plugin.api.getName())) {
                                    self = server;
                                    break;
                                }
                            }

                            for (SubServer server : select.subservers) {
                                merge.reserve();
                                if (self != server) server.stop((sender instanceof Player)?((Player) sender).getUniqueId():null, response -> stopper.accept(new ContainedPair<>(response, server)));
                            }
                            if (self != null) {
                                final SubServer fself = self;
                                self.stop((sender instanceof Player)?((Player) sender).getUniqueId():null, response -> stopper.accept(new ContainedPair<>(response, fself)));
                            }
                        }
                    });
                    return CommandResult.builder().successCount(1).build();
                } else {
                    sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Generic.Usage").replace("$str$", "/sub stop <Subservers>")));
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
                Optional<String[]> s = args.getOne(Text.of("Subservers"));
                if (s.isPresent()) {
                    selectServers(sender, s.get(), true, new String[]{"subservers.subserver.%.*", "subservers.subserver.%.terminate"}, select -> {
                        if (select.subservers.length > 0) {
                            Container<Integer> success = new Container<Integer>(0);
                            Container<Integer> running = new Container<Integer>(0);
                            Merger merge = new Merger(() -> {
                                if (running.value > 0) sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Terminate.Not-Running").replace("$int$", running.value.toString())));
                                if (success.value > 0) sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Terminate").replace("$int$", success.value.toString())));
                            });
                            Consumer<Pair<Integer, SubServer>> stopper = data -> {
                                switch (data.key()) {
                                    case 3:
                                    case 4:
                                        sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Terminate.Disappeared").replace("$str$", data.value().getName())));
                                        break;
                                    case 5:
                                        running.value++;
                                        break;
                                    case 0:
                                        success.value++;
                                        break;
                                }
                                merge.release();
                            };

                            SubServer self = null;
                            for (SubServer server : select.subservers) {
                                if (server.getName().equalsIgnoreCase(plugin.api.getName())) {
                                    self = server;
                                    break;
                                }
                            }

                            for (SubServer server : select.subservers) {
                                merge.reserve();
                                if (self != server) server.terminate((sender instanceof Player)?((Player) sender).getUniqueId():null, response -> stopper.accept(new ContainedPair<>(response, server)));
                            }
                            if (self != null) {
                                final SubServer fself = self;
                                fself.terminate((sender instanceof Player) ? ((Player) sender).getUniqueId() : null, response -> stopper.accept(new ContainedPair<>(response, fself)));
                            }
                        }
                    });
                    return CommandResult.builder().successCount(1).build();
                } else {
                    sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Generic.Usage").replace("$str$", "/sub terminate <Subservers>")));
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
                Optional<String[]> s = args.getOne(Text.of("Subservers"));
                Optional<String> command = args.getOne(Text.of("Command"));
                if (s.isPresent()) {
                    selectServers(sender, s.get(), false, new String[]{"subservers.subserver.%.*", "subservers.subserver.%.command"}, select -> {
                        if (select.servers.length > 0) {
                            if (command.isPresent()) {
                                Container<Integer> success = new Container<Integer>(0);
                                Container<Integer> running = new Container<Integer>(0);
                                Merger merge = new Merger(() -> {
                                    if (running.value > 0) sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Command.Not-Running").replace("$int$", running.value.toString())));
                                    if (success.value > 0) sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Command").replace("$int$", success.value.toString())));
                                });
                                for (Server server : select.servers) {
                                    merge.reserve();
                                    server.command((sender instanceof Player)?((Player) sender).getUniqueId():null, command.get(), response -> {
                                        switch (response) {
                                            case 3:
                                            case 4:
                                                sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Command.Disappeared").replace("$str$", server.getName())));
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
                                sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Command.No-Command")));
                            }
                        }
                    });
                    return CommandResult.builder().successCount(1).build();
                } else {
                    sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Generic.Usage").replace("$str$", "/sub command <Servers> <Command> [Args...]")));
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
                if (name.isPresent() && host.isPresent() && template.isPresent()) {
                    if (permits(host.get(), sender, "subservers.host.%.*", "subservers.host.%.create")) {
                        if (port.isPresent() && !Try.all.run(() -> Integer.parseInt(port.get()))) {
                            sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Creator.Invalid-Port")));
                            return CommandResult.builder().successCount(0).build();
                        } else {
                            ((SubDataClient) SubAPI.getInstance().getSubDataNetwork()[0]).sendPacket(new PacketCreateServer((sender instanceof Player)?((Player) sender).getUniqueId():null, name.get(), host.get(), template.get(), (version.isPresent() && version.get().length() > 0)?new Version(version.get()):null, (port.isPresent())?Integer.parseInt(port.get()):null, data -> {
                                switch (data.getInt(0x0001)) {
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
                                        sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Creator.Version-Required")));
                                        break;
                                    case 11:
                                        sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Creator.Invalid-Port")));
                                        break;
                                    case 0:
                                    case 1:
                                        sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Creator").replace("$str$", name.get())));
                                        break;
                                }
                            }));
                            return CommandResult.builder().successCount(1).build();
                        }
                    } else {
                        sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Generic.Invalid-Permission").replace("$str$", "subservers.host." + host.get().toLowerCase() + ".create")));
                        return CommandResult.builder().successCount(0).build();
                    }
                } else {
                    sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Generic.Usage").replace("$str$", "/sub create <Name> <Host> <Template> [Version] [Port]")));
                    return CommandResult.builder().successCount(0).build();
                }
            } else {
                sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Generic.Invalid-Permission").replace("$str$", "subservers.command")));
                return CommandResult.builder().successCount(0).build();
            }
        }
    }

    public final class UPDATE implements CommandExecutor {
        public CommandResult execute(CommandSource sender, CommandContext args) throws CommandException {
            if (canRun(sender)) {
                Optional<String[]> s = args.getOne(Text.of("Subservers"));
                Optional<String> template = args.getOne(Text.of("Template"));
                Optional<String> version = args.getOne(Text.of("Version"));

                final String ft;
                final Version fv;
                if (version.isPresent()) {
                    ft = template.get();
                    fv = new Version(version.get());
                } else if (template.isPresent()) {
                    ft = null;
                    fv = new Version(template.get());
                } else {
                    ft = null;
                    fv = null;
                }

                if (s.isPresent()) {
                    selectServers(sender, s.get(), true, new String[]{"subservers.subserver.%.*", "subservers.subserver.%.update"}, select -> {
                        if (select.subservers.length > 0) {
                            boolean ts = ft == null;

                            Container<Integer> success = new Container<Integer>(0);
                            Merger merge = new Merger(() -> {
                                if (success.value > 0) sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Update").replace("$int$", success.value.toString())));
                            });
                            for (SubServer server : select.subservers) {
                                merge.reserve();
                                ((SubDataClient) SubAPI.getInstance().getSubDataNetwork()[0]).sendPacket(new PacketUpdateServer((sender instanceof Player)?((Player) sender).getUniqueId():null, server.getName(), ft, fv, data -> {
                                    switch (data.getInt(0x0001)) {
                                        case 3:
                                        case 4:
                                            sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Update.Disappeared").replace("$str$", server.getName())));
                                            break;
                                        case 5:
                                            sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Update.Host-Unavailable").replace("$str$", server.getName())));
                                            break;
                                        case 6:
                                            sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Update.Host-Disabled").replace("$str$", server.getName())));
                                            break;
                                        case 7:
                                            sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Update.Server-Unavailable").replace("$str$", server.getName())));
                                            break;
                                        case 8:
                                            sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Update.Running").replace("$str$", server.getName())));
                                            break;
                                        case 9:
                                            if (ts) sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Update.Unknown-Template").replace("$str$", server.getName())));
                                            else    sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Creator.Unknown-Template")));
                                            break;
                                        case 10:
                                            if (ts) sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Update.Template-Disabled").replace("$str$", server.getName())));
                                            else    sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Creator.Template-Disabled")));
                                            break;
                                        case 11:
                                            if (ts) sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Update.Template-Invalid").replace("$str$", server.getName())));
                                            else    sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Creator.Template-Invalid")));
                                            break;
                                        case 12:
                                            sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Update.Version-Required").replace("$str$", server.getName())));
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
                    return CommandResult.builder().successCount(1).build();
                } else {
                    sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Generic.Usage").replace("$str$", "/sub update <Subservers> [[Template] <Version>]")));
                    return CommandResult.builder().successCount(0).build();
                }
            } else {
                sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Generic.Invalid-Permission").replace("$str$", "subservers.command")));
                return CommandResult.builder().successCount(0).build();
            }
        }
    }

    public final class DELETE implements CommandExecutor {
        public CommandResult execute(CommandSource sender, CommandContext args) throws CommandException {
            if (plugin.config.get().getMap("Settings").getBoolean("Allow-Deletion", false)) {
                if (canRun(sender)) {
                    Optional<String[]> s = args.getOne(Text.of("Subservers"));
                    if (s.isPresent()) {
                        selectServers(sender, s.get(), true, "subservers.subserver.%.delete", select -> {
                            if (select.subservers.length > 0) {
                                Container<Integer> success = new Container<Integer>(0);
                                Merger merge = new Merger(() -> {
                                    if (success.value > 0) sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Delete").replace("$int$", success.value.toString())));
                                });
                                for (SubServer server : select.subservers) {
                                    if (server.isRunning()) {
                                        sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Delete.Running").replace("$str$", server.getName())));
                                    } else {
                                        server.getHost(host -> {
                                            if (host == null) {
                                                sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Delete.Disappeared").replace("$str$", server.getName())));
                                            } else {
                                                merge.reserve();
                                                host.recycleSubServer(server.getName(), response -> {
                                                    switch (response) {
                                                        case 3:
                                                        case 4:
                                                            sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Delete.Disappeared").replace("$str$", server.getName())));
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
                        return CommandResult.builder().successCount(1).build();
                    } else {
                        sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Generic.Usage").replace("$str$", "/sub delete <Subservers>")));
                        return CommandResult.builder().successCount(0).build();
                    }
                } else {
                    sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Generic.Invalid-Permission").replace("$str$", "subservers.command")));
                    return CommandResult.builder().successCount(0).build();
                }
            } else {
                return CommandResult.builder().successCount(0).build();
            }
        }
    }

    public final class TELEPORT implements CommandExecutor {
        public CommandResult execute(CommandSource sender, CommandContext args) throws CommandException {
            if (canRun(sender, true)) {
                Optional<String> p = args.getOne(Text.of("Player"));
                Optional<String> s = args.getOne(Text.of("Server"));
                if (!s.isPresent()) {
                    Optional<String> tmp = s;
                    s = p;
                    p = tmp;
                }

                if (s.isPresent() && (p.isPresent() || sender instanceof Player)) {
                    if (sender.hasPermission("subservers.teleport")) {
                        String name = (p.isPresent())?p.get():null;
                        String select = s.get();
                        plugin.api.getServer(select, server -> {
                            if (server != null) {
                                if (!(server instanceof SubServer) || ((SubServer) server).isRunning()) {
                                    Value<Boolean> msg = new Container<>(false);
                                    Consumer<Player> action = target -> {
                                        if (target == sender || sender.hasPermission("subservers.teleport-others")) {
                                            sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", (target == sender)?"Command.Teleport":"Command.Teleport.Others").replace("$name$", target.getName()).replace("$str$", server.getDisplayName())));
                                            plugin.pmc(target, "Connect", server.getName());
                                        } else if (!msg.value()) {
                                            msg.value(true);
                                            sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Generic.Invalid-Permission").replace("$str$", "subservers.teleport-others")));
                                        }
                                    };

                                    Optional<Player> player;
                                    Container<Set<Entity>> entities = new Container<>(null);
                                    if (name == null) {
                                        action.accept((Player) sender);
                                    } else if ((player = plugin.game.getServer().getPlayer(name)).isPresent()) {
                                        action.accept(player.get());
                                    } else if (Try.all.get(() -> (entities.value = Selector.parse(name).resolve(sender)).size() > 0, false)) {
                                        for (Entity entity : entities.value) {
                                            if (entity instanceof Player) {
                                                action.accept((Player) entity);
                                            } else {
                                                sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Generic.Unknown-Player").replace("$str$", entity.getUniqueId().toString())));
                                            }
                                        }
                                    } else {
                                        sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Generic.Unknown-Player").replace("$str$", name)));
                                    }
                                } else {
                                    sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Teleport.Not-Running").replace("$str$", server.getName())));
                                }
                            } else {
                                sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Generic.Unknown-Server").replace("$str$", select)));
                            }
                        });
                        return CommandResult.builder().successCount(1).build();
                    } else {
                        sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Generic.Invalid-Permission").replace("$str$", "subservers.teleport")));
                        return CommandResult.builder().successCount(0).build();
                    }
                } else {
                    sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Generic.Usage").replace("$str$", "/sub teleport " + ((sender instanceof Player)?"[Player]":"<Player>") + " <Server>")));
                    return CommandResult.builder().successCount(0).build();
                }
            } else {
                sender.sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Generic.Invalid-Permission").replace("$str$", "subservers.request")));
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
                                    if (permits(menuopts[0], sender, "subservers.host.%.*", "subservers.host.%.create"))
                                        plugin.gui.getRenderer((Player) sender).hostCreator(new UIRenderer.CreatorOptions(menuopts[0]));
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
                                case "subserver":
                                    if (menuopts.length > 2) plugin.gui.getRenderer((Player) sender).serverMenu(Integer.parseInt(menuopts[0]), menuopts[2], null);
                                    else if (menuopts.length > 1) plugin.gui.getRenderer((Player) sender).serverMenu(Integer.parseInt(menuopts[0]), null, menuopts[1]);
                                    else if (menuopts.length > 0) plugin.gui.getRenderer((Player) sender).serverMenu(Integer.parseInt(menuopts[0]), null, null);
                                    else plugin.gui.getRenderer((Player) sender).serverMenu(1, null, null);
                                    break;
                                case "server/":
                                case "subserver/":
                                    plugin.gui.getRenderer((Player) sender).serverAdmin(menuopts[0]);
                                    break;
                                case "server/plugin":
                                case "subserver/plugin":
                                    if (menuopts.length > 1) plugin.gui.getRenderer((Player) sender).serverPlugin(Integer.parseInt(menuopts[1]), menuopts[0]);
                                    else plugin.gui.getRenderer((Player) sender).serverPlugin(1, menuopts[0]);
                                    break;
                            }
                            return CommandResult.builder().successCount(1).build();
                        } catch (Throwable e) { /*
                            List<String> list = new LinkedList<String>();
                            list.addAll(Arrays.asList(menuopts));
                            list.remove(0);
                            new InvocationTargetException(e, "Could not render page with arguments: " + list.toString()).printStackTrace(); */
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
    private void selectServers(CommandSource sender, String[] selection, boolean mode, String permissions, Consumer<ServerSelection> callback) {
        selectServers(sender, selection, mode, new String[]{ permissions }, callback);
    }
    private void selectServers(CommandSource sender, String[] selection, boolean mode, String[] permissions, Consumer<ServerSelection> callback) {
        selectServers(sender, selection, mode, new String[][]{ permissions }, callback);
    }
    @SuppressWarnings("unchecked")
    private void selectServers(CommandSource sender, String[] selection, boolean mode, String[][] permissions, Consumer<ServerSelection> callback) {
        StackTraceElement[] origin = new Throwable().getStackTrace();
        LinkedList<Text> msgs = new LinkedList<Text>();
        LinkedList<Server> select = new LinkedList<Server>();

        // Step 3
        Runnable finished = () -> {
            LinkedList<Server> history = new LinkedList<Server>();
            LinkedList<Server> servers = new LinkedList<Server>();
            LinkedList<SubServer> subservers = new LinkedList<SubServer>();
            for (Server server : select) {
                if (!history.contains(server)) {
                    history.add(server);

                    boolean permitted = sender == null || permissions == null || permissions.length == 0;
                    if (!permitted) {
                        permitted = true;
                        for (int p = 0; permitted && p < permissions.length; ++p) {
                            if (permissions[p] == null || permissions[p].length == 0) continue;
                            else permitted = permits(server, sender, permissions[p]);
                        }
                    }


                    if (permitted) {
                        servers.add(server);
                        if (server instanceof SubServer)
                            subservers.add((SubServer) server);
                    } else {
                        Text msg = ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Generic.Invalid-Select-Permission").replace("$str$", server.getName()));
                        sender.sendMessage(msg);
                        msgs.add(msg);
                    }
                }
            }

            if ((!mode && servers.size() <= 0) || (mode && subservers.size() <= 0)) {
                Text msg = ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Generic.No-" + ((mode)?"Sub":"") + "Servers-Selected"));
                if (sender != null) sender.sendMessage(msg);
                msgs.add(msg);
            }

            try {
                callback.accept(new ServerSelection(msgs, selection, servers, subservers));
            } catch (Throwable e) {
                Throwable ew = new InvocationTargetException(e);
                ew.setStackTrace(origin);
                ew.printStackTrace();
            }
        };

        // Step 2
        Merger merge = new Merger(finished);
        for (Value<Integer> ic = new Container<Integer>(0); ic.value() < selection.length; ic.value(ic.value() + 1)) {
            String current = selection[ic.value()];

            if (current.length() > 0) {
                merge.reserve();

                if (current.startsWith("::") && current.length() > 2) {
                    current = current.substring(2);

                    if (current.equals(".")) {
                        plugin.api.getSubServer(plugin.api.getName(), self -> {
                            if (self != null) {
                                merge.reserve();
                                self.getHost(host -> {
                                    select.addAll(host.getSubServers().values());
                                    merge.release();
                                });
                            } else {
                                Text msg = ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Generic.Unknown-SubServer").replace("$str$", plugin.api.getName()));
                                if (sender != null) sender.sendMessage(msg);
                                msgs.add(msg);
                            }
                            merge.release();
                        });
                    } else if (current.equals("*")) {
                        plugin.api.getHosts(hostMap -> {
                            for (Host host : hostMap.values()) {
                                select.addAll(host.getSubServers().values());
                            }
                            merge.release();
                        });
                    } else {
                        final String fcurrent = current;
                        plugin.api.getHost(current, host -> {
                            if (host != null) {
                                if (!select.addAll(host.getSubServers().values())) {
                                    Text msg = ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Generic.No-" + ((mode)?"Sub":"") + "Servers-On-Host").replace("$str$", host.getName()));
                                    if (sender != null) sender.sendMessage(msg);
                                    msgs.add(msg);
                                }
                            } else {
                                Text msg = ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Generic.Unknown-Host").replace("$str$", fcurrent));
                                if (sender != null) sender.sendMessage(msg);
                                msgs.add(msg);
                            }
                            merge.release();
                        });
                    }
                } else if (current.startsWith(":") && current.length() > 1) {
                    current = current.substring(1);

                    if (current.equals(".")) {
                        plugin.api.getSubServer(plugin.api.getName(), self -> {
                            Merger merge2 = new Merger(merge::release);
                            for (String name : self.getGroups()) {
                                merge2.reserve();
                                plugin.api.getGroup(name, group -> {
                                    for (Server server : group.value()) {
                                        if (!mode || server instanceof SubServer) select.add(server);
                                    }
                                    merge2.release();
                                });
                            }
                        });
                    } else if (current.equals("*")) {
                        plugin.api.getGroups(groupMap -> {
                            for (List<Server> group : groupMap.values()) for (Server server : group) {
                                if (!mode || server instanceof SubServer) select.add(server);
                            }
                            merge.release();
                        });
                    } else {
                        final String fcurrent = current;
                        plugin.api.getGroup(current, group -> {
                            if (group != null) {
                                int i = 0;
                                for (Server server : group.value()) {
                                    if (!mode || server instanceof SubServer) {
                                        select.add(server);
                                        i++;
                                    }
                                }
                                if (i <= 0) {
                                    Text msg = ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Generic.No-" + ((mode)?"Sub":"") + "Servers-In-Group").replace("$str$", group.key()));
                                    if (sender != null) sender.sendMessage(msg);
                                    msgs.add(msg);
                                }
                            } else {
                                Text msg = ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Generic.Unknown-Group").replace("$str$", fcurrent));
                                if (sender != null) sender.sendMessage(msg);
                                msgs.add(msg);
                            }
                            merge.release();
                        });
                    }
                } else {

                    if (current.equals(".")) {
                        plugin.api.getServer(plugin.api.getName(), self -> {
                            if (!mode || self instanceof SubServer) select.add(self);
                            merge.release();
                        });
                    } else if (current.equals("*")) {
                        plugin.api.getServers(serverMap -> {
                            for (Server server : serverMap.values()) {
                                if (!mode || server instanceof SubServer) select.add(server);
                            }
                            merge.release();
                        });
                    } else {
                        final String fcurrent = current;
                        plugin.api.getServer(current, server -> {
                            if (server != null) {
                                select.add(server);
                            } else {
                                Text msg = ChatColor.convertColor(plugin.api.getLang("SubServers", "Command.Generic.Unknown-" + ((mode)?"Sub":"") + "Server").replace("$str$", fcurrent));
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
        private final Text[] msgs;
        private final String[] selection;
        private final Server[] servers;
        private final SubServer[] subservers;

        private ServerSelection(List<Text> msgs, String[] selection, List<Server> servers, List<SubServer> subservers) {
            this.msgs = msgs.toArray(new Text[0]);
            this.selection = selection;
            this.servers = servers.toArray(new Server[0]);
            this.subservers = subservers.toArray(new SubServer[0]);

            Arrays.sort(this.selection);
        }
    }

    private Text[] printHelp() {
        return new Text[]{
                ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Help.Header")),
                ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Help.Help").replace("$str$", "/sub help")),
                ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Help.List").replace("$str$", "/sub list")),
                ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Help.Version").replace("$str$", "/sub version")),
                ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Help.Info").replace("$str$", "/sub info [proxy|host|group|server|player] <Name>")),
                ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Help.SubServer.Start").replace("$str$", "/sub start <Subservers>")),
                ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Help.SubServer.Restart").replace("$str$", "/sub restart <Subservers>")),
                ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Help.SubServer.Stop").replace("$str$", "/sub stop <Subservers>")),
                ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Help.SubServer.Terminate").replace("$str$", "/sub kill <Subservers>")),
                ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Help.SubServer.Command").replace("$str$", "/sub cmd <Servers> <Command> [Args...]")),
                ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Help.Host.Create").replace("$str$", "/sub create <Name> <Host> <Template> [Version] [Port]")),
                ChatColor.convertColor(plugin.api.getLang("SubServers","Command.Help.SubServer.Update").replace("$str$", "/sub update <Subservers> [[Template] <Version>]")),
        };
    }
}
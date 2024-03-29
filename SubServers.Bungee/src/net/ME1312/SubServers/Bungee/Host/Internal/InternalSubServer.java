package net.ME1312.SubServers.Bungee.Host.Internal;

import net.ME1312.Galaxi.Library.Container.ContainedPair;
import net.ME1312.Galaxi.Library.Container.Container;
import net.ME1312.Galaxi.Library.Container.Value;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Map.ObjectMapValue;
import net.ME1312.Galaxi.Library.Try;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.SubData.Server.SubDataClient;
import net.ME1312.SubServers.Bungee.Event.*;
import net.ME1312.SubServers.Bungee.Host.*;
import net.ME1312.SubServers.Bungee.Library.Compatibility.Logger;
import net.ME1312.SubServers.Bungee.Library.Exception.InvalidServerException;
import net.ME1312.SubServers.Bungee.Network.Packet.PacketOutExEditServer;
import net.ME1312.SubServers.Bungee.Network.Packet.PacketOutExEditServer.Edit;
import net.ME1312.SubServers.Bungee.SubAPI;
import net.ME1312.SubServers.Bungee.SubProxy;

import net.md_5.bungee.api.ChatColor;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;

/**
 * Internal SubServer Class
 */
public class InternalSubServer extends SubServerImpl {
    private InternalHost host;
    private boolean enabled;
    private Value<Boolean> log;
    private String dir;
    private File directory;
    private String executable;
    private String stopcmd;
    private StopAction stopaction;
    private LinkedList<LoggedCommand> history;
    private Process process;
    private InternalSubLogger logger;
    private Thread thread;
    private BufferedWriter command;
    private boolean allowrestart;
    private boolean lock;

    /**
     * Creates an Internal SubServer
     *
     * @param host Host
     * @param name Name
     * @param enabled Enabled Status
     * @param port Port Number
     * @param motd MOTD
     * @param log Logging Status
     * @param directory Directory
     * @param executable Executable String
     * @param stopcmd Stop Command
     * @param hidden Hidden Status
     * @param restricted Restricted Status
     * @throws InvalidServerException
     */
    public static InternalSubServer construct(InternalHost host, String name, boolean enabled, int port, String motd, boolean log, String directory, String executable, String stopcmd, boolean hidden, boolean restricted) throws InvalidServerException {
        try {
            return new InternalSubServer(host, name, enabled, port, motd, log, directory, executable, stopcmd, hidden, restricted);
        } catch (NoSuchMethodError e) {
            return new InternalSubServer(host, name, enabled, (Integer) port, motd, log, directory, executable, stopcmd, hidden, restricted);
        }
    }

    /**
     * Super Method 2 (newest)
     * @see #construct(InternalHost, String, boolean, int, String, boolean, String, String, String, boolean, boolean) for method details
     */
    protected InternalSubServer(InternalHost host, String name, boolean enabled, int port, String motd, boolean log, String directory, String executable, String stopcmd, boolean hidden, boolean restricted) throws InvalidServerException {
        super(host, name, port, motd, hidden, restricted);
        init(host, name, enabled, port, motd, log, directory, executable, stopcmd, hidden, restricted);
    }

    /**
     * Super Method 1 (oldest)
     * @see #construct(InternalHost, String, boolean, int, String, boolean, String, String, String, boolean, boolean) for method details
     */
    protected InternalSubServer(InternalHost host, String name, boolean enabled, Integer port, String motd, boolean log, String directory, String executable, String stopcmd, boolean hidden, boolean restricted) throws InvalidServerException {
        super(host, name, port, motd, hidden, restricted);
        init(host, name, enabled, port, motd, log, directory, executable, stopcmd, hidden, restricted);
    }

    private void init(InternalHost host, String name, boolean enabled, int port, String motd, boolean log, String directory, String executable, String stopcmd, boolean hidden, boolean restricted) throws InvalidServerException {
        Util.nullpo(host, name, enabled, port, motd, log, directory, executable, stopcmd, hidden, restricted);
        this.host = host;
        this.enabled = enabled;
        this.log = new Container<Boolean>(log);
        this.dir = directory;
        this.directory = new File(host.getPath(), directory);
        this.executable = executable;
        this.stopcmd = stopcmd;
        this.stopaction = StopAction.NONE;
        this.history = new LinkedList<LoggedCommand>();
        this.process = null;
        this.logger = new InternalSubLogger(null, this, getName(), this.log, null);
        this.thread = null;
        this.command = null;
        final File[] locations = new File[] {
                new File(this.directory, "plugins/SubServers.Client.jar"),
                new File(this.directory, "mods/SubServers.Client.jar")
        };

        for (File location : locations) {
            if (location.exists()) {
                try {
                    JarInputStream updated = new JarInputStream(SubProxy.class.getResourceAsStream("/net/ME1312/SubServers/Bungee/Library/Files/client.jar"));
                    JarFile existing = new JarFile(location);

                    if (existing.getManifest().getMainAttributes().getValue("Implementation-Title") != null && existing.getManifest().getMainAttributes().getValue("Implementation-Title").startsWith("SubServers.Client") && existing.getManifest().getMainAttributes().getValue("Specification-Title") != null &&
                            updated.getManifest().getMainAttributes().getValue("Implementation-Title") != null && updated.getManifest().getMainAttributes().getValue("Implementation-Title").startsWith("SubServers.Client") && updated.getManifest().getMainAttributes().getValue("Specification-Title") != null) {
                        if (new Version(existing.getManifest().getMainAttributes().getValue("Specification-Title")).compareTo(new Version(updated.getManifest().getMainAttributes().getValue("Specification-Title"))) < 0) {
                            location.delete();
                            Util.copyFromJar(SubProxy.class.getClassLoader(), "net/ME1312/SubServers/Bungee/Library/Files/client.jar", location.getPath());
                        }
                    }
                    existing.close();
                    updated.close();
                } catch (Throwable e) {
                    System.out.println("Couldn't auto-update SubServers.Client for subserver: " + name);
                    e.printStackTrace();
                }
            }
        }

        this.lock = false;
    }

    void registered(boolean value) {
        registered = value;
    }

    void updating(boolean value) {
        updating = value;
    }

    private void run() {
        boolean locked = lock;
        allowrestart = true;
        stopping = false;
        started = false;
        try {
            ProcessBuilder pb = new ProcessBuilder().command(Executable.parse(host.getCreator().getBashDirectory(), executable)).directory(directory);
            pb.environment().put("java", System.getProperty("java.home") + File.separator + "bin" + File.separator + "java");
            pb.environment().put("name", getName());
            pb.environment().put("host", host.getName());
            pb.environment().put("address", host.getAddress().getHostAddress());
            pb.environment().put("port", Integer.toString(getAddress().getPort()));
            logger.init();
            process = pb.start();
            Logger.get("SubServers").info("Now starting " + getName());
            logger.process = process;
            logger.start();
            lock = locked = false;
            command = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
            for (LoggedCommand command : history) if (process.isAlive()) {
                this.command.write(command.getCommand());
                this.command.newLine();
                this.command.flush();
            }

            if (process.isAlive()) process.waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            if (locked) lock = false;
            allowrestart = false;
        }

        logger.destroy();
        Logger.get("SubServers").info(getName() + " has stopped");
        process = null;
        command = null;
        started = false;
        stopping = false;
        history.clear();

        SubStoppedEvent event = new SubStoppedEvent(this);
        host.plugin.getPluginManager().callEvent(event);

        if (stopaction == StopAction.REMOVE_SERVER || stopaction == StopAction.RECYCLE_SERVER || stopaction == StopAction.DELETE_SERVER) {
            try {
                if (stopaction == StopAction.RECYCLE_SERVER) {
                    host.recycleSubServer(null, getName(), false, false);
                } else if (stopaction == StopAction.DELETE_SERVER) {
                    host.deleteSubServer(null, getName(), false, false);
                } else {
                    try {
                        if (host.plugin.servers.get().getMap("Servers").getKeys().contains(getName())) {
                            host.plugin.servers.get().getMap("Servers").remove(getName());
                            host.plugin.servers.save();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    host.removeSubServer(getName());
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else if (stopaction == StopAction.RESTART) {
            if (allowrestart) {
                new Thread(() -> {
                    try {
                        while (thread != null && thread.isAlive()) {
                            Thread.sleep(250);
                        }
                        start();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }, "SubServers.Bungee::Internal_Server_Restart_Handler(" + getName() + ')').start();
            }
        }
    }

    @Override
    public boolean start(UUID player) {
        if (!lock && isAvailable() && isEnabled() && !(thread != null && thread.isAlive()) && getCurrentIncompatibilities().size() == 0) {
            lock = true;
            SubStartEvent event = new SubStartEvent(player, this);
            host.plugin.getPluginManager().callEvent(event);
            if (!event.isCancelled()) {
                (thread = new Thread(this::run, "SubServers.Bungee::Internal_Server_Process_Handler(" + getName() + ')')).start();
                return true;
            } else {
                lock = false;
                return false;
            }
        } else return false;
    }

    @Override
    public boolean stop(UUID player) {
        if (thread != null && thread.isAlive()) {
            SubStopEvent event = new SubStopEvent(player, this, false);
            host.plugin.getPluginManager().callEvent(event);
            if (!event.isCancelled()) {
                try {
                    stopping = true;
                    allowrestart = false;
                    history.add(new LoggedCommand(player, stopcmd));
                    if (process != null && process.isAlive()) {
                        command.write(stopcmd);
                        command.newLine();
                        command.flush();
                    }
                    return true;
                } catch (IOException e) {
                    e.printStackTrace();
                    return false;
                }
            } else return false;
        } else return false;
    }

    @Override
    public boolean terminate(UUID player) {
        if (thread != null && thread.isAlive()) {
            SubStopEvent event = new SubStopEvent(player, this, true);
            host.plugin.getPluginManager().callEvent(event);
            if (!event.isCancelled()) {
                stopping = true;
                allowrestart = false;
                if (process != null && process.isAlive()) Executable.terminate(process);
                return true;
            } else return false;
        } else return false;
    }

    @Override
    public boolean command(UUID player, String command) {
        Util.nullpo(command);
        if (thread != null && thread.isAlive()) {
            SubSendCommandEvent event = new SubSendCommandEvent(player, this, command, null);
            host.plugin.getPluginManager().callEvent(event);
            if (!event.isCancelled() && (player == null || !DISALLOWED_COMMANDS.matcher(command).find())) {
                try {
                    if (event.getCommand().equalsIgnoreCase(stopcmd)) {
                        stopping = true;
                        allowrestart = false;
                    }
                    history.add(new LoggedCommand(player, event.getCommand()));
                    if (process != null && process.isAlive()) {
                        this.command.write(event.getCommand());
                        this.command.newLine();
                        this.command.flush();
                    }
                    return true;
                } catch (IOException e) {
                    e.printStackTrace();
                    return false;
                }
            } else return false;
        } else return false;
    }

    @SuppressWarnings({"deprecation", "unchecked"})
    @Override
    protected int edit(UUID player, ObjectMap<String> edit, boolean perma) {
        if (isAvailable()) {
            int c = 0;
            boolean state = isRunning();
            SubServer forward = null;
            ObjectMap<String> pending = edit.clone();
            for (String key : edit.getKeys()) {
                pending.remove(key);
                ObjectMapValue value = edit.get(key);
                boolean allowed = true;
                if (perma) {
                    SubEditServerEvent event = new SubEditServerEvent(player, this, new ContainedPair<String, ObjectMapValue>(key, value));
                    host.plugin.getPluginManager().callEvent(event);
                    allowed = !event.isCancelled();
                }
                if (allowed) {
                    try {
                        switch (key.toLowerCase()) {
                            case "name":
                                if (value.isString() && host.removeSubServer(player, getName())) {
                                    SubServer server = host.constructSubServer(value.asString(), isEnabled(), getAddress().getPort(), getMotd(), isLogging(), getPath(), getExecutable(), getStopCommand(), isHidden(), isRestricted());
                                    if (server != null) {
                                        if (perma && this.host.plugin.servers.get().getMap("Servers").getKeys().contains(getName())) {
                                            ObjectMap<String> config = this.host.plugin.servers.get().getMap("Servers").getMap(getName());
                                            this.host.plugin.servers.get().getMap("Servers").remove(getName());
                                            this.host.plugin.servers.get().getMap("Servers").set(server.getName(), config);
                                            this.host.plugin.servers.save();
                                        }
                                        forward = server;
                                        c++;
                                    }
                                }
                                break;
                            case "display":
                                if (value.isString()) {
                                    setDisplayName(value.asString());
                                    logger.name = getDisplayName();
                                    if (perma && this.host.plugin.servers.get().getMap("Servers").getKeys().contains(getName())) {
                                        if (getName().equals(getDisplayName())) {
                                            this.host.plugin.servers.get().getMap("Servers").getMap(getName()).remove("Display");
                                        } else {
                                            this.host.plugin.servers.get().getMap("Servers").getMap(getName()).set("Display", getDisplayName());
                                        }
                                        this.host.plugin.servers.save();
                                    }
                                    c++;
                                }
                                break;
                            case "enabled":
                                if (value.isBoolean()) {
                                    enabled = value.asBoolean();
                                    if (perma && this.host.plugin.servers.get().getMap("Servers").getKeys().contains(getName())) {
                                        this.host.plugin.servers.get().getMap("Servers").getMap(getName()).set("Enabled", isEnabled());
                                        this.host.plugin.servers.save();
                                    }
                                    c++;
                                }
                                break;
                            case "group":
                                if (value.isList()) {
                                    Util.reflect(ServerImpl.class.getDeclaredField("groups"), this, value.asStringList());
                                    if (perma && this.host.plugin.servers.get().getMap("Servers").getKeys().contains(getName())) {
                                        this.host.plugin.servers.get().getMap("Servers").getMap(getName()).set("Group", value.asStringList());
                                        this.host.plugin.servers.save();
                                    }
                                    c++;
                                }
                                break;
                            case "host":
                                if (value.isString() && host.removeSubServer(player, getName())) {
                                    SubServer server = this.host.plugin.api.getHost(value.asString()).constructSubServer(getName(), isEnabled(), getAddress().getPort(), getMotd(), isLogging(), getPath(), getExecutable(), getStopCommand(), isHidden(), isRestricted());
                                    if (server != null) {
                                        if (perma && this.host.plugin.servers.get().getMap("Servers").getKeys().contains(getName())) {
                                            this.host.plugin.servers.get().getMap("Servers").getMap(getName()).set("Host", server.getHost().getName());
                                            this.host.plugin.servers.save();
                                        }
                                        forward = server;
                                        c++;
                                    }
                                }
                                break;
                            case "template":
                                if (value.isString()) {
                                    setTemplate(value.asString());
                                    if (perma && this.host.plugin.servers.get().getMap("Servers").getKeys().contains(getName())) {
                                        this.host.plugin.servers.get().getMap("Servers").getMap(getName()).set("Template", value.asString());
                                        this.host.plugin.servers.save();
                                    }
                                    c++;
                                }
                                break;
                            case "port":
                                if (value.isNumber() && host.removeSubServer(player, getName())) {
                                    SubServer server = host.constructSubServer(getName(), isEnabled(), value.asInt(), getMotd(), isLogging(), getPath(), getExecutable(), getStopCommand(), isHidden(), isRestricted());
                                    if (server != null) {
                                        if (perma && this.host.plugin.servers.get().getMap("Servers").getKeys().contains(getName())) {
                                            this.host.plugin.servers.get().getMap("Servers").getMap(getName()).set("Port", server.getAddress().getPort());
                                            this.host.plugin.servers.save();
                                        }
                                        forward = server;
                                        c++;
                                    }
                                }
                                break;
                            case "motd":
                                if (value.isString()) {
                                    setMotd(ChatColor.translateAlternateColorCodes('&', Util.unescapeJavaString(value.asString())));
                                    if (perma && this.host.plugin.servers.get().getMap("Servers").getKeys().contains(getName())) {
                                        this.host.plugin.servers.get().getMap("Servers").getMap(getName()).set("Motd", value.asString());
                                        this.host.plugin.servers.save();
                                    }
                                    c++;
                                }
                                break;
                            case "log":
                                if (value.isBoolean()) {
                                    log.value(value.asBoolean());
                                    if (perma && this.host.plugin.servers.get().getMap("Servers").getKeys().contains(getName())) {
                                        this.host.plugin.servers.get().getMap("Servers").getMap(getName()).set("Log", isLogging());
                                        this.host.plugin.servers.save();
                                    }
                                    c++;
                                }
                                break;
                            case "dir":
                            case "directory":
                                if (value.isString()) {
                                    if (isRunning()) {
                                        stop(player);
                                        waitFor();
                                    }
                                    dir = value.asString();
                                    directory = new File(getHost().getPath(), value.asString());
                                    if (perma && this.host.plugin.servers.get().getMap("Servers").getKeys().contains(getName())) {
                                        this.host.plugin.servers.get().getMap("Servers").getMap(getName()).set("Directory", getPath());
                                        this.host.plugin.servers.save();
                                    }
                                    c++;
                                }
                                break;
                            case "exec":
                            case "executable":
                                if (value.isString()) {
                                    if (isRunning()) {
                                        stop(player);
                                        waitFor();
                                    }
                                    executable = value.asString();
                                    if (perma && this.host.plugin.servers.get().getMap("Servers").getKeys().contains(getName())) {
                                        this.host.plugin.servers.get().getMap("Servers").getMap(getName()).set("Executable", value.asString());
                                        this.host.plugin.servers.save();
                                    }
                                    c++;
                                }
                                break;
                            case "stop-cmd":
                            case "stop-command":
                                if (value.isString()) {
                                    stopcmd = value.asString();
                                    if (perma && this.host.plugin.servers.get().getMap("Servers").getKeys().contains(getName())) {
                                        this.host.plugin.servers.get().getMap("Servers").getMap(getName()).set("Stop-Command", getStopCommand());
                                        this.host.plugin.servers.save();
                                    }
                                    c++;
                                }
                                break;
                            case "stop-action":
                                if (value.isString()) {
                                    StopAction action = Try.all.get(() -> StopAction.valueOf(value.asString().toUpperCase().replace('-', '_').replace(' ', '_')));
                                    if (action != null) {
                                        stopaction = action;
                                        if (perma && this.host.plugin.servers.get().getMap("Servers").getKeys().contains(getName())) {
                                            this.host.plugin.servers.get().getMap("Servers").getMap(getName()).set("Stop-Action", getStopAction().toString());
                                            this.host.plugin.servers.save();
                                        }
                                        c++;
                                    }
                                }
                                break;
                            case "state":
                                if (value.isBoolean()) {
                                    state = value.asBoolean();
                                }
                                break;
                            case "auto-run":
                            case "run-on-launch":
                                if (value.isBoolean()) {
                                    if (perma && this.host.plugin.servers.get().getMap("Servers").getKeys().contains(getName())) {
                                        this.host.plugin.servers.get().getMap("Servers").getMap(getName()).set("Run-On-Launch", value.asBoolean());
                                        this.host.plugin.servers.save();
                                    }
                                    c++;
                                }
                                break;
                            case "incompatible":
                                if (value.isList()) {
                                    for (SubServer oserver : getIncompatibilities()) toggleCompatibility(oserver);
                                    for (String oname : (List<String>) value.asStringList()) {
                                        SubServer oserver = host.plugin.api.getSubServer(oname);
                                        if (oserver != null && isCompatible(oserver)) toggleCompatibility(oserver);
                                    }
                                    if (perma && this.host.plugin.servers.get().getMap("Servers").getKeys().contains(getName())) {
                                        this.host.plugin.servers.get().getMap("Servers").getMap(getName()).set("Incompatible", value.asStringList());
                                        this.host.plugin.servers.save();
                                    }
                                    c++;
                                }
                                break;
                            case "restricted":
                                if (value.isBoolean()) {
                                    setRestricted(value.asBoolean());
                                    if (perma && this.host.plugin.servers.get().getMap("Servers").getKeys().contains(getName())) {
                                        this.host.plugin.servers.get().getMap("Servers").getMap(getName()).set("Restricted", isRestricted());
                                        this.host.plugin.servers.save();
                                    }
                                    c++;
                                }
                                break;
                            case "hidden":
                                if (value.isBoolean()) {
                                    setHidden(value.asBoolean());
                                    if (perma && this.host.plugin.servers.get().getMap("Servers").getKeys().contains(getName())) {
                                        this.host.plugin.servers.get().getMap("Servers").getMap(getName()).set("Hidden", isHidden());
                                        this.host.plugin.servers.save();
                                    }
                                    c++;
                                }
                                break;
                            case "whitelist":
                                if (value.isList()) {
                                    Util.reflect(ServerImpl.class.getDeclaredField("whitelist"), this, value.asUUIDList());
                                    if (isRegistered()) for (Proxy proxy : SubAPI.getInstance().getProxies().values()) if (proxy.getSubData()[0] != null) {
                                        ((SubDataClient) proxy.getSubData()[0]).sendPacket(new PacketOutExEditServer(this, Edit.WHITELIST_SET, value.asUUIDList()));
                                    }
                                    c++;
                                }
                                break;
                        }
                        if (forward != null) {
                            forward.setStopAction(getStopAction());
                            if (!getName().equals(getDisplayName())) forward.setDisplayName(getDisplayName());
                            forward.setTemplate(getTemplate());
                            List<String> groups = new ArrayList<String>();
                            groups.addAll(getGroups());
                            for (String group : groups) {
                                removeGroup(group);
                                forward.addGroup(group);
                            }
                            for (SubServer server : getIncompatibilities()) {
                                toggleCompatibility(server);
                                forward.toggleCompatibility(server);
                            }
                            for (String extra : getExtra().getKeys()) forward.addExtra(extra, getExtra(extra));
                            forward.getHost().addSubServer(player, forward);

                            if (state) pending.set("state", true);
                            c += (perma)?forward.permaEdit(player, pending):forward.edit(player, pending);
                            break;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            if (!isRunning() && forward == null && state) start(player);
            return c;
        } else return -1;
    }

    @Override
    public void waitFor() throws InterruptedException {
        while (thread != null && thread.isAlive()) {
            Thread.sleep(250);
        }
    }

    @Override
    public boolean isRunning() {
        return (process != null && process.isAlive()) || lock;
    }

    @Override
    public void setDisplayName(String value) {
        super.setDisplayName(value);
        logger.name = getDisplayName();
    }

    @Override
    public Host getHost() {
        return host;
    }

    @Override
    public boolean isEnabled() {
        return enabled && host.isEnabled();
    }

    @Override
    public void setEnabled(boolean value) {
        Util.nullpo(value);
        enabled = value;
    }

    @Override
    public boolean isLogging() {
        return log.value();
    }

    @Override
    public void setLogging(boolean value) {
        Util.nullpo(value);
        log.value(value);
    }

    @Override
    public SubLogger getLogger() {
        return logger;
    }

    @Override
    public LinkedList<LoggedCommand> getCommandHistory() {
        return new LinkedList<LoggedCommand>(history);
    }

    @Override
    public String getPath() {
        return dir;
    }

    @Override
    public String getExecutable() {
        return executable;
    }

    @Override
    public String getStopCommand() {
        return stopcmd;
    }

    @Override
    public void setStopCommand(String value) {
        Util.nullpo(value);
        stopcmd = value;
    }

    @Override
    public StopAction getStopAction() {
        return stopaction;
    }

    @Override
    public void setStopAction(StopAction action) {
        Util.nullpo(action);
        stopaction = action;
    }
}

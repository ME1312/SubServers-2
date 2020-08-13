package net.ME1312.SubServers.Bungee.Host.Internal;

import net.ME1312.SubServers.Bungee.Event.*;
import net.ME1312.SubServers.Bungee.Host.*;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Map.ObjectMapValue;
import net.ME1312.Galaxi.Library.Container.Container;
import net.ME1312.SubServers.Bungee.Library.Compatibility.Logger;
import net.ME1312.SubServers.Bungee.Library.Exception.InvalidServerException;
import net.ME1312.Galaxi.Library.Container.NamedContainer;
import net.ME1312.Galaxi.Library.UniversalFile;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.SubServers.Bungee.SubProxy;
import net.md_5.bungee.BungeeServerInfo;
import net.md_5.bungee.api.ChatColor;

import java.io.*;
import java.lang.reflect.Field;
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
    private Container<Boolean> log;
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
        if (Util.isNull(host, name, enabled, port, motd, log, directory, executable, stopcmd, hidden, restricted)) throw new NullPointerException();
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

        if (new UniversalFile(this.directory, "plugins:SubServers.Client.jar").exists()) {
            try {
                JarInputStream updated = new JarInputStream(SubProxy.class.getResourceAsStream("/net/ME1312/SubServers/Bungee/Library/Files/client.jar"));
                JarFile existing = new JarFile(new UniversalFile(this.directory, "plugins:SubServers.Client.jar"));

                if (existing.getManifest().getMainAttributes().getValue("Implementation-Title") != null && existing.getManifest().getMainAttributes().getValue("Implementation-Title").startsWith("SubServers.Client") && existing.getManifest().getMainAttributes().getValue("Specification-Title") != null &&
                        updated.getManifest().getMainAttributes().getValue("Implementation-Title") != null && updated.getManifest().getMainAttributes().getValue("Implementation-Title").startsWith("SubServers.Client") && updated.getManifest().getMainAttributes().getValue("Specification-Title") != null) {
                    if (new Version(existing.getManifest().getMainAttributes().getValue("Specification-Title")).compareTo(new Version(updated.getManifest().getMainAttributes().getValue("Specification-Title"))) < 0) {
                        new UniversalFile(this.directory, "plugins:SubServers.Client.jar").delete();
                        Util.copyFromJar(SubProxy.class.getClassLoader(), "net/ME1312/SubServers/Bungee/Library/Files/client.jar", new UniversalFile(this.directory, "plugins:SubServers.Client.jar").getPath());
                    }
                }
                existing.close();
                updated.close();
            } catch (Throwable e) {
                System.out.println("Couldn't auto-update SubServers.Client.jar for " + name);
                e.printStackTrace();
            }
        } else if (new UniversalFile(this.directory, "mods:SubServers.Client.jar").exists()) {
            try {
                JarInputStream updated = new JarInputStream(SubProxy.class.getResourceAsStream("/net/ME1312/SubServers/Bungee/Library/Files/client.jar"));
                JarFile existing = new JarFile(new UniversalFile(this.directory, "mods:SubServers.Client.jar"));

                if (existing.getManifest().getMainAttributes().getValue("Implementation-Title") != null && existing.getManifest().getMainAttributes().getValue("Implementation-Title").startsWith("SubServers.Client") && existing.getManifest().getMainAttributes().getValue("Specification-Title") != null &&
                        updated.getManifest().getMainAttributes().getValue("Implementation-Title") != null && updated.getManifest().getMainAttributes().getValue("Implementation-Title").startsWith("SubServers.Client") && updated.getManifest().getMainAttributes().getValue("Specification-Title") != null) {
                    if (new Version(existing.getManifest().getMainAttributes().getValue("Specification-Title")).compareTo(new Version(updated.getManifest().getMainAttributes().getValue("Specification-Title"))) < 0) {
                        new UniversalFile(this.directory, "mods:SubServers.Client.jar").delete();
                        Util.copyFromJar(SubProxy.class.getClassLoader(), "net/ME1312/SubServers/Bungee/Library/Files/client.jar", new UniversalFile(this.directory, "mods:SubServers.Client.jar").getPath());
                    }
                }
                existing.close();
                updated.close();
            } catch (Throwable e) {
                System.out.println("Couldn't auto-update SubServers.Client.jar for " + name);
                e.printStackTrace();
            }
        }
        this.lock = false;
    }

    private void run() {
        allowrestart = true;
        started = false;
        try {
            ProcessBuilder pb = new ProcessBuilder().command(Executable.parse(host.getCreator().getBashDirectory(), executable)).directory(directory);
            pb.environment().put("java", System.getProperty("java.home") + File.separator + "bin" + File.separator + "java");
            pb.environment().put("name", getName());
            pb.environment().put("host", host.getName());
            pb.environment().put("address", host.getAddress().getHostAddress());
            pb.environment().put("port", Integer.toString(getAddress().getPort()));
            process = pb.start();
            Logger.get("SubServers").info("Now starting " + getName());
            logger.process = process;
            logger.start();
            command = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
            for (LoggedCommand command : history) if (process.isAlive()) {
                this.command.write(command.getCommand());
                this.command.newLine();
                this.command.flush();
            }

            if (process.isAlive()) process.waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            allowrestart = false;
        }

        Logger.get("SubServers").info(getName() + " has stopped");
        process = null;
        command = null;
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
            lock = false;
            if (!event.isCancelled()) {
                (thread = new Thread(this::run, "SubServers.Bungee::Internal_Server_Process_Handler(" + getName() + ')')).start();
                return true;
            } else return false;
        } else return false;
    }

    @Override
    public boolean stop(UUID player) {
        if (thread != null && thread.isAlive()) {
            SubStopEvent event = new SubStopEvent(player, this, false);
            host.plugin.getPluginManager().callEvent(event);
            if (!event.isCancelled()) {
                try {
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
                allowrestart = false;
                if (process != null && process.isAlive()) Executable.terminate(process);
                return true;
            } else return false;
        } else return false;
    }

    @Override
    public boolean command(UUID player, String command) {
        if (Util.isNull(command)) throw new NullPointerException();
        if (thread != null && thread.isAlive()) {
            SubSendCommandEvent event = new SubSendCommandEvent(player, this, command);
            host.plugin.getPluginManager().callEvent(event);
            if (!event.isCancelled()) {
                try {
                    if (event.getCommand().equalsIgnoreCase(stopcmd)) allowrestart = false;
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

    public int edit(UUID player, ObjectMap<String> edit) {
        return edit(player, edit, false);
    }

    public int permaEdit(UUID player, ObjectMap<String> edit) {
        return edit(player, edit, true);
    }

    @SuppressWarnings({"deprecation", "unchecked"})
    private int edit(UUID player, ObjectMap<String> edit, boolean perma) {
        if (isAvailable()) {
            int c = 0;
            boolean state = isRunning();
            SubServer forward = null;
            ObjectMap<String> pending = edit.clone();
            for (String key : edit.getKeys()) {
                pending.remove(key);
                ObjectMapValue value = edit.get(key);
                SubEditServerEvent event = new SubEditServerEvent(player, this, new NamedContainer<String, ObjectMapValue>(key, value), perma);
                host.plugin.getPluginManager().callEvent(event);
                if (!event.isCancelled()) {
                    try {
                        switch (key.toLowerCase()) {
                            case "name":
                                if (value.isString() && host.removeSubServer(player, getName())) {
                                    SubServer server = host.addSubServer(player, value.asRawString(), isEnabled(), getAddress().getPort(), getMotd(), isLogging(), getPath(), getExecutable(), getStopCommand(), isHidden(), isRestricted());
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
                                    Field f = ServerImpl.class.getDeclaredField("nick");
                                    f.setAccessible(true);
                                    if (value.isNull() || value.asString().length() == 0 || getName().equals(value.asString())) {
                                        f.set(this, null);
                                    } else {
                                        f.set(this, value.asString());
                                    }
                                    f.setAccessible(false);
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
                                    Util.reflect(ServerImpl.class.getDeclaredField("groups"), this, value.asRawStringList());
                                    if (perma && this.host.plugin.servers.get().getMap("Servers").getKeys().contains(getName())) {
                                        this.host.plugin.servers.get().getMap("Servers").getMap(getName()).set("Group", value.asRawStringList());
                                        this.host.plugin.servers.save();
                                    }
                                    c++;
                                }
                                break;
                            case "host":
                                if (value.isString() && host.removeSubServer(player, getName())) {
                                    SubServer server = this.host.plugin.api.getHost(value.asRawString()).addSubServer(player, getName(), isEnabled(), getAddress().getPort(), getMotd(), isLogging(), getPath(), getExecutable(), getStopCommand(), isHidden(), isRestricted());
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
                                    Util.reflect(SubServerImpl.class.getDeclaredField("template"), this, value.asRawString());
                                    if (perma && this.host.plugin.servers.get().getMap("Servers").getKeys().contains(getName())) {
                                        this.host.plugin.servers.get().getMap("Servers").getMap(getName()).set("Template", value.asRawString());
                                        this.host.plugin.servers.save();
                                    }
                                    c++;
                                }
                                break;
                            case "port":
                                if (value.isNumber() && host.removeSubServer(player, getName())) {
                                    SubServer server = host.addSubServer(player, getName(), isEnabled(), value.asInt(), getMotd(), isLogging(), getPath(), getExecutable(), getStopCommand(), isHidden(), isRestricted());
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
                                    Util.reflect(BungeeServerInfo.class.getDeclaredField("motd"), this, ChatColor.translateAlternateColorCodes('&', value.asString()));
                                    if (perma && this.host.plugin.servers.get().getMap("Servers").getKeys().contains(getName())) {
                                        this.host.plugin.servers.get().getMap("Servers").getMap(getName()).set("Motd", value.asString());
                                        this.host.plugin.servers.save();
                                    }
                                    c++;
                                }
                                break;
                            case "log":
                                if (value.isBoolean()) {
                                    log.set(value.asBoolean());
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
                                    dir = value.asRawString();
                                    directory = new File(getHost().getPath(), value.asRawString());
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
                                    executable = value.asRawString();
                                    if (perma && this.host.plugin.servers.get().getMap("Servers").getKeys().contains(getName())) {
                                        this.host.plugin.servers.get().getMap("Servers").getMap(getName()).set("Executable", value.asRawString());
                                        this.host.plugin.servers.save();
                                    }
                                    c++;
                                }
                                break;
                            case "stop-cmd":
                            case "stop-command":
                                if (value.isString()) {
                                    stopcmd = value.asRawString();
                                    if (perma && this.host.plugin.servers.get().getMap("Servers").getKeys().contains(getName())) {
                                        this.host.plugin.servers.get().getMap("Servers").getMap(getName()).set("Stop-Command", getStopCommand());
                                        this.host.plugin.servers.save();
                                    }
                                    c++;
                                }
                                break;
                            case "stop-action":
                                if (value.isString()) {
                                    StopAction action = Util.getDespiteException(() -> StopAction.valueOf(value.asRawString().toUpperCase().replace('-', '_').replace(' ', '_')), null);
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
                                    Util.reflect(BungeeServerInfo.class.getDeclaredField("restricted"), this, value.asBoolean());
                                    if (perma && this.host.plugin.servers.get().getMap("Servers").getKeys().contains(getName())) {
                                        this.host.plugin.servers.get().getMap("Servers").getMap(getName()).set("Restricted", isRestricted());
                                        this.host.plugin.servers.save();
                                    }
                                    c++;
                                }
                                break;
                            case "hidden":
                                if (value.isBoolean()) {
                                    Util.reflect(ServerImpl.class.getDeclaredField("hidden"), this, value.asBoolean());
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
                                    c++;
                                }
                                break;
                        }
                        if (forward != null) {
                            forward.setStopAction(getStopAction());
                            if (!getName().equals(getDisplayName())) forward.setDisplayName(getDisplayName());
                            Util.reflect(SubServerImpl.class.getDeclaredField("template"), forward, Util.reflect(SubServerImpl.class.getDeclaredField("template"), this));
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
        return process != null && process.isAlive();
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
        if (Util.isNull(value)) throw new NullPointerException();
        host.plugin.getPluginManager().callEvent(new SubEditServerEvent(null, this, new NamedContainer<String, Object>("enabled", value), false));
        enabled = value;
    }

    @Override
    public boolean isLogging() {
        return log.get();
    }

    @Override
    public void setLogging(boolean value) {
        if (Util.isNull(value)) throw new NullPointerException();
        host.plugin.getPluginManager().callEvent(new SubEditServerEvent(null, this, new NamedContainer<String, Object>("log", value), false));
        log.set(value);
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
        if (Util.isNull(value)) throw new NullPointerException();
        host.plugin.getPluginManager().callEvent(new SubEditServerEvent(null, this, new NamedContainer<String, Object>("stop-cmd", value), false));
        stopcmd = value;
    }

    @Override
    public StopAction getStopAction() {
        return stopaction;
    }

    @Override
    public void setStopAction(StopAction action) {
        if (Util.isNull(action)) throw new NullPointerException();
        host.plugin.getPluginManager().callEvent(new SubEditServerEvent(null, this, new NamedContainer<String, Object>("stop-action", action), false));
        stopaction = action;
    }
}

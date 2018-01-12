package net.ME1312.SubServers.Bungee.Host.Internal;

import net.ME1312.SubServers.Bungee.Event.*;
import net.ME1312.SubServers.Bungee.Host.*;
import net.ME1312.SubServers.Bungee.Library.Config.YAMLSection;
import net.ME1312.SubServers.Bungee.Library.Config.YAMLValue;
import net.ME1312.SubServers.Bungee.Library.Container;
import net.ME1312.SubServers.Bungee.Library.Exception.InvalidServerException;
import net.ME1312.SubServers.Bungee.Library.NamedContainer;
import net.ME1312.SubServers.Bungee.Library.UniversalFile;
import net.ME1312.SubServers.Bungee.Library.Util;
import net.ME1312.SubServers.Bungee.Library.Version.Version;
import net.ME1312.SubServers.Bungee.SubAPI;
import net.ME1312.SubServers.Bungee.SubPlugin;
import net.md_5.bungee.BungeeServerInfo;
import org.json.JSONObject;

import java.io.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.jar.JarFile;

/**
 * Internal SubServer Class
 */
public class InternalSubServer extends SubServerContainer {
    private InternalHost host;
    private boolean enabled;
    private boolean editable;
    private Container<Boolean> log;
    private String dir;
    private File directory;
    private Executable executable;
    private String stopcmd;
    private LinkedList<LoggedCommand> history;
    private Process process;
    private InternalSubLogger logger;
    private Thread thread;
    private BufferedWriter command;
    private boolean restart;
    private boolean allowrestart;
    private boolean temporary;

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
     * @param start Auto-Start
     * @param restart Auto-Restart
     * @param hidden Hidden Status
     * @param restricted Restricted Status
     * @param temporary Temporary Status
     * @throws InvalidServerException
     */
    public InternalSubServer(InternalHost host, String name, boolean enabled, int port, String motd, boolean log, String directory, Executable executable, String stopcmd, boolean start, boolean restart, boolean hidden, boolean restricted, boolean temporary) throws InvalidServerException {
        super(host, name, port, motd, hidden, restricted);
        if (Util.isNull(host, name, enabled, port, motd, log, directory, executable, stopcmd, start, restart, hidden, restricted, temporary)) throw new NullPointerException();
        this.host = host;
        this.enabled = enabled;
        this.editable = false;
        this.log = new Container<Boolean>(log);
        this.dir = directory;
        this.directory = new File(host.getPath(), directory);
        this.executable = executable;
        this.stopcmd = stopcmd;
        this.history = new LinkedList<LoggedCommand>();
        this.process = null;
        this.logger = new InternalSubLogger(null, this, getName(), this.log, null);
        this.thread = null;
        this.command = null;
        this.restart = restart;

        if (new UniversalFile(this.directory, "plugins:SubServers.Client.jar").exists()) {
            try {
                JarFile jar = new JarFile(new UniversalFile(this.directory, "plugins:SubServers.Client.jar"));
                YAMLSection plugin = new YAMLSection(Util.readAll(new InputStreamReader(jar.getInputStream(jar.getJarEntry("plugin.yml")))));
                YAMLSection bplugin = new YAMLSection(Util.readAll(new InputStreamReader(SubPlugin.class.getResourceAsStream("/net/ME1312/SubServers/Bungee/Library/Files/Client/spigot.yml"))));
                if (new Version(plugin.getString("version")).compareTo(new Version(bplugin.getString("version"))) < 0) {
                    new UniversalFile(this.directory, "plugins:SubServers.Client.jar").delete();
                    Util.copyFromJar(SubPlugin.class.getClassLoader(), "net/ME1312/SubServers/Bungee/Library/Files/Client/spigot.jar", new UniversalFile(this.directory, "plugins:SubServers.Client.jar").getPath());
                }
            } catch (Throwable e) {
                System.out.println("Couldn't auto-update SubServers.Client.jar");
                e.printStackTrace();
            }
        }
        this.temporary = !((start || temporary) && !start()) && temporary;
    }

    private void run() {
        allowrestart = true;
        try {
            process = Runtime.getRuntime().exec(executable.toString(), null, directory);
            System.out.println("SubServers > Now starting " + getName());
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

        SubStoppedEvent event = new SubStoppedEvent(this);
        host.plugin.getPluginManager().callEvent(event);
        System.out.println("SubServers > " + getName() + " has stopped");
        process = null;
        command = null;
        history.clear();

        if (isTemporary()) {
            try {
                host.removeSubServer(getName());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            if (willAutoRestart() && allowrestart) {
                new Thread(() -> {
                    try {
                        while (thread != null && thread.isAlive()) {
                            Thread.sleep(250);
                        }
                        start();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();
            }
        }
    }

    @Override
    public boolean start(UUID player) {
        if (isEnabled() && !(thread != null && thread.isAlive()) && getCurrentIncompatibilities().size() == 0) {
            SubStartEvent event = new SubStartEvent(player, this);
            host.plugin.getPluginManager().callEvent(event);
            if (!event.isCancelled()) {
                (thread = new Thread(this::run)).start();
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
                if (process != null && process.isAlive()) process.destroyForcibly();
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

    @SuppressWarnings("deprecation")
    public int edit(UUID player, YAMLSection edit) {
        int c = 0;
        boolean state = isRunning();
        SubServer forward = null;
        YAMLSection pending = edit.clone();
        if (editable) for (String key : edit.getKeys()) {
            pending.remove(key);
            YAMLValue value = edit.get(key);
            SubEditServerEvent event = new SubEditServerEvent(player, this, new NamedContainer<String, YAMLValue>(key, value), true);
            host.plugin.getPluginManager().callEvent(event);
            if (!event.isCancelled()) {
                try {
                    switch (key) {
                        case "name":
                            if (value.isString() && host.removeSubServer(player, getName())) {
                                SubServer server = host.addSubServer(player, value.asRawString(), isEnabled(), getAddress().getPort(), getMotd(), isLogging(), getPath(), getExecutable(), getStopCommand(), false, willAutoRestart(), isHidden(), isRestricted(), isTemporary());
                                if (server != null) {
                                    if (this.host.plugin.config.get().getSection("Servers").getKeys().contains(getName())) {
                                        YAMLSection config = this.host.plugin.config.get().getSection("Servers").getSection(getName());
                                        this.host.plugin.config.get().getSection("Servers").remove(getName());
                                        this.host.plugin.config.get().getSection("Servers").set(server.getName(), config);
                                        this.host.plugin.config.save();
                                    }
                                    forward = server;
                                    c++;
                                }
                            }
                            break;
                        case "display":
                            if (value.isString()) {
                                Field f = ServerContainer.class.getDeclaredField("nick");
                                f.setAccessible(true);
                                if (value == null || value.asString().length() == 0 || getName().equals(value)) {
                                    f.set(this, null);
                                } else {
                                    f.set(this, value.asString());
                                }
                                f.setAccessible(false);
                                logger.name = getDisplayName();
                                if (this.host.plugin.config.get().getSection("Servers").getKeys().contains(getName())) {
                                    if (getName().equals(getDisplayName())) {
                                        this.host.plugin.config.get().getSection("Servers").getSection(getName()).remove("Display");
                                    } else {
                                        this.host.plugin.config.get().getSection("Servers").getSection(getName()).set("Display", getDisplayName());
                                    }
                                    this.host.plugin.config.save();
                                }
                                c++;
                            }
                            break;
                        case "enabled":
                            if (value.isBoolean()) {
                                enabled = value.asBoolean();
                                if (this.host.plugin.config.get().getSection("Servers").getKeys().contains(getName())) {
                                    this.host.plugin.config.get().getSection("Servers").getSection(getName()).set("Enabled", isEnabled());
                                    this.host.plugin.config.save();
                                }
                                c++;
                            }
                            break;
                        case "group":
                            if (value.isList()) {
                                Field f = ServerContainer.class.getDeclaredField("groups");
                                f.setAccessible(true);
                                f.set(this, value.asStringList());
                                f.setAccessible(false);
                                if (this.host.plugin.config.get().getSection("Servers").getKeys().contains(getName())) {
                                    this.host.plugin.config.get().getSection("Servers").getSection(getName()).set("Group", value.asStringList());
                                    this.host.plugin.config.save();
                                }
                                c++;
                            }
                            break;
                        case "host":
                            if (value.isString() && host.removeSubServer(player, getName())) {
                                SubServer server = this.host.plugin.api.getHost(value.asRawString()).addSubServer(player, getName(), isEnabled(), getAddress().getPort(), getMotd(), isLogging(), getPath(), getExecutable(), getStopCommand(), false, willAutoRestart(), isHidden(), isRestricted(), isTemporary());
                                if (server != null) {
                                    if (this.host.plugin.config.get().getSection("Servers").getKeys().contains(getName())) {
                                        this.host.plugin.config.get().getSection("Servers").getSection(getName()).set("Host", server.getHost().getName());
                                        this.host.plugin.config.save();
                                    }
                                    forward = server;
                                    c++;
                                }
                            }
                            break;
                        case "port":
                            if (value.isNumber() && host.removeSubServer(player, getName())) {
                                SubServer server = host.addSubServer(player, getName(), isEnabled(), value.asInt(), getMotd(), isLogging(), getPath(), getExecutable(), getStopCommand(), false, willAutoRestart(), isHidden(), isRestricted(), isTemporary());
                                if (server != null) {
                                    if (this.host.plugin.config.get().getSection("Servers").getKeys().contains(getName())) {
                                        this.host.plugin.config.get().getSection("Servers").getSection(getName()).set("Port", server.getAddress().getPort());
                                        this.host.plugin.config.save();
                                    }
                                    forward = server;
                                    c++;
                                }
                            }
                            break;
                        case "motd":
                            if (value.isString()) {
                                Field f = BungeeServerInfo.class.getDeclaredField("motd");
                                f.setAccessible(true);
                                f.set(this, value.asColoredString('&'));
                                f.setAccessible(false);
                                if (this.host.plugin.config.get().getSection("Servers").getKeys().contains(getName())) {
                                    this.host.plugin.config.get().getSection("Servers").getSection(getName()).set("Motd", value.asString());
                                    this.host.plugin.config.save();
                                }
                                c++;
                            }
                            break;
                        case "log":
                            if (value.isBoolean()) {
                                log.set(value.asBoolean());
                                if (this.host.plugin.config.get().getSection("Servers").getKeys().contains(getName())) {
                                    this.host.plugin.config.get().getSection("Servers").getSection(getName()).set("Log", isLogging());
                                    this.host.plugin.config.save();
                                }
                                c++;
                            }
                            break;
                        case "dir":
                            if (value.isString()) {
                                if (isRunning()) {
                                    stop(player);
                                    waitFor();
                                }
                                dir = value.asRawString();
                                directory = new File(getHost().getPath(), value.asRawString());
                                if (this.host.plugin.config.get().getSection("Servers").getKeys().contains(getName())) {
                                    this.host.plugin.config.get().getSection("Servers").getSection(getName()).set("Directory", getPath());
                                    this.host.plugin.config.save();
                                }
                                c++;
                            }
                            break;
                        case "exec":
                            if (value.isString()) {
                                if (isRunning()) {
                                    stop(player);
                                    waitFor();
                                }
                                executable = new Executable(value.asRawString());
                                if (this.host.plugin.config.get().getSection("Servers").getKeys().contains(getName())) {
                                    this.host.plugin.config.get().getSection("Servers").getSection(getName()).set("Executable", value.asRawString());
                                    this.host.plugin.config.save();
                                }
                                c++;
                            }
                            break;
                        case "stop-cmd":
                            if (value.isString()) {
                                stopcmd = value.asRawString();
                                if (this.host.plugin.config.get().getSection("Servers").getKeys().contains(getName())) {
                                    this.host.plugin.config.get().getSection("Servers").getSection(getName()).set("Stop-Command", getStopCommand());
                                    this.host.plugin.config.save();
                                }
                                c++;
                            }
                            break;
                        case "state":
                            if (value.isBoolean()) {
                                state = value.asBoolean();
                            }
                            break;
                        case "auto-run":
                            if (value.isBoolean()) {
                                if (this.host.plugin.config.get().getSection("Servers").getKeys().contains(getName())) {
                                    this.host.plugin.config.get().getSection("Servers").getSection(getName()).set("Run-On-Launch", value.asBoolean());
                                    this.host.plugin.config.save();
                                }
                                c++;
                            }
                            break;
                        case "auto-restart":
                            if (value.isBoolean()) {
                                restart = value.asBoolean();
                                if (this.host.plugin.config.get().getSection("Servers").getKeys().contains(getName())) {
                                    this.host.plugin.config.get().getSection("Servers").getSection(getName()).set("Auto-Restart", willAutoRestart());
                                    this.host.plugin.config.save();
                                }
                                c++;
                            }
                            break;
                        case "incompatible":
                            if (value.isList()) {
                                for (String oname : value.asStringList()) {
                                    SubServer oserver = host.plugin.api.getSubServer(oname);
                                    if (oserver != null && isCompatible(oserver)) toggleCompatibility(oserver);
                                }
                                if (this.host.plugin.config.get().getSection("Servers").getKeys().contains(getName())) {
                                    this.host.plugin.config.get().getSection("Servers").getSection(getName()).set("Incompatible", value.asStringList());
                                    this.host.plugin.config.save();
                                }
                                c++;
                            }
                            break;
                        case "restricted":
                            if (value.isBoolean()) {
                                Field f = BungeeServerInfo.class.getDeclaredField("restricted");
                                f.setAccessible(true);
                                f.set(this, value.asBoolean());
                                f.setAccessible(false);
                                if (this.host.plugin.config.get().getSection("Servers").getKeys().contains(getName())) {
                                    this.host.plugin.config.get().getSection("Servers").getSection(getName()).set("Restricted", isRestricted());
                                    this.host.plugin.config.save();
                                }
                                c++;
                            }
                            break;
                        case "hidden":
                            if (value.isBoolean()) {
                                Field f = ServerContainer.class.getDeclaredField("hidden");
                                f.setAccessible(true);
                                f.set(this, value.asBoolean());
                                f.setAccessible(false);
                                if (this.host.plugin.config.get().getSection("Servers").getKeys().contains(getName())) {
                                    this.host.plugin.config.get().getSection("Servers").getSection(getName()).set("Hidden", isHidden());
                                    this.host.plugin.config.save();
                                }
                                c++;
                            }
                            break;
                    }
                    if (forward != null) {
                        if (!getName().equals(getDisplayName())) forward.setDisplayName(getDisplayName());
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

                        forward.setEditable(true);
                        if (state) pending.set("state", true);
                        c += forward.edit(player, pending);
                        break;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        if (!isRunning() && forward == null && state) start(player);
        return c;
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
        return enabled;
    }

    @Override
    public void setEnabled(boolean value) {
        if (Util.isNull(value)) throw new NullPointerException();
        new SubEditServerEvent(null, this, new NamedContainer<String, Object>("enabled", value), false);
        enabled = value;
    }

    @Override
    public boolean isEditable() {
        return editable;
    }

    @Override
    public void setEditable(boolean value) {
        editable = value;
    }

    @Override
    public boolean isLogging() {
        return log.get();
    }

    @Override
    public void setLogging(boolean value) {
        if (Util.isNull(value)) throw new NullPointerException();
        new SubEditServerEvent(null, this, new NamedContainer<String, Object>("log", value), false);
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
    public Executable getExecutable() {
        return executable;
    }

    @Override
    public String getStopCommand() {
        return stopcmd;
    }

    @Override
    public void setStopCommand(String value) {
        if (Util.isNull(value)) throw new NullPointerException();
        new SubEditServerEvent(null, this, new NamedContainer<String, Object>("stop-cmd", value), false);
        stopcmd = value;
    }

    @Override
    public boolean willAutoRestart() {
        return restart;
    }

    @Override
    public void setAutoRestart(boolean value) {
        if (Util.isNull(value)) throw new NullPointerException();
        new SubEditServerEvent(null, this, new NamedContainer<String, Object>("auto-restart", value), false);
        restart = value;
    }

    @Override
    public boolean isTemporary() {
        return temporary;
    }

    @Override
    public void setTemporary(boolean value) {
        if (Util.isNull(value)) throw new NullPointerException();
        new SubEditServerEvent(null, this, new NamedContainer<String, Object>("temp", value), false);
        temporary = !(value && !isRunning() && !start()) && value;
    }
}

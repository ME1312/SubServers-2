package net.ME1312.SubServers.Bungee.Host.External;

import net.ME1312.Galaxi.Library.Callback.ReturnRunnable;
import net.ME1312.Galaxi.Library.Container.ContainedPair;
import net.ME1312.Galaxi.Library.Container.Container;
import net.ME1312.Galaxi.Library.Container.Value;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Map.ObjectMapValue;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubServers.Bungee.Event.*;
import net.ME1312.SubServers.Bungee.Host.*;
import net.ME1312.SubServers.Bungee.Library.Compatibility.Logger;
import net.ME1312.SubServers.Bungee.Library.Exception.InvalidServerException;
import net.ME1312.SubServers.Bungee.Network.Packet.PacketExEditServer;

import net.md_5.bungee.BungeeServerInfo;
import net.md_5.bungee.api.ChatColor;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

/**
 * External SubServer Class
 */
public class ExternalSubServer extends SubServerImpl {
    private ExternalHost host;
    private boolean enabled;
    private Value<Boolean> log;
    private String dir;
    String exec;
    private String stopcmd;
    private StopAction stopaction;
    private LinkedList<LoggedCommand> history;
    private ExternalSubLogger logger;
    private boolean running;
    private boolean lock;

    /**
     * Creates an External SubServer
     *
     * @param host Host
     * @param name Name
     * @param enabled Enabled Status
     * @param port Port Number
     * @param motd MOTD
     * @param log Logging Status
     * @param directory Directory
     * @param executable Executable
     * @param stopcmd Stop Command
     * @param hidden Hidden Status
     * @param restricted Restricted Status
     * @throws InvalidServerException
     */
    public static ExternalSubServer construct(ExternalHost host, String name, boolean enabled, int port, String motd, boolean log, String directory, String executable, String stopcmd, boolean hidden, boolean restricted) throws InvalidServerException {
        try {
            return new ExternalSubServer(host, name, enabled, port, motd, log, directory, executable, stopcmd, hidden, restricted);
        } catch (NoSuchMethodError e) {
            return new ExternalSubServer(host, name, enabled, (Integer) port, motd, log, directory, executable, stopcmd, hidden, restricted);
        }
    }

    /**
     * Super Method 2 (newest)
     * @see #construct(ExternalHost, String, boolean, int, String, boolean, String, String, String, boolean, boolean) for method details
     */
    protected ExternalSubServer(ExternalHost host, String name, boolean enabled, int port, String motd, boolean log, String directory, String executable, String stopcmd, boolean hidden, boolean restricted) throws InvalidServerException {
        super(host, name, port, motd, hidden, restricted);
        init(host, name, enabled, port, motd, log, directory, executable, stopcmd, hidden, restricted);
    }

    /**
     * Super Method 1 (oldest)
     * @see #construct(ExternalHost, String, boolean, int, String, boolean, String, String, String, boolean, boolean) for method details
     */
    protected ExternalSubServer(ExternalHost host, String name, boolean enabled, Integer port, String motd, boolean log, String directory, String executable, String stopcmd, boolean hidden, boolean restricted) throws InvalidServerException {
        super(host, name, port, motd, hidden, restricted);
        init(host, name, enabled, port, motd, log, directory, executable, stopcmd, hidden, restricted);
    }

    private void init(ExternalHost host, String name, boolean enabled, int port, String motd, boolean log, String directory, String executable, String stopcmd, boolean hidden, boolean restricted) throws InvalidServerException {
        if (Util.isNull(host, name, enabled, port, motd, log, stopcmd, hidden, restricted)) throw new NullPointerException();
        this.host = host;
        this.enabled = enabled;
        this.log = new Container<Boolean>(log);
        this.dir = directory;
        this.exec = executable;
        this.stopcmd = stopcmd;
        this.stopaction = StopAction.NONE;
        this.history = new LinkedList<LoggedCommand>();
        this.logger = new ExternalSubLogger(this, getName(), this.log, null);

        this.running = false;
        this.lock = false;
    }

    @Override
    public boolean start(UUID player) {
        if (!lock && isAvailable() && isEnabled() && !running && getCurrentIncompatibilities().size() == 0) {
            lock = true;
            SubStartEvent event = new SubStartEvent(player, this);
            host.plugin.getPluginManager().callEvent(event);
            if (!event.isCancelled()) {
                Logger.get("SubServers").info("Now starting " + getName());
                started(null);
                host.queue(new PacketExEditServer(this, PacketExEditServer.UpdateType.START, logger.getExternalAddress().toString()));
                return true;
            } else {
                lock = false;
                return false;
            }
        } else return false;
    }
    void started(UUID address) {
        if (!running) {
            started = false;
            running = true;
            lock = false;
            logger.start();
            if (address != null) {
                if (address != logger.getExternalAddress()) host.queue(new PacketExEditServer(this, PacketExEditServer.UpdateType.SET_LOGGING_ADDRESS, logger.getExternalAddress().toString()));
                host.plugin.getPluginManager().callEvent(new SubStartEvent(null, this));
            }
        }
    }
    private void falsestart() {
        Logger.get("SubServers").info("Couldn't start " + getName() + " - See the " + host.getName() + " console for more details");
        running = false;
        logger.stop();
    }

    @Override
    public boolean stop(UUID player) {
        if (running) {
            SubStopEvent event = new SubStopEvent(player, this, false);
            host.plugin.getPluginManager().callEvent(event);
            if (!event.isCancelled()) {
                history.add(new LoggedCommand(player, stopcmd));
                host.queue(new PacketExEditServer(this, PacketExEditServer.UpdateType.STOP));
                return true;
            } else return false;
        } else return false;
    }
    private void stopped(Boolean allowrestart) {
        logger.stop();
        history.clear();
        running = false;
        SubStoppedEvent event = new SubStoppedEvent(this);
        host.plugin.getPluginManager().callEvent(event);
        Logger.get("SubServers").info(getName() + " has stopped");

        if (stopaction == StopAction.REMOVE_SERVER || stopaction == StopAction.RECYCLE_SERVER || stopaction == StopAction.DELETE_SERVER) {
            try {
                if (stopaction == StopAction.RECYCLE_SERVER) {
                    host.recycleSubServer(getName());
                } else if (stopaction == StopAction.DELETE_SERVER) {
                    host.deleteSubServer(getName());
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
                        Thread.sleep(250);
                        start();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }, "SubServers.Bungee::External_Server_Restart_Handler(" + getName() + ')').start();
            }
        }
    }

    @Override
    public boolean terminate(UUID player) {
        if (running) {
            SubStopEvent event = new SubStopEvent(player, this, true);
            host.plugin.getPluginManager().callEvent(event);
            if (!event.isCancelled()) {
                host.queue(new PacketExEditServer(this, PacketExEditServer.UpdateType.TERMINATE));
                return true;
            } else return false;
        } else return false;
    }

    @Override
    public boolean command(UUID player, String command) {
        if (Util.isNull(command)) throw new NullPointerException();
        if (running) {
            SubSendCommandEvent event = new SubSendCommandEvent(player, this, command);
            host.plugin.getPluginManager().callEvent(event);
            if (!event.isCancelled()) {
                history.add(new LoggedCommand(player, event.getCommand()));
                if (event.getCommand().equalsIgnoreCase(stopcmd)) {
                    host.queue(new PacketExEditServer(this, PacketExEditServer.UpdateType.STOP));
                } else {
                    host.queue(new PacketExEditServer(this, PacketExEditServer.UpdateType.COMMAND, event.getCommand()));
                }
                return true;
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
                SubEditServerEvent event = new SubEditServerEvent(player, this, new ContainedPair<String, ObjectMapValue>(key, value), perma);
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
                                    if (enabled != value.asBoolean()) host.queue(new PacketExEditServer(this, PacketExEditServer.UpdateType.SET_ENABLED, (Boolean) value.asBoolean()));
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
                                    waitFor(() -> host.getSubServer(getName()), null);
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
                                    Util.reflect(SubServerImpl.class.getDeclaredField("template"), this, value.asString());
                                    if (perma && this.host.plugin.servers.get().getMap("Servers").getKeys().contains(getName())) {
                                        this.host.plugin.servers.get().getMap("Servers").getMap(getName()).set("Template", value.asString());
                                        this.host.plugin.servers.save();
                                    }
                                    c++;
                                }
                                break;
                            case "port":
                                if (value.isNumber() && host.removeSubServer(player, getName())) {
                                    waitFor(() -> host.getSubServer(getName()), null);
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
                                    if (log.value() != value.asBoolean()) host.queue(new PacketExEditServer(this, PacketExEditServer.UpdateType.SET_LOGGING, (Boolean) value.asBoolean()));
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
                                if (value.isString() && host.removeSubServer(player, getName())) {
                                    waitFor(() -> host.getSubServer(getName()), null);
                                    SubServer server = host.addSubServer(player, getName(), isEnabled(), getAddress().getPort(), getMotd(), isLogging(), value.asRawString(), getExecutable(), getStopCommand(), isHidden(), isRestricted());
                                    if (server != null) {
                                        if (perma && this.host.plugin.servers.get().getMap("Servers").getKeys().contains(getName())) {
                                            this.host.plugin.servers.get().getMap("Servers").getMap(getName()).set("Directory", server.getPath());
                                            this.host.plugin.servers.save();
                                        }
                                        forward = server;
                                        c++;
                                    }
                                }
                                break;
                            case "exec":
                            case "executable":
                                if (value.isString() && host.removeSubServer(player, getName())) {
                                    waitFor(() -> host.getSubServer(getName()), null);
                                    SubServer server = host.addSubServer(player, getName(), isEnabled(), getAddress().getPort(), getMotd(), isLogging(), getPath(), value.asRawString(), getStopCommand(), isHidden(), isRestricted());
                                    if (server != null) {
                                        if (perma && this.host.plugin.servers.get().getMap("Servers").getKeys().contains(getName())) {
                                            this.host.plugin.servers.get().getMap("Servers").getMap(getName()).set("Executable", value.asRawString());
                                            this.host.plugin.servers.save();
                                        }
                                        forward = server;
                                        c++;
                                    }
                                }
                                break;
                            case "state":
                                if (value.isBoolean()) {
                                    state = value.asBoolean();
                                }
                                break;
                            case "stop-cmd":
                            case "stop-command":
                                if (value.isString()) {
                                    if (!stopcmd.equals(value)) host.queue(new PacketExEditServer(this, PacketExEditServer.UpdateType.SET_STOP_COMMAND, value.asRawString()));
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
                            List<String> groups = new ArrayList<String>();
                            Util.reflect(SubServerImpl.class.getDeclaredField("template"), forward, Util.reflect(SubServerImpl.class.getDeclaredField("template"), this));
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
    } private <V> void waitFor(ReturnRunnable<V> method, V value) throws InterruptedException {
        while (method.run() != value) {
            Thread.sleep(250);
        }
    }

    @Override
    public void waitFor() throws InterruptedException {
        while (running && host.getSubData()[0] != null) {
            Thread.sleep(250);
        }
    }

    @Override
    public boolean isRunning() {
        return running || lock;
    }

    @Override
    public void setDisplayName(String value) {
        super.setDisplayName(value);
        logger.name = getDisplayName();
        host.plugin.getPluginManager().callEvent(new SubEditServerEvent(null, this, new ContainedPair<String, Object>("display", value), false));
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
        host.plugin.getPluginManager().callEvent(new SubEditServerEvent(null, this, new ContainedPair<String, Object>("enabled", value), false));
        if (enabled != value) host.queue(new PacketExEditServer(this, PacketExEditServer.UpdateType.SET_ENABLED, (Boolean) value));
        enabled = value;
    }

    @Override
    public boolean isLogging() {
        return log.value();
    }

    @Override
    public void setLogging(boolean value) {
        if (Util.isNull(value)) throw new NullPointerException();
        host.plugin.getPluginManager().callEvent(new SubEditServerEvent(null, this, new ContainedPair<String, Object>("log", value), false));
        if (log.value() != value) host.queue(new PacketExEditServer(this, PacketExEditServer.UpdateType.SET_LOGGING, (Boolean) value));
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
        return exec;
    }

    @Override
    public String getStopCommand() {
        return stopcmd;
    }

    @Override
    public void setStopCommand(String value) {
        if (Util.isNull(value)) throw new NullPointerException();
        host.plugin.getPluginManager().callEvent(new SubEditServerEvent(null, this, new ContainedPair<String, Object>("stop-cmd", value), false));
        if (!stopcmd.equals(value)) host.queue(new PacketExEditServer(this, PacketExEditServer.UpdateType.SET_STOP_COMMAND, value));
        stopcmd = value;
    }

    @Override
    public StopAction getStopAction() {
        return stopaction;
    }

    @Override
    public void setStopAction(StopAction action) {
        if (Util.isNull(action)) throw new NullPointerException();
        host.plugin.getPluginManager().callEvent(new SubEditServerEvent(null, this, new ContainedPair<String, Object>("stop-action", action), false));
        stopaction = action;
    }
}

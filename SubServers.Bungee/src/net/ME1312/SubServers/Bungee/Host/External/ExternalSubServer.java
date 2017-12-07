package net.ME1312.SubServers.Bungee.Host.External;

import net.ME1312.SubServers.Bungee.Event.*;
import net.ME1312.SubServers.Bungee.Host.*;
import net.ME1312.SubServers.Bungee.Library.Config.YAMLSection;
import net.ME1312.SubServers.Bungee.Library.Config.YAMLValue;
import net.ME1312.SubServers.Bungee.Library.Container;
import net.ME1312.SubServers.Bungee.Library.Exception.InvalidServerException;
import net.ME1312.SubServers.Bungee.Library.NamedContainer;
import net.ME1312.SubServers.Bungee.Library.Util;
import net.ME1312.SubServers.Bungee.Network.Packet.PacketExUpdateServer;
import net.ME1312.SubServers.Bungee.SubAPI;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

/**
 * External SubServer Class
 */
public class ExternalSubServer extends SubServerContainer {
    private ExternalHost host;
    private boolean enabled;
    private boolean editable;
    private Container<Boolean> log;
    private String dir;
    protected Executable exec;
    private String stopcmd;
    private LinkedList<LoggedCommand> history;
    private ExternalSubLogger logger;
    private boolean restart;
    private boolean temporary;
    private boolean running;

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
     * @param restart Auto-Restart
     * @param hidden Hidden Status
     * @param restricted Restricted Status
     * @throws InvalidServerException
     */
    public ExternalSubServer(ExternalHost host, String name, boolean enabled, int port, String motd, boolean log, String directory, Executable executable, String stopcmd, boolean restart, boolean hidden, boolean restricted) throws InvalidServerException {
        super(host, name, port, motd, hidden, restricted);
        if (Util.isNull(host, name, enabled, port, motd, log, stopcmd, restart, hidden, restricted)) throw new NullPointerException();
        this.host = host;
        this.enabled = enabled;
        this.editable = false;
        this.log = new Container<Boolean>(log);
        this.dir = directory;
        this.exec = executable;
        this.stopcmd = stopcmd;
        this.history = new LinkedList<LoggedCommand>();
        this.logger = new ExternalSubLogger(this, getName(), this.log, null);
        this.restart = restart;

        this.running = false;
        this.temporary = false;
    }

    @Override
    public boolean start(UUID player) {
        if (isEnabled() && !running && getCurrentIncompatibilities().size() == 0) {
            SubStartEvent event = new SubStartEvent(player, this);
            host.plugin.getPluginManager().callEvent(event);
            if (!event.isCancelled()) {
                System.out.println("SubServers > Now starting " + getName());
                running = true;
                logger.start();
                host.queue(new PacketExUpdateServer(this, PacketExUpdateServer.UpdateType.START, logger.getExternalAddress().toString()));
                return true;
            } else return false;
        } else return false;
    }
    private void falsestart() {
        System.out.println("SubServers > Couldn't start " + getName() + " \u2014 See the " + host.getName() + " console for more details");
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
                host.queue(new PacketExUpdateServer(this, PacketExUpdateServer.UpdateType.STOP));
                return true;
            } else return false;
        } else return false;
    }
    private void stopped(Boolean allowrestart) {
        SubStoppedEvent event = new SubStoppedEvent(this);
        host.plugin.getPluginManager().callEvent(event);
        System.out.println("SubServers > " + getName() + " has stopped");
        logger.stop();
        history.clear();
        running = false;

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
                        Thread.sleep(250);
                        start();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();
            }
        }
    }

    @Override
    public boolean terminate(UUID player) {
        if (running) {
            SubStopEvent event = new SubStopEvent(player, this, true);
            host.plugin.getPluginManager().callEvent(event);
            if (!event.isCancelled()) {
                host.queue(new PacketExUpdateServer(this, PacketExUpdateServer.UpdateType.TERMINATE));
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
                    host.queue(new PacketExUpdateServer(this, PacketExUpdateServer.UpdateType.STOP));
                } else {
                    host.queue(new PacketExUpdateServer(this, PacketExUpdateServer.UpdateType.COMMAND, event.getCommand()));
                }
                return true;
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
                                    if (!getName().equals(getDisplayName())) server.setDisplayName(getDisplayName());
                                    for (String group : getGroups()) {
                                        removeGroup(group);
                                        server.addGroup(group);
                                    }
                                    for (String extra : getExtra().getKeys()) server.addExtra(extra, getExtra(extra));
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
                                logger.name = value.asString();
                                if (this.host.plugin.config.get().getSection("Servers").getKeys().contains(getName())) {
                                    if (getName().equals(getDisplayName())) {
                                        this.host.plugin.config.get().getSection("Servers").getSection(getName()).remove("Display-Name");
                                    } else {
                                        this.host.plugin.config.get().getSection("Servers").getSection(getName()).set("Display-Name", getDisplayName());
                                    }
                                    this.host.plugin.config.save();
                                }
                                c++;
                            }
                            break;
                        case "enabled":
                            if (value.isBoolean()) {
                                if (enabled != value.asBoolean()) host.queue(new PacketExUpdateServer(this, PacketExUpdateServer.UpdateType.SET_ENABLED, (Boolean) value.asBoolean()));
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
                                for (String group : getGroups()) removeGroup(group);
                                for (String group : value.asStringList()) addGroup(group);
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
                                    if (!getName().equals(getDisplayName())) server.setDisplayName(getDisplayName());
                                    for (String group : getGroups()) {
                                        removeGroup(group);
                                        server.addGroup(group);
                                    }
                                    for (String extra : getExtra().getKeys()) server.addExtra(extra, getExtra(extra));
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
                            if (value.isInt() && host.removeSubServer(player, getName())) {
                                SubServer server = host.addSubServer(player, getName(), isEnabled(), value.asInt(), getMotd(), isLogging(), getPath(), getExecutable(), getStopCommand(), false, willAutoRestart(), isHidden(), isRestricted(), isTemporary());
                                if (server != null) {
                                    if (!getName().equals(getDisplayName())) server.setDisplayName(getDisplayName());
                                    for (String group : getGroups()) {
                                        removeGroup(group);
                                        server.addGroup(group);
                                    }
                                    for (String extra : getExtra().getKeys()) server.addExtra(extra, getExtra(extra));
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
                                Field f = ServerContainer.class.getDeclaredField("motd");
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
                                if (log.get() != value.asBoolean()) host.queue(new PacketExUpdateServer(this, PacketExUpdateServer.UpdateType.SET_LOGGING, (Boolean) value.asBoolean()));
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
                                SubServer server = host.addSubServer(player, getName(), isEnabled(), getAddress().getPort(), getMotd(), isLogging(), value.asRawString(), getExecutable(), getStopCommand(), false, willAutoRestart(), isHidden(), isRestricted(), isTemporary());
                                if (server != null) {
                                    if (!getName().equals(getDisplayName())) server.setDisplayName(getDisplayName());
                                    for (String group : getGroups()) {
                                        removeGroup(group);
                                        server.addGroup(group);
                                    }
                                    for (String extra : getExtra().getKeys()) server.addExtra(extra, getExtra(extra));
                                    if (this.host.plugin.config.get().getSection("Servers").getKeys().contains(getName())) {
                                        this.host.plugin.config.get().getSection("Servers").getSection(getName()).set("Directory", server.getPath());
                                        this.host.plugin.config.save();
                                    }
                                    forward = server;
                                    c++;
                                }
                            }
                            break;
                        case "exec":
                            if (value.isString()) {
                                SubServer server = host.addSubServer(player, getName(), isEnabled(), getAddress().getPort(), getMotd(), isLogging(), getPath(), new Executable(value.asRawString()), getStopCommand(), false, willAutoRestart(), isHidden(), isRestricted(), isTemporary());
                                if (server != null) {
                                    if (!getName().equals(getDisplayName())) server.setDisplayName(getDisplayName());
                                    for (String group : getGroups()) {
                                        removeGroup(group);
                                        server.addGroup(group);
                                    }
                                    for (String extra : getExtra().getKeys()) server.addExtra(extra, getExtra(extra));
                                    if (this.host.plugin.config.get().getSection("Servers").getKeys().contains(getName())) {
                                        this.host.plugin.config.get().getSection("Servers").getSection(getName()).set("Executable", value.asRawString());
                                        this.host.plugin.config.save();
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
                            if (value.isString()) {
                                if (!stopcmd.equals(value)) host.queue(new PacketExUpdateServer(this, PacketExUpdateServer.UpdateType.SET_STOP_COMMAND, value.asRawString()));
                                stopcmd = value.asRawString();
                                if (this.host.plugin.config.get().getSection("Servers").getKeys().contains(getName())) {
                                    this.host.plugin.config.get().getSection("Servers").getSection(getName()).set("Stop-Command", getStopCommand());
                                    this.host.plugin.config.save();
                                }
                                c++;
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
                        case "restricted":
                            if (value.isBoolean()) {
                                Field f = ServerContainer.class.getDeclaredField("restricted");
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
        while (running && host.client.get() != null) {
            Thread.sleep(250);
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public void setDisplayName(String value) {
        super.setDisplayName(value);
        logger.name = getDisplayName();
        new SubEditServerEvent(null, this, new NamedContainer<String, Object>("display", value), false);
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
        if (enabled != value) host.queue(new PacketExUpdateServer(this, PacketExUpdateServer.UpdateType.SET_ENABLED, (Boolean) value));
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
        if (log.get() != value) host.queue(new PacketExUpdateServer(this, PacketExUpdateServer.UpdateType.SET_LOGGING, (Boolean) value));
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
        return exec;
    }

    @Override
    public String getStopCommand() {
        return stopcmd;
    }

    @Override
    public void setStopCommand(String value) {
        if (Util.isNull(value)) throw new NullPointerException();
        new SubEditServerEvent(null, this, new NamedContainer<String, Object>("stop-cmd", value), false);
        if (!stopcmd.equals(value)) host.queue(new PacketExUpdateServer(this, PacketExUpdateServer.UpdateType.SET_STOP_COMMAND, value));
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
        this.temporary = !(value && !isRunning() && !start()) && value;
    }
}

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
import net.md_5.bungee.BungeeServerInfo;

import java.io.File;
import java.io.IOException;
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
    private Container<Boolean> log;
    private String dir;
    protected String exec;
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
    public ExternalSubServer(ExternalHost host, String name, boolean enabled, int port, String motd, boolean log, String directory, String executable, String stopcmd, boolean hidden, boolean restricted) throws InvalidServerException {
        super(host, name, port, motd, hidden, restricted);
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
        if (!lock && isEnabled() && !running && getCurrentIncompatibilities().size() == 0) {
            lock = true;
            SubStartEvent event = new SubStartEvent(player, this);
            host.plugin.getPluginManager().callEvent(event);
            lock = false;
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
        System.out.println("SubServers > Couldn't start " + getName() + " - See the " + host.getName() + " console for more details");
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

        if (stopaction == StopAction.REMOVE_SERVER || stopaction == StopAction.DELETE_SERVER) {
            try {
                if (stopaction == StopAction.DELETE_SERVER) {
                    host.deleteSubServer(getName());
                } else {
                    try {
                        if (host.plugin.config.get().getSection("Servers").getKeys().contains(getName())) {
                            host.plugin.config.get().getSection("Servers").remove(getName());
                            host.plugin.config.save();
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
        for (String key : edit.getKeys()) {
            pending.remove(key);
            YAMLValue value = edit.get(key);
            SubEditServerEvent event = new SubEditServerEvent(player, this, new NamedContainer<String, YAMLValue>(key, value), true);
            host.plugin.getPluginManager().callEvent(event);
            if (!event.isCancelled()) {
                try {
                    switch (key) {
                        case "name":
                            if (value.isString() && host.removeSubServer(player, getName())) {
                                SubServer server = host.addSubServer(player, value.asRawString(), isEnabled(), getAddress().getPort(), getMotd(), isLogging(), getPath(), getExecutable(), getStopCommand(), isHidden(), isRestricted());
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
                                Util.reflect(ServerContainer.class.getDeclaredField("groups"), this, value.asStringList());
                                if (this.host.plugin.config.get().getSection("Servers").getKeys().contains(getName())) {
                                    this.host.plugin.config.get().getSection("Servers").getSection(getName()).set("Group", value.asStringList());
                                    this.host.plugin.config.save();
                                }
                                c++;
                            }
                            break;
                        case "host":
                            if (value.isString() && host.removeSubServer(player, getName())) {
                                waitFor(() -> host.getSubServer(getName()), null);
                                SubServer server = this.host.plugin.api.getHost(value.asRawString()).addSubServer(player, getName(), isEnabled(), getAddress().getPort(), getMotd(), isLogging(), getPath(), getExecutable(), getStopCommand(), isHidden(), isRestricted());
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
                                waitFor(() -> host.getSubServer(getName()), null);
                                SubServer server = host.addSubServer(player, getName(), isEnabled(), value.asInt(), getMotd(), isLogging(), getPath(), getExecutable(), getStopCommand(), isHidden(), isRestricted());
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
                                Util.reflect(BungeeServerInfo.class.getDeclaredField("motd"), this, value.asColoredString('&'));
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
                            if (value.isString() && host.removeSubServer(player, getName())) {
                                waitFor(() -> host.getSubServer(getName()), null);
                                SubServer server = host.addSubServer(player, getName(), isEnabled(), getAddress().getPort(), getMotd(), isLogging(), value.asRawString(), getExecutable(), getStopCommand(), isHidden(), isRestricted());
                                if (server != null) {
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
                            if (value.isString() && host.removeSubServer(player, getName())) {
                                waitFor(() -> host.getSubServer(getName()), null);
                                SubServer server = host.addSubServer(player, getName(), isEnabled(), getAddress().getPort(), getMotd(), isLogging(), getPath(), value.asRawString(), getStopCommand(), isHidden(), isRestricted());
                                if (server != null) {
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
                        case "stop-action":
                            if (value.isString()) {
                                StopAction action = Util.getDespiteException(() -> StopAction.valueOf(value.asRawString().toUpperCase().replace('-', '_').replace(' ', '_')), null);
                                if (action != null) {
                                    stopaction = action;
                                    if (this.host.plugin.config.get().getSection("Servers").getKeys().contains(getName())) {
                                        this.host.plugin.config.get().getSection("Servers").getSection(getName()).set("Stop-Action", getStopAction().toString());
                                        this.host.plugin.config.save();
                                    }
                                    c++;
                                }
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
                                Util.reflect(BungeeServerInfo.class.getDeclaredField("restricted"), this, value.asBoolean());
                                if (this.host.plugin.config.get().getSection("Servers").getKeys().contains(getName())) {
                                    this.host.plugin.config.get().getSection("Servers").getSection(getName()).set("Restricted", isRestricted());
                                    this.host.plugin.config.save();
                                }
                                c++;
                            }
                            break;
                        case "hidden":
                            if (value.isBoolean()) {
                                Util.reflect(ServerContainer.class.getDeclaredField("hidden"), this, value.asBoolean());
                                if (this.host.plugin.config.get().getSection("Servers").getKeys().contains(getName())) {
                                    this.host.plugin.config.get().getSection("Servers").getSection(getName()).set("Hidden", isHidden());
                                    this.host.plugin.config.save();
                                }
                                c++;
                            }
                            break;
                    }
                    if (forward != null) {
                        forward.setStopAction(getStopAction());
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
    } private <V> void waitFor(Util.ReturnRunnable<V> method, V value) throws InterruptedException {
        while (method.run() != value) {
            Thread.sleep(250);
        }
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
        host.plugin.getPluginManager().callEvent(new SubEditServerEvent(null, this, new NamedContainer<String, Object>("display", value), false));
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
        host.plugin.getPluginManager().callEvent(new SubEditServerEvent(null, this, new NamedContainer<String, Object>("enabled", value), false));
        if (enabled != value) host.queue(new PacketExUpdateServer(this, PacketExUpdateServer.UpdateType.SET_ENABLED, (Boolean) value));
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
        host.plugin.getPluginManager().callEvent(new SubEditServerEvent(null, this, new NamedContainer<String, Object>("stop-cmd", value), false));
        if (!stopcmd.equals(value)) host.queue(new PacketExUpdateServer(this, PacketExUpdateServer.UpdateType.SET_STOP_COMMAND, value));
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

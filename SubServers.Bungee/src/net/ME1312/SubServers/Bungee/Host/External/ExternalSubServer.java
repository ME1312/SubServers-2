package net.ME1312.SubServers.Bungee.Host.External;

import net.ME1312.SubServers.Bungee.Event.*;
import net.ME1312.SubServers.Bungee.Host.Executable;
import net.ME1312.SubServers.Bungee.Host.SubLogger;
import net.ME1312.SubServers.Bungee.Library.Container;
import net.ME1312.SubServers.Bungee.Library.Exception.InvalidServerException;
import net.ME1312.SubServers.Bungee.Host.Host;
import net.ME1312.SubServers.Bungee.Host.SubServer;
import net.ME1312.SubServers.Bungee.Library.Util;
import net.ME1312.SubServers.Bungee.Network.Packet.PacketExUpdateServer;

import java.util.LinkedList;
import java.util.UUID;

/**
 * External SubServer Class
 */
public class ExternalSubServer extends SubServer {
    private ExternalHost host;
    private boolean enabled;
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
        if (isEnabled() && !running) {
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

    @Override
    public void waitFor() throws InterruptedException {
        while (running) {
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
    public String getDirectory() {
        return dir;
    }

    @Override
    public String getStopCommand() {
        return stopcmd;
    }

    @Override
    public void setStopCommand(String value) {
        if (Util.isNull(value)) throw new NullPointerException();
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
        restart = value;
    }

    @Override
    public boolean isTemporary() {
        return temporary;
    }

    @Override
    public void setTemporary(boolean value) {
        if (Util.isNull(value)) throw new NullPointerException();
        this.temporary = !(value && !isRunning() && !start()) && value;
    }
}

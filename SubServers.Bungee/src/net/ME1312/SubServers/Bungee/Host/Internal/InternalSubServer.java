package net.ME1312.SubServers.Bungee.Host.Internal;

import net.ME1312.SubServers.Bungee.Event.*;
import net.ME1312.SubServers.Bungee.Host.Executable;
import net.ME1312.SubServers.Bungee.Host.SubLogger;
import net.ME1312.SubServers.Bungee.Library.Container;
import net.ME1312.SubServers.Bungee.Library.Exception.InvalidServerException;
import net.ME1312.SubServers.Bungee.Host.Host;
import net.ME1312.SubServers.Bungee.Host.SubServer;
import net.ME1312.SubServers.Bungee.Library.NamedContainer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.UUID;

/**
 * Internal SubServer Class
 */
public class InternalSubServer extends SubServer {
    private InternalHost host;
    private boolean enabled;
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
        this.host = host;
        this.enabled = enabled;
        this.log = new Container<Boolean>(log);
        this.dir = directory;
        this.directory = new File(host.getDirectory(), directory);
        this.executable = executable;
        this.stopcmd = stopcmd;
        this.history = new LinkedList<LoggedCommand>();
        this.process = null;
        this.logger = new InternalSubLogger(null, this, getName(), this.log, null);
        this.thread = null;
        this.command = null;
        this.restart = restart;
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
        if (isEnabled() && !(thread != null && thread.isAlive())) {
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
        enabled = value;
    }

    @Override
    public boolean isLogging() {
        return log.get();
    }

    @Override
    public void setLogging(boolean value) {
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
        stopcmd = value;
    }

    @Override
    public boolean willAutoRestart() {
        return restart;
    }

    @Override
    public void setAutoRestart(boolean value) {
        restart = value;
    }

    @Override
    public boolean isTemporary() {
        return temporary;
    }

    @Override
    public void setTemporary(boolean value) {
        temporary = !(value && !isRunning() && !start()) && value;
    }
}

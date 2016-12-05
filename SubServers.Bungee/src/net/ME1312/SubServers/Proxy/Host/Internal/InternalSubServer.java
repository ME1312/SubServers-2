package net.ME1312.SubServers.Proxy.Host.Internal;

import net.ME1312.SubServers.Proxy.Event.SubSendCommandEvent;
import net.ME1312.SubServers.Proxy.Event.SubStartEvent;
import net.ME1312.SubServers.Proxy.Event.SubStopEvent;
import net.ME1312.SubServers.Proxy.Event.SubStoppedEvent;
import net.ME1312.SubServers.Proxy.Host.Executable;
import net.ME1312.SubServers.Proxy.Libraries.Container;
import net.ME1312.SubServers.Proxy.Libraries.Exception.InvalidServerException;
import net.ME1312.SubServers.Proxy.Host.Host;
import net.ME1312.SubServers.Proxy.Host.SubServer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class InternalSubServer extends SubServer {
    private InternalHost host;
    private boolean enabled;
    private Container<Boolean> log;
    private File directory;
    private Executable executable;
    private String stopcmd;
    private Process process;
    private List<String> queue;
    private boolean restart;
    private boolean temporary;
    private InternalSubServer instance;

    public InternalSubServer(Host host, String name, boolean enabled, int port, String motd, boolean log, String directory, Executable executable, String stopcmd, boolean start, boolean restart, boolean temporary) throws InvalidServerException {
        super(host, name, port, motd);
        this.host = (InternalHost) host;
        this.enabled = enabled;
        this.log = new Container<Boolean>(log);
        this.directory = new File(((InternalHost) host).directory, directory);
        this.executable = executable;
        this.stopcmd = stopcmd;
        this.process = null;
        this.queue = new ArrayList<String>();
        this.restart = restart;
        this.temporary = temporary;
        this.instance = this;

        if (start || temporary) start();
    }

    private void run() {
        new Thread() {
            public void run() {
                final Container<Boolean> allowRestart = new Container<Boolean>(true);
                try {
                    process = Runtime.getRuntime().exec(executable.toString(), null, directory);
                    System.out.println("SubServers > Now starting " + instance.getName());
                    final InternalSubLogger read = new InternalSubLogger(process.getInputStream(), instance.getName(), log, null);
                    read.start();
                    final BufferedWriter cmd = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
                    new Thread() {
                        @Override
                        public void run() {
                            try {
                                do {
                                    if (!queue.isEmpty()) {
                                        while (queue.size() > 0) {
                                            try {
                                                if (queue.get(0).equalsIgnoreCase(stopcmd)) allowRestart.set(false);
                                                cmd.write(queue.get(0));
                                                cmd.newLine();
                                                cmd.flush();
                                                queue.remove(0);
                                                Thread.sleep(100);
                                            } catch (IOException | InterruptedException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    }
                                    Thread.sleep(500);

                                } while (read.isAlive());
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }.start();
                    try {
                        process.waitFor();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    allowRestart.set(false);
                }

                SubStoppedEvent event = new SubStoppedEvent(instance);
                host.plugin.getPluginManager().callEvent(event);
                System.out.println("SubServers > " + instance.getName() + " has stopped");
                process = null;
                queue.clear();

                if (temporary) {
                    try {
                        host.removeSubServer(instance.getName());
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else {
                    if (restart && allowRestart.get()) {
                        try {
                            Thread.sleep(2500);
                            start();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }.start();
    }

    @Override
    public void start(UUID player) {
        if (enabled && !isRunning()) {
            SubStartEvent event = new SubStartEvent(this, player);
            host.plugin.getPluginManager().callEvent(event);
            if (!event.isCancelled()) {
                run();
            }
        }
    }

    @Override
    public void stop(UUID player) {
        if (isRunning()) {
            SubStopEvent event = new SubStopEvent(this, player, false);
            host.plugin.getPluginManager().callEvent(event);
            if (!event.isCancelled()) {
                queue.add(stopcmd);
            }
        }
    }

    @Override
    public void terminate(UUID player) {
        if (isRunning()) {
            SubStopEvent event = new SubStopEvent(this, player, true);
            host.plugin.getPluginManager().callEvent(event);
            if (!event.isCancelled()) {
                process.destroyForcibly();
            }
        }
    }

    @Override
    public void command(UUID player, String command) {
        if (isRunning()) {
            SubSendCommandEvent event = new SubSendCommandEvent(this, player, command);
            host.plugin.getPluginManager().callEvent(event);
            if (!event.isCancelled()) {
                queue.add(command);
            }
        }
    }

    @Override
    public void waitFor() throws InterruptedException {
        if (isRunning()) {
            process.waitFor();
        }
    }

    @Override
    public boolean isRunning() {
        return process != null && process.isAlive();
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
}

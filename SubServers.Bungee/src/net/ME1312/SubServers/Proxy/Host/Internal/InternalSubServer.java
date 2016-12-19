package net.ME1312.SubServers.Proxy.Host.Internal;

import net.ME1312.SubServers.Proxy.Event.*;
import net.ME1312.SubServers.Proxy.Host.Executable;
import net.ME1312.SubServers.Proxy.Library.Container;
import net.ME1312.SubServers.Proxy.Library.Exception.InvalidServerException;
import net.ME1312.SubServers.Proxy.Host.Host;
import net.ME1312.SubServers.Proxy.Host.SubServer;
import net.ME1312.SubServers.Proxy.Library.NamedContainer;
import net.ME1312.SubServers.Proxy.SubPlugin;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.UUID;

public class InternalSubServer extends SubServer {
    private InternalHost host;
    private String name;
    private boolean enabled;
    private Container<Boolean> log;
    private File directory;
    private Executable executable;
    private String stopcmd;
    private Process process;
    private Thread thread;
    private BufferedWriter command;
    private boolean restart;
    private boolean allowrestart;
    private boolean temporary;

    public InternalSubServer(Host host, String name, boolean enabled, int port, String motd, boolean log, String directory, Executable executable, String stopcmd, boolean start, boolean restart, boolean hidden, boolean restricted, boolean temporary) throws InvalidServerException {
        super(host, name, port, motd, hidden, restricted);
        this.host = (InternalHost) host;
        this.name = name;
        this.enabled = enabled;
        this.log = new Container<Boolean>(log);
        this.directory = new File(host.getDirectory(), directory);
        this.executable = executable;
        this.stopcmd = stopcmd;
        this.process = null;
        this.thread = null;
        this.command = null;
        this.restart = restart;
        this.temporary = temporary;

        if (start || temporary) start();
    }

    private void run() {
        (thread = new Thread(() -> {
            allowrestart = true;
            try {
                process = Runtime.getRuntime().exec(executable.toString(), null, directory);
                System.out.println("SubServers > Now starting " + getName());
                final InternalSubLogger read = new InternalSubLogger(process.getInputStream(), getName(), log, null);
                read.start();
                command = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));

                try {
                    process.waitFor();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } catch (IOException e) {
                e.printStackTrace();
                allowrestart = false;
            }

            SubStoppedEvent event = new SubStoppedEvent(this);
            host.plugin.getPluginManager().callEvent(event);
            System.out.println("SubServers > " + getName() + " has stopped");
            process = null;
            command = null;

            if (temporary) {
                try {
                    host.removeSubServer(getName());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                if (restart && allowrestart) {
                    try {
                        Thread.sleep(2500);
                        start();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        })).start();
    }

    @Override
    public boolean start(UUID player) {
        if (enabled && !isRunning()) {
            SubStartEvent event = new SubStartEvent(player, this);
            host.plugin.getPluginManager().callEvent(event);
            if (!event.isCancelled()) {
                run();
                return true;
            } else return false;
        } else return false;
    }

    @Override
    public boolean stop(UUID player) {
        if (isRunning()) {
            SubStopEvent event = new SubStopEvent(player, this, false);
            host.plugin.getPluginManager().callEvent(event);
            if (!event.isCancelled()) {
                try {
                    allowrestart = false;
                    command.write(stopcmd);
                    command.newLine();
                    command.flush();
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
        if (isRunning()) {
            SubStopEvent event = new SubStopEvent(player, this, true);
            host.plugin.getPluginManager().callEvent(event);
            if (!event.isCancelled()) {
                allowrestart = false;
                process.destroyForcibly();
                return true;
            } else return false;
        } else return false;
    }

    @Override
    public boolean command(UUID player, String command) {
        if (isRunning()) {
            SubSendCommandEvent event = new SubSendCommandEvent(player, this, command);
            host.plugin.getPluginManager().callEvent(event);
            if (!event.isCancelled()) {
                try {
                    if (event.getCommand().equalsIgnoreCase(stopcmd)) allowrestart = false;
                    this.command.write(event.getCommand());
                    this.command.newLine();
                    this.command.flush();
                    return true;
                } catch (IOException e) {
                    e.printStackTrace();
                    return false;
                }
            } else return false;
        } else return false;
    }

    @Override
    public int edit(UUID player, NamedContainer<String, ?>... changes) {
        int i = 0;
        SubEditServerEvent eEvent = new SubEditServerEvent(player, this, changes);
        host.plugin.getPluginManager().callEvent(eEvent);
        if (!eEvent.isCancelled()) {
            for (NamedContainer<String, ?> change : changes) {
                try {
                    boolean running = isRunning();
                    switch (change.name().toLowerCase()) {
                        case "host":
                            if (change.get() instanceof String) {
                                InternalHost oldhost = host;
                                Host newhost = host.plugin.hosts.get(((String) change.get()).toLowerCase());
                                if (newhost != null) {
                                    if (running) allowrestart = false;
                                    if (host.removeSubServer(player, getName())) {
                                        if (newhost.addSubServer(player, getName(), isEnabled(), getAddress().getPort(), getMotd(), isLogging(), directory.getPath(), executable, getStopCommand(), running, willAutoRestart(), isHidden(), isRestricted(), isTemporary()) != null) {
                                            if (host.plugin.config.get().getSection("Servers").getKeys().contains(getName())) {
                                                host.plugin.config.get().getSection("Servers").getSection(getName()).set("Host", newhost.getName());
                                                host.plugin.config.save();
                                            }
                                            i++;
                                        } else {
                                            oldhost.servers.put(getName().toLowerCase(), this);
                                            if (running) start(player);
                                        }
                                    }
                                }
                            }
                            break;
                        case "name":
                            if (change.get() instanceof String) {
                                host.servers.remove(getName().toLowerCase());
                                if (host.plugin.config.get().getSection("Servers").getKeys().contains(getName())) {
                                    host.plugin.config.get().getSection("Servers").set((String) change.get(), host.plugin.config.get().getSection("Servers").getSection(getName()));
                                    host.plugin.config.get().getSection("Servers").remove(getName());
                                    host.plugin.config.save();
                                }
                                name = (String) change.get();
                                host.servers.put(((String) change.get()).toLowerCase(), this);
                                i++;
                            }
                            break;
                        case "enabled":
                            if (change.get() instanceof Boolean) {
                                setEnabled((Boolean) change.get());
                                if (host.plugin.config.get().getSection("Servers").getKeys().contains(getName())) {
                                    host.plugin.config.get().getSection("Servers").getSection(getName()).set("Enabled", isEnabled());
                                    host.plugin.config.save();
                                }
                                i++;
                            }
                            break;
                        case "log":
                            if (change.get() instanceof Boolean) {
                                setLogging((Boolean) change.get());
                                if (host.plugin.config.get().getSection("Servers").getKeys().contains(getName())) {
                                    host.plugin.config.get().getSection("Servers").getSection(getName()).set("Log", isLogging());
                                    host.plugin.config.save();
                                }
                                i++;
                            }
                            break;
                        case "dir":
                            if (change.get() instanceof String) {
                                directory = new File((String) change.get());
                                if (host.plugin.config.get().getSection("Servers").getKeys().contains(getName())) {
                                    host.plugin.config.get().getSection("Servers").getSection(getName()).set("Directory", directory.getPath());
                                    host.plugin.config.save();
                                }
                                i++;
                            } else if (change.get() instanceof File) {
                                directory = (File) change.get();
                                if (host.plugin.config.get().getSection("Servers").getKeys().contains(getName())) {
                                    host.plugin.config.get().getSection("Servers").getSection(getName()).set("Directory", directory.getPath());
                                    host.plugin.config.save();
                                }
                                i++;
                            }
                            break;
                        case "exec":
                            if (change.get() instanceof String) {
                                executable = new Executable((String) change.get());
                                if (host.plugin.config.get().getSection("Servers").getKeys().contains(getName())) {
                                    host.plugin.config.get().getSection("Servers").getSection(getName()).set("Executable", executable.toString());
                                    host.plugin.config.save();
                                }
                                i++;
                            } else if (change.get() instanceof Executable) {
                                executable = (Executable) change.get();
                                if (host.plugin.config.get().getSection("Servers").getKeys().contains(getName())) {
                                    host.plugin.config.get().getSection("Servers").getSection(getName()).set("Executable", executable.toString());
                                    host.plugin.config.save();
                                }
                                i++;
                            }
                            break;
                        case "running":
                            if (change.get() instanceof Boolean) {
                                if (running) {
                                    if (!((Boolean) change.get())) stop(player);
                                } else {
                                    if (((Boolean) change.get())) start(player);
                                }
                                i++;
                            }
                            break;
                        case "stop-cmd":
                            if (change.get() instanceof String) {
                                setStopCommand((String) change.get());
                                if (host.plugin.config.get().getSection("Servers").getKeys().contains(getName())) {
                                    host.plugin.config.get().getSection("Servers").getSection(getName()).set("Stop-Command", getStopCommand());
                                    host.plugin.config.save();
                                }
                                i++;
                            }
                            break;
                        case "auto-run":
                            if (change.get() instanceof Boolean) {
                                if (host.plugin.config.get().getSection("Servers").getKeys().contains(getName())) {
                                    host.plugin.config.get().getSection("Servers").getSection(getName()).set("Run-On-Launch", change.get());
                                    host.plugin.config.save();
                                    i++;
                                }
                            }
                            break;
                        case "auto-restart":
                            if (change.get() instanceof Boolean) {
                                setAutoRestart((Boolean) change.get());
                                if (host.plugin.config.get().getSection("Servers").getKeys().contains(getName())) {
                                    host.plugin.config.get().getSection("Servers").getSection(getName()).set("Auto-Restart", willAutoRestart());
                                    host.plugin.config.save();
                                }
                                i++;
                            }
                            break;
                        case "restricted":
                            if (change.get() instanceof Boolean) {
                                setRestricted((Boolean) change.get());
                                if (host.plugin.config.get().getSection("Servers").getKeys().contains(getName())) {
                                    host.plugin.config.get().getSection("Servers").getSection(getName()).set("Restricted", isRestricted());
                                    host.plugin.config.save();
                                }
                                i++;
                            }
                            break;
                        case "hidden":
                            if (change.get() instanceof Boolean) {
                                setHidden((Boolean) change.get());
                                if (host.plugin.config.get().getSection("Servers").getKeys().contains(getName())) {
                                    host.plugin.config.get().getSection("Servers").getSection(getName()).set("Hidden", isHidden());
                                    host.plugin.config.save();
                                }
                                i++;
                            }
                            break;
                        case "motd":
                            if (change.get() instanceof String) {
                                setMotd((String) change.get());
                                if (host.plugin.config.get().getSection("Servers").getKeys().contains(getName())) {
                                    host.plugin.config.get().getSection("Servers").getSection(getName()).set("Motd", getMotd());
                                    host.plugin.config.save();
                                }
                                i++;
                            }
                            break;
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        }
        return i;
    }

    @Override
    public void waitFor() throws InterruptedException {
        while (thread != null && thread.isAlive()) {
            Thread.sleep(250);
        }
    }

    @Override
    public String getName() {
        return name;
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
    public boolean isEditable() {
        return host.isEditable() && !isRunning();
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

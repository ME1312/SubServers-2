package net.ME1312.SubServers.Host.Executable;

import net.ME1312.Galaxi.Library.Container;
import net.ME1312.Galaxi.Library.UniversalFile;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.SubData.Client.SubDataClient;
import net.ME1312.SubServers.Host.ExHost;
import net.ME1312.SubServers.Host.Library.Exception.InvalidServerException;
import net.ME1312.SubServers.Host.Network.Packet.PacketExEditServer;
import net.ME1312.SubServers.Host.SubAPI;

import java.io.*;
import java.util.LinkedList;
import java.util.UUID;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;

/**
 * Internal SubServer Class
 */
public class SubServerImpl {
    private ExHost host;
    private String name;
    private boolean enabled;
    private int port;
    private Container<Boolean> log;
    private String dir;
    private File directory;
    private String executable;
    private Process process;
    private SubLoggerImpl logger;
    private Thread thread;
    private BufferedWriter command;
    private LinkedList<String> queue;
    private String stopcmd;
    private boolean allowrestart;

    /**
     * Creates a SubServer
     *
     * @param host SubServers.Host
     * @param name Name
     * @param enabled Enabled Status
     * @param log Logging Status
     * @param directory Directory
     * @param executable Executable String
     * @param stopcmd Stop Command
     * @throws InvalidServerException
     */
    public SubServerImpl(ExHost host, String name, boolean enabled, int port, boolean log, String directory, String executable, String stopcmd) throws InvalidServerException {
        if (Util.isNull(host, name, enabled, log, directory, executable)) throw new NullPointerException();
        this.host = host;
        this.name = name;
        this.enabled = enabled;
        this.port = port;
        this.log = new Container<Boolean>(log);
        this.dir = directory;
        this.directory = new File(host.host.getRawString("Directory"), directory);
        this.executable = executable;
        this.process = null;
        this.logger = new SubLoggerImpl(null, this, name, null, this.log, null);
        this.thread = null;
        this.command = null;
        this.queue = new LinkedList<String>();
        this.stopcmd = stopcmd;

        if (new UniversalFile(this.directory, "plugins:SubServers.Client.jar").exists()) {
            try {
                JarInputStream updated = new JarInputStream(ExHost.class.getResourceAsStream("/net/ME1312/SubServers/Host/Library/Files/client.jar"));
                JarFile existing = new JarFile(new UniversalFile(this.directory, "plugins:SubServers.Client.jar"));

                if (existing.getManifest().getMainAttributes().getValue("Implementation-Title") != null && existing.getManifest().getMainAttributes().getValue("Implementation-Title").startsWith("SubServers.Client") && existing.getManifest().getMainAttributes().getValue("Specification-Title") != null &&
                        updated.getManifest().getMainAttributes().getValue("Implementation-Title") != null && updated.getManifest().getMainAttributes().getValue("Implementation-Title").startsWith("SubServers.Client") && updated.getManifest().getMainAttributes().getValue("Specification-Title") != null) {
                    if (new Version(existing.getManifest().getMainAttributes().getValue("Specification-Title")).compareTo(new Version(updated.getManifest().getMainAttributes().getValue("Specification-Title"))) < 0) {
                        new UniversalFile(this.directory, "plugins:SubServers.Client.jar").delete();
                        Util.copyFromJar(ExHost.class.getClassLoader(), "net/ME1312/SubServers/Host/Library/Files/client.jar", new UniversalFile(this.directory, "plugins:SubServers.Client.jar").getPath());
                    }
                }
                existing.close();
                updated.close();
            } catch (Throwable e) {
                host.log.info.println("Couldn't auto-update SubServers.Client.jar for " + name);
                host.log.error.println(e);
            }
        } else if (new UniversalFile(this.directory, "mods:SubServers.Client.jar").exists()) {
            try {
                JarInputStream updated = new JarInputStream(ExHost.class.getResourceAsStream("/net/ME1312/SubServers/Host/Library/Files/client.jar"));
                JarFile existing = new JarFile(new UniversalFile(this.directory, "mods:SubServers.Client.jar"));

                if (existing.getManifest().getMainAttributes().getValue("Implementation-Title") != null && existing.getManifest().getMainAttributes().getValue("Implementation-Title").startsWith("SubServers.Client") && existing.getManifest().getMainAttributes().getValue("Specification-Title") != null &&
                        updated.getManifest().getMainAttributes().getValue("Implementation-Title") != null && updated.getManifest().getMainAttributes().getValue("Implementation-Title").startsWith("SubServers.Client") && updated.getManifest().getMainAttributes().getValue("Specification-Title") != null) {
                    if (new Version(existing.getManifest().getMainAttributes().getValue("Specification-Title")).compareTo(new Version(updated.getManifest().getMainAttributes().getValue("Specification-Title"))) < 0) {
                        new UniversalFile(this.directory, "mods:SubServers.Client.jar").delete();
                        Util.copyFromJar(ExHost.class.getClassLoader(), "net/ME1312/SubServers/Host/Library/Files/client.jar", new UniversalFile(this.directory, "mods:SubServers.Client.jar").getPath());
                    }
                }
                existing.close();
                updated.close();
            } catch (Throwable e) {
                host.log.info.println("Couldn't auto-update SubServers.Client.jar for " + name);
                host.log.error.println(e);
            }
        }
    }

    private void run() {
        boolean falsestart = true;
        allowrestart = true;
        try {
            ProcessBuilder pb = new ProcessBuilder().command(Executable.parse(host.host.getRawString("Git-Bash"), executable)).directory(directory);
            pb.environment().put("java", System.getProperty("java.home") + File.separator + "bin" + File.separator + "java");
            pb.environment().put("name", getName());
            if (SubAPI.getInstance().getSubDataNetwork()[0] != null) pb.environment().put("host", SubAPI.getInstance().getName());
            pb.environment().put("address", host.config.get().getMap("Settings").getRawString("Server-Bind"));
            pb.environment().put("port", Integer.toString(getPort()));
            process = pb.start();
            falsestart = false;
            host.log.info.println("Now starting " + name);
            logger.process = process;
            logger.start();
            command = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
            for (String command : queue) if (process.isAlive()) {
                this.command.write(command);
                this.command.newLine();
                this.command.flush();
            }
            queue.clear();

            if (process.isAlive()) process.waitFor();
        } catch (IOException | InterruptedException e) {
            host.log.error.println(e);
            allowrestart = false;
            if (falsestart) ((SubDataClient) SubAPI.getInstance().getSubDataNetwork()[0]).sendPacket(new PacketExEditServer(this, PacketExEditServer.UpdateType.LAUNCH_EXCEPTION));
        }

        if (SubAPI.getInstance().getSubDataNetwork()[0] != null)
            ((SubDataClient) SubAPI.getInstance().getSubDataNetwork()[0]).sendPacket(new PacketExEditServer(this, PacketExEditServer.UpdateType.STOPPED, (Integer) process.exitValue(), (Boolean) allowrestart));
        host.log.info.println(name + " has stopped");
        process = null;
        command = null;
    }

    /**
     * Starts the Server
     *
     * @param address External Logging Address
     */
    public void start(UUID address) {
        if (isEnabled() && !(thread != null && thread.isAlive())) {
            logger.address = address;
            (thread = new Thread(this::run, SubAPI.getInstance().getAppInfo().getName() + "::Server_Process_Handler(" + name + ')')).start();
        }
    }

    /**
     * Stops the Server
     */
    public void stop() {
        if (thread != null && thread.isAlive()) {
            try {
                allowrestart = false;
                if (process != null && process.isAlive()) {
                    command.write(stopcmd);
                    command.newLine();
                    command.flush();
                }
            } catch (IOException e) {
                host.log.error.println(e);
            }
        }
    }
    /**
     * Terminates the Server
     */
    public void terminate() {
        allowrestart = false;
        if (process != null && process.isAlive()) Executable.terminate(process);
    }

    /**
     * Commands the Server
     *
     * @param command Command to Send
     */
    public void command(String command) {
        if (Util.isNull(command)) throw new NullPointerException();
        if (thread != null && thread.isAlive()) {
            try {
                if (command.equalsIgnoreCase(stopcmd)) allowrestart = false;
                if (process != null && process.isAlive()) {
                    this.command.write(command);
                    this.command.newLine();
                    this.command.flush();
                }
            } catch (IOException e) {
                host.log.error.println(e);
            }
        }
    }

    /**
     * Waits for the Server to Stop
     *
     * @throws InterruptedException
     */
    public void waitFor() throws InterruptedException {
        while (thread != null && thread.isAlive()) {
            Thread.sleep(250);
        }
    }

    /**
     * Gets the name of the Server
     *
     * @return Server Name
     */
    public String getName() {
        return name;
    }

    /**
     * If the Server is Running
     *
     * @return Running Status
     */
    public boolean isRunning() {
        return process != null && process.isAlive();
    }

    /**
     * If the Server is Enabled
     *
     * @return Enabled Status
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Set if the Server is Enabled
     *
     * @param value Value
     */
    public void setEnabled(boolean value) {
        if (Util.isNull(value)) throw new NullPointerException();
        enabled = value;
    }

    /**
     * Get the Port of the Server
     *
     * @return Server Port Number
     */
    public int getPort() {
        return port;
    }

    /**
     * If the Server is Logging
     *
     * @return Logging Status
     */
    public boolean isLogging() {
        return log.get();
    }

    /**
     * Set if the Server is Logging
     *
     * @param value Value
     */
    public void setLogging(boolean value) {
        if (Util.isNull(value)) throw new NullPointerException();
        log.set(value);
    }

    /**
     * Get Process Logger
     */
    public SubLoggerImpl getLogger() {
        return logger;
    }

    /**
     * Get the Server Directory
     *
     * @return Server Directory
     */
    public String getDirectory() {
        return dir;
    }

    /**
     * Get the Server's Executable String
     *
     * @return Executable String
     */
    public String getExecutable() {
        return executable;
    }

    /**
     * Grab the Command to Stop the Server
     *
     * @return Stop Command
     */
    public String getStopCommand() {
        return stopcmd;
    }

    /**
     * Set the Command that Stops the Server
     *
     * @param value Value
     */
    public void setStopCommand(String value) {
        if (Util.isNull(value)) throw new NullPointerException();
        stopcmd = value;
    }
}

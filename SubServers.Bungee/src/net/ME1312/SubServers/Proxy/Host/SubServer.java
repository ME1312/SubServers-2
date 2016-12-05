package net.ME1312.SubServers.Proxy.Host;

import net.ME1312.SubServers.Proxy.Libraries.Exception.InvalidServerException;

import java.net.InetSocketAddress;
import java.util.UUID;

/**
 * SubServer Layout Class
 *
 * @author ME1312
 */
public abstract class SubServer extends Server {

    /**
     * Creates a SubServer
     *
     * @param host Host
     * @param name Server Name
     * @param port Port Number
     * @param motd Server MOTD
     * @throws InvalidServerException
     */
    public SubServer(Host host, String name, int port, String motd) throws InvalidServerException {
        super(name, InetSocketAddress.createUnresolved(host.getAddress().getHostAddress(), port), motd, false);
    }

    /**
     * Starts the Server
     *
     * @param player Player who Started
     */
    public abstract void start(UUID player);

    /**
     * Starts the Server
     */
    public void start() {
        start(null);
    }

    /**
     * Stops the Server
     *
     * @param player Player who Stopped
     */
    public abstract void stop(UUID player);

    /**
     * Stops the Server
     */
    public void stop() {
        stop(null);
    }

    /**
     * Terminates the Server
     *
     * @param player Player who Terminated
     */
    public abstract void terminate(UUID player);

    /**
     * Terminates the Server
     */
    public void terminate() {
        terminate(null);
    }

    /**
     * Commands the Server
     *
     * @param player Player who Commanded
     * @param command Command to Send
     */
    public abstract void command(UUID player, String command);

    /**
     * Commands the Server
     *
     * @param command Command to Send
     */
    public void command(String command) {
        command(null, command);
    }

    /**
     * Waits for the Server to Stop
     *
     * @throws InterruptedException
     */
    public abstract void waitFor() throws InterruptedException;

    /**
     * If the Server is Running
     *
     * @return Running Status
     */
    public abstract boolean isRunning();

    /**
     * Grabs the Host of the Server
     *
     * @return The Host
     */
    public abstract Host getHost();

    /**
     * If the Server is Enabled
     *
     * @return Enabled Status
     */
    public abstract boolean isEnabled();

    /**
     * Set if the Server is Enabled
     *
     * @param value Value
     */
    public abstract void setEnabled(boolean value);

    /**
     * If the Server is Logging
     *
     * @return Logging Status
     */
    public abstract boolean isLogging();

    /**
     * Set if the Server is Logging
     *
     * @param value Value
     */
    public abstract void setLogging(boolean value);

    /**
     * Grab the Command to Stop the Server
     *
     * @return Stop Command
     */
    public abstract String getStopCommand();

    /**
     * Set the Command that Stops the Server
     *
     * @param value Value
     */
    public abstract void setStopCommand(String value);

    /**
     * If the Server will Auto Restart on unexpected shutdowns
     *
     * @return Auto Restart Status
     */
    public abstract boolean willAutoRestart();

    /**
     * Set if the Server will Auto Restart on unexpected shutdowns
     *
     * @param value Value
     */
    public abstract void setAutoRestart(boolean value);

    /**
     * If the Server is Temporary
     *
     * @return Temporary Status
     */
    public abstract boolean isTemporary();


}

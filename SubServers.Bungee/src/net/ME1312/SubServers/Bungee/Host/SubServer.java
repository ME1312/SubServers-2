package net.ME1312.SubServers.Bungee.Host;

import net.ME1312.SubServers.Bungee.Library.Exception.InvalidServerException;
import net.ME1312.SubServers.Bungee.Library.NamedContainer;

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
     * @param restricted Players will need a permission to join if true
     * @throws InvalidServerException
     */
    public SubServer(Host host, String name, int port, String motd, boolean hidden, boolean restricted) throws InvalidServerException {
        super(name, InetSocketAddress.createUnresolved(host.getAddress().getHostAddress(), port), motd, hidden, restricted);
    }

    /**
     * Starts the Server
     *
     * @param player Player who Started
     * @return Success Status
     */
    public abstract boolean start(UUID player);

    /**
     * Starts the Server
     *
     * @return Success Status
     */
    public boolean start() {
        return start(null);
    }

    /**
     * Stops the Server
     *
     * @param player Player who Stopped
     * @return Success Status
     */
    public abstract boolean stop(UUID player);

    /**
     * Stops the Server
     *
     * @return Success Status
     */
    public boolean stop() {
        return stop(null);
    }

    /**
     * Terminates the Server
     *
     * @param player Player who Terminated
     * @return Success Status
     */
    public abstract boolean terminate(UUID player);

    /**
     * Terminates the Server
     *
     * @return Success Status
     */
    public boolean terminate() {
        return terminate(null);
    }

    /**
     * Commands the Server
     *
     * @param player Player who Commanded
     * @param command Command to Send
     * @return Success Status
     */
    public abstract boolean command(UUID player, String command);

    /**
     * Commands the Server
     *
     * @param command Command to Send
     * @return Success Status
     */
    public boolean command(String command) {
        return command(null, command);
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
     * Get the Server Directory
     *
     * @return Server Directory
     */
    public abstract String getDirectory();

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

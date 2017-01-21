package net.ME1312.SubServers.Bungee.Host;

import net.ME1312.SubServers.Bungee.Library.Exception.InvalidServerException;
import net.ME1312.SubServers.Bungee.Library.NamedContainer;

import java.net.InetSocketAddress;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.UUID;
import java.util.logging.Level;

/**
 * SubServer Layout Class
 */
public abstract class SubServer extends Server {
    /**
     * Command Storage Class
     */
    public static class LoggedCommand {
        private Date date;
        private UUID sender;
        private String command;

        /**
         * Store a Command
         *
         * @param command Command
         */
        public LoggedCommand(String command) {
            this.date = Calendar.getInstance().getTime();
            this.sender = null;
            this.command = command;
        }

        /**
         * Store a Command
         *
         * @param sender Command Sender (null for CONSOLE)
         * @param command Command
         */
        public LoggedCommand(UUID sender, String command) {
            this.date = Calendar.getInstance().getTime();
            this.sender = sender;
            this.command = command;
        }

        /**
         * Store a Command
         *
         * @param date Date
         * @param sender Command Sender (null for CONSOLE)
         * @param command Command
         */
        public LoggedCommand(Date date, UUID sender, String command) {
            this.date = Calendar.getInstance().getTime();
            this.sender = sender;
            this.command = command;
        }

        /**
         * Get the date this command was logged
         *
         * @return Date
         */
        public Date getDate() {
            return date;
        }

        /**
         * Get the command sender
         *
         * @return Command Sender (null if CONSOLE)
         */
        public UUID getSender() {
            return sender;
        }

        /**
         * Get the command
         *
         * @return Command
         */
        public String getCommand() {
            return command;
        }
    }

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
     * Get Process Logger
     */
    public abstract SubLogger getLogger();

    /**
     * Gets all the commands that were sent to this SubServer successfully
     *
     * @return Command History
     */
    public abstract LinkedList<LoggedCommand> getCommandHistory();

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

    /**
     * Set If the Server is Temporary (will start server if not running)
     *
     * @param value Value
     */
    public abstract void setTemporary(boolean value);
}

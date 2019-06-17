package net.ME1312.SubServers.Bungee.Host;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Util;

import java.util.*;

/**
 * SubServer Interface
 */
public interface SubServer extends Server {

    /**
     * SubServer Stop Action Class
     */
    enum StopAction {
        NONE,
        RESTART,
        REMOVE_SERVER,
        RECYCLE_SERVER,
        DELETE_SERVER;

        @Override
        public String toString() {
            return super.toString().substring(0, 1).toUpperCase()+super.toString().substring(1).toLowerCase().replace('_', ' ');
        }
    }

    /**
     * Command Storage Class
     */
    class LoggedCommand {
        private Date date;
        private UUID sender;
        private String command;

        /**
         * Store a Command
         *
         * @param command Command
         */
        public LoggedCommand(String command) {
            if (Util.isNull(command)) throw new NullPointerException();
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
            if (Util.isNull(command)) throw new NullPointerException();
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
            if (Util.isNull(date, command)) throw new NullPointerException();
            this.date = date;
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
     * Starts the Server
     *
     * @param player Player who Started
     * @return Success Status
     */
    boolean start(UUID player);

    /**
     * Starts the Server
     *
     * @return Success Status
     */
    boolean start();

    /**
     * Stops the Server
     *
     * @param player Player who Stopped
     * @return Success Status
     */
    boolean stop(UUID player);

    /**
     * Stops the Server
     *
     * @return Success Status
     */
    boolean stop();

    /**
     * Terminates the Server
     *
     * @param player Player who Terminated
     * @return Success Status
     */
    boolean terminate(UUID player);

    /**
     * Terminates the Server
     *
     * @return Success Status
     */
    boolean terminate();

    /**
     * Commands the Server
     *
     * @param player Player who Commanded
     * @param command Command to Send
     * @return Success Status
     */
    boolean command(UUID player, String command);

    /**
     * Commands the Server
     *
     * @param command Command to Send
     * @return Success Status
     */
    boolean command(String command);

    /**
     * Edits the Server
     *
     * @param player Player Editing
     * @param edit Edits
     * @return Success Status
     */
    default int edit(UUID player, ObjectMap<String> edit) {
        return -1;
    }

    /**
     * Edits the Server
     *
     * @param edit Edits
     * @return Success Status
     */
    default int edit(ObjectMap<String> edit) {
        return edit(null, edit);
    }

    /**
     * Edits the Server (& Saves Changes)
     *
     * @param player Player Editing
     * @param edit Edits
     * @return Success Status
     */
    default int permaEdit(UUID player, ObjectMap<String> edit) {
        return -1;
    }

    /**
     * Edits the Server (& Saves Changes)
     *
     * @param edit Edits
     * @return Success Status
     */
    default int permaEdit(ObjectMap<String> edit) {
        return permaEdit(null, edit);
    }

    /**
     * Waits for the Server to Stop
     *
     * @throws InterruptedException
     */
    void waitFor() throws InterruptedException;

    /**
     * If the Server is Running
     *
     * @return Running Status
     */
    boolean isRunning();

    /**
     * Grabs the Host of the Server
     *
     * @return The Host
     */
    Host getHost();

    /**
     * Grabs the Template this Server was created from
     *
     * @return The Template
     */
    SubCreator.ServerTemplate getTemplate();

    /**
     * Sets the Template this Server was created from
     *
     * @param value Value
     */
    void setTemplate(SubCreator.ServerTemplate value);

    /**
     * Is this Host Available?
     *
     * @return Availability Status
     */
    boolean isAvailable();

    /**
     * If the Server is Enabled
     *
     * @return Enabled Status
     */
    boolean isEnabled();

    /**
     * Set if the Server is Enabled
     *
     * @param value Value
     */
    void setEnabled(boolean value);

    /**
     * If the Server is accepting requests to edit()
     *
     * @see #permaEdit(ObjectMap<String>)
     * @see #permaEdit(UUID, ObjectMap<String>)
     * @return Edit Status
     */
    default boolean isEditable() {
        return permaEdit(new ObjectMap<String>()) >= 0;
    }

    /**
     * If the Server is Logging
     *
     * @return Logging Status
     */
    boolean isLogging();

    /**
     * Set if the Server is Logging
     *
     * @param value Value
     */
    void setLogging(boolean value);

    /**
     * Get Process Logger
     */
    SubLogger getLogger();

    /**
     * Gets all the commands that were sent to this Server successfully
     *
     * @return Command History
     */
    LinkedList<LoggedCommand> getCommandHistory();

    /**
     * Get the Server Directory Path
     *
     * @return Server Directory Path
     */
    String getPath();

    /**
     * Get the Full Server Directory Path
     *
     * @return Full Server Directory Path
     */
    String getFullPath();

    /**
     * Get the Server's Executable String
     *
     * @return Executable String
     */
    String getExecutable();

    /**
     * Grab the Command to Stop the Server
     *
     * @return Stop Command
     */
    String getStopCommand();

    /**
     * Set the Command that Stops the Server
     *
     * @param value Value
     */
    void setStopCommand(String value);

    /**
     * Get the action the Server will take when it stops
     *
     * @return Stop Action
     */
    StopAction getStopAction();

    /**
     * Set the action the Server will take when it stops
     *
     * @param action Stop Action
     */
    void setStopAction(StopAction action);

    /**
     * Toggles compatibility with other Servers
     *
     * @param server SubServers to toggle
     */
    void toggleCompatibility(SubServer... server);

    /**
     * Checks if a Server is compatible
     *
     * @param server Server to check
     * @return Compatible Status
     */
    boolean isCompatible(SubServer server);

    /**
     * Get all listed incompatibilities for this Server
     *
     * @return Incompatibility List
     */
    List<SubServer> getIncompatibilities();

    /**
     * Get incompatibility issues this server currently has
     *
     * @return Current Incompatibility List
     */
    List<SubServer> getCurrentIncompatibilities();
}

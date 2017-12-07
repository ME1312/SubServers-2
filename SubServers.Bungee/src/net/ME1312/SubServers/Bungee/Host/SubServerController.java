package net.ME1312.SubServers.Bungee.Host;

import net.ME1312.SubServers.Bungee.Library.Config.YAMLSection;
import net.ME1312.SubServers.Bungee.Library.Exception.InvalidServerException;

import java.util.*;

/**
 * API-Safe SubServer Layout Class
 */
public abstract class SubServerController {
    private final net.ME1312.SubServers.Bungee.Host.SubServerContainer control;

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
    public SubServerController(Host host, String name, int port, String motd, boolean hidden, boolean restricted) throws InvalidServerException {
        control = new SubServerContainer(host, name, port, motd, hidden, restricted) {
            @Override
            public boolean start() {
                return SubServerController.this.start();
            }

            @Override
            public boolean start(UUID player) {
                return SubServerController.this.start(player);
            }

            @Override
            public boolean stop() {
                return SubServerController.this.stop();
            }

            @Override
            public boolean stop(UUID player) {
                return SubServerController.this.stop(player);
            }

            @Override
            public boolean terminate() {
                return SubServerController.this.terminate();
            }

            @Override
            public boolean terminate(UUID player) {
                return SubServerController.this.terminate(player);
            }

            @Override
            public boolean command(String command) {
                return SubServerController.this.command(command);
            }

            @Override
            public boolean command(UUID player, String command) {
                return SubServerController.this.command(player, command);
            }

            @Override
            public int edit(YAMLSection edit) {
                return SubServerController.this.edit(edit);
            }

            @Override
            public int edit(UUID player, YAMLSection edit) {
                return SubServerController.this.edit(player, edit);
            }

            @Override
            public void waitFor() throws InterruptedException {
                SubServerController.this.waitFor();
            }

            @Override
            public boolean isRunning() {
                return SubServerController.this.isRunning();
            }

            @Override
            public Host getHost() {
                return SubServerController.this.getHost();
            }

            @Override
            public boolean isEnabled() {
                return SubServerController.this.isEnabled();
            }

            @Override
            public void setEnabled(boolean value) {
                SubServerController.this.setEnabled(value);
            }

            @Override
            public boolean isEditable() {
                return SubServerController.this.isEditable();
            }

            @Override
            public void setEditable(boolean value) {
                SubServerController.this.setEditable(value);
            }

            @Override
            public boolean isLogging() {
                return SubServerController.this.isLogging();
            }

            @Override
            public void setLogging(boolean value) {
                SubServerController.this.setLogging(value);
            }

            @Override
            public SubLogger getLogger() {
                return SubServerController.this.getLogger();
            }

            @Override
            public LinkedList<LoggedCommand> getCommandHistory() {
                return SubServerController.this.getCommandHistory();
            }

            @Override
            public String getPath() {
                return SubServerController.this.getPath();
            }

            @Override
            public Executable getExecutable() {
                return SubServerController.this.getExecutable();
            }

            @Override
            public String getStopCommand() {
                return SubServerController.this.getStopCommand();
            }

            @Override
            public void setStopCommand(String value) {
                SubServerController.this.setStopCommand(value);
            }

            @Override
            public boolean willAutoRestart() {
                return SubServerController.this.willAutoRestart();
            }

            @Override
            public void setAutoRestart(boolean value) {
                SubServerController.this.setAutoRestart(value);
            }

            @Override
            public boolean isTemporary() {
                return SubServerController.this.isTemporary();
            }

            @Override
            public void setTemporary(boolean value) {
                SubServerController.this.setTemporary(value);
            }
        };
    }

    /**
     * Get the SubServer that is being controlled
     *
     * @return SubServer
     */
    public SubServer get() {
        return control;
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
     * Edits the Server
     *
     * @param player Player Editing
     * @param edit Edits
     * @return Success Status
     */
    public abstract int edit(UUID player, YAMLSection edit);

    /**
     * Edits the Server
     *
     * @param edit Edits
     * @return Success Status
     */
    public int edit(YAMLSection edit) {
        return edit(null, edit);
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
     * If the Server is accepting requests to edit()
     *
     * @see #edit(YAMLSection)
     * @see #edit(UUID, YAMLSection)
     * @return Edit Status
     */
    public abstract boolean isEditable();

    /**
     * Set if the Server should accept requests to edit()
     *
     * @param value Edit Status
     * @see #edit(YAMLSection)
     * @see #edit(UUID, YAMLSection)
     */
    public abstract void setEditable(boolean value);

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
     * Gets all the commands that were sent to this Server successfully
     *
     * @return Command History
     */
    public abstract LinkedList<SubServer.LoggedCommand> getCommandHistory();

    /**
     * Get the Server Directory Path
     *
     * @return Server Directory Path
     */
    public abstract String getPath();

    /**
     * Get the Server's Executable String
     *
     * @return Executable String
     */
    public abstract Executable getExecutable();

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

    @Override
    public String toString() {
        return control.toString();
    }
}
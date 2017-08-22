package net.ME1312.SubServers.Bungee.Host;

import net.ME1312.SubServers.Bungee.Library.Config.YAMLSection;
import net.ME1312.SubServers.Bungee.Library.Exception.InvalidServerException;
import net.ME1312.SubServers.Bungee.Library.NamedContainer;
import net.ME1312.SubServers.Bungee.Library.Util;
import net.ME1312.SubServers.Bungee.SubAPI;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.json.JSONObject;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.*;

/**
 * SubServer Layout Class
 */
public abstract class SubServer extends Server {
    private List<NamedContainer<String, String>> incompatibilities = new ArrayList<NamedContainer<String, String>>();

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
        super(name, new InetSocketAddress(host.getAddress().getHostAddress(), port), motd, hidden, restricted);
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
    public abstract LinkedList<LoggedCommand> getCommandHistory();

    /**
     * Get the Server Directory Path
     *
     * @return Server Directory Path
     */
    public abstract String getPath();

    /**
     * Get the Full Server Directory Path
     *
     * @return Full Server Directory Path
     */
    public String getFullPath() {
        return new File(getHost().getPath(), getPath()).getPath();
    }

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
     * Toggles compatibility with other Servers
     *
     * @param server SubServers to toggle
     */
    public void toggleCompatibility(SubServer... server) {
        for (SubServer s : server) {
            if (!equals(s)) {
                NamedContainer<String, String> info = new NamedContainer<String, String>(s.getHost().getName(), s.getName());
                if (isCompatible(s)) {
                    incompatibilities.add(info);
                    if (s.isCompatible(this)) toggleCompatibility(this);
                } else {
                    incompatibilities.remove(info);
                    if (!s.isCompatible(this)) toggleCompatibility(this);
                }
            }
        }
    }

    /**
     * Checks if a Server is compatible
     *
     * @param server Server to check
     * @return Compatible Status
     */
    public boolean isCompatible(SubServer server) {
        return !incompatibilities.contains(new NamedContainer<String, String>(server.getHost().getName(), server.getName()));
    }

    /**
     * Get all listed incompatibilities for this Server
     *
     * @return Incompatibility List
     */
    public List<SubServer> getIncompatibilities() {
        List<SubServer> servers = new ArrayList<SubServer>();
        List<NamedContainer<String, String>> temp = new ArrayList<NamedContainer<String, String>>();
        temp.addAll(incompatibilities);
        for (NamedContainer<String, String> info : temp) {
            try {
                SubServer server = SubAPI.getInstance().getHost(info.name()).getSubServer(info.get());
                if (server == null) throw new NullPointerException();
                servers.add(server);
            } catch (Throwable e) {
                incompatibilities.remove(info);
            }
        }
        return servers;
    }

    /**
     * Get incompatibility issues this server currently has
     *
     * @return Current Incompatibility List
     */
    public List<SubServer> getCurrentIncompatibilities() {
        List<SubServer> servers = new ArrayList<SubServer>();
        for (SubServer server : getIncompatibilities()) {
            if (server.isRunning()) servers.add(server);
        }
        return servers;
    }

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
        JSONObject sinfo = new JSONObject(super.toString());
        sinfo.put("type", "SubServer");
        sinfo.put("enabled", getHost().isEnabled() && isEnabled());
        sinfo.put("running", isRunning());
        sinfo.put("log", isLogging());
        List<String> incompatible = new ArrayList<String>();
        for (SubServer server : getCurrentIncompatibilities()) incompatible.add(server.getName());
        sinfo.put("incompatible", incompatible);
        sinfo.put("temp", isTemporary());
        return sinfo.toString();
    }
}

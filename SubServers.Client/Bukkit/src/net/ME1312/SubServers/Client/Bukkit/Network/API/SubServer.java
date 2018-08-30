package net.ME1312.SubServers.Client.Bukkit.Network.API;

import net.ME1312.SubServers.Client.Bukkit.Library.Callback;
import net.ME1312.SubServers.Client.Bukkit.Library.Config.YAMLSection;
import net.ME1312.SubServers.Client.Bukkit.Library.Util;
import net.ME1312.SubServers.Client.Bukkit.Network.Packet.*;
import net.ME1312.SubServers.Client.Bukkit.SubAPI;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class SubServer extends Server {
    private List<SubServer> incompatibilities = null;
    private Host host = null;

    /**
     * SubServer Stop Action Class
     */
    public enum StopAction {
        NONE,
        RESTART,
        REMOVE_SERVER,
        DELETE_SERVER;

        @Override
        public String toString() {
            return super.toString().substring(0, 1).toUpperCase()+super.toString().substring(1).toLowerCase().replace('_', ' ');
        }
    }

    /**
     * Create an API representation of a Server
     *
     * @param raw JSON representation of the Server
     */
    public SubServer(YAMLSection raw) {
        super(raw);
    }

    /**
     * Create an API representation of a Server
     *
     * @param host Host
     * @param raw JSON representation of the Server
     */
    SubServer(Host host, YAMLSection raw) {
        super(raw);
        this.host = host;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof SubServer && super.equals(obj);
    }

    @Override
    public void refresh() {
        host = null;
        incompatibilities = null;
        super.refresh();
    }

    /**
     * Starts the Server
     *
     * @param player Player who Started
     * @param response Response Code
     */
    public void start(UUID player, Callback<Integer> response) {
        if (Util.isNull(response)) throw new NullPointerException();
        StackTraceElement[] origin = new Exception().getStackTrace();
        SubAPI.getInstance().getSubDataNetwork().sendPacket(new PacketStartServer(player, getName(), data -> {
            try {
                response.run(data.getInt("r"));
            } catch (Throwable e) {
                Throwable ew = new InvocationTargetException(e);
                ew.setStackTrace(origin);
                ew.printStackTrace();
            }
        }));
    }

    /**
     * Starts the Server
     *
     * @param response Response Code
     */
    public void start(Callback<Integer> response) {
        start(null, response);
    }

    /**
     * Starts the Server
     *
     * @param player Player who Started
     */
    public void start(UUID player) {
        start(player, i -> {});
    }

    /**
     * Starts the Server
     */
    public void start() {
        start(i -> {});
    }

    /**
     * Stops the Server
     *
     * @param player Player who Stopped
     * @param response Response Code
     */
    public void stop(UUID player, Callback<Integer> response) {
        if (Util.isNull(response)) throw new NullPointerException();
        StackTraceElement[] origin = new Exception().getStackTrace();
        SubAPI.getInstance().getSubDataNetwork().sendPacket(new PacketStopServer(player, getName(), false, data -> {
            try {
                response.run(data.getInt("r"));
            } catch (Throwable e) {
                Throwable ew = new InvocationTargetException(e);
                ew.setStackTrace(origin);
                ew.printStackTrace();
            }
        }));
    }

    /**
     * Stops the Server
     *
     * @param response Response Code
     */
    public void stop(Callback<Integer> response) {
        stop(null, response);
    }

    /**
     * Stops the Server
     *
     * @param player Player who Stopped
     */
    public void stop(UUID player) {
        stop(player, i -> {});
    }

    /**
     * Stops the Server
     */
    public void stop() {
        stop(i -> {});
    }

    /**
     * Terminates the Server
     *
     * @param player Player who Terminated
     * @param response Response Code
     */
    public void terminate(UUID player, Callback<Integer> response) {
        if (Util.isNull(response)) throw new NullPointerException();
        StackTraceElement[] origin = new Exception().getStackTrace();
        SubAPI.getInstance().getSubDataNetwork().sendPacket(new PacketStopServer(player, getName(), true, data -> {
            try {
                response.run(data.getInt("r"));
            } catch (Throwable e) {
                Throwable ew = new InvocationTargetException(e);
                ew.setStackTrace(origin);
                ew.printStackTrace();
            }
        }));
    }

    /**
     * Terminates the Server
     *
     * @param response Response Code
     */
    public void terminate(Callback<Integer> response) {
        terminate(null, response);
    }

    /**
     * Terminates the Server
     *
     * @param player Player who Terminated
     */
    public void terminate(UUID player) {
        terminate(player, i -> {});
    }

    /**
     * Terminates the Server
     */
    public void terminate() {
        terminate(i -> {});
    }

    /**
     * Commands the Server
     *
     * @param player Player who Commanded
     * @param command Commmand to Send
     * @param response Response Code
     */
    public void command(UUID player, String command, Callback<Integer> response) {
        if (Util.isNull(command, response)) throw new NullPointerException();
        StackTraceElement[] origin = new Exception().getStackTrace();
        SubAPI.getInstance().getSubDataNetwork().sendPacket(new PacketCommandServer(player, getName(), command, data -> {
            try {
                response.run(data.getInt("r"));
            } catch (Throwable e) {
                Throwable ew = new InvocationTargetException(e);
                ew.setStackTrace(origin);
                ew.printStackTrace();
            }
        }));
    }

    /**
     * Commands the Server
     *
     * @param command Commmand to Send
     * @param response Response Code
     */
    public void command(String command, Callback<Integer> response) {
        command(null, command, response);
    }

    /**
     * Commands the Server
     *
     * @param player Player who Commanded
     * @param command Command to Send
     */
    public void command(UUID player, String command) {
        command(player, command, i -> {});
    }

    /**
     * Commands the Server
     *
     * @param command Command to Send
     */
    public void command(String command) {
        command(command, i -> {});
    }

    /**
     * If the Server is Running
     *
     * @return Running Status
     */
    public boolean isRunning() {
        return raw.getBoolean("running");
    }

    /**
     * Grabs the Host of the Server
     *
     * @return The Host Name
     */
    public String getHost() {
        return raw.getRawString("host");
    }

    /**
     * Grabs the Host of the Server
     *
     * @param callback The Host
     */
    public void getHost(Callback<Host> callback) {
        if (Util.isNull(callback)) throw new NullPointerException();
        StackTraceElement[] origin = new Exception().getStackTrace();
        Runnable run = () -> {
            try {
                callback.run(host);
            } catch (Throwable e) {
                Throwable ew = new InvocationTargetException(e);
                ew.setStackTrace(origin);
                ew.printStackTrace();
            }
        };

        if (host == null || !host.getName().equalsIgnoreCase(raw.getRawString("host"))) {
            SubAPI.getInstance().getHost(raw.getRawString("host"), host -> {
                this.host = host;
                run.run();
            });
        } else {
            run.run();
        }
    }

    /**
     * If the Server is Enabled
     *
     * @return Enabled Status
     */
    public boolean isEnabled() {
        return raw.getBoolean("enabled");
    }

    /**
     * If the Server is accepting requests to edit()
     *
     * @return Edit Status
     */
    public boolean isEditable() {
        return raw.getBoolean("editable");
    }

    /**
     * If the Server is Logging
     *
     * @return Logging Status
     */
    public boolean isLogging() {
        return raw.getBoolean("log");
    }

    /**
     * Get the Server Directory Path
     *
     * @return Server Directory Path
     */
    public String getPath() {
        return raw.getRawString("dir");
    }

    /**
     * Get the Server's Executable String
     *
     * @return Executable String
     */
    public String getExecutable() {
        return raw.getRawString("exec");
    }

    /**
     * Grab the Command to Stop the Server
     *
     * @return Stop Command
     */
    public String getStopCommand() {
        return raw.getRawString("stop-cmd");
    }

    /**
     * Get the action the Server will take when it stops
     *
     * @return Stop Action
     */
    public StopAction getStopAction() {
        return Util.getDespiteException(() -> StopAction.valueOf(raw.getRawString("stop-action").toUpperCase().replace('-', '_').replace(' ', '_')), null);
    }

    /**
     * Checks if a Server is compatible
     *
     * @param server Server name to check
     * @return Compatible Status
     */
    public boolean isCompatible(String server) {
        LinkedList<String> lowercaseIncompatibilities = new LinkedList<String>();
        for (String key : getIncompatibilities()) {
            lowercaseIncompatibilities.add(key.toLowerCase());
        }
        return lowercaseIncompatibilities.contains(server.toLowerCase());
    }

    /**
     * Get all listed incompatibilities for this Server
     *
     * @return Incompatibility List
     */
    public List<String> getIncompatibilities() {
        return new LinkedList<String>(raw.getRawStringList("incompatible-list"));
    }

    /**
     * Get all listed incompatibilities for this Server
     *
     * @param callback Incompatibility List
     */
    public void getIncompatibilities(Callback<List<SubServer>> callback) {
        if (Util.isNull(callback)) throw new NullPointerException();
        StackTraceElement[] origin = new Exception().getStackTrace();
        Runnable run = () -> {
            try {
                callback.run(incompatibilities);
            } catch (Throwable e) {
                Throwable ew = new InvocationTargetException(e);
                ew.setStackTrace(origin);
                ew.printStackTrace();
            }
        };

        if (incompatibilities == null) {
            LinkedList<String> incompatableNames = new LinkedList<String>();
            for (String subserver : raw.getRawStringList("incompatible-list")) incompatableNames.add(subserver.toLowerCase());
            SubAPI.getInstance().getSubServers(subservers -> {
                LinkedList<SubServer> incompatibilities = new LinkedList<SubServer>();
                for (SubServer subserver : subservers.values())
                    if (incompatableNames.contains(subserver.getName().toLowerCase()))
                        incompatibilities.add(subserver);
                this.incompatibilities = incompatibilities;
                run.run();
            });
        } else {
            run.run();
        }
    }

    /**
     * Get incompatibility issues this server currently has
     *
     * @return Current Incompatibility List
     */
    public List<String> getCurrentIncompatibilities() {
        return new LinkedList<String>(raw.getRawStringList("incompatible"));
    }

    /**
     * Get incompatibility issues this server currently has
     *
     * @param callback Current Incompatibility List
     */
    public void getCurrentIncompatibilities(Callback<List<SubServer>> callback) {
        getIncompatibilities(incompatibilities -> {
            LinkedList<String> incompatableNames = new LinkedList<String>();
            for (String subserver : raw.getRawStringList("incompatible")) incompatableNames.add(subserver.toLowerCase());

            LinkedList<SubServer> current = new LinkedList<SubServer>();
            for (SubServer subserver : incompatibilities)
                if (incompatableNames.contains(subserver.getName().toLowerCase()))
                    current.add(subserver);
            callback.run(current);
        });
    }
}

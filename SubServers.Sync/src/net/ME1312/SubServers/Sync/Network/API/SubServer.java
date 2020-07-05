package net.ME1312.SubServers.Sync.Network.API;

import net.ME1312.Galaxi.Library.Callback.Callback;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Client.SubDataClient;
import net.ME1312.SubServers.Sync.Network.Packet.*;
import net.ME1312.SubServers.Sync.SubAPI;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

public class SubServer extends Server {
    private List<SubServer> incompatibilities = null;
    private List<SubServer> currentIncompatibilities = null;
    private Host host = null;

    /**
     * SubServer Stop Action Class
     */
    public enum StopAction {
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
     * Create an API representation of a Server
     *
     * @param raw JSON representation of the Server
     */
    public SubServer(ObjectMap<String> raw) {
        super(raw);
    }

    /**
     * Create an API representation of a Server
     *
     * @param host Host
     * @param raw JSON representation of the Server
     */
    SubServer(Host host, ObjectMap<String> raw) {
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
        currentIncompatibilities = null;
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
        ((SubDataClient) SubAPI.getInstance().getSubDataNetwork()[0]).sendPacket(new PacketStartServer(player, getName(), data -> {
            try {
                response.run(data.getInt(0x0001));
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
        ((SubDataClient) SubAPI.getInstance().getSubDataNetwork()[0]).sendPacket(new PacketStopServer(player, getName(), false, data -> {
            try {
                response.run(data.getInt(0x0001));
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
        ((SubDataClient) SubAPI.getInstance().getSubDataNetwork()[0]).sendPacket(new PacketStopServer(player, getName(), true, data -> {
            try {
                response.run(data.getInt(0x0001));
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
        ((SubDataClient) SubAPI.getInstance().getSubDataNetwork()[0]).sendPacket(new PacketCommandServer(player, getName(), command, data -> {
            try {
                response.run(data.getInt(0x0001));
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
     * Edits the Server
     *
     * @param player Player Editing
     * @param edit Edits
     * @param response Negative Response Code -or- Positive Success Status
     */
    public void edit(UUID player, ObjectMap<String> edit, Callback<Integer> response) {
        edit(player, edit, false, response);
    }

    /**
     * Edits the Server
     *
     * @param edit Edits
     * @param response Negative Response Code -or- Positive Success Status
     */
    public void edit(ObjectMap<String> edit, Callback<Integer> response) {
        edit(null, edit, response);
    }

    /**
     * Edits the Server
     *
     * @param player Player Editing
     * @param edit Edits
     */
    public void edit(UUID player, ObjectMap<String> edit) {
        edit(player, edit, i -> {});
    }

    /**
     * Edits the Server
     *
     * @param edit Edits
     */
    public void edit(ObjectMap<String> edit) {
        edit(null, edit);
    }

    /**
     * Edits the Server (& Saves Changes)
     *
     * @param player Player Editing
     * @param edit Edits
     * @param response Negative Response Code -or- Positive Success Status
     */
    public void permaEdit(UUID player, ObjectMap<String> edit, Callback<Integer> response) {
        edit(player, edit, true, response);
    }

    /**
     * Edits the Server (& Saves Changes)
     *
     * @param edit Edits
     * @param response Negative Response Code -or- Positive Success Status
     */
    public void permaEdit(ObjectMap<String> edit, Callback<Integer> response) {
        permaEdit(null, edit, response);
    }

    /**
     * Edits the Server (& Saves Changes)
     *
     * @param player Player Editing
     * @param edit Edits
     */
    public void permaEdit(UUID player, ObjectMap<String> edit) {
        permaEdit(player, edit, i -> {});
    }

    /**
     * Edits the Server (& Saves Changes)
     *
     * @param edit Edits
     */
    public void permaEdit(ObjectMap<String> edit) {
        permaEdit(null, edit);
    }

    private void edit(UUID player, ObjectMap<String> edit, boolean perma, Callback<Integer> response) {
        if (Util.isNull(response)) throw new NullPointerException();
        StackTraceElement[] origin = new Exception().getStackTrace();
        ((SubDataClient) SubAPI.getInstance().getSubDataNetwork()[0]).sendPacket(new PacketEditServer(player, getName(), edit, perma, data -> {
            try {
                if (data.getInt(0x0001) != 0) {
                    response.run(data.getInt(0x0001) * -1);
                } else {
                    response.run(data.getInt(0x0002));
                }
            } catch (Throwable e) {
                Throwable ew = new InvocationTargetException(e);
                ew.setStackTrace(origin);
                ew.printStackTrace();
            }
        }));
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
     * If the Server is Online<br>
     * <b>This method can only be true when a SubData connection is made!</b>
     *
     * @return Online Status
     */
    public boolean isOnline() {
        return raw.getBoolean("online");
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
     * Grabs the Template this Server was created from
     *
     * @return The Template
     */
    public String getTemplate() {
        return raw.getRawString("template");
    }

    /**
     * Is this Server Available?
     *
     * @return Availability Status
     */
    public boolean isAvailable() {
        return raw.getBoolean("available");
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
     * Set if the Server is Enabled
     *
     * @param value Value
     */
    public void setEnabled(boolean value) {
        setEnabled(value, b -> {});
    }

    /**
     * Set if the Server is Enabled
     *
     * @param value Value
     * @param response Success Status
     */
    public void setEnabled(boolean value, Callback<Boolean> response) {
        if (Util.isNull(value, response)) throw new NullPointerException();
        ObjectMap<String> edit = new ObjectMap<String>();
        edit.set("enabled", value);
        edit(edit, r -> {
            if (r > 0) raw.set("enabled", value);
            response.run(r > 0);
        });
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
     * Set if the Server is Logging
     *
     * @param value Value
     */
    public void setLogging(boolean value) {
        setLogging(value, b -> {});
    }

    /**
     * Set if the Server is Logging
     *
     * @param value Value
     * @param response Success Status
     */
    public void setLogging(boolean value, Callback<Boolean> response) {
        if (Util.isNull(value, response)) throw new NullPointerException();
        ObjectMap<String> edit = new ObjectMap<String>();
        edit.set("log", value);
        edit(edit, r -> {
            if (r > 0) raw.set("log", value);
            response.run(r > 0);
        });
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
     * Set the Command that Stops the Server
     *
     * @param value Value
     */
    public void setStopCommand(String value) {
        setStopCommand(value, b -> {});
    }

    /**
     * Set the Command that Stops the Server
     *
     * @param value Value
     * @param response Success Status
     */
    public void setStopCommand(String value, Callback<Boolean> response) {
        if (Util.isNull(value, response)) throw new NullPointerException();
        ObjectMap<String> edit = new ObjectMap<String>();
        edit.set("stop-cmd", value);
        edit(edit, r -> {
            if (r > 0) raw.set("stop-cmd", value);
            response.run(r > 0);
        });
    }

    /**
     * Get the action the Server will take when it stops
     *
     * @return Stop Action
     */
    public StopAction getStopAction() {
        return Util.getDespiteException(() -> StopAction.valueOf(raw.getRawString("stop-action").toUpperCase().replace('-', '_').replace(' ', '_')), StopAction.NONE);
    }

    /**
     * Set the action the Server will take when it stops
     *
     * @param action Stop Action
     */
    public void setStopAction(StopAction action) {
        setStopAction(action, b -> {});
    }

    /**
     * Set the action the Server will take when it stops
     *
     * @param action Stop Action
     * @param response Success Status
     */
    public void setStopAction(StopAction action, Callback<Boolean> response) {
        if (Util.isNull(action, response)) throw new NullPointerException();
        ObjectMap<String> edit = new ObjectMap<String>();
        edit.set("stop-action", action.toString());
        edit(edit, r -> {
            if (r > 0) raw.set("stop-action", action.toString());
            response.run(r > 0);
        });
    }

    /**
     * Toggles compatibility with other Servers
     *
     * @param server SubServer to toggle
     */
    public void toggleCompatibility(String server) {
        toggleCompatibility(server, b -> {});
    }

    /**
     * Toggles compatibility with other Servers
     *
     * @param server SubServer to toggle
     */
    public void toggleCompatibility(String server, Callback<Boolean> response) {
        if (Util.isNull(server, response)) throw new NullPointerException();
        ArrayList<String> value = new ArrayList<String>();
        value.addAll(getIncompatibilities());
        if (!value.contains(server)) value.add(server);
        else value.remove(server);

        ObjectMap<String> edit = new ObjectMap<String>();
        edit.set("incompatible", value);
        edit(edit, r -> {
            if (r > 0) raw.set("incompatible", value);
            response.run(r > 0);
        });
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
            LinkedList<String> incompatible = new LinkedList<String>();
            for (String subserver : raw.getRawStringList("incompatible-list")) incompatible.add(subserver.toLowerCase());
            ((SubDataClient) SubAPI.getInstance().getSubDataNetwork()[0]).sendPacket(new PacketDownloadServerInfo(incompatible, data -> {
                LinkedList<SubServer> incompatibilities = new LinkedList<SubServer>();
                for (String server : data.getKeys()) {
                    if (data.getMap(server).getRawString("type", "Server").equals("SubServer")) incompatibilities.add(new SubServer(data.getMap(server)));
                }

                this.incompatibilities = incompatibilities;
                run.run();
            }));
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
        if (Util.isNull(callback)) throw new NullPointerException();
        StackTraceElement[] origin = new Exception().getStackTrace();
        Runnable run = () -> {
            try {
                callback.run(currentIncompatibilities);
            } catch (Throwable e) {
                Throwable ew = new InvocationTargetException(e);
                ew.setStackTrace(origin);
                ew.printStackTrace();
            }
        };

        if (currentIncompatibilities == null) {
            LinkedList<String> incompatible = new LinkedList<String>();
            for (String subserver : raw.getRawStringList("incompatible")) incompatible.add(subserver.toLowerCase());
            ((SubDataClient) SubAPI.getInstance().getSubDataNetwork()[0]).sendPacket(new PacketDownloadServerInfo(incompatible, data -> {
                LinkedList<SubServer> incompatibilities = new LinkedList<SubServer>();
                for (String server : data.getKeys()) {
                    if (data.getMap(server).getRawString("type", "Server").equals("SubServer")) incompatibilities.add(new SubServer(data.getMap(server)));
                }

                this.currentIncompatibilities = incompatibilities;
                run.run();
            }));
        } else {
            run.run();
        }
    }

    /**
     * Sets the Display Name for this Server
     *
     * @param value Value (or null to reset)
     */
    public void setDisplayName(String value) {
        setMotd(value, b -> {});
    }

    /**
     * Sets the Display Name for this Server
     *
     * @param value Value (or null to reset)
     * @param response Success Status
     */
    public void setDisplayName(String value, Callback<Boolean> response) {
        if (Util.isNull(value, response)) throw new NullPointerException();
        ObjectMap<String> edit = new ObjectMap<String>();
        edit.set("display", (value == null)?"":value);
        edit(edit, r -> {
            if (r > 0) raw.set("display", (value == null)?getName():value);
            response.run(r > 0);
        });
    }

    /**
     * Add this Server to a Group
     *
     * @param value Group name
     */
    public void addGroup(String value) {
        addGroup(value, b -> {});
    }

    /**
     * Add this Server to a Group
     *
     * @param value Group name
     * @param response Success Status
     */
    public void addGroup(String value, Callback<Boolean> response) {
        if (Util.isNull(value, response)) throw new NullPointerException();
        ArrayList<String> v = new ArrayList<String>();
        v.addAll(getGroups());
        if (!v.contains(value)) v.add(value);

        ObjectMap<String> edit = new ObjectMap<String>();
        edit.set("group", v);
        edit(edit, r -> {
            if (r > 0) raw.set("group", v);
            response.run(r > 0);
        });
    }

    /**
     * Remove this Server from a Group
     *
     * @param value value Group name
     */
    public void removeGroup(String value) {
        removeGroup(value, b -> {});
    }

    /**
     * Remove this Server from a Group
     *
     * @param value value Group name
     * @param response Success Status
     */
    public void removeGroup(String value, Callback<Boolean> response) {
        if (Util.isNull(value, response)) throw new NullPointerException();
        ArrayList<UUID> v = new ArrayList<UUID>();
        v.addAll(getWhitelist());
        v.remove(value);

        ObjectMap<String> edit = new ObjectMap<String>();
        edit.set("group", v);
        edit(edit, r -> {
            if (r > 0) raw.set("group", v);
            response.run(r > 0);
        });
    }

    /**
     * Set if the server is hidden from players
     *
     * @param value Value
     */
    public void setHidden(boolean value) {
        setHidden(value, b -> {});
    }

    /**
     * Set if the server is hidden from players
     *
     * @param value Value
     * @param response Success Status
     */
    public void setHidden(boolean value, Callback<Boolean> response) {
        if (Util.isNull(value, response)) throw new NullPointerException();
        ObjectMap<String> edit = new ObjectMap<String>();
        edit.set("restricted", value);
        edit(edit, r -> {
            if (r > 0) raw.set("restricted", value);
            response.run(r > 0);
        });
    }

    /**
     * Sets the MOTD of the Server
     *
     * @param value Value
     */
    public void setMotd(String value) {
        setMotd(value, b -> {});
    }

    /**
     * Sets the MOTD of the Server
     *
     * @param value Value
     * @param response Success Status
     */
    public void setMotd(String value, Callback<Boolean> response) {
        if (Util.isNull(value, response)) throw new NullPointerException();
        ObjectMap<String> edit = new ObjectMap<String>();
        edit.set("motd", value);
        edit(edit, r -> {
            if (r > 0) raw.set("motd", value);
            response.run(r > 0);
        });
    }

    /**
     * Sets if the Server is Restricted
     *
     * @param value Value
     */
    public void setRestricted(boolean value) {
        setRestricted(value, b -> {});
    }

    /**
     * Sets if the Server is Restricted
     *
     * @param value Value
     * @param response Success Status
     */
    public void setRestricted(boolean value, Callback<Boolean> response) {
        if (Util.isNull(value, response)) throw new NullPointerException();
        ObjectMap<String> edit = new ObjectMap<String>();
        edit.set("restricted", value);
        edit(edit, r -> {
            if (r > 0) raw.set("restricted", value);
            response.run(r > 0);
        });
    }

    /**
     * Add a player to the whitelist (for use with restricted servers)
     *
     * @param player Player to add
     */
    public void whitelist(UUID player) {
        whitelist(player, b -> {});
    }

    /**
     * Add a player to the whitelist (for use with restricted servers)
     *
     * @param player Player to add
     * @param response Success Status
     */
    public void whitelist(UUID player, Callback<Boolean> response) {
        if (Util.isNull(player, response)) throw new NullPointerException();
        ArrayList<UUID> value = new ArrayList<UUID>();
        value.addAll(getWhitelist());
        if (!value.contains(player)) value.add(player);

        ObjectMap<String> edit = new ObjectMap<String>();
        edit.set("whitelist", value);
        edit(edit, r -> {
            if (r > 0) raw.set("whitelist", value);
            response.run(r > 0);
        });
    }

    /**
     * Remove a player to the whitelist
     *
     * @param player Player to remove
     */
    public void unwhitelist(UUID player) {
        unwhitelist(player, b -> {});
    }

    /**
     * Remove a player to the whitelist
     *
     * @param player Player to remove
     * @param response Success Status
     */
    public void unwhitelist(UUID player, Callback<Boolean> response) {
        if (Util.isNull(player, response)) throw new NullPointerException();
        ArrayList<UUID> value = new ArrayList<UUID>();
        value.addAll(getWhitelist());
        value.remove(player);

        ObjectMap<String> edit = new ObjectMap<String>();
        edit.set("whitelist", value);
        edit(edit, r -> {
            if (r > 0) raw.set("whitelist", value);
            response.run(r > 0);
        });
    }
}

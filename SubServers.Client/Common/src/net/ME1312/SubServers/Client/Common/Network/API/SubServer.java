package net.ME1312.SubServers.Client.Common.Network.API;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Try;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Client.DataClient;
import net.ME1312.SubServers.Client.Common.ClientAPI;
import net.ME1312.SubServers.Client.Common.Network.Packet.*;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * Simplified SubServer Data Class
 */
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
     * Create an API representation of a SubServer
     *
     * @param raw JSON representation of the SubServer
     */
    public SubServer(ObjectMap<String> raw) {
        super(null, raw);
    }

    /**
     * Create an API representation of a SubServer
     *
     * @param client SubData connection
     * @param raw JSON representation of the SubServer
     */
    SubServer(DataClient client, ObjectMap<String> raw) {
        super(client, raw);
    }


    /**
     * Create an API representation of a SubServer
     *
     * @param host Host
     * @param raw JSON representation of the SubServer
     */
    SubServer(Host host, ObjectMap<String> raw) {
        this(host.client, raw);
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
    public void start(UUID player, IntConsumer response) {
        Util.nullpo(response);
        StackTraceElement[] origin = new Exception().getStackTrace();
        client().sendPacket(new PacketStartServer(player, getName(), data -> {
            try {
                response.accept(data.getInt(0x0001));
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
    public void start(IntConsumer response) {
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
    public void stop(UUID player, IntConsumer response) {
        Util.nullpo(response);
        StackTraceElement[] origin = new Exception().getStackTrace();
        client().sendPacket(new PacketStopServer(player, getName(), false, data -> {
            try {
                response.accept(data.getInt(0x0001));
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
    public void stop(IntConsumer response) {
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
    public void terminate(UUID player, IntConsumer response) {
        Util.nullpo(response);
        StackTraceElement[] origin = new Exception().getStackTrace();
        client().sendPacket(new PacketStopServer(player, getName(), true, data -> {
            try {
                response.accept(data.getInt(0x0001));
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
    public void terminate(IntConsumer response) {
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
     * Edits the Server
     *
     * @param player Player Editing
     * @param edit Edits
     * @param response Negative Response Code -or- Positive Success Status
     */
    public void edit(UUID player, ObjectMap<String> edit, IntConsumer response) {
        edit(player, edit, false, response);
    }

    /**
     * Edits the Server
     *
     * @param edit Edits
     * @param response Negative Response Code -or- Positive Success Status
     */
    public void edit(ObjectMap<String> edit, IntConsumer response) {
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
    public void permaEdit(UUID player, ObjectMap<String> edit, IntConsumer response) {
        edit(player, edit, true, response);
    }

    /**
     * Edits the Server (& Saves Changes)
     *
     * @param edit Edits
     * @param response Negative Response Code -or- Positive Success Status
     */
    public void permaEdit(ObjectMap<String> edit, IntConsumer response) {
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

    private void edit(UUID player, ObjectMap<String> edit, boolean perma, IntConsumer response) {
        Util.nullpo(response);
        StackTraceElement[] origin = new Exception().getStackTrace();
        client().sendPacket(new PacketEditServer(player, getName(), edit, perma, data -> {
            try {
                if (data.getInt(0x0001) != 0) {
                    response.accept(data.getInt(0x0001) * -1);
                } else {
                    response.accept(data.getInt(0x0002));
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
        return raw.getString("host");
    }

    /**
     * Grabs the Host of the Server
     *
     * @param callback The Host
     */
    public void getHost(Consumer<Host> callback) {
        Util.nullpo(callback);
        StackTraceElement[] origin = new Exception().getStackTrace();
        Runnable run = () -> {
            try {
                callback.accept(host);
            } catch (Throwable e) {
                Throwable ew = new InvocationTargetException(e);
                ew.setStackTrace(origin);
                ew.printStackTrace();
            }
        };

        if (host == null || !host.getName().equalsIgnoreCase(raw.getString("host"))) {
            ClientAPI.getInstance().getHost(raw.getString("host"), host -> {
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
        return raw.getString("template");
    }

    /**
     * Grabs the Template this Server was created from
     *
     * @param callback  The Template
     */
    public void getTemplate(Consumer<SubCreator.ServerTemplate> callback) {
        Util.nullpo(callback);
        String name = getTemplate();
        if (name == null) {
            callback.accept(null);
        } else getHost(host -> {
            callback.accept(host.getCreator().getTemplate(name));
        });
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
    public void setEnabled(boolean value, Consumer<Boolean> response) {
        Util.nullpo(value, response);
        ObjectMap<String> edit = new ObjectMap<String>();
        edit.set("enabled", value);
        edit(edit, r -> {
            if (r > 0) raw.set("enabled", value);
            response.accept(r > 0);
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
    public void setLogging(boolean value, Consumer<Boolean> response) {
        Util.nullpo(value, response);
        ObjectMap<String> edit = new ObjectMap<String>();
        edit.set("log", value);
        edit(edit, r -> {
            if (r > 0) raw.set("log", value);
            response.accept(r > 0);
        });
    }

    /**
     * Get the Server Directory Path
     *
     * @return Server Directory Path
     */
    public String getPath() {
        return raw.getString("dir");
    }

    /**
     * Get the Server's Executable String
     *
     * @return Executable String
     */
    public String getExecutable() {
        return raw.getString("exec");
    }

    /**
     * Grab the Command to Stop the Server
     *
     * @return Stop Command
     */
    public String getStopCommand() {
        return raw.getString("stop-cmd");
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
    public void setStopCommand(String value, Consumer<Boolean> response) {
        Util.nullpo(value, response);
        ObjectMap<String> edit = new ObjectMap<String>();
        edit.set("stop-cmd", value);
        edit(edit, r -> {
            if (r > 0) raw.set("stop-cmd", value);
            response.accept(r > 0);
        });
    }

    /**
     * Get the action the Server will take when it stops
     *
     * @return Stop Action
     */
    public StopAction getStopAction() {
        return Try.all.get(() -> StopAction.valueOf(raw.getString("stop-action").toUpperCase().replace('-', '_').replace(' ', '_')), StopAction.NONE);
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
    public void setStopAction(StopAction action, Consumer<Boolean> response) {
        Util.nullpo(action, response);
        ObjectMap<String> edit = new ObjectMap<String>();
        edit.set("stop-action", action.toString());
        edit(edit, r -> {
            if (r > 0) raw.set("stop-action", action.toString());
            response.accept(r > 0);
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
    public void toggleCompatibility(String server, Consumer<Boolean> response) {
        Util.nullpo(server, response);
        ArrayList<String> value = new ArrayList<String>();
        value.addAll(getIncompatibilities());
        if (!value.contains(server)) value.add(server);
        else value.remove(server);

        ObjectMap<String> edit = new ObjectMap<String>();
        edit.set("incompatible", value);
        edit(edit, r -> {
            if (r > 0) raw.set("incompatible", value);
            response.accept(r > 0);
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
        return new LinkedList<String>(raw.getStringList("incompatible-list"));
    }

    /**
     * Get all listed incompatibilities for this Server
     *
     * @param callback Incompatibility List
     */
    public void getIncompatibilities(Consumer<List<SubServer>> callback) {
        Util.nullpo(callback);
        StackTraceElement[] origin = new Exception().getStackTrace();
        Runnable run = () -> {
            try {
                callback.accept(incompatibilities);
            } catch (Throwable e) {
                Throwable ew = new InvocationTargetException(e);
                ew.setStackTrace(origin);
                ew.printStackTrace();
            }
        };

        if (incompatibilities == null) {
            LinkedList<String> incompatible = new LinkedList<String>();
            for (String subserver : raw.getStringList("incompatible-list")) incompatible.add(subserver.toLowerCase());
            client().sendPacket(new PacketDownloadServerInfo(incompatible, data -> {
                LinkedList<SubServer> incompatibilities = new LinkedList<SubServer>();
                for (String server : data.getKeys()) {
                    if (data.getMap(server).getString("type", "Server").equals("SubServer")) incompatibilities.add(new SubServer(data.getMap(server)));
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
        return new LinkedList<String>(raw.getStringList("incompatible"));
    }

    /**
     * Get incompatibility issues this server currently has
     *
     * @param callback Current Incompatibility List
     */
    public void getCurrentIncompatibilities(Consumer<List<SubServer>> callback) {
        Util.nullpo(callback);
        StackTraceElement[] origin = new Exception().getStackTrace();
        Runnable run = () -> {
            try {
                callback.accept(currentIncompatibilities);
            } catch (Throwable e) {
                Throwable ew = new InvocationTargetException(e);
                ew.setStackTrace(origin);
                ew.printStackTrace();
            }
        };

        if (currentIncompatibilities == null) {
            LinkedList<String> incompatible = new LinkedList<String>();
            for (String subserver : raw.getStringList("incompatible")) incompatible.add(subserver.toLowerCase());
            client().sendPacket(new PacketDownloadServerInfo(incompatible, data -> {
                LinkedList<SubServer> incompatibilities = new LinkedList<SubServer>();
                for (String server : data.getKeys()) {
                    if (data.getMap(server).getString("type", "Server").equals("SubServer")) incompatibilities.add(new SubServer(data.getMap(server)));
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
    public void setDisplayName(String value, Consumer<Boolean> response) {
        Util.nullpo(value, response);
        ObjectMap<String> edit = new ObjectMap<String>();
        edit.set("display", (value == null)?"":value);
        edit(edit, r -> {
            if (r > 0) raw.set("display", (value == null)?getName():value);
            response.accept(r > 0);
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
    public void addGroup(String value, Consumer<Boolean> response) {
        Util.nullpo(value, response);
        ArrayList<String> v = new ArrayList<String>();
        v.addAll(getGroups());
        if (!v.contains(value)) v.add(value);

        ObjectMap<String> edit = new ObjectMap<String>();
        edit.set("group", v);
        edit(edit, r -> {
            if (r > 0) raw.set("group", v);
            response.accept(r > 0);
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
    public void removeGroup(String value, Consumer<Boolean> response) {
        Util.nullpo(value, response);
        ArrayList<UUID> v = new ArrayList<UUID>();
        v.addAll(getWhitelist());
        v.remove(value);

        ObjectMap<String> edit = new ObjectMap<String>();
        edit.set("group", v);
        edit(edit, r -> {
            if (r > 0) raw.set("group", v);
            response.accept(r > 0);
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
    public void setHidden(boolean value, Consumer<Boolean> response) {
        Util.nullpo(value, response);
        ObjectMap<String> edit = new ObjectMap<String>();
        edit.set("restricted", value);
        edit(edit, r -> {
            if (r > 0) raw.set("restricted", value);
            response.accept(r > 0);
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
    public void setMotd(String value, Consumer<Boolean> response) {
        Util.nullpo(value, response);
        ObjectMap<String> edit = new ObjectMap<String>();
        edit.set("motd", value);
        edit(edit, r -> {
            if (r > 0) raw.set("motd", value);
            response.accept(r > 0);
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
    public void setRestricted(boolean value, Consumer<Boolean> response) {
        Util.nullpo(value, response);
        ObjectMap<String> edit = new ObjectMap<String>();
        edit.set("restricted", value);
        edit(edit, r -> {
            if (r > 0) raw.set("restricted", value);
            response.accept(r > 0);
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
    public void whitelist(UUID player, Consumer<Boolean> response) {
        Util.nullpo(player, response);
        ArrayList<UUID> value = new ArrayList<UUID>();
        value.addAll(getWhitelist());
        if (!value.contains(player)) value.add(player);

        ObjectMap<String> edit = new ObjectMap<String>();
        edit.set("whitelist", value);
        edit(edit, r -> {
            if (r > 0) raw.set("whitelist", value);
            response.accept(r > 0);
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
    public void unwhitelist(UUID player, Consumer<Boolean> response) {
        Util.nullpo(player, response);
        ArrayList<UUID> value = new ArrayList<UUID>();
        value.addAll(getWhitelist());
        value.remove(player);

        ObjectMap<String> edit = new ObjectMap<String>();
        edit.set("whitelist", value);
        edit(edit, r -> {
            if (r > 0) raw.set("whitelist", value);
            response.accept(r > 0);
        });
    }
}

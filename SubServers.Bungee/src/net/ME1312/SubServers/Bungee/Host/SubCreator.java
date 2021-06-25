package net.ME1312.SubServers.Bungee.Host;

import net.ME1312.Galaxi.Library.Callback.Callback;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.SubServers.Bungee.Library.Exception.InvalidTemplateException;
import net.ME1312.SubServers.Bungee.SubAPI;

import com.google.common.collect.Range;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.*;

/**
 * SubCreator Layout Class
 */
public abstract class SubCreator {
    public static class ServerTemplate {
        private final boolean dynamic;
        private String name;
        private String nick = null;
        private boolean enabled;
        private boolean internal;
        private String icon;
        private File directory;
        private ServerType type;
        private ObjectMap<String> build;
        private ObjectMap<String> options;

        /**
         * Create a SubCreator Template
         *
         * @param name Template Name
         * @param enabled Template Enabled Status
         * @param icon Template Item Icon Name
         * @param directory Template Directory
         * @param build Build Options
         * @param options Configuration Options
         */
        public ServerTemplate(String name, boolean enabled, String icon, File directory, ObjectMap<String> build, ObjectMap<String> options) {
            this(name, enabled, false, icon, directory, build, options, true);
        }

        private ServerTemplate(String name, boolean enabled, boolean internal, String icon, File directory, ObjectMap<String> build, ObjectMap<String> options, boolean dynamic) {
            if (Util.isNull(name, enabled, directory, build, options)) throw new NullPointerException();
            if (name.contains(" ")) throw new InvalidTemplateException("Template names cannot have spaces: " + name);
            this.name = name;
            this.enabled = enabled;
            this.internal = internal;
            this.icon = icon;
            this.directory = directory;
            this.type = (build.contains("Server-Type"))?ServerType.valueOf(build.getRawString("Server-Type").toUpperCase()):ServerType.CUSTOM;
            this.build = build;
            this.options = options;
            this.dynamic = dynamic;
        }

        /**
         * Get the Name of this Template
         *
         * @return Template Name
         */
        public String getName() {
            return name;
        }

        /**
         * Get the Display Name of this Template
         *
         * @return Display Name
         */
        public String getDisplayName() {
            return (nick == null)?getName():nick;
        }

        /**
         * Sets the Display Name for this Template
         *
         * @param value Value (or null to reset)
         */
        public void setDisplayName(String value) {
            if (value == null || value.length() == 0 || getName().equals(value)) {
                this.nick = null;
            } else {
                this.nick = value;
            }
        }

        /**
         * Get the Enabled Status of this Template
         *
         * @return Enabled Status
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * Set the Enabled Status of this Template
         *
         * @param value Value
         */
        public void setEnabled(boolean value) {
            enabled = value;
        }

        /**
         * Get if this Template is for Internal use only
         *
         * @return Internal Status
         */
        public boolean isInternal() {
            return internal;
        }

        /**
         * Get the Item Icon for this Template
         *
         * @return Item Icon Name/ID
         */
        public String getIcon() {
            return icon;
        }

        /**
         * Set the Item Icon for this Template
         *
         * @param value Value
         */
        public void setIcon(String value) {
            icon = value;
        }

        /**
         * Get the Directory for this Template
         *
         * @return Directory
         */
        public File getDirectory() {
            return directory;
        }

        /**
         * Get the Type of this Template
         *
         * @return Template Type
         */
        public ServerType getType() {
            return type;
        }

        /**
         * Get whether this Template requires the Version argument
         *
         * @return Version Requirement
         */
        public boolean requiresVersion() {
            return getBuildOptions().getBoolean("Require-Version", false);
        }

        /**
         * Get whether this Template can be used to update it's servers
         *
         * @return Updatable Status
         */
        public boolean canUpdate() {
            return getBuildOptions().getBoolean("Can-Update", false);
        }

        /**
         * Get whether this Template was generated by a SubCreator instance
         *
         * @return Dynamic Status
         */
        public boolean isDynamic() {
            return dynamic;
        }

        /**
         * Get the Build Options for this Template
         *
         * @return Build Options
         */
        public ObjectMap<String> getBuildOptions() {
            return build;
        }

        /**
         * Get the Configuration Options for this Template
         *
         * @return Configuration Options
         */
        public ObjectMap<String> getConfigOptions() {
            return options;
        }


        public ObjectMap<String> forSubData() {
            ObjectMap<String> tinfo = new ObjectMap<String>();
            tinfo.set("enabled", isEnabled());
            tinfo.set("name", getName());
            tinfo.set("display", getDisplayName());
            tinfo.set("icon", getIcon());
            tinfo.set("type", getType().toString());
            tinfo.set("version-req", requiresVersion());
            tinfo.set("can-update", canUpdate());
            return tinfo;
        }
    }
    public enum ServerType {
        SPIGOT,
        VANILLA,
        FORGE,
        SPONGE,
        CUSTOM;

        @Override
        public String toString() {
            return super.toString().substring(0, 1).toUpperCase()+super.toString().substring(1).toLowerCase();
        }
    }

    /**
     * Create a SubServer
     *
     * @param player Player Creating
     * @param name Server Name
     * @param template Server Template
     * @param version Server Version (may be null)
     * @param port Server Port Number (null to auto-select)
     * @param callback Callback
     * @return Success Status
     */
    public abstract boolean create(UUID player, String name, ServerTemplate template, Version version, Integer port, Callback<SubServer> callback);

    /**
     * Create a SubServer
     *
     * @param player Player Creating
     * @param name Server Name
     * @param template Server Template
     * @param version Server Version (may be null)
     * @param port Server Port Number (null to auto-select)
     * @return Success Status
     */
    public boolean create(UUID player, String name, ServerTemplate template, Version version, Integer port) {
        return create(player, name, template, version, port, null);
    }

    /**
     * Create a SubServer
     *
     * @param name Server Name
     * @param template Server Template
     * @param version Server Version (may be null)
     * @param port Server Port Number (null to auto-select)
     * @param callback Callback
     * @return Success Status
     */
    public boolean create(String name, ServerTemplate template, Version version, Integer port, Callback<SubServer> callback) {
        return create(null, name, template, version, port, callback);
    }

    /**
     * Create a SubServer
     *
     * @param name Server Name
     * @param template Server Template
     * @param version Server Version (may be null)
     * @param port Server Port Number (null to auto-select)
     * @return Success Status
     */
    public boolean create(String name, ServerTemplate template, Version version, Integer port) {
        return create(null, name, template, version, port);
    }

    /**
     * Update a SubServer
     *
     * @param player Player Updating
     * @param server Server to Update
     * @param template Server Template
     * @param version Server Version (may be null)
     * @param callback Callback
     * @return Success Status
     */
    public abstract boolean update(UUID player, SubServer server, ServerTemplate template, Version version, Callback<Boolean> callback);

    /**
     * Update a SubServer
     *
     * @param player Player Updating
     * @param server Server to Update
     * @param template Server Template
     * @param version Server Version (may be null)
     * @return Success Status
     */
    public boolean update(UUID player, SubServer server, ServerTemplate template, Version version) {
        return update(player, server, template, version, null);
    }

    /**
     * Update a SubServer
     *
     * @param server Server to Update
     * @param template Server Template
     * @param version Server Version (may be null)
     * @param callback Callback
     * @return Success Status
     */
    public boolean update(SubServer server, ServerTemplate template, Version version, Callback<Boolean> callback) {
        return update(null, server, template, version, callback);
    }

    /**
     * Update a SubServer
     *
     * @param server Server to Update
     * @param template Server Template
     * @param version Server Version (may be null)
     * @return Success Status
     */
    public boolean update(SubServer server, ServerTemplate template, Version version) {
        return update(null, server, template, version);
    }

    /**
     * Update a SubServer
     *
     * @param player Player Updating
     * @param server Server to Update
     * @param version Server Version (may be null)
     * @return Success Status
     */
    public boolean update(UUID player, SubServer server, Version version) {
        return update(player, server, null, version);
    }

    /**
     * Update a SubServer
     *
     * @param server Server to Update
     * @param version Server Version (may be null)
     * @return Success Status
     */
    public boolean update(SubServer server, Version version) {
        return update(null, server, version);
    }

    /**
     * Terminate All SubCreator Instances on this host
     */
    public abstract void terminate();

    /**
     * Terminate a SubCreator Instance
     *
     * @param name Name of current creating server
     */
    public abstract void terminate(String name);

    /**
     * Wait for All SubCreator Instances to Finish
     *
     * @throws InterruptedException
     */
    public abstract void waitFor() throws InterruptedException;

    /**
     * Wait for SubCreator to Finish
     *
     * @param name Name of current creating server
     * @throws InterruptedException
     */
    public abstract void waitFor(String name) throws InterruptedException;

    /**
     * Gets the host this creator belongs to
     *
     * @return Host
     */
    public abstract Host getHost();

    /**
     * Get the range of available port numbers
     *
     * @return Port Range
     */
    public abstract Range getPortRange();

    /**
     * Get the range of available port numbers
     *
     * @param value Value
     */
    public abstract void setPortRange(Range<Integer> value);

    /**
     * Gets the Git Bash install directory
     *
     * @return Git Bash Directory
     */
    public abstract String getBashDirectory();

    /**
     * Gets all loggers for All SubCreator Instances
     *
     * @return SubCreator Loggers
     */
    public abstract List<SubLogger> getLoggers();

    /**
     * Gets the Logger for a SubCreator Instance
     *
     * @param thread Thread ID
     * @return SubCreator Logger
     */
    public abstract SubLogger getLogger(String thread);

    /**
     * If the Creator is Logging to console
     *
     * @return Logging Status
     */
    public abstract boolean isLogging();

    /**
     * Set if the Creator is Logging
     *
     * @param value Value
     */
    public abstract void setLogging(boolean value);


    /**
     * Get a list of currently reserved Server names
     *
     * @return Reserved Names
     */
    public abstract List<String> getReservedNames();

    /**
     * Get a list of currently reserved Server ports
     *
     * @return Reserved Ports
     */
    public abstract List<Integer> getReservedPorts();

    /**
     * Check if a name has been reserved
     *
     * @param name Name to check
     * @return Reserved Status
     */
    public static boolean isReserved(String name) {
        boolean reserved = false;
        for (List<String> list : getAllReservedNames().values()) for (String reserve : list) {
            if (reserve.equalsIgnoreCase(name)) reserved = true;
        }
        return reserved;
    }

    /**
     * Check if an address has been reserved
     *
     * @param address Address to check
     * @return Reserved Status
     */
    public static boolean isReserved(InetSocketAddress address) {
        boolean reserved = false;
        for (InetSocketAddress list : getAllReservedAddresses()) {
            if (list.equals(address)) reserved = true;
        }
        return reserved;
    }

    /**
     * Get a list of all currently reserved Server names across all hosts
     *
     * @return All Reserved Names
     */
    public static Map<Host, List<String>> getAllReservedNames() {
        HashMap<Host, List<String>> names = new HashMap<Host, List<String>>();
        for (Host host : SubAPI.getInstance().getHosts().values()) names.put(host, host.getCreator().getReservedNames());
        return names;
    }

    /**
     * Get a list of all currently reserved Server names across all hosts
     *
     * @return All Reserved Names
     */
    public static List<InetSocketAddress> getAllReservedAddresses() {
        List<InetSocketAddress> addresses = new ArrayList<InetSocketAddress>();
        for (Server server : SubAPI.getInstance().getSubServers().values()) {
            addresses.add(server.getAddress());
        }
        for (Host host : SubAPI.getInstance().getHosts().values())
            for (int port : host.getCreator().getReservedPorts())
                addresses.add(new InetSocketAddress(host.getAddress(), port));
        return addresses;
    }

    /**
     * Gets the Templates that can be used in this SubCreator instance
     *
     * @return Template Map
     */
    public abstract Map<String, ServerTemplate> getTemplates();

    /**
     * Gets a SubCreator Template by name
     *
     * @param name Template Name
     * @return Template
     */
    public abstract ServerTemplate getTemplate(String name);

    /**
     * Create a SubCreator Template
     *
     * @param name Template Name
     * @param enabled Template Enabled Status
     * @param internal Template Internal Status
     * @param icon Template Item Icon Name
     * @param directory Template Directory
     * @param build Build Options
     * @param options Configuration Options
     */
    protected ServerTemplate loadTemplate(String name, boolean enabled, boolean internal, String icon, File directory, ObjectMap<String> build, ObjectMap<String> options) {
        return new ServerTemplate(name, enabled, internal, icon, directory, build, options, false);
    }

    /**
     * Reload SubCreator
     */
    public abstract void reload();
}

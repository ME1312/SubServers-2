package net.ME1312.SubServers.Client.Sponge.Graphic;
import net.ME1312.SubServers.Client.Sponge.Library.NamedContainer;
import net.ME1312.SubServers.Client.Sponge.Library.Util;
import net.ME1312.SubServers.Client.Sponge.Library.Version.Version;
import net.ME1312.SubServers.Client.Sponge.Network.API.Host;
import net.ME1312.SubServers.Client.Sponge.Network.API.SubServer;
import net.ME1312.SubServers.Client.Sponge.SubPlugin;

import java.util.*;

/**
 * GUI Renderer Layout Class
 */
public abstract class UIRenderer {
    protected static HashMap<String, Renderer<Host>> hostPlugins = new HashMap<String, Renderer<Host>>();
    protected static HashMap<String, Renderer<SubServer>> subserverPlugins = new HashMap<String, Renderer<SubServer>>();
    private NamedContainer<String, Integer> tdownload = null;
    private int download = -1;
    private final UUID player;
    private SubPlugin plugin;

    /**
     * Creates a new UIRenderer
     *
     * @param plugin SubPlugin
     * @param player Player
     */
    public UIRenderer(SubPlugin plugin, UUID player) {
        if (Util.isNull(plugin, player)) throw new NullPointerException();
        this.plugin = plugin;
        this.player = player;
    }

    /**
     * Opens a new window
     */
    public abstract void newUI();

    /**
     * Clears the Window History
     */
    public abstract void clearHistory();

    /**
     * If there is any Window History
     *
     * @return Window History Status
     */
    public abstract boolean hasHistory();

    /**
     * Reopens the current window
     */
    public abstract void reopen();

    /**
     * Reopens the previous window
     */
    public abstract void back();

    // TODO Re-Add GUI API Methods that belong here

    /**
     * Add Host Plugin
     *
     * @param handle Handle to bind
     * @param renderer Renderer
     */
    public static void addHostPlugin(String handle, Renderer<Host> renderer) {
        if (Util.isNull(handle, renderer)) throw new NullPointerException();
        hostPlugins.put(handle, renderer);
    }

    /**
     * Get Host Plugins
     *
     * @return Host Plugins
     */
    public static Map<String, Renderer<Host>> getHostPlugins() {
        return new HashMap<String, Renderer<Host>>(hostPlugins);
    }

    /**
     * Remove Host Plugin
     *
     * @param handle Handle
     */
    public static void removeHostPlugin(String handle) {
        if (Util.isNull(handle)) throw new NullPointerException();
        hostPlugins.remove(handle);
    }

    /**
     * Add SubServer Plugin
     *
     * @param handle Handle to bind
     * @param renderer Renderer
     */
    public static void addSubServerPlugin(String handle, Renderer<SubServer> renderer) {
        if (Util.isNull(handle, renderer)) throw new NullPointerException();
        subserverPlugins.put(handle, renderer);
    }

    /**
     * Get SubServer Plugins
     *
     * @return SubServer Plugins
     */
    public static Map<String, Renderer<SubServer>> getSubServerPlugins() {
        return new HashMap<String, Renderer<SubServer>>(subserverPlugins);
    }

    /**
     * Remove SubServer Plugin
     *
     * @param handle Handle
     */
    public static void removeSubServerPlugin(String handle) {
        if (Util.isNull(handle)) throw new NullPointerException();
        subserverPlugins.remove(handle);
    }

    /**
     * Opens the Host Menu
     *
     * @param page Page Number (starting from page 1)
     */
    public abstract void hostMenu(int page);

    /**
     * Opens Host/&lt;name&gt;
     *
     * @param host Host Name
     */
    public abstract void hostAdmin(String host);

    /**
     * Opens Host/&lt;name&gt;/Create
     *
     * @param options Creator Options
     */
    public abstract void hostCreator(CreatorOptions options);

    /**
     * Opens Host/&lt;name&gt;/Plugins
     *
     * @param host Host Name
     */
    public abstract void hostPlugin(int page, String host);

    /**
     * Opens the Group Menu
     *
     * @param page Page Number (starting from page 1)
     */
    public abstract void groupMenu(int page);

    /**
     * Opens the SubServer Menu
     *
     * @param page Page Number (starting from page 1)
     * @param host Host Name (or null to scan all hosts)
     */
    public abstract void serverMenu(int page, String host, String group);

    /**
     * Opens SubServer/&lt;name&gt;
     *
     * @param server SubServer Name
     */
    public abstract void subserverAdmin(String server);

    /**
     * Opens SubServer/&lt;name&gt;/Plugins
     *
     * @param server SubServer Name
     */
    public abstract void subserverPlugin(int page, String server);

    /**
     * Options Layout Class
     */
    public abstract static class Options {
        List<Runnable> history = new LinkedList<Runnable>();
        private boolean init = false;

        public boolean init() {
            if (!init) {
                init = true;
                return false;
            } else {
                return true;
            }
        }

        /**
         * If there is any undo history
         *
         * @return Undo History Status
         */
        public boolean hasHistory() {
            return !history.isEmpty();
        }

        /**
         * Reverts the last change
         */
        public void undo() {
            Runnable lastWindow = history.get(history.size() - 1);
            history.remove(history.size() - 1);
            lastWindow.run();
        }
    }

    /**
     * SubCreator Options Class
     */
    public static class CreatorOptions extends Options {
        private String host;
        private String name = null;
        private String template = null;
        private Version version = null;
        private int memory = 1024;
        private int port = -1;

        /**
         * Grabs a raw CreatorOptions instance
         *
         * @param host Host Name
         */
        public CreatorOptions(String host) {
            this.host = host;
        }

        /**
         * Gets the Host Name
         *
         * @return Host Name
         */
        public String getHost() {
            return this.host;
        }

        /**
         * Gets the Server Name
         *
         * @return Server Name
         */
        public String getName() {
            return name;
        }

        /**
         * Sets the Server Name
         *
         * @param value Value
         */
        public void setName(String value) {
            if (Util.isNull(value)) throw new NullPointerException();
            final String name = this.name;
            history.add(() -> this.name = name);
            this.name = value;
        }

        /**
         * Gets the Template
         *
         * @return Template
         */
        public String getTemplate() {
            return template;
        }

        /**
         * Sets the Template
         *
         * @param value Value
         */
        public void setTemplate(String value) {
            if (Util.isNull(value)) throw new NullPointerException();
            final String template = this.template;
            history.add(() -> this.template = template);
            this.template = value;
        }

        /**
         * Gets the Server Version
         *
         * @return Server Version
         */
        public Version getVersion() {
            return version;
        }

        /**
         * Sets the Server Version
         *
         * @param value Value
         */
        public void setVersion(Version value) {
            if (Util.isNull(value)) throw new NullPointerException();
            final Version version = this.version;
            history.add(() -> this.version = version);
            this.version = value;
        }

        /**
         * Gets the RAM Amount for the Server
         *
         * @return Server RAM Amount (in MB)
         */
        public int getMemory() {
            return memory;
        }

        /**
         * Sets the RAM AMount for the Server
         *
         * @param value Value (in MB)
         */
        public void setMemory(int value) {
            if (Util.isNull(value)) throw new NullPointerException();
            final int memory = this.memory;
            history.add(() -> this.memory = memory);
            this.memory = value;
        }

        /**
         * Gets the Port Number for the Server
         *
         * @return Server Port Number
         */
        public int getPort() {
            return port;
        }

        /**
         * Sets the Port Number for the Server
         *
         * @param value Value
         */
        public void setPort(int value) {
            if (Util.isNull(value)) throw new NullPointerException();
            final int port = this.port;
            history.add(() -> this.port = port);
            this.port = value;
        }
    }
}

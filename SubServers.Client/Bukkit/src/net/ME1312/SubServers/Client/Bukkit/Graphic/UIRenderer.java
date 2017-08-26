package net.ME1312.SubServers.Client.Bukkit.Graphic;

import net.ME1312.SubServers.Client.Bukkit.Library.Container;
import net.ME1312.SubServers.Client.Bukkit.Library.NamedContainer;
import net.ME1312.SubServers.Client.Bukkit.Library.Util;
import net.ME1312.SubServers.Client.Bukkit.Library.Version.Version;
import net.ME1312.SubServers.Client.Bukkit.Network.Packet.PacketCreateServer;
import net.ME1312.SubServers.Client.Bukkit.SubPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/**
 * GUI Renderer Layout Class
 */
public abstract class UIRenderer {
    protected static HashMap<String, Renderer> hostPlugins = new HashMap<String, Renderer>();
    protected static HashMap<String, Renderer> subserverPlugins = new HashMap<String, Renderer>();
    private NamedContainer<String, Integer> tdownload = null;
    private BukkitTask download = null;
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

    /**
     * Attempt to send a Title Message
     *
     * @param str Message
     * @return Success Status
     */
    public boolean sendTitle(String str) {
        return sendTitle(str, -1);
    }

    /**
     * Attempt to send a Title Message
     *
     * @param str Message
     * @param stay How long the message should stay
     * @return Success Status
     */
    public boolean sendTitle(String str, int stay) {
        return sendTitle(str, -1, stay, -1);
    }

    /**
     * Attempt to send a Title Message
     *
     * @param str Message
     * @param fadein FadeIn Transition length
     * @param stay How long the message should stay
     * @param fadeout FadeOut Transition length
     * @return Success Status
     */
    public boolean sendTitle(String str, int fadein, int stay, int fadeout) {
        if (Util.isNull(str, fadein, stay, fadeout)) throw new NullPointerException();
        if (Bukkit.getPluginManager().getPlugin("TitleManager") != null && plugin.config.get().getSection("Settings").getBoolean("Use-Title-Messages", true)) {
            String line1, line2;
            if (!str.startsWith("\n") && str.contains("\n")) {
                line1 = str.split("\\n")[0];
                line2 = str.split("\\n")[1];
            } else {
                line1 = str.replace("\n", "");
                line2 = ChatColor.RESET.toString();
            }
            try {
                io.puharesource.mc.titlemanager.api.TitleObject obj = io.puharesource.mc.titlemanager.api.TitleObject.class.getConstructor(String.class, String.class).newInstance(line1, line2);
                if (fadein >= 0) obj.setFadeIn(fadein);
                if (stay >= 0) obj.setStay(stay);
                if (fadeout >= 0) obj.setFadeOut(fadeout);
                obj.send(Bukkit.getPlayer(player));
                return true;
            } catch (Throwable e) {
                return false;
            }
        } else {
            return false;
        }
    }

    /**
     * Shows/Hides the Downloading Title Message
     *
     * @param subtitle Subtitle to display (or null to hide)
     */
    public void setDownloading(String subtitle) {
        if (subtitle != null && !(Bukkit.getPluginManager().getPlugin("TitleManager") != null && plugin.config.get().getSection("Settings").getBoolean("Use-Title-Messages", true))) {
            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                if (tdownload != null) Bukkit.getPlayer(player).sendMessage(plugin.lang.getSection("Lang").getColoredString("Interface.Generic.Downloading", '&').replace("$str$", subtitle));
                download = null;
            }, 30L);
        } if (subtitle != null && tdownload == null) {
            tdownload = new NamedContainer<String, Integer>(subtitle, 0);
            final Container<Integer> delay = new Container<Integer>(0);
            Bukkit.getScheduler().runTask(plugin, new Runnable() {
                @Override
                public void run() {
                    if (tdownload != null) {
                        String word = ChatColor.stripColor(plugin.lang.getSection("Lang").getColoredString("Interface.Generic.Downloading.Title", '&'));
                        int i = 0;
                        int start = (tdownload.get() - 3 < 0)?0: tdownload.get()-3;
                        int end = (tdownload.get() >= word.length())?word.length(): tdownload.get();
                        String str = plugin.lang.getSection("Lang").getColoredString((delay.get() > 7 && start == 0)?"Interface.Generic.Downloading.Title-Color-Alt":"Interface.Generic.Downloading.Title-Color", '&');
                        delay.set(delay.get() + 1);
                        if (delay.get() > 7) tdownload.set(tdownload.get() + 1);
                        if (tdownload.get() >= word.length() + 3) {
                            tdownload.set(0);
                            delay.set(0);
                        }

                        for (char c : word.toCharArray()) {
                            i++;
                            if (i == start) str += plugin.lang.getSection("Lang").getColoredString("Interface.Generic.Downloading.Title-Color-Alt", '&');
                            str += c;
                            if (i == end) str += plugin.lang.getSection("Lang").getColoredString("Interface.Generic.Downloading.Title-Color", '&');
                        }

                        str += '\n' + plugin.lang.getSection("Lang").getColoredString("Interface.Generic.Downloading.Title-Color-Alt", '&') + tdownload.name();
                        sendTitle(str, 0, 10, 5);
                        Bukkit.getScheduler().runTaskLater(plugin, this, 1);
                    } else {
                        sendTitle(ChatColor.RESET.toString(), 0, 1, 0);
                    }
                }
            });
        } else if (subtitle != null) {
            tdownload.rename(subtitle);
        } else {
            if (tdownload != null) {
                tdownload = null;
            }
            if (download != null) {
                download.cancel();
                download = null;
            }
        }
    }

    /**
     * Add Host Plugin
     *
     * @param handle Handle to bind
     * @param renderer Renderer
     */
    public static void addHostPlugin(String handle, Renderer renderer) {
        if (Util.isNull(handle, renderer)) throw new NullPointerException();
        hostPlugins.put(handle, renderer);
    }

    /**
     * Get Host Plugins
     *
     * @return Host Plugins
     */
    public static Map<String, Renderer> getHostPlugins() {
        return new HashMap<String, Renderer>(hostPlugins);
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
    public static void addSubServerPlugin(String handle, Renderer renderer) {
        if (Util.isNull(handle, renderer)) throw new NullPointerException();
        subserverPlugins.put(handle, renderer);
    }

    /**
     * Get SubServer Plugins
     *
     * @return SubServer Plugins
     */
    public static Map<String, Renderer> getSubServerPlugins() {
        return new HashMap<String, Renderer>(subserverPlugins);
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

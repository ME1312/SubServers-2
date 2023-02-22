package net.ME1312.SubServers.Client.Bukkit.Graphic;

import net.ME1312.Galaxi.Library.Container.ContainedPair;
import net.ME1312.Galaxi.Library.Container.Container;
import net.ME1312.Galaxi.Library.Try;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.SubServers.Client.Bukkit.SubPlugin;
import net.ME1312.SubServers.Client.Common.Network.API.Host;
import net.ME1312.SubServers.Client.Common.Network.API.Server;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.*;
import java.util.PrimitiveIterator.OfInt;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * GUI Renderer Layout Class
 */
public abstract class UIRenderer {
    private final boolean USE_TITLES;
    private final boolean TAPI_1_11;
    private final boolean TAPI_PLUGIN;
    private final MethodHandle MAPI_1_13;

    static final HashMap<String, PluginRenderer<Host>> hostPlugins = new HashMap<String, PluginRenderer<Host>>();
    static final HashMap<String, PluginRenderer<Server>> serverPlugins = new HashMap<String, PluginRenderer<Server>>();
    private ContainedPair<String, Integer> tdownload = null;
    private final String[] adownload;
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
        Util.nullpo(plugin, player);
        this.plugin = plugin;
        this.player = player;

        // Detect Material API
        if (plugin.api.getGameVersion().compareTo(new Version("1.13")) < 0) {
            MAPI_1_13 = null;
        } else {
            MAPI_1_13 = Try.all.get(() -> MethodHandles.publicLookup().findStatic(Material.class, "getMaterial", MethodType.methodType(Material.class, new Class[]{ String.class, boolean.class})));
        }

        // Detect Title API
        if (USE_TITLES = plugin.config.get().getMap("Settings").getBoolean("Use-Title-Messages", true)) {
            if (TAPI_1_11 = plugin.api.getGameVersion().compareTo(new Version("1.11")) >= 0) {
                TAPI_PLUGIN = false;
            } else {
                TAPI_PLUGIN = Bukkit.getPluginManager().getPlugin("TitleAPI") != null;
            }
        } else {
            TAPI_1_11 = TAPI_PLUGIN = false;
        }

        // Pre-render Animations
        {
            String a = plugin.api.getLang("SubServers", "Interface.Generic.Downloading.Title-Color-Alt");
            String b = plugin.api.getLang("SubServers", "Interface.Generic.Downloading.Title-Color");
            String word = ChatColor.stripColor(plugin.api.getLang("SubServers", "Interface.Generic.Downloading.Title"));
            String bword = b + word;

            final LinkedList<String> frames = new LinkedList<String>();
            for (int i = 0; i < 10; ++i) {
                frames.add(bword);
            }

            int wordpoints = (int) word.codePoints().count();
            int frame = 0;
            do {
                ++frame;
                int start = Math.max(frame - 3, 0);
                int end = Math.min(frame, wordpoints);
                if (start < wordpoints) {
                    StringBuilder s = new StringBuilder((start == 0)? a : b);

                    int i = 0;
                    for (OfInt iterator = word.codePoints().iterator(); iterator.hasNext(); ) {
                        ++i;
                        if (start == i) s.append(a);
                        s.appendCodePoint(iterator.nextInt());
                        if (end == i) s.append(b);
                    }

                    frames.add(s.toString());
                } else {
                    break;
                }
            } while (true);
            adownload = frames.toArray(new String[0]);
        }
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
     * @param fadein FadeIn Transition length (in ticks)
     * @param stay How long the message should stay (in ticks)
     * @param fadeout FadeOut Transition length (in ticks)
     * @return Success Status
     */
    public boolean sendTitle(String str, int fadein, int stay, int fadeout) {
        String line1, line2;
        if (str == null) {
            line1 = line2 = null;
        } else {
            if (!str.contains("\n")) {
                line1 = str;
                line2 = ChatColor.RESET.toString();
            } else if (str.startsWith("\n")) {
                line1 = str.replace("\n", "");
                line2 = ChatColor.RESET.toString();
            } else {
                String[] arr = str.split("\\n", 2);
                line1 = arr[0];
                line2 = arr[1];
            }
        }
        return sendTitle(line1, line2, fadein, stay, fadeout);
    }


    /**
     * Attempt to send a Title Message
     *
     * @param line1 Message
     * @param line2 Message
     * @param fadein FadeIn Transition length (in ticks)
     * @param stay How long the message should stay (in ticks)
     * @param fadeout FadeOut Transition length (in ticks)
     * @return Success Status
     */
    public boolean sendTitle(String line1, String line2, int fadein, int stay, int fadeout) {
        if (USE_TITLES) {
            try {
                Player player = Bukkit.getPlayer(this.player);
                if (player != null) {
                    if (TAPI_1_11) {
                        if (line1 == null) {
                            player.resetTitle();
                        } else {
                            player.sendTitle(line1, line2, (fadein >= 0)?fadein:10, (stay >= 0)?stay:70, (fadeout >= 0)?fadeout:20);
                        }
                        return true;
                    } else if (TAPI_PLUGIN) {
                        if (line1 == null) {
                            com.connorlinfoot.titleapi.TitleAPI.clearTitle(player);
                        } else {
                            com.connorlinfoot.titleapi.TitleAPI.sendTitle(player, (fadein >= 0)?fadein:10, (stay >= 0)?stay:70, (fadeout >= 0)?fadeout:20, line1, line2);
                        }
                        return true;
                    }
                }
            } catch (Throwable e) {
                return false;
            }
        }
        return false;
    }

    /**
     * See if Title Messages are available for use
     *
     * @return Title Message Availability
     */
    public boolean canSendTitle() {
        return USE_TITLES && (TAPI_1_11 || TAPI_PLUGIN);
    }

    /**
     * Shows/Hides the Downloading Title Message
     *
     * @param subtitle Subtitle to display (or null to hide)
     */
    public void setDownloading(String subtitle) {
        if (subtitle != null) {
            if (!canSendTitle()) {
                final String text = subtitle;
                if (download != -1) Bukkit.getScheduler().cancelTask(download);
                download = Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                    if (tdownload != null) Bukkit.getPlayer(player).sendMessage(plugin.api.getLang("SubServers", "Interface.Generic.Downloading").replace("$str$", text));
                    download = -1;
                }, 50L);
                return;
            }

            if (!subtitle.startsWith(Character.toString(ChatColor.COLOR_CHAR))) {
                subtitle = plugin.api.getLang("SubServers", "Interface.Generic.Downloading.Title-Color-Alt") + subtitle;
            }
            if (tdownload == null) {
                tdownload = new ContainedPair<String, Integer>(subtitle, 0);

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (tdownload != null) {
                            if (++tdownload.value >= adownload.length) {
                                tdownload.value = 0;
                            }

                            if (!sendTitle(adownload[tdownload.value], tdownload.key, 0, 10, 5)) {
                                cancel();
                            }
                        } else {
                            sendTitle(null);
                            cancel();
                        }
                    }
                }.runTaskTimer(plugin, 0, 1);
            } else {
                tdownload.key = subtitle;
            }
        } else {
            if (tdownload != null) {
                tdownload = null;
            }
            if (download != -1) {
                Bukkit.getScheduler().cancelTask(download);
                download = -1;
            }
        }
    }

    /**
     * Parse an ItemStack from a String
     *
     * @param str String to parse
     * @return ItemStack
     */
    public ItemStack parseItem(String str) {
        return parseItem(str, new ItemStack(Material.AIR));
    }

    /**
     * Parse an ItemStack from a String
     *
     * @param str String to parse
     * @param def Default to return if unable to parse
     * @return ItemStack
     */
    @SuppressWarnings({"deprecation", "JavaReflectionMemberAccess"})
    public ItemStack parseItem(String str, ItemStack def) {
        final Container<String> item = new Container<String>(str);
        if (MAPI_1_13 == null) {
            try {
                // int
                Matcher matcher = Pattern.compile("(?i)^(\\d+)$").matcher(item.value);
                if (matcher.find()) {
                    return new ItemStack(Integer.parseInt(matcher.group(1)), 1);
                }
                // int:int
                matcher.reset();
                matcher = Pattern.compile("(?i)^(\\d+):(\\d+)$").matcher(item.value);
                if (matcher.find()) {
                    return new ItemStack(Integer.parseInt(matcher.group(1)), 1, Short.parseShort(matcher.group(2)));
                }
            } catch (Exception e) {
                return def;
            }
        }

        if (item.value.toLowerCase().startsWith("minecraft:")) {
            item.value(item.value.substring(10));
        } else if (item.value.toLowerCase().startsWith("bukkit:")) {
            item.value(item.value.substring(7));

            // Legacy Material Name
            Matcher matcher = Pattern.compile("(?i)\\W(\\d+)$").matcher(item.value);
            try {
                if (matcher.find()) {
                    item.value(item.value.substring(0, item.value.length() - matcher.group().length()));
                    return new ItemStack(Material.valueOf(item.value.toUpperCase()), 1, Short.parseShort(matcher.group(1)));
                } else {
                    return new ItemStack(Material.valueOf(item.value.toUpperCase()), 1);
                }
            } catch (IllegalArgumentException e) {}
        }

        // Material Name
        if (MAPI_1_13 == null) {
            try {
                return new ItemStack(Material.valueOf(item.value.toUpperCase()), 1);
            } catch (IllegalArgumentException e) {}
        } else try {
            Material material = (Material) MAPI_1_13.invokeExact(item.value.toUpperCase(), false);
            if (material != null) {
                return new ItemStack(material, 1);
            }
        } catch (Throwable e) {}

        return def;
    }

    /**
     * Add Host Plugin
     *
     * @param handle Handle to bind
     * @param renderer Renderer
     */
    public static void addHostPlugin(String handle, PluginRenderer<Host> renderer) {
        Util.nullpo(handle, renderer);
        hostPlugins.put(handle, renderer);
    }

    /**
     * Get Host Plugins
     *
     * @return Host Plugins
     */
    public static Map<String, PluginRenderer<Host>> getHostPlugins() {
        return new HashMap<String, PluginRenderer<Host>>(hostPlugins);
    }

    /**
     * Remove Host Plugin
     *
     * @param handle Handle
     */
    public static void removeHostPlugin(String handle) {
        Util.nullpo(handle);
        hostPlugins.remove(handle);
    }

    /**
     * Add Server Plugin
     *
     * @param handle Handle to bind
     * @param renderer Renderer
     */
    public static void addServerPlugin(String handle, PluginRenderer<Server> renderer) {
        Util.nullpo(handle, renderer);
        serverPlugins.put(handle, renderer);
    }

    /**
     * Get Server Plugins
     *
     * @return SubServer Plugins
     */
    public static Map<String, PluginRenderer<Server>> getServerPlugins() {
        return new HashMap<String, PluginRenderer<Server>>(serverPlugins);
    }

    /**
     * Remove Server Plugin
     *
     * @param handle Handle
     */
    public static void removeServerPlugin(String handle) {
        Util.nullpo(handle);
        serverPlugins.remove(handle);
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
     * Opens Server/&lt;name&gt;
     *
     * @param server SubServer Name
     */
    public abstract void serverAdmin(String server);

    /**
     * Opens Server/&lt;name&gt;/Plugins
     *
     * @param server SubServer Name
     */
    public abstract void serverPlugin(int page, String server);

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
        private Integer port = null;

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
            Util.nullpo(value);
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
            Util.nullpo(value);
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
            final Version version = this.version;
            history.add(() -> this.version = version);
            this.version = value;
        }

        /**
         * Gets the Port Number for the Server
         *
         * @return Server Port Number (null for auto-select)
         */
        public Integer getPort() {
            return port;
        }

        /**
         * Sets the Port Number for the Server
         *
         * @param value Value (null for auto-select)
         */
        public void setPort(Integer value) {
            final Integer port = this.port;
            history.add(() -> this.port = port);
            this.port = value;
        }
    }
}

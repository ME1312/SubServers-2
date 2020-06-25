package net.ME1312.SubServers.Client.Bukkit.Graphic;

import net.ME1312.Galaxi.Library.Container.Container;
import net.ME1312.Galaxi.Library.Container.NamedContainer;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.SubServers.Client.Bukkit.Network.API.Host;
import net.ME1312.SubServers.Client.Bukkit.Network.API.SubServer;
import net.ME1312.SubServers.Client.Bukkit.SubPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * GUI Renderer Layout Class
 */
public abstract class UIRenderer {
    static HashMap<String, PluginRenderer<Host>> hostPlugins = new HashMap<String, PluginRenderer<Host>>();
    static HashMap<String, PluginRenderer<SubServer>> subserverPlugins = new HashMap<String, PluginRenderer<SubServer>>();
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
        if (Util.isNull(str, fadein, stay, fadeout)) throw new NullPointerException();
        if (plugin.config.get().getMap("Settings").getBoolean("Use-Title-Messages", true)) {
            String line1, line2;
            if (!str.startsWith("\n") && str.contains("\n")) {
                line1 = str.split("\\n")[0];
                line2 = str.split("\\n")[1];
            } else {
                line1 = str.replace("\n", "");
                line2 = ChatColor.RESET.toString();
            }
            try {
                Player player = Bukkit.getPlayer(this.player);
                if (plugin.api.getGameVersion().compareTo(new Version("1.11")) >= 0) {
                    if (ChatColor.stripColor(line1).length() == 0 && ChatColor.stripColor(line2).length() == 0) {
                        player.resetTitle();
                    } else {
                        player.sendTitle(line1, line2, (fadein >= 0)?fadein:10, (stay >= 0)?stay:70, (fadeout >= 0)?fadeout:20);
                    }
                    return true;
                } else if (Bukkit.getPluginManager().getPlugin("TitleManager") != null) {
                    if (Util.isException(() -> Util.reflect(Class.forName("io.puharesource.mc.titlemanager.api.v2.TitleManagerAPI").getMethod("sendTitles", Player.class, String.class, String.class, int.class, int.class, int.class),
                            Bukkit.getPluginManager().getPlugin("TitleManager"), player, line1, line2, (fadein >= 0)?fadein:10, (stay >= 0)?stay:70, (fadeout >= 0)?fadeout:20))) { // Attempt TitleAPI v2

                        // Fallback to TitleAPI v1
                        io.puharesource.mc.titlemanager.api.TitleObject obj = io.puharesource.mc.titlemanager.api.TitleObject.class.getConstructor(String.class, String.class).newInstance(line1, line2);
                        if (fadein >= 0) obj.setFadeIn(fadein);
                        if (stay >= 0) obj.setStay(stay);
                        if (fadeout >= 0) obj.setFadeOut(fadeout);
                        obj.send(player);
                    }
                    return true;
                } else return false;
            } catch (Throwable e) {
                return false;
            }
        } else return false;
    }

    /**
     * Shows/Hides the Downloading Title Message
     *
     * @param subtitle Subtitle to display (or null to hide)
     */
    public void setDownloading(String subtitle) {
        if (subtitle != null && !(plugin.config.get().getMap("Settings").getBoolean("Use-Title-Messages", true) && (plugin.api.getGameVersion().compareTo(new Version("1.11")) >= 0 || Bukkit.getPluginManager().getPlugin("TitleManager") != null))) {
            if (download != -1) Bukkit.getScheduler().cancelTask(download);
            download = Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                if (tdownload != null) Bukkit.getPlayer(player).sendMessage(plugin.api.getLang("SubServers", "Interface.Generic.Downloading").replace("$str$", subtitle));
                download = -1;
            }, 50L);
        } if (subtitle != null && tdownload == null) {
            tdownload = new NamedContainer<String, Integer>(subtitle, 0);
            final Container<Integer> delay = new Container<Integer>(0);
            Bukkit.getScheduler().runTask(plugin, new Runnable() {
                @Override
                public void run() {
                    if (tdownload != null) {
                        String word = ChatColor.stripColor(plugin.api.getLang("SubServers", "Interface.Generic.Downloading.Title"));
                        int i = 0;
                        int start = (tdownload.get() - 3 < 0)?0: tdownload.get()-3;
                        int end = (tdownload.get() >= word.length())?word.length(): tdownload.get();
                        String str = plugin.api.getLang("SubServers", (delay.get() > 7 && start == 0)?"Interface.Generic.Downloading.Title-Color-Alt":"Interface.Generic.Downloading.Title-Color");
                        delay.set(delay.get() + 1);
                        if (delay.get() > 7) tdownload.set(tdownload.get() + 1);
                        if (tdownload.get() >= word.length() + 3) {
                            tdownload.set(0);
                            delay.set(0);
                        }

                        for (char c : word.toCharArray()) {
                            i++;
                            if (i == start) str += plugin.api.getLang("SubServers", "Interface.Generic.Downloading.Title-Color-Alt");
                            str += c;
                            if (i == end) str += plugin.api.getLang("SubServers", "Interface.Generic.Downloading.Title-Color");
                        }

                        str += '\n' + plugin.api.getLang("SubServers", "Interface.Generic.Downloading.Title-Color-Alt") + tdownload.name();
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
    public ItemStack parseItem(String str, ItemStack def) {
        final Container<String> item = new Container<String>(str);
        if (plugin.api.getGameVersion().compareTo(new Version("1.13")) < 0) {
            try {
                // int
                Matcher matcher = Pattern.compile("(?i)^(\\d+)$").matcher(item.get());
                if (matcher.find()) {
                    return ItemStack.class.getConstructor(int.class, int.class).newInstance(Integer.parseInt(matcher.group(1)), 1);
                }
                // int:int
                matcher.reset();
                matcher = Pattern.compile("(?i)^(\\d+):(\\d+)$").matcher(item.get());
                if (matcher.find()) {
                    return ItemStack.class.getConstructor(int.class, int.class, short.class).newInstance(Integer.parseInt(matcher.group(1)), 1, Short.parseShort(matcher.group(2)));
                }
            } catch (Exception e) {
                return def;
            }
        }
        // minecraft:name
        if (item.get().toLowerCase().startsWith("minecraft:")) {
            item.set(item.get().substring(10));
        } else

        // bukkit:name
        if (item.get().toLowerCase().startsWith("bukkit:")) {
            item.set(item.get().substring(7));

            if (!Util.isException(() -> Material.valueOf(item.get().toUpperCase()))) {
                return new ItemStack(Material.valueOf(item.get().toUpperCase()), 1);
            }
        }

        // material name
        if (plugin.api.getGameVersion().compareTo(new Version("1.13")) < 0) {
            if (!Util.isException(() -> Material.valueOf(item.get().toUpperCase()))) {
                return new ItemStack(Material.valueOf(item.get().toUpperCase()), 1);
            }
        } else try {
            if (Material.class.getMethod("getMaterial", String.class, boolean.class).invoke(null, item.get().toUpperCase(), false) != null) {
                return new ItemStack((Material) Material.class.getMethod("getMaterial", String.class, boolean.class).invoke(null, item.get().toUpperCase(), false), 1);
            }
        } catch (Exception e) {}

        return def;
    }

    /**
     * Add Host Plugin
     *
     * @param handle Handle to bind
     * @param renderer Renderer
     */
    public static void addHostPlugin(String handle, PluginRenderer<Host> renderer) {
        if (Util.isNull(handle, renderer)) throw new NullPointerException();
        hostPlugins.put(handle, renderer);
    }

    /**
     * Get Host Plugins
     *
     * @return Host Plugins
     */
    public static Map<String, PluginRenderer> getHostPlugins() {
        return new HashMap<String, PluginRenderer>(hostPlugins);
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
    public static void addSubServerPlugin(String handle, PluginRenderer<SubServer> renderer) {
        if (Util.isNull(handle, renderer)) throw new NullPointerException();
        subserverPlugins.put(handle, renderer);
    }

    /**
     * Get SubServer Plugins
     *
     * @return SubServer Plugins
     */
    public static Map<String, PluginRenderer> getSubServerPlugins() {
        return new HashMap<String, PluginRenderer>(subserverPlugins);
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

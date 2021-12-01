package net.ME1312.SubServers.Client.Sponge.Graphic;

import net.ME1312.Galaxi.Library.Container.ContainedPair;
import net.ME1312.Galaxi.Library.Container.Container;
import net.ME1312.Galaxi.Library.Container.Pair;
import net.ME1312.Galaxi.Library.Container.Value;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.SubServers.Client.Common.Network.API.Host;
import net.ME1312.SubServers.Client.Common.Network.API.Server;
import net.ME1312.SubServers.Client.Common.Network.API.SubServer;
import net.ME1312.SubServers.Client.Sponge.Library.Compatibility.ChatColor;
import net.ME1312.SubServers.Client.Sponge.SubPlugin;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.title.Title;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * GUI Renderer Layout Class
 */
public abstract class UIRenderer {
    static HashMap<String, PluginRenderer<Host>> hostPlugins = new HashMap<String, PluginRenderer<Host>>();
    static HashMap<String, PluginRenderer<Server>> serverPlugins = new HashMap<String, PluginRenderer<Server>>();
    private Pair<String, Integer> tdownload = null;
    private UUID download = null;
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
        Util.nullpo(str, fadein, stay, fadeout);
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
                if (ChatColor.stripColor(line1).length() == 0 && ChatColor.stripColor(line2).length() == 0) {
                    Sponge.getServer().getPlayer(player).get().resetTitle();
                } else {
                    Sponge.getServer().getPlayer(player).get().sendTitle(Title.builder().title(ChatColor.convertColor(line1)).subtitle(ChatColor.convertColor(line2)).fadeIn((fadein >= 0)?fadein:10).stay((stay >= 0)?stay:70).fadeOut((fadeout >= 0)?fadeout:20).build());
                }
                return true;
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
        if (subtitle != null && !plugin.config.get().getMap("Settings").getBoolean("Use-Title-Messages", true)) {
            if (download != null && Sponge.getScheduler().getTaskById(download).isPresent()) Sponge.getScheduler().getTaskById(download).get().cancel();
            download = Sponge.getScheduler().createTaskBuilder().execute(() -> {
                if (tdownload != null) Sponge.getServer().getPlayer(player).get().sendMessage(ChatColor.convertColor(plugin.api.getLang("SubServers", "Interface.Generic.Downloading").replace("$str$", subtitle)));
                download = null;
            }).delay(2500, TimeUnit.MILLISECONDS).submit(plugin).getUniqueId();
        } if (subtitle != null && tdownload == null) {
            tdownload = new ContainedPair<String, Integer>(subtitle, 0);
            final Value<Integer> delay = new Container<Integer>(0);
            Sponge.getScheduler().createTaskBuilder().execute(new Runnable() {
                @Override
                public void run() {
                    if (tdownload != null) {
                        String word = ChatColor.stripColor(plugin.api.getLang("SubServers", "Interface.Generic.Downloading.Title"));
                        int i = 0;
                        int start = (tdownload.value() - 3 < 0)?0: tdownload.value()-3;
                        int end = (tdownload.value() >= word.length())?word.length(): tdownload.value();
                        String str = plugin.api.getLang("SubServers", (delay.value() > 7 && start == 0)?"Interface.Generic.Downloading.Title-Color-Alt":"Interface.Generic.Downloading.Title-Color");
                        delay.value(delay.value() + 1);
                        if (delay.value() > 7) tdownload.value(tdownload.value() + 1);
                        if (tdownload.value() >= word.length() + 3) {
                            tdownload.value(0);
                            delay.value(0);
                        }

                        for (char c : word.toCharArray()) {
                            i++;
                            if (i == start) str += plugin.api.getLang("SubServers", "Interface.Generic.Downloading.Title-Color-Alt");
                            str += c;
                            if (i == end) str += plugin.api.getLang("SubServers", "Interface.Generic.Downloading.Title-Color");
                        }

                        str += '\n' + plugin.api.getLang("SubServers", "Interface.Generic.Downloading.Title-Color-Alt") + tdownload.key();
                        sendTitle(str, 0, 10, 5);
                        Sponge.getScheduler().createTaskBuilder().execute(this).delay(50, TimeUnit.MILLISECONDS).submit(plugin);
                    } else {
                        sendTitle(ChatColor.RESET.toString(), 0, 1, 0);
                    }
                }
            }).submit(plugin);
        } else if (subtitle != null) {
            tdownload.key(subtitle);
        } else {
            if (tdownload != null) {
                tdownload = null;
            }
            if (download != null) {
                if (Sponge.getScheduler().getTaskById(download).isPresent()) Sponge.getScheduler().getTaskById(download).get().cancel();
                download = null;
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
        return parseItem(str, ItemStack.builder().itemType(ItemTypes.NONE).quantity(1).build());
    }

    /**
     * Parse an ItemStack from a String
     *
     * @param str String to parse
     * @param def Default to return if unable to parse
     * @return ItemStack
     */
    public ItemStack parseItem(String str, ItemStack def) {
        final Value<String> item = new Container<String>(str);
        // minecraft:name
        if (item.value().toLowerCase().startsWith("minecraft:")) {
            item.value(item.value().substring(10));
        } else {

            // bukkit:name (ignored on sponge)
            if (item.value().toLowerCase().startsWith("bukkit:")) {
                item.value(item.value().substring(7));
            }
        }

        // material name
        try {
            return ItemStack.builder().itemType((ItemType) ItemTypes.class.getDeclaredField(item.value().toUpperCase()).get(null)).quantity(1).build();
        } catch (NoSuchFieldException | NoSuchFieldError | IllegalAccessException | IllegalAccessError e) {
            return def;
        }
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
            Util.nullpo(value);
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
            Util.nullpo(value);
            final int port = this.port;
            history.add(() -> this.port = port);
            this.port = value;
        }
    }
}

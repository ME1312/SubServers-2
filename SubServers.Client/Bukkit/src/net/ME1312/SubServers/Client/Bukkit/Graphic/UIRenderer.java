package net.ME1312.SubServers.Client.Bukkit.Graphic;

import net.ME1312.SubServers.Client.Bukkit.Library.Container;
import net.ME1312.SubServers.Client.Bukkit.Library.NamedContainer;
import net.ME1312.SubServers.Client.Bukkit.Library.Version.Version;
import net.ME1312.SubServers.Client.Bukkit.Network.Packet.PacketCreateServer;
import net.ME1312.SubServers.Client.Bukkit.Network.Packet.PacketDownloadHostInfo;
import net.ME1312.SubServers.Client.Bukkit.Network.Packet.PacketDownloadServerInfo;
import net.ME1312.SubServers.Client.Bukkit.Network.Packet.PacketDownloadServerList;
import net.ME1312.SubServers.Client.Bukkit.SubPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class UIRenderer {
    private List<Runnable> windowHistory = new LinkedList<Runnable>();
    protected Options lastUsedOptions = null;
    protected String lastVistedObject = null;
    protected int lastPage = 1;
    protected Runnable lastMenu = null;
    private NamedContainer<String, Integer> downloading = null;
    protected boolean open = false;
    protected final UUID player;
    private SubPlugin plugin;
    public abstract static class Options {
        List<Runnable> history = new LinkedList<Runnable>();
        private boolean init = false;

        boolean init() {
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

    protected UIRenderer(SubPlugin plugin, UUID player) {
        this.plugin = plugin;
        this.player = player;
    }

    /**
     * Opens a new window
     */
    public void newUI() {
        clearHistory();
        if (lastMenu == null) {
            hostMenu(1);
        } else {
            lastMenu.run();
        }
    }

    /**
     * Clears the Window History
     */
    public void clearHistory() {
        windowHistory.clear();
    }

    /**
     * If there is any Window History
     *
     * @return Window History Status
     */
    public boolean hasHistory() {
        return !windowHistory.isEmpty();
    }

    /**
     * Reopens the current window
     */
    public void reopen() {
        Runnable lastWindow = windowHistory.get(windowHistory.size() - 1);
        windowHistory.remove(windowHistory.size() - 1);
        lastWindow.run();
    }

    /**
     * Reopens the previous window
     */
    public void back() {
        windowHistory.remove(windowHistory.size() - 1);
        reopen();
    }

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
        if (Bukkit.getPluginManager().getPlugin("TitleManager") != null && plugin.pluginconf.get().getSection("Settings").getBoolean("Use-Title-Messages", true)) {
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
        if (subtitle != null && !(Bukkit.getPluginManager().getPlugin("TitleManager") != null && plugin.pluginconf.get().getSection("Settings").getBoolean("Use-Title-Messages", true))) {
            Bukkit.getPlayer(player).sendMessage(plugin.lang.getSection("Lang").getColoredString("Interface.Generic.Downloading", '&').replace("$str$", subtitle));
        } if (subtitle != null && downloading == null) {
            downloading = new NamedContainer<String, Integer>(subtitle, 0);
            final Container<Integer> delay = new Container<Integer>(0);
            Bukkit.getScheduler().runTask(plugin, new Runnable() {
                @Override
                public void run() {
                    if (downloading != null) {
                        String word = ChatColor.stripColor(plugin.lang.getSection("Lang").getColoredString("Interface.Generic.Downloading.Title", '&'));
                        int i = 0;
                        int start = (downloading.get() - 3 < 0)?0:downloading.get()-3;
                        int end = (downloading.get() >= word.length())?word.length():downloading.get();
                        String str = plugin.lang.getSection("Lang").getColoredString((delay.get() > 7 && start == 0)?"Interface.Generic.Downloading.Title-Color-Alt":"Interface.Generic.Downloading.Title-Color", '&');
                        delay.set(delay.get() + 1);
                        if (delay.get() > 7) downloading.set(downloading.get() + 1);
                        if (downloading.get() >= word.length() + 3) {
                            downloading.set(0);
                            delay.set(0);
                        }

                        for (char c : word.toCharArray()) {
                            i++;
                            if (i == start) str += plugin.lang.getSection("Lang").getColoredString("Interface.Generic.Downloading.Title-Color-Alt", '&');
                            str += c;
                            if (i == end) str += plugin.lang.getSection("Lang").getColoredString("Interface.Generic.Downloading.Title-Color", '&');
                        }

                        str += '\n' + plugin.lang.getSection("Lang").getColoredString("Interface.Generic.Downloading.Title-Color-Alt", '&') + downloading.name();
                        sendTitle(str, 0, 10, 5);
                        Bukkit.getScheduler().runTaskLater(plugin, this, 1);
                    } else {
                        sendTitle(ChatColor.RESET.toString(), 0, 1, 0);
                    }
                }
            });
        } else if (subtitle != null) {
            downloading.rename(subtitle);
        } else if (downloading != null) {
            downloading = null;
        }
    }

    /**
     * Opens the Host Menu
     *
     * @param page Page Number (starting from page 1)
     */
    public void hostMenu(final Integer page) {
        setDownloading(ChatColor.stripColor(plugin.lang.getSection("Lang").getColoredString("Interface.Host-Menu.Title", '&')));
        plugin.subdata.sendPacket(new PacketDownloadServerList(null, UUID.randomUUID().toString(), (json) -> {
            setDownloading(null);
            lastVistedObject = null;
            lastPage = page;
            lastMenu = () -> hostMenu(1);
            windowHistory.add(() -> hostMenu(page));
            List<String> hosts = new ArrayList<String>();
            hosts.addAll(json.getJSONObject("hosts").keySet());

            ItemStack block;
            ItemMeta blockMeta;
            ItemStack div = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 15);
            ItemMeta divMeta = div.getItemMeta();
            divMeta.setDisplayName(ChatColor.RESET.toString());
            div.setItemMeta(divMeta);

            int i = 0;
            int min = ((page - 1) * 36);
            int max = (min + 35);
            int count = (hosts.size() == 0)?27:((hosts.size() - min - 1 >= max)?36:hosts.size() - min);
            int area = (count % 9 == 0) ? count : (int) (Math.floor(count / 9) + 1) * 9;

            Inventory inv = Bukkit.createInventory(null, 18 + area, plugin.lang.getSection("Lang").getColoredString("Interface.Host-Menu.Title", '&'));
            block = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 7);
            block.setItemMeta(divMeta);
            while (i < area) {
                inv.setItem(i, block);
                i++;
            }
            ItemStack adiv = block;
            i = (int) ((count < 9) ? Math.floor((9 - count) / 2) : 0);

            boolean even = (count & 1) == 0 && count < 9;
            short enabled, disabled;

            for (String host : hosts) {
                if (hosts.indexOf(host) >= min && hosts.indexOf(host) <= max) {
                    if (even && (i == 4 || i == 13 || i == 22 || i == 31)) inv.setItem(i++, adiv);
                    enabled = (short) (((i & 1) == 0) ? 3 : 11);
                    disabled = (short) (((i & 1) == 0) ? 2 : 14);

                    if (json.getJSONObject("hosts").getJSONObject(host).getBoolean("enabled")) {
                        block = new ItemStack(Material.STAINED_GLASS_PANE, 1, enabled);
                        blockMeta = block.getItemMeta();
                        blockMeta.setDisplayName(ChatColor.AQUA + host);
                        blockMeta.setLore(Arrays.asList(plugin.lang.getSection("Lang").getColoredString("Interface.Host-Menu.Host-Server-Count", '&').replace("$int$", Integer.toString(json.getJSONObject("hosts").getJSONObject(host).getJSONObject("servers").keySet().size()))));
                    } else {
                        block = new ItemStack(Material.STAINED_GLASS_PANE, 1, disabled);
                        blockMeta = block.getItemMeta();
                        blockMeta.setDisplayName(ChatColor.RED + host);
                        blockMeta.setLore(Arrays.asList(plugin.lang.getSection("Lang").getColoredString("Interface.Host-Menu.Host-Disabled", '&')));
                    }
                    block.setItemMeta(blockMeta);
                    inv.setItem(i, block);

                    count--;
                    if (count < 9 && (i == 8 || i == 17 || i == 26)) {
                        i += (int) Math.floor((9 - count) / 2) + 1;
                        even = (count & 1) == 0;
                    } else {
                        i++;
                    }
                }
            }

            if (hosts.size() == 0) {
                block = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 14);
                blockMeta = block.getItemMeta();
                blockMeta.setDisplayName(plugin.lang.getSection("Lang").getColoredString("Interface.Host-Menu.No-Hosts", '&'));
                block.setItemMeta(blockMeta);
                inv.setItem(12, block);
                inv.setItem(13, block);
                inv.setItem(14, block);
            }

            i = inv.getSize() - 18;
            while (i < inv.getSize()) {
                inv.setItem(i, div);
                i++;
            }
            i = inv.getSize() - 9;

            if (min != 0) {
                block = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 4);
                blockMeta = block.getItemMeta();
                blockMeta.setDisplayName(plugin.lang.getSection("Lang").getColoredString("Interface.Generic.Back-Arrow", '&'));
                block.setItemMeta(blockMeta);
                inv.setItem(i++, block);
                inv.setItem(i++, block);
            } else i += 2;
            i++;
            block = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 5);
            blockMeta = block.getItemMeta();
            blockMeta.setDisplayName(plugin.lang.getSection("Lang").getColoredString("Interface.Host-Menu.SubServer-Menu", '&'));
            block.setItemMeta(blockMeta);
            inv.setItem(i++, block);
            inv.setItem(i++, block);
            inv.setItem(i++, block);
            i++;
            if (hosts.size() - 1 > max) {
                block = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 4);
                blockMeta = block.getItemMeta();
                blockMeta.setDisplayName(plugin.lang.getSection("Lang").getColoredString("Interface.Generic.Next-Arrow", '&'));
                block.setItemMeta(blockMeta);
                inv.setItem(i++, block);
                inv.setItem(i, block);
            }

            Bukkit.getPlayer(player).openInventory(inv);
            open = true;
        }));
    }

    /**
     * Opens Hosts/&lt;name&gt;
     *
     * @param host Host Name
     */
    public void hostAdmin(final String host) {
        setDownloading(ChatColor.stripColor(plugin.lang.getSection("Lang").getColoredString("Interface.Host-Admin.Title", '&').replace("$str$", host)));
        plugin.subdata.sendPacket(new PacketDownloadHostInfo(host, UUID.randomUUID().toString(), (json) -> {
            windowHistory.add(() -> hostAdmin(host));
            if (!json.getBoolean("valid")) {
                back();
            } else {
                setDownloading(null);
                lastVistedObject = host;

                ItemStack block;
                ItemMeta blockMeta;
                ItemStack div = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 15);
                ItemMeta divMeta = div.getItemMeta();
                divMeta.setDisplayName(ChatColor.RESET.toString());
                div.setItemMeta(divMeta);

                Inventory inv = Bukkit.createInventory(null, 36, plugin.lang.getSection("Lang").getColoredString("Interface.Host-Admin.Title", '&').replace("$str$", host));

                int i = 0;
                while (i < inv.getSize()) {
                    inv.setItem(i, div);
                    i++;
                }

                if (!(Bukkit.getPlayer(player).hasPermission("subservers.host.create.*") || Bukkit.getPlayer(player).hasPermission("subservers.host.create." + host.toLowerCase()))) {
                    block = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 7);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(ChatColor.GRAY+ChatColor.stripColor(plugin.lang.getSection("Lang").getColoredString("Interface.Host-Admin.Creator", '&')));
                    blockMeta.setLore(Arrays.asList(plugin.lang.getSection("Lang").getColoredString("Interface.Generic.Invalid-Permission", '&').replace("$str$", "subservers.host.create." + host.toLowerCase())));
                } else if (!json.getJSONObject("host").getBoolean("enabled") || json.getJSONObject("host").getJSONObject("creator").getBoolean("busy")) {
                    block = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 7);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(ChatColor.GRAY+ChatColor.stripColor(plugin.lang.getSection("Lang").getColoredString("Interface.Host-Admin.Creator", '&')));
                    blockMeta.setLore(Arrays.asList(plugin.lang.getSection("Lang").getColoredString("Interface.Host-Admin.Creator-Busy", '&')));
                } else {
                    block = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 4);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(plugin.lang.getSection("Lang").getColoredString("Interface.Host-Admin.Creator", '&'));

                }
                block.setItemMeta(blockMeta);
                inv.setItem(1, block);
                inv.setItem(10, block);

                block = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 5);
                blockMeta = block.getItemMeta();
                blockMeta.setDisplayName(plugin.lang.getSection("Lang").getColoredString("Interface.Host-Admin.SubServers", '&'));
                block.setItemMeta(blockMeta);
                inv.setItem(2, block);
                inv.setItem(3, block);
                inv.setItem(11, block);
                inv.setItem(12, block);

                if (!(Bukkit.getPlayer(player).hasPermission("subservers.host.edit.*") || Bukkit.getPlayer(player).hasPermission("subservers.host.edit." + host.toLowerCase()))) {
                    block = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 7);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(ChatColor.GRAY+ChatColor.stripColor(plugin.lang.getSection("Lang").getColoredString("Interface.Host-Admin.Editor", '&')));
                    blockMeta.setLore(Arrays.asList(plugin.lang.getSection("Lang").getColoredString("Interface.Generic.Invalid-Permission", '&').replace("$str$", "subservers.host.edit." + host.toLowerCase())));
                } else if (!json.getJSONObject("host").getBoolean("editable")) {
                    block = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 7);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(ChatColor.GRAY+ChatColor.stripColor(plugin.lang.getSection("Lang").getColoredString("Interface.Host-Admin.Editor", '&')));
                    blockMeta.setLore(Arrays.asList(plugin.lang.getSection("Lang").getColoredString("Interface.Host-Admin.Editor-Unavailable", '&')));
                } else {
                    block = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 1);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(plugin.lang.getSection("Lang").getColoredString("Interface.Host-Admin.Editor", '&'));

                }
                block.setItemMeta(blockMeta);
                inv.setItem(5, block);
                inv.setItem(6, block);
                inv.setItem(7, block);
                inv.setItem(14, block);
                inv.setItem(15, block);
                inv.setItem(16, block);


                if (json.getJSONObject("host").getBoolean("enabled")) {
                    block = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 11);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(ChatColor.AQUA + host);
                    blockMeta.setLore(Arrays.asList(plugin.lang.getSection("Lang").getColoredString("Interface.Host-Menu.Host-Server-Count", '&').replace("$int$", Integer.toString(json.getJSONObject("host").getJSONObject("servers").keySet().size()))));
                } else {
                    block = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 14);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(ChatColor.RED + host);
                    blockMeta.setLore(Arrays.asList(plugin.lang.getSection("Lang").getColoredString("Interface.Host-Menu.Host-Disabled", '&')));
                }
                block.setItemMeta(blockMeta);
                inv.setItem(30, block);
                inv.setItem(31, block);
                inv.setItem(32, block);


                block = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 14);
                blockMeta = block.getItemMeta();
                blockMeta.setDisplayName(plugin.lang.getSection("Lang").getColoredString("Interface.Generic.Back", '&'));
                block.setItemMeta(blockMeta);
                inv.setItem(34, block);
                inv.setItem(35, block);

                Bukkit.getPlayer(player).openInventory(inv);
                open = true;
            }
        }));
    }

    /**
     * Opens Hosts/&lt;name&gt;/Create
     *
     * @param options Creator Options
     */
    public void hostCreator(final CreatorOptions options) {
        setDownloading(ChatColor.stripColor(plugin.lang.getSection("Lang").getColoredString("Interface.Host-Creator.Title", '&').replace("$str$", options.getHost())));
        lastUsedOptions = options;
        if (!options.init()) {
            windowHistory.add(() -> hostCreator(options));
            lastVistedObject = options.getHost();
        }

        plugin.subdata.sendPacket(new PacketDownloadHostInfo(options.getHost(), UUID.randomUUID().toString(), json -> {
            if (!json.getJSONObject("host").getBoolean("enabled") || json.getJSONObject("host").getJSONObject("creator").getBoolean("busy")) {
                lastUsedOptions = null;
                back();
            } else {
                setDownloading(null);
                ItemStack block;
                ItemMeta blockMeta;
                ItemStack div = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 15);
                ItemMeta divMeta = div.getItemMeta();
                divMeta.setDisplayName(ChatColor.RESET.toString());
                div.setItemMeta(divMeta);

                Inventory inv = Bukkit.createInventory(null, 54, plugin.lang.getSection("Lang").getColoredString("Interface.Host-Creator.Title", '&').replace("$str$", options.getHost()));

                int i = 0;
                while (i < inv.getSize()) {
                    inv.setItem(i, div);
                    i++;
                }

                if (options.getName() == null) {
                    block = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 14);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(ChatColor.RED + plugin.lang.getSection("Lang").getColoredString("Interface.Host-Creator.Edit-Name", '&'));
                } else {
                    block = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 5);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(ChatColor.GREEN + plugin.lang.getSection("Lang").getColoredString("Interface.Host-Creator.Edit-Name", '&'));
                    blockMeta.setLore(Arrays.asList(ChatColor.GRAY + options.getName()));
                }
                block.setItemMeta(blockMeta);
                inv.setItem(3, block);
                inv.setItem(4, block);
                inv.setItem(5, block);

                if (options.getType() == null) {
                    block = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 14);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(ChatColor.RED + plugin.lang.getSection("Lang").getColoredString("Interface.Host-Creator.Edit-Type", '&'));
                } else {
                    block = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 5);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(ChatColor.GREEN + plugin.lang.getSection("Lang").getColoredString("Interface.Host-Creator.Edit-Type", '&'));
                    blockMeta.setLore(Arrays.asList(ChatColor.GRAY + options.getType().toString()));
                }
                block.setItemMeta(blockMeta);
                inv.setItem(10, block);
                inv.setItem(11, block);

                if (options.getVersion() == null) {
                    block = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 14);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(ChatColor.RED + plugin.lang.getSection("Lang").getColoredString("Interface.Host-Creator.Edit-Version", '&'));
                } else {
                    block = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 5);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(ChatColor.GREEN + plugin.lang.getSection("Lang").getColoredString("Interface.Host-Creator.Edit-Version", '&'));
                    blockMeta.setLore(Arrays.asList(ChatColor.GRAY + "v" + options.getVersion().toString()));
                }
                block.setItemMeta(blockMeta);
                inv.setItem(15, block);
                inv.setItem(16, block);

                if (options.getPort() <= 0) {
                    block = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 14);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(ChatColor.RED + plugin.lang.getSection("Lang").getColoredString("Interface.Host-Creator.Edit-Port", '&'));
                } else {
                    block = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 5);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(ChatColor.GREEN + plugin.lang.getSection("Lang").getColoredString("Interface.Host-Creator.Edit-Port", '&'));
                    blockMeta.setLore(Arrays.asList(ChatColor.GRAY.toString() + options.getPort()));
                }
                block.setItemMeta(blockMeta);
                inv.setItem(28, block);
                inv.setItem(29, block);

                if (options.getMemory() < 256) {
                    block = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 14);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(ChatColor.RED + plugin.lang.getSection("Lang").getColoredString("Interface.Host-Creator.Edit-RAM", '&'));
                } else {
                    block = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 5);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(ChatColor.GREEN + plugin.lang.getSection("Lang").getColoredString("Interface.Host-Creator.Edit-RAM", '&'));
                    blockMeta.setLore(Arrays.asList(ChatColor.GRAY.toString() + options.getMemory() + "MB"));
                }
                block.setItemMeta(blockMeta);
                inv.setItem(33, block);
                inv.setItem(34, block);

                if (!options.hasHistory()) {
                    block = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 7);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(ChatColor.GRAY + ChatColor.stripColor(plugin.lang.getSection("Lang").getColoredString("Interface.Generic.Undo", '&')));
                    block.setItemMeta(blockMeta);
                } else {
                    block = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 1);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(plugin.lang.getSection("Lang").getColoredString("Interface.Generic.Undo", '&'));
                    block.setItemMeta(blockMeta);
                }
                block.setItemMeta(blockMeta);
                inv.setItem(45, block);
                inv.setItem(46, block);

                if (options.getName() == null || options.getType() == null || options.getVersion() == null || options.getPort() <= 0 && options.getMemory() < 256) {
                    block = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 7);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(ChatColor.GRAY + ChatColor.stripColor(plugin.lang.getSection("Lang").getColoredString("Interface.Host-Creator.Submit", '&')));
                    blockMeta.setLore(Arrays.asList(plugin.lang.getSection("Lang").getColoredString("Interface.Host-Creator.Form-Incomplete", '&')));
                    block.setItemMeta(blockMeta);
                } else {
                    block = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 4);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(plugin.lang.getSection("Lang").getColoredString("Interface.Host-Creator.Submit", '&'));
                    block.setItemMeta(blockMeta);
                }
                block.setItemMeta(blockMeta);
                inv.setItem(48, block);
                inv.setItem(49, block);
                inv.setItem(50, block);

                block = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 14);
                blockMeta = block.getItemMeta();
                blockMeta.setDisplayName(plugin.lang.getSection("Lang").getColoredString("Interface.Generic.Back", '&'));
                block.setItemMeta(blockMeta);
                inv.setItem(52, block);
                inv.setItem(53, block);

                Bukkit.getPlayer(player).openInventory(inv);
                open = true;
            }
        }));
    }
    public static class CreatorOptions extends Options {
        private String host;
        private String name = null;
        private PacketCreateServer.ServerType type = null;
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
            final String name = this.name;
            history.add(() -> this.name = name);
            this.name = value;
        }

        /**
         * Gets the ServerType
         *
         * @return ServerType
         */
        public PacketCreateServer.ServerType getType() {
            return type;
        }

        /**
         * Sets the ServerType
         *
         * @param value Value
         */
        public void setType(PacketCreateServer.ServerType value) {
            final PacketCreateServer.ServerType type = this.type;
            history.add(() -> this.type = type);
            this.type = value;
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
            final int port = this.port;
            history.add(() -> this.port = port);
            this.port = value;
        }
    }

    /**
     * Opens Hosts/&lt;name&gt;/Edit
     *
     * @param options Host Editor Options
     */
    public void hostEditor(final HostEditorOptions options) {
        lastUsedOptions = options;
        if (!options.init()) {
            windowHistory.add(() -> hostEditor(options));
            lastVistedObject = options.getHost();
        }
    }
    public static class HostEditorOptions extends Options {
        private String host;

        /**
         * Grabs a raw HostCreatorOptions instance
         *
         * @param host Host Name
         */
        public HostEditorOptions(String host) {
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
    }

    /**
     * Opens the SubServer Menu
     *
     * @param page Page Number (starting from page 1)
     * @param host Host Name (or null to scan all hosts)
     */
    public void subserverMenu(final Integer page, final String host) {
        setDownloading(ChatColor.stripColor((host == null)?plugin.lang.getSection("Lang").getColoredString("Interface.SubServer-Menu.Title", '&'):plugin.lang.getSection("Lang").getColoredString("Interface.Host-SubServer.Title", '&').replace("$str$", host)));
        plugin.subdata.sendPacket(new PacketDownloadServerList(host, UUID.randomUUID().toString(), json -> {
            setDownloading(null);
            lastPage = page;

            HashMap<String, String> hosts = new HashMap<String, String>();
            List<String> subservers = new ArrayList<String>();
            if (host != null) {
                lastVistedObject = host;
                for (String subserver : json.getJSONObject("hosts").getJSONObject(host).getJSONObject("servers").keySet()) {
                    hosts.put(subserver, host);
                    subservers.add(subserver);
                }
            } else {
                lastVistedObject = null;
                lastMenu = () -> subserverMenu(1, null);
                for (String tmphost : json.getJSONObject("hosts").keySet()) {
                    for (String tmpsubserver : json.getJSONObject("hosts").getJSONObject(tmphost).getJSONObject("servers").keySet()) {
                        hosts.put(tmpsubserver, tmphost);
                        subservers.add(tmpsubserver);
                    }
                }
            }
            windowHistory.add(() -> subserverMenu(page, host));

            ItemStack block;
            ItemMeta blockMeta;
            ItemStack div = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 15);
            ItemMeta divMeta = div.getItemMeta();
            divMeta.setDisplayName(ChatColor.RESET.toString());
            div.setItemMeta(divMeta);

            int i = 0;
            int min = ((page - 1) * 36);
            int max = (min + 35);
            int count = (subservers.size() == 0)?27:((subservers.size() - min - 1 >= max)?36:subservers.size() - min);
            int area = (count % 9 == 0) ? count : (int) (Math.floor(count / 9) + 1) * 9;

            Inventory inv = Bukkit.createInventory(null, 18 + area, (host == null)?plugin.lang.getSection("Lang").getColoredString("Interface.SubServer-Menu.Title", '&'):plugin.lang.getSection("Lang").getColoredString("Interface.Host-SubServer.Title", '&').replace("$str$", host));
            block = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 7);
            block.setItemMeta(divMeta);
            while (i < area) {
                inv.setItem(i, block);
                i++;
            }
            ItemStack adiv = block;
            i = (int) ((count < 9) ? Math.floor((9 - count) / 2) : 0);

            boolean even = (count & 1) == 0 && count < 9;
            short online, temp, offline, disabled;

            for (String subserver : subservers) {
                if (subservers.indexOf(subserver) >= min && subservers.indexOf(subserver) <= max) {
                    if (even && (i == 4 || i == 13 || i == 22 || i == 31)) inv.setItem(i++, adiv);
                    online = (short) (((i & 1) == 0) ? 5 : 13);
                    temp = (short) (((i & 1) == 0) ? 3 : 11);
                    offline = (short) (((i & 1) == 0) ? 4 : 1);
                    disabled = (short) (((i & 1) == 0) ? 2 : 14);

                    if (json.getJSONObject("hosts").getJSONObject(hosts.get(subserver)).getJSONObject("servers").getJSONObject(subserver).getBoolean("temp")) {
                        block = new ItemStack(Material.STAINED_GLASS_PANE, 1, temp);
                        blockMeta = block.getItemMeta();
                        blockMeta.setDisplayName(ChatColor.BLUE + subserver);
                        blockMeta.setLore(Arrays.asList(plugin.lang.getSection("Lang").getColoredString("Interface.SubServer-Menu.SubServer-Player-Count", '&').replace("$int$", Integer.toString(json.getJSONObject("hosts").getJSONObject(hosts.get(subserver)).getJSONObject("servers").getJSONObject(subserver).getJSONObject("players").keySet().size())), plugin.lang.getSection("Lang").getColoredString("Interface.SubServer-Menu.SubServer-Temporary", '&')));
                    } else if (json.getJSONObject("hosts").getJSONObject(hosts.get(subserver)).getJSONObject("servers").getJSONObject(subserver).getBoolean("running")) {
                        block = new ItemStack(Material.STAINED_GLASS_PANE, 1, online);
                        blockMeta = block.getItemMeta();
                        blockMeta.setDisplayName(ChatColor.GREEN + subserver);
                        blockMeta.setLore(Arrays.asList(plugin.lang.getSection("Lang").getColoredString("Interface.SubServer-Menu.SubServer-Player-Count", '&').replace("$int$", Integer.toString(json.getJSONObject("hosts").getJSONObject(hosts.get(subserver)).getJSONObject("servers").getJSONObject(subserver).getJSONObject("players").keySet().size()))));
                    } else if (json.getJSONObject("hosts").getJSONObject(hosts.get(subserver)).getJSONObject("servers").getJSONObject(subserver).getBoolean("enabled")) {
                        block = new ItemStack(Material.STAINED_GLASS_PANE, 1, offline);
                        blockMeta = block.getItemMeta();
                        blockMeta.setDisplayName(ChatColor.YELLOW + subserver);
                        blockMeta.setLore(Arrays.asList(plugin.lang.getSection("Lang").getColoredString("Interface.SubServer-Menu.SubServer-Offline", '&')));
                    } else {
                        block = new ItemStack(Material.STAINED_GLASS_PANE, 1, disabled);
                        blockMeta = block.getItemMeta();
                        blockMeta.setDisplayName(ChatColor.RED + subserver);
                        blockMeta.setLore(Arrays.asList(plugin.lang.getSection("Lang").getColoredString("Interface.SubServer-Menu.SubServer-Disabled", '&')));
                    }
                    block.setItemMeta(blockMeta);
                    inv.setItem(i, block);

                    count--;
                    if (count < 9 && (i == 8 || i == 17 || i == 26)) {
                        i += (int) Math.floor((9 - count) / 2) + 1;
                        even = (count & 1) == 0;
                    } else {
                        i++;
                    }
                }
            }

            if (subservers.size() == 0) {
                block = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 14);
                blockMeta = block.getItemMeta();
                blockMeta.setDisplayName(plugin.lang.getSection("Lang").getColoredString("Interface.SubServer-Menu.No-SubServers", '&'));
                block.setItemMeta(blockMeta);
                inv.setItem(12, block);
                inv.setItem(13, block);
                inv.setItem(14, block);
            }

            i = inv.getSize() - 18;
            while (i < inv.getSize()) {
                inv.setItem(i, div);
                i++;
            }
            i = inv.getSize() - 9;

            if (min != 0) {
                block = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 4);
                blockMeta = block.getItemMeta();
                blockMeta.setDisplayName(plugin.lang.getSection("Lang").getColoredString("Interface.Generic.Back-Arrow", '&'));
                block.setItemMeta(blockMeta);
                inv.setItem(i++, block);
                inv.setItem(i++, block);
            } else i += 2;
            i++;
            block = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) ((host == null)?11:14));
            blockMeta = block.getItemMeta();
            blockMeta.setDisplayName((host == null)?plugin.lang.getSection("Lang").getColoredString("Interface.SubServer-Menu.Host-Menu", '&'):plugin.lang.getSection("Lang").getColoredString("Interface.Generic.Back", '&'));
            block.setItemMeta(blockMeta);
            inv.setItem(i++, block);
            inv.setItem(i++, block);
            inv.setItem(i++, block);
            i++;
            if (subservers.size() - 1 > max) {
                block = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 4);
                blockMeta = block.getItemMeta();
                blockMeta.setDisplayName(plugin.lang.getSection("Lang").getColoredString("Interface.Generic.Next-Arrow", '&'));
                block.setItemMeta(blockMeta);
                inv.setItem(i++, block);
                inv.setItem(i, block);
            }

            Bukkit.getPlayer(player).openInventory(inv);
            open = true;
        }));
    }

    /**
     * Opens SubServer/&lt;name&gt;
     *
     * @param subserver SubServer Name
     */
    public void subserverAdmin(final String subserver) {
        setDownloading(ChatColor.stripColor(plugin.lang.getSection("Lang").getColoredString("Interface.SubServer-Admin.Title", '&').replace("$str$", subserver)));
        plugin.subdata.sendPacket(new PacketDownloadServerInfo(subserver, UUID.randomUUID().toString(), json -> {
            windowHistory.add(() -> subserverAdmin(subserver));
            if (!json.getString("type").equals("subserver")) {
                back();
            } else {
                setDownloading(null);
                lastVistedObject = subserver;
                ItemStack block;
                ItemMeta blockMeta;
                ItemStack div = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 15);
                ItemMeta divMeta = div.getItemMeta();
                divMeta.setDisplayName(ChatColor.RESET.toString());
                div.setItemMeta(divMeta);

                Inventory inv = Bukkit.createInventory(null, 36, plugin.lang.getSection("Lang").getColoredString("Interface.SubServer-Admin.Title", '&').replace("$str$", subserver));

                int i = 0;
                while (i < inv.getSize()) {
                    inv.setItem(i, div);
                    i++;
                }
                i = 0;

                if (json.getJSONObject("server").getBoolean("running")) {
                    if (!(Bukkit.getPlayer(player).hasPermission("subservers.subserver.terminate.*") || Bukkit.getPlayer(player).hasPermission("subservers.subserver.terminate." + subserver.toLowerCase()))) {
                        block = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 7);
                        blockMeta = block.getItemMeta();
                        blockMeta.setDisplayName(ChatColor.GRAY+ChatColor.stripColor(plugin.lang.getSection("Lang").getColoredString("Interface.SubServer-Admin.Terminate", '&')));
                        blockMeta.setLore(Arrays.asList(plugin.lang.getSection("Lang").getColoredString("Interface.Generic.Invalid-Permission", '&').replace("$str$", "subservers.subserver.terminate." + subserver.toLowerCase())));
                    } else {
                        block = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 14);
                        blockMeta = block.getItemMeta();
                        blockMeta.setDisplayName(plugin.lang.getSection("Lang").getColoredString("Interface.SubServer-Admin.Terminate", '&'));
                    }

                    block.setItemMeta(blockMeta);
                    inv.setItem(1, block);
                    inv.setItem(10, block);

                    if (!(Bukkit.getPlayer(player).hasPermission("subservers.subserver.stop.*") || Bukkit.getPlayer(player).hasPermission("subservers.subserver.stop." + subserver.toLowerCase()))) {
                        block = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 7);
                        blockMeta = block.getItemMeta();
                        blockMeta.setDisplayName(ChatColor.GRAY+ChatColor.stripColor(plugin.lang.getSection("Lang").getColoredString("Interface.SubServer-Admin.Stop", '&')));
                        blockMeta.setLore(Arrays.asList(plugin.lang.getSection("Lang").getColoredString("Interface.Generic.Invalid-Permission", '&').replace("$str$", "subservers.subserver.stop." + subserver.toLowerCase())));
                    } else {
                        block = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 2);
                        blockMeta = block.getItemMeta();
                        blockMeta.setDisplayName(plugin.lang.getSection("Lang").getColoredString("Interface.SubServer-Admin.Stop", '&'));
                    }
                    block.setItemMeta(blockMeta);
                    inv.setItem(2, block);
                    inv.setItem(3, block);
                    inv.setItem(11, block);
                    inv.setItem(12, block);

                    if (!(Bukkit.getPlayer(player).hasPermission("subservers.subserver.command.*") || Bukkit.getPlayer(player).hasPermission("subservers.subserver.command." + subserver.toLowerCase()))) {
                        block = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 7);
                        blockMeta = block.getItemMeta();
                        blockMeta.setDisplayName(ChatColor.GRAY+ChatColor.stripColor(plugin.lang.getSection("Lang").getColoredString("Interface.SubServer-Admin.Command", '&')));
                        blockMeta.setLore(Arrays.asList(plugin.lang.getSection("Lang").getColoredString("Interface.Generic.Invalid-Permission", '&').replace("$str$", "subservers.subserver.command." + subserver.toLowerCase())));
                    } else {
                        block = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 4);
                        blockMeta = block.getItemMeta();
                        blockMeta.setDisplayName(plugin.lang.getSection("Lang").getColoredString("Interface.SubServer-Admin.Command", '&'));
                    }
                    block.setItemMeta(blockMeta);
                    inv.setItem(5, block);
                    inv.setItem(6, block);
                    inv.setItem(7, block);
                    inv.setItem(14, block);
                    inv.setItem(15, block);
                    inv.setItem(16, block);

                    if (!(Bukkit.getPlayer(player).hasPermission("subservers.subserver.teleport.*") || Bukkit.getPlayer(player).hasPermission("subservers.subserver.teleport." + subserver.toLowerCase()))) {
                        block = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 7);
                        blockMeta = block.getItemMeta();
                        blockMeta.setDisplayName(ChatColor.GRAY+ChatColor.stripColor(plugin.lang.getSection("Lang").getColoredString("Interface.SubServer-Admin.Teleport", '&')));
                        blockMeta.setLore(Arrays.asList(plugin.lang.getSection("Lang").getColoredString("Interface.Generic.Invalid-Permission", '&').replace("$str$", "subservers.subserver.teleport." + subserver.toLowerCase())));
                    } else {
                        block = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 11);
                        blockMeta = block.getItemMeta();
                        blockMeta.setDisplayName(plugin.lang.getSection("Lang").getColoredString("Interface.SubServer-Admin.Teleport", '&'));
                    }
                    block.setItemMeta(blockMeta);
                    inv.setItem(27, block);
                    inv.setItem(28, block);
                } else {
                    if (!(Bukkit.getPlayer(player).hasPermission("subservers.subserver.start.*") || Bukkit.getPlayer(player).hasPermission("subservers.subserver.start." + subserver.toLowerCase()))) {
                        block = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 7);
                        blockMeta = block.getItemMeta();
                        blockMeta.setDisplayName(ChatColor.GRAY+ChatColor.stripColor(plugin.lang.getSection("Lang").getColoredString("Interface.SubServer-Admin.Start", '&')));
                        blockMeta.setLore(Arrays.asList(plugin.lang.getSection("Lang").getColoredString("Interface.Generic.Invalid-Permission", '&').replace("$str$", "subservers.subserver.start." + subserver.toLowerCase())));
                    } else if (!json.getJSONObject("server").getBoolean("enabled")) {
                        block = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 7);
                        blockMeta = block.getItemMeta();
                        blockMeta.setDisplayName(ChatColor.GRAY+ChatColor.stripColor(plugin.lang.getSection("Lang").getColoredString("Interface.SubServer-Admin.Start", '&')));
                    } else {
                        block = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 5);
                        blockMeta = block.getItemMeta();
                        blockMeta.setDisplayName(plugin.lang.getSection("Lang").getColoredString("Interface.SubServer-Admin.Start", '&'));
                    }
                    block.setItemMeta(blockMeta);
                    inv.setItem(1, block);
                    inv.setItem(2, block);
                    inv.setItem(3, block);
                    inv.setItem(10, block);
                    inv.setItem(11, block);
                    inv.setItem(12, block);

                    if (!(Bukkit.getPlayer(player).hasPermission("subservers.subserver.edit.*") || Bukkit.getPlayer(player).hasPermission("subservers.subserver.edit." + subserver.toLowerCase()))) {
                        block = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 7);
                        blockMeta = block.getItemMeta();
                        blockMeta.setDisplayName(ChatColor.GRAY+ChatColor.stripColor(plugin.lang.getSection("Lang").getColoredString("Interface.SubServer-Admin.Editor", '&')));
                        blockMeta.setLore(Arrays.asList(plugin.lang.getSection("Lang").getColoredString("Interface.Generic.Invalid-Permission", '&').replace("$str$", "subservers.subserver.edit." + subserver.toLowerCase())));
                    } else if (!json.getJSONObject("server").getBoolean("editable")) {
                        block = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 7);
                        blockMeta = block.getItemMeta();
                        blockMeta.setDisplayName(ChatColor.GRAY+ChatColor.stripColor(plugin.lang.getSection("Lang").getColoredString("Interface.SubServer-Admin.Editor", '&')));
                        blockMeta.setLore(Arrays.asList(plugin.lang.getSection("Lang").getColoredString("Interface.SubServer-Admin.Editor-Unavailable", '&')));
                    } else {
                        block = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 1);
                        blockMeta = block.getItemMeta();
                        blockMeta.setDisplayName(plugin.lang.getSection("Lang").getColoredString("Interface.SubServer-Admin.Editor", '&'));

                    }
                    block.setItemMeta(blockMeta);
                    inv.setItem(5, block);
                    inv.setItem(6, block);
                    inv.setItem(7, block);
                    inv.setItem(14, block);
                    inv.setItem(15, block);
                    inv.setItem(16, block);
                }

                if (json.getJSONObject("server").getBoolean("temp")) {
                    block = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 11);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(ChatColor.BLUE + subserver);
                    blockMeta.setLore(Arrays.asList(plugin.lang.getSection("Lang").getColoredString("Interface.SubServer-Menu.SubServer-Player-Count", '&').replace("$int$", Integer.toString(json.getJSONObject("server").getJSONObject("players").keySet().size())), plugin.lang.getSection("Lang").getColoredString("Interface.SubServer-Menu.SubServer-Temporary", '&')));
                } else if (json.getJSONObject("server").getBoolean("running")) {
                    block = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 5);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(ChatColor.GREEN + subserver);
                    blockMeta.setLore(Arrays.asList(plugin.lang.getSection("Lang").getColoredString("Interface.SubServer-Menu.SubServer-Player-Count", '&').replace("$int$", Integer.toString(json.getJSONObject("server").getJSONObject("players").keySet().size()))));
                } else if (json.getJSONObject("server").getBoolean("enabled")) {
                    block = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 4);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(ChatColor.YELLOW + subserver);
                    blockMeta.setLore(Arrays.asList(plugin.lang.getSection("Lang").getColoredString("Interface.SubServer-Menu.SubServer-Offline", '&')));
                } else {
                    block = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 14);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(ChatColor.RED + subserver);
                    blockMeta.setLore(Arrays.asList(plugin.lang.getSection("Lang").getColoredString("Interface.SubServer-Menu.SubServer-Disabled", '&')));
                }
                block.setItemMeta(blockMeta);
                inv.setItem(30, block);
                inv.setItem(31, block);
                inv.setItem(32, block);


                block = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 14);
                blockMeta = block.getItemMeta();
                blockMeta.setDisplayName(plugin.lang.getSection("Lang").getColoredString("Interface.Generic.Back", '&'));
                block.setItemMeta(blockMeta);
                inv.setItem(34, block);
                inv.setItem(35, block);

                Bukkit.getPlayer(player).openInventory(inv);
                open = true;
            }
        }));

    }

    /**
     * Opens SubServer/&lt;name&gt;/Edit
     *
     * @param options SubServerEditorOptions
     */
    public void subserverEditor(final SubServerEditorOptions options) {
        lastUsedOptions = options;
        if (!options.init()) {
            windowHistory.add(() -> subserverEditor(options));
            lastVistedObject = options.getSubserver();
        }
    }
    public static class SubServerEditorOptions extends Options {
        private List<Runnable> history = new LinkedList<Runnable>();
        private String subserver;
        private boolean init = false;

        /**
         * Grabs a raw SubServerEditorOptions instance
         *
         * @param subserver SubServer Name
         */
        public SubServerEditorOptions(String subserver) {
            this.subserver = subserver;
        }

        /**
         * Gets the SubServer Name
         *
         * @return SubServer Name
         */
        public String getSubserver() {
            return this.subserver;
        }
    }
}
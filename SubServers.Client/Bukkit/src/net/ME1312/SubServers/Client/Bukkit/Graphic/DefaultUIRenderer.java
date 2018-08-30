package net.ME1312.SubServers.Client.Bukkit.Graphic;

import net.ME1312.SubServers.Client.Bukkit.Library.Container;
import net.ME1312.SubServers.Client.Bukkit.Library.NamedContainer;
import net.ME1312.SubServers.Client.Bukkit.Library.Version.Version;
import net.ME1312.SubServers.Client.Bukkit.Network.API.Host;
import net.ME1312.SubServers.Client.Bukkit.Network.API.Server;
import net.ME1312.SubServers.Client.Bukkit.Network.API.SubCreator;
import net.ME1312.SubServers.Client.Bukkit.Network.API.SubServer;
import net.ME1312.SubServers.Client.Bukkit.SubPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.DecimalFormat;
import java.util.*;

/**
 * Default GUI Renderer Class
 */
public class DefaultUIRenderer extends UIRenderer {
    private static int MAX_VISITED_OBJECTS = 2;
    private List<Runnable> windowHistory = new LinkedList<Runnable>();
    protected Object[] lastVisitedObjects = new Object[MAX_VISITED_OBJECTS];
    protected int lastPage = 1;
    protected Runnable lastMenu = null;
    protected boolean open = false;
    protected final UUID player;
    private SubPlugin plugin;

    protected DefaultUIRenderer(SubPlugin plugin, UUID player) {
        super(plugin, player);
        this.plugin = plugin;
        this.player = player;
    }

    public void newUI() {
        clearHistory();
        if (lastMenu == null) {
            hostMenu(1);
        } else {
            lastMenu.run();
        }
    }

    public void clearHistory() {
        windowHistory.clear();
    }

    public boolean hasHistory() {
        return windowHistory.size() > 1;
    }

    public void reopen() {
        Runnable lastWindow = windowHistory.get(windowHistory.size() - 1);
        windowHistory.remove(windowHistory.size() - 1);
        lastWindow.run();
    }

    public void back() {
        windowHistory.remove(windowHistory.size() - 1);
        reopen();
    }

    ItemStack createItem(String material, String name, short damage) {
        try {
            if (plugin.api.getGameVersion().compareTo(new Version("1.13")) < 0) {
                return ItemStack.class.getConstructor(Material.class, int.class, short.class).newInstance(Material.valueOf(material), 1, damage);
            } else {
                return new ItemStack((Material) Material.class.getMethod("getMaterial", String.class, boolean.class).invoke(null, name, false), 1);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void hostMenu(final int page) {
        setDownloading(ChatColor.stripColor(plugin.api.getLang("SubServers", "Interface.Host-Menu.Title")));
        plugin.api.getHosts(hosts -> plugin.api.getGroups(groups -> {
            setDownloading(null);
            lastVisitedObjects[0] = null;
            lastPage = page;
            lastMenu = () -> hostMenu(1);
            windowHistory.add(() -> hostMenu(page));
            List<Host> index = new LinkedList<Host>();
            index.addAll(hosts.values());

            ItemStack block;
            ItemMeta blockMeta;
            ItemStack div = createItem("STAINED_GLASS_PANE", "BLACK_STAINED_GLASS_PANE", (short) 15);
            ItemMeta divMeta = div.getItemMeta();
            divMeta.setDisplayName(ChatColor.RESET.toString());
            div.setItemMeta(divMeta);

            int i = 0;
            int min = ((page - 1) * 36);
            int max = (min + 35);
            int count = (index.size() == 0)?27:((index.size() - min >= max)?36:index.size() - min);
            int area = (count % 9 == 0) ? count : (int) (Math.floor(count / 9) + 1) * 9;

            Inventory inv = Bukkit.createInventory(null, 18 + area, plugin.api.getLang("SubServers", "Interface.Host-Menu.Title"));
            block = createItem("STAINED_GLASS_PANE", "GRAY_STAINED_GLASS_PANE", (short) 7);
            block.setItemMeta(divMeta);
            while (i < area) {
                inv.setItem(i, block);
                i++;
            }
            ItemStack adiv = block;
            i = (int) ((count < 9) ? Math.floor((9 - count) / 2) : 0);

            boolean even = (count & 1) == 0 && count < 9;
            NamedContainer<String, Short> enabled, disabled;

            for (Host host : index) {
                if (index.indexOf(host) >= min && index.indexOf(host) <= max) {
                    if (even && (i == 4 || i == 13 || i == 22 || i == 31)) inv.setItem(i++, adiv);
                    enabled = (((i & 1) == 0) ? new NamedContainer<>("BLUE_STAINED_GLASS_PANE", (short) 3) : new NamedContainer<>("LIGHT_BLUE_STAINED_GLASS_PANE", (short) 11));
                    disabled = (((i & 1) == 0) ? new NamedContainer<>("MAGENTA_STAINED_GLASS_PANE", (short) 2) : new NamedContainer<>("RED_STAINED_GLASS_PANE", (short) 14));

                    if (host.isAvailable() && host.isEnabled()) {
                        block = createItem("STAINED_GLASS_PANE", enabled.name(), enabled.get());
                        blockMeta = block.getItemMeta();
                        blockMeta.setDisplayName(ChatColor.AQUA + host.getDisplayName());
                        LinkedList<String> lore = new LinkedList<String>();
                        if (!host.getName().equals(host.getDisplayName()))
                            lore.add(ChatColor.GRAY + host.getName());
                        lore.add(plugin.api.getLang("SubServers", "Interface.Host-Menu.Host-Server-Count").replace("$int$", new DecimalFormat("#,###").format(host.getSubServers().keySet().size())));
                        if (plugin.config.get().getSection("Settings").getBoolean("Show-Addresses", false)) lore.add(ChatColor.WHITE + host.getAddress().getHostAddress());
                        blockMeta.setLore(lore);
                    } else {
                        block = createItem("STAINED_GLASS_PANE", disabled.name(), disabled.get());
                        blockMeta = block.getItemMeta();
                        blockMeta.setDisplayName(ChatColor.RED + host.getDisplayName());
                        LinkedList<String> lore = new LinkedList<String>();
                        if (!host.getName().equals(host.getDisplayName()))
                            lore.add(ChatColor.GRAY + host.getName());
                        if (!host.isAvailable()) lore.add(plugin.api.getLang("SubServers", "Interface.Host-Menu.Host-Unavailable"));
                        else lore.add(plugin.api.getLang("SubServers", "Interface.Host-Menu.Host-Disabled"));
                        if (plugin.config.get().getSection("Settings").getBoolean("Show-Addresses", false)) lore.add(ChatColor.WHITE + host.getAddress().getHostAddress());
                        blockMeta.setLore(lore);
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

            if (index.size() == 0) {
                block = createItem("STAINED_GLASS_PANE", "RED_STAINED_GLASS_PANE", (short) 14);
                blockMeta = block.getItemMeta();
                blockMeta.setDisplayName(plugin.api.getLang("SubServers", "Interface.Host-Menu.No-Hosts"));
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
                block = createItem("STAINED_GLASS_PANE", "YELLOW_STAINED_GLASS_PANE", (short) 4);
                blockMeta = block.getItemMeta();
                blockMeta.setDisplayName(plugin.api.getLang("SubServers", "Interface.Generic.Back-Arrow"));
                block.setItemMeta(blockMeta);
                inv.setItem(i++, block);
                inv.setItem(i++, block);
            } else i += 2;
            i++;
            if (groups.keySet().size() <= 0) {
                block = createItem("STAINED_GLASS_PANE", "LIME_STAINED_GLASS_PANE", (short) 5);
                blockMeta = block.getItemMeta();
                blockMeta.setDisplayName(plugin.api.getLang("SubServers", "Interface.Host-Menu.Server-Menu"));
            } else {
                block = createItem("STAINED_GLASS_PANE", "ORANGE_STAINED_GLASS_PANE", (short) 1);
                blockMeta = block.getItemMeta();
                blockMeta.setDisplayName(plugin.api.getLang("SubServers", "Interface.Host-Menu.Group-Menu"));
            }
            block.setItemMeta(blockMeta);
            inv.setItem(i++, block);
            inv.setItem(i++, block);
            inv.setItem(i++, block);
            i++;
            if (index.size() - 1 > max) {
                block = createItem("STAINED_GLASS_PANE", "YELLOW_STAINED_GLASS_PANE", (short) 4);
                blockMeta = block.getItemMeta();
                blockMeta.setDisplayName(plugin.api.getLang("SubServers", "Interface.Generic.Next-Arrow"));
                block.setItemMeta(blockMeta);
                inv.setItem(i++, block);
                inv.setItem(i, block);
            }

            Bukkit.getPlayer(player).openInventory(inv);
            open = true;
        }));
    }

    public void hostAdmin(final String name) {
        setDownloading(ChatColor.stripColor(plugin.api.getLang("SubServers", "Interface.Host-Admin.Title").replace("$str$", name)));
        plugin.api.getHost(name, host -> {
            windowHistory.add(() -> hostAdmin(name));
            if (host == null) {
                if (hasHistory()) back();
            } else {
                setDownloading(null);
                lastVisitedObjects[0] = name;

                ItemStack block;
                ItemMeta blockMeta;
                ItemStack div = createItem("STAINED_GLASS_PANE", "BLACK_STAINED_GLASS_PANE", (short) 15);
                ItemMeta divMeta = div.getItemMeta();
                divMeta.setDisplayName(ChatColor.RESET.toString());
                div.setItemMeta(divMeta);

                Inventory inv = Bukkit.createInventory(null, 36, plugin.api.getLang("SubServers", "Interface.Host-Admin.Title").replace("$str$", host.getDisplayName()));

                int i = 0;
                while (i < inv.getSize()) {
                    inv.setItem(i, div);
                    i++;
                }

                if (!(Bukkit.getPlayer(player).hasPermission("subservers.host.create.*") || Bukkit.getPlayer(player).hasPermission("subservers.host.create." + name.toLowerCase()))) {
                    block = createItem("STAINED_GLASS_PANE", "GRAY_STAINED_GLASS_PANE", (short) 7);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(ChatColor.GRAY+ChatColor.stripColor(plugin.api.getLang("SubServers", "Interface.Host-Admin.Creator")));
                    blockMeta.setLore(Arrays.asList(plugin.api.getLang("SubServers", "Interface.Generic.Invalid-Permission").replace("$str$", "subservers.host.create." + name.toLowerCase())));
                } else if (!host.isEnabled()) {
                    block = createItem("STAINED_GLASS_PANE", "GRAY_STAINED_GLASS_PANE", (short) 7);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(ChatColor.GRAY+ChatColor.stripColor(plugin.api.getLang("SubServers", "Interface.Host-Admin.Creator")));
                } else {
                    block = createItem("STAINED_GLASS_PANE", "YELLOW_STAINED_GLASS_PANE", (short) 4);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(plugin.api.getLang("SubServers", "Interface.Host-Admin.Creator"));
                }
                block.setItemMeta(blockMeta);
                inv.setItem(1, block);
                inv.setItem(2, block);
                inv.setItem(3, block);
                inv.setItem(10, block);
                inv.setItem(11, block);
                inv.setItem(12, block);

                block = createItem("STAINED_GLASS_PANE", "LIME_STAINED_GLASS_PANE", (short) 5);
                blockMeta = block.getItemMeta();
                blockMeta.setDisplayName(plugin.api.getLang("SubServers", "Interface.Host-Admin.SubServers"));
                block.setItemMeta(blockMeta);
                inv.setItem(5, block);
                inv.setItem(6, block);
                inv.setItem(7, block);
                inv.setItem(14, block);
                inv.setItem(15, block);
                inv.setItem(16, block);

                if (!host.isEnabled() || hostPlugins.size() <= 0) {
                    block = div;
                } else {
                    block = createItem("STAINED_GLASS_PANE", "BLUE_STAINED_GLASS_PANE", (short) 11);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(plugin.api.getLang("SubServers", "Interface.Host-Admin.Plugins"));
                    block.setItemMeta(blockMeta);
                }
                inv.setItem(27, block);
                inv.setItem(28, block);

                if (host.isAvailable() && host.isEnabled()) {
                    block = createItem("STAINED_GLASS_PANE", "BLUE_STAINED_GLASS_PANE", (short) 11);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(ChatColor.AQUA + host.getDisplayName());
                    LinkedList<String> lore = new LinkedList<String>();
                    if (!host.getName().equals(host.getDisplayName()))
                        lore.add(ChatColor.GRAY + host.getName());
                    lore.add(plugin.api.getLang("SubServers", "Interface.Host-Menu.Host-Server-Count").replace("$int$", new DecimalFormat("#,###").format(host.getSubServers().keySet().size())));
                    if (plugin.config.get().getSection("Settings").getBoolean("Show-Addresses", false)) lore.add(ChatColor.WHITE + host.getAddress().getHostAddress());
                    blockMeta.setLore(lore);
                } else {
                    block = createItem("STAINED_GLASS_PANE", "RED_STAINED_GLASS_PANE", (short) 14);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(ChatColor.RED + host.getDisplayName());
                    LinkedList<String> lore = new LinkedList<String>();
                    if (!host.getName().equals(host.getDisplayName()))
                        lore.add(ChatColor.GRAY + host.getName());
                    if (!host.isAvailable()) lore.add(plugin.api.getLang("SubServers", "Interface.Host-Menu.Host-Unavailable"));
                    else lore.add(plugin.api.getLang("SubServers", "Interface.Host-Menu.Host-Disabled"));
                    if (plugin.config.get().getSection("Settings").getBoolean("Show-Addresses", false)) lore.add(ChatColor.WHITE + host.getAddress().getHostAddress());
                    blockMeta.setLore(lore);
                }
                block.setItemMeta(blockMeta);
                inv.setItem(30, block);
                inv.setItem(31, block);
                inv.setItem(32, block);


                if (hasHistory()) {
                    block = createItem("STAINED_GLASS_PANE", "RED_STAINED_GLASS_PANE", (short) 14);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(plugin.api.getLang("SubServers", "Interface.Generic.Back"));
                    block.setItemMeta(blockMeta);
                    inv.setItem(34, block);
                    inv.setItem(35, block);
                }

                Bukkit.getPlayer(player).openInventory(inv);
                open = true;
            }
        });
    }

    public void hostCreator(final CreatorOptions options) {
        setDownloading(ChatColor.stripColor(plugin.api.getLang("SubServers", "Interface.Host-Creator.Title").replace("$str$", options.getHost())));
        if (!options.init())
            windowHistory.add(() -> hostCreator(options));
        lastVisitedObjects[0] = options;

        plugin.api.getHost(options.getHost(), host -> {
            if (host == null || !host.isEnabled()) {
                lastVisitedObjects[0] = null;
                if (hasHistory()) back();
            } else {
                setDownloading(null);
                ItemStack block;
                ItemMeta blockMeta;
                ItemStack div = createItem("STAINED_GLASS_PANE", "BLACK_STAINED_GLASS_PANE", (short) 15);
                ItemMeta divMeta = div.getItemMeta();
                divMeta.setDisplayName(ChatColor.RESET.toString());
                div.setItemMeta(divMeta);

                Inventory inv = Bukkit.createInventory(null, 54, plugin.api.getLang("SubServers", "Interface.Host-Creator.Title").replace("$str$", host.getDisplayName()));

                int i = 0;
                while (i < inv.getSize()) {
                    inv.setItem(i, div);
                    i++;
                }

                if (options.getName() == null) {
                    block = createItem("STAINED_GLASS_PANE", "RED_STAINED_GLASS_PANE", (short) 14);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(ChatColor.RED + plugin.api.getLang("SubServers", "Interface.Host-Creator.Edit-Name"));
                } else {
                    block = createItem("STAINED_GLASS_PANE", "LIME_STAINED_GLASS_PANE", (short) 5);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(ChatColor.GREEN + plugin.api.getLang("SubServers", "Interface.Host-Creator.Edit-Name"));
                    blockMeta.setLore(Arrays.asList(ChatColor.GRAY + options.getName()));
                }
                block.setItemMeta(blockMeta);
                inv.setItem(10, block);
                inv.setItem(11, block);
                inv.setItem(12, block);

                if (options.getPort() <= 0) {
                    block = createItem("STAINED_GLASS_PANE", "RED_STAINED_GLASS_PANE", (short) 14);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(ChatColor.RED + plugin.api.getLang("SubServers", "Interface.Host-Creator.Edit-Port"));
                } else {
                    block = createItem("STAINED_GLASS_PANE", "LIME_STAINED_GLASS_PANE", (short) 5);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(ChatColor.GREEN + plugin.api.getLang("SubServers", "Interface.Host-Creator.Edit-Port"));
                    blockMeta.setLore(Arrays.asList(ChatColor.GRAY.toString() + options.getPort()));
                }
                block.setItemMeta(blockMeta);
                inv.setItem(14, block);
                inv.setItem(15, block);
                inv.setItem(16, block);

                if (options.getTemplate() == null) {
                    block = createItem("STAINED_GLASS_PANE", "RED_STAINED_GLASS_PANE", (short) 14);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(ChatColor.RED + plugin.api.getLang("SubServers", "Interface.Host-Creator.Edit-Template"));
                } else {
                    block = createItem("STAINED_GLASS_PANE", "LIME_STAINED_GLASS_PANE", (short) 5);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(ChatColor.GREEN + plugin.api.getLang("SubServers", "Interface.Host-Creator.Edit-Template"));
                    blockMeta.setLore(Arrays.asList(ChatColor.GRAY + options.getTemplate()));
                }
                block.setItemMeta(blockMeta);
                inv.setItem(28, block);
                inv.setItem(29, block);
                inv.setItem(30, block);

                if (options.getVersion() == null) {
                    block = createItem("STAINED_GLASS_PANE", "RED_STAINED_GLASS_PANE", (short) 14);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(ChatColor.RED + plugin.api.getLang("SubServers", "Interface.Host-Creator.Edit-Version"));
                } else {
                    block = createItem("STAINED_GLASS_PANE", "LIME_STAINED_GLASS_PANE", (short) 5);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(ChatColor.GREEN + plugin.api.getLang("SubServers", "Interface.Host-Creator.Edit-Version"));
                    blockMeta.setLore(Arrays.asList(ChatColor.GRAY + "Minecraft " + options.getVersion().toString()));
                }
                block.setItemMeta(blockMeta);
                inv.setItem(32, block);
                inv.setItem(33, block);
                inv.setItem(34, block);

                if (!options.hasHistory()) {
                    block = createItem("STAINED_GLASS_PANE", "GRAY_STAINED_GLASS_PANE", (short) 7);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(ChatColor.GRAY + ChatColor.stripColor(plugin.api.getLang("SubServers", "Interface.Generic.Undo")));
                    block.setItemMeta(blockMeta);
                } else {
                    block = createItem("STAINED_GLASS_PANE", "ORANGE_STAINED_GLASS_PANE", (short) 1);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(plugin.api.getLang("SubServers", "Interface.Generic.Undo"));
                    block.setItemMeta(blockMeta);
                }
                block.setItemMeta(blockMeta);
                inv.setItem(45, block);
                inv.setItem(46, block);

                if (options.getName() == null || options.getTemplate() == null || options.getVersion() == null || options.getPort() <= 0 && options.getMemory() < 256) {
                    block = createItem("STAINED_GLASS_PANE", "GRAY_STAINED_GLASS_PANE", (short) 7);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(ChatColor.GRAY + ChatColor.stripColor(plugin.api.getLang("SubServers", "Interface.Host-Creator.Submit")));
                    blockMeta.setLore(Arrays.asList(plugin.api.getLang("SubServers", "Interface.Host-Creator.Form-Incomplete")));
                    block.setItemMeta(blockMeta);
                } else {
                    block = createItem("STAINED_GLASS_PANE", "YELLOW_STAINED_GLASS_PANE", (short) 4);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(plugin.api.getLang("SubServers", "Interface.Host-Creator.Submit"));
                    block.setItemMeta(blockMeta);
                }
                block.setItemMeta(blockMeta);
                inv.setItem(48, block);
                inv.setItem(49, block);
                inv.setItem(50, block);

                if (hasHistory()) {
                    block = createItem("STAINED_GLASS_PANE", "RED_STAINED_GLASS_PANE", (short) 14);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(plugin.api.getLang("SubServers", "Interface.Generic.Back"));
                    block.setItemMeta(blockMeta);
                    inv.setItem(52, block);
                    inv.setItem(53, block);
                }

                Bukkit.getPlayer(player).openInventory(inv);
                open = true;
            }
        });
    }

    public void hostCreatorTemplates(final int page, final CreatorOptions options) {
        setDownloading(ChatColor.stripColor(plugin.api.getLang("SubServers", "Interface.Host-Creator.Edit-Template.Title").replace("$str$", options.getHost())));
        lastVisitedObjects[0] = options;
        if (!options.init()) lastVisitedObjects[0] = options.getHost();
        plugin.api.getHost(options.getHost(), host -> {
            if (host == null || !host.isEnabled()) {
                lastVisitedObjects[0] = null;
                if (hasHistory()) back();
            } else {
                lastPage = page;
                setDownloading(null);
                List<SubCreator.ServerTemplate> index = new LinkedList<SubCreator.ServerTemplate>();
                for (SubCreator.ServerTemplate template : host.getCreator().getTemplates().values()) {
                    if (template.isEnabled()) index.add(template);
                }

                ItemStack block;
                ItemMeta blockMeta;
                ItemStack div = createItem("STAINED_GLASS_PANE", "BLACK_STAINED_GLASS_PANE", (short) 15);
                ItemMeta divMeta = div.getItemMeta();
                divMeta.setDisplayName(ChatColor.RESET.toString());
                div.setItemMeta(divMeta);

                int i = 0;
                int min = ((page - 1) * 36);
                int max = (min + 35);
                int count = (index.size() == 0)?27:((index.size() - min >= max)?36:index.size() - min);
                int area = (count % 9 == 0)?count: (int) (Math.floor(count / 9) + 1) * 9;

                Inventory inv = Bukkit.createInventory(null, 18 + area, plugin.api.getLang("SubServers", "Interface.Host-Creator.Edit-Template.Title").replace("$str$", host.getDisplayName()));
                block = createItem("STAINED_GLASS_PANE", "GRAY_STAINED_GLASS_PANE", (short) 7);
                block.setItemMeta(divMeta);
                while (i < area) {
                    inv.setItem(i, block);
                    i++;
                }
                ItemStack adiv = block;
                i = (int) ((count < 9) ? Math.floor((9 - count) / 2) : 0);

                boolean even = (count & 1) == 0 && count < 9;
                for (SubCreator.ServerTemplate template : index) {
                    if (index.indexOf(template) >= min && index.indexOf(template) <= max) {
                        if (even && (i == 4 || i == 13 || i == 22 || i == 31)) inv.setItem(i++, adiv);

                        block = parseItem(template.getIcon(), new ItemStack(Material.ENDER_CHEST));
                        blockMeta = block.getItemMeta();
                        blockMeta.setDisplayName(ChatColor.YELLOW + template.getDisplayName());
                        LinkedList<String> lore = new LinkedList<String>();
                        if (!template.getName().equals(template.getDisplayName()))
                            lore.add(ChatColor.GRAY + template.getName());
                        blockMeta.setLore(lore);
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

                if (index.size() == 0) {
                    block = createItem("STAINED_GLASS_PANE", "RED_STAINED_GLASS_PANE", (short) 14);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(plugin.api.getLang("SubServers", "Interface.Host-Creator.Edit-Template.No-Templates"));
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
                    block = createItem("STAINED_GLASS_PANE", "YELLOW_STAINED_GLASS_PANE", (short) 4);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(plugin.api.getLang("SubServers", "Interface.Generic.Back-Arrow"));
                    block.setItemMeta(blockMeta);
                    inv.setItem(i++, block);
                    inv.setItem(i++, block);
                } else i += 2;
                i++;
                block = createItem("STAINED_GLASS_PANE", "RED_STAINED_GLASS_PANE", (short) 14);
                blockMeta = block.getItemMeta();
                blockMeta.setDisplayName(plugin.api.getLang("SubServers", "Interface.Generic.Back"));
                block.setItemMeta(blockMeta);
                inv.setItem(i++, block);
                inv.setItem(i++, block);
                inv.setItem(i++, block);
                i++;
                if (index.size() - 1 > max) {
                    block = createItem("STAINED_GLASS_PANE", "YELLOW_STAINED_GLASS_PANE", (short) 4);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(plugin.api.getLang("SubServers", "Interface.Generic.Next-Arrow"));
                    block.setItemMeta(blockMeta);
                    inv.setItem(i++, block);
                    inv.setItem(i, block);
                }

                Bukkit.getPlayer(player).openInventory(inv);
                open = true;
            }
        });
    }

    public void hostPlugin(final int page, final String name) {
        setDownloading(ChatColor.stripColor(plugin.api.getLang("SubServers", "Interface.Host-Plugin.Title").replace("$str$", name)));
        plugin.api.getHost(name, host -> {
            windowHistory.add(() -> hostPlugin(page, name));
            if (host == null) {
                if (hasHistory()) back();
            } else {
                setDownloading(null);
                lastVisitedObjects[0] = name;
                lastPage = page;
                List<String> renderers = new LinkedList<String>();
                for (String renderer : renderers) {
                    if (hostPlugins.get(renderer).isEnabled(host)) renderers.add(renderer);
                }
                Collections.sort(renderers);

                ItemStack block;
                ItemMeta blockMeta;
                ItemStack div = createItem("STAINED_GLASS_PANE", "BLACK_STAINED_GLASS_PANE", (short) 15);
                ItemMeta divMeta = div.getItemMeta();
                divMeta.setDisplayName(ChatColor.RESET.toString());
                div.setItemMeta(divMeta);

                int i = 0;
                int min = ((page - 1) * 36);
                int max = (min + 35);
                int count = (renderers.size() == 0)?27:((renderers.size() - min >= max)?36:renderers.size() - min);
                int area = (count % 9 == 0) ? count : (int) (Math.floor(count / 9) + 1) * 9;

                Inventory inv = Bukkit.createInventory(null, 18 + area, plugin.api.getLang("SubServers", "Interface.Host-Plugin.Title").replace("$str$", host.getDisplayName()));
                block = createItem("STAINED_GLASS_PANE", "GRAY_STAINED_GLASS_PANE", (short) 7);
                block.setItemMeta(divMeta);
                while (i < area) {
                    inv.setItem(i, block);
                    i++;
                }
                ItemStack adiv = block;
                i = (int) ((count < 9) ? Math.floor((9 - count) / 2) : 0);

                boolean even = (count & 1) == 0 && count < 9;

                for (String renderer : renderers) {
                    if (renderers.indexOf(renderer) >= min && renderers.indexOf(renderer) <= max) {
                        if (even && (i == 4 || i == 13 || i == 22 || i == 31)) inv.setItem(i++, adiv);

                        inv.setItem(i, hostPlugins.get(renderer).getIcon());

                        count--;
                        if (count < 9 && (i == 8 || i == 17 || i == 26)) {
                            i += (int) Math.floor((9 - count) / 2) + 1;
                            even = (count & 1) == 0;
                        } else {
                            i++;
                        }
                    }
                }

                if (renderers.size() == 0) {
                    block = createItem("STAINED_GLASS_PANE", "RED_STAINED_GLASS_PANE", (short) 14);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(plugin.api.getLang("SubServers", "Interface.Host-Plugin.No-Plugins"));
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
                    block = createItem("STAINED_GLASS_PANE", "YELLOW_STAINED_GLASS_PANE", (short) 4);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(plugin.api.getLang("SubServers", "Interface.Generic.Back-Arrow"));
                    block.setItemMeta(blockMeta);
                    inv.setItem(i++, block);
                    inv.setItem(i++, block);
                } else i += 2;
                i++;
                if (hasHistory()) {
                    block = createItem("STAINED_GLASS_PANE", "RED_STAINED_GLASS_PANE", (short) 14);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(plugin.api.getLang("SubServers", "Interface.Generic.Back"));
                    block.setItemMeta(blockMeta);
                    inv.setItem(i++, block);
                    inv.setItem(i++, block);
                    inv.setItem(i++, block);
                    i++;
                }
                if (renderers.size() - 1 > max) {
                    block = createItem("STAINED_GLASS_PANE", "YELLOW_STAINED_GLASS_PANE", (short) 4);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(plugin.api.getLang("SubServers", "Interface.Generic.Next-Arrow"));
                    block.setItemMeta(blockMeta);
                    inv.setItem(i++, block);
                    inv.setItem(i, block);
                }

                Bukkit.getPlayer(player).openInventory(inv);
                open = true;
            }
        });
    }

    public void groupMenu(final int page) {
        setDownloading(ChatColor.stripColor(plugin.api.getLang("SubServers", "Interface.Group-Menu.Title")));
        plugin.api.getGroups(groups -> {
            setDownloading(null);
            lastVisitedObjects[0] = null;
            lastPage = page;
            lastMenu = () -> groupMenu(1);
            windowHistory.add(() -> groupMenu(page));
            List<String> index = new LinkedList<String>();
            index.addAll(groups.keySet());

            ItemStack block;
            ItemMeta blockMeta;
            ItemStack div = createItem("STAINED_GLASS_PANE", "BLACK_STAINED_GLASS_PANE", (short) 15);
            ItemMeta divMeta = div.getItemMeta();
            divMeta.setDisplayName(ChatColor.RESET.toString());
            div.setItemMeta(divMeta);

            int i = 0;
            int min = ((page - 1) * 36);
            int max = (min + 35);
            int count = (index.size() == 0)?27:((index.size() - min >= max)?36:index.size() - min);
            int area = (count % 9 == 0) ? count : (int) (Math.floor(count / 9) + 1) * 9;

            Inventory inv = Bukkit.createInventory(null, 18 + area, plugin.api.getLang("SubServers", "Interface.Group-Menu.Title"));
            block = createItem("STAINED_GLASS_PANE", "GRAY_STAINED_GLASS_PANE", (short) 7);
            block.setItemMeta(divMeta);
            while (i < area) {
                inv.setItem(i, block);
                i++;
            }
            ItemStack adiv = block;
            i = (int) ((count < 9) ? Math.floor((9 - count) / 2) : 0);

            boolean even = (count & 1) == 0 && count < 9;
            NamedContainer<String, Short> color;

            for (String group : index) {
                if (index.indexOf(group) >= min && index.indexOf(group) <= max) {
                    if (even && (i == 4 || i == 13 || i == 22 || i == 31)) inv.setItem(i++, adiv);
                    color = (((i & 1) == 0) ? new NamedContainer<>("ORANGE_STAINED_GLASS_PANE", (short) 1) : new NamedContainer<>("YELLOW_STAINED_GLASS_PANE", (short) 4));

                    block = createItem("STAINED_GLASS_PANE", color.name(), color.get());
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(ChatColor.GOLD + group);
                    LinkedList<String> lore = new LinkedList<String>();
                    lore.add(plugin.api.getLang("SubServers", "Interface.Group-Menu.Group-Server-Count").replace("$int$", new DecimalFormat("#,###").format(groups.get(group).size())));
                    blockMeta.setLore(lore);
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

            if (index.size() == 0) {
                block = createItem("STAINED_GLASS_PANE", "RED_STAINED_GLASS_PANE", (short) 14);
                blockMeta = block.getItemMeta();
                blockMeta.setDisplayName(plugin.api.getLang("SubServers", "Interface.Group-Menu.No-Groups"));
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
                block = createItem("STAINED_GLASS_PANE", "YELLOW_STAINED_GLASS_PANE", (short) 4);
                blockMeta = block.getItemMeta();
                blockMeta.setDisplayName(plugin.api.getLang("SubServers", "Interface.Generic.Back-Arrow"));
                block.setItemMeta(blockMeta);
                inv.setItem(i++, block);
                inv.setItem(i++, block);
            } else i += 2;
            i++;
            block = createItem("STAINED_GLASS_PANE", "LIME_STAINED_GLASS_PANE", (short) 5);
            blockMeta = block.getItemMeta();
            blockMeta.setDisplayName(plugin.api.getLang("SubServers", "Interface.Group-Menu.Server-Menu"));
            block.setItemMeta(blockMeta);
            inv.setItem(i++, block);
            inv.setItem(i++, block);
            inv.setItem(i++, block);
            i++;
            if (index.size() - 1 > max) {
                block = createItem("STAINED_GLASS_PANE", "YELLOW_STAINED_GLASS_PANE", (short) 4);
                blockMeta = block.getItemMeta();
                blockMeta.setDisplayName(plugin.api.getLang("SubServers", "Interface.Generic.Next-Arrow"));
                block.setItemMeta(blockMeta);
                inv.setItem(i++, block);
                inv.setItem(i, block);
            }

            Bukkit.getPlayer(player).openInventory(inv);
            open = true;
        });
    }

    public void serverMenu(final int page, final String host, final String group) {
        setDownloading(ChatColor.stripColor((host == null)?((group == null)?plugin.api.getLang("SubServers", "Interface.Server-Menu.Title"):plugin.api.getLang("SubServers", "Interface.Group-SubServer.Title").replace("$str$", group)):plugin.api.getLang("SubServers", "Interface.Host-SubServer.Title").replace("$str$", host)));
        Container<String> hostname = new Container<String>(host);
        Container<List<Server>> servercontainer = new Container<List<Server>>(new LinkedList<Server>());
        Runnable renderer = () -> {
            setDownloading(null);
            lastPage = page;

            List<Server> servers = servercontainer.get();
            lastVisitedObjects[0] = host;
            lastVisitedObjects[1] = group;
            windowHistory.add(() -> serverMenu(page, host, group));

            ItemStack block;
            ItemMeta blockMeta;
            ItemStack div = createItem("STAINED_GLASS_PANE", "BLACK_STAINED_GLASS_PANE", (short) 15);
            ItemMeta divMeta = div.getItemMeta();
            divMeta.setDisplayName(ChatColor.RESET.toString());
            div.setItemMeta(divMeta);

            int i = 0;
            int min = ((page - 1) * 36);
            int max = (min + 35);
            int count = (servers.size() == 0)?27:((servers.size() - min >= max)?36:servers.size() - min);
            int area = (count % 9 == 0) ? count : (int) (Math.floor(count / 9) + 1) * 9;

            Inventory inv = Bukkit.createInventory(null, 18 + area, (host == null)?((group == null)?plugin.api.getLang("SubServers", "Interface.Server-Menu.Title"):plugin.api.getLang("SubServers", "Interface.Group-SubServer.Title").replace("$str$", group)):plugin.api.getLang("SubServers", "Interface.Host-SubServer.Title").replace("$str$", hostname.get()));
            block = createItem("STAINED_GLASS_PANE", "GRAY_STAINED_GLASS_PANE", (short) 7);
            block.setItemMeta(divMeta);
            while (i < area) {
                inv.setItem(i, block);
                i++;
            }
            ItemStack adiv = block;
            i = (int) ((count < 9) ? Math.floor((9 - count) / 2) : 0);

            boolean even = (count & 1) == 0 && count < 9;
            NamedContainer<String, Short> external, online, temp, offline, disabled;

            for (Server server : servers) {
                if (servers.indexOf(server) >= min && servers.indexOf(server) <= max) {
                    if (even && (i == 4 || i == 13 || i == 22 || i == 31)) inv.setItem(i++, adiv);
                    external = (((i & 1) == 0) ? new NamedContainer<>("WHITE_STAINED_GLASS_PANE", (short) 0) : new NamedContainer<>("LIGHT_GRAY_STAINED_GLASS_PANE", (short) 8));
                    online = (((i & 1) == 0) ? new NamedContainer<>("LIME_STAINED_GLASS_PANE", (short) 5) : new NamedContainer<>("GREEN_STAINED_GLASS_PANE", (short) 13));
                    temp = (((i & 1) == 0) ? new NamedContainer<>("LIGHT_BLUE_STAINED_GLASS_PANE", (short) 3) : new NamedContainer<>("BLUE_STAINED_GLASS_PANE", (short) 11));
                    offline = (((i & 1) == 0) ? new NamedContainer<>("YELLOW_STAINED_GLASS_PANE", (short) 4) : new NamedContainer<>("ORANGE_STAINED_GLASS_PANE", (short) 1));
                    disabled = (((i & 1) == 0) ? new NamedContainer<>("MAGENTA_STAINED_GLASS_PANE", (short) 2) : new NamedContainer<>("RED_STAINED_GLASS_PANE", (short) 14));

                    if (!(server instanceof SubServer)) {
                        block = createItem("STAINED_GLASS_PANE", external.name(), external.get());
                        blockMeta = block.getItemMeta();
                        blockMeta.setDisplayName(ChatColor.WHITE + server.getDisplayName());
                        LinkedList<String> lore = new LinkedList<String>();
                        if (!server.getName().equals(server.getDisplayName()))
                            lore.add(ChatColor.GRAY + server.getName());
                        lore.add(plugin.api.getLang("SubServers", "Interface.Server-Menu.Server-External"));
                        lore.add(plugin.api.getLang("SubServers", "Interface.Server-Menu.Server-Player-Count").replace("$int$", new DecimalFormat("#,###").format(server.getPlayers().size())));
                        lore.add(plugin.api.getLang("SubServers", "Interface.Server-Menu.SubServer-Invalid"));
                        lore.add(ChatColor.WHITE + ((plugin.config.get().getSection("Settings").getBoolean("Show-Addresses", false))?server.getAddress().getAddress().getHostAddress()+':':"") + server.getAddress().getPort());
                        blockMeta.setLore(lore);
                    } else if (((SubServer) server).isRunning()) {
                        NamedContainer<String, Short> blockinfo = (((SubServer) server).getStopAction() == SubServer.StopAction.REMOVE_SERVER || ((SubServer) server).getStopAction() == SubServer.StopAction.DELETE_SERVER)?temp:online;
                        block = createItem("STAINED_GLASS_PANE", blockinfo.name(), blockinfo.get());
                        blockMeta = block.getItemMeta();
                        LinkedList<String> lore = new LinkedList<String>();
                        if (!server.getName().equals(server.getDisplayName()))
                            lore.add(ChatColor.GRAY + server.getName());
                        if (((SubServer) server).getStopAction() == SubServer.StopAction.REMOVE_SERVER || ((SubServer) server).getStopAction() == SubServer.StopAction.DELETE_SERVER) {
                            blockMeta.setDisplayName(ChatColor.AQUA + server.getDisplayName());
                            lore.add(plugin.api.getLang("SubServers", "Interface.Server-Menu.SubServer-Temporary"));
                        } else blockMeta.setDisplayName(ChatColor.GREEN + server.getDisplayName());
                        lore.add(plugin.api.getLang("SubServers", "Interface.Server-Menu.Server-Player-Count").replace("$int$", new DecimalFormat("#,###").format(server.getPlayers().size())));
                        lore.add(ChatColor.WHITE + ((plugin.config.get().getSection("Settings").getBoolean("Show-Addresses", false))?server.getAddress().getAddress().getHostAddress()+':':"") + server.getAddress().getPort());
                        blockMeta.setLore(lore);
                    } else if (((SubServer) server).isEnabled() && ((SubServer) server).getCurrentIncompatibilities().size() == 0) {
                        block = createItem("STAINED_GLASS_PANE", offline.name(), offline.get());
                        blockMeta = block.getItemMeta();
                        blockMeta.setDisplayName(ChatColor.YELLOW + server.getDisplayName());
                        LinkedList<String> lore = new LinkedList<String>();
                        if (!server.getName().equals(server.getDisplayName()))
                            lore.add(ChatColor.GRAY + server.getName());
                        lore.add(plugin.api.getLang("SubServers", "Interface.Server-Menu.SubServer-Offline"));
                        lore.add(ChatColor.WHITE + ((plugin.config.get().getSection("Settings").getBoolean("Show-Addresses", false))?server.getAddress().getAddress().getHostAddress()+':':"") + server.getAddress().getPort());
                        blockMeta.setLore(lore);
                    } else {
                        block = createItem("STAINED_GLASS_PANE", disabled.name(), disabled.get());
                        blockMeta = block.getItemMeta();
                        blockMeta.setDisplayName(ChatColor.RED + server.getDisplayName());
                        LinkedList<String> lore = new LinkedList<String>();
                        if (!server.getName().equals(server.getDisplayName()))
                            lore.add(ChatColor.GRAY + server.getName());
                        if (((SubServer) server).getCurrentIncompatibilities().size() != 0) {
                            String list = "";
                            for (String other : ((SubServer) server).getCurrentIncompatibilities()) {
                                if (list.length() != 0) list += ", ";
                                list += other;
                            }
                            lore.add(plugin.api.getLang("SubServers", "Interface.Server-Menu.SubServer-Incompatible").replace("$str$", list));
                        }
                        if (!((SubServer) server).isEnabled()) lore.add(plugin.api.getLang("SubServers", "Interface.Server-Menu.SubServer-Disabled"));
                        lore.add(ChatColor.WHITE + ((plugin.config.get().getSection("Settings").getBoolean("Show-Addresses", false))?server.getAddress().getAddress().getHostAddress()+':':"") + server.getAddress().getPort());
                        blockMeta.setLore(lore);
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

            if (servers.size() == 0) {
                block = createItem("STAINED_GLASS_PANE", "RED_STAINED_GLASS_PANE", (short) 14);
                blockMeta = block.getItemMeta();
                blockMeta.setDisplayName(plugin.api.getLang("SubServers", "Interface.Server-Menu.No-Servers"));
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
                block = createItem("STAINED_GLASS_PANE", "YELLOW_STAINED_GLASS_PANE", (short) 4);
                blockMeta = block.getItemMeta();
                blockMeta.setDisplayName(plugin.api.getLang("SubServers", "Interface.Generic.Back-Arrow"));
                block.setItemMeta(blockMeta);
                inv.setItem(i++, block);
                inv.setItem(i++, block);
            } else i += 2;
            i++;
            if (host == null || group == null || hasHistory()) {
                block = createItem("STAINED_GLASS_PANE", ((host == null && group == null)?"BLUE_STAINED_GLASS_PANE":"RED_STAINED_GLASS_PANE"), (short) ((host == null && group == null)?11:14));
                blockMeta = block.getItemMeta();
                blockMeta.setDisplayName((host == null && group == null)?plugin.api.getLang("SubServers", "Interface.Server-Menu.Host-Menu"):plugin.api.getLang("SubServers", "Interface.Generic.Back"));
                block.setItemMeta(blockMeta);
                inv.setItem(i++, block);
                inv.setItem(i++, block);
                inv.setItem(i++, block);
                i++;
            }
            if (servers.size() - 1 > max) {
                block = createItem("STAINED_GLASS_PANE", "YELLOW_STAINED_GLASS_PANE", (short) 4);
                blockMeta = block.getItemMeta();
                blockMeta.setDisplayName(plugin.api.getLang("SubServers", "Interface.Generic.Next-Arrow"));
                block.setItemMeta(blockMeta);
                inv.setItem(i++, block);
                inv.setItem(i, block);
            }

            Bukkit.getPlayer(player).openInventory(inv);
            open = true;
        };

        if (host != null && host.length() > 0) {
            plugin.api.getHost(host, object -> {
                if (object == null) {
                    if (hasHistory()) back();
                } else {
                    hostname.set(object.getDisplayName());
                    servercontainer.get().addAll(object.getSubServers().values());
                    renderer.run();
                }
            });
        } else if (group != null && group.length() > 0) {
            plugin.api.getGroup(group, servers -> {
                if (servers == null) {
                    if (hasHistory()) back();
                } else {
                    servercontainer.get().addAll(servers);
                    renderer.run();
                }
            });
        } else {
            plugin.api.getServers(servers -> {
                servercontainer.get().addAll(servers.values());
                renderer.run();
            });
        }
    }

    public void subserverAdmin(final String name) {
        setDownloading(ChatColor.stripColor(plugin.api.getLang("SubServers", "Interface.SubServer-Admin.Title").replace("$str$", name)));
        plugin.api.getSubServer(name, subserver -> {
            windowHistory.add(() -> subserverAdmin(name));
            if (subserver == null) {
                if (hasHistory()) back();
            } else subserver.getHost(host -> {
                if (host == null) {
                    if (hasHistory()) back();
                } else {
                    setDownloading(null);
                    lastVisitedObjects[0] = name;
                    ItemStack block;
                    ItemMeta blockMeta;
                    ItemStack div = createItem("STAINED_GLASS_PANE", "BLACK_STAINED_GLASS_PANE", (short) 15);
                    ItemMeta divMeta = div.getItemMeta();
                    divMeta.setDisplayName(ChatColor.RESET.toString());
                    div.setItemMeta(divMeta);

                    Inventory inv = Bukkit.createInventory(null, 36, plugin.api.getLang("SubServers", "Interface.SubServer-Admin.Title").replace("$str$", subserver.getDisplayName()));

                    int i = 0;
                    while (i < inv.getSize()) {
                        inv.setItem(i, div);
                        i++;
                    }
                    i = 0;

                    if (subserver.isRunning()) {
                        if (!(Bukkit.getPlayer(player).hasPermission("subservers.subserver.terminate.*") || Bukkit.getPlayer(player).hasPermission("subservers.subserver.terminate." + name.toLowerCase()))) {
                            block = createItem("STAINED_GLASS_PANE", "GRAY_STAINED_GLASS_PANE", (short) 7);
                            blockMeta = block.getItemMeta();
                            blockMeta.setDisplayName(ChatColor.GRAY+ChatColor.stripColor(plugin.api.getLang("SubServers", "Interface.SubServer-Admin.Terminate")));
                            blockMeta.setLore(Arrays.asList(plugin.api.getLang("SubServers", "Interface.Generic.Invalid-Permission").replace("$str$", "subservers.subserver.terminate." + name.toLowerCase())));
                        } else {
                            block = createItem("STAINED_GLASS_PANE", "RED_STAINED_GLASS_PANE", (short) 14);
                            blockMeta = block.getItemMeta();
                            blockMeta.setDisplayName(plugin.api.getLang("SubServers", "Interface.SubServer-Admin.Terminate"));
                        }

                        block.setItemMeta(blockMeta);
                        inv.setItem(1, block);
                        inv.setItem(10, block);

                        if (!(Bukkit.getPlayer(player).hasPermission("subservers.subserver.stop.*") || Bukkit.getPlayer(player).hasPermission("subservers.subserver.stop." + name.toLowerCase()))) {
                            block = createItem("STAINED_GLASS_PANE", "GRAY_STAINED_GLASS_PANE", (short) 7);
                            blockMeta = block.getItemMeta();
                            blockMeta.setDisplayName(ChatColor.GRAY+ChatColor.stripColor(plugin.api.getLang("SubServers", "Interface.SubServer-Admin.Stop")));
                            blockMeta.setLore(Arrays.asList(plugin.api.getLang("SubServers", "Interface.Generic.Invalid-Permission").replace("$str$", "subservers.subserver.stop." + name.toLowerCase())));
                        } else {
                            block = createItem("STAINED_GLASS_PANE", "MAGENTA_STAINED_GLASS_PANE", (short) 2);
                            blockMeta = block.getItemMeta();
                            blockMeta.setDisplayName(plugin.api.getLang("SubServers", "Interface.SubServer-Admin.Stop"));
                        }
                        block.setItemMeta(blockMeta);
                        inv.setItem(2, block);
                        inv.setItem(3, block);
                        inv.setItem(11, block);
                        inv.setItem(12, block);

                        if (!(Bukkit.getPlayer(player).hasPermission("subservers.subserver.command.*") || Bukkit.getPlayer(player).hasPermission("subservers.subserver.command." + name.toLowerCase()))) {
                            block = createItem("STAINED_GLASS_PANE", "GRAY_STAINED_GLASS_PANE", (short) 7);
                            blockMeta = block.getItemMeta();
                            blockMeta.setDisplayName(ChatColor.GRAY+ChatColor.stripColor(plugin.api.getLang("SubServers", "Interface.SubServer-Admin.Command")));
                            blockMeta.setLore(Arrays.asList(plugin.api.getLang("SubServers", "Interface.Generic.Invalid-Permission").replace("$str$", "subservers.subserver.command." + name.toLowerCase())));
                        } else {
                            block = createItem("STAINED_GLASS_PANE", "YELLOW_STAINED_GLASS_PANE", (short) 4);
                            blockMeta = block.getItemMeta();
                            blockMeta.setDisplayName(plugin.api.getLang("SubServers", "Interface.SubServer-Admin.Command"));
                        }
                        block.setItemMeta(blockMeta);
                        inv.setItem(5, block);
                        inv.setItem(6, block);
                        inv.setItem(7, block);
                        inv.setItem(14, block);
                        inv.setItem(15, block);
                        inv.setItem(16, block);
                    } else {
                        if (!(Bukkit.getPlayer(player).hasPermission("subservers.subserver.start.*") || Bukkit.getPlayer(player).hasPermission("subservers.subserver.start." + name.toLowerCase()))) {
                            block = createItem("STAINED_GLASS_PANE", "GRAY_STAINED_GLASS_PANE", (short) 7);
                            blockMeta = block.getItemMeta();
                            blockMeta.setDisplayName(ChatColor.GRAY+ChatColor.stripColor(plugin.api.getLang("SubServers", "Interface.SubServer-Admin.Start")));
                            blockMeta.setLore(Arrays.asList(plugin.api.getLang("SubServers", "Interface.Generic.Invalid-Permission").replace("$str$", "subservers.subserver.start." + name.toLowerCase())));
                        } else if (!host.isAvailable() || !host.isEnabled() || !subserver.isEnabled() || subserver.getCurrentIncompatibilities().size() != 0) {
                            block = createItem("STAINED_GLASS_PANE", "GRAY_STAINED_GLASS_PANE", (short) 7);
                            blockMeta = block.getItemMeta();
                            blockMeta.setDisplayName(ChatColor.GRAY+ChatColor.stripColor(plugin.api.getLang("SubServers", "Interface.SubServer-Admin.Start")));
                        } else {
                            block = createItem("STAINED_GLASS_PANE", "LIME_STAINED_GLASS_PANE", (short) 5);
                            blockMeta = block.getItemMeta();
                            blockMeta.setDisplayName(plugin.api.getLang("SubServers", "Interface.SubServer-Admin.Start"));
                        }
                        block.setItemMeta(blockMeta);
                        inv.setItem(3, block);
                        inv.setItem(4, block);
                        inv.setItem(5, block);
                        inv.setItem(12, block);
                        inv.setItem(13, block);
                        inv.setItem(14, block);
                    }

                    if (!host.isAvailable() || !host.isEnabled() || !subserver.isEnabled() || subserverPlugins.size() <= 0) {
                        block = div;
                    } else {
                        block = createItem("STAINED_GLASS_PANE", "BLUE_STAINED_GLASS_PANE", (short) 11);
                        blockMeta = block.getItemMeta();
                        blockMeta.setDisplayName(plugin.api.getLang("SubServers", "Interface.SubServer-Admin.Plugins"));
                        block.setItemMeta(blockMeta);
                    }
                    inv.setItem(27, block);
                    inv.setItem(28, block);

                    if (subserver.isRunning()) {
                        block = (subserver.getStopAction() == SubServer.StopAction.REMOVE_SERVER || subserver.getStopAction() == SubServer.StopAction.DELETE_SERVER)?createItem("STAINED_GLASS_PANE", "BLUE_STAINED_GLASS_PANE", (short) 11):createItem("STAINED_GLASS_PANE", "LIME_STAINED_GLASS_PANE", (short) 5);
                        blockMeta = block.getItemMeta();
                        LinkedList<String> lore = new LinkedList<String>();
                        if (!subserver.getName().equals(subserver.getDisplayName()))
                            lore.add(ChatColor.GRAY + subserver.getName());
                        if (subserver.getStopAction() != SubServer.StopAction.REMOVE_SERVER || subserver.getStopAction() != SubServer.StopAction.DELETE_SERVER) {
                            blockMeta.setDisplayName(ChatColor.AQUA + subserver.getDisplayName());
                            lore.add(plugin.api.getLang("SubServers", "Interface.Server-Menu.SubServer-Temporary"));
                        } else blockMeta.setDisplayName(ChatColor.GREEN + subserver.getDisplayName());
                        lore.add(plugin.api.getLang("SubServers", "Interface.Server-Menu.Server-Player-Count").replace("$int$", new DecimalFormat("#,###").format(subserver.getPlayers().size())));
                        lore.add(ChatColor.WHITE + ((plugin.config.get().getSection("Settings").getBoolean("Show-Addresses", false))?subserver.getAddress().getAddress().getHostAddress()+':':"") + subserver.getAddress().getPort());
                        blockMeta.setLore(lore);
                    } else if (subserver.isEnabled() && subserver.getCurrentIncompatibilities().size() == 0) {
                        block = createItem("STAINED_GLASS_PANE", "YELLOW_STAINED_GLASS_PANE", (short) 4);
                        blockMeta = block.getItemMeta();
                        blockMeta.setDisplayName(ChatColor.YELLOW + subserver.getDisplayName());
                        LinkedList<String> lore = new LinkedList<String>();
                        if (!subserver.getName().equals(subserver.getDisplayName()))
                            lore.add(ChatColor.GRAY + subserver.getName());
                        lore.add(plugin.api.getLang("SubServers", "Interface.Server-Menu.SubServer-Offline"));
                        lore.add(ChatColor.WHITE + ((plugin.config.get().getSection("Settings").getBoolean("Show-Addresses", false))?subserver.getAddress().getAddress().getHostAddress()+':':"") + subserver.getAddress().getPort());
                        blockMeta.setLore(lore);
                    } else {
                        block = createItem("STAINED_GLASS_PANE", "RED_STAINED_GLASS_PANE", (short) 14);
                        blockMeta = block.getItemMeta();
                        blockMeta.setDisplayName(ChatColor.RED + subserver.getDisplayName());
                        LinkedList<String> lore = new LinkedList<String>();
                        if (!subserver.getName().equals(subserver.getDisplayName()))
                            lore.add(ChatColor.GRAY + subserver.getName());
                        if (subserver.getCurrentIncompatibilities().size() != 0) {
                            String list = "";
                            for (String other : subserver.getCurrentIncompatibilities()) {
                                if (list.length() != 0) list += ", ";
                                list += other;
                            }
                            lore.add(plugin.api.getLang("SubServers", "Interface.Server-Menu.SubServer-Incompatible").replace("$str$", list));
                        }
                        if (!subserver.isEnabled()) lore.add(plugin.api.getLang("SubServers", "Interface.Server-Menu.SubServer-Disabled"));
                        lore.add(ChatColor.WHITE + ((plugin.config.get().getSection("Settings").getBoolean("Show-Addresses", false))?subserver.getAddress().getAddress().getHostAddress()+':':"") + subserver.getAddress().getPort());
                        blockMeta.setLore(lore);
                    }
                    block.setItemMeta(blockMeta);
                    inv.setItem(30, block);
                    inv.setItem(31, block);
                    inv.setItem(32, block);

                    if (hasHistory()) {
                        block = createItem("STAINED_GLASS_PANE", "RED_STAINED_GLASS_PANE", (short) 14);
                        blockMeta = block.getItemMeta();
                        blockMeta.setDisplayName(plugin.api.getLang("SubServers", "Interface.Generic.Back"));
                        block.setItemMeta(blockMeta);
                        inv.setItem(34, block);
                        inv.setItem(35, block);
                    }

                    Bukkit.getPlayer(player).openInventory(inv);
                    open = true;
                }
            });
        });

    }

    public void subserverPlugin(final int page, final String name) {
        setDownloading(ChatColor.stripColor(plugin.api.getLang("SubServers", "Interface.SubServer-Plugin.Title").replace("$str$", name)));
        plugin.api.getSubServer(name, subserver -> {
            windowHistory.add(() -> subserverPlugin(page, name));
            if (subserver == null) {
                if (hasHistory()) back();
            } else {
                setDownloading(null);
                lastVisitedObjects[0] = name;
                lastPage = page;
                List<String> renderers = new LinkedList<String>();
                for (String renderer : renderers) {
                    if (subserverPlugins.get(renderer).isEnabled(subserver)) renderers.add(renderer);
                }
                Collections.sort(renderers);

                ItemStack block;
                ItemMeta blockMeta;
                ItemStack div = createItem("STAINED_GLASS_PANE", "BLACK_STAINED_GLASS_PANE", (short) 15);
                ItemMeta divMeta = div.getItemMeta();
                divMeta.setDisplayName(ChatColor.RESET.toString());
                div.setItemMeta(divMeta);

                int i = 0;
                int min = ((page - 1) * 36);
                int max = (min + 35);
                int count = (renderers.size() == 0)?27:((renderers.size() - min >= max)?36:renderers.size() - min);
                int area = (count % 9 == 0) ? count : (int) (Math.floor(count / 9) + 1) * 9;

                Inventory inv = Bukkit.createInventory(null, 18 + area, plugin.api.getLang("SubServers", "Interface.SubServer-Plugin.Title").replace("$str$", subserver.getDisplayName()));
                block = createItem("STAINED_GLASS_PANE", "GRAY_STAINED_GLASS_PANE", (short) 7);
                block.setItemMeta(divMeta);
                while (i < area) {
                    inv.setItem(i, block);
                    i++;
                }
                ItemStack adiv = block;
                i = (int) ((count < 9) ? Math.floor((9 - count) / 2) : 0);

                boolean even = (count & 1) == 0 && count < 9;

                for (String renderer : renderers) {
                    if (renderers.indexOf(renderer) >= min && renderers.indexOf(renderer) <= max) {
                        if (even && (i == 4 || i == 13 || i == 22 || i == 31)) inv.setItem(i++, adiv);

                        inv.setItem(i, subserverPlugins.get(renderer).getIcon());

                        count--;
                        if (count < 9 && (i == 8 || i == 17 || i == 26)) {
                            i += (int) Math.floor((9 - count) / 2) + 1;
                            even = (count & 1) == 0;
                        } else {
                            i++;
                        }
                    }
                }

                if (renderers.size() == 0) {
                    block = createItem("STAINED_GLASS_PANE", "RED_STAINED_GLASS_PANE", (short) 14);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(plugin.api.getLang("SubServers", "Interface.SubServer-Plugin.No-Plugins"));
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
                    block = createItem("STAINED_GLASS_PANE", "YELLOW_STAINED_GLASS_PANE", (short) 4);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(plugin.api.getLang("SubServers", "Interface.Generic.Back-Arrow"));
                    block.setItemMeta(blockMeta);
                    inv.setItem(i++, block);
                    inv.setItem(i++, block);
                } else i += 2;
                i++;
                if (hasHistory()) {
                    block = createItem("STAINED_GLASS_PANE", "RED_STAINED_GLASS_PANE", (short) 14);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(plugin.api.getLang("SubServers", "Interface.Generic.Back"));
                    block.setItemMeta(blockMeta);
                    inv.setItem(i++, block);
                    inv.setItem(i++, block);
                    inv.setItem(i++, block);
                    i++;
                }
                if (renderers.size() - 1 > max) {
                    block = createItem("STAINED_GLASS_PANE", "YELLOW_STAINED_GLASS_PANE", (short) 4);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(plugin.api.getLang("SubServers", "Interface.Generic.Next-Arrow"));
                    block.setItemMeta(blockMeta);
                    inv.setItem(i++, block);
                    inv.setItem(i, block);
                }

                Bukkit.getPlayer(player).openInventory(inv);
                open = true;
            }
        });
    }
}
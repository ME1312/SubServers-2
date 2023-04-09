package net.ME1312.SubServers.Client.Bukkit.Graphic;

import net.ME1312.Galaxi.Library.Container.Container;
import net.ME1312.Galaxi.Library.Container.Value;
import net.ME1312.SubServers.Client.Bukkit.Library.Compatibility.AgnosticScheduler;
import net.ME1312.SubServers.Client.Bukkit.SubPlugin;
import net.ME1312.SubServers.Client.Common.Network.API.Host;
import net.ME1312.SubServers.Client.Common.Network.API.Server;
import net.ME1312.SubServers.Client.Common.Network.API.SubCreator;
import net.ME1312.SubServers.Client.Common.Network.API.SubServer;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.DecimalFormat;
import java.util.*;
import java.util.function.BiConsumer;

import static net.ME1312.SubServers.Client.Bukkit.Library.ObjectPermission.permits;

/**
 * Default GUI Renderer Class
 */
public class DefaultUIRenderer extends UIRenderer {
    private static final int MAX_VISITED_OBJECTS = 2;
    private final List<Runnable> windowHistory = new LinkedList<Runnable>();
    Object[] lastVisitedObjects = new Object[MAX_VISITED_OBJECTS];
    int lastPage = 1;
    Runnable lastMenu = null;
    boolean open = false;

    DefaultUIRenderer(SubPlugin plugin, Player player) {
        super(plugin, player);
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
/*
    private static final String[] STAINED_GLASS_INDEX = new String[] {
            "WHITE_STAINED_GLASS_PANE",
            "ORANGE_STAINED_GLASS_PANE",
            "MAGENTA_STAINED_GLASS_PANE",
            "LIGHT_BLUE_STAINED_GLASS_PANE",
            "YELLOW_STAINED_GLASS_PANE",
            "LIME_STAINED_GLASS_PANE",
            "PINK_STAINED_GLASS_PANE",
            "GRAY_STAINED_GLASS_PANE",
            "LIGHT_GRAY_STAINED_GLASS_PANE",
            "CYAN_STAINED_GLASS_PANE",
            "PURPLE_STAINED_GLASS_PANE",
            "BLUE_STAINED_GLASS_PANE",
            "BROWN_STAINED_GLASS_PANE",
            "GREEN_STAINED_GLASS_PANE",
            "RED_STAINED_GLASS_PANE",
            "BLACK_STAINED_GLASS_PANE"
    }; */
    private ItemStack color(int color) {
        try {
//          if (plugin.api.getGameVersion().compareTo(new Version("1.13")) < 0) {
                return new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) color);
//          } else {
//              return new ItemStack((Material) Material.class.getMethod("getMaterial", String.class, boolean.class).invoke(null, STAINED_GLASS_INDEX[color], false), 1);
//          }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void hostMenu(final int page) {
        setDownloading(ChatColor.stripColor(plugin.api.getLang("SubServers", "Interface.Host-Menu.Title")));
        plugin.api.getHosts(hosts -> plugin.api.getGroups(groups -> AgnosticScheduler.following(player).runs(plugin, c -> {
            setDownloading(null);
            lastVisitedObjects[0] = null;
            lastPage = page;
            lastMenu = () -> hostMenu(1);
            windowHistory.add(() -> hostMenu(page));
            List<Host> index = new LinkedList<Host>();
            index.addAll(hosts.values());

            ItemStack block;
            ItemMeta blockMeta;
            ItemStack div = color(15);
            ItemMeta divMeta = div.getItemMeta();
            divMeta.setDisplayName(ChatColor.RESET.toString());
            div.setItemMeta(divMeta);

            int i = 0;
            int min = ((page - 1) * 36);
            int max = (min + 35);
            int count = (index.size() == 0)?27:((index.size() - min >= max)?36:index.size() - min);
            int area = (count % 9 == 0) ? count : ((count / 9) + 1) * 9;

            Inventory inv = Bukkit.createInventory(null, 18 + area, plugin.api.getLang("SubServers", "Interface.Host-Menu.Title"));
            block = color(7);
            block.setItemMeta(divMeta);
            while (i < area) {
                inv.setItem(i, block);
                i++;
            }
            ItemStack adiv = block;
            i = ((count < 9) ? ((9 - count) / 2) : 0);

            boolean even = (count & 1) == 0 && count < 9;
            int enabled, disabled;

            for (Host host : index) {
                if (index.indexOf(host) >= min && index.indexOf(host) <= max) {
                    if (even && (i == 4 || i == 13 || i == 22 || i == 31)) inv.setItem(i++, adiv);
                    enabled = (((i & 1) == 0)? 3 : 11);
                    disabled = (((i & 1) == 0)? 2 : 14);

                    if (host.isAvailable() && host.isEnabled()) {
                        block = color(enabled);
                        blockMeta = block.getItemMeta();
                        blockMeta.setDisplayName(ChatColor.AQUA + host.getDisplayName());
                        LinkedList<String> lore = new LinkedList<String>();
                        if (!host.getName().equals(host.getDisplayName()))
                            lore.add(ChatColor.GRAY + host.getName());
                        lore.add(plugin.api.getLang("SubServers", "Interface.Host-Menu.Host-Server-Count").replace("$int$", new DecimalFormat("#,###").format(host.getSubServers().keySet().size())));
                        if (plugin.config.get().getMap("Settings").getBoolean("Show-Addresses", false)) lore.add(ChatColor.WHITE + host.getAddress().getHostAddress());
                        blockMeta.setLore(lore);
                    } else {
                        block = color(disabled);
                        blockMeta = block.getItemMeta();
                        blockMeta.setDisplayName(ChatColor.RED + host.getDisplayName());
                        LinkedList<String> lore = new LinkedList<String>();
                        if (!host.getName().equals(host.getDisplayName()))
                            lore.add(ChatColor.GRAY + host.getName());
                        if (!host.isAvailable()) lore.add(plugin.api.getLang("SubServers", "Interface.Host-Menu.Host-Unavailable"));
                        else lore.add(plugin.api.getLang("SubServers", "Interface.Host-Menu.Host-Disabled"));
                        if (plugin.config.get().getMap("Settings").getBoolean("Show-Addresses", false)) lore.add(ChatColor.WHITE + host.getAddress().getHostAddress());
                        blockMeta.setLore(lore);
                    }
                    block.setItemMeta(blockMeta);
                    inv.setItem(i, block);

                    count--;
                    if (count < 9 && (i == 8 || i == 17 || i == 26)) {
                        i += ((9 - count) / 2) + 1;
                        even = (count & 1) == 0;
                    } else {
                        i++;
                    }
                }
            }

            if (index.size() == 0) {
                block = color(14);
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
                block = color(4);
                blockMeta = block.getItemMeta();
                blockMeta.setDisplayName(plugin.api.getLang("SubServers", "Interface.Generic.Back-Arrow"));
                block.setItemMeta(blockMeta);
                inv.setItem(i++, block);
                inv.setItem(i++, block);
            } else i += 2;
            i++;
            if (groups.keySet().size() <= 0) {
                block = color(5);
                blockMeta = block.getItemMeta();
                blockMeta.setDisplayName(plugin.api.getLang("SubServers", "Interface.Host-Menu.Server-Menu"));
            } else {
                block = color(1);
                blockMeta = block.getItemMeta();
                blockMeta.setDisplayName(plugin.api.getLang("SubServers", "Interface.Host-Menu.Group-Menu"));
            }
            block.setItemMeta(blockMeta);
            inv.setItem(i++, block);
            inv.setItem(i++, block);
            inv.setItem(i++, block);
            i++;
            if (index.size() - 1 > max) {
                block = color(4);
                blockMeta = block.getItemMeta();
                blockMeta.setDisplayName(plugin.api.getLang("SubServers", "Interface.Generic.Next-Arrow"));
                block.setItemMeta(blockMeta);
                inv.setItem(i++, block);
                inv.setItem(i, block);
            }

            player.openInventory(inv);
            open = true;
        })));
    }

    public void hostAdmin(final String name) {
        setDownloading(ChatColor.stripColor(plugin.api.getLang("SubServers", "Interface.Host-Admin.Title").replace("$str$", name)));
        plugin.api.getHost(name, host -> AgnosticScheduler.following(player).runs(plugin, c -> {
            windowHistory.add(() -> hostAdmin(name));
            if (host == null) {
                if (hasHistory()) back();
            } else {
                setDownloading(null);
                lastVisitedObjects[0] = host;

                ItemStack block;
                ItemMeta blockMeta;
                ItemStack div = color(15);
                ItemMeta divMeta = div.getItemMeta();
                divMeta.setDisplayName(ChatColor.RESET.toString());
                div.setItemMeta(divMeta);

                Inventory inv = Bukkit.createInventory(null, 36, plugin.api.getLang("SubServers", "Interface.Host-Admin.Title").replace("$str$", host.getDisplayName()));

                int i = 0;
                while (i < inv.getSize()) {
                    inv.setItem(i, div);
                    i++;
                }

                Player player = this.player;
                if (!permits(name, player, "subservers.host.%.*", "subservers.host.%.create")) {
                    block = color(7);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(ChatColor.GRAY+ChatColor.stripColor(plugin.api.getLang("SubServers", "Interface.Host-Admin.Creator")));
                    blockMeta.setLore(Arrays.asList(plugin.api.getLang("SubServers", "Interface.Generic.Invalid-Permission").replace("$str$", "subservers.host." + name.toLowerCase() + ".create")));
                } else if (!host.isAvailable() || !host.isEnabled()) {
                    block = color(7);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(ChatColor.GRAY+ChatColor.stripColor(plugin.api.getLang("SubServers", "Interface.Host-Admin.Creator")));
                } else {
                    block = color(4);
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

                block = color(3);
                blockMeta = block.getItemMeta();
                blockMeta.setDisplayName(plugin.api.getLang("SubServers", "Interface.Host-Admin.SubServers"));
                block.setItemMeta(blockMeta);
                inv.setItem(5, block);
                inv.setItem(6, block);
                inv.setItem(7, block);
                inv.setItem(14, block);
                inv.setItem(15, block);
                inv.setItem(16, block);

                if (hostPlugins.size() <= 0) {
                    block = div;
                } else {
                    block = color(11);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(plugin.api.getLang("SubServers", "Interface.Host-Admin.Plugins"));
                    block.setItemMeta(blockMeta);
                }
                inv.setItem(27, block);
                inv.setItem(28, block);

                if (host.isAvailable() && host.isEnabled()) {
                    block = color(11);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(ChatColor.AQUA + host.getDisplayName());
                    LinkedList<String> lore = new LinkedList<String>();
                    if (!host.getName().equals(host.getDisplayName()))
                        lore.add(ChatColor.GRAY + host.getName());
                    lore.add(plugin.api.getLang("SubServers", "Interface.Host-Menu.Host-Server-Count").replace("$int$", new DecimalFormat("#,###").format(host.getSubServers().keySet().size())));
                    if (plugin.config.get().getMap("Settings").getBoolean("Show-Addresses", false)) lore.add(ChatColor.WHITE + host.getAddress().getHostAddress());
                    blockMeta.setLore(lore);
                } else {
                    block = color(14);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(ChatColor.RED + host.getDisplayName());
                    LinkedList<String> lore = new LinkedList<String>();
                    if (!host.getName().equals(host.getDisplayName()))
                        lore.add(ChatColor.GRAY + host.getName());
                    if (!host.isAvailable()) lore.add(plugin.api.getLang("SubServers", "Interface.Host-Menu.Host-Unavailable"));
                    else lore.add(plugin.api.getLang("SubServers", "Interface.Host-Menu.Host-Disabled"));
                    if (plugin.config.get().getMap("Settings").getBoolean("Show-Addresses", false)) lore.add(ChatColor.WHITE + host.getAddress().getHostAddress());
                    blockMeta.setLore(lore);
                }
                block.setItemMeta(blockMeta);
                inv.setItem(30, block);
                inv.setItem(31, block);
                inv.setItem(32, block);


                if (hasHistory()) {
                    block = color(14);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(plugin.api.getLang("SubServers", "Interface.Generic.Back"));
                    block.setItemMeta(blockMeta);
                    inv.setItem(34, block);
                    inv.setItem(35, block);
                }

                player.openInventory(inv);
                open = true;
            }
        }));
    }

    public void hostCreator(final CreatorOptions options) {
        setDownloading(ChatColor.stripColor(plugin.api.getLang("SubServers", "Interface.Host-Creator.Title").replace("$str$", options.getHost())));
        if (!options.init()) windowHistory.add(() -> hostCreator(options));
        lastVisitedObjects[0] = options;

        plugin.api.getHost(options.getHost(), host -> AgnosticScheduler.following(player).runs(plugin, c -> {
            if (host == null || !host.isAvailable() || !host.isEnabled()) {
                lastVisitedObjects[0] = null;
                if (hasHistory()) back();
            } else {
                setDownloading(null);
                ItemStack block;
                ItemMeta blockMeta;
                ItemStack div = color(15);
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
                    block = color(14);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(ChatColor.RED + plugin.api.getLang("SubServers", "Interface.Host-Creator.Edit-Name"));
                } else {
                    block = color(5);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(ChatColor.GREEN + plugin.api.getLang("SubServers", "Interface.Host-Creator.Edit-Name"));
                    blockMeta.setLore(Arrays.asList(ChatColor.GRAY + options.getName()));
                }
                block.setItemMeta(blockMeta);
                inv.setItem(10, block);
                inv.setItem(11, block);
                inv.setItem(12, block);

                block = color(5);
                blockMeta = block.getItemMeta();
                blockMeta.setDisplayName(ChatColor.GREEN + plugin.api.getLang("SubServers", "Interface.Host-Creator.Edit-Port"));
                blockMeta.setLore(Arrays.asList(ChatColor.GRAY.toString() + ((options.getPort() == null)?"Auto Select":options.getPort())));
                block.setItemMeta(blockMeta);
                inv.setItem(14, block);
                inv.setItem(15, block);
                inv.setItem(16, block);

                if (options.getTemplate() == null) {
                    block = color(14);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(ChatColor.RED + plugin.api.getLang("SubServers", "Interface.Host-Creator.Edit-Template"));
                } else {
                    block = color(5);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(ChatColor.GREEN + plugin.api.getLang("SubServers", "Interface.Host-Creator.Edit-Template"));
                    blockMeta.setLore(Arrays.asList(ChatColor.GRAY + options.getTemplate()));
                }
                block.setItemMeta(blockMeta);
                inv.setItem(28, block);
                inv.setItem(29, block);
                inv.setItem(30, block);

                if (options.getVersion() == null && (options.getTemplate() == null || host.getCreator().getTemplate(options.getTemplate()).requiresVersion())) {
                    block = color(14);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(ChatColor.RED + plugin.api.getLang("SubServers", "Interface.Host-Creator.Edit-Version"));
                } else {
                    block = color(5);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(ChatColor.GREEN + plugin.api.getLang("SubServers", "Interface.Host-Creator.Edit-Version"));
                    blockMeta.setLore(Arrays.asList(ChatColor.GRAY + ((options.getVersion() == null)?"Unspecified":"Minecraft "+options.getVersion().toString())));
                }
                block.setItemMeta(blockMeta);
                inv.setItem(32, block);
                inv.setItem(33, block);
                inv.setItem(34, block);

                if (!options.hasHistory()) {
                    block = color(7);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(ChatColor.GRAY + ChatColor.stripColor(plugin.api.getLang("SubServers", "Interface.Generic.Undo")));
                    block.setItemMeta(blockMeta);
                } else {
                    block = color(1);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(plugin.api.getLang("SubServers", "Interface.Generic.Undo"));
                    block.setItemMeta(blockMeta);
                }
                block.setItemMeta(blockMeta);
                inv.setItem(45, block);
                inv.setItem(46, block);

                if (options.getName() == null || options.getTemplate() == null || (options.getVersion() == null && host.getCreator().getTemplate(options.getTemplate()).requiresVersion())) {
                    block = color(7);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(ChatColor.GRAY + ChatColor.stripColor(plugin.api.getLang("SubServers", "Interface.Host-Creator.Submit")));
                    blockMeta.setLore(Arrays.asList(plugin.api.getLang("SubServers", "Interface.Host-Creator.Form-Incomplete")));
                    block.setItemMeta(blockMeta);
                } else {
                    block = color(4);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(plugin.api.getLang("SubServers", "Interface.Host-Creator.Submit"));
                    block.setItemMeta(blockMeta);
                }
                block.setItemMeta(blockMeta);
                inv.setItem(48, block);
                inv.setItem(49, block);
                inv.setItem(50, block);

                if (hasHistory()) {
                    block = color(14);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(plugin.api.getLang("SubServers", "Interface.Generic.Back"));
                    block.setItemMeta(blockMeta);
                    inv.setItem(52, block);
                    inv.setItem(53, block);
                }

                player.openInventory(inv);
                open = true;
            }
        }));
    }

    public void hostCreatorTemplates(final int page, final CreatorOptions options) {
        setDownloading(ChatColor.stripColor(plugin.api.getLang("SubServers", "Interface.Host-Creator.Edit-Template.Title").replace("$str$", options.getHost())));
        options.init();
        lastVisitedObjects[0] = options;
        plugin.api.getHost(options.getHost(), host -> AgnosticScheduler.following(player).runs(plugin, c -> {
            if (host == null || !host.isAvailable() || !host.isEnabled()) {
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
                ItemStack div = color(15);
                ItemMeta divMeta = div.getItemMeta();
                divMeta.setDisplayName(ChatColor.RESET.toString());
                div.setItemMeta(divMeta);

                int i = 0;
                int min = ((page - 1) * 36);
                int max = (min + 35);
                int count = (index.size() == 0)?27:((index.size() - min >= max)?36:index.size() - min);
                int area = (count % 9 == 0)?count: ((count / 9) + 1) * 9;

                Inventory inv = Bukkit.createInventory(null, 18 + area, plugin.api.getLang("SubServers", "Interface.Host-Creator.Edit-Template.Title").replace("$str$", host.getDisplayName()));
                block = color(7);
                block.setItemMeta(divMeta);
                while (i < area) {
                    inv.setItem(i, block);
                    i++;
                }
                ItemStack adiv = block;
                i = ((count < 9) ? ((9 - count) / 2) : 0);

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
                            i += ((9 - count) / 2) + 1;
                            even = (count & 1) == 0;
                        } else {
                            i++;
                        }
                    }
                }

                if (index.size() == 0) {
                    block = color(14);
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
                    block = color(4);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(plugin.api.getLang("SubServers", "Interface.Generic.Back-Arrow"));
                    block.setItemMeta(blockMeta);
                    inv.setItem(i++, block);
                    inv.setItem(i++, block);
                } else i += 2;
                i++;
                block = color(14);
                blockMeta = block.getItemMeta();
                blockMeta.setDisplayName(plugin.api.getLang("SubServers", "Interface.Generic.Back"));
                block.setItemMeta(blockMeta);
                inv.setItem(i++, block);
                inv.setItem(i++, block);
                inv.setItem(i++, block);
                i++;
                if (index.size() - 1 > max) {
                    block = color(4);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(plugin.api.getLang("SubServers", "Interface.Generic.Next-Arrow"));
                    block.setItemMeta(blockMeta);
                    inv.setItem(i++, block);
                    inv.setItem(i, block);
                }

                player.openInventory(inv);
                open = true;
            }
        }));
    }

    public void hostPlugin(final int page, final String name) {
        setDownloading(ChatColor.stripColor(plugin.api.getLang("SubServers", "Interface.Host-Plugin.Title").replace("$str$", name)));
        plugin.api.getHost(name, host -> AgnosticScheduler.following(player).runs(plugin, c -> {
            windowHistory.add(() -> hostPlugin(page, name));
            if (host == null) {
                if (hasHistory()) back();
            } else {
                setDownloading(null);
                lastVisitedObjects[0] = host;
                lastPage = page;
                List<String> renderers = new LinkedList<String>();
                for (String renderer : hostPlugins.keySet()) {
                    if (hostPlugins.get(renderer).isEnabled(host)) renderers.add(renderer);
                }
                Collections.sort(renderers);

                ItemStack block;
                ItemMeta blockMeta;
                ItemStack div = color(15);
                ItemMeta divMeta = div.getItemMeta();
                divMeta.setDisplayName(ChatColor.RESET.toString());
                div.setItemMeta(divMeta);

                int i = 0;
                int min = ((page - 1) * 36);
                int max = (min + 35);
                int count = (renderers.size() == 0)?27:((renderers.size() - min >= max)?36:renderers.size() - min);
                int area = (count % 9 == 0) ? count : ((count / 9) + 1) * 9;

                Inventory inv = Bukkit.createInventory(null, 18 + area, plugin.api.getLang("SubServers", "Interface.Host-Plugin.Title").replace("$str$", host.getDisplayName()));
                block = color(7);
                block.setItemMeta(divMeta);
                while (i < area) {
                    inv.setItem(i, block);
                    i++;
                }
                ItemStack adiv = block;
                i = ((count < 9) ? ((9 - count) / 2) : 0);

                boolean even = (count & 1) == 0 && count < 9;

                for (String renderer : renderers) {
                    if (renderers.indexOf(renderer) >= min && renderers.indexOf(renderer) <= max) {
                        if (even && (i == 4 || i == 13 || i == 22 || i == 31)) inv.setItem(i++, adiv);

                        inv.setItem(i, hostPlugins.get(renderer).getIcon());

                        count--;
                        if (count < 9 && (i == 8 || i == 17 || i == 26)) {
                            i += ((9 - count) / 2) + 1;
                            even = (count & 1) == 0;
                        } else {
                            i++;
                        }
                    }
                }

                if (renderers.size() == 0) {
                    block = color(14);
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
                    block = color(4);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(plugin.api.getLang("SubServers", "Interface.Generic.Back-Arrow"));
                    block.setItemMeta(blockMeta);
                    inv.setItem(i++, block);
                    inv.setItem(i++, block);
                } else i += 2;
                i++;
                if (hasHistory()) {
                    block = color(14);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(plugin.api.getLang("SubServers", "Interface.Generic.Back"));
                    block.setItemMeta(blockMeta);
                    inv.setItem(i++, block);
                    inv.setItem(i++, block);
                    inv.setItem(i++, block);
                    i++;
                }
                if (renderers.size() - 1 > max) {
                    block = color(4);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(plugin.api.getLang("SubServers", "Interface.Generic.Next-Arrow"));
                    block.setItemMeta(blockMeta);
                    inv.setItem(i++, block);
                    inv.setItem(i, block);
                }

                player.openInventory(inv);
                open = true;
            }
        }));
    }

    public void groupMenu(final int page) {
        setDownloading(ChatColor.stripColor(plugin.api.getLang("SubServers", "Interface.Group-Menu.Title")));
        plugin.api.getServers(servers -> AgnosticScheduler.following(player).runs(plugin, c -> {
            setDownloading(null);
            lastVisitedObjects[0] = null;
            lastPage = page;
            lastMenu = () -> groupMenu(1);
            windowHistory.add(() -> groupMenu(page));

            TreeMap<String, List<Server>> groups = new TreeMap<String, List<Server>>();
            List<Server> ungrouped = new ArrayList<Server>();
            {
                HashMap<String, String> conflitresolver = new HashMap<String, String>();
                for (Server server : servers.values()) {
                    List<String> sgl = server.getGroups();
                    if (sgl.size() == 0) {
                        ungrouped.add(server);
                    } else {
                        for (String name : sgl) {
                            String group = name;
                            if (conflitresolver.containsKey(name.toLowerCase())) {
                                group = conflitresolver.get(name.toLowerCase());
                            } else {
                                conflitresolver.put(name.toLowerCase(), name);
                            }
                            List<Server> list = (groups.containsKey(group))? groups.get(group) : new ArrayList<Server>();
                            list.add(server);
                            groups.put(group, list);
                        }
                    }
                }
            }

            List<String> index = new LinkedList<String>();
            if (ungrouped.size() != 0) index.add(null);
            index.addAll(groups.keySet());

            ItemStack block;
            ItemMeta blockMeta;
            ItemStack div = color(15);
            ItemMeta divMeta = div.getItemMeta();
            divMeta.setDisplayName(ChatColor.RESET.toString());
            div.setItemMeta(divMeta);

            int i = 0;
            int min = ((page - 1) * 36);
            int max = (min + 35);
            int count = (index.size() == 0)?27:((index.size() - min >= max)?36:index.size() - min);
            int area = (count % 9 == 0) ? count : ((count / 9) + 1) * 9;

            Inventory inv = Bukkit.createInventory(null, 18 + area, plugin.api.getLang("SubServers", "Interface.Group-Menu.Title"));
            block = color(7);
            block.setItemMeta(divMeta);
            while (i < area) {
                inv.setItem(i, block);
                i++;
            }
            ItemStack adiv = block;
            i = ((count < 9) ? ((9 - count) / 2) : 0);

            boolean even = (count & 1) == 0 && count < 9;
            int color;

            for (String group : index) {
                if (index.indexOf(group) >= min && index.indexOf(group) <= max) {
                    if (even && (i == 4 || i == 13 || i == 22 || i == 31)) inv.setItem(i++, adiv);
                    color = (((i & 1) == 0)? 1 : 4);

                    block = color(color);
                    blockMeta = block.getItemMeta();
                    int size;
                    if (group == null) {
                        blockMeta.setDisplayName(plugin.api.getLang("SubServers", "Interface.Group-Menu.Ungrouped"));
                        size = ungrouped.size();
                    } else {
                        blockMeta.setDisplayName(ChatColor.GOLD + group);
                        size = groups.get(group).size();
                    }
                    LinkedList<String> lore = new LinkedList<String>();
                    lore.add(plugin.api.getLang("SubServers", "Interface.Group-Menu.Group-Server-Count").replace("$int$", new DecimalFormat("#,###").format(size)));
                    blockMeta.setLore(lore);
                    block.setItemMeta(blockMeta);
                    inv.setItem(i, block);

                    count--;
                    if (count < 9 && (i == 8 || i == 17 || i == 26)) {
                        i += ((9 - count) / 2) + 1;
                        even = (count & 1) == 0;
                    } else {
                        i++;
                    }
                }
            }

            i = inv.getSize() - 18;
            while (i < inv.getSize()) {
                inv.setItem(i, div);
                i++;
            }
            i = inv.getSize() - 9;

            if (min != 0) {
                block = color(4);
                blockMeta = block.getItemMeta();
                blockMeta.setDisplayName(plugin.api.getLang("SubServers", "Interface.Generic.Back-Arrow"));
                block.setItemMeta(blockMeta);
                inv.setItem(i++, block);
                inv.setItem(i++, block);
            } else i += 2;
            i++;
            block = color(5);
            blockMeta = block.getItemMeta();
            blockMeta.setDisplayName(plugin.api.getLang("SubServers", "Interface.Group-Menu.Server-Menu"));
            block.setItemMeta(blockMeta);
            inv.setItem(i++, block);
            inv.setItem(i++, block);
            inv.setItem(i++, block);
            i++;
            if (index.size() - 1 > max) {
                block = color(4);
                blockMeta = block.getItemMeta();
                blockMeta.setDisplayName(plugin.api.getLang("SubServers", "Interface.Generic.Next-Arrow"));
                block.setItemMeta(blockMeta);
                inv.setItem(i++, block);
                inv.setItem(i, block);
            }

            player.openInventory(inv);
            open = true;
        }));
    }

    public void serverMenu(final int page, final String host, final String group) {
        setDownloading(ChatColor.stripColor((host == null)?((group == null)?plugin.api.getLang("SubServers", "Interface.Server-Menu.Title"):((group.length() == 0)?plugin.api.getLang("SubServers", "Interface.Group-SubServer.Title-Ungrouped"):plugin.api.getLang("SubServers", "Interface.Group-SubServer.Title").replace("$str$", group))):plugin.api.getLang("SubServers", "Interface.Host-SubServer.Title").replace("$str$", host)));
        Value<String> hostname = new Container<String>(host);
        Value<List<Server>> servercontainer = new Container<List<Server>>(new LinkedList<Server>());
        Runnable renderer = () -> AgnosticScheduler.following(player).runs(plugin, c -> {
            setDownloading(null);
            lastPage = page;

            List<Server> servers = servercontainer.value();
            lastVisitedObjects[0] = host;
            lastVisitedObjects[1] = group;
            windowHistory.add(() -> serverMenu(page, host, group));

            ItemStack block;
            ItemMeta blockMeta;
            ItemStack div = color(15);
            ItemMeta divMeta = div.getItemMeta();
            divMeta.setDisplayName(ChatColor.RESET.toString());
            div.setItemMeta(divMeta);

            int i = 0;
            int min = ((page - 1) * 36);
            int max = (min + 35);
            int count = (servers.size() == 0)?27:((servers.size() - min >= max)?36:servers.size() - min);
            int area = (count % 9 == 0) ? count : ((count / 9) + 1) * 9;

            Inventory inv = Bukkit.createInventory(null, 18 + area, (host == null)?((group == null)?plugin.api.getLang("SubServers", "Interface.Server-Menu.Title"):((group.length() == 0)?plugin.api.getLang("SubServers", "Interface.Group-SubServer.Title-Ungrouped"):plugin.api.getLang("SubServers", "Interface.Group-SubServer.Title").replace("$str$", group))):plugin.api.getLang("SubServers", "Interface.Host-SubServer.Title").replace("$str$", hostname.value()));
            block = color(7);
            block.setItemMeta(divMeta);
            while (i < area) {
                inv.setItem(i, block);
                i++;
            }
            ItemStack adiv = block;
            i = ((count < 9) ? ((9 - count) / 2) : 0);

            boolean even = (count & 1) == 0 && count < 9;
            int external, online, temp, offline, disabled;

            for (Server server : servers) {
                if (servers.indexOf(server) >= min && servers.indexOf(server) <= max) {
                    if (even && (i == 4 || i == 13 || i == 22 || i == 31)) inv.setItem(i++, adiv);
                    external = (((i & 1) == 0)? 0 : 8);
                    online = (((i & 1) == 0)? 5 : 13);
                    temp = (((i & 1) == 0)? 3 : 11);
                    offline = (((i & 1) == 0)? 4 : 1);
                    disabled = (((i & 1) == 0)? 2 : 14);

                    if (!(server instanceof SubServer)) {
                        block = color(external);
                        blockMeta = block.getItemMeta();
                        blockMeta.setDisplayName(ChatColor.WHITE + server.getDisplayName());
                        LinkedList<String> lore = new LinkedList<String>();
                        if (!server.getName().equals(server.getDisplayName()))
                            lore.add(ChatColor.GRAY + server.getName());
                        lore.add(plugin.api.getLang("SubServers", "Interface.Server-Menu.Server-External"));
                        lore.add(plugin.api.getLang("SubServers", "Interface.Server-Menu.Server-Player-Count").replace("$int$", new DecimalFormat("#,###").format(server.getRemotePlayers().size())));
                        lore.add(ChatColor.WHITE + ((plugin.config.get().getMap("Settings").getBoolean("Show-Addresses", false))?server.getAddress().getAddress().getHostAddress()+':':"") + server.getAddress().getPort());
                        blockMeta.setLore(lore);
                    } else if (((SubServer) server).isRunning()) {
                        int blocktype = (((SubServer) server).getStopAction() == SubServer.StopAction.REMOVE_SERVER || ((SubServer) server).getStopAction() == SubServer.StopAction.RECYCLE_SERVER || ((SubServer) server).getStopAction() == SubServer.StopAction.DELETE_SERVER)?temp:online;
                        block = color(blocktype);
                        blockMeta = block.getItemMeta();
                        LinkedList<String> lore = new LinkedList<String>();
                        if (!server.getName().equals(server.getDisplayName()))
                            lore.add(ChatColor.GRAY + server.getName());
                        if (((SubServer) server).getStopAction() == SubServer.StopAction.REMOVE_SERVER || ((SubServer) server).getStopAction() == SubServer.StopAction.RECYCLE_SERVER || ((SubServer) server).getStopAction() == SubServer.StopAction.DELETE_SERVER) {
                            blockMeta.setDisplayName(ChatColor.AQUA + server.getDisplayName());
                            lore.add(plugin.api.getLang("SubServers", "Interface.Server-Menu.SubServer-Temporary"));
                        } else blockMeta.setDisplayName(ChatColor.GREEN + server.getDisplayName());
                        lore.add(plugin.api.getLang("SubServers", "Interface.Server-Menu.Server-Player-Count").replace("$int$", new DecimalFormat("#,###").format(server.getRemotePlayers().size())));
                        lore.add(ChatColor.WHITE + ((plugin.config.get().getMap("Settings").getBoolean("Show-Addresses", false))?server.getAddress().getAddress().getHostAddress()+':':"") + server.getAddress().getPort());
                        blockMeta.setLore(lore);
                    } else if (((SubServer) server).isAvailable() && ((SubServer) server).isEnabled() && ((SubServer) server).getCurrentIncompatibilities().size() == 0) {
                        block = color(offline);
                        blockMeta = block.getItemMeta();
                        blockMeta.setDisplayName(ChatColor.YELLOW + server.getDisplayName());
                        LinkedList<String> lore = new LinkedList<String>();
                        if (!server.getName().equals(server.getDisplayName()))
                            lore.add(ChatColor.GRAY + server.getName());
                        lore.add(plugin.api.getLang("SubServers", "Interface.Server-Menu.SubServer-Offline"));
                        lore.add(ChatColor.WHITE + ((plugin.config.get().getMap("Settings").getBoolean("Show-Addresses", false))?server.getAddress().getAddress().getHostAddress()+':':"") + server.getAddress().getPort());
                        blockMeta.setLore(lore);
                    } else {
                        block = color(disabled);
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
                        if (!((SubServer) server).isAvailable() || !((SubServer) server).isEnabled()) lore.add(plugin.api.getLang("SubServers", (!((SubServer) server).isAvailable())?"Interface.Server-Menu.SubServer-Unavailable":"Interface.Server-Menu.SubServer-Disabled"));
                        lore.add(ChatColor.WHITE + ((plugin.config.get().getMap("Settings").getBoolean("Show-Addresses", false))?server.getAddress().getAddress().getHostAddress()+':':"") + server.getAddress().getPort());
                        blockMeta.setLore(lore);
                    }
                    block.setItemMeta(blockMeta);
                    inv.setItem(i, block);

                    count--;
                    if (count < 9 && (i == 8 || i == 17 || i == 26)) {
                        i += ((9 - count) / 2) + 1;
                        even = (count & 1) == 0;
                    } else {
                        i++;
                    }
                }
            }

            if (servers.size() == 0) {
                block = color(14);
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
                block = color(4);
                blockMeta = block.getItemMeta();
                blockMeta.setDisplayName(plugin.api.getLang("SubServers", "Interface.Generic.Back-Arrow"));
                block.setItemMeta(blockMeta);
                inv.setItem(i++, block);
                inv.setItem(i++, block);
            } else i += 2;
            i++;
            if (host == null || group == null || hasHistory()) {
                block = color(((host == null && group == null)? 11 : 14));
                blockMeta = block.getItemMeta();
                blockMeta.setDisplayName((host == null && group == null)?plugin.api.getLang("SubServers", "Interface.Server-Menu.Host-Menu"):plugin.api.getLang("SubServers", "Interface.Generic.Back"));
                block.setItemMeta(blockMeta);
                inv.setItem(i++, block);
                inv.setItem(i++, block);
                inv.setItem(i++, block);
                i++;
            }
            if (servers.size() - 1 > max) {
                block = color(4);
                blockMeta = block.getItemMeta();
                blockMeta.setDisplayName(plugin.api.getLang("SubServers", "Interface.Generic.Next-Arrow"));
                block.setItemMeta(blockMeta);
                inv.setItem(i++, block);
                inv.setItem(i, block);
            }

            player.openInventory(inv);
            open = true;
        });

        if (host != null && host.length() > 0) {
            plugin.api.getHost(host, object -> {
                if (object == null) {
                    if (hasHistory()) back();
                } else {
                    hostname.value(object.getDisplayName());
                    servercontainer.value().addAll(object.getSubServers().values());
                    renderer.run();
                }
            });
        } else if (group != null) {
            plugin.api.getGroup((group.length() == 0)?null:group, servers -> {
                if (servers == null) {
                    if (hasHistory()) back();
                } else {
                    servercontainer.value().addAll(servers.value());
                    renderer.run();
                }
            });
        } else {
            plugin.api.getServers(servers -> {
                servercontainer.value().addAll(servers.values());
                renderer.run();
            });
        }
    }

    public void serverAdmin(final String name) {
        setDownloading(ChatColor.stripColor(plugin.api.getLang("SubServers", "Interface.Server-Admin.Title").replace("$str$", name)));
        BiConsumer<Server, Host> renderer = (server, host) -> AgnosticScheduler.following(player).runs(plugin, c -> {
            setDownloading(null);
            lastVisitedObjects[0] = server;
            ItemStack block;
            ItemMeta blockMeta;
            ItemStack div = color(15);
            ItemMeta divMeta = div.getItemMeta();
            divMeta.setDisplayName(ChatColor.RESET.toString());
            div.setItemMeta(divMeta);

            Inventory inv = Bukkit.createInventory(null, 36, plugin.api.getLang("SubServers", "Interface.Server-Admin.Title").replace("$str$", server.getDisplayName()));
            SubServer subserver = (host != null)? (SubServer) server : null;

            int i = 0;
            while (i < inv.getSize()) {
                inv.setItem(i, div);
                i++;
            }
            i = 0;

            Player player = this.player;
            if (host == null || ((SubServer) server).isRunning()) {
                if (host == null || !permits(server, player, "subservers.subserver.%.*", "subservers.subserver.%.terminate")) {
                    block = color(7);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(ChatColor.GRAY+ChatColor.stripColor(plugin.api.getLang("SubServers", "Interface.Server-Admin.Terminate")));
                    if (host != null) blockMeta.setLore(Arrays.asList(plugin.api.getLang("SubServers", "Interface.Generic.Invalid-Permission").replace("$str$", "subservers.subserver." + name.toLowerCase() + ".terminate")));
                } else {
                    block = color(14);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(plugin.api.getLang("SubServers", "Interface.Server-Admin.Terminate"));
                }

                block.setItemMeta(blockMeta);
                inv.setItem(1, block);
                inv.setItem(10, block);

                if (host == null || !permits(server, player, "subservers.subserver.%.*", "subservers.subserver.%.stop")) {
                    block = color(7);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(ChatColor.GRAY+ChatColor.stripColor(plugin.api.getLang("SubServers", "Interface.Server-Admin.Stop")));
                    if (host != null) blockMeta.setLore(Arrays.asList(plugin.api.getLang("SubServers", "Interface.Generic.Invalid-Permission").replace("$str$", "subservers.subserver." + name.toLowerCase() + ".stop")));
                } else {
                    block = color(2);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(plugin.api.getLang("SubServers", "Interface.Server-Admin.Stop"));
                }
                block.setItemMeta(blockMeta);
                inv.setItem(2, block);
                inv.setItem(3, block);
                inv.setItem(11, block);
                inv.setItem(12, block);

                boolean permits;
                if ((host == null && server.getSubData()[0] == null) | (permits = !permits(server, player, "subservers.subserver.%.*", "subservers.subserver.%.command"))) {
                    block = color(7);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(ChatColor.GRAY+ChatColor.stripColor(plugin.api.getLang("SubServers", "Interface.Server-Admin.Command")));
                    if (permits) blockMeta.setLore(Arrays.asList(plugin.api.getLang("SubServers", "Interface.Generic.Invalid-Permission").replace("$str$", "subservers.subserver." + name.toLowerCase() + ".command")));
                } else {
                    block = color(3);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(plugin.api.getLang("SubServers", "Interface.Server-Admin.Command"));
                }
                block.setItemMeta(blockMeta);
                inv.setItem(5, block);
                inv.setItem(6, block);
                inv.setItem(7, block);
                inv.setItem(14, block);
                inv.setItem(15, block);
                inv.setItem(16, block);
            } else {
                if (!permits(subserver, player, "subservers.subserver.%.*", "subservers.subserver.%.start")) {
                    block = color(7);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(ChatColor.GRAY+ChatColor.stripColor(plugin.api.getLang("SubServers", "Interface.Server-Admin.Start")));
                    blockMeta.setLore(Arrays.asList(plugin.api.getLang("SubServers", "Interface.Generic.Invalid-Permission").replace("$str$", "subservers.subserver." + name.toLowerCase() + ".start")));
                } else if (!host.isAvailable() || !host.isEnabled() || !subserver.isAvailable() || !subserver.isEnabled() || subserver.getCurrentIncompatibilities().size() != 0) {
                    block = color(7);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(ChatColor.GRAY+ChatColor.stripColor(plugin.api.getLang("SubServers", "Interface.Server-Admin.Start")));
                } else {
                    block = color(5);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(plugin.api.getLang("SubServers", "Interface.Server-Admin.Start"));
                }
                block.setItemMeta(blockMeta);
                SubCreator.ServerTemplate template;
                if (subserver.getTemplate() == null || !(template = host.getCreator().getTemplate(subserver.getTemplate())).isEnabled() || !template.canUpdate()) {
                    inv.setItem(3, block);
                    inv.setItem(4, block);
                    inv.setItem(5, block);
                    inv.setItem(12, block);
                    inv.setItem(13, block);
                    inv.setItem(14, block);
                } else {
                    inv.setItem(1, block);
                    inv.setItem(2, block);
                    inv.setItem(3, block);
                    inv.setItem(10, block);
                    inv.setItem(11, block);
                    inv.setItem(12, block);

                    if (!permits(subserver, player, "subservers.subserver.%.*", "subservers.subserver.%.update")) {
                        block = color(7);
                        blockMeta = block.getItemMeta();
                        blockMeta.setDisplayName(ChatColor.GRAY+ChatColor.stripColor(plugin.api.getLang("SubServers", "Interface.Server-Admin.Update")));
                        blockMeta.setLore(Arrays.asList(plugin.api.getLang("SubServers", "Interface.Generic.Invalid-Permission").replace("$str$", "subservers.subserver." + name.toLowerCase() + ".update")));
                    } else if (!host.isAvailable() || !host.isEnabled() || !subserver.isAvailable() || subserver.getCurrentIncompatibilities().size() != 0) {
                        block = color(7);
                        blockMeta = block.getItemMeta();
                        blockMeta.setDisplayName(ChatColor.GRAY+ChatColor.stripColor(plugin.api.getLang("SubServers", "Interface.Server-Admin.Update")));
                    } else {
                        block = color(4);
                        blockMeta = block.getItemMeta();
                        blockMeta.setDisplayName(plugin.api.getLang("SubServers", "Interface.Server-Admin.Update"));
                    }
                    block.setItemMeta(blockMeta);
                    inv.setItem(5, block);
                    inv.setItem(6, block);
                    inv.setItem(7, block);
                    inv.setItem(14, block);
                    inv.setItem(15, block);
                    inv.setItem(16, block);
                }
            }

            if (serverPlugins.size() > 0) {
                block = color(11);
                blockMeta = block.getItemMeta();
                blockMeta.setDisplayName(plugin.api.getLang("SubServers", "Interface.Server-Admin.Plugins"));
                block.setItemMeta(blockMeta);
            } else {
                block = div;
            }
            inv.setItem(27, block);
            inv.setItem(28, block);

            if (host == null) {
                block = color(0);
                blockMeta = block.getItemMeta();
                blockMeta.setDisplayName(ChatColor.WHITE + server.getDisplayName());
                LinkedList<String> lore = new LinkedList<String>();
                if (!server.getName().equals(server.getDisplayName()))
                    lore.add(ChatColor.GRAY + server.getName());
                lore.add(plugin.api.getLang("SubServers", "Interface.Server-Menu.Server-External"));
                lore.add(plugin.api.getLang("SubServers", "Interface.Server-Menu.Server-Player-Count").replace("$int$", new DecimalFormat("#,###").format(server.getRemotePlayers().size())));
                lore.add(ChatColor.WHITE + ((plugin.config.get().getMap("Settings").getBoolean("Show-Addresses", false))?server.getAddress().getAddress().getHostAddress()+':':"") + server.getAddress().getPort());
                blockMeta.setLore(lore);
            } else if (subserver.isRunning()) {
                int blocktype = (subserver.getStopAction() == SubServer.StopAction.REMOVE_SERVER || subserver.getStopAction() == SubServer.StopAction.RECYCLE_SERVER || subserver.getStopAction() == SubServer.StopAction.DELETE_SERVER)? 11 : 5;
                block = color(blocktype);
                blockMeta = block.getItemMeta();
                LinkedList<String> lore = new LinkedList<String>();
                if (!subserver.getName().equals(subserver.getDisplayName()))
                    lore.add(ChatColor.GRAY + subserver.getName());
                if (subserver.getStopAction() == SubServer.StopAction.REMOVE_SERVER || subserver.getStopAction() == SubServer.StopAction.RECYCLE_SERVER || subserver.getStopAction() == SubServer.StopAction.DELETE_SERVER) {
                    blockMeta.setDisplayName(ChatColor.AQUA + subserver.getDisplayName());
                    lore.add(plugin.api.getLang("SubServers", "Interface.Server-Menu.SubServer-Temporary"));
                } else blockMeta.setDisplayName(ChatColor.GREEN + subserver.getDisplayName());
                lore.add(plugin.api.getLang("SubServers", "Interface.Server-Menu.Server-Player-Count").replace("$int$", new DecimalFormat("#,###").format(subserver.getRemotePlayers().size())));
                lore.add(ChatColor.WHITE + ((plugin.config.get().getMap("Settings").getBoolean("Show-Addresses", false))?subserver.getAddress().getAddress().getHostAddress()+':':"") + subserver.getAddress().getPort());
                blockMeta.setLore(lore);
            } else if (subserver.isAvailable() && subserver.isEnabled() && subserver.getCurrentIncompatibilities().size() == 0) {
                block = color(4);
                blockMeta = block.getItemMeta();
                blockMeta.setDisplayName(ChatColor.YELLOW + subserver.getDisplayName());
                LinkedList<String> lore = new LinkedList<String>();
                if (!subserver.getName().equals(subserver.getDisplayName()))
                    lore.add(ChatColor.GRAY + subserver.getName());
                lore.add(plugin.api.getLang("SubServers", "Interface.Server-Menu.SubServer-Offline"));
                lore.add(ChatColor.WHITE + ((plugin.config.get().getMap("Settings").getBoolean("Show-Addresses", false))?subserver.getAddress().getAddress().getHostAddress()+':':"") + subserver.getAddress().getPort());
                blockMeta.setLore(lore);
            } else {
                block = color(14);
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
                if (!subserver.isAvailable() || !subserver.isEnabled()) lore.add(plugin.api.getLang("SubServers", (!subserver.isAvailable())?"Interface.Server-Menu.SubServer-Unavailable":"Interface.Server-Menu.SubServer-Disabled"));
                lore.add(ChatColor.WHITE + ((plugin.config.get().getMap("Settings").getBoolean("Show-Addresses", false))?subserver.getAddress().getAddress().getHostAddress()+':':"") + subserver.getAddress().getPort());
                blockMeta.setLore(lore);
            }
            block.setItemMeta(blockMeta);
            inv.setItem(30, block);
            inv.setItem(31, block);
            inv.setItem(32, block);

            if (hasHistory()) {
                block = color(14);
                blockMeta = block.getItemMeta();
                blockMeta.setDisplayName(plugin.api.getLang("SubServers", "Interface.Generic.Back"));
                block.setItemMeta(blockMeta);
                inv.setItem(34, block);
                inv.setItem(35, block);
            }

            player.openInventory(inv);
            open = true;
        });

        plugin.api.getServer(name, server -> {
            windowHistory.add(() -> serverAdmin(name));
            if (server == null) {
                if (hasHistory()) back();
            } else {
                if (server instanceof SubServer) {
                    ((SubServer) server).getHost(host -> {
                        if (host == null) {
                            if (hasHistory()) back();
                        } else {
                            renderer.accept(server, host);
                        }
                    });
                } else {
                    renderer.accept(server, null);
                }
            }
        });
    }

    public void serverPlugin(final int page, final String name) {
        setDownloading(ChatColor.stripColor(plugin.api.getLang("SubServers", "Interface.SubServer-Plugin.Title").replace("$str$", name)));
        plugin.api.getServer(name, server -> AgnosticScheduler.following(player).runs(plugin, c -> {
            windowHistory.add(() -> serverPlugin(page, name));
            if (server == null) {
                if (hasHistory()) back();
            } else {
                setDownloading(null);
                lastVisitedObjects[0] = server;
                lastPage = page;
                List<String> renderers = new LinkedList<String>();
                for (String renderer : serverPlugins.keySet()) {
                    if (serverPlugins.get(renderer).isEnabled(server)) renderers.add(renderer);
                }
                Collections.sort(renderers);

                ItemStack block;
                ItemMeta blockMeta;
                ItemStack div = color(15);
                ItemMeta divMeta = div.getItemMeta();
                divMeta.setDisplayName(ChatColor.RESET.toString());
                div.setItemMeta(divMeta);

                int i = 0;
                int min = ((page - 1) * 36);
                int max = (min + 35);
                int count = (renderers.size() == 0)?27:((renderers.size() - min >= max)?36:renderers.size() - min);
                int area = (count % 9 == 0) ? count : ((count / 9) + 1) * 9;

                Inventory inv = Bukkit.createInventory(null, 18 + area, plugin.api.getLang("SubServers", "Interface.SubServer-Plugin.Title").replace("$str$", server.getDisplayName()));
                block = color(7);
                block.setItemMeta(divMeta);
                while (i < area) {
                    inv.setItem(i, block);
                    i++;
                }
                ItemStack adiv = block;
                i = ((count < 9) ? ((9 - count) / 2) : 0);

                boolean even = (count & 1) == 0 && count < 9;

                for (String renderer : renderers) {
                    if (renderers.indexOf(renderer) >= min && renderers.indexOf(renderer) <= max) {
                        if (even && (i == 4 || i == 13 || i == 22 || i == 31)) inv.setItem(i++, adiv);

                        inv.setItem(i, serverPlugins.get(renderer).getIcon());

                        count--;
                        if (count < 9 && (i == 8 || i == 17 || i == 26)) {
                            i += ((9 - count) / 2) + 1;
                            even = (count & 1) == 0;
                        } else {
                            i++;
                        }
                    }
                }

                if (renderers.size() == 0) {
                    block = color(14);
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
                    block = color(4);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(plugin.api.getLang("SubServers", "Interface.Generic.Back-Arrow"));
                    block.setItemMeta(blockMeta);
                    inv.setItem(i++, block);
                    inv.setItem(i++, block);
                } else i += 2;
                i++;
                if (hasHistory()) {
                    block = color(14);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(plugin.api.getLang("SubServers", "Interface.Generic.Back"));
                    block.setItemMeta(blockMeta);
                    inv.setItem(i++, block);
                    inv.setItem(i++, block);
                    inv.setItem(i++, block);
                    i++;
                }
                if (renderers.size() - 1 > max) {
                    block = color(4);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(plugin.api.getLang("SubServers", "Interface.Generic.Next-Arrow"));
                    block.setItemMeta(blockMeta);
                    inv.setItem(i++, block);
                    inv.setItem(i, block);
                }

                player.openInventory(inv);
                open = true;
            }
        }));
    }
}
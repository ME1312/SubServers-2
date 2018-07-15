package net.ME1312.SubServers.Client.Bukkit.Graphic;

import net.ME1312.SubServers.Client.Bukkit.Library.NamedContainer;
import net.ME1312.SubServers.Client.Bukkit.Library.Version.Version;
import net.ME1312.SubServers.Client.Bukkit.Network.Packet.PacketDownloadHostInfo;
import net.ME1312.SubServers.Client.Bukkit.Network.Packet.PacketDownloadServerInfo;
import net.ME1312.SubServers.Client.Bukkit.Network.Packet.PacketDownloadServerList;
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
 * Internal GUI Renderer Class
 */
public class InternalUIRenderer extends UIRenderer {
    private static int MAX_VISITED_OBJECTS = 2;
    private List<Runnable> windowHistory = new LinkedList<Runnable>();
    protected Object[] lastVisitedObjects = new Object[MAX_VISITED_OBJECTS];
    protected int lastPage = 1;
    protected Runnable lastMenu = null;
    protected boolean open = false;
    protected final UUID player;
    private SubPlugin plugin;

    protected InternalUIRenderer(SubPlugin plugin, UUID player) {
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
            return new ItemStack(Material.AIR);
        }
    }

    public void hostMenu(final int page) {
        setDownloading(ChatColor.stripColor(plugin.api.getLang("SubServers", "Interface.Host-Menu.Title")));
        plugin.subdata.sendPacket(new PacketDownloadServerList(null, null, (json) -> {
            setDownloading(null);
            lastVisitedObjects[0] = null;
            lastPage = page;
            lastMenu = () -> hostMenu(1);
            windowHistory.add(() -> hostMenu(page));
            List<String> hosts = new ArrayList<String>();
            hosts.addAll(json.getSection("hosts").getKeys());

            ItemStack block;
            ItemMeta blockMeta;
            ItemStack div = createItem("STAINED_GLASS_PANE", "BLACK_STAINED_GLASS_PANE", (short) 15);
            ItemMeta divMeta = div.getItemMeta();
            divMeta.setDisplayName(ChatColor.RESET.toString());
            div.setItemMeta(divMeta);

            int i = 0;
            int min = ((page - 1) * 36);
            int max = (min + 35);
            int count = (hosts.size() == 0)?27:((hosts.size() - min >= max)?36:hosts.size() - min);
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

            for (String host : hosts) {
                if (hosts.indexOf(host) >= min && hosts.indexOf(host) <= max) {
                    if (even && (i == 4 || i == 13 || i == 22 || i == 31)) inv.setItem(i++, adiv);
                    enabled = (((i & 1) == 0) ? new NamedContainer<>("BLUE_STAINED_GLASS_PANE", (short) 3) : new NamedContainer<>("LIGHT_BLUE_STAINED_GLASS_PANE", (short) 11));
                    disabled = (((i & 1) == 0) ? new NamedContainer<>("MAGENTA_STAINED_GLASS_PANE", (short) 2) : new NamedContainer<>("RED_STAINED_GLASS_PANE", (short) 14));

                    if (json.getSection("hosts").getSection(host).getBoolean("enabled")) {
                        block = createItem("STAINED_GLASS_PANE", enabled.name(), enabled.get());
                        blockMeta = block.getItemMeta();
                        blockMeta.setDisplayName(ChatColor.AQUA + json.getSection("hosts").getSection(host).getString("display"));
                        LinkedList<String> lore = new LinkedList<String>();
                        if (!host.equals(json.getSection("hosts").getSection(host).getString("display")))
                            lore.add(ChatColor.GRAY + host);
                        lore.add(plugin.api.getLang("SubServers", "Interface.Host-Menu.Host-Server-Count").replace("$int$", new DecimalFormat("#,###").format(json.getSection("hosts").getSection(host).getSection("servers").getKeys().size())));
                        if (plugin.config.get().getSection("Settings").getBoolean("Show-Addresses", false)) lore.add(ChatColor.WHITE + json.getSection("hosts").getSection(host).getString("address"));
                        blockMeta.setLore(lore);
                    } else {
                        block = createItem("STAINED_GLASS_PANE", disabled.name(), disabled.get());
                        blockMeta = block.getItemMeta();
                        blockMeta.setDisplayName(ChatColor.RED + json.getSection("hosts").getSection(host).getString("display"));
                        LinkedList<String> lore = new LinkedList<String>();
                        if (!host.equals(json.getSection("hosts").getSection(host).getString("display")))
                            lore.add(ChatColor.GRAY + host);
                        lore.add(plugin.api.getLang("SubServers", "Interface.Host-Menu.Host-Disabled"));
                        if (plugin.config.get().getSection("Settings").getBoolean("Show-Addresses", false)) lore.add(ChatColor.WHITE + json.getSection("hosts").getSection(host).getString("address"));
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

            if (hosts.size() == 0) {
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
            if (json.getSection("groups").getKeys().size() <= 0) {
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
            if (hosts.size() - 1 > max) {
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

    public void hostAdmin(final String host) {
        setDownloading(ChatColor.stripColor(plugin.api.getLang("SubServers", "Interface.Host-Admin.Title").replace("$str$", host)));
        plugin.subdata.sendPacket(new PacketDownloadHostInfo(host, (json) -> {
            windowHistory.add(() -> hostAdmin(host));
            if (!json.getBoolean("valid")) {
                if (hasHistory()) back();
            } else {
                setDownloading(null);
                lastVisitedObjects[0] = host;

                ItemStack block;
                ItemMeta blockMeta;
                ItemStack div = createItem("STAINED_GLASS_PANE", "BLACK_STAINED_GLASS_PANE", (short) 15);
                ItemMeta divMeta = div.getItemMeta();
                divMeta.setDisplayName(ChatColor.RESET.toString());
                div.setItemMeta(divMeta);

                Inventory inv = Bukkit.createInventory(null, 36, plugin.api.getLang("SubServers", "Interface.Host-Admin.Title").replace("$str$", json.getSection("host").getString("display")));

                int i = 0;
                while (i < inv.getSize()) {
                    inv.setItem(i, div);
                    i++;
                }

                if (!(Bukkit.getPlayer(player).hasPermission("subservers.host.create.*") || Bukkit.getPlayer(player).hasPermission("subservers.host.create." + host.toLowerCase()))) {
                    block = createItem("STAINED_GLASS_PANE", "GRAY_STAINED_GLASS_PANE", (short) 7);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(ChatColor.GRAY+ChatColor.stripColor(plugin.api.getLang("SubServers", "Interface.Host-Admin.Creator")));
                    blockMeta.setLore(Arrays.asList(plugin.api.getLang("SubServers", "Interface.Generic.Invalid-Permission").replace("$str$", "subservers.host.create." + host.toLowerCase())));
                } else if (!json.getSection("host").getBoolean("enabled")) {
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

                if (!json.getSection("host").getBoolean("enabled") || hostPlugins.size() <= 0) {
                    block = div;
                } else {
                    block = createItem("STAINED_GLASS_PANE", "BLUE_STAINED_GLASS_PANE", (short) 11);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(plugin.api.getLang("SubServers", "Interface.Host-Admin.Plugins"));
                    block.setItemMeta(blockMeta);
                }
                inv.setItem(27, block);
                inv.setItem(28, block);

                if (json.getSection("host").getBoolean("enabled")) {
                    block = createItem("STAINED_GLASS_PANE", "BLUE_STAINED_GLASS_PANE", (short) 11);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(ChatColor.AQUA + json.getSection("host").getString("display"));
                    LinkedList<String> lore = new LinkedList<String>();
                    if (!host.equals(json.getSection("host").getString("display")))
                        lore.add(ChatColor.GRAY + host);
                    lore.add(plugin.api.getLang("SubServers", "Interface.Host-Menu.Host-Server-Count").replace("$int$", new DecimalFormat("#,###").format(json.getSection("host").getSection("servers").getKeys().size())));
                    if (plugin.config.get().getSection("Settings").getBoolean("Show-Addresses", false)) lore.add(ChatColor.WHITE + json.getSection("host").getString("address"));
                    blockMeta.setLore(lore);
                } else {
                    block = createItem("STAINED_GLASS_PANE", "RED_STAINED_GLASS_PANE", (short) 14);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(ChatColor.RED + json.getSection("host").getString("display"));
                    LinkedList<String> lore = new LinkedList<String>();
                    if (!host.equals(json.getSection("host").getString("display")))
                        lore.add(ChatColor.GRAY + host);
                    lore.add(plugin.api.getLang("SubServers", "Interface.Host-Menu.Host-Disabled"));
                    if (plugin.config.get().getSection("Settings").getBoolean("Show-Addresses", false)) lore.add(ChatColor.WHITE + json.getSection("host").getString("address"));
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
        }));
    }

    public void hostCreator(final CreatorOptions options) {
        setDownloading(ChatColor.stripColor(plugin.api.getLang("SubServers", "Interface.Host-Creator.Title").replace("$str$", options.getHost())));
        if (!options.init())
            windowHistory.add(() -> hostCreator(options));
        lastVisitedObjects[0] = options;

        plugin.subdata.sendPacket(new PacketDownloadHostInfo(options.getHost(), json -> {
            if (!json.getBoolean("valid")|| !json.getSection("host").getBoolean("enabled")) {
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

                Inventory inv = Bukkit.createInventory(null, 54, plugin.api.getLang("SubServers", "Interface.Host-Creator.Title").replace("$str$", json.getSection("host").getString("display")));

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
                    blockMeta.setLore(Arrays.asList(ChatColor.GRAY + "v" + options.getVersion().toString()));
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
        }));
    }

    public void hostCreatorTemplates(final int page, final CreatorOptions options) {
        setDownloading(ChatColor.stripColor(plugin.api.getLang("SubServers", "Interface.Host-Creator.Edit-Template.Title").replace("$str$", options.getHost())));
        lastVisitedObjects[0] = options;
        if (!options.init()) lastVisitedObjects[0] = options.getHost();
        plugin.subdata.sendPacket(new PacketDownloadHostInfo(options.getHost(), (json) -> {
            if (!json.getBoolean("valid")|| !json.getSection("host").getBoolean("enabled")) {
                lastVisitedObjects[0] = null;
                if (hasHistory()) back();
            } else {
                lastPage = page;
                setDownloading(null);
                List<String> templates = new ArrayList<String>();
                for (String template : json.getSection("host").getSection("creator").getSection("templates").getKeys()) {
                    if (json.getSection("host").getSection("creator").getSection("templates").getSection(template).getBoolean("enabled")) templates.add(template);
                }
                Collections.sort(templates);

                ItemStack block;
                ItemMeta blockMeta;
                ItemStack div = createItem("STAINED_GLASS_PANE", "BLACK_STAINED_GLASS_PANE", (short) 15);
                ItemMeta divMeta = div.getItemMeta();
                divMeta.setDisplayName(ChatColor.RESET.toString());
                div.setItemMeta(divMeta);

                int i = 0;
                int min = ((page - 1) * 36);
                int max = (min + 35);
                int count = (templates.size() == 0)?27:((templates.size() - min >= max)?36:templates.size() - min);
                int area = (count % 9 == 0)?count: (int) (Math.floor(count / 9) + 1) * 9;

                Inventory inv = Bukkit.createInventory(null, 18 + area, plugin.api.getLang("SubServers", "Interface.Host-Creator.Edit-Template.Title").replace("$str$", json.getSection("host").getString("display")));
                block = createItem("STAINED_GLASS_PANE", "GRAY_STAINED_GLASS_PANE", (short) 7);
                block.setItemMeta(divMeta);
                while (i < area) {
                    inv.setItem(i, block);
                    i++;
                }
                ItemStack adiv = block;
                i = (int) ((count < 9) ? Math.floor((9 - count) / 2) : 0);

                boolean even = (count & 1) == 0 && count < 9;
                for (String template : templates) {
                    if (templates.indexOf(template) >= min && templates.indexOf(template) <= max) {
                        if (even && (i == 4 || i == 13 || i == 22 || i == 31)) inv.setItem(i++, adiv);

                        block = parseItem(json.getSection("host").getSection("creator").getSection("templates").getSection(template).getString("icon"), new ItemStack(Material.ENDER_CHEST));
                        blockMeta = block.getItemMeta();
                        blockMeta.setDisplayName(ChatColor.YELLOW + json.getSection("host").getSection("creator").getSection("templates").getSection(template).getString("display"));
                        LinkedList<String> lore = new LinkedList<String>();
                        if (!template.equals(json.getSection("host").getSection("creator").getSection("templates").getSection(template).getString("display")))
                            lore.add(ChatColor.GRAY + template);
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

                if (templates.size() == 0) {
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
                if (templates.size() - 1 > max) {
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
        }));
    }

    public void hostPlugin(final int page, final String host) {
        setDownloading(ChatColor.stripColor(plugin.api.getLang("SubServers", "Interface.Host-Plugin.Title").replace("$str$", host)));
        plugin.subdata.sendPacket(new PacketDownloadHostInfo(host, (json) -> {
            windowHistory.add(() -> hostPlugin(page, host));
            if (!json.getBoolean("valid")) {
                if (hasHistory()) back();
            } else {
                setDownloading(null);
                lastVisitedObjects[0] = host;
                lastPage = page;
                List<String> renderers = new ArrayList<String>();
                for (String renderer : renderers) {
                    if (hostPlugins.get(renderer).isEnabled(json.getSection("host"))) renderers.add(renderer);
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

                Inventory inv = Bukkit.createInventory(null, 18 + area, plugin.api.getLang("SubServers", "Interface.Host-Plugin.Title").replace("$str$", json.getSection("host").getString("display")));
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
        }));
    }

    public void groupMenu(final int page) {
        setDownloading(ChatColor.stripColor(plugin.api.getLang("SubServers", "Interface.Group-Menu.Title")));
        plugin.subdata.sendPacket(new PacketDownloadServerList(null, null, (json) -> {
            setDownloading(null);
            lastVisitedObjects[0] = null;
            lastPage = page;
            lastMenu = () -> groupMenu(1);
            windowHistory.add(() -> groupMenu(page));
            List<String> groups = new ArrayList<String>();
            groups.addAll(json.getSection("groups").getKeys());

            ItemStack block;
            ItemMeta blockMeta;
            ItemStack div = createItem("STAINED_GLASS_PANE", "BLACK_STAINED_GLASS_PANE", (short) 15);
            ItemMeta divMeta = div.getItemMeta();
            divMeta.setDisplayName(ChatColor.RESET.toString());
            div.setItemMeta(divMeta);

            int i = 0;
            int min = ((page - 1) * 36);
            int max = (min + 35);
            int count = (groups.size() == 0)?27:((groups.size() - min >= max)?36:groups.size() - min);
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

            for (String group : groups) {
                if (groups.indexOf(group) >= min && groups.indexOf(group) <= max) {
                    if (even && (i == 4 || i == 13 || i == 22 || i == 31)) inv.setItem(i++, adiv);
                    color = (((i & 1) == 0) ? new NamedContainer<>("ORANGE_STAINED_GLASS_PANE", (short) 1) : new NamedContainer<>("YELLOW_STAINED_GLASS_PANE", (short) 4));

                    block = createItem("STAINED_GLASS_PANE", color.name(), color.get());
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(ChatColor.GOLD + group);
                    LinkedList<String> lore = new LinkedList<String>();
                    lore.add(plugin.api.getLang("SubServers", "Interface.Group-Menu.Group-Server-Count").replace("$int$", new DecimalFormat("#,###").format(json.getSection("groups").getSection(group).getKeys().size())));
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

            if (groups.size() == 0) {
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
            if (groups.size() - 1 > max) {
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

    public void serverMenu(final int page, final String host, final String group) {
        setDownloading(ChatColor.stripColor((host == null)?((group == null)?plugin.api.getLang("SubServers", "Interface.Server-Menu.Title"):plugin.api.getLang("SubServers", "Interface.Group-SubServer.Title").replace("$str$", group)):plugin.api.getLang("SubServers", "Interface.Host-SubServer.Title").replace("$str$", host)));
        plugin.subdata.sendPacket(new PacketDownloadServerList(host, (host != null)?null:group, json -> {
            setDownloading(null);
            lastPage = page;

            HashMap<String, String> hosts = new HashMap<String, String>();
            List<String> servers = new ArrayList<String>();
            lastVisitedObjects[0] = host;
            lastVisitedObjects[1] = group;
            if (host != null && json.getSection("hosts").getKeys().contains(host)) {
                for (String subserver : json.getSection("hosts").getSection(host).getSection("servers").getKeys()) {
                    hosts.put(subserver, host);
                    servers.add(subserver);
                }
            } else if (group != null && json.getSection("groups").getKeys().contains(group)) {
                for (String server : json.getSection("groups").getSection(group).getKeys()) {
                    hosts.put(server, (json.getSection("groups").getSection(group).getSection(server).getKeys().contains("host") && json.getSection("hosts").getKeys().contains(json.getSection("groups").getSection(group).getSection(server).getString("host")))?json.getSection("groups").getSection(group).getSection(server).getString("host"):null);
                    servers.add(server);
                }
            } else {
                lastMenu = () -> serverMenu(1, null, null);
                for (String s : json.getSection("servers").getKeys()) {
                    hosts.put(s, null);
                    servers.add(s);
                }
                for (String h : json.getSection("hosts").getKeys()) {
                    for (String ss : json.getSection("hosts").getSection(h).getSection("servers").getKeys()) {
                        hosts.put(ss, h);
                        servers.add(ss);
                    }
                }
            }
            Collections.sort(servers);
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

            Inventory inv = Bukkit.createInventory(null, 18 + area, (host == null)?((group == null)?plugin.api.getLang("SubServers", "Interface.Server-Menu.Title"):plugin.api.getLang("SubServers", "Interface.Group-SubServer.Title").replace("$str$", group)):plugin.api.getLang("SubServers", "Interface.Host-SubServer.Title").replace("$str$", json.getSection("hosts").getSection(host).getString("display")));
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

            for (String server : servers) {
                if (servers.indexOf(server) >= min && servers.indexOf(server) <= max) {
                    if (even && (i == 4 || i == 13 || i == 22 || i == 31)) inv.setItem(i++, adiv);
                    external = (((i & 1) == 0) ? new NamedContainer<>("WHITE_STAINED_GLASS_PANE", (short) 0) : new NamedContainer<>("LIGHT_GRAY_STAINED_GLASS_PANE", (short) 8));
                    online = (((i & 1) == 0) ? new NamedContainer<>("LIME_STAINED_GLASS_PANE", (short) 5) : new NamedContainer<>("GREEN_STAINED_GLASS_PANE", (short) 13));
                    temp = (((i & 1) == 0) ? new NamedContainer<>("LIGHT_BLUE_STAINED_GLASS_PANE", (short) 3) : new NamedContainer<>("BLUE_STAINED_GLASS_PANE", (short) 11));
                    offline = (((i & 1) == 0) ? new NamedContainer<>("YELLOW_STAINED_GLASS_PANE", (short) 4) : new NamedContainer<>("ORANGE_STAINED_GLASS_PANE", (short) 1));
                    disabled = (((i & 1) == 0) ? new NamedContainer<>("MAGENTA_STAINED_GLASS_PANE", (short) 2) : new NamedContainer<>("RED_STAINED_GLASS_PANE", (short) 14));

                    if (hosts.get(server) == null) {
                        block = createItem("STAINED_GLASS_PANE", external.name(), external.get());
                        blockMeta = block.getItemMeta();
                        blockMeta.setDisplayName(ChatColor.AQUA + json.getSection("servers").getSection(server).getString("display"));
                        LinkedList<String> lore = new LinkedList<String>();
                        if (!server.equals(json.getSection("servers").getSection(server).getString("display")))
                            lore.add(ChatColor.GRAY + server);
                        lore.add(plugin.api.getLang("SubServers", "Interface.Server-Menu.Server-Player-Count").replace("$int$", new DecimalFormat("#,###").format(json.getSection("servers").getSection(server).getSection("players").getKeys().size())));
                        lore.add(plugin.api.getLang("SubServers", "Interface.Server-Menu.Server-External"));
                        lore.add(plugin.api.getLang("SubServers", "Interface.Server-Menu.SubServer-Invalid"));
                        lore.add(ChatColor.WHITE + ((plugin.config.get().getSection("Settings").getBoolean("Show-Addresses", false))?json.getSection("servers").getSection(server).getString("address"):json.getSection("servers").getSection(server).getString("address").split(":")[json.getSection("servers").getSection(server).getString("address").split(":").length - 1]));
                        blockMeta.setLore(lore);
                    } else if (json.getSection("hosts").getSection(hosts.get(server)).getSection("servers").getSection(server).getBoolean("temp")) {
                        block = createItem("STAINED_GLASS_PANE", temp.name(), temp.get());
                        blockMeta = block.getItemMeta();
                        blockMeta.setDisplayName(ChatColor.AQUA + json.getSection("hosts").getSection(hosts.get(server)).getSection("servers").getSection(server).getString("display"));
                        LinkedList<String> lore = new LinkedList<String>();
                        if (!server.equals(json.getSection("hosts").getSection(hosts.get(server)).getSection("servers").getSection(server).getString("display")))
                            lore.add(ChatColor.GRAY + server);
                        lore.add(plugin.api.getLang("SubServers", "Interface.Server-Menu.Server-Player-Count").replace("$int$", new DecimalFormat("#,###").format(json.getSection("hosts").getSection(hosts.get(server)).getSection("servers").getSection(server).getSection("players").getKeys().size())));
                        lore.add(plugin.api.getLang("SubServers", "Interface.Server-Menu.SubServer-Temporary"));
                        lore.add(ChatColor.WHITE + ((plugin.config.get().getSection("Settings").getBoolean("Show-Addresses", false))?json.getSection("hosts").getSection(hosts.get(server)).getSection("servers").getSection(server).getString("address"):json.getSection("hosts").getSection(hosts.get(server)).getSection("servers").getSection(server).getString("address").split(":")[json.getSection("hosts").getSection(hosts.get(server)).getSection("servers").getSection(server).getString("address").split(":").length - 1]));
                        blockMeta.setLore(lore);
                    } else if (json.getSection("hosts").getSection(hosts.get(server)).getSection("servers").getSection(server).getBoolean("running")) {
                        block = createItem("STAINED_GLASS_PANE", online.name(), online.get());
                        blockMeta = block.getItemMeta();
                        blockMeta.setDisplayName(ChatColor.GREEN + json.getSection("hosts").getSection(hosts.get(server)).getSection("servers").getSection(server).getString("display"));
                        LinkedList<String> lore = new LinkedList<String>();
                        if (!server.equals(json.getSection("hosts").getSection(hosts.get(server)).getSection("servers").getSection(server).getString("display")))
                            lore.add(ChatColor.GRAY + server);
                        lore.add(plugin.api.getLang("SubServers", "Interface.Server-Menu.Server-Player-Count").replace("$int$", new DecimalFormat("#,###").format(json.getSection("hosts").getSection(hosts.get(server)).getSection("servers").getSection(server).getSection("players").getKeys().size())));
                        lore.add(ChatColor.WHITE + ((plugin.config.get().getSection("Settings").getBoolean("Show-Addresses", false))?json.getSection("hosts").getSection(hosts.get(server)).getSection("servers").getSection(server).getString("address"):json.getSection("hosts").getSection(hosts.get(server)).getSection("servers").getSection(server).getString("address").split(":")[json.getSection("hosts").getSection(hosts.get(server)).getSection("servers").getSection(server).getString("address").split(":").length - 1]));
                        blockMeta.setLore(lore);
                    } else if (json.getSection("hosts").getSection(hosts.get(server)).getSection("servers").getSection(server).getBoolean("enabled") && json.getSection("hosts").getSection(hosts.get(server)).getSection("servers").getSection(server).getList("incompatible").size() == 0) {
                        block = createItem("STAINED_GLASS_PANE", offline.name(), offline.get());
                        blockMeta = block.getItemMeta();
                        blockMeta.setDisplayName(ChatColor.YELLOW + json.getSection("hosts").getSection(hosts.get(server)).getSection("servers").getSection(server).getString("display"));
                        LinkedList<String> lore = new LinkedList<String>();
                        if (!server.equals(json.getSection("hosts").getSection(hosts.get(server)).getSection("servers").getSection(server).getString("display")))
                            lore.add(ChatColor.GRAY + server);
                        lore.add(plugin.api.getLang("SubServers", "Interface.Server-Menu.SubServer-Offline"));
                        lore.add(ChatColor.WHITE + ((plugin.config.get().getSection("Settings").getBoolean("Show-Addresses", false))?json.getSection("hosts").getSection(hosts.get(server)).getSection("servers").getSection(server).getString("address"):json.getSection("hosts").getSection(hosts.get(server)).getSection("servers").getSection(server).getString("address").split(":")[json.getSection("hosts").getSection(hosts.get(server)).getSection("servers").getSection(server).getString("address").split(":").length - 1]));
                        blockMeta.setLore(lore);
                    } else {
                        block = createItem("STAINED_GLASS_PANE", disabled.name(), disabled.get());
                        blockMeta = block.getItemMeta();
                        blockMeta.setDisplayName(ChatColor.RED + json.getSection("hosts").getSection(hosts.get(server)).getSection("servers").getSection(server).getString("display"));
                        LinkedList<String> lore = new LinkedList<String>();
                        if (!server.equals(json.getSection("hosts").getSection(hosts.get(server)).getSection("servers").getSection(server).getString("display")))
                            lore.add(ChatColor.GRAY + server);
                        if (json.getSection("hosts").getSection(hosts.get(server)).getSection("servers").getSection(server).getList("incompatible").size() != 0) {
                            String list = "";
                            for (int ii = 0; ii < json.getSection("hosts").getSection(hosts.get(server)).getSection("servers").getSection(server).getList("incompatible").size(); ii++) {
                                if (list.length() != 0) list += ", ";
                                list += json.getSection("hosts").getSection(hosts.get(server)).getSection("servers").getSection(server).getList("incompatible").get(ii).asString();
                            }
                            lore.add(plugin.api.getLang("SubServers", "Interface.Server-Menu.SubServer-Incompatible").replace("$str$", list));
                        }
                        if (!json.getSection("hosts").getSection(hosts.get(server)).getSection("servers").getSection(server).getBoolean("enabled")) lore.add(plugin.api.getLang("SubServers", "Interface.Server-Menu.SubServer-Disabled"));
                        lore.add(ChatColor.WHITE + ((plugin.config.get().getSection("Settings").getBoolean("Show-Addresses", false))?json.getSection("hosts").getSection(hosts.get(server)).getSection("servers").getSection(server).getString("address"):json.getSection("hosts").getSection(hosts.get(server)).getSection("servers").getSection(server).getString("address").split(":")[json.getSection("hosts").getSection(hosts.get(server)).getSection("servers").getSection(server).getString("address").split(":").length - 1]));
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
        }));
    }

    public void subserverAdmin(final String subserver) {
        setDownloading(ChatColor.stripColor(plugin.api.getLang("SubServers", "Interface.SubServer-Admin.Title").replace("$str$", subserver)));
        plugin.subdata.sendPacket(new PacketDownloadServerInfo(subserver, json -> {
            windowHistory.add(() -> subserverAdmin(subserver));
            if (!json.getString("type").equals("subserver")) {
                if (hasHistory()) back();
            } else {
                setDownloading(null);
                lastVisitedObjects[0] = subserver;
                ItemStack block;
                ItemMeta blockMeta;
                ItemStack div = createItem("STAINED_GLASS_PANE", "BLACK_STAINED_GLASS_PANE", (short) 15);
                ItemMeta divMeta = div.getItemMeta();
                divMeta.setDisplayName(ChatColor.RESET.toString());
                div.setItemMeta(divMeta);

                Inventory inv = Bukkit.createInventory(null, 36, plugin.api.getLang("SubServers", "Interface.SubServer-Admin.Title").replace("$str$", json.getSection("server").getString("display")));

                int i = 0;
                while (i < inv.getSize()) {
                    inv.setItem(i, div);
                    i++;
                }
                i = 0;

                if (json.getSection("server").getBoolean("running")) {
                    if (!(Bukkit.getPlayer(player).hasPermission("subservers.subserver.terminate.*") || Bukkit.getPlayer(player).hasPermission("subservers.subserver.terminate." + subserver.toLowerCase()))) {
                        block = createItem("STAINED_GLASS_PANE", "GRAY_STAINED_GLASS_PANE", (short) 7);
                        blockMeta = block.getItemMeta();
                        blockMeta.setDisplayName(ChatColor.GRAY+ChatColor.stripColor(plugin.api.getLang("SubServers", "Interface.SubServer-Admin.Terminate")));
                        blockMeta.setLore(Arrays.asList(plugin.api.getLang("SubServers", "Interface.Generic.Invalid-Permission").replace("$str$", "subservers.subserver.terminate." + subserver.toLowerCase())));
                    } else {
                        block = createItem("STAINED_GLASS_PANE", "RED_STAINED_GLASS_PANE", (short) 14);
                        blockMeta = block.getItemMeta();
                        blockMeta.setDisplayName(plugin.api.getLang("SubServers", "Interface.SubServer-Admin.Terminate"));
                    }

                    block.setItemMeta(blockMeta);
                    inv.setItem(1, block);
                    inv.setItem(10, block);

                    if (!(Bukkit.getPlayer(player).hasPermission("subservers.subserver.stop.*") || Bukkit.getPlayer(player).hasPermission("subservers.subserver.stop." + subserver.toLowerCase()))) {
                        block = createItem("STAINED_GLASS_PANE", "GRAY_STAINED_GLASS_PANE", (short) 7);
                        blockMeta = block.getItemMeta();
                        blockMeta.setDisplayName(ChatColor.GRAY+ChatColor.stripColor(plugin.api.getLang("SubServers", "Interface.SubServer-Admin.Stop")));
                        blockMeta.setLore(Arrays.asList(plugin.api.getLang("SubServers", "Interface.Generic.Invalid-Permission").replace("$str$", "subservers.subserver.stop." + subserver.toLowerCase())));
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

                    if (!(Bukkit.getPlayer(player).hasPermission("subservers.subserver.command.*") || Bukkit.getPlayer(player).hasPermission("subservers.subserver.command." + subserver.toLowerCase()))) {
                        block = createItem("STAINED_GLASS_PANE", "GRAY_STAINED_GLASS_PANE", (short) 7);
                        blockMeta = block.getItemMeta();
                        blockMeta.setDisplayName(ChatColor.GRAY+ChatColor.stripColor(plugin.api.getLang("SubServers", "Interface.SubServer-Admin.Command")));
                        blockMeta.setLore(Arrays.asList(plugin.api.getLang("SubServers", "Interface.Generic.Invalid-Permission").replace("$str$", "subservers.subserver.command." + subserver.toLowerCase())));
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
                    if (!(Bukkit.getPlayer(player).hasPermission("subservers.subserver.start.*") || Bukkit.getPlayer(player).hasPermission("subservers.subserver.start." + subserver.toLowerCase()))) {
                        block = createItem("STAINED_GLASS_PANE", "GRAY_STAINED_GLASS_PANE", (short) 7);
                        blockMeta = block.getItemMeta();
                        blockMeta.setDisplayName(ChatColor.GRAY+ChatColor.stripColor(plugin.api.getLang("SubServers", "Interface.SubServer-Admin.Start")));
                        blockMeta.setLore(Arrays.asList(plugin.api.getLang("SubServers", "Interface.Generic.Invalid-Permission").replace("$str$", "subservers.subserver.start." + subserver.toLowerCase())));
                    } else if (!json.getSection("server").getBoolean("enabled") || json.getSection("server").getList("incompatible").size() != 0) {
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

                if (!json.getSection("server").getBoolean("enabled") || subserverPlugins.size() <= 0) {
                    block = div;
                } else {
                    block = createItem("STAINED_GLASS_PANE", "BLUE_STAINED_GLASS_PANE", (short) 11);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(plugin.api.getLang("SubServers", "Interface.SubServer-Admin.Plugins"));
                    block.setItemMeta(blockMeta);
                }
                inv.setItem(27, block);
                inv.setItem(28, block);

                if (json.getSection("server").getBoolean("temp")) {
                    block = createItem("STAINED_GLASS_PANE", "BLUE_STAINED_GLASS_PANE", (short) 11);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(ChatColor.AQUA + json.getSection("server").getString("display"));
                    LinkedList<String> lore = new LinkedList<String>();
                    if (!subserver.equals(json.getSection("server").getString("display")))
                        lore.add(ChatColor.GRAY + subserver);
                    lore.add(plugin.api.getLang("SubServers", "Interface.Server-Menu.Server-Player-Count").replace("$int$", new DecimalFormat("#,###").format(json.getSection("server").getSection("players").getKeys().size())));
                    lore.add(plugin.api.getLang("SubServers", "Interface.Server-Menu.SubServer-Temporary"));
                    lore.add(ChatColor.WHITE + ((plugin.config.get().getSection("Settings").getBoolean("Show-Addresses", false))?json.getSection("server").getString("address"):json.getSection("server").getString("address").split(":")[json.getSection("server").getString("address").split(":").length - 1]));
                    blockMeta.setLore(lore);
                } else if (json.getSection("server").getBoolean("running")) {
                    block = createItem("STAINED_GLASS_PANE", "LIME_STAINED_GLASS_PANE", (short) 5);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(ChatColor.GREEN + json.getSection("server").getString("display"));
                    LinkedList<String> lore = new LinkedList<String>();
                    if (!subserver.equals(json.getSection("server").getString("display")))
                        lore.add(ChatColor.GRAY + subserver);
                    lore.add(plugin.api.getLang("SubServers", "Interface.Server-Menu.Server-Player-Count").replace("$int$", new DecimalFormat("#,###").format(json.getSection("server").getSection("players").getKeys().size())));
                    lore.add(ChatColor.WHITE + ((plugin.config.get().getSection("Settings").getBoolean("Show-Addresses", false))?json.getSection("server").getString("address"):json.getSection("server").getString("address").split(":")[json.getSection("server").getString("address").split(":").length - 1]));
                    blockMeta.setLore(lore);
                } else if (json.getSection("server").getBoolean("enabled") && json.getSection("server").getList("incompatible").size() == 0) {
                    block = createItem("STAINED_GLASS_PANE", "YELLOW_STAINED_GLASS_PANE", (short) 4);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(ChatColor.YELLOW + json.getSection("server").getString("display"));
                    LinkedList<String> lore = new LinkedList<String>();
                    if (!subserver.equals(json.getSection("server").getString("display")))
                        lore.add(ChatColor.GRAY + subserver);
                    lore.add(plugin.api.getLang("SubServers", "Interface.Server-Menu.SubServer-Offline"));
                    lore.add(ChatColor.WHITE + ((plugin.config.get().getSection("Settings").getBoolean("Show-Addresses", false))?json.getSection("server").getString("address"):json.getSection("server").getString("address").split(":")[json.getSection("server").getString("address").split(":").length - 1]));
                    blockMeta.setLore(lore);
                } else {
                    block = createItem("STAINED_GLASS_PANE", "RED_STAINED_GLASS_PANE", (short) 14);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(ChatColor.RED + json.getSection("server").getString("display"));
                    LinkedList<String> lore = new LinkedList<String>();
                    if (!subserver.equals(json.getSection("server").getString("display")))
                        lore.add(ChatColor.GRAY + subserver);
                    if (json.getSection("server").getList("incompatible").size() != 0) {
                        String list = "";
                        for (int ii = 0; ii < json.getSection("server").getList("incompatible").size(); ii++) {
                            if (list.length() != 0) list += ", ";
                            list += json.getSection("server").getList("incompatible").get(ii).asString();
                        }
                        lore.add(plugin.api.getLang("SubServers", "Interface.Server-Menu.SubServer-Incompatible").replace("$str$", list));
                    }
                    if (!json.getSection("server").getBoolean("enabled")) lore.add(plugin.api.getLang("SubServers", "Interface.Server-Menu.SubServer-Disabled"));
                    lore.add(ChatColor.WHITE + ((plugin.config.get().getSection("Settings").getBoolean("Show-Addresses", false))?json.getSection("server").getString("address"):json.getSection("server").getString("address").split(":")[json.getSection("server").getString("address").split(":").length - 1]));
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
        }));

    }

    public void subserverPlugin(final int page, final String subserver) {
        setDownloading(ChatColor.stripColor(plugin.api.getLang("SubServers", "Interface.SubServer-Plugin.Title").replace("$str$", subserver)));
        plugin.subdata.sendPacket(new PacketDownloadServerInfo(subserver, json -> {
            windowHistory.add(() -> subserverPlugin(page, subserver));
            if (!json.getString("type").equals("subserver")) {
                if (hasHistory()) back();
            } else {
                setDownloading(null);
                lastVisitedObjects[0] = subserver;
                lastPage = page;
                List<String> renderers = new ArrayList<String>();
                for (String renderer : renderers) {
                    if (subserverPlugins.get(renderer).isEnabled(json.getSection("server"))) renderers.add(renderer);
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

                Inventory inv = Bukkit.createInventory(null, 18 + area, plugin.api.getLang("SubServers", "Interface.SubServer-Plugin.Title").replace("$str$", json.getSection("server").getString("display")));
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
        }));
    }
}
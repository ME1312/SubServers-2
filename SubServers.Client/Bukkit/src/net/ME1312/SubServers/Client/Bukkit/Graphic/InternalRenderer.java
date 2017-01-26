package net.ME1312.SubServers.Client.Bukkit.Graphic;

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
public class InternalRenderer extends UIRenderer {
    private List<Runnable> windowHistory = new LinkedList<Runnable>();
    protected Options lastUsedOptions = null;
    protected String lastVistedObject = null;
    protected int lastPage = 1;
    protected Runnable lastMenu = null;
    protected boolean open = false;
    protected final UUID player;
    private SubPlugin plugin;

    protected InternalRenderer(SubPlugin plugin, UUID player) {
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

    public void hostMenu(final int page) {
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
                        blockMeta.setDisplayName(ChatColor.AQUA + json.getJSONObject("hosts").getJSONObject(host).getString("display"));
                        LinkedList<String> lore = new LinkedList<String>();
                        if (!host.equals(json.getJSONObject("hosts").getJSONObject(host).getString("display")))
                            lore.add(ChatColor.GRAY + host);
                        lore.add(plugin.lang.getSection("Lang").getColoredString("Interface.Host-Menu.Host-Server-Count", '&').replace("$int$", new DecimalFormat("#,###").format(json.getJSONObject("hosts").getJSONObject(host).getJSONObject("servers").keySet().size())));
                        blockMeta.setLore(lore);
                    } else {
                        block = new ItemStack(Material.STAINED_GLASS_PANE, 1, disabled);
                        blockMeta = block.getItemMeta();
                        blockMeta.setDisplayName(ChatColor.RED + json.getJSONObject("hosts").getJSONObject(host).getString("display"));
                        LinkedList<String> lore = new LinkedList<String>();
                        if (!host.equals(json.getJSONObject("hosts").getJSONObject(host).getString("display")))
                            lore.add(ChatColor.GRAY + host);
                        lore.add(plugin.lang.getSection("Lang").getColoredString("Interface.Host-Menu.Host-Disabled", '&'));
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

    public void hostAdmin(final String host) {
        setDownloading(ChatColor.stripColor(plugin.lang.getSection("Lang").getColoredString("Interface.Host-Admin.Title", '&').replace("$str$", host)));
        plugin.subdata.sendPacket(new PacketDownloadHostInfo(host, UUID.randomUUID().toString(), (json) -> {
            windowHistory.add(() -> hostAdmin(host));
            if (!json.getBoolean("valid")) {
                if (hasHistory()) back();
            } else {
                setDownloading(null);
                lastVistedObject = host;

                ItemStack block;
                ItemMeta blockMeta;
                ItemStack div = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 15);
                ItemMeta divMeta = div.getItemMeta();
                divMeta.setDisplayName(ChatColor.RESET.toString());
                div.setItemMeta(divMeta);

                Inventory inv = Bukkit.createInventory(null, 36, plugin.lang.getSection("Lang").getColoredString("Interface.Host-Admin.Title", '&').replace("$str$", json.getJSONObject("host").getString("display")));

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
                inv.setItem(2, block);
                inv.setItem(3, block);
                inv.setItem(10, block);
                inv.setItem(11, block);
                inv.setItem(12, block);

                block = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 5);
                blockMeta = block.getItemMeta();
                blockMeta.setDisplayName(plugin.lang.getSection("Lang").getColoredString("Interface.Host-Admin.SubServers", '&'));
                block.setItemMeta(blockMeta);
                inv.setItem(5, block);
                inv.setItem(6, block);
                inv.setItem(7, block);
                inv.setItem(14, block);
                inv.setItem(15, block);
                inv.setItem(16, block);

                if (!json.getJSONObject("host").getBoolean("enabled")) {
                    block = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 7);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(ChatColor.GRAY+ChatColor.stripColor(plugin.lang.getSection("Lang").getColoredString("Interface.Host-Admin.Plugins", '&')));
                } else {
                    block = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 11);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(plugin.lang.getSection("Lang").getColoredString("Interface.Host-Admin.Plugins", '&'));
                }
                inv.setItem(27, block);
                inv.setItem(28, block);

                if (json.getJSONObject("host").getBoolean("enabled")) {
                    block = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 11);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(ChatColor.AQUA + json.getJSONObject("host").getString("display"));
                    LinkedList<String> lore = new LinkedList<String>();
                    if (!host.equals(json.getJSONObject("host").getString("display")))
                        lore.add(ChatColor.GRAY + host);
                    lore.add(plugin.lang.getSection("Lang").getColoredString("Interface.Host-Menu.Host-Server-Count", '&').replace("$int$", new DecimalFormat("#,###").format(json.getJSONObject("host").getJSONObject("servers").keySet().size())));
                    blockMeta.setLore(lore);
                } else {
                    block = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 14);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(ChatColor.RED + json.getJSONObject("host").getString("display"));
                    LinkedList<String> lore = new LinkedList<String>();
                    if (!host.equals(json.getJSONObject("host").getString("display")))
                        lore.add(ChatColor.GRAY + host);
                    lore.add(plugin.lang.getSection("Lang").getColoredString("Interface.Host-Menu.Host-Disabled", '&'));
                    blockMeta.setLore(lore);
                }
                block.setItemMeta(blockMeta);
                inv.setItem(30, block);
                inv.setItem(31, block);
                inv.setItem(32, block);


                if (hasHistory()) {
                    block = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 14);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(plugin.lang.getSection("Lang").getColoredString("Interface.Generic.Back", '&'));
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
        setDownloading(ChatColor.stripColor(plugin.lang.getSection("Lang").getColoredString("Interface.Host-Creator.Title", '&').replace("$str$", options.getHost())));
        lastUsedOptions = options;
        if (!options.init()) {
            windowHistory.add(() -> hostCreator(options));
            lastVistedObject = options.getHost();
        }

        plugin.subdata.sendPacket(new PacketDownloadHostInfo(options.getHost(), UUID.randomUUID().toString(), json -> {
            if (!json.getBoolean("valid")|| !json.getJSONObject("host").getBoolean("enabled") || json.getJSONObject("host").getJSONObject("creator").getBoolean("busy")) {
                lastUsedOptions = null;
                if (hasHistory()) back();
            } else {
                setDownloading(null);
                ItemStack block;
                ItemMeta blockMeta;
                ItemStack div = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 15);
                ItemMeta divMeta = div.getItemMeta();
                divMeta.setDisplayName(ChatColor.RESET.toString());
                div.setItemMeta(divMeta);

                Inventory inv = Bukkit.createInventory(null, 54, plugin.lang.getSection("Lang").getColoredString("Interface.Host-Creator.Title", '&').replace("$str$", json.getJSONObject("host").getString("display")));

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

                if (hasHistory()) {
                    block = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 14);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(plugin.lang.getSection("Lang").getColoredString("Interface.Generic.Back", '&'));
                    block.setItemMeta(blockMeta);
                    inv.setItem(52, block);
                    inv.setItem(53, block);
                }

                Bukkit.getPlayer(player).openInventory(inv);
                open = true;
            }
        }));
    }

    public void hostPlugin(final int page, final String host) {
        setDownloading(ChatColor.stripColor(plugin.lang.getSection("Lang").getColoredString("Interface.Host-Plugin.Title", '&').replace("$str$", host)));
        plugin.subdata.sendPacket(new PacketDownloadHostInfo(host, UUID.randomUUID().toString(), (json) -> {
            windowHistory.add(() -> hostPlugin(page, host));
            if (!json.getBoolean("valid")) {
                if (hasHistory()) back();
            } else {
                setDownloading(null);
                lastVistedObject = host;
                lastPage = page;
                List<String> renderers = new ArrayList<String>();
                for (String renderer : renderers) {
                    if (subserverPlugins.get(renderer).isEnabled(json.getJSONObject("host"))) renderers.add(renderer);
                }
                Collections.sort(renderers);

                ItemStack block;
                ItemMeta blockMeta;
                ItemStack div = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 15);
                ItemMeta divMeta = div.getItemMeta();
                divMeta.setDisplayName(ChatColor.RESET.toString());
                div.setItemMeta(divMeta);

                int i = 0;
                int min = ((page - 1) * 36);
                int max = (min + 35);
                int count = (renderers.size() == 0)?27:((renderers.size() - min - 1 >= max)?36:renderers.size() - min);
                int area = (count % 9 == 0) ? count : (int) (Math.floor(count / 9) + 1) * 9;

                Inventory inv = Bukkit.createInventory(null, 18 + area, plugin.lang.getSection("Lang").getColoredString("Interface.Host-Plugin.Title", '&').replace("$str$", json.getJSONObject("host").getString("display")));
                block = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 7);
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
                    block = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 14);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(plugin.lang.getSection("Lang").getColoredString("Interface.Host-Plugin.No-Plugins", '&'));
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
                if (hasHistory()) {
                    block = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 14);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(plugin.lang.getSection("Lang").getColoredString("Interface.Generic.Back", '&'));
                    block.setItemMeta(blockMeta);
                    inv.setItem(i++, block);
                    inv.setItem(i++, block);
                    inv.setItem(i++, block);
                    i++;
                }
                if (renderers.size() - 1 > max) {
                    block = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 4);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(plugin.lang.getSection("Lang").getColoredString("Interface.Generic.Next-Arrow", '&'));
                    block.setItemMeta(blockMeta);
                    inv.setItem(i++, block);
                    inv.setItem(i, block);
                }

                Bukkit.getPlayer(player).openInventory(inv);
                open = true;
            }
        }));
    }

    public void subserverMenu(final int page, final String host) {
        setDownloading(ChatColor.stripColor((host == null)?plugin.lang.getSection("Lang").getColoredString("Interface.SubServer-Menu.Title", '&'):plugin.lang.getSection("Lang").getColoredString("Interface.Host-SubServer.Title", '&').replace("$str$", host)));
        plugin.subdata.sendPacket(new PacketDownloadServerList(host, UUID.randomUUID().toString(), json -> {
            setDownloading(null);
            lastPage = page;

            HashMap<String, String> hosts = new HashMap<String, String>();
            List<String> subservers = new ArrayList<String>();
            if (host != null && json.getJSONObject("hosts").keySet().contains(host)) {
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
            Collections.sort(subservers);
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

            Inventory inv = Bukkit.createInventory(null, 18 + area, (host == null)?plugin.lang.getSection("Lang").getColoredString("Interface.SubServer-Menu.Title", '&'):plugin.lang.getSection("Lang").getColoredString("Interface.Host-SubServer.Title", '&').replace("$str$", json.getJSONObject("hosts").getJSONObject(host).getString("display")));
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
                        blockMeta.setDisplayName(ChatColor.AQUA + json.getJSONObject("hosts").getJSONObject(hosts.get(subserver)).getJSONObject("servers").getJSONObject(subserver).getString("display"));
                        LinkedList<String> lore = new LinkedList<String>();
                        if (!subserver.equals(json.getJSONObject("hosts").getJSONObject(hosts.get(subserver)).getJSONObject("servers").getJSONObject(subserver).getString("display")))
                            lore.add(ChatColor.GRAY + subserver);
                        lore.add(plugin.lang.getSection("Lang").getColoredString("Interface.SubServer-Menu.SubServer-Player-Count", '&').replace("$int$", new DecimalFormat("#,###").format(json.getJSONObject("hosts").getJSONObject(hosts.get(subserver)).getJSONObject("servers").getJSONObject(subserver).getJSONObject("players").keySet().size())));
                        lore.add(plugin.lang.getSection("Lang").getColoredString("Interface.SubServer-Menu.SubServer-Temporary", '&'));
                        blockMeta.setLore(lore);
                    } else if (json.getJSONObject("hosts").getJSONObject(hosts.get(subserver)).getJSONObject("servers").getJSONObject(subserver).getBoolean("running")) {
                        block = new ItemStack(Material.STAINED_GLASS_PANE, 1, online);
                        blockMeta = block.getItemMeta();
                        blockMeta.setDisplayName(ChatColor.GREEN + json.getJSONObject("hosts").getJSONObject(hosts.get(subserver)).getJSONObject("servers").getJSONObject(subserver).getString("display"));
                        LinkedList<String> lore = new LinkedList<String>();
                        if (!subserver.equals(json.getJSONObject("hosts").getJSONObject(hosts.get(subserver)).getJSONObject("servers").getJSONObject(subserver).getString("display")))
                            lore.add(ChatColor.GRAY + subserver);
                        lore.add(plugin.lang.getSection("Lang").getColoredString("Interface.SubServer-Menu.SubServer-Player-Count", '&').replace("$int$", new DecimalFormat("#,###").format(json.getJSONObject("hosts").getJSONObject(hosts.get(subserver)).getJSONObject("servers").getJSONObject(subserver).getJSONObject("players").keySet().size())));
                        blockMeta.setLore(lore);
                    } else if (json.getJSONObject("hosts").getJSONObject(hosts.get(subserver)).getJSONObject("servers").getJSONObject(subserver).getBoolean("enabled")) {
                        block = new ItemStack(Material.STAINED_GLASS_PANE, 1, offline);
                        blockMeta = block.getItemMeta();
                        blockMeta.setDisplayName(ChatColor.YELLOW + json.getJSONObject("hosts").getJSONObject(hosts.get(subserver)).getJSONObject("servers").getJSONObject(subserver).getString("display"));
                        LinkedList<String> lore = new LinkedList<String>();
                        if (!subserver.equals(json.getJSONObject("hosts").getJSONObject(hosts.get(subserver)).getJSONObject("servers").getJSONObject(subserver).getString("display")))
                            lore.add(ChatColor.GRAY + subserver);
                        lore.add(plugin.lang.getSection("Lang").getColoredString("Interface.SubServer-Menu.SubServer-Offline", '&'));
                        blockMeta.setLore(lore);
                    } else {
                        block = new ItemStack(Material.STAINED_GLASS_PANE, 1, disabled);
                        blockMeta = block.getItemMeta();
                        blockMeta.setDisplayName(ChatColor.RED + json.getJSONObject("hosts").getJSONObject(hosts.get(subserver)).getJSONObject("servers").getJSONObject(subserver).getString("display"));
                        LinkedList<String> lore = new LinkedList<String>();
                        if (!subserver.equals(json.getJSONObject("hosts").getJSONObject(hosts.get(subserver)).getJSONObject("servers").getJSONObject(subserver).getString("display")))
                            lore.add(ChatColor.GRAY + subserver);
                        lore.add(plugin.lang.getSection("Lang").getColoredString("Interface.SubServer-Menu.SubServer-Disabled", '&'));
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
            if (host == null || hasHistory()) {
                block = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) ((host == null) ? 11 : 14));
                blockMeta = block.getItemMeta();
                blockMeta.setDisplayName((host == null) ? plugin.lang.getSection("Lang").getColoredString("Interface.SubServer-Menu.Host-Menu", '&') : plugin.lang.getSection("Lang").getColoredString("Interface.Generic.Back", '&'));
                block.setItemMeta(blockMeta);
                inv.setItem(i++, block);
                inv.setItem(i++, block);
                inv.setItem(i++, block);
                i++;
            }
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

    public void subserverAdmin(final String subserver) {
        setDownloading(ChatColor.stripColor(plugin.lang.getSection("Lang").getColoredString("Interface.SubServer-Admin.Title", '&').replace("$str$", subserver)));
        plugin.subdata.sendPacket(new PacketDownloadServerInfo(subserver, UUID.randomUUID().toString(), json -> {
            windowHistory.add(() -> subserverAdmin(subserver));
            if (!json.getString("type").equals("subserver")) {
                if (hasHistory()) back();
            } else {
                setDownloading(null);
                lastVistedObject = subserver;
                ItemStack block;
                ItemMeta blockMeta;
                ItemStack div = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 15);
                ItemMeta divMeta = div.getItemMeta();
                divMeta.setDisplayName(ChatColor.RESET.toString());
                div.setItemMeta(divMeta);

                Inventory inv = Bukkit.createInventory(null, 36, plugin.lang.getSection("Lang").getColoredString("Interface.SubServer-Admin.Title", '&').replace("$str$", json.getJSONObject("server").getString("display")));

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
                    inv.setItem(3, block);
                    inv.setItem(4, block);
                    inv.setItem(5, block);
                    inv.setItem(12, block);
                    inv.setItem(13, block);
                    inv.setItem(14, block);
                }

                if (!json.getJSONObject("server").getBoolean("enabled")) {
                    block = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 7);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(ChatColor.GRAY+ChatColor.stripColor(plugin.lang.getSection("Lang").getColoredString("Interface.SubServer-Admin.Plugins", '&')));
                } else {
                    block = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 11);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(plugin.lang.getSection("Lang").getColoredString("Interface.SubServer-Admin.Plugins", '&'));
                }
                block.setItemMeta(blockMeta);
                inv.setItem(27, block);
                inv.setItem(28, block);

                if (json.getJSONObject("server").getBoolean("temp")) {
                    block = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 11);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(ChatColor.AQUA + json.getJSONObject("server").getString("display"));
                    LinkedList<String> lore = new LinkedList<String>();
                    if (!subserver.equals(json.getJSONObject("server").getString("display")))
                        lore.add(ChatColor.GRAY + subserver);
                    lore.add(plugin.lang.getSection("Lang").getColoredString("Interface.SubServer-Menu.SubServer-Player-Count", '&').replace("$int$", new DecimalFormat("#,###").format(json.getJSONObject("server").getJSONObject("players").keySet().size())));
                    lore.add(plugin.lang.getSection("Lang").getColoredString("Interface.SubServer-Menu.SubServer-Temporary", '&'));
                    blockMeta.setLore(lore);
                } else if (json.getJSONObject("server").getBoolean("running")) {
                    block = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 5);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(ChatColor.GREEN + json.getJSONObject("server").getString("display"));
                    LinkedList<String> lore = new LinkedList<String>();
                    if (!subserver.equals(json.getJSONObject("server").getString("display")))
                        lore.add(ChatColor.GRAY + subserver);
                    lore.add(plugin.lang.getSection("Lang").getColoredString("Interface.SubServer-Menu.SubServer-Player-Count", '&').replace("$int$", new DecimalFormat("#,###").format(json.getJSONObject("server").getJSONObject("players").keySet().size())));
                    blockMeta.setLore(lore);
                } else if (json.getJSONObject("server").getBoolean("enabled")) {
                    block = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 4);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(ChatColor.YELLOW + json.getJSONObject("server").getString("display"));
                    LinkedList<String> lore = new LinkedList<String>();
                    if (!subserver.equals(json.getJSONObject("server").getString("display")))
                        lore.add(ChatColor.GRAY + subserver);
                    lore.add(plugin.lang.getSection("Lang").getColoredString("Interface.SubServer-Menu.SubServer-Offline", '&'));
                    blockMeta.setLore(lore);
                } else {
                    block = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 14);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(ChatColor.RED + json.getJSONObject("server").getString("display"));
                    LinkedList<String> lore = new LinkedList<String>();
                    if (!subserver.equals(json.getJSONObject("server").getString("display")))
                        lore.add(ChatColor.GRAY + subserver);
                    lore.add(plugin.lang.getSection("Lang").getColoredString("Interface.SubServer-Menu.SubServer-Disabled", '&'));
                    blockMeta.setLore(lore);
                }
                block.setItemMeta(blockMeta);
                inv.setItem(30, block);
                inv.setItem(31, block);
                inv.setItem(32, block);

                if (hasHistory()) {
                    block = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 14);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(plugin.lang.getSection("Lang").getColoredString("Interface.Generic.Back", '&'));
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
        setDownloading(ChatColor.stripColor(plugin.lang.getSection("Lang").getColoredString("Interface.SubServer-Plugin.Title", '&').replace("$str$", subserver)));
        plugin.subdata.sendPacket(new PacketDownloadServerInfo(subserver, UUID.randomUUID().toString(), json -> {
            windowHistory.add(() -> subserverPlugin(page, subserver));
            if (!json.getString("type").equals("subserver")) {
                if (hasHistory()) back();
            } else {
                setDownloading(null);
                lastVistedObject = subserver;
                lastPage = page;
                List<String> renderers = new ArrayList<String>();
                for (String renderer : renderers) {
                    if (subserverPlugins.get(renderer).isEnabled(json.getJSONObject("server"))) renderers.add(renderer);
                }
                Collections.sort(renderers);

                ItemStack block;
                ItemMeta blockMeta;
                ItemStack div = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 15);
                ItemMeta divMeta = div.getItemMeta();
                divMeta.setDisplayName(ChatColor.RESET.toString());
                div.setItemMeta(divMeta);

                int i = 0;
                int min = ((page - 1) * 36);
                int max = (min + 35);
                int count = (renderers.size() == 0)?27:((renderers.size() - min - 1 >= max)?36:renderers.size() - min);
                int area = (count % 9 == 0) ? count : (int) (Math.floor(count / 9) + 1) * 9;

                Inventory inv = Bukkit.createInventory(null, 18 + area, plugin.lang.getSection("Lang").getColoredString("Interface.SubServer-Plugin.Title", '&').replace("$str$", json.getJSONObject("server").getString("display")));
                block = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 7);
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
                    block = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 14);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(plugin.lang.getSection("Lang").getColoredString("Interface.SubServer-Plugin.No-Plugins", '&'));
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
                if (hasHistory()) {
                    block = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 14);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(plugin.lang.getSection("Lang").getColoredString("Interface.Generic.Back", '&'));
                    block.setItemMeta(blockMeta);
                    inv.setItem(i++, block);
                    inv.setItem(i++, block);
                    inv.setItem(i++, block);
                    i++;
                }
                if (renderers.size() - 1 > max) {
                    block = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 4);
                    blockMeta = block.getItemMeta();
                    blockMeta.setDisplayName(plugin.lang.getSection("Lang").getColoredString("Interface.Generic.Next-Arrow", '&'));
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
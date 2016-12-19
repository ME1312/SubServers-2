package net.ME1312.SubServers.Client.Bukkit.Graphic;

import net.ME1312.SubServers.Client.Bukkit.Library.Container;
import net.ME1312.SubServers.Client.Bukkit.Library.JSONCallback;
import net.ME1312.SubServers.Client.Bukkit.Library.Util;
import net.ME1312.SubServers.Client.Bukkit.Library.Version.Version;
import net.ME1312.SubServers.Client.Bukkit.Network.Packet.*;
import net.ME1312.SubServers.Client.Bukkit.Network.SubDataClient;
import net.ME1312.SubServers.Client.Bukkit.SubPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.UUID;

public class UIListener implements Listener {
    private HashMap<UUID, JSONCallback> input = new HashMap<UUID, JSONCallback>();
    private HashMap<UUID, UIRenderer> gui = new HashMap<UUID, UIRenderer>();
    private SubPlugin plugin;

    public UIListener(SubPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Grabs the current UIRenderer for the player
     *
     * @param player Player
     * @return UIRenderer
     */
    public UIRenderer getRenderer(Player player) {
        if (!gui.keySet().contains(player.getUniqueId())) gui.put(player.getUniqueId(), new UIRenderer(plugin, player.getUniqueId()));
        return gui.get(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void click(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        if (!event.isCancelled() && gui.keySet().contains(player.getUniqueId())) {
            UIRenderer gui = this.gui.get(player.getUniqueId());
            if (gui.open && event.getClickedInventory() != null && event.getClickedInventory().getTitle() != null) {
                if (event.getClickedInventory().getTitle().equals(plugin.lang.getSection("Lang").getColoredString("Interface.Host-Menu.Title", '&'))) { // Host Menu
                    if (event.getCurrentItem() != null && event.getCurrentItem().getType() != Material.AIR && event.getCurrentItem().hasItemMeta()) {
                        String item = event.getCurrentItem().getItemMeta().getDisplayName();

                        if (item.equals(plugin.lang.getSection("Lang").getColoredString("Interface.Generic.Back-Arrow", '&'))) {
                            player.closeInventory();
                            gui.hostMenu(gui.lastPage - 1);
                        } else if (item.equals(plugin.lang.getSection("Lang").getColoredString("Interface.Generic.Next-Arrow", '&'))) {
                            player.closeInventory();
                            gui.hostMenu(gui.lastPage + 1);
                        } else if (item.equals(plugin.lang.getSection("Lang").getColoredString("Interface.Host-Menu.SubServer-Menu", '&'))) {
                            player.closeInventory();
                            gui.subserverMenu(1, null);
                        } else if (!item.equals(ChatColor.RESET.toString()) && !item.equals(plugin.lang.getSection("Lang").getColoredString("Interface.Host-Menu.No-Hosts", '&'))) {
                            player.closeInventory();
                            gui.hostAdmin(ChatColor.stripColor(item));
                        }
                    }
                } else if (event.getClickedInventory().getTitle().startsWith(plugin.lang.getSection("Lang").getColoredString("Interface.Host-Creator.Title", '&').split("\\$str\\$")[0]) && // Host Creator
                        (plugin.lang.getSection("Lang").getColoredString("Interface.Host-Creator.Title", '&').split("\\$str\\$").length == 1 ||
                                event.getClickedInventory().getTitle().endsWith(plugin.lang.getSection("Lang").getColoredString("Interface.Host-Creator.Title", '&').split("\\$str\\$")[1]))) {
                    if (event.getCurrentItem() != null && event.getCurrentItem().getType() != Material.AIR && event.getCurrentItem().hasItemMeta()) {
                        String item = event.getCurrentItem().getItemMeta().getDisplayName();
                        if (item.equals(plugin.lang.getSection("Lang").getColoredString("Interface.Generic.Back", '&'))) {
                            player.closeInventory();
                            gui.back();
                        } else if (item.equals(plugin.lang.getSection("Lang").getColoredString("Interface.Generic.Undo", '&'))) {
                            player.closeInventory();
                            gui.lastUsedOptions.undo();
                            gui.hostCreator((UIRenderer.CreatorOptions) gui.lastUsedOptions);
                        } else if (item.equals(plugin.lang.getSection("Lang").getColoredString("Interface.Host-Creator.Submit", '&'))) {
                            if (player.hasPermission("subservers.host.create.*") || player.hasPermission("subservers.host.create." + gui.lastVistedObject.toLowerCase())) {
                                player.closeInventory();
                                gui.setDownloading(plugin.lang.getSection("Lang").getColoredString("Interface.Generic.Downloading.Response", '&'));
                                plugin.subdata.sendPacket(new PacketCreateServer(player.getUniqueId(), (UIRenderer.CreatorOptions) gui.lastUsedOptions, UUID.randomUUID().toString(), json -> {
                                    gui.back();
                                }));
                            } else {
                                gui.back();
                            }
                        } else if (ChatColor.stripColor(item).equals(ChatColor.stripColor(plugin.lang.getSection("Lang").getColoredString("Interface.Host-Creator.Edit-Name", '&')))) {
                            player.closeInventory();
                            if (!gui.sendTitle(plugin.lang.getSection("Lang").getColoredString("Interface.Host-Creator.Edit-Name.Title", '&'), 4 * 20))
                                player.sendMessage(plugin.lang.getSection("Lang").getColoredString("Interface.Host-Creator.Edit-Name.Message", '&'));
                            input.put(player.getUniqueId(), m -> {
                                if (m.getString("message").contains(" ")) {
                                    if (!gui.sendTitle(plugin.lang.getSection("Lang").getColoredString("Interface.Host-Creator.Edit-Name.Invalid-Title", '&'), 4 * 20))
                                        player.sendMessage(plugin.lang.getSection("Lang").getColoredString("Interface.Host-Creator.Edit-Name.Invalid", '&'));
                                    Bukkit.getScheduler().runTaskLater(plugin, () -> gui.hostCreator((UIRenderer.CreatorOptions) gui.lastUsedOptions), 4 * 20);
                                } else {
                                    gui.setDownloading(plugin.lang.getSection("Lang").getColoredString("Interface.Generic.Downloading.Response", '&'));
                                    plugin.subdata.sendPacket(new PacketDownloadServerList(null, UUID.randomUUID().toString(), json -> {
                                        boolean match = false;
                                        for (String tmphost : json.getJSONObject("hosts").keySet()) {
                                            for (String tmpsubserver : json.getJSONObject("hosts").getJSONObject(tmphost).getJSONObject("servers").keySet()) {
                                                if (tmpsubserver.equalsIgnoreCase(m.getString("message"))) match = true;
                                            }
                                        }
                                        if (match) {
                                            gui.setDownloading(null);
                                            if (!gui.sendTitle(plugin.lang.getSection("Lang").getColoredString("Interface.Host-Creator.Edit-Name.Exists-Title", '&'), 4 * 20))
                                                player.sendMessage(plugin.lang.getSection("Lang").getColoredString("Interface.Host-Creator.Edit-Name.Exists", '&'));
                                            Bukkit.getScheduler().runTaskLater(plugin, () -> gui.hostCreator((UIRenderer.CreatorOptions) gui.lastUsedOptions), 4 * 20);
                                        } else {
                                            ((UIRenderer.CreatorOptions) gui.lastUsedOptions).setName(m.getString("message"));
                                            gui.hostCreator((UIRenderer.CreatorOptions) gui.lastUsedOptions);
                                        }
                                    }));
                                }
                            });
                        } else if (ChatColor.stripColor(item).equals(ChatColor.stripColor(plugin.lang.getSection("Lang").getColoredString("Interface.Host-Creator.Edit-Type", '&')))) {
                            player.closeInventory();
                            if (!gui.sendTitle(plugin.lang.getSection("Lang").getColoredString("Interface.Host-Creator.Edit-Type.Title", '&'), 4 * 20))
                                player.sendMessage(plugin.lang.getSection("Lang").getColoredString("Interface.Host-Creator.Edit-Type.Message", '&'));
                            input.put(player.getUniqueId(), m -> {
                                if (Util.isException(() -> PacketCreateServer.ServerType.valueOf(m.getString("message").toUpperCase()))) {
                                    if (!gui.sendTitle(plugin.lang.getSection("Lang").getColoredString("Interface.Host-Creator.Edit-Type.Invalid-Title", '&'), 4 * 20))
                                        player.sendMessage(plugin.lang.getSection("Lang").getColoredString("Interface.Host-Creator.Edit-Type.Invalid", '&'));
                                    Bukkit.getScheduler().runTaskLater(plugin, () -> gui.hostCreator((UIRenderer.CreatorOptions) gui.lastUsedOptions), 4 * 20);
                                } else {
                                    ((UIRenderer.CreatorOptions) gui.lastUsedOptions).setType(PacketCreateServer.ServerType.valueOf(m.getString("message").toUpperCase()));
                                    gui.hostCreator((UIRenderer.CreatorOptions) gui.lastUsedOptions);
                                }
                            });
                        } else if (ChatColor.stripColor(item).equals(ChatColor.stripColor(plugin.lang.getSection("Lang").getColoredString("Interface.Host-Creator.Edit-Version", '&')))) {
                            player.closeInventory();
                            if (!gui.sendTitle(plugin.lang.getSection("Lang").getColoredString("Interface.Host-Creator.Edit-Version.Title", '&'), 4 * 20))
                                player.sendMessage(plugin.lang.getSection("Lang").getColoredString("Interface.Host-Creator.Edit-Version.Message", '&'));
                            input.put(player.getUniqueId(), m -> {
                                if (new Version("1.8").compareTo(new Version(m.getString("message"))) > 0) {
                                    if (!gui.sendTitle(plugin.lang.getSection("Lang").getColoredString("Interface.Host-Creator.Edit-Version.Unavailable-Title", '&'), 4 * 20))
                                        player.sendMessage(plugin.lang.getSection("Lang").getColoredString("Interface.Host-Creator.Edit-Version.Unavailable", '&'));
                                    Bukkit.getScheduler().runTaskLater(plugin, () -> gui.hostCreator((UIRenderer.CreatorOptions) gui.lastUsedOptions), 4 * 20);
                                } else {
                                    ((UIRenderer.CreatorOptions) gui.lastUsedOptions).setVersion(new Version(m.getString("message")));
                                    gui.hostCreator((UIRenderer.CreatorOptions) gui.lastUsedOptions);
                                }
                            });
                        } else if (ChatColor.stripColor(item).equals(ChatColor.stripColor(plugin.lang.getSection("Lang").getColoredString("Interface.Host-Creator.Edit-Port", '&')))) {
                            player.closeInventory();
                            if (!gui.sendTitle(plugin.lang.getSection("Lang").getColoredString("Interface.Host-Creator.Edit-Port.Title", '&'), 4 * 20))
                                player.sendMessage(plugin.lang.getSection("Lang").getColoredString("Interface.Host-Creator.Edit-Port.Message", '&'));
                            input.put(player.getUniqueId(), m -> {
                                if (Util.isException(() -> Integer.parseInt(m.getString("message"))) || Integer.parseInt(m.getString("message")) <= 0 || Integer.parseInt(m.getString("message")) > 65535) {
                                    if (!gui.sendTitle(plugin.lang.getSection("Lang").getColoredString("Interface.Host-Creator.Edit-Port.Invalid-Title", '&'), 4 * 20))
                                        player.sendMessage(plugin.lang.getSection("Lang").getColoredString("Interface.Host-Creator.Edit-Port.Invalid", '&'));
                                    Bukkit.getScheduler().runTaskLater(plugin, () -> gui.hostCreator((UIRenderer.CreatorOptions) gui.lastUsedOptions), 4 * 20);
                                } else {
                                    ((UIRenderer.CreatorOptions) gui.lastUsedOptions).setPort(Integer.valueOf(m.getString("message")));
                                    gui.hostCreator((UIRenderer.CreatorOptions) gui.lastUsedOptions);
                                }
                            });
                        } else if (ChatColor.stripColor(item).equals(ChatColor.stripColor(plugin.lang.getSection("Lang").getColoredString("Interface.Host-Creator.Edit-RAM", '&')))) {
                            player.closeInventory();
                            if (!gui.sendTitle(plugin.lang.getSection("Lang").getColoredString("Interface.Host-Creator.Edit-RAM.Title", '&'), 4 * 20))
                                player.sendMessage(plugin.lang.getSection("Lang").getColoredString("Interface.Host-Creator.Edit-RAM.Message", '&'));
                            input.put(player.getUniqueId(), m -> {
                                if (Util.isException(() -> Integer.parseInt(m.getString("message"))) || Integer.parseInt(m.getString("message")) < 256) {
                                    if (!gui.sendTitle(plugin.lang.getSection("Lang").getColoredString("Interface.Host-Creator.Edit-RAM.Invalid-Title", '&'), 4 * 20))
                                        player.sendMessage(plugin.lang.getSection("Lang").getColoredString("Interface.Host-Creator.Edit-RAM.Invalid", '&'));
                                    Bukkit.getScheduler().runTaskLater(plugin, () -> gui.hostCreator((UIRenderer.CreatorOptions) gui.lastUsedOptions), 4 * 20);
                                } else {
                                    ((UIRenderer.CreatorOptions) gui.lastUsedOptions).setMemory(Integer.valueOf(m.getString("message")));
                                    gui.hostCreator((UIRenderer.CreatorOptions) gui.lastUsedOptions);
                                }
                            });
                        }

                    }
                } else if (event.getClickedInventory().getTitle().equals(plugin.lang.getSection("Lang").getColoredString("Interface.SubServer-Menu.Title", '&')) || // SubServer Menu
                        event.getClickedInventory().getTitle().startsWith(plugin.lang.getSection("Lang").getColoredString("Interface.Host-SubServer.Title", '&').split("\\$str\\$")[0]) &&
                                (plugin.lang.getSection("Lang").getColoredString("Interface.Host-SubServer.Title", '&').split("\\$str\\$").length == 1 ||
                                        event.getClickedInventory().getTitle().endsWith(plugin.lang.getSection("Lang").getColoredString("Interface.Host-SubServer.Title", '&').split("\\$str\\$")[1]))) {
                    if (event.getCurrentItem() != null && event.getCurrentItem().getType() != Material.AIR && event.getCurrentItem().hasItemMeta()) {
                        String item = event.getCurrentItem().getItemMeta().getDisplayName();

                        if (item.equals(plugin.lang.getSection("Lang").getColoredString("Interface.Generic.Back-Arrow", '&'))) {
                            player.closeInventory();
                            gui.subserverMenu(gui.lastPage - 1, null);
                        } else if (item.equals(plugin.lang.getSection("Lang").getColoredString("Interface.Generic.Next-Arrow", '&'))) {
                            player.closeInventory();
                            gui.subserverMenu(gui.lastPage + 1, null);
                        } else if (item.equals(plugin.lang.getSection("Lang").getColoredString("Interface.SubServer-Menu.Host-Menu", '&'))) {
                            player.closeInventory();
                            gui.hostMenu(1);
                        } else if (item.equals(plugin.lang.getSection("Lang").getColoredString("Interface.Generic.Back", '&'))) {
                            player.closeInventory();
                            gui.back();
                        } else if (!item.equals(ChatColor.RESET.toString()) && !item.equals(plugin.lang.getSection("Lang").getColoredString("Interface.SubServer-Menu.No-SubServers", '&'))) {
                            player.closeInventory();
                            gui.subserverAdmin(ChatColor.stripColor(item));
                        }
                    }
                } else if (event.getClickedInventory().getTitle().startsWith(plugin.lang.getSection("Lang").getColoredString("Interface.Host-Admin.Title", '&').split("\\$str\\$")[0]) && // Host Admin
                        (plugin.lang.getSection("Lang").getColoredString("Interface.Host-Admin.Title", '&').split("\\$str\\$").length == 1 ||
                                event.getClickedInventory().getTitle().endsWith(plugin.lang.getSection("Lang").getColoredString("Interface.Host-Admin.Title", '&').split("\\$str\\$")[1]))) {
                    if (event.getCurrentItem() != null && event.getCurrentItem().getType() != Material.AIR && event.getCurrentItem().hasItemMeta()) {
                        String item = event.getCurrentItem().getItemMeta().getDisplayName();

                        if (item.equals(plugin.lang.getSection("Lang").getColoredString("Interface.Generic.Back", '&'))) {
                            player.closeInventory();
                            gui.back();
                        } else if (item.equals(plugin.lang.getSection("Lang").getColoredString("Interface.Host-Admin.Creator", '&'))) {
                            player.closeInventory();
                            if (player.hasPermission("subservers.host.create.*") || player.hasPermission("subservers.host.create." + gui.lastVistedObject.toLowerCase())) {
                                gui.hostCreator(new UIRenderer.CreatorOptions(gui.lastVistedObject));
                            } else gui.reopen();
                        } else if (item.equals(plugin.lang.getSection("Lang").getColoredString("Interface.Host-Admin.Editor", '&'))) {
                            player.closeInventory();
                            if (player.hasPermission("subservers.host.edit.*") || player.hasPermission("subservers.host.edit." + gui.lastVistedObject.toLowerCase())) {
                                gui.hostEditor(new UIRenderer.HostEditorOptions(gui.lastVistedObject));
                            } else gui.reopen();
                        } else if (item.equals(plugin.lang.getSection("Lang").getColoredString("Interface.Host-Admin.SubServers", '&'))) {
                            player.closeInventory();
                            gui.subserverMenu(1, gui.lastVistedObject);
                        }
                    }
                } else if (event.getClickedInventory().getTitle().startsWith(plugin.lang.getSection("Lang").getColoredString("Interface.SubServer-Admin.Title", '&').split("\\$str\\$")[0]) && // SubServer Admin
                        (plugin.lang.getSection("Lang").getColoredString("Interface.SubServer-Admin.Title", '&').split("\\$str\\$").length == 1 ||
                                event.getClickedInventory().getTitle().endsWith(plugin.lang.getSection("Lang").getColoredString("Interface.SubServer-Admin.Title", '&').split("\\$str\\$")[1]))) {
                    if (event.getCurrentItem() != null && event.getCurrentItem().getType() != Material.AIR && event.getCurrentItem().hasItemMeta()) {
                        String item = event.getCurrentItem().getItemMeta().getDisplayName();

                        if (item.equals(plugin.lang.getSection("Lang").getColoredString("Interface.Generic.Back", '&'))) {
                            player.closeInventory();
                            gui.back();
                        } else if (item.equals(plugin.lang.getSection("Lang").getColoredString("Interface.SubServer-Admin.Editor", '&'))) {
                            player.closeInventory();
                            if (player.hasPermission("subservers.subserver.edit.*") || player.hasPermission("subservers.subserver.edit." + gui.lastVistedObject.toLowerCase())) {
                                gui.subserverEditor(new UIRenderer.SubServerEditorOptions(gui.lastVistedObject));
                            } else gui.reopen();
                        } else if (item.equals(plugin.lang.getSection("Lang").getColoredString("Interface.SubServer-Admin.Start", '&'))) {
                            player.closeInventory();
                            if (player.hasPermission("subservers.subserver.start.*") || player.hasPermission("subservers.subserver.start." + gui.lastVistedObject.toLowerCase())) {
                                gui.setDownloading(plugin.lang.getSection("Lang").getColoredString("Interface.Generic.Downloading.Response", '&'));
                                plugin.subdata.sendPacket(new PacketStartServer(player.getUniqueId(), gui.lastVistedObject, UUID.randomUUID().toString(), json -> {
                                    gui.setDownloading(plugin.lang.getSection("Lang").getColoredString("Interface.SubServer-Admin.Start.Title", '&'));
                                    Bukkit.getScheduler().runTaskLater(plugin, gui::reopen, 30);
                                }));
                            } else gui.reopen();
                        } else if (item.equals(plugin.lang.getSection("Lang").getColoredString("Interface.SubServer-Admin.Stop", '&'))) {
                            player.closeInventory();
                            if (player.hasPermission("subservers.subserver.stop.*") || player.hasPermission("subservers.subserver.stop." + gui.lastVistedObject.toLowerCase())) {
                                gui.setDownloading(plugin.lang.getSection("Lang").getColoredString("Interface.Generic.Downloading.Response", '&'));
                                final Container<Boolean> listening = new Container<Boolean>(true);
                                ((PacketInRunEvent) SubDataClient.getPacket("SubRunEvent")).callback("SubStoppedEvent", new JSONCallback() {
                                    @Override
                                    public void run(JSONObject json) {
                                        try {
                                            if (listening.get()) if (!json.getString("server").equalsIgnoreCase(gui.lastVistedObject)) {
                                                ((PacketInRunEvent) SubDataClient.getPacket("RunEvent")).callback("SubStoppedEvent", this);
                                            } else {
                                                gui.reopen();
                                            }
                                        } catch (Exception e) {}
                                    }
                                });
                                plugin.subdata.sendPacket(new PacketStopServer(player.getUniqueId(), gui.lastVistedObject, false, UUID.randomUUID().toString(), json -> {
                                    if (json.getInt("r") != 0) {
                                        gui.reopen();
                                        listening.set(false);
                                    } else gui.setDownloading(plugin.lang.getSection("Lang").getColoredString("Interface.SubServer-Admin.Stop.Title", '&').replace("$str$", gui.lastVistedObject));
                                }));
                            } else gui.reopen();
                        } else if (item.equals(plugin.lang.getSection("Lang").getColoredString("Interface.SubServer-Admin.Terminate", '&'))) {
                            player.closeInventory();
                            if (player.hasPermission("subservers.subserver.terminate.*") || player.hasPermission("subservers.subserver.terminate." + gui.lastVistedObject.toLowerCase())) {
                                gui.setDownloading(plugin.lang.getSection("Lang").getColoredString("Interface.Generic.Downloading.Response", '&'));
                                final Container<Boolean> listening = new Container<Boolean>(true);
                                ((PacketInRunEvent) SubDataClient.getPacket("SubRunEvent")).callback("SubStoppedEvent", new JSONCallback() {
                                    @Override
                                    public void run(JSONObject json) {
                                        try {
                                            if (listening.get()) if (!json.getString("server").equalsIgnoreCase(gui.lastVistedObject)) {
                                               ((PacketInRunEvent) SubDataClient.getPacket("RunEvent")).callback("SubStoppedEvent", this);
                                            } else {
                                                gui.reopen();
                                            }
                                        } catch (Exception e) {}
                                    }
                                });
                                plugin.subdata.sendPacket(new PacketStopServer(player.getUniqueId(), gui.lastVistedObject, false, UUID.randomUUID().toString(), json -> {
                                    if (json.getInt("r") != 0) {
                                        gui.reopen();
                                        listening.set(false);
                                    } else gui.setDownloading(plugin.lang.getSection("Lang").getColoredString("Interface.SubServer-Admin.Terminate.Title", '&').replace("$str$", gui.lastVistedObject));
                                }));
                            } else gui.reopen();
                        } else if (item.equals(plugin.lang.getSection("Lang").getColoredString("Interface.SubServer-Admin.Command", '&'))) {
                            player.closeInventory();
                            if (player.hasPermission("subservers.subserver.command.*") || player.hasPermission("subservers.subserver.command." + gui.lastVistedObject.toLowerCase())) {
                                if (!gui.sendTitle(plugin.lang.getSection("Lang").getColoredString("Interface.SubServer-Admin.Command.Title", '&'), 4 * 20))
                                    player.sendMessage(plugin.lang.getSection("Lang").getColoredString("Interface.SubServer-Admin.Command.Message", '&'));
                                input.put(player.getUniqueId(), m -> {
                                    gui.setDownloading(plugin.lang.getSection("Lang").getColoredString("Interface.Generic.Downloading.Response", '&'));
                                    plugin.subdata.sendPacket(new PacketCommandServer(player.getUniqueId(), gui.lastVistedObject, m.getString("message"), UUID.randomUUID().toString(), json -> {
                                        gui.reopen();
                                    }));
                                });
                            } else gui.reopen();
                        } else if (item.equals(plugin.lang.getSection("Lang").getColoredString("Interface.SubServer-Admin.Teleport", '&'))) {
                            player.closeInventory();
                            if (player.hasPermission("subservers.subserver.teleport.*") || player.hasPermission("subservers.subserver.teleport." + gui.lastVistedObject.toLowerCase())) {
                                gui.setDownloading(plugin.lang.getSection("Lang").getColoredString("Interface.Generic.Downloading.Response", '&'));
                                plugin.subdata.sendPacket(new PacketTeleportPlayer(player.getUniqueId(), gui.lastVistedObject, UUID.randomUUID().toString(), json -> {
                                    if (json.getInt("r") != 0) gui.reopen();
                                }));
                            } else gui.reopen();
                        }
                    }
                }
                event.setCancelled(true);
            }
        }
    }

    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.HIGHEST)
    public void input(org.bukkit.event.player.PlayerChatEvent event) {
        if (!event.isCancelled() && input.keySet().contains(event.getPlayer().getUniqueId())) {
            JSONObject json = new JSONObject();
            json.put("message", event.getMessage());
            input.get(event.getPlayer().getUniqueId()).run(json);
            input.remove(event.getPlayer().getUniqueId());
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void input(PlayerCommandPreprocessEvent event) {
        if (!event.isCancelled() && input.keySet().contains(event.getPlayer().getUniqueId())) {
            JSONObject json = new JSONObject();
            json.put("message", event.getMessage());
            input.get(event.getPlayer().getUniqueId()).run(json);
            input.remove(event.getPlayer().getUniqueId());
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void close(InventoryCloseEvent event) {
        if (gui.keySet().contains(event.getPlayer().getUniqueId())) gui.get(event.getPlayer().getUniqueId()).open = false;
    }

    @EventHandler
    public void clean(PlayerQuitEvent event) {
        if (gui.keySet().contains(event.getPlayer().getUniqueId())) {
            gui.get(event.getPlayer().getUniqueId()).setDownloading(null);
            gui.remove(event.getPlayer().getUniqueId());
            input.remove(event.getPlayer().getUniqueId());
        }
    }
}

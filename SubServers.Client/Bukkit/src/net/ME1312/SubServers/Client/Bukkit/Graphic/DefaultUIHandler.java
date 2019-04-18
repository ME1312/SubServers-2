package net.ME1312.SubServers.Client.Bukkit.Graphic;

import net.ME1312.Galaxi.Library.Config.YAMLSection;
import net.ME1312.Galaxi.Library.Container;
import net.ME1312.Galaxi.Library.Callback.Callback;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.SubServers.Client.Bukkit.Network.API.Host;
import net.ME1312.SubServers.Client.Bukkit.Network.API.SubServer;
import net.ME1312.SubServers.Client.Bukkit.Network.Packet.*;
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

import java.util.HashMap;
import java.util.UUID;

/**
 * Default GUI Listener
 */
public class DefaultUIHandler implements UIHandler, Listener {
    private HashMap<UUID, Callback<YAMLSection>> input = new HashMap<UUID, Callback<YAMLSection>>();
    private HashMap<UUID, DefaultUIRenderer> gui = new HashMap<UUID, DefaultUIRenderer>();
    private boolean enabled = true;
    private SubPlugin plugin;

    /**
     * Creates a new Internal GUI Listener
     *
     * @param plugin Event
     */
    public DefaultUIHandler(SubPlugin plugin) {
        if (Util.isNull(plugin)) throw new NullPointerException();
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public DefaultUIRenderer getRenderer(Player player) {
        if (!gui.keySet().contains(player.getUniqueId())) gui.put(player.getUniqueId(), new DefaultUIRenderer(plugin, player.getUniqueId()));
        return gui.get(player.getUniqueId());
    }

    public void disable() {
        enabled = false;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void click(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        if (!event.isCancelled() && enabled && gui.keySet().contains(player.getUniqueId())) {
            DefaultUIRenderer gui = this.gui.get(player.getUniqueId());
            if (gui.open && event.getClickedInventory() != null && event.getClickedInventory().getTitle() != null) {
                if (plugin.subdata == null) {
                    new IllegalStateException("SubData is not connected").printStackTrace();
                } else if (Util.isException(() -> plugin.api.getLangChannels())) {
                    new IllegalStateException("There are no lang options available at this time").printStackTrace();
                } else if (event.getClickedInventory().getTitle().equals(plugin.api.getLang("SubServers", "Interface.Host-Menu.Title"))) { // Host Menu
                    if (event.getCurrentItem() != null && event.getCurrentItem().getType() != Material.AIR && event.getCurrentItem().hasItemMeta()) {
                        String item = event.getCurrentItem().getItemMeta().getDisplayName();

                        if (item.equals(plugin.api.getLang("SubServers", "Interface.Generic.Back-Arrow"))) {
                            player.closeInventory();
                            gui.hostMenu(gui.lastPage - 1);
                        } else if (item.equals(plugin.api.getLang("SubServers", "Interface.Generic.Next-Arrow"))) {
                            player.closeInventory();
                            gui.hostMenu(gui.lastPage + 1);
                        } else if (item.equals(plugin.api.getLang("SubServers", "Interface.Host-Menu.Group-Menu"))) {
                            player.closeInventory();
                            gui.groupMenu(1);
                        } else if (item.equals(plugin.api.getLang("SubServers", "Interface.Host-Menu.Server-Menu"))) {
                            player.closeInventory();
                            gui.serverMenu(1, null, null);
                        } else if (!item.equals(ChatColor.RESET.toString()) && !item.equals(plugin.api.getLang("SubServers", "Interface.Host-Menu.No-Hosts"))) {
                            player.closeInventory();
                            String obj;
                            if (event.getCurrentItem().getItemMeta().getLore() != null && event.getCurrentItem().getItemMeta().getLore().size() > 0 && event.getCurrentItem().getItemMeta().getLore().get(0).startsWith(ChatColor.GRAY.toString())) {
                                obj = ChatColor.stripColor(event.getCurrentItem().getItemMeta().getLore().get(0));
                            } else {
                                obj = ChatColor.stripColor(item);
                            }
                            gui.hostAdmin(obj);
                        }
                    }
                } else if (event.getClickedInventory().getTitle().startsWith(plugin.api.getLang("SubServers", "Interface.Host-Creator.Title").split("\\$str\\$")[0]) && // Host Creator
                        (plugin.api.getLang("SubServers", "Interface.Host-Creator.Title").split("\\$str\\$").length == 1 ||
                                event.getClickedInventory().getTitle().endsWith(plugin.api.getLang("SubServers", "Interface.Host-Creator.Title").split("\\$str\\$")[1]))) {
                    if (event.getCurrentItem() != null && event.getCurrentItem().getType() != Material.AIR && event.getCurrentItem().hasItemMeta()) {
                        String item = event.getCurrentItem().getItemMeta().getDisplayName();
                        if (item.equals(plugin.api.getLang("SubServers", "Interface.Generic.Back"))) {
                            player.closeInventory();
                            gui.back();
                        } else if (item.equals(plugin.api.getLang("SubServers", "Interface.Generic.Undo"))) {
                            player.closeInventory();
                            ((UIRenderer.CreatorOptions) gui.lastVisitedObjects[0]).undo();
                            gui.hostCreator((UIRenderer.CreatorOptions) gui.lastVisitedObjects[0]);
                        } else if (item.equals(plugin.api.getLang("SubServers", "Interface.Host-Creator.Submit"))) {
                            if (player.hasPermission("subservers.host.create.*") || player.hasPermission("subservers.host.create." + ((UIRenderer.CreatorOptions) gui.lastVisitedObjects[0]).getHost().toLowerCase())) {
                                player.closeInventory();
                                gui.setDownloading(plugin.api.getLang("SubServers", "Interface.Generic.Downloading.Response"));
                                plugin.subdata.sendPacket(new PacketCreateServer(player.getUniqueId(), ((UIRenderer.CreatorOptions) gui.lastVisitedObjects[0]), data -> {
                                    gui.back();
                                }));
                            } else {
                                gui.back();
                            }
                        } else if (ChatColor.stripColor(item).equals(ChatColor.stripColor(plugin.api.getLang("SubServers", "Interface.Host-Creator.Edit-Name")))) {
                            player.closeInventory();
                            if (!gui.sendTitle(plugin.api.getLang("SubServers", "Interface.Host-Creator.Edit-Name.Title"), 4 * 20))
                                player.sendMessage(plugin.api.getLang("SubServers", "Interface.Host-Creator.Edit-Name.Message"));
                            input.put(player.getUniqueId(), m -> {
                                if (m.getString("message").contains(" ")) {
                                    if (!gui.sendTitle(plugin.api.getLang("SubServers", "Interface.Host-Creator.Edit-Name.Invalid-Title"), 4 * 20))
                                        player.sendMessage(plugin.api.getLang("SubServers", "Interface.Host-Creator.Edit-Name.Invalid"));
                                    Bukkit.getScheduler().runTaskLater(plugin, () -> gui.hostCreator((UIRenderer.CreatorOptions) gui.lastVisitedObjects[0]), 4 * 20);
                                } else {
                                    gui.setDownloading(plugin.api.getLang("SubServers", "Interface.Generic.Downloading.Response"));
                                    plugin.api.getSubServer(m.getString("message"), server -> {
                                        if (server != null) {
                                            gui.setDownloading(null);
                                            if (!gui.sendTitle(plugin.api.getLang("SubServers", "Interface.Host-Creator.Edit-Name.Exists-Title"), 4 * 20))
                                                player.sendMessage(plugin.api.getLang("SubServers", "Interface.Host-Creator.Edit-Name.Exists"));
                                            Bukkit.getScheduler().runTaskLater(plugin, () -> gui.hostCreator((UIRenderer.CreatorOptions) gui.lastVisitedObjects[0]), 4 * 20);
                                        } else {
                                            ((UIRenderer.CreatorOptions) gui.lastVisitedObjects[0]).setName(m.getString("message"));
                                            gui.hostCreator((UIRenderer.CreatorOptions) gui.lastVisitedObjects[0]);
                                        }
                                    });
                                }
                            });
                        } else if (ChatColor.stripColor(item).equals(ChatColor.stripColor(plugin.api.getLang("SubServers", "Interface.Host-Creator.Edit-Template")))) {
                            player.closeInventory();
                            gui.hostCreatorTemplates(1, (UIRenderer.CreatorOptions) gui.lastVisitedObjects[0]);
                        } else if (ChatColor.stripColor(item).equals(ChatColor.stripColor(plugin.api.getLang("SubServers", "Interface.Host-Creator.Edit-Version")))) {
                            player.closeInventory();
                            if (!gui.sendTitle(plugin.api.getLang("SubServers", "Interface.Host-Creator.Edit-Version.Title"), 4 * 20))
                                player.sendMessage(plugin.api.getLang("SubServers", "Interface.Host-Creator.Edit-Version.Message"));
                            input.put(player.getUniqueId(), m -> {
                                if (new Version("1.8").compareTo(new Version(m.getString("message"))) > 0) {
                                    if (!gui.sendTitle(plugin.api.getLang("SubServers", "Interface.Host-Creator.Edit-Version.Unavailable-Title"), 4 * 20))
                                        player.sendMessage(plugin.api.getLang("SubServers", "Interface.Host-Creator.Edit-Version.Unavailable"));
                                    Bukkit.getScheduler().runTaskLater(plugin, () -> gui.hostCreator((UIRenderer.CreatorOptions) gui.lastVisitedObjects[0]), 4 * 20);
                                } else {
                                    ((UIRenderer.CreatorOptions) gui.lastVisitedObjects[0]).setVersion(new Version(m.getString("message")));
                                    gui.hostCreator((UIRenderer.CreatorOptions) gui.lastVisitedObjects[0]);
                                }
                            });
                        } else if (ChatColor.stripColor(item).equals(ChatColor.stripColor(plugin.api.getLang("SubServers", "Interface.Host-Creator.Edit-Port")))) {
                            player.closeInventory();
                            if (!gui.sendTitle(plugin.api.getLang("SubServers", "Interface.Host-Creator.Edit-Port.Title"), 4 * 20))
                                player.sendMessage(plugin.api.getLang("SubServers", "Interface.Host-Creator.Edit-Port.Message"));
                            input.put(player.getUniqueId(), m -> {
                                if (Util.isException(() -> Integer.parseInt(m.getString("message"))) || Integer.parseInt(m.getString("message")) <= 0 || Integer.parseInt(m.getString("message")) > 65535) {
                                    if (!gui.sendTitle(plugin.api.getLang("SubServers", "Interface.Host-Creator.Edit-Port.Invalid-Title"), 4 * 20))
                                        player.sendMessage(plugin.api.getLang("SubServers", "Interface.Host-Creator.Edit-Port.Invalid"));
                                    Bukkit.getScheduler().runTaskLater(plugin, () -> gui.hostCreator((UIRenderer.CreatorOptions) gui.lastVisitedObjects[0]), 4 * 20);
                                } else {
                                    ((UIRenderer.CreatorOptions) gui.lastVisitedObjects[0]).setPort(Integer.valueOf(m.getString("message")));
                                    gui.hostCreator((UIRenderer.CreatorOptions) gui.lastVisitedObjects[0]);
                                }
                            });
                        }
                    }
                } else if (event.getClickedInventory().getTitle().startsWith(plugin.api.getLang("SubServers", "Interface.Host-Creator.Edit-Template.Title").split("\\$str\\$")[0]) && // Host Creator Templates
                        (plugin.api.getLang("SubServers", "Interface.Host-Creator.Edit-Template.Title").split("\\$str\\$").length == 1 ||
                                event.getClickedInventory().getTitle().endsWith(plugin.api.getLang("SubServers", "Interface.Host-Creator.Edit-Template.Title").split("\\$str\\$")[1]))) {
                    if (event.getCurrentItem() != null && event.getCurrentItem().getType() != Material.AIR && event.getCurrentItem().hasItemMeta()) {
                        String item = event.getCurrentItem().getItemMeta().getDisplayName();
                        if (item.equals(plugin.api.getLang("SubServers", "Interface.Generic.Back-Arrow"))) {
                            player.closeInventory();
                            gui.hostCreatorTemplates(gui.lastPage - 1, (UIRenderer.CreatorOptions) gui.lastVisitedObjects[0]);
                        } else if (item.equals(plugin.api.getLang("SubServers", "Interface.Generic.Next-Arrow"))) {
                            player.closeInventory();
                            gui.hostCreatorTemplates(gui.lastPage + 1, (UIRenderer.CreatorOptions) gui.lastVisitedObjects[0]);
                        } else if (item.equals(plugin.api.getLang("SubServers", "Interface.Generic.Back"))) {
                            player.closeInventory();
                            gui.hostCreator((UIRenderer.CreatorOptions) gui.lastVisitedObjects[0]);
                        } else {
                            player.closeInventory();
                            String obj;
                            if (event.getCurrentItem().getItemMeta().getLore() != null && event.getCurrentItem().getItemMeta().getLore().size() > 0 && event.getCurrentItem().getItemMeta().getLore().get(0).startsWith(ChatColor.GRAY.toString())) {
                                obj = ChatColor.stripColor(event.getCurrentItem().getItemMeta().getLore().get(0));
                            } else {
                                obj = ChatColor.stripColor(item);
                            }
                            ((UIRenderer.CreatorOptions) gui.lastVisitedObjects[0]).setTemplate(obj);
                            gui.hostCreator((UIRenderer.CreatorOptions) gui.lastVisitedObjects[0]);
                        }
                    }
                } else if (event.getClickedInventory().getTitle().startsWith(plugin.api.getLang("SubServers", "Interface.Host-Plugin.Title").split("\\$str\\$")[0]) && // Host Plugin
                        (plugin.api.getLang("SubServers", "Interface.Host-Plugin.Title").split("\\$str\\$").length == 1 ||
                                event.getClickedInventory().getTitle().endsWith(plugin.api.getLang("SubServers", "Interface.Host-Plugin.Title").split("\\$str\\$")[1]))) {
                    if (event.getCurrentItem() != null && event.getCurrentItem().getType() != Material.AIR && event.getCurrentItem().hasItemMeta()) {
                        String item = event.getCurrentItem().getItemMeta().getDisplayName();
                        if (item.equals(plugin.api.getLang("SubServers", "Interface.Generic.Back-Arrow"))) {
                            player.closeInventory();
                            gui.hostPlugin(gui.lastPage - 1, ((String) gui.lastVisitedObjects[0]));
                        } else if (item.equals(plugin.api.getLang("SubServers", "Interface.Generic.Next-Arrow"))) {
                            player.closeInventory();
                            gui.hostPlugin(gui.lastPage + 1, ((String) gui.lastVisitedObjects[0]));
                        } else if (item.equals(plugin.api.getLang("SubServers", "Interface.Generic.Back"))) {
                            player.closeInventory();
                            gui.back();
                        } else {
                            player.closeInventory();
                            final Container<PluginRenderer<Host>> plugin = new Container<PluginRenderer<Host>>(null);
                            for (PluginRenderer<Host> renderer : DefaultUIRenderer.hostPlugins.values()) {
                                if (item.equals(renderer.getIcon().getItemMeta().getDisplayName())) plugin.set(renderer);
                            }
                            if (plugin.get() == null) {
                                gui.reopen();
                            } else {
                                gui.setDownloading(ChatColor.stripColor(this.plugin.api.getLang("SubServers", "Interface.Host-Plugin.Title").replace("$str$", (String) gui.lastVisitedObjects[0])));
                                this.plugin.api.getHost((String) gui.lastVisitedObjects[0], host -> {
                                    if (host != null) {
                                        gui.setDownloading(null);
                                        plugin.get().open(player, host);
                                    } else {
                                        gui.back();
                                    }
                                });
                            }
                        }
                    }

                } else if (event.getClickedInventory().getTitle().equals(plugin.api.getLang("SubServers", "Interface.Group-Menu.Title"))) { // Host Menu
                    if (event.getCurrentItem() != null && event.getCurrentItem().getType() != Material.AIR && event.getCurrentItem().hasItemMeta()) {
                        String item = event.getCurrentItem().getItemMeta().getDisplayName();

                        if (item.equals(plugin.api.getLang("SubServers", "Interface.Generic.Back-Arrow"))) {
                            player.closeInventory();
                            gui.groupMenu(gui.lastPage - 1);
                        } else if (item.equals(plugin.api.getLang("SubServers", "Interface.Generic.Next-Arrow"))) {
                            player.closeInventory();
                            gui.groupMenu(gui.lastPage + 1);
                        } else if (item.equals(plugin.api.getLang("SubServers", "Interface.Group-Menu.Server-Menu"))) {
                            player.closeInventory();
                            gui.serverMenu(1, null, null);
                        } else if (!item.equals(ChatColor.RESET.toString()) && !item.equals(plugin.api.getLang("SubServers", "Interface.Group-Menu.No-Groups"))) {
                            player.closeInventory();
                            gui.serverMenu(1, null, ChatColor.stripColor(item));
                        }
                    }
                } else if (event.getClickedInventory().getTitle().equals(plugin.api.getLang("SubServers", "Interface.Server-Menu.Title")) || // SubServer Menu
                        event.getClickedInventory().getTitle().startsWith(plugin.api.getLang("SubServers", "Interface.Host-SubServer.Title").split("\\$str\\$")[0]) &&
                                (plugin.api.getLang("SubServers", "Interface.Host-SubServer.Title").split("\\$str\\$").length == 1 ||
                                        event.getClickedInventory().getTitle().endsWith(plugin.api.getLang("SubServers", "Interface.Host-SubServer.Title").split("\\$str\\$")[1])) ||
                        event.getClickedInventory().getTitle().startsWith(plugin.api.getLang("SubServers", "Interface.Group-SubServer.Title").split("\\$str\\$")[0]) &&
                                (plugin.api.getLang("SubServers", "Interface.Group-SubServer.Title").split("\\$str\\$").length == 1 ||
                                        event.getClickedInventory().getTitle().endsWith(plugin.api.getLang("SubServers", "Interface.Group-SubServer.Title").split("\\$str\\$")[1]))) {
                    if (event.getCurrentItem() != null && event.getCurrentItem().getType() != Material.AIR && event.getCurrentItem().hasItemMeta()) {
                        String item = event.getCurrentItem().getItemMeta().getDisplayName();

                        if (item.equals(plugin.api.getLang("SubServers", "Interface.Generic.Back-Arrow"))) {
                            player.closeInventory();
                            gui.serverMenu(gui.lastPage - 1, (String) gui.lastVisitedObjects[0], (String) gui.lastVisitedObjects[1]);
                        } else if (item.equals(plugin.api.getLang("SubServers", "Interface.Generic.Next-Arrow"))) {
                            player.closeInventory();
                            gui.serverMenu(gui.lastPage + 1, (String) gui.lastVisitedObjects[0], (String) gui.lastVisitedObjects[1]);
                        } else if (item.equals(plugin.api.getLang("SubServers", "Interface.Server-Menu.Host-Menu"))) {
                            player.closeInventory();
                            gui.hostMenu(1);
                        } else if (item.equals(plugin.api.getLang("SubServers", "Interface.Generic.Back"))) {
                            player.closeInventory();
                            gui.back();
                        } else if (!item.equals(ChatColor.RESET.toString()) && !item.startsWith(ChatColor.WHITE.toString()) && !item.equals(plugin.api.getLang("SubServers", "Interface.Server-Menu.No-Servers"))) {
                            player.closeInventory();
                            String obj;
                            if (event.getCurrentItem().getItemMeta().getLore() != null && event.getCurrentItem().getItemMeta().getLore().size() > 0 && event.getCurrentItem().getItemMeta().getLore().get(0).startsWith(ChatColor.GRAY.toString())) {
                                obj = ChatColor.stripColor(event.getCurrentItem().getItemMeta().getLore().get(0));
                            } else {
                                obj = ChatColor.stripColor(item);
                            }
                            gui.subserverAdmin(obj);
                        }
                    }
                } else if (event.getClickedInventory().getTitle().startsWith(plugin.api.getLang("SubServers", "Interface.Host-Admin.Title").split("\\$str\\$")[0]) && // Host Admin
                        (plugin.api.getLang("SubServers", "Interface.Host-Admin.Title").split("\\$str\\$").length == 1 ||
                                event.getClickedInventory().getTitle().endsWith(plugin.api.getLang("SubServers", "Interface.Host-Admin.Title").split("\\$str\\$")[1]))) {
                    if (event.getCurrentItem() != null && event.getCurrentItem().getType() != Material.AIR && event.getCurrentItem().hasItemMeta()) {
                        String item = event.getCurrentItem().getItemMeta().getDisplayName();

                        if (item.equals(plugin.api.getLang("SubServers", "Interface.Generic.Back"))) {
                            player.closeInventory();
                            gui.back();
                        } else if (item.equals(plugin.api.getLang("SubServers", "Interface.Host-Admin.Creator"))) {
                            player.closeInventory();
                            if (player.hasPermission("subservers.host.create.*") || player.hasPermission("subservers.host.create." + ((String) gui.lastVisitedObjects[0]).toLowerCase())) {
                                gui.hostCreator(new UIRenderer.CreatorOptions((String) gui.lastVisitedObjects[0]));
                            } else gui.reopen();
                        } else if (item.equals(plugin.api.getLang("SubServers", "Interface.Host-Admin.SubServers"))) {
                            player.closeInventory();
                            gui.serverMenu(1, (String) gui.lastVisitedObjects[0], null);
                        } else if (item.equals(plugin.api.getLang("SubServers", "Interface.Host-Admin.Plugins"))) {
                            player.closeInventory();
                            gui.hostPlugin(1, (String) gui.lastVisitedObjects[0]);
                        }
                    }
                } else if (event.getClickedInventory().getTitle().startsWith(plugin.api.getLang("SubServers", "Interface.SubServer-Admin.Title").split("\\$str\\$")[0]) && // SubServer Admin
                        (plugin.api.getLang("SubServers", "Interface.SubServer-Admin.Title").split("\\$str\\$").length == 1 ||
                                event.getClickedInventory().getTitle().endsWith(plugin.api.getLang("SubServers", "Interface.SubServer-Admin.Title").split("\\$str\\$")[1]))) {
                    if (event.getCurrentItem() != null && event.getCurrentItem().getType() != Material.AIR && event.getCurrentItem().hasItemMeta()) {
                        String item = event.getCurrentItem().getItemMeta().getDisplayName();

                        if (item.equals(plugin.api.getLang("SubServers", "Interface.Generic.Back"))) {
                            player.closeInventory();
                            gui.back();
                        } else if (item.equals(plugin.api.getLang("SubServers", "Interface.SubServer-Admin.Start"))) {
                            player.closeInventory();
                            if (player.hasPermission("subservers.subserver.start.*") || player.hasPermission("subservers.subserver.start." + ((String) gui.lastVisitedObjects[0]).toLowerCase())) {
                                gui.setDownloading(plugin.api.getLang("SubServers", "Interface.Generic.Downloading.Response"));
                                plugin.subdata.sendPacket(new PacketStartServer(player.getUniqueId(), (String) gui.lastVisitedObjects[0], data -> {
                                    gui.setDownloading(plugin.api.getLang("SubServers", "Interface.SubServer-Admin.Start.Title"));
                                    Bukkit.getScheduler().runTaskLater(plugin, gui::reopen, 30);
                                }));
                            } else gui.reopen();
                        } else if (item.equals(plugin.api.getLang("SubServers", "Interface.SubServer-Admin.Stop"))) {
                            player.closeInventory();
                            if (player.hasPermission("subservers.subserver.stop.*") || player.hasPermission("subservers.subserver.stop." + ((String) gui.lastVisitedObjects[0]).toLowerCase())) {
                                gui.setDownloading(plugin.api.getLang("SubServers", "Interface.Generic.Downloading.Response"));
                                final Container<Boolean> listening = new Container<Boolean>(true);
                                PacketInExRunEvent.callback("SubStoppedEvent", new Callback<ObjectMap<String>>() {
                                    @Override
                                    public void run(ObjectMap<String> json) {
                                        try {
                                            if (listening.get()) if (!json.getString("server").equalsIgnoreCase((String) gui.lastVisitedObjects[0])) {
                                                PacketInExRunEvent.callback("SubStoppedEvent", this);
                                            } else {
                                                Bukkit.getScheduler().runTaskLater(plugin, gui::reopen, 5);
                                            }
                                        } catch (Exception e) {}
                                    }
                                });
                                plugin.subdata.sendPacket(new PacketStopServer(player.getUniqueId(), (String) gui.lastVisitedObjects[0], false, data -> {
                                    if (data.getInt(0x0001) != 0) {
                                        gui.reopen();
                                        listening.set(false);
                                    } else gui.setDownloading(plugin.api.getLang("SubServers", "Interface.SubServer-Admin.Stop.Title").replace("$str$", (String) gui.lastVisitedObjects[0]));
                                }));
                            } else gui.reopen();
                        } else if (item.equals(plugin.api.getLang("SubServers", "Interface.SubServer-Admin.Terminate"))) {
                            player.closeInventory();
                            if (player.hasPermission("subservers.subserver.terminate.*") || player.hasPermission("subservers.subserver.terminate." + ((String) gui.lastVisitedObjects[0]).toLowerCase())) {
                                gui.setDownloading(plugin.api.getLang("SubServers", "Interface.Generic.Downloading.Response"));
                                final Container<Boolean> listening = new Container<Boolean>(true);
                                PacketInExRunEvent.callback("SubStoppedEvent", new Callback<ObjectMap<String>>() {
                                    @Override
                                    public void run(ObjectMap<String> json) {
                                        try {
                                            if (listening.get()) if (!json.getString("server").equalsIgnoreCase((String) gui.lastVisitedObjects[0])) {
                                                PacketInExRunEvent.callback("SubStoppedEvent", this);
                                            } else {
                                                gui.reopen();
                                            }
                                        } catch (Exception e) {}
                                    }
                                });
                                plugin.subdata.sendPacket(new PacketStopServer(player.getUniqueId(), (String) gui.lastVisitedObjects[0], false, data -> {
                                    if (data.getInt(0x0001) != 0) {
                                        gui.reopen();
                                        listening.set(false);
                                    } else gui.setDownloading(plugin.api.getLang("SubServers", "Interface.SubServer-Admin.Terminate.Title").replace("$str$", (String) gui.lastVisitedObjects[0]));
                                }));
                            } else gui.reopen();
                        } else if (item.equals(plugin.api.getLang("SubServers", "Interface.SubServer-Admin.Command"))) {
                            player.closeInventory();
                            if (player.hasPermission("subservers.subserver.command.*") || player.hasPermission("subservers.subserver.command." + ((String) gui.lastVisitedObjects[0]).toLowerCase())) {
                                if (!gui.sendTitle(plugin.api.getLang("SubServers", "Interface.SubServer-Admin.Command.Title"), 4 * 20))
                                    player.sendMessage(plugin.api.getLang("SubServers", "Interface.SubServer-Admin.Command.Message"));
                                input.put(player.getUniqueId(), m -> {
                                    gui.setDownloading(plugin.api.getLang("SubServers", "Interface.Generic.Downloading.Response"));
                                    plugin.subdata.sendPacket(new PacketCommandServer(player.getUniqueId(), (String) gui.lastVisitedObjects[0], (m.getString("message").startsWith("/"))?m.getString("message").substring(1):m.getString("message"), data -> {
                                        gui.reopen();
                                    }));
                                });
                            } else gui.reopen();
                        } else if (item.equals(plugin.api.getLang("SubServers", "Interface.SubServer-Admin.Plugins"))) {
                            player.closeInventory();
                            gui.subserverPlugin(1, (String) gui.lastVisitedObjects[0]);
                        }
                    }
                } else if (event.getClickedInventory().getTitle().startsWith(plugin.api.getLang("SubServers", "Interface.SubServer-Plugin.Title").split("\\$str\\$")[0]) && // SubServer Plugin
                        (plugin.api.getLang("SubServers", "Interface.SubServer-Plugin.Title").split("\\$str\\$").length == 1 ||
                                event.getClickedInventory().getTitle().endsWith(plugin.api.getLang("SubServers", "Interface.SubServer-Plugin.Title").split("\\$str\\$")[1]))) {
                    if (event.getCurrentItem() != null && event.getCurrentItem().getType() != Material.AIR && event.getCurrentItem().hasItemMeta()) {
                        String item = event.getCurrentItem().getItemMeta().getDisplayName();
                        if (item.equals(plugin.api.getLang("SubServers", "Interface.Generic.Back-Arrow"))) {
                            player.closeInventory();
                            gui.subserverPlugin(gui.lastPage - 1, (String) gui.lastVisitedObjects[0]);
                        } else if (item.equals(plugin.api.getLang("SubServers", "Interface.Generic.Next-Arrow"))) {
                            player.closeInventory();
                            gui.subserverPlugin(gui.lastPage + 1, (String) gui.lastVisitedObjects[0]);
                        } else if (item.equals(plugin.api.getLang("SubServers", "Interface.Generic.Back"))) {
                            player.closeInventory();
                            gui.back();
                        } else {
                            player.closeInventory();
                            Container<PluginRenderer<SubServer>> plugin = new Container<PluginRenderer<SubServer>>(null);
                            for (PluginRenderer<SubServer> renderer : DefaultUIRenderer.subserverPlugins.values()) {
                                if (item.equals(renderer.getIcon().getItemMeta().getDisplayName())) plugin.set(renderer);
                            }
                            if (plugin.get() == null) {
                                gui.reopen();
                            } else {
                                gui.setDownloading(ChatColor.stripColor(this.plugin.api.getLang("SubServers", "Interface.SubServer-Plugin.Title").replace("$str$", (String) gui.lastVisitedObjects[0])));
                                this.plugin.api.getSubServer((String) gui.lastVisitedObjects[0], subserver -> {
                                    if (subserver != null) {
                                        gui.setDownloading(null);
                                        plugin.get().open(player, subserver);
                                    } else {
                                        gui.back();
                                    }
                                });
                            }
                        }
                    }
                }
                event.setCancelled(true);
            }
        }
    }

    /**
     * Input Listener
     *
     * @param event Event
     */
    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.LOWEST)
    public void input(org.bukkit.event.player.PlayerChatEvent event) {
        if (!event.isCancelled() && enabled && input.keySet().contains(event.getPlayer().getUniqueId())) {
            YAMLSection data = new YAMLSection();
            data.set("message", event.getMessage());
            input.get(event.getPlayer().getUniqueId()).run(data);
            input.remove(event.getPlayer().getUniqueId());
            event.setCancelled(true);
        }
    }

    /**
     * Input Listener
     *
     * @param event Event
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void input(PlayerCommandPreprocessEvent event) {
        if (!event.isCancelled() && enabled && input.keySet().contains(event.getPlayer().getUniqueId())) {
            YAMLSection data = new YAMLSection();
            data.set("message", event.getMessage());
            input.get(event.getPlayer().getUniqueId()).run(data);
            input.remove(event.getPlayer().getUniqueId());
            event.setCancelled(true);
        }
    }

    /**
     * GUI Close Listener
     *
     * @param event Event
     */
    @EventHandler
    public void close(InventoryCloseEvent event) {
        if (gui.keySet().contains(event.getPlayer().getUniqueId())) gui.get(event.getPlayer().getUniqueId()).open = false;
    }

    /**
     * Clean Renderers
     *
     * @param event Event
     */
    @EventHandler
    public void clean(PlayerQuitEvent event) {
        if (gui.keySet().contains(event.getPlayer().getUniqueId())) {
            gui.get(event.getPlayer().getUniqueId()).setDownloading(null);
            gui.remove(event.getPlayer().getUniqueId());
            input.remove(event.getPlayer().getUniqueId());
        }
    }
}

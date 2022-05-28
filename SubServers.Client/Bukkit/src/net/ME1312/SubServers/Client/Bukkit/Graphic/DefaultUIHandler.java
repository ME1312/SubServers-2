package net.ME1312.SubServers.Client.Bukkit.Graphic;

import net.ME1312.Galaxi.Library.Config.YAMLSection;
import net.ME1312.Galaxi.Library.Container.Container;
import net.ME1312.Galaxi.Library.Container.Value;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Try;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.SubData.Client.SubDataClient;
import net.ME1312.SubServers.Client.Bukkit.Network.Packet.PacketInExRunEvent;
import net.ME1312.SubServers.Client.Bukkit.SubPlugin;
import net.ME1312.SubServers.Client.Common.Network.API.Host;
import net.ME1312.SubServers.Client.Common.Network.API.Server;
import net.ME1312.SubServers.Client.Common.Network.API.SubServer;
import net.ME1312.SubServers.Client.Common.Network.Packet.PacketCreateServer;
import net.ME1312.SubServers.Client.Common.Network.Packet.PacketUpdateServer;

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
import java.util.function.Consumer;

import static net.ME1312.SubServers.Client.Bukkit.Library.ObjectPermission.permits;

/**
 * Default GUI Listener
 */
public class DefaultUIHandler implements UIHandler, Listener {
    private HashMap<UUID, Consumer<YAMLSection>> input = new HashMap<UUID, Consumer<YAMLSection>>();
    private HashMap<UUID, DefaultUIRenderer> gui = new HashMap<UUID, DefaultUIRenderer>();
    private boolean enabled = true;
    private SubPlugin plugin;

    /**
     * Creates a new Internal GUI Listener
     *
     * @param plugin Event
     */
    public DefaultUIHandler(SubPlugin plugin) {
        this.plugin = Util.nullpo(plugin);
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public DefaultUIRenderer getRenderer(Player player) {
        if (!gui.containsKey(player.getUniqueId())) gui.put(player.getUniqueId(), new DefaultUIRenderer(plugin, player.getUniqueId()));
        return gui.get(player.getUniqueId());
    }

    public void disable() {
        enabled = false;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void click(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        if (!event.isCancelled() && enabled && gui.containsKey(player.getUniqueId())) {
            DefaultUIRenderer gui = this.gui.get(player.getUniqueId());
            String title = event.getView().getTitle();
            
            if (gui.open && event.getClickedInventory() != null && title != null) {
                if (plugin.api.getSubDataNetwork()[0] == null) {
                    new IllegalStateException("SubData is not connected").printStackTrace();
                } else if (!Try.all.run(() -> plugin.api.getLangChannels())) {
                    new IllegalStateException("There are no lang options available at this time").printStackTrace();
                } else if (title.equals(plugin.api.getLang("SubServers", "Interface.Host-Menu.Title"))) { // Host Menu
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
                        } else if ((item.length() != 0 && !item.equals(ChatColor.RESET.toString())) && !item.equals(plugin.api.getLang("SubServers", "Interface.Host-Menu.No-Hosts"))) {
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
                } else if (title.startsWith(plugin.api.getLang("SubServers", "Interface.Host-Creator.Title").split("\\$str\\$")[0]) && // Host Creator
                        (plugin.api.getLang("SubServers", "Interface.Host-Creator.Title").split("\\$str\\$").length == 1 ||
                                title.endsWith(plugin.api.getLang("SubServers", "Interface.Host-Creator.Title").split("\\$str\\$")[1]))) {
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
                            if (permits(((UIRenderer.CreatorOptions) gui.lastVisitedObjects[0]).getHost(), player, "subservers.host.%.*", "subservers.host.%.create")) {
                                player.closeInventory();
                                gui.setDownloading(plugin.api.getLang("SubServers", "Interface.Generic.Downloading.Response"));
                                UIRenderer.CreatorOptions options = ((UIRenderer.CreatorOptions) gui.lastVisitedObjects[0]);
                                ((SubDataClient) plugin.api.getSubDataNetwork()[0]).sendPacket(new PacketCreateServer(player.getUniqueId(), options.getName(), options.getHost(), options.getTemplate(), options.getVersion(), options.getPort(), data -> {
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
                                if (m.getString("message").length() <= 0) {
                                    ((UIRenderer.CreatorOptions) gui.lastVisitedObjects[0]).setVersion(null);
                                } else ((UIRenderer.CreatorOptions) gui.lastVisitedObjects[0]).setVersion(new Version(m.getString("message")));
                                gui.hostCreator((UIRenderer.CreatorOptions) gui.lastVisitedObjects[0]);
                            });
                        } else if (ChatColor.stripColor(item).equals(ChatColor.stripColor(plugin.api.getLang("SubServers", "Interface.Host-Creator.Edit-Port")))) {
                            player.closeInventory();
                            if (!gui.sendTitle(plugin.api.getLang("SubServers", "Interface.Host-Creator.Edit-Port.Title"), 4 * 20))
                                player.sendMessage(plugin.api.getLang("SubServers", "Interface.Host-Creator.Edit-Port.Message"));
                            input.put(player.getUniqueId(), m -> {
                                if (m.getString("message").length() <= 0) {
                                    ((UIRenderer.CreatorOptions) gui.lastVisitedObjects[0]).setPort(null);
                                    gui.hostCreator((UIRenderer.CreatorOptions) gui.lastVisitedObjects[0]);
                                } else if (!Try.all.run(() -> Integer.parseInt(m.getString("message"))) || Integer.parseInt(m.getString("message")) <= 0 || Integer.parseInt(m.getString("message")) > 65535) {
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
                } else if (title.startsWith(plugin.api.getLang("SubServers", "Interface.Host-Creator.Edit-Template.Title").split("\\$str\\$")[0]) && // Host Creator Templates
                        (plugin.api.getLang("SubServers", "Interface.Host-Creator.Edit-Template.Title").split("\\$str\\$").length == 1 ||
                                title.endsWith(plugin.api.getLang("SubServers", "Interface.Host-Creator.Edit-Template.Title").split("\\$str\\$")[1]))) {
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
                        } else if ((item.length() != 0 && !item.equals(ChatColor.RESET.toString())) && !item.equals(plugin.api.getLang("SubServers", "Interface.Host-Creator.Edit-Template.No-Templates"))) {
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
                } else if (title.startsWith(plugin.api.getLang("SubServers", "Interface.Host-Plugin.Title").split("\\$str\\$")[0]) && // Host Plugin
                        (plugin.api.getLang("SubServers", "Interface.Host-Plugin.Title").split("\\$str\\$").length == 1 ||
                                title.endsWith(plugin.api.getLang("SubServers", "Interface.Host-Plugin.Title").split("\\$str\\$")[1]))) {
                    if (event.getCurrentItem() != null && event.getCurrentItem().getType() != Material.AIR && event.getCurrentItem().hasItemMeta()) {
                        String item = event.getCurrentItem().getItemMeta().getDisplayName();
                        if (item.equals(plugin.api.getLang("SubServers", "Interface.Generic.Back-Arrow"))) {
                            player.closeInventory();
                            gui.hostPlugin(gui.lastPage - 1, ((Host) gui.lastVisitedObjects[0]).getName());
                        } else if (item.equals(plugin.api.getLang("SubServers", "Interface.Generic.Next-Arrow"))) {
                            player.closeInventory();
                            gui.hostPlugin(gui.lastPage + 1, ((Host) gui.lastVisitedObjects[0]).getName());
                        } else if (item.equals(plugin.api.getLang("SubServers", "Interface.Generic.Back"))) {
                            player.closeInventory();
                            gui.back();
                        } else if ((item.length() != 0 && !item.equals(ChatColor.RESET.toString())) && !item.equals(plugin.api.getLang("SubServers", "Interface.Host-Plugin.No-Plugins"))) {
                            player.closeInventory();
                            final Value<PluginRenderer<Host>> plugin = new Container<PluginRenderer<Host>>(null);
                            for (PluginRenderer<Host> renderer : DefaultUIRenderer.hostPlugins.values()) {
                                if (item.equals(renderer.getIcon().getItemMeta().getDisplayName())) plugin.value(renderer);
                            }
                            if (plugin.value() == null) {
                                gui.reopen();
                            } else {
                                gui.setDownloading(ChatColor.stripColor(this.plugin.api.getLang("SubServers", "Interface.Host-Plugin.Title").replace("$str$", ((Host) gui.lastVisitedObjects[0]).getName())));
                                this.plugin.api.getHost(((Host) gui.lastVisitedObjects[0]).getName(), host -> {
                                    if (host != null) {
                                        gui.setDownloading(null);
                                        plugin.value().open(player, host);
                                    } else {
                                        gui.back();
                                    }
                                });
                            }
                        }
                    }

                } else if (title.equals(plugin.api.getLang("SubServers", "Interface.Group-Menu.Title"))) { // Group Menu
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
                        } else if (item.equals(plugin.api.getLang("SubServers", "Interface.Group-Menu.Ungrouped"))) {
                            player.closeInventory();
                            gui.serverMenu(1, null, "");
                        } else if (item.length() != 0 && !item.equals(ChatColor.RESET.toString())) {
                            player.closeInventory();
                            gui.serverMenu(1, null, ChatColor.stripColor(item));
                        }
                    }
                } else if (title.equals(plugin.api.getLang("SubServers", "Interface.Server-Menu.Title")) || // SubServer Menu
                        title.startsWith(plugin.api.getLang("SubServers", "Interface.Host-SubServer.Title").split("\\$str\\$")[0]) &&
                                (plugin.api.getLang("SubServers", "Interface.Host-SubServer.Title").split("\\$str\\$").length == 1 ||
                                        title.endsWith(plugin.api.getLang("SubServers", "Interface.Host-SubServer.Title").split("\\$str\\$")[1])) ||
                        title.startsWith(plugin.api.getLang("SubServers", "Interface.Group-SubServer.Title").split("\\$str\\$")[0]) &&
                                (plugin.api.getLang("SubServers", "Interface.Group-SubServer.Title").split("\\$str\\$").length == 1 ||
                                        title.endsWith(plugin.api.getLang("SubServers", "Interface.Group-SubServer.Title").split("\\$str\\$")[1])) ||
                        title.equals(plugin.api.getLang("SubServers", "Interface.Group-SubServer.Title-Ungrouped"))) {
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
                        } else if ((item.length() != 0 && !item.equals(ChatColor.RESET.toString())) && !item.equals(plugin.api.getLang("SubServers", "Interface.Server-Menu.No-Servers"))) {
                            player.closeInventory();
                            String obj;
                            if (event.getCurrentItem().getItemMeta().getLore() != null && event.getCurrentItem().getItemMeta().getLore().size() > 0 && event.getCurrentItem().getItemMeta().getLore().get((item.startsWith(ChatColor.WHITE.toString()))? 1 : 0).startsWith(ChatColor.GRAY.toString())) {
                                obj = ChatColor.stripColor(event.getCurrentItem().getItemMeta().getLore().get(0));
                            } else {
                                obj = ChatColor.stripColor(item);
                            }
                            gui.serverAdmin(obj);
                        }
                    }
                } else if (title.startsWith(plugin.api.getLang("SubServers", "Interface.Host-Admin.Title").split("\\$str\\$")[0]) && // Host Admin
                        (plugin.api.getLang("SubServers", "Interface.Host-Admin.Title").split("\\$str\\$").length == 1 ||
                                title.endsWith(plugin.api.getLang("SubServers", "Interface.Host-Admin.Title").split("\\$str\\$")[1]))) {
                    if (event.getCurrentItem() != null && event.getCurrentItem().getType() != Material.AIR && event.getCurrentItem().hasItemMeta()) {
                        String item = event.getCurrentItem().getItemMeta().getDisplayName();

                        if (item.equals(plugin.api.getLang("SubServers", "Interface.Generic.Back"))) {
                            player.closeInventory();
                            gui.back();
                        } else if (item.equals(plugin.api.getLang("SubServers", "Interface.Host-Admin.Creator"))) {
                            player.closeInventory();
                            if (player.hasPermission("subservers.host.create.*") || player.hasPermission("subservers.host.create." + ((Host) gui.lastVisitedObjects[0]).getName().toLowerCase())) {
                                gui.hostCreator(new UIRenderer.CreatorOptions(((Host) gui.lastVisitedObjects[0]).getName()));
                            } else gui.reopen();
                        } else if (item.equals(plugin.api.getLang("SubServers", "Interface.Host-Admin.SubServers"))) {
                            player.closeInventory();
                            gui.serverMenu(1, ((Host) gui.lastVisitedObjects[0]).getName(), null);
                        } else if (item.equals(plugin.api.getLang("SubServers", "Interface.Host-Admin.Plugins"))) {
                            player.closeInventory();
                            gui.hostPlugin(1, ((Host) gui.lastVisitedObjects[0]).getName());
                        }
                    }
                } else if (title.startsWith(plugin.api.getLang("SubServers", "Interface.Server-Admin.Title").split("\\$str\\$")[0]) && // SubServer Admin
                        (plugin.api.getLang("SubServers", "Interface.Server-Admin.Title").split("\\$str\\$").length == 1 ||
                                title.endsWith(plugin.api.getLang("SubServers", "Interface.Server-Admin.Title").split("\\$str\\$")[1]))) {
                    if (event.getCurrentItem() != null && event.getCurrentItem().getType() != Material.AIR && event.getCurrentItem().hasItemMeta()) {
                        String item = event.getCurrentItem().getItemMeta().getDisplayName();

                        if (item.equals(plugin.api.getLang("SubServers", "Interface.Generic.Back"))) {
                            player.closeInventory();
                            gui.back();
                        } else if (item.equals(plugin.api.getLang("SubServers", "Interface.Server-Admin.Update"))) {
                            player.closeInventory();
                            if (permits((Server) gui.lastVisitedObjects[0], player, "subservers.subserver.%.*", "subservers.subserver.%.update")) {
                                if (!gui.sendTitle(plugin.api.getLang("SubServers", "Interface.Server-Admin.Update.Title"), 4 * 20))
                                    player.sendMessage(plugin.api.getLang("SubServers", "Interface.Server-Admin.Update.Message"));
                                input.put(player.getUniqueId(), m -> {
                                    gui.setDownloading(plugin.api.getLang("SubServers", "Interface.Generic.Downloading.Response"));
                                    ((SubDataClient) plugin.api.getSubDataNetwork()[0]).sendPacket(new PacketUpdateServer(player.getUniqueId(), ((Server) gui.lastVisitedObjects[0]).getName(),
                                            null, (m.getString("message").length() == 0 || m.getString("message").equals("/"))?null:new Version((m.getString("message").startsWith("/"))?m.getString("message").substring(1):m.getString("message")), data -> {
                                        gui.reopen();
                                    }));
                                });
                            } else gui.reopen();
                        } else if (item.equals(plugin.api.getLang("SubServers", "Interface.Server-Admin.Start"))) {
                            player.closeInventory();
                            if (permits((Server) gui.lastVisitedObjects[0], player, "subservers.subserver.%.*", "subservers.subserver.%.start")) {
                                gui.setDownloading(plugin.api.getLang("SubServers", "Interface.Generic.Downloading.Response"));
                                ((SubServer) gui.lastVisitedObjects[0]).start(player.getUniqueId(), response -> {
                                    gui.setDownloading(plugin.api.getLang("SubServers", "Interface.Server-Admin.Start.Title"));
                                    Bukkit.getScheduler().runTaskLater(plugin, gui::reopen, 30);
                                });
                            } else gui.reopen();
                        } else if (item.equals(plugin.api.getLang("SubServers", "Interface.Server-Admin.Stop"))) {
                            player.closeInventory();
                            if (permits((Server) gui.lastVisitedObjects[0], player, "subservers.subserver.%.*", "subservers.subserver.%.stop")) {
                                gui.setDownloading(plugin.api.getLang("SubServers", "Interface.Generic.Downloading.Response"));
                                final Value<Boolean> listening = new Container<Boolean>(true);
                                PacketInExRunEvent.callback("SubStoppedEvent", new Consumer<ObjectMap<String>>() {
                                    @Override
                                    public void accept(ObjectMap<String> json) {
                                        try {
                                            if (listening.value()) if (!json.getString("server").equalsIgnoreCase(((Server) gui.lastVisitedObjects[0]).getName())) {
                                                PacketInExRunEvent.callback("SubStoppedEvent", this);
                                            } else {
                                                Bukkit.getScheduler().runTaskLater(plugin, gui::reopen, 5);
                                            }
                                        } catch (Exception e) {}
                                    }
                                });
                                ((SubServer) gui.lastVisitedObjects[0]).stop(player.getUniqueId(), response -> {
                                    if (response != 0) {
                                        gui.reopen();
                                        listening.value(false);
                                    } else gui.setDownloading(plugin.api.getLang("SubServers", "Interface.Server-Admin.Stop.Title").replace("$str$", ((Server) gui.lastVisitedObjects[0]).getName()));
                                });
                            } else gui.reopen();
                        } else if (item.equals(plugin.api.getLang("SubServers", "Interface.Server-Admin.Terminate"))) {
                            player.closeInventory();
                            if (permits((Server) gui.lastVisitedObjects[0], player, "subservers.subserver.%.*", "subservers.subserver.%.terminate")) {
                                gui.setDownloading(plugin.api.getLang("SubServers", "Interface.Generic.Downloading.Response"));
                                final Value<Boolean> listening = new Container<Boolean>(true);
                                PacketInExRunEvent.callback("SubStoppedEvent", new Consumer<ObjectMap<String>>() {
                                    @Override
                                    public void accept(ObjectMap<String> json) {
                                        try {
                                            if (listening.value()) if (!json.getString("server").equalsIgnoreCase(((Server) gui.lastVisitedObjects[0]).getName())) {
                                                PacketInExRunEvent.callback("SubStoppedEvent", this);
                                            } else {
                                                gui.reopen();
                                            }
                                        } catch (Exception e) {}
                                    }
                                });
                                ((SubServer) gui.lastVisitedObjects[0]).terminate(player.getUniqueId(), response -> {
                                    if (response != 0) {
                                        gui.reopen();
                                        listening.value(false);
                                    } else gui.setDownloading(plugin.api.getLang("SubServers", "Interface.Server-Admin.Terminate.Title").replace("$str$", ((Server) gui.lastVisitedObjects[0]).getName()));
                                });
                            } else gui.reopen();
                        } else if (item.equals(plugin.api.getLang("SubServers", "Interface.Server-Admin.Command"))) {
                            player.closeInventory();
                            if (permits((Server) gui.lastVisitedObjects[0], player, "subservers.subserver.%.*", "subservers.subserver.%.command")) {
                                if (!gui.sendTitle(plugin.api.getLang("SubServers", "Interface.Server-Admin.Command.Title"), 4 * 20))
                                    player.sendMessage(plugin.api.getLang("SubServers", "Interface.Server-Admin.Command.Message"));
                                input.put(player.getUniqueId(), m -> {
                                    gui.setDownloading(plugin.api.getLang("SubServers", "Interface.Generic.Downloading.Response"));
                                    ((Server) gui.lastVisitedObjects[0]).command(player.getUniqueId(), (m.getString("message").startsWith("/"))?m.getString("message").substring(1):m.getString("message"), response -> {
                                        gui.reopen();
                                    });
                                });
                            } else gui.reopen();
                        } else if (item.equals(plugin.api.getLang("SubServers", "Interface.Server-Admin.Plugins"))) {
                            player.closeInventory();
                            gui.serverPlugin(1, ((Server) gui.lastVisitedObjects[0]).getName());
                        }
                    }
                } else if (title.startsWith(plugin.api.getLang("SubServers", "Interface.SubServer-Plugin.Title").split("\\$str\\$")[0]) && // SubServer Plugin
                        (plugin.api.getLang("SubServers", "Interface.SubServer-Plugin.Title").split("\\$str\\$").length == 1 ||
                                title.endsWith(plugin.api.getLang("SubServers", "Interface.SubServer-Plugin.Title").split("\\$str\\$")[1]))) {
                    if (event.getCurrentItem() != null && event.getCurrentItem().getType() != Material.AIR && event.getCurrentItem().hasItemMeta()) {
                        String item = event.getCurrentItem().getItemMeta().getDisplayName();
                        if (item.equals(plugin.api.getLang("SubServers", "Interface.Generic.Back-Arrow"))) {
                            player.closeInventory();
                            gui.serverPlugin(gui.lastPage - 1, ((Server) gui.lastVisitedObjects[0]).getName());
                        } else if (item.equals(plugin.api.getLang("SubServers", "Interface.Generic.Next-Arrow"))) {
                            player.closeInventory();
                            gui.serverPlugin(gui.lastPage + 1, ((Server) gui.lastVisitedObjects[0]).getName());
                        } else if (item.equals(plugin.api.getLang("SubServers", "Interface.Generic.Back"))) {
                            player.closeInventory();
                            gui.back();
                        } else if ((item.length() != 0 && !item.equals(ChatColor.RESET.toString())) && !item.equals(plugin.api.getLang("SubServers", "Interface.SubServer-Plugin.No-Plugins"))) {
                            player.closeInventory();
                            Value<PluginRenderer<Server>> plugin = new Container<PluginRenderer<Server>>(null);
                            for (PluginRenderer<Server> renderer : DefaultUIRenderer.serverPlugins.values()) {
                                if (item.equals(renderer.getIcon().getItemMeta().getDisplayName())) plugin.value(renderer);
                            }
                            if (plugin.value() == null) {
                                gui.reopen();
                            } else {
                                gui.setDownloading(ChatColor.stripColor(this.plugin.api.getLang("SubServers", "Interface.SubServer-Plugin.Title").replace("$str$", ((Server) gui.lastVisitedObjects[0]).getName())));
                                this.plugin.api.getSubServer(((Server) gui.lastVisitedObjects[0]).getName(), subserver -> {
                                    if (subserver != null) {
                                        gui.setDownloading(null);
                                        plugin.value().open(player, subserver);
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
        if (!event.isCancelled() && enabled && input.containsKey(event.getPlayer().getUniqueId())) {
            YAMLSection data = new YAMLSection();
            data.set("message", event.getMessage());
            input.get(event.getPlayer().getUniqueId()).accept(data);
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
        if (!event.isCancelled() && enabled && input.containsKey(event.getPlayer().getUniqueId())) {
            YAMLSection data = new YAMLSection();
            data.set("message", (event.getMessage().startsWith("/"))?event.getMessage().substring(1):event.getMessage());
            input.get(event.getPlayer().getUniqueId()).accept(data);
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
        if (gui.containsKey(event.getPlayer().getUniqueId())) gui.get(event.getPlayer().getUniqueId()).open = false;
    }

    /**
     * Clean Renderers
     *
     * @param event Event
     */
    @EventHandler
    public void clean(PlayerQuitEvent event) {
        if (gui.containsKey(event.getPlayer().getUniqueId())) {
            gui.get(event.getPlayer().getUniqueId()).setDownloading(null);
            gui.remove(event.getPlayer().getUniqueId());
            input.remove(event.getPlayer().getUniqueId());
        }
    }
}

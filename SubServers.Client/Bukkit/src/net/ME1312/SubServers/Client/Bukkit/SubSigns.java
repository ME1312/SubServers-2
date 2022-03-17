package net.ME1312.SubServers.Client.Bukkit;

import net.ME1312.SubData.Client.Library.EscapedOutputStream;
import net.ME1312.SubServers.Client.Bukkit.Event.SubStartEvent;
import net.ME1312.SubServers.Client.Bukkit.Event.SubStartedEvent;
import net.ME1312.SubServers.Client.Bukkit.Event.SubStopEvent;
import net.ME1312.SubServers.Client.Bukkit.Event.SubStoppedEvent;
import net.ME1312.SubServers.Client.Bukkit.Library.Compatibility.OfflineBlock;
import net.ME1312.SubServers.Client.Common.Network.API.SubServer;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.WorldLoadEvent;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.UUID;

/**
 * SubServers Signs Class
 */
public class SubSigns implements Listener {
    private static final HashMap<OfflineBlock, String> data = new HashMap<OfflineBlock, String>();
    private static HashMap<Location, String> signs = new HashMap<Location, String>();
    private static File file;
    private final SubPlugin plugin;
    private boolean active = false;

    SubSigns(SubPlugin plugin, File file) throws IOException {
        this.plugin = plugin;
        SubSigns.file = file;
        load();
    }

    public static void save() throws IOException {
        if (!data.isEmpty() || (file.exists() && !file.delete())) {
            FileOutputStream raw = new FileOutputStream(file, false);
            EscapedOutputStream escaped = new EscapedOutputStream(raw, '\u001B', '\u0003');
            for (Entry<OfflineBlock, String> sign : data.entrySet()) {
                raw.write(ByteBuffer.allocate(28).order(ByteOrder.BIG_ENDIAN)
                        .putLong(sign.getKey().world.getMostSignificantBits())
                        .putLong(sign.getKey().world.getLeastSignificantBits())
                        .putInt(sign.getKey().x)
                        .putInt(sign.getKey().y)
                        .putInt(sign.getKey().z)
                        .array()
                );
                escaped.write(sign.getValue().getBytes(StandardCharsets.UTF_8));
                escaped.control('\u0003');
            }
        }
    }

    private void load() throws IOException {
        if (file.exists()) {
            FileInputStream in = new FileInputStream(file);
            ByteArrayOutputStream string = new ByteArrayOutputStream();
            ByteBuffer magic = ByteBuffer.allocate(28).order(ByteOrder.BIG_ENDIAN);

            boolean escaped = false;
            int b, i = 0;
            while ((b = in.read()) != -1) {
                if (i < 28) {
                    magic.put((byte) b);
                    ++i;
                } else if (escaped) {
                    switch (b) {
                        case '\u001B': // [ESC] (Escape character)
                            string.write('\u001B');
                            break;
                        case '\u0003': // [ETX] (End of String character)
                            magic.position(0);
                            String server = string.toString(StandardCharsets.UTF_8.name());
                            OfflineBlock location = new OfflineBlock(new UUID(magic.getLong(), magic.getLong()), magic.getInt(), magic.getInt(), magic.getInt());
                            Location loaded = location.load();
                            if (loaded == null) {
                                data.put(location, server);
                            } else if (loaded.getBlock().getState() instanceof Sign) {
                                data.put(location, server);
                                signs.put(loaded, server);
                            } else {
                                Bukkit.getLogger().warning("SubServers > Removed invalid sign data: [\"" + loaded.getWorld().getName() + "\", " + location.x + ", " + location.y + ", " + location.z + "] -> \"" + server + '\"');
                            }
                            magic.clear();
                            string.reset();
                            i = 0;
                            break;
                        default:
                            string.write('\u001B');
                            string.write(b);
                            break;
                    }
                    escaped = false;
                } else if (b == '\u001B') {
                    escaped = true;
                } else {
                    string.write(b);
                }
            }
            listen();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void load(WorldLoadEvent e) {
        UUID wid = e.getWorld().getUID();
        ArrayList<OfflineBlock> removals = new ArrayList<OfflineBlock>();
        HashMap<Location, String> signs = new HashMap<Location, String>(SubSigns.signs);
        for (Entry<OfflineBlock, String> sign : data.entrySet()) {
            if (wid == sign.getKey().world) {
                OfflineBlock location = sign.getKey();
                Location loaded = location.load();
                if (loaded.getBlock().getState() instanceof Sign) {
                    signs.put(loaded, sign.getValue());
                } else {
                    removals.add(sign.getKey());
                    Bukkit.getLogger().warning("SubServers > Removed invalid sign data: [\"" + loaded.getWorld().getName() + "\", " + location.x + ", " + location.y + ", " + location.z + "] -> \"" + sign.getValue() + '\"');
                }
            }
        }
        SubSigns.signs = signs;
        for (OfflineBlock location : removals) {
            data.remove(location);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void create(SignChangeEvent e) {
        if (!e.isCancelled() && plugin.lang != null && e.getLine(0).trim().equalsIgnoreCase("[SubServers]")) {
            Player player = e.getPlayer();
            String server = e.getLine(1).trim();
            if (server.length() > 0 && player.hasPermission("subservers.signs")) {
                Location pos = e.getBlock().getLocation();
                if (pos.getBlock().getState() instanceof Sign) {
                    HashMap<Location, String> signs = new HashMap<Location, String>(SubSigns.signs);
                    signs.put(pos, server);
                    SubSigns.signs = signs;
                    data.put(new OfflineBlock(pos), server);

                    listen();
                    refresh(e.getBlock(), server, null);
                    Bukkit.getLogger().info("SubServers > Server sign created: [\"" + pos.getWorld().getName() + "\", " + pos.getBlockX() + ", " + pos.getBlockY() + ", " + pos.getBlockZ() + "] -> \"" + server + '\"');
                    player.sendMessage(plugin.api.getLang("SubServers", "Signs.Create"));
                }
            }
        }
    }

    private void listen() {
        if (!active && !signs.isEmpty()) {
            active = true;
            plugin.phi.listen(this::refresh);
            plugin.phi.start();
        }
    }

    private void refresh() {
        if (plugin.lang != null) {
            for (Entry<Location, String> pos : signs.entrySet()) {
                refresh(pos.getKey().getBlock(), pos.getValue(), null);
            }
        }
    }

    private void refresh(String name, String lang) {
        if (plugin.lang != null) {
            for (Entry<Location, String> pos : signs.entrySet()) {
                if (pos.getValue().equalsIgnoreCase(name)) refresh(pos.getKey().getBlock(), pos.getValue(), lang);
            }
        }
    }

    private void refresh(Block block, String name, String lang) {
        if (block.getState() instanceof Sign) {
            Sign sign = (Sign) block.getState();
            SubServer server = plugin.phi.cache.getSubServer(name);
            if (lang == null) {
                if (server == null) {
                    lang = "Signs.Text.Error";
                } else if (server.isStopping()) {
                    lang = "Signs.Text.Stopping";
                } else if (server.isOnline()) {
                    lang = "Signs.Text.Online";
                } else if (server.isRunning()) {
                    lang = "Signs.Text.Starting";
                } else {
                    lang = "Signs.Text.Offline";
                }
            }
            String[] text = plugin.phi.replace(null, plugin.api.getLang("SubServers", lang).replace("$str$", name)).split("\n", 4);
            for (int i = 0; i < 4; ++i) if (i < text.length) {
                sign.setLine(i, text[i]);
            } else {
                sign.setLine(i, "");
            }
            Bukkit.getScheduler().runTask(plugin, sign::update);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void interact(PlayerInteractEvent e) {
        if (!e.isCancelled() && e.getClickedBlock() != null && plugin.lang != null && plugin.api.getSubDataNetwork()[0] != null && !plugin.api.getSubDataNetwork()[0].isClosed() && signs.containsKey(e.getClickedBlock().getLocation())) {
            Player player = e.getPlayer();
            if ((e.getAction() == Action.RIGHT_CLICK_BLOCK || !player.hasPermission("subservers.signs")) && player.hasPermission("subservers.teleport")) {
                SubServer server = plugin.phi.cache.getSubServer(signs.get(e.getClickedBlock().getLocation()));
                if (server != null) {
                    if (!server.isRunning()) {
                        if (server.isEnabled() && server.isAvailable() && server.getCurrentIncompatibilities().size() == 0) server.start();
                    } else if (!server.isStopping()) {
                        player.sendMessage(plugin.api.getLang("SubServers", "Command.Teleport").replace("$name$", player.getName()).replace("$str$", server.getDisplayName()));
                        plugin.pmc(player, "Connect", server.getName());
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void delete(BlockBreakEvent e) {
        if (!e.isCancelled() && e.getBlock().getState() instanceof Sign && signs.containsKey(e.getBlock().getLocation())) {
            Player player = e.getPlayer();
            if (plugin.lang != null && player.hasPermission("subservers.signs")) {
                Location pos = e.getBlock().getLocation();

                HashMap<Location, String> signs = new HashMap<Location, String>(SubSigns.signs);
                signs.remove(pos);
                SubSigns.signs = signs;
                data.remove(new OfflineBlock(pos));

                player.sendMessage(plugin.api.getLang("SubServers", "Signs.Delete"));
            } else {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void start(SubStartEvent e) {
        refresh(e.getServer(), "Signs.Text.Starting");
    }

    @EventHandler
    public void started(SubStartedEvent e) {
        refresh(e.getServer(), "Signs.Text.Online");
    }

    @EventHandler
    public void stopping(SubStopEvent e) {
        refresh(e.getServer(), "Signs.Text.Stopping");
    }

    @EventHandler
    public void stopped(SubStoppedEvent e) {
        refresh(e.getServer(), "Signs.Text.Offline");
    }
}

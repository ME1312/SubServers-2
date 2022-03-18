package net.ME1312.SubServers.Client.Bukkit;

import net.ME1312.Galaxi.Library.Container.Pair;
import net.ME1312.SubData.Client.Library.EscapedOutputStream;
import net.ME1312.SubServers.Client.Bukkit.Event.SubStartEvent;
import net.ME1312.SubServers.Client.Bukkit.Event.SubStartedEvent;
import net.ME1312.SubServers.Client.Bukkit.Event.SubStopEvent;
import net.ME1312.SubServers.Client.Bukkit.Event.SubStoppedEvent;
import net.ME1312.SubServers.Client.Bukkit.Library.Compatibility.OfflineBlock;
import net.ME1312.SubServers.Client.Common.Network.API.Host;
import net.ME1312.SubServers.Client.Common.Network.API.Server;
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
import java.text.NumberFormat;
import java.util.*;
import java.util.Map.Entry;

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
                            String object = string.toString(StandardCharsets.UTF_8.name());
                            OfflineBlock location = new OfflineBlock(new UUID(magic.getLong(), magic.getLong()), magic.getInt(), magic.getInt(), magic.getInt());
                            Location loaded = location.load();
                            if (loaded == null) {
                                data.put(location, object);
                            } else if (loaded.getBlock().getState() instanceof Sign) {
                                data.put(location, object);
                                signs.put(loaded, object);
                            } else {
                                Bukkit.getLogger().warning("SubServers > Removed invalid sign data: [\"" + loaded.getWorld().getName() + "\", " + location.x + ", " + location.y + ", " + location.z + "] -> \"" + object + '\"');
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
            String object = e.getLine(1).trim();
            if (object.length() > 0 && player.hasPermission("subservers.signs")) {
                Location pos = e.getBlock().getLocation();
                if (pos.getBlock().getState() instanceof Sign) {
                    HashMap<Location, String> signs = new HashMap<Location, String>(SubSigns.signs);
                    signs.put(pos, object);
                    SubSigns.signs = signs;
                    data.put(new OfflineBlock(pos), object);

                    listen();
                    refresh(e.getBlock(), object);
                    Bukkit.getLogger().info("SubServers > Server sign created: [\"" + pos.getWorld().getName() + "\", " + pos.getBlockX() + ", " + pos.getBlockY() + ", " + pos.getBlockZ() + "] -> \"" + object + '\"');
                    player.sendMessage(plugin.api.getLang("SubServers", "Signs.Create"));
                }
            }
        }
    }

    private Object translate(String name) {
        if (name.startsWith("::")) {
            return plugin.phi.cache.getHost(name.substring(2));
        } else if (name.startsWith(":")) {
            return plugin.phi.cache.getGroup(name.substring(1));
        } else {
            return plugin.phi.cache.getServer(name);
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
                refresh(pos.getKey().getBlock(), pos.getValue());
            }
        }
    }

    private void refresh(SubServer server) {
        if (plugin.lang != null) {
            String name;
            for (Entry<Location, String> pos : signs.entrySet()) {
                if ((name = pos.getValue()).equalsIgnoreCase(server.getName())) {
                    refresh(pos.getKey().getBlock(), server);
                } else if (name.equalsIgnoreCase("::" + server.getHost())) {
                    refresh(pos.getKey().getBlock(), plugin.phi.cache.getHost(server.getHost()));
                } else {
                    for (String group : server.getGroups()) {
                        if (name.equalsIgnoreCase(':' + group)) {
                            refresh(pos.getKey().getBlock(), plugin.phi.cache.getGroup(group));
                            break;
                        }
                    }
                }
            }
        }
    }

    private enum Text {
        UNKNOWN(0, "Signs.Text.Unknown"),
        OFFLINE(1, "Signs.Text.Offline"),
        STARTING(3, "Signs.Text.Starting"),
        ONLINE(4, "Signs.Text.Online"),
        STOPPING(2, "Signs.Text.Stopping"),
        ;
        private final byte priority;
        private final String text;
        Text(int priority, String text) {
            this.priority = (byte) priority;
            this.text = text;
        }

        private static Text determine(SubServer server) {
            if (!server.isRunning()) {
                return Text.OFFLINE;
            } else if (server.isStopping()) {
                return Text.STOPPING;
            } else if (server.isOnline()) {
                return Text.ONLINE;
            } else {
                return Text.STARTING;
            }
        }

        private static Text determine(Server server) {
            if (server instanceof SubServer) {
                return determine((SubServer) server);
            } else if (server.getSubData()[0] == null) {
                return Text.UNKNOWN;
            } else {
                return Text.ONLINE;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void refresh(Block block, Object object) {
        if (block.getState() instanceof Sign) {
            Sign sign = (Sign) block.getState();
            Text state = Text.UNKNOWN;
            String name = "null";
            int players = 0;

            // re-define the object
            if (object instanceof String) {
                object = translate(name = (String) object);
            }

            // read the object
            if (object instanceof Host) {
                Host host = (Host) object;
                name = host.getDisplayName();

                Text incoming;
                for (SubServer server : ((Host) object).getSubServers().values()) {
                    players += server.getRemotePlayers().size();
                    incoming = Text.determine(server);
                    if (incoming.priority > state.priority)
                        state = incoming;
                }
            } else if (object instanceof Pair) {
                Pair<String, List<Server>> group = (Pair<String, List<Server>>) object;
                name = group.key();

                Text incoming;
                for (Server server : group.value()) {
                    players += server.getRemotePlayers().size();
                    incoming = Text.determine(server);
                    if (incoming.priority > state.priority)
                        state = incoming;
                }
            } else if (object instanceof Server) {
                Server server = (Server) object;
                state = Text.determine(server);
                name = server.getDisplayName();
                players = server.getRemotePlayers().size();
            }

            // update the sign
            String[] text = plugin.phi.replace(null, plugin.api.getLang("SubServers", state.text).replace("$str$", name).replace("$int$", NumberFormat.getInstance().format(players))).split("\n", 4);
            for (int i = 0; i < 4; ++i) if (i < text.length) {
                sign.setLine(i, text[i]);
            } else {
                sign.setLine(i, "");
            }
            Bukkit.getScheduler().runTask(plugin, sign::update);
        }
    }

    @SuppressWarnings("unchecked")
    @EventHandler(priority = EventPriority.MONITOR)
    public void interact(PlayerInteractEvent e) {
        if (!e.isCancelled() && e.getClickedBlock() != null && plugin.lang != null && plugin.api.getSubDataNetwork()[0] != null && !plugin.api.getSubDataNetwork()[0].isClosed() && signs.containsKey(e.getClickedBlock().getLocation())) {
            Player player = e.getPlayer();
            if ((e.getAction() == Action.RIGHT_CLICK_BLOCK || !player.hasPermission("subservers.signs")) && player.hasPermission("subservers.teleport")) {
                Object object = translate(signs.get(e.getClickedBlock().getLocation()));

                Collection<? extends Server> servers;
                if (object instanceof Host) {
                    servers = ((Host) object).getSubServers().values();
                } else if (object instanceof Pair) {
                    servers = ((Pair<String, List<Server>>) object).value();
                } else if (object instanceof Server) {
                    servers = Collections.singleton((Server) object);
                } else {
                    return;
                }

                Text incoming, state = Text.UNKNOWN;
                List<Server> selected = new ArrayList<>();
                for (Server server : servers) {
                    incoming = Text.determine(server);
                    if (incoming.priority > state.priority) {
                        selected.clear();
                        state = incoming;
                    }

                    if (incoming == state) {
                        if (state == Text.OFFLINE) {
                            SubServer subserver = (SubServer) server;
                            if (!(subserver.isEnabled() && subserver.isAvailable() && subserver.getCurrentIncompatibilities().size() == 0)) continue;
                        }
                        selected.add(server);
                    }
                }

                if (selected.size() > 0) {
                    Server server = selected.get(new Random().nextInt(selected.size()));
                    if (state == Text.OFFLINE) {
                        ((SubServer) server).start();
                    } else {
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

    @EventHandler(priority = EventPriority.MONITOR)
    public void start(SubStartEvent e) {
        refresh(plugin.phi.cache.getSubServer(e.getServer()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void started(SubStartedEvent e) {
        refresh(plugin.phi.cache.getSubServer(e.getServer()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void stopping(SubStopEvent e) {
        refresh(plugin.phi.cache.getSubServer(e.getServer()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void stopped(SubStoppedEvent e) {
        refresh(plugin.phi.cache.getSubServer(e.getServer()));
    }
}

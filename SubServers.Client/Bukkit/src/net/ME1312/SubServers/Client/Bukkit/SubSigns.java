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

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Supplier;

/**
 * SubServers Signs Class
 */
public class SubSigns implements Listener {
    private static final HashMap<OfflineBlock, String> data = new HashMap<OfflineBlock, String>();
    private static final HashMap<String, Location> locations = new HashMap<String, Location>();
    private static HashMap<Location, Supplier<?>> signs = new HashMap<Location, Supplier<?>>();
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
                            String name = string.toString(StandardCharsets.UTF_8.name());
                            OfflineBlock location = new OfflineBlock(new UUID(magic.getLong(), magic.getLong()), magic.getInt(), magic.getInt(), magic.getInt());
                            Location loaded = location.load();
                            if (loaded == null) {
                                data.put(location, name);
                            } else if (loaded.getBlock().getState() instanceof Sign) {
                                data.put(location, name);
                                signs.put(loaded, translate(name));
                                locations.put(name.toLowerCase(), loaded);
                            } else {
                                Bukkit.getLogger().warning("SubServers > Removed invalid sign data: [\"" + loaded.getWorld().getName() + "\", " + location.x + ", " + location.y + ", " + location.z + "] -> \"" + name + '\"');
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
    public void create(SignChangeEvent e) {
        if (!e.isCancelled() && e.getLine(0).trim().equalsIgnoreCase("[SubServers]") && plugin.lang != null) {
            Player player = e.getPlayer();
            String name = e.getLine(1).trim();
            if (name.length() > 0 && player.hasPermission("subservers.signs")) {
                Location location = e.getBlock().getLocation();
                if (location.getBlock().getState() instanceof Sign) {
                    HashMap<Location, Supplier<?>> signs = new HashMap<Location, Supplier<?>>(SubSigns.signs);
                    Supplier<?> translator = translate(name);
                    signs.put(location, translator);
                    SubSigns.data.put(new OfflineBlock(location), name);
                    SubSigns.signs = signs;
                    SubSigns.locations.put(name.toLowerCase(), location);

                    listen();
                    refresh(e.getBlock(), translator);
                    Bukkit.getLogger().info("SubServers > Server sign created: [\"" + location.getWorld().getName() + "\", " + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ() + "] -> \"" + name + '\"');
                    player.sendMessage(plugin.api.getLang("SubServers", "Signs.Create"));
                }
            }
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Supplier<?> translate(String name) {
        if (name.startsWith("::") && name.length() > 2) {
            final String translated = name.substring(2).toLowerCase();
            return () -> ((Map<String, Object>) (Map) plugin.phi.cache.getHosts()).getOrDefault(translated, name);
        } else if (name.startsWith(":") && name.length() > 1) {
            final String translated = name.substring(1);
            return () -> {
                Pair<String, List<Server>> group = plugin.phi.cache.getGroup(translated);
                return (group == null)? name : group;
            };
        } else {
            final String translated = name.toLowerCase();
            return () -> ((Map<String, Object>) (Map) plugin.phi.cache.getServers()).getOrDefault(translated, name);
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
            for (Entry<Location, Supplier<?>> sign : signs.entrySet()) {
                refresh(sign.getKey().getBlock(), sign.getValue());
            }
        }
    }

    private void refresh(SubServer server) {
        if (server != null && plugin.lang != null) {
            Location location = locations.get(server.getName().toLowerCase());
            if (location != null) {
                refresh(location.getBlock(), () -> server);
            }
            for (String group : server.getGroups()) {
                if ((location = locations.get(':' + group.toLowerCase())) != null) {
                    refresh(location.getBlock(), signs.get(location));
                }
            }
            if ((location = locations.get("::" + server.getHost().toLowerCase())) != null) {
                refresh(location.getBlock(), signs.get(location));
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
    private void refresh(Block block, Supplier<?> translator) {
        if (block.getState() instanceof Sign) {
            Object object = translator.get();
            String name;
            int players = 0;

            Sign sign = (Sign) block.getState();
            Text state = Text.UNKNOWN;

            if (object instanceof Server) {
                Server server = (Server) object;
                state = Text.determine(server);
                name = server.getDisplayName();
                players = server.getRemotePlayers().size();

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
            } else if (object instanceof Host) {
                Host host = (Host) object;
                name = host.getDisplayName();

                Text incoming;
                for (SubServer server : host.getSubServers().values()) {
                    players += server.getRemotePlayers().size();
                    incoming = Text.determine(server);
                    if (incoming.priority > state.priority)
                        state = incoming;
                }
            } else if (object instanceof String) {
                name = (String) object;
            } else {
                return;
            }

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
        if (!e.isCancelled() && e.getClickedBlock() != null && signs.containsKey(e.getClickedBlock().getLocation()) && plugin.lang != null) {
            Player player = e.getPlayer();
            if (player.hasPermission("subservers.teleport") && (e.getAction() == Action.RIGHT_CLICK_BLOCK || !player.hasPermission("subservers.signs"))) {
                Object object = signs.get(e.getClickedBlock().getLocation()).get();

                Collection<? extends Server> servers;
                if (object instanceof Server) {
                    servers = Collections.singleton((Server) object);
                } else if (object instanceof Pair) {
                    servers = ((Pair<String, List<Server>>) object).value();
                } else if (object instanceof Host) {
                    servers = ((Host) object).getSubServers().values();
                } else {
                    return;
                }

                Text incoming, state = Text.UNKNOWN;
                List<Server> selected = new ArrayList<>();
                for (Server server : servers) {
                    incoming = Text.determine(server);
                    if (incoming != Text.STOPPING) {
                        if (incoming == Text.OFFLINE) {
                            SubServer subserver = (SubServer) server;
                            if (!(subserver.isEnabled() && subserver.isAvailable() && subserver.getCurrentIncompatibilities().size() == 0)) continue;
                        }

                        if (incoming.priority > state.priority) {
                            selected.clear();
                            state = incoming;
                        }

                        if (incoming == state) {
                            selected.add(server);
                        }
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
                Location location = e.getBlock().getLocation();
                String name = data.remove(new OfflineBlock(location));
                if (name != null) locations.remove(name.toLowerCase());

                HashMap<Location, Supplier<?>> signs = new HashMap<Location, Supplier<?>>(SubSigns.signs);
                signs.remove(location);
                SubSigns.signs = signs;

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

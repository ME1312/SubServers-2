package net.ME1312.SubServers.Client.Bukkit.SubSigns;

import net.ME1312.Galaxi.Library.Container.Pair;
import net.ME1312.SubData.Client.Library.EscapedOutputStream;
import net.ME1312.SubServers.Client.Bukkit.Library.Compatibility.OfflineBlock;
import net.ME1312.SubServers.Client.Bukkit.SubPlugin;
import net.ME1312.SubServers.Client.Bukkit.SubSigns.Listeners.SignListeners;
import net.ME1312.SubServers.Client.Bukkit.SubSigns.Listeners.SubServerListeners;
import net.ME1312.SubServers.Client.Common.Network.API.Host;
import net.ME1312.SubServers.Client.Common.Network.API.Server;
import net.ME1312.SubServers.Client.Common.Network.API.SubServer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.event.Listener;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * SubServers Signs Class
 */
public class SubSigns implements Listener {
    private final Map<OfflineBlock, String> data = new HashMap<>();
    private final Map<String, Location> locations = new HashMap<>();
    private Map<Location, Supplier<?>> signs = new HashMap<>();
    private File file;
    private final SubPlugin plugin;
    private boolean active = false;

    public SubSigns(SubPlugin plugin, File file) throws IOException {
        this.plugin = plugin;
        this.file = file;

        plugin.getServer().getPluginManager().registerEvents(new SignListeners(plugin, this), plugin);
        plugin.getServer().getPluginManager().registerEvents(new SubServerListeners(plugin, this), plugin);

        load();
    }

    public void save() throws IOException {
        if (data.isEmpty() && (!file.exists() || file.delete())) {
            return;
        }

        FileOutputStream raw = new FileOutputStream(file, false);
        try (EscapedOutputStream escaped = new EscapedOutputStream(raw, '\u001B', '\u0003')) {
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
        if (!file.exists()) {
            return;
        }

        try (FileInputStream in = new FileInputStream(file)) {
            ByteArrayOutputStream string = new ByteArrayOutputStream();
            ByteBuffer magic = ByteBuffer.allocate(28).order(ByteOrder.BIG_ENDIAN);

            boolean escaped = false;
            int b;
            int i = 0;
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

    @SuppressWarnings({"rawtypes", "unchecked"})
    public Supplier<?> translate(String name) {
        if (name.startsWith("::") && name.length() > 2) {
            final String translated = name.substring(2).toLowerCase();
            return () -> ((Map<String, Object>) (Map) plugin.phi.cache.getHosts()).getOrDefault(translated, name);
        } else if (name.startsWith(":") && name.length() > 1) {
            final String translated = name.substring(1);
            return () -> {
                Pair<String, List<Server>> group = plugin.phi.cache.getGroup(translated);
                return (group == null) ? name : group;
            };
        } else {
            final String translated = name.toLowerCase();
            return () -> ((Map<String, Object>) (Map) plugin.phi.cache.getServers()).getOrDefault(translated, name);
        }
    }

    public void listen() {
        if (!active && !signs.isEmpty()) {
            active = true;
            plugin.phi.listen(this::refresh);
            plugin.phi.start();
        }
    }

    private void refresh() {
        if (plugin.getLang() == null) {
            return;
        }

        for (Entry<Location, Supplier<?>> sign : signs.entrySet()) {
            refresh(sign.getKey().getBlock(), sign.getValue());
        }
    }

    public void refresh(SubServer server) {
        if (server == null || plugin.getLang() == null) {
            return;
        }

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

    @SuppressWarnings("unchecked")
    public void refresh(Block block, Supplier<?> translator) {
        if (!(block.getState() instanceof Sign)) {
            return;
        }

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
                if (incoming.getPriority() > state.getPriority())
                    state = incoming;
            }
        } else if (object instanceof Host) {
            Host host = (Host) object;
            name = host.getDisplayName();

            Text incoming;
            for (SubServer server : host.getSubServers().values()) {
                players += server.getRemotePlayers().size();
                incoming = Text.determine(server);
                if (incoming.getPriority() > state.getPriority())
                    state = incoming;
            }
        } else if (object instanceof String) {
            name = (String) object;
        } else {
            return;
        }

        String[] text = plugin.phi.replace(null, plugin.api.getLang("SubServers", state.getText()).replace("$str$", name).replace("$int$", NumberFormat.getInstance().format(players))).split("\n", 4);
        for (int i = 0; i < 4; ++i) {
            if (i < text.length) {
                sign.setLine(i, text[i]);
            } else {
                sign.setLine(i, "");
            }
        }

        Bukkit.getScheduler().runTask(plugin, sign::update);
    }


    public Map<OfflineBlock, String> getData() {
        return data;
    }

    public Map<String, Location> getLocations() {
        return locations;
    }

    public Map<Location, Supplier<?>> getSigns() {
        return signs;
    }

    public void setSigns(Map<Location, Supplier<?>> signs) {
        this.signs = signs;
    }

    public SubPlugin getPlugin() {
        return plugin;
    }
}

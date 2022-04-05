package net.ME1312.SubServers.Client.Bukkit.SubSigns.Listeners;

import net.ME1312.Galaxi.Library.Container.Pair;
import net.ME1312.SubServers.Client.Bukkit.Library.Compatibility.OfflineBlock;
import net.ME1312.SubServers.Client.Bukkit.SubPlugin;
import net.ME1312.SubServers.Client.Bukkit.SubSigns.SubSigns;
import net.ME1312.SubServers.Client.Bukkit.SubSigns.Text;
import net.ME1312.SubServers.Client.Common.Network.API.Host;
import net.ME1312.SubServers.Client.Common.Network.API.Server;
import net.ME1312.SubServers.Client.Common.Network.API.SubServer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.*;
import java.util.function.Supplier;

public class SignListeners implements Listener {

    private final SubPlugin subPlugin;
    private final SubSigns subSigns;
    private final Random random = new Random();

    public SignListeners(SubPlugin subPlugin, SubSigns subSigns) {
        this.subPlugin = subPlugin;
        this.subSigns = subSigns;
    }


    @EventHandler(priority = EventPriority.MONITOR)
    public void create(SignChangeEvent e) {
        if (e.isCancelled() || !e.getLine(0).trim().equalsIgnoreCase("[SubServers]")) {
            return;
        }

        String name = e.getLine(1).trim();

        if (name.length() <= 0 || subPlugin.getLang() == null) {
            return;
        }

        Player player = e.getPlayer();

        if (!player.hasPermission("subservers.signs")) {
            return;
        }

        Supplier<?> translator = subSigns.translate(name);
        Location location = e.getBlock().getLocation();

        HashMap<Location, Supplier<?>> signs = new HashMap<>(subSigns.getSigns());
        signs.put(location, translator);
        subSigns.getData().put(new OfflineBlock(location), name);
        subSigns.setSigns(signs);
        subSigns.getLocations().put(name.toLowerCase(), location);

        subSigns.listen();
        subSigns.refresh(e.getBlock(), translator);
        Bukkit.getLogger().info("SubServers > Server sign created: [\"" + location.getWorld().getName() + "\", " + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ() + "] -> \"" + name + '\"');
        player.sendMessage(subPlugin.api.getLang("SubServers", "Signs.Create"));
    }


    @SuppressWarnings("unchecked")
    @EventHandler(priority = EventPriority.MONITOR)
    public void interact(PlayerInteractEvent e) {
        if (e.isCancelled() || e.getClickedBlock() == null || !(e.getClickedBlock().getState() instanceof Sign)) {
            return;
        }

        Supplier<?> translator = subSigns.getSigns().get(e.getClickedBlock().getLocation());

        if (translator == null || subPlugin.getLang() == null) {
            return;
        }

        Player player = e.getPlayer();

        if (!player.hasPermission("subservers.teleport") || (e.getAction() != Action.RIGHT_CLICK_BLOCK && player.hasPermission("subservers.signs"))) {
            return;
        }

        Object object = translator.get();

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

        Text incoming;
        Text state = Text.UNKNOWN;
        List<Server> selected = new ArrayList<>();
        for (Server server : servers) {
            incoming = Text.determine(server);
            if (incoming == Text.STOPPING) {
                continue;
            }

            if (incoming == Text.OFFLINE) {
                SubServer subserver = (SubServer) server;
                if (!subserver.isEnabled() || !subserver.isAvailable() || subserver.getCurrentIncompatibilities().size() != 0) {
                    continue;
                }
            }

            if (incoming.getPriority() > state.getPriority()) {
                state = incoming;
                selected.clear();
                selected.add(server);
            } else if (incoming == state) {
                selected.add(server);
            }
        }

        if (selected.size() > 0) {
            Server server = selected.get(random.nextInt(selected.size()));
            if (state == Text.OFFLINE) {
                ((SubServer) server).start();
            } else {
                player.sendMessage(subPlugin.api.getLang("SubServers", "Command.Teleport").replace("$name$", player.getName()).replace("$str$", server.getDisplayName()));
                subPlugin.pmc(player, "Connect", server.getName());
            }
        }
    }


    @EventHandler(priority = EventPriority.MONITOR)
    public void delete(BlockBreakEvent e) {
        if (e.isCancelled() || !(e.getBlock().getState() instanceof Sign) || !subSigns.getSigns().containsKey(e.getBlock().getLocation())) {
            return;
        }

        Player player = e.getPlayer();

        if (player.hasPermission("subservers.signs") && subPlugin.getLang() != null) {
            Location location = e.getBlock().getLocation();
            String name = subSigns.getData().remove(new OfflineBlock(location));
            if (name != null) {
                subSigns.getLocations().remove(name.toLowerCase());
            }

            HashMap<Location, Supplier<?>> signs = new HashMap<>(subSigns.getSigns());
            signs.remove(location);
            subSigns.setSigns(signs);

            player.sendMessage(subPlugin.api.getLang("SubServers", "Signs.Delete"));
        } else {
            e.setCancelled(true);
        }
    }
}

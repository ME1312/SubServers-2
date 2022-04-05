package net.ME1312.SubServers.Client.Bukkit.SubSigns.Listeners;

import net.ME1312.SubServers.Client.Bukkit.Event.SubStartEvent;
import net.ME1312.SubServers.Client.Bukkit.Event.SubStartedEvent;
import net.ME1312.SubServers.Client.Bukkit.Event.SubStopEvent;
import net.ME1312.SubServers.Client.Bukkit.Event.SubStoppedEvent;
import net.ME1312.SubServers.Client.Bukkit.SubPlugin;
import net.ME1312.SubServers.Client.Bukkit.SubSigns.SubSigns;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class SubServerListeners implements Listener {

    private final SubPlugin subPlugin;
    private final SubSigns subSigns;

    public SubServerListeners(SubPlugin subPlugin, SubSigns subSigns) {
        this.subPlugin = subPlugin;
        this.subSigns = subSigns;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void start(SubStartEvent e) {
        subSigns.refresh(subPlugin.phi.cache.getSubServer(e.getServer()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void started(SubStartedEvent e) {
        subSigns.refresh(subPlugin.phi.cache.getSubServer(e.getServer()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void stopping(SubStopEvent e) {
        subSigns.refresh(subPlugin.phi.cache.getSubServer(e.getServer()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void stopped(SubStoppedEvent e) {
        subSigns.refresh(subPlugin.phi.cache.getSubServer(e.getServer()));
    }
}

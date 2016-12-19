package net.ME1312.SubServers.Client.Bukkit;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class SubCommand implements CommandExecutor {
    private SubPlugin plugin;

    public SubCommand(SubPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        plugin.gui.getRenderer((Player) sender).newUI();
        return true;
    }
}
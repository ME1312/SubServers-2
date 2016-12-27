package net.ME1312.SubServers.Client.Bukkit.Graphic;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public interface Renderer {
    void open(Player player, String object);
    ItemStack getIcon();
    boolean isEnabled(String object);
}

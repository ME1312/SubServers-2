package net.ME1312.SubServers.Client.Bukkit.Library.Compatibility;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.Objects;
import java.util.UUID;

/**
 * Offline Block Location Class
 */
public class OfflineBlock {
    public final UUID world;
    public final int x, y, z;

    /**
     * Convert an existing location object
     *
     * @param location Location
     */
    public OfflineBlock(Location location) {
        this(location.getWorld().getUID(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    /**
     * Store location data
     *
     * @param world World ID
     * @param x X position
     * @param y Y position
     * @param z Z position
     */
    public OfflineBlock(UUID world, int x, int y, int z) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /**
     * Find the World if loaded
     *
     * @return World (or null if unavailable)
     */
    public World world() {
        return Bukkit.getWorld(this.world);
    }

    /**
     * Find the Location if loaded
     *
     * @return Location (or null if unavailable)
     */
    public Location load() {
        World world = Bukkit.getWorld(this.world);
        return (world == null)? null : new Location(world, x, y, z);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OfflineBlock that = (OfflineBlock) o;
        return x == that.x && y == that.y && z == that.z && Objects.equals(world, that.world);
    }

    @Override
    public int hashCode() {
        return Objects.hash(world, x, y, z);
    }
}

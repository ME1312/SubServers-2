package net.ME1312.SubServers.Proxy.Host;

import net.ME1312.SubServers.Proxy.Libraries.Version.Version;

import java.io.File;
import java.util.UUID;

/**
 * SubCreator Layout Class
 *
 * @author ME1312
 */
public abstract class SubCreator {
    public enum ServerType {
        SPIGOT,
        BUKKIT,
        VANILLA,
        SPONGE,
    }

    public abstract void create(UUID player, String name, int port, File directory, ServerType type, Version version, int memory);
    public void create(String name, int port, File directory, ServerType type, Version version, int memory) {
        create(null, name, port, directory, type, version, memory);
    }

    public abstract void waitFor() throws InterruptedException;

    public abstract Host getHost();

    public abstract boolean isBusy();
}

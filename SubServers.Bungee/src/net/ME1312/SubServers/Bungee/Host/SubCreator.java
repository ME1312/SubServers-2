package net.ME1312.SubServers.Bungee.Host;

import net.ME1312.SubServers.Bungee.Library.Version.Version;

import java.util.UUID;

/**
 * SubCreator Layout Class
 *
 * @author ME1312
 */
public abstract class SubCreator {
    public enum ServerType {
        SPIGOT,
        VANILLA,
        SPONGE,
    }

    public abstract boolean create(UUID player, String name, ServerType type, Version version, int memory, int port);
    public boolean create(String name, ServerType type, Version version, int memory, int port) {
        return create(null, name, type, version, memory, port);
    }

    public abstract void waitFor() throws InterruptedException;

    public abstract Host getHost();

    public abstract String getGitBashDirectory();

    public abstract boolean isBusy();
}

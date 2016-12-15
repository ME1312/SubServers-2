package net.ME1312.SubServers.Proxy.Host;

import net.ME1312.SubServers.Proxy.Library.Version.Version;

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
        VANILLA,
        SPONGE,
    }

    public abstract void create(UUID player, String name, ServerType type, Version version, int memory, int port);
    public void create(String name, ServerType type, Version version, int memory, int port) {
        create(null, name, type, version, memory, port);
    }

    public abstract void waitFor() throws InterruptedException;

    public abstract Host getHost();

    public abstract String getGitBashDirectory();

    public abstract boolean isBusy();
}

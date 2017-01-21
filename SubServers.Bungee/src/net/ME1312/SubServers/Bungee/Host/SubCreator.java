package net.ME1312.SubServers.Bungee.Host;

import net.ME1312.SubServers.Bungee.Library.Version.Version;

import java.util.UUID;

/**
 * SubCreator Layout Class
 */
public abstract class SubCreator {
    public enum ServerType {
        SPIGOT,
        VANILLA,
        SPONGE,;

        @Override
        public String toString() {
            return super.toString().substring(0, 1).toUpperCase()+super.toString().substring(1).toLowerCase();
        }
    }

    /**
     * Create a SubServer
     *
     * @param player Player Creating
     * @param name Server Name
     * @param type Server Type
     * @param version Server Version
     * @param memory Server Memory Amount (in MB)
     * @param port Server Port Number
     * @return Success Status
     */
    public abstract boolean create(UUID player, String name, ServerType type, Version version, int memory, int port);

    /**
     * Create a SubServer
     *
     * @param name Server Name
     * @param type Server Type
     * @param version Server Version
     * @param memory Server Memory Amount (in MB)
     * @param port Server Port Number
     * @return Success Status
     */
    public boolean create(String name, ServerType type, Version version, int memory, int port) {
        return create(null, name, type, version, memory, port);
    }

    /**
     * Terminate SubCreator
     */
    public abstract void terminate();

    /**
     * Wait for SubCreator to Finish
     *
     * @throws InterruptedException
     */
    public abstract void waitFor() throws InterruptedException;

    /**
     * Gets the host this creator belongs to
     *
     * @return Host
     */
    public abstract Host getHost();

    /**
     * Gets the Git Bash install directory
     *
     * @return Git Bash Directory
     */
    public abstract String getBashDirectory();

    /**
     * Gets the Logger for the creator
     *
     * @return
     */
    public abstract SubLogger getLogger();

    /**
     * Gets the status of SubCreator
     *
     * @return SubCreator Status
     */
    public abstract boolean isBusy();
}

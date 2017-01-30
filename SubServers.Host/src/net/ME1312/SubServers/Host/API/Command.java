package net.ME1312.SubServers.Host.API;

/**
 * Command Layout Class
 */
public interface Command {
    /**
     * Run Command
     *
     * @param command Command Name
     * @param args Arguments
     */
    void command(String command, String[] args);
}

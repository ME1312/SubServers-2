package net.ME1312.SubServers.Sync.Server;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/**
 * SubServer Class
 */
public class SubServerContainer extends ServerContainer {
    private boolean running;

    public SubServerContainer(String signature, String name, String display, InetSocketAddress address, Map<Integer, UUID> subdata, String motd, boolean hidden, boolean restricted, Collection<UUID> whitelist, boolean running) {
        super(signature, name, display, address, subdata, motd, hidden, restricted, whitelist);
        this.running = running;
    }

    /**
     * Gets the Running Status
     *
     * @return Running Status
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Sets the Running Status
     *
     * @param running Running Status
     */
    public void setRunning(boolean running) {
        this.running = running;
    }
}

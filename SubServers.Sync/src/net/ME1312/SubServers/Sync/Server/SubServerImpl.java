package net.ME1312.SubServers.Sync.Server;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/**
 * SubServer Class
 */
public class SubServerImpl extends ServerImpl {
    private boolean running;

    public static SubServerImpl construct(String signature, String name, String display, SocketAddress address, Map<Integer, UUID> subdata, String motd, boolean hidden, boolean restricted, Collection<UUID> whitelist, boolean running) {
        try {
            return new SubServerImpl(signature, name, display, address, subdata, motd, hidden, restricted, whitelist, running);
        } catch (NoSuchMethodError e) {
            return new SubServerImpl(signature, name, display, (InetSocketAddress) address, subdata, motd, hidden, restricted, whitelist, running);
        }
    }

    /*
     * Super Method 2
     */
    protected SubServerImpl(String signature, String name, String display, SocketAddress address, Map<Integer, UUID> subdata, String motd, boolean hidden, boolean restricted, Collection<UUID> whitelist, boolean running) {
        super(signature, name, display, address, subdata, motd, hidden, restricted, whitelist);
        this.running = running;
    }

    /*
     * Super Method 1
     */
    protected SubServerImpl(String signature, String name, String display, InetSocketAddress address, Map<Integer, UUID> subdata, String motd, boolean hidden, boolean restricted, Collection<UUID> whitelist, boolean running) {
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

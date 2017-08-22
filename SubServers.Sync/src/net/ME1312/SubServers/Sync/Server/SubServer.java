package net.ME1312.SubServers.Sync.Server;

import java.net.InetSocketAddress;

/**
 * SubServer Class
 */
public class SubServer extends Server {
    private boolean running;

    public SubServer(String name, String display, InetSocketAddress address, String motd, boolean hidden, boolean restricted, boolean running) {
        super(name, display, address, motd, hidden, restricted);
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

package net.ME1312.SubServers.Proxy.Host.Internal;

import net.ME1312.SubServers.Proxy.Host.Host;
import net.ME1312.SubServers.Proxy.Host.SubCreator;
import net.ME1312.SubServers.Proxy.Libraries.Version.Version;

import java.io.File;
import java.util.UUID;

public class InternalSubCreator extends SubCreator {
    private Host host;

    public InternalSubCreator(Host host) {
        this.host = host;
    }

    @Override
    public void create(UUID player, String name, int port, File directory, ServerType type, Version version, int memory) {

    }

    @Override
    public void waitFor() throws InterruptedException {

    }

    @Override
    public Host getHost() {
        return host;
    }

    @Override
    public boolean isBusy() {
        return false;
    }
}

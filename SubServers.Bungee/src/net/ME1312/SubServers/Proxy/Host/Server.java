package net.ME1312.SubServers.Proxy.Host;

import net.ME1312.SubServers.Proxy.Library.Exception.InvalidServerException;
import net.ME1312.SubServers.Proxy.Network.Client;
import net.ME1312.SubServers.Proxy.Network.ClientHandler;
import net.md_5.bungee.BungeeServerInfo;
import net.md_5.bungee.api.ChatColor;

import java.net.InetSocketAddress;

/**
 * Server Class
 *
 * @author ME1312
 */
public class Server extends BungeeServerInfo implements ClientHandler {
    private Client client = null;

    public Server(String name, InetSocketAddress address, String motd, boolean restricted) throws InvalidServerException {
        super(name, address, ChatColor.translateAlternateColorCodes('&', motd), restricted);
        if (name.contains(" ")) throw new InvalidServerException("Server names cannot have spaces: " + name);
    }

    @Override
    public Client getSubDataClient() {
        return client;
    }

    @Override
    public void linkSubDataClient(Client client) {
        if (this.client == null) {
            client.setHandler(this);
            this.client = client;
        } else if (client == null) {
            this.client = null;
        } else throw new IllegalStateException("A SubData Client is already linked to Server: " + getName());
    }
}

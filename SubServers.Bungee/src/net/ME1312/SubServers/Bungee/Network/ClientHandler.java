package net.ME1312.SubServers.Bungee.Network;

/**
 * Client Handler Layout Class
 *
 * @author ME1312
 */
public interface ClientHandler {
    /**
     * Gets the SubData Client
     *
     * @return SubData Client (or null if not linked)
     */
    Client getSubDataClient();

    /**
     * Link a SubData Client to this Object
     *
     * @param client Client to Link
     */
    void linkSubDataClient(Client client);
}
